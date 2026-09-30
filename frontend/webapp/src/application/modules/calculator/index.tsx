import { useEffect, useMemo, useRef, useState } from "react";
import type { GridApi } from "ag-grid-community";
import { fetchOptionMatrix } from "@atlas/external";
import { useNatGas, yearsToExpiry } from "@atlas/data";
import type { OptionMatrix } from "@atlas/data";
import { acquireScreenStream } from "@/application/subscriptions/natgas-subscriptions-manager";
import { reloadIfSignedOut } from "@/application/auth/reload-if-signed-out";
import { useNow, useScreen, useThrottled } from "@/application/modules/screen/screen.hooks";
import { ScreenComponent } from "@/application/modules/screen/screen.component";
import { formatPricesAsOf } from "@/application/modules/screen/screen.view-model";
import { buildChainRows } from "@/application/modules/chain/chain.view-model";
import { chainView } from "@/application/registries/chain-views";
import {
  Strategies, expiriesOf, legsOf, referencePriceOf, settlementValueOf, straddlesOf, strategyTitle, strikesOf, valueStrategy,
} from "./calculator.view-model";
import type { Strategy } from "./calculator.view-model";
import { contractCode } from "./contract-code";
import { atmRowIndex, buildMatrixRows, matrixRowClassRules } from "./matrix.view-model";
import { buildMatrixColumnDefs } from "./matrix.col-defs";
import "./calculator.css";

// The products with an option chain, as the desk's tabs name them, the API's name for each, and ICE's option root.
const Products = [
  { label: "TTF", product: "TTF", key: "xcom-ttf", root: "TFO" },
  { label: "EUA", product: "EUA", key: "xcom-eua", root: "EFO" },
  { label: "WTI", product: "WTI", key: "xcom-wti", root: "WBS" },
  { label: "Brent", product: "Brent", key: "xcom-brent", root: "BRN" },
] as const;

const fixed = (v: number | undefined, dp: number) => (v == null ? "" : v.toFixed(dp));

/**
 * How often the calculator takes up new prices. The store changes at every frame the browser draws; each change
 * here rebuilds the matrix, which prices some four thousand cells, so the page follows the prices five times a
 * second. The expiry headings read their futures from the store themselves and follow every frame.
 */
const CalculatorPriceIntervalMs = 200;

/**
 * The product's matrix skeleton from the API (GET /api/options/{product}); undefined until it answers. The
 * answer is kept with the product it is for, so switching product shows nothing stale while the next one loads.
 */
function useOptionMatrix(product: string) {
  const [loaded, setLoaded] = useState<{ product: string; matrix: OptionMatrix }>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    let cancelled = false;
    fetchOptionMatrix(product)
      .then(matrix => { if (!cancelled) { setError(undefined); setLoaded({ product, matrix }); } })
      .catch(async e => {
        if (cancelled || await reloadIfSignedOut()) return;
        setError(e instanceof Error ? e.message : String(e));
      });
    return () => { cancelled = true; };
  }, [product]);

  return { matrix: loaded?.product === product ? loaded.matrix : undefined, error };
}

/**
 * The options calculator (docs/roadmap.md, section 12, item 2), as the central box of the desk's own: product
 * tabs, a slim strategy bar, and the matrix - every listed strike of every expiry the desk asked for (12 months,
 * 8 quarters, 8 seasons, 5 cals), each priced with Black-76 at the vol the market gives for that strike or,
 * elsewhere, the skew fitted through the strikes the chain streams. The futures' prices and the near-the-money
 * quotes come from the product's chain stream, so every figure moves with the market.
 */
