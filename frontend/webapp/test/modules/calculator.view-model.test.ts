import { describe, it, expect } from "vitest";
import { black76 } from "@atlas/data";
import type { Screen, ScreenRow } from "@atlas/data";
import type { ChainGridRow } from "@/application/modules/chain/chain.view-model";
import {
  expiriesOf, legsOf, referencePriceOf, settlementValueOf, straddlesOf, strategyTitle, strikesOf, valueStrategy,
} from "@/application/modules/calculator/calculator.view-model";

describe("strategyTitle", () => {
  it("writes the strategy as the desk's calculator does: root, expiry code, strikes high to low, the strategy", () => {
    expect(strategyTitle("TFO", "Z26", [50, 60, 55], "butterfly")).toBe("TFO Z26 60.00/55.00/50.00 Butterfly");
    expect(strategyTitle("EFO", "X26", [87.5], "put")).toBe("EFO X26 87.50 Put");
  });
});

describe("referencePriceOf", () => {
  it("prices off the last trade first, then the settlement, then the mid - never the live screen alone", () => {
    expect(referencePriceOf({ last: 3.6, settle: 3.4, bid: 3.5, ask: 3.7 })).toEqual({ price: 3.6, basis: "last" });
    expect(referencePriceOf({ settle: 3.4, bid: 3.5, ask: 3.7 })).toEqual({ price: 3.4, basis: "settle" });
    expect(referencePriceOf({ bid: 3.5, ask: 3.7 })).toEqual({ price: 3.6, basis: "mid" });
  });

  it("has no answer with one side only, with ICE's zero for no price, or with no quote at all", () => {
    expect(referencePriceOf({ bid: 3.5 })).toBeUndefined();
    expect(referencePriceOf({ last: 0, settle: 0 })).toBeUndefined();
    expect(referencePriceOf(undefined)).toBeUndefined();
  });
});

describe("legsOf", () => {
  it("builds the desk's butterfly as +1 / -2 / +1 across the three strikes, lowest first", () => {
    expect(legsOf("butterfly", [60, 55, 50])).toEqual([
      { right: "C", strike: 50, quantity: 1 },
      { right: "C", strike: 55, quantity: -2 },
      { right: "C", strike: 60, quantity: 1 },
    ]);
  });

  it("buys the lower call and sells the higher in a call spread; buys the higher put and sells the lower in a put spread", () => {
    expect(legsOf("callSpread", [80, 85])).toEqual([{ right: "C", strike: 80, quantity: 1 }, { right: "C", strike: 85, quantity: -1 }]);
    expect(legsOf("putSpread", [80, 85])).toEqual([{ right: "P", strike: 85, quantity: 1 }, { right: "P", strike: 80, quantity: -1 }]);
  });

  it("makes a straddle of a call and a put at one strike, and an outright of one option", () => {
    expect(legsOf("straddle", [80])).toEqual([{ right: "C", strike: 80, quantity: 1 }, { right: "P", strike: 80, quantity: 1 }]);
    expect(legsOf("put", [80])).toEqual([{ right: "P", strike: 80, quantity: 1 }]);
  });
});

// A ladder as buildChainRows makes it, with prices that Black-76 itself produced at known vols.
const terms = (right: "C" | "P", strike: number) => ({ right, strike, future: 72, years: 0.1, rate: 0 });
const priced = (right: "C" | "P", strike: number, vol: number) => ({ last: black76(terms(right, strike), vol).price });

const ladder: ChainGridRow[] = [
  { kind: "future", symbol: "Nov26", tenor: "future", label: "Nov26 options on Nov26 TFM 26X-ICN", call: { last: 72 } },
  { kind: "strike", symbol: "Nov26", tenor: "70.00", label: "70.00", call: priced("C", 70, 0.9), put: priced("P", 70, 0.9) },
  { kind: "strike", symbol: "Nov26", tenor: "72.00", label: "72.00", call: priced("C", 72, 0.85), put: priced("P", 72, 0.85) },
  { kind: "strike", symbol: "Nov26", tenor: "74.00", label: "74.00", call: priced("C", 74, 0.88), put: {} },
];

