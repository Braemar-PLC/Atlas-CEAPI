import { black76, fitSkew, impliedVol, volOnSkew, yearsToExpiry } from "@atlas/data";
import type { NatGasQuote, OptionMatrix, OptionMatrixExpiry, SkewFit, SkewPoint } from "@atlas/data";
import type { RowClassRules } from "ag-grid-community";
import type { ChainGridRow } from "@/application/modules/chain/chain.view-model";
import { referencePriceOf } from "./calculator.view-model";
import type { Straddle } from "./calculator.view-model";

type Quote = Partial<NatGasQuote>;

/**
 * Where a cell's vol came from: "market" - read off this strike's own traded price; "fit" - the skew fitted
 * through this expiry's streamed strikes; "borrowed" - the nearest earlier expiry's skew, because this expiry
 * streams no quotes at all (the trial feed sends nothing for the later expiries).
 */
export type VolSource = "market" | "fit" | "borrowed";

/**
 * One cell of the matrix: what Black-76 makes of the call and the put at one strike of one expiry, at the vol
 * chosen for that strike. Deltas and the vol are in percent, as the desk's calculator shows them. A cell exists
 * for every strike the expiry lists, so the strike shows even before there is a vol to price it at.
 */
export type MatrixCell = {
  strike: number;
  call?: number;
  callDelta?: number;
  put?: number;
  putDelta?: number;
  vol?: number;
  volSource?: VolSource;
};

/** One row of the matrix: a strike, with a cell per expiry keyed by the expiry's label. `symbol` and `tenor` identify the row to the grid. */
export type MatrixGridRow = {
  symbol: "matrix";
  tenor: string;
  strike: number;
  /** True on the strikes of the strategy being priced, so the grid can pick the rows out. */
  leg: boolean;
  /** True on every multiple of 5, the desk's permanent blue guide rows (Sean, 2026-09-25). */
  stripe: boolean;
  cells: Record<string, MatrixCell>;
};

/** Marks the guide rows and the strategy's strikes so the stylesheet can pick them out. A constant, as the grid component requires. */
export const matrixRowClassRules: RowClassRules<MatrixGridRow> = {
  "ice-matrix-stripe": p => p.data?.stripe === true,
  "ice-matrix-leg": p => p.data?.leg === true,
};

export type MatrixInputs = {
  /** The straddles per expiry (calculator.view-model): the futures' traded prices and the at-the-money vols. */
  straddles: Straddle[];
  now: Date;
  rate: number;
  /** The strategy's strikes, to flag their rows. */
  legStrikes: number[];
  /** The step between the matrix's strikes: 0.5, as the desk's ladder. */
  strikeStep: number;
};

/** Every strike from `low` to `high` at the step, on a grid that includes the whole numbers. */
export function strikeGrid(low: number, high: number, step: number): number[] {
  const strikes: number[] = [];
  for (let k = Math.ceil(low / step) * step; k <= high + 1e-9; k += step) {
    strikes.push(Math.round(k * 1000) / 1000);
  }
  return strikes;
}

/**
 * Which strikes the matrix shows: from half to one-and-a-half times the front expiry's future, as the desk
 * scrolls a ladder around the money rather than every strike ICE lists (Brent lists strikes from 1 up). With no
 * future price yet, the strikes the chain screen chose for the front expiry stand in.
 */
export function strikeWindow(matrix: OptionMatrix, straddles: Straddle[], ladder: ChainGridRow[]): [number, number] | undefined {
  const front = matrix.expiries[0];
  if (!front) return undefined;

  const future = straddles.find(s => s.expiry === front.label)?.future?.price;
  if (future != null) return [future * 0.5, future * 1.5];

  const shown = ladder.filter(r => r.kind === "strike" && r.symbol === front.label).map(r => Number(r.tenor));
  return shown.length ? [Math.min(...shown), Math.max(...shown)] : undefined;
}

// The ladder's strike rows by expiry and strike, built once per ladder and kept for as long as that ladder lives:
// a build of the matrix asks for a quote some four thousand times, and scanning the 344 rows each time was most
// of the 14 ms a build cost (2026-09-25). A new ladder (every store change makes one) gets its own index.
const ladderIndexes = new WeakMap<ChainGridRow[], Map<string, ChainGridRow>>();
const strikeKey = (expiry: string, strike: number) => `${expiry}|${strike}`;

function strikeRowsOf(ladder: ChainGridRow[]): Map<string, ChainGridRow> {
  let index = ladderIndexes.get(ladder);
  if (!index) {
    index = new Map();
    for (const row of ladder) {
      if (row.kind === "strike") index.set(strikeKey(row.symbol, Number(row.tenor)), row);
    }
    ladderIndexes.set(ladder, index);
  }
  return index;
}

/** The quote of the call or put at a strike of one expiry, from the chain's ladder rows. */
function quoteAt(ladder: ChainGridRow[], expiry: string, strike: number, right: "C" | "P"): Quote | undefined {
  const row = strikeRowsOf(ladder).get(strikeKey(expiry, strike));
  return right === "C" ? row?.call : row?.put;
}

/** The out-of-the-money side at a strike: a call above the future, a put below. Its price carries the vol; the other side's is mostly intrinsic. */
const outOfTheMoneySide = (strike: number, future: number): "C" | "P" => (strike >= future ? "C" : "P");

