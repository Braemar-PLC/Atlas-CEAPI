import { describe, it, expect } from "vitest";
import { black76, fitSkew, volOnSkew, yearsToExpiry } from "@atlas/data";
import type { OptionMatrix } from "@atlas/data";
import type { ChainGridRow } from "@/application/modules/chain/chain.view-model";
import type { Straddle } from "@/application/modules/calculator/calculator.view-model";
import {
  atmRowIndex, buildMatrixRows, marketVolAt, marketVolsOf, matrixRowClassRules, skewsOf, strikeGrid, strikeWindow, volAt,
} from "@/application/modules/calculator/matrix.view-model";

const now = new Date("2026-09-25T10:00:00Z");

const matrix: OptionMatrix = {
  product: "TTF",
  expiries: [
    { kind: "Month", label: "Nov26", expiryDate: "2026-10-27", underlyingSymbol: "TFM 26X-ICN", strikes: [10, 40, 70, 72, 74, 80, 200] },
    { kind: "Month", label: "Dec26", expiryDate: "2026-11-26", underlyingSymbol: "TFM 26Z-ICN", strikes: [70, 72, 80] },
    { kind: "Quarter", label: "Q1 27", expiryDate: "2026-12-24", underlyingSymbol: "TFMQ 27F-ICN", strikes: [70, 75, 80] },
  ],
};

// The chain streams a band around the money for Nov26 only: 70, 72 and 74, priced by Black-76 on a known smile,
// at the time to expiry the matrix will work out for 27 Oct. Dec26 has a future but no option quotes.
const years = yearsToExpiry("2026-10-27", now);
const smile = (strike: number) => { const x = Math.log(strike / 72); return 0.85 + 0.3 * x + 1.2 * x * x; };
const terms = (right: "C" | "P", strike: number) => ({ right, strike, future: 72, years, rate: 0 });
const priced = (right: "C" | "P", strike: number) => ({ last: black76(terms(right, strike), smile(strike)).price });
const ladder: ChainGridRow[] = [
  { kind: "future", symbol: "Nov26", tenor: "future", label: "Nov26 options on Nov26 TFM 26X-ICN", call: { last: 72 } },
  { kind: "strike", symbol: "Nov26", tenor: "70.00", label: "70.00", call: priced("C", 70), put: priced("P", 70) },
  { kind: "strike", symbol: "Nov26", tenor: "72.00", label: "72.00", call: priced("C", 72), put: priced("P", 72) },
  { kind: "strike", symbol: "Nov26", tenor: "74.00", label: "74.00", call: priced("C", 74), put: {} },
  { kind: "future", symbol: "Dec26", tenor: "future", label: "Dec26 options on Dec26 TFM 26Z-ICN", call: { last: 73 } },
];

const straddles: Straddle[] = [
  { expiry: "Nov26", expiryDate: "2026-10-27", future: { price: 72, basis: "last" }, strike: 72, price: 10, vol: 0.85 },
  { expiry: "Dec26", expiryDate: "2026-11-26", future: { price: 73, basis: "last" } },
  { expiry: "Q1 27", expiryDate: "2026-12-24", future: undefined },
];

const inputs = { straddles, now, rate: 0, legStrikes: [74], strikeStep: 0.5 };

describe("strikeWindow", () => {
  it("runs from half to one-and-a-half times the front expiry's future", () => {
    expect(strikeWindow(matrix, straddles, ladder)).toEqual([36, 108]);
  });

  it("falls back to the strikes the chain screen chose while the future has no price", () => {
    expect(strikeWindow(matrix, [{ expiry: "Nov26", expiryDate: "2026-10-27", future: undefined }], ladder)).toEqual([70, 74]);
  });
});

describe("strikeGrid", () => {
  it("steps through the window at the step, on the whole numbers, without floating-point drift", () => {
    expect(strikeGrid(46.2, 48, 0.5)).toEqual([46.5, 47, 47.5, 48]);
    expect(strikeGrid(36, 37.6, 0.5)).toEqual([36, 36.5, 37, 37.5]);
    expect(strikeGrid(0.1, 1.5, 0.5)).toEqual([0.5, 1, 1.5]);
  });
});

describe("marketVolsOf and skewsOf", () => {
  it("reads the band's vols off the out-of-the-money side and fits the expiry's skew through them", () => {
    const nov26 = matrix.expiries[0];

    const points = marketVolsOf(nov26, 72, years, 0, ladder);
    expect(points.map(p => p.strike)).toEqual([70, 72, 74]);
    expect(points[0].vol).toBeCloseTo(smile(70), 6);

    const skews = skewsOf(matrix, ladder, straddles, now, 0);
    expect([...skews.keys()]).toEqual(["Nov26"]);
    expect(volOnSkew(skews.get("Nov26")!, 80)).toBeCloseTo(smile(80), 6);
  });
});