describe("valueStrategy", () => {
  const inputs = { future: 72, years: 0.1, rate: 0 };

  it("reads each leg's implied vol off its traded price and prices the leg at it", () => {
    const { legs, net } = valueStrategy(legsOf("call", [70]), inputs, ladder);

    expect(legs[0].market).toEqual({ price: ladder[1].call!.last, basis: "last" });
    expect(legs[0].impliedVol).toBeCloseTo(0.9, 6);
    expect(legs[0].vol).toBeCloseTo(0.9, 6);
    expect(legs[0].valuation!.price).toBeCloseTo(ladder[1].call!.last!, 9);
    expect(net!.price).toBeCloseTo(ladder[1].call!.last!, 9);
  });

  it("nets the legs by quantity: a butterfly's theo and Greeks are +1 / -2 / +1 of its legs'", () => {
    const { legs, net } = valueStrategy(legsOf("butterfly", [70, 72, 74]), inputs, ladder);

    const expected = (key: "price" | "delta" | "gamma" | "vega" | "theta") =>
      legs[0].valuation![key] - 2 * legs[1].valuation![key] + legs[2].valuation![key];
    expect(net!.price).toBeCloseTo(expected("price"), 9);
    expect(net!.delta).toBeCloseTo(expected("delta"), 9);
    expect(net!.vega).toBeCloseTo(expected("vega"), 9);
    expect(net!.theta).toBeCloseTo(expected("theta"), 9);
  });

  it("prices every leg at the override vol when one is given, while still showing each leg's implied vol", () => {
    const { legs, net } = valueStrategy(legsOf("straddle", [72]), { ...inputs, volOverride: 0.5 }, ladder);

    expect(legs.map(l => l.vol)).toEqual([0.5, 0.5]);
    expect(legs[0].impliedVol).toBeCloseTo(0.85, 6);
    expect(net!.price).toBeCloseTo(black76(terms("C", 72), 0.5).price + black76(terms("P", 72), 0.5).price, 9);
  });

  it("leaves the net blank when a leg has no price to price off, and says so on the leg", () => {
    const { legs, net } = valueStrategy(legsOf("put", [74]), inputs, ladder);

    expect(legs[0].market).toBeUndefined();
    expect(legs[0].valuation).toBeUndefined();
    expect(net).toBeUndefined();
  });
});

describe("settlementValueOf", () => {
  const settled: ChainGridRow[] = [
    { kind: "strike", symbol: "Nov26", tenor: "70.00", label: "70.00", call: { settle: 4.5 }, put: { settle: 2.5 } },
    { kind: "strike", symbol: "Nov26", tenor: "72.00", label: "72.00", call: { settle: 3.5 }, put: {} },
  ];

  it("adds each leg's settlement times its quantity: the desk's Sett", () => {
    expect(settlementValueOf(legsOf("callSpread", [70, 72]), settled)).toBeCloseTo(4.5 - 3.5, 9);
    expect(settlementValueOf(legsOf("straddle", [70]), settled)).toBeCloseTo(7, 9);
  });

  it("is blank when a leg has no settlement", () => {
    expect(settlementValueOf(legsOf("put", [72]), settled)).toBeUndefined();
  });
});

describe("straddlesOf", () => {
  const option = (expiry: string, expiryDate: string, strike: number, right: "C" | "P"): ScreenRow => ({
    hub: "TTF", label: `${expiry} ${strike.toFixed(2)} ${right}`, group: expiry, source: "quoted", symbol: "s",
    near: null, far: null, option: { expiryDate, underlyingSymbol: "u", strike, right },
  });
  const screen: Screen = {
    key: "xcom-ttf", title: "TTF Options",
    rows: [
      { hub: "TTF", label: "Nov26", group: "Nov26", source: "quoted", symbol: "u", near: null, far: null, option: null },
      option("Nov26", "2026-10-27", 70, "C"), option("Nov26", "2026-10-27", 70, "P"),
      option("Nov26", "2026-10-27", 72, "C"), option("Nov26", "2026-10-27", 72, "P"),
      option("Dec26", "2026-11-26", 72, "C"),
    ],
  };

  it("lists the chain's expiries in order with their last trading days, and the strikes of each", () => {
    expect(expiriesOf(screen)).toEqual([{ expiry: "Nov26", expiryDate: "2026-10-27" }, { expiry: "Dec26", expiryDate: "2026-11-26" }]);
    expect(strikesOf(ladder, "Nov26")).toEqual([70, 72, 74]);
  });

  it("prices each expiry's at-the-money straddle off traded prices and reads one vol off it", () => {
    const now = new Date("2026-09-25T10:00:00Z");

    const [nov26] = straddlesOf(screen, ladder, now);

    expect(nov26.strike).toBe(72);
    expect(nov26.future).toEqual({ price: 72, basis: "last" });
    const expected = ladder[2].call!.last! + ladder[2].put!.last!;
    expect(nov26.price).toBeCloseTo(expected, 9);
    expect(nov26.breakEvens![0]).toBeCloseTo(72 - expected, 9);
    expect(nov26.breakEvens![1]).toBeCloseTo(72 + expected, 9);
    // The ladder's prices were made at 0.1 years; the straddle is priced at the real time to 27 Oct, so its vol
    // differs - but it must be a real number in the same region.
    expect(nov26.vol).toBeGreaterThan(0.5);
    expect(nov26.vol).toBeLessThan(1.5);
  });

  it("gives an expiry without a priced straddle its future and strike only", () => {
    const [, dec26] = straddlesOf(screen, ladder, new Date("2026-09-25T10:00:00Z"));

    expect(dec26).toEqual({ expiry: "Dec26", expiryDate: "2026-11-26", future: undefined });
  });
});