export default function CalculatorModule() {
  const [productIndex, setProductIndex] = useState(0);
  const [expiry, setExpiry] = useState<string>();
  const [strategy, setStrategy] = useState<Strategy>("call");
  const [chosenStrikes, setChosenStrikes] = useState<number[]>([]);
  const [refPriceText, setRefPriceText] = useState("");
  const [volText, setVolText] = useState("");
  const [rateText, setRateText] = useState("0");
  const gridApi = useRef<GridApi | null>(null);
  const scrolledFor = useRef<string | undefined>(undefined);

  const { product, key: screenKey, root } = Products[productIndex];
  const { screen, error: screenError } = useScreen(screenKey);
  const { matrix, error: matrixError } = useOptionMatrix(product);
  useEffect(() => {
    if (!screen) return;

    const handle = acquireScreenStream(screen);
    return () => handle.release();
  }, [screen]);

  const curves = useThrottled(useNatGas(s => s.curves), CalculatorPriceIntervalMs);
  const now = useNow();
  const view = chainView(screenKey);
  const ladder = useMemo(() => (screen ? buildChainRows(curves, screen) : []), [curves, screen]);
  const expiries = useMemo(() => (screen ? expiriesOf(screen) : []), [screen]);
  const rate = rateText.trim() === "" ? 0 : Number(rateText) / 100;
  const straddles = useMemo(() => (screen ? straddlesOf(screen, ladder, now, rate) : []), [screen, ladder, now, rate]);

  // The strategy: the first expiry once the chain arrives, the strikes nearest the future unless chosen.
  const activeExpiry = expiries.find(e => e.expiry === expiry) ?? expiries[0];
  const strikes = useMemo(() => (activeExpiry ? strikesOf(ladder, activeExpiry.expiry) : []), [ladder, activeExpiry]);
  // Memoised (remembered until an input changes), as the strikes below are, so the matrix at the bottom is rebuilt
  // only when the ladder, the expiry or the typed reference price really change - not on every keystroke elsewhere.
  const futureMarket = useMemo(() => {
    const futureRow = activeExpiry ? ladder.find(r => r.kind === "future" && r.symbol === activeExpiry.expiry) : undefined;
    return referencePriceOf(futureRow?.call);
  }, [ladder, activeExpiry]);
  const refPrice = useMemo(() => (refPriceText.trim() === "" ? futureMarket?.price : Number(refPriceText)), [refPriceText, futureMarket]);
  const needed = Strategies[strategy].strikes;
  // Memoised, like the ladder's strikes above, so the matrix below is rebuilt only when the strikes really change.
  const activeStrikes = useMemo(
    () => (chosenStrikes.length === needed && chosenStrikes.every(k => strikes.includes(k)) ? chosenStrikes : nearestStrikes(strikes, refPrice, needed)),
    [chosenStrikes, needed, strikes, refPrice],
  );
  const volOverride = volText.trim() === "" ? undefined : Number(volText) / 100;
  const years = activeExpiry ? yearsToExpiry(activeExpiry.expiryDate, now) : 0;
  const expiryLadder = activeExpiry ? ladder.filter(r => r.symbol === activeExpiry.expiry) : [];
  const legs = activeStrikes.length === needed ? legsOf(strategy, activeStrikes) : [];
  const priced = refPrice != null && activeExpiry && legs.length
    ? valueStrategy(legs, { future: refPrice, years, rate, volOverride }, expiryLadder)
    : undefined;
  const settlement = legs.length ? settlementValueOf(legs, expiryLadder) : undefined;
  const title = activeExpiry && legs.length ? strategyTitle(root, contractCode(activeExpiry.expiry), activeStrikes, strategy) : "";

  // The matrix: columns follow the expiries and the chain's labels (rebuilt when those change); rows follow the
  // ladder, the clock and the inputs, and are rebuilt only when one of those changes - a build prices some four
  // thousand cells, so never on a render that changed none of them.
  const colDefs = useMemo(() => (matrix ? buildMatrixColumnDefs(matrix, screen, view) : []), [matrix, screen, view]);
  const strikeStep = view.strikeStep;
  const rows = useMemo(
    () => (matrix ? buildMatrixRows(matrix, ladder, { straddles, now, rate, legStrikes: activeStrikes, strikeStep }) : []),
    [matrix, ladder, straddles, now, rate, activeStrikes, strikeStep],
  );

  // Open each product scrolled to the money, once - never again on a tick, so a person's own scrolling holds.
  const atmIndex = atmRowIndex(rows, straddles[0]?.future?.price);
  useEffect(() => {
    const api = gridApi.current;
    if (!api || atmIndex < 0 || scrolledFor.current === product) return;
    api.ensureIndexVisible(atmIndex, "middle");
    scrolledFor.current = product;
  }, [product, atmIndex]);

  const delays = ladder.flatMap(r => [r.call?.delayMinutes, r.put?.delayMinutes]).filter((d): d is number => d != null);
  const asOf = delays.length ? formatPricesAsOf(now, Math.max(...delays)) : "";
  const error = matrixError ?? screenError;
  const gridTitle = matrix ? `${product} options · vol: market where streamed (amber), fitted elsewhere, borrowed for an expiry with no quotes (grey)`
    : (error ? `Cannot load the calculator: ${error}` : "Loading…");

  const choose = (index: number, strike: number) => {
    const next = [...activeStrikes];
    next[index] = strike;
    setChosenStrikes(next);
  };
  const reset = () => { setExpiry(undefined); setChosenStrikes([]); setRefPriceText(""); };

  return (
    <div className="calc">
      <div className="calc-tabs" role="tablist">
        {Products.map((p, i) => (
          <button key={p.key} type="button" role="tab" aria-selected={i === productIndex}
            className={i === productIndex ? "calc-tab calc-tab-active" : "calc-tab"}
            onClick={() => { setProductIndex(i); reset(); }}>
            {p.label}
          </button>
        ))}
      </div>

      <div className="calc-strategy">
        <span className="calc-strategy-title">{title}</span>
        <label>Expiry
          <select value={activeExpiry?.expiry ?? ""} onChange={e => { setExpiry(e.target.value); setChosenStrikes([]); setRefPriceText(""); }}>
            {expiries.map(e => <option key={e.expiry} value={e.expiry}>{contractCode(e.expiry)}</option>)}
          </select>
        </label>
        <label>Strategy
          <select value={strategy} onChange={e => { setStrategy(e.target.value as Strategy); setChosenStrikes([]); }}>
            {Object.entries(Strategies).map(([k, s]) => <option key={k} value={k}>{s.label}</option>)}
          </select>
        </label>
        {Array.from({ length: needed }, (_, i) => (
          <label key={i}>{needed === 1 ? "Strike" : `Strike ${i + 1}`}
            <select value={activeStrikes[i] ?? ""} onChange={e => choose(i, Number(e.target.value))}>
              {strikes.map(k => <option key={k} value={k}>{k.toFixed(2)}</option>)}
            </select>
          </label>
        ))}
        <label>RefPr
          <input value={refPriceText} onChange={e => setRefPriceText(e.target.value)} inputMode="decimal"
            placeholder={futureMarket ? `${futureMarket.price.toFixed(view.priceDecimals)} (${futureMarket.basis})` : ""} />
        </label>
        <label>Vol %
          <input value={volText} onChange={e => setVolText(e.target.value)} inputMode="decimal" placeholder="implied" />
        </label>
        <label>Rate %
          <input value={rateText} onChange={e => setRateText(e.target.value)} inputMode="decimal" />
        </label>
        <div className="calc-results">
          <span className="calc-result calc-result-theo"><b>{fixed(priced?.net?.price, view.priceDecimals)}</b><small>Theo</small></span>
          <span className="calc-result"><b>{fixed(priced?.net == null ? undefined : priced.net.delta * 100, 1)}</b><small>ΔNet</small></span>
          <span className="calc-result"><b>{fixed(priced?.net?.gamma, 2)}</b><small>Γ</small></span>
          <span className="calc-result"><b>{priced?.legs.map(l => (l.vol == null ? "" : `${(l.vol * 100).toFixed(2)}%`)).join(" / ")}</b><small>Vol</small></span>
          <span className="calc-result"><b>{fixed(priced?.net?.vega, 4)}</b><small>V</small></span>
          <span className="calc-result"><b>{fixed(priced?.net?.theta, 4)}</b><small>Θ</small></span>
          <span className="calc-result"><b>{fixed(refPrice, view.priceDecimals)}</b><small>RefPr</small></span>
          <span className="calc-result"><b>{fixed(settlement, view.priceDecimals)}</b><small>Sett</small></span>
        </div>
      </div>

      <div className="calc-matrix">
        <ScreenComponent
          screenKey={`${screenKey}-matrix`}
          title={gridTitle}
          data={rows}
          colDefs={colDefs}
          asOf={asOf}
          rowClassRules={matrixRowClassRules}
          headerHeight={0}
          groupHeaderHeight={30}
          onGridReady={api => { gridApi.current = api; }}
        />
      </div>

      <div className="calc-totals">
        <span>Δ: <b>{fixed(priced?.net == null ? undefined : priced.net.delta * 100, 1)}</b></span>
        <span>Γ: <b>{fixed(priced?.net?.gamma, 2)}</b></span>
        <span>V: <b>{fixed(priced?.net?.vega, 4)}</b></span>
        <span>Θ: <b>{fixed(priced?.net?.theta, 4)}</b></span>
        <span>Days: <b>{activeExpiry ? (years * 365).toFixed(1) : ""}</b></span>
      </div>
    </div>
  );
}

/** The `count` listed strikes nearest the price, rising: the sensible starting point for any strategy. */
function nearestStrikes(strikes: number[], price: number | undefined, count: number): number[] {
  if (price == null || strikes.length === 0) return [];
  const nearest = strikes.reduce((best, k) => Math.abs(k - price) < Math.abs(best - price) ? k : best);
  const at = strikes.indexOf(nearest);
  const first = Math.max(0, Math.min(at - Math.floor((count - 1) / 2), strikes.length - count));
  return strikes.slice(first, first + count);
}