describe("buildMatrixRows", () => {
  const rows = buildMatrixRows(matrix, ladder, inputs);

  it("has one row per half-point strike across the window, whether ICE lists it or not, with a cell for every expiry", () => {
    expect(rows[0].tenor).toBe("36.00");
    expect(rows[1].tenor).toBe("36.50");
    expect(rows[rows.length - 1].tenor).toBe("108.00");
    expect(rows).toHaveLength((108 - 36) * 2 + 1);
    expect(rows.find(r => r.strike === 75.5)!.cells["Q1 27"]).toEqual({ strike: 75.5 });
    expect(Object.keys(rows[0].cells)).toEqual(["Nov26", "Dec26", "Q1 27"]);
  });

  it("flags every multiple of 5 as a guide row, as the desk's blue rows", () => {
    expect(rows.filter(r => r.stripe).map(r => r.strike)).toEqual([40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105]);
    const rule = matrixRowClassRules["ice-matrix-stripe"] as (p: { data?: { stripe: boolean } }) => boolean;
    expect(rule({ data: { stripe: true } })).toBe(true);
    expect(rule({ data: { stripe: false } })).toBe(false);
  });

  it("prices a strike ICE does not list, at the fitted vol", () => {
    const cell = rows.find(r => r.strike === 71.5)!.cells["Nov26"];

    expect(cell.volSource).toBe("fit");
    expect(cell.call).toBeCloseTo(black76(terms("C", 71.5), smile(71.5)).price, 4);
  });

  it("prices a strike the chain streams at its own market vol", () => {
    const cell = rows.find(r => r.strike === 74)!.cells["Nov26"];

    expect(cell.volSource).toBe("market");
    expect(cell.vol).toBeCloseTo(smile(74) * 100, 4);
    expect(cell.call).toBeCloseTo(black76(terms("C", 74), smile(74)).price, 4);
    expect(cell.callDelta).toBeCloseTo(black76(terms("C", 74), smile(74)).delta * 100, 4);
    expect(cell.putDelta).toBeLessThan(0);
  });

  it("prices a strike outside the band at the vol the fitted skew gives there", () => {
    const cell = rows.find(r => r.strike === 80)!.cells["Nov26"];

    expect(cell.volSource).toBe("fit");
    expect(cell.vol).toBeCloseTo(smile(80) * 100, 4);
    expect(cell.call).toBeCloseTo(black76(terms("C", 80), smile(80)).price, 4);
  });

  it("borrows the nearest earlier expiry's skew for an expiry that streams no quotes, read at the same moneyness", () => {
    const cell = rows.find(r => r.strike === 80)!.cells["Dec26"];
    const nov26 = fitSkew([70, 72, 74].map(strike => ({ strike, vol: smile(strike) })), 72)!;

    expect(cell.volSource).toBe("borrowed");
    expect(cell.vol).toBeCloseTo(volOnSkew(nov26, 80 * (72 / 73)) * 100, 4);
    expect(cell.call).toBeGreaterThan(0);
  });

  it("leaves an expiry unpriced while its future has no price", () => {
    expect(rows.find(r => r.strike === 80)!.cells["Q1 27"]).toEqual({ strike: 80 });
  });

  it("flags the strategy's strikes so the grid can pick their rows out", () => {
    expect(rows.filter(r => r.leg).map(r => r.strike)).toEqual([74]);
    const rule = matrixRowClassRules["ice-matrix-leg"] as (p: { data?: { leg: boolean } }) => boolean;
    expect(rule({ data: { leg: true } })).toBe(true);
    expect(rule({})).toBe(false);
  });

  it("is empty until the matrix has an expiry", () => {
    expect(buildMatrixRows({ product: "TTF", expiries: [] }, ladder, inputs)).toEqual([]);
  });
});

describe("the strategy's strikes", () => {
  it("are flagged apart from the guide rows", () => {
    const rows = buildMatrixRows(matrix, ladder, { ...inputs, legStrikes: [74, 75] });

    expect(rows.filter(r => r.leg).map(r => r.strike)).toEqual([74, 75]);
    expect(rows.find(r => r.strike === 75)!.stripe).toBe(true);
    expect(rows.find(r => r.strike === 74)!.stripe).toBe(false);
  });
});

describe("volAt", () => {
  const nov26 = matrix.expiries[0];
  const fit = fitSkew([70, 72, 74].map(strike => ({ strike, vol: smile(strike) })), 72)!;

  it("prefers the market's vol where the strike is streamed, and the skew elsewhere", () => {
    expect(volAt(nov26, 74, 72, years, 0, { fit, source: "fit" }, ladder)!.source).toBe("market");
    expect(volAt(nov26, 80, 72, years, 0, { fit, source: "fit" }, ladder)).toEqual({ vol: volOnSkew(fit, 80), source: "fit" });
  });

  it("has no vol at all with neither a quote nor a skew", () => {
    expect(volAt(nov26, 80, 72, years, 0, undefined, ladder)).toBeUndefined();
  });
});

describe("atmRowIndex", () => {
  it("is the row nearest the money, or -1 with nothing to go on", () => {
    const rows = buildMatrixRows(matrix, ladder, inputs);

    expect(rows[atmRowIndex(rows, 72.4)].strike).toBe(72.5);
    expect(rows[atmRowIndex(rows, 76.2)].strike).toBe(76);
    expect(atmRowIndex(rows, undefined)).toBe(-1);
    expect(atmRowIndex([], 72)).toBe(-1);
  });
});

describe("the ladder's quotes, looked up per cell", () => {
  it("follows the ladder: a new ladder with a changed quote gives the changed vol, the old ladder still its own", () => {
    const nov26 = matrix.expiries[0];
    const before = marketVolAt(nov26, 70, 72, years, 0, ladder);
    expect(before).toBeCloseTo(smile(70), 6);

    const cheaper: ChainGridRow[] = ladder.map(r =>
      r.symbol === "Nov26" && r.tenor === "70.00" ? { ...r, put: { last: r.put!.last! * 0.5 } } : r);
    const after = marketVolAt(nov26, 70, 72, years, 0, cheaper);

    expect(after).toBeDefined();
    expect(after!).toBeLessThan(before!);
    expect(marketVolAt(nov26, 70, 72, years, 0, ladder)).toBeCloseTo(smile(70), 6);
  });
});