/** The vol the market gives at one strike, read off the out-of-the-money side's traded price; undefined without a usable price. */
export function marketVolAt(
  expiry: OptionMatrixExpiry, strike: number, future: number, years: number, rate: number, ladder: ChainGridRow[],
): number | undefined {
  const right = outOfTheMoneySide(strike, future);
  const market = referencePriceOf(quoteAt(ladder, expiry.label, strike, right));
  return market ? impliedVol({ right, strike, future, years, rate }, market.price) : undefined;
}

/** Every strike of the expiry the chain streams with a usable price: the points a skew is fitted through. */
export function marketVolsOf(
  expiry: OptionMatrixExpiry, future: number, years: number, rate: number, ladder: ChainGridRow[],
): SkewPoint[] {
  const points: SkewPoint[] = [];
  for (const row of ladder) {
    if (row.kind !== "strike" || row.symbol !== expiry.label) continue;
    const strike = Number(row.tenor);
    const vol = marketVolAt(expiry, strike, future, years, rate, ladder);
    if (vol != null) points.push({ strike, vol });
  }
  return points;
}

/**
 * One skew per expiry that has a future price and something to fit through: the band's market vols, with the
 * at-the-money straddle vol as the level when the band is thin. An expiry with a future but no quotes at all
 * gets no entry here; buildMatrixRows borrows for it.
 */
export function skewsOf(matrix: OptionMatrix, ladder: ChainGridRow[], straddles: Straddle[], now: Date, rate: number): Map<string, SkewFit> {
  const skews = new Map<string, SkewFit>();
  for (const expiry of matrix.expiries) {
    const straddle = straddles.find(s => s.expiry === expiry.label);
    const future = straddle?.future?.price;
    if (future == null) continue;
    const years = yearsToExpiry(expiry.expiryDate, now);
    const fit = fitSkew(marketVolsOf(expiry, future, years, rate, ladder), future, straddle?.vol);
    if (fit) skews.set(expiry.label, fit);
  }
  return skews;
}

/** The vol to price a strike at, and where it came from: the market's own where streamed, else the skew. */
export function volAt(
  expiry: OptionMatrixExpiry, strike: number, future: number, years: number, rate: number,
  skew: { fit: SkewFit; source: "fit" | "borrowed" } | undefined, ladder: ChainGridRow[],
): { vol: number; source: VolSource } | undefined {
  const market = marketVolAt(expiry, strike, future, years, rate, ladder);
  if (market != null) return { vol: market, source: "market" };
  // A borrowed skew is another expiry's shape around its own future; read it at the same moneyness here.
  return skew ? { vol: volOnSkew(skew.fit, strike * (skew.fit.future / future)), source: skew.source } : undefined;
}

/**
 * Store and matrix → the rows of the matrix: one per strike on a regular grid across the window (half-point
 * steps, as the desk's ladder - the model prices any strike, whether ICE lists it or not), each with a cell per
 * expiry, priced with Black-76 wherever the expiry's future has a price and a vol can be found.
 */
export function buildMatrixRows(matrix: OptionMatrix, ladder: ChainGridRow[], inputs: MatrixInputs): MatrixGridRow[] {
  const window = strikeWindow(matrix, inputs.straddles, ladder);
  if (!window) return [];
  const [low, high] = window;

  const rows = strikeGrid(low, high, inputs.strikeStep).map((strike): MatrixGridRow => ({
    symbol: "matrix",
    tenor: strike.toFixed(2),
    strike,
    leg: inputs.legStrikes.includes(strike),
    stripe: Math.round(strike * 100) % 500 === 0,
    cells: Object.fromEntries(matrix.expiries.map(e => [e.label, { strike }])),
  }));

  const skews = skewsOf(matrix, ladder, inputs.straddles, inputs.now, inputs.rate);
  let lastFit: SkewFit | undefined;

  for (const expiry of matrix.expiries) {
    const future = inputs.straddles.find(s => s.expiry === expiry.label)?.future?.price;
    if (future == null) continue;
    const own = skews.get(expiry.label);
    if (own) lastFit = own;
    const skew = own ? { fit: own, source: "fit" as const } : lastFit ? { fit: lastFit, source: "borrowed" as const } : undefined;
    const years = yearsToExpiry(expiry.expiryDate, inputs.now);

    for (const row of rows) {
      const chosen = volAt(expiry, row.strike, future, years, inputs.rate, skew, ladder);
      if (!chosen) continue;

      const cell = row.cells[expiry.label];
      const call = black76({ right: "C", strike: row.strike, future, years, rate: inputs.rate }, chosen.vol);
      const put = black76({ right: "P", strike: row.strike, future, years, rate: inputs.rate }, chosen.vol);
      cell.call = call.price;
      cell.callDelta = call.delta * 100;
      cell.put = put.price;
      cell.putDelta = put.delta * 100;
      cell.vol = chosen.vol * 100;
      cell.volSource = chosen.source;
    }
  }

  return rows;
}

/** The row nearest the money, for the grid to open on; -1 with no rows or no price. */
export function atmRowIndex(rows: MatrixGridRow[], future: number | undefined): number {
  if (future == null || rows.length === 0) return -1;
  let best = 0;
  for (let i = 1; i < rows.length; i++) {
    if (Math.abs(rows[i].strike - future) < Math.abs(rows[best].strike - future)) best = i;
  }
  return best;
}
