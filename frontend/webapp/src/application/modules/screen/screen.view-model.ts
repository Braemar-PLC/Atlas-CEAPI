import { useMemo } from "react";
import { SpreadsGroup, useNatGas } from "@atlas/data";
import type { NatGasCurveMap, NatGasQuote, Screen } from "@atlas/data";
import { GasView, screenView } from "@/application/registries/screen-views";
import type { ScreenView } from "@/application/registries/screen-views";
import { productOf } from "@/application/registries/products";
import { buildScreenColumnDefs } from "./screen.col-defs";
import { impliedQuote } from "./implied";
import type { ImpliedQuote } from "./implied";

type Quote = Partial<NatGasQuote>;

/**
 * One grid row: the hub (`symbol`), the strip label (`tenor`), the product where the screen shows one (coal)
 * and whatever figures are known so far.
 */
export type ScreenGridRow = Quote & ImpliedQuote & { symbol: string; tenor: string; product?: string };

/**
 * A spread worked out from its two legs, for pairs ICE does not quote as a contract (a month against a
 * quarter, say). The sides are crossed, because that is what you could actually deal at:
 *   to SELL the spread you sell the near leg (at its bid) and buy the far leg (at its offer);
 *   to BUY the spread you buy the near leg (at its offer) and sell the far leg (at its bid).
 * Checked against the desk's Edgeview screen on 21 Sep 2026: Oct26 75.925/75.965 against Q1 27 73.640/73.740
 * shows as 2.185/2.325 there, and here.
 * Last, change, settle, high, low and volume belong to a traded contract, and there is none - they stay blank.
 */
export function computeSpreadQuote(near: Quote | undefined, far: Quote | undefined): Quote {
  const quote: Quote = {};
  // Rounded to ICE's finest tick (0.005 needs 3 decimals) so 75.925 - 73.74 is 2.185, not 2.1850000000000023.
  const tidy = (n: number) => Math.round(n * 1000) / 1000;

  if (near?.bid != null && far?.ask != null) quote.bid = tidy(near.bid - far.ask);
  if (near?.ask != null && far?.bid != null) quote.ask = tidy(near.ask - far.bid);

  // As delayed as its more delayed leg.
  const delays = [near?.delayMinutes, far?.delayMinutes].filter((d): d is number => d != null);
  if (delays.length) quote.delayMinutes = Math.max(...delays);

  return quote;
}

/**
 * Store → grid rows for one screen.
 * The screen (from the API) says which rows there are and in what order; the store supplies the figures.
 * Every row of the screen is shown even before it has a price - as on ICE, where a strip with no orders still
 * has its line. Anything in the store that the screen does not list (another hub, an expired month) is ignored.
 * With `spreads` (the coal flat screens: screen-views.ts names it), every outright row also gets the bid and
 * offer that hub's spread rows imply for it (implied.ts); the spread rows themselves get none.
 */
export function buildScreenRows(curves: NatGasCurveMap, screen: Screen, view: ScreenView = GasView, spreads?: Screen): ScreenGridRow[] {
  const spreadLabels = spreads ? spreadLabelsByHub(spreads) : undefined;

  return screen.rows.map(row => {
    const strips = curves[row.hub] ?? {};
    const quote = row.source === "computed"
      ? computeSpreadQuote(strips[row.near?.label ?? ""], strips[row.far?.label ?? ""])
      : strips[row.label] ?? {};

    const implied = spreadLabels && row.group !== SpreadsGroup
      ? impliedQuote(row.label, spreadLabels.get(row.hub) ?? [], strips, view.priceDecimals)
      : {};
    const product = productOf(row.hub, row.group);

    // `symbol` and `tenor` go last so a quote can never overwrite the row's identity.
    return { ...quote, ...implied, ...(product ? { product } : {}), symbol: row.hub, tenor: row.label };
  });
}

/** The spread rows of a spreads screen, by hub - the candidates for each hub's implied prices. */
function spreadLabelsByHub(spreads: Screen): Map<string, string[]> {
  const byHub = new Map<string, string[]>();
  for (const row of spreads.rows) {
    if (row.group === SpreadsGroup) {
      byHub.set(row.hub, [...(byHub.get(row.hub) ?? []), row.label]);
    }
  }
  return byHub;
}

/**
 * How many minutes behind the exchange the rows are, as ICE reports it on each quote.
 * The largest value wins, so the screen never claims to be fresher than its oldest row.
 * Undefined until a row has said anything.
 */
export function delayMinutesOf(rows: { delayMinutes?: number }[]): number | undefined {
  const delays = rows.map(r => r.delayMinutes).filter((d): d is number => d != null);
  return delays.length ? Math.max(...delays) : undefined;
}

/** "Prices as of 15:10:00" — the clock time the prices belong to: now, less the feed's delay. Local time. */
export function formatPricesAsOf(now: Date, delayMinutes: number): string {
  const asOf = new Date(now.getTime() - delayMinutes * 60_000);
  const two = (n: number) => String(n).padStart(2, "0");
  return `Prices as of ${two(asOf.getHours())}:${two(asOf.getMinutes())}:${two(asOf.getSeconds())}`;
}

/**
 * VM = domain state → UI props
 * No SSE here. No lifecycle. No React other than selectors.
 * `screenKey` picks the columns (screen-views.ts); `screen` is undefined until the API has answered, and the
 * grid is simply empty until then. `spreads` is the spreads screen the implied prices come from, where the view
 * names one.
 */
export function useScreenViewModel(screenKey: string, screen: Screen | undefined, spreads?: Screen) {
  const curves = useNatGas(s => s.curves);
  const view = screenView(screenKey);
  // Built once per screen, not per render: a new array on every price tick would make AG Grid rebuild its
  // columns. screenView answers the same object for the same key, so the memo holds across ticks.
  const colDefs = useMemo(() => buildScreenColumnDefs(view), [view]);
  const rows = screen ? buildScreenRows(curves, screen, view, spreads) : [];

  return { title: screen?.title ?? "", rows, colDefs, delayMinutes: delayMinutesOf(rows) };
}
