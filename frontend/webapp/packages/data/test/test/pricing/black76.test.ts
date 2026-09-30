import { describe, it, expect } from "vitest";
import {
  black76, impliedVol, intrinsic, normalCdf, normalPdf, straddleVol, yearsToExpiry,
} from "../../../src/pricing/black76";
import type { OptionTerms } from "../../../src/pricing/black76";

// A round, textbook case: at the money, one year, 20% vol, no rate. Black-76 gives the call and the put the same
// value, F × (2Φ(σ√T/2) − 1) = 100 × (2 × 0.539828 − 1) = 7.9656.
const atTheMoney: OptionTerms = { right: "C", future: 100, strike: 100, years: 1 };

describe("normalCdf", () => {
  it("matches the standard normal table to 6 decimals", () => {
    expect(normalCdf(0)).toBeCloseTo(0.5, 7);
    expect(normalCdf(1.96)).toBeCloseTo(0.9750021, 6);
    expect(normalCdf(-1)).toBeCloseTo(0.1586553, 6);
    expect(normalCdf(3)).toBeCloseTo(0.9986501, 6);
    expect(normalPdf(0)).toBeCloseTo(0.3989423, 7);
  });
});

describe("black76 price", () => {
  it("prices the textbook at-the-money call and put at 7.9656", () => {
    expect(black76(atTheMoney, 0.2).price).toBeCloseTo(7.9656, 4);
    expect(black76({ ...atTheMoney, right: "P" }, 0.2).price).toBeCloseTo(7.9656, 4);
  });

  it("keeps put-call parity: call minus put is the discounted distance of the future from the strike", () => {
    const terms: OptionTerms = { right: "C", future: 71.535, strike: 60, years: 0.17, rate: 0.04 };

    const call = black76(terms, 0.85).price;
    const put = black76({ ...terms, right: "P" }, 0.85).price;

    expect(call - put).toBeCloseTo(Math.exp(-0.04 * 0.17) * (71.535 - 60), 9);
  });

  it("is worth about its intrinsic value deep in the money and about nothing far out", () => {
    expect(black76({ right: "C", future: 100, strike: 20, years: 0.25 }, 0.2).price).toBeCloseTo(80, 6);
    expect(black76({ right: "P", future: 100, strike: 300, years: 0.25 }, 0.2).price).toBeCloseTo(200, 6);
    expect(black76({ right: "C", future: 100, strike: 300, years: 0.25 }, 0.2).price).toBeCloseTo(0, 6);
  });

  it("is worth exactly its intrinsic value at expiry, with no Greeks left but a one-or-nothing delta", () => {
    expect(black76({ right: "C", future: 105, strike: 100, years: 0 }, 0.5))
      .toEqual({ price: 5, delta: 1, gamma: 0, vega: 0, theta: 0 });
    expect(black76({ right: "P", future: 105, strike: 100, years: 0 }, 0.5))
      .toEqual({ price: 0, delta: 0, gamma: 0, vega: 0, theta: 0 });
  });

  it("discounts a paid-up-front option by the rate, and not a futures-style one", () => {
    const margined = black76(atTheMoney, 0.2).price;
    const paidUpFront = black76({ ...atTheMoney, rate: 0.05 }, 0.2).price;

    expect(paidUpFront).toBeCloseTo(margined * Math.exp(-0.05), 9);
  });
});

describe("black76 Greeks", () => {
  // Each Greek is a derivative of the price. Nudge the input a little either way and the price's slope should agree.
  const terms: OptionTerms = { right: "C", future: 72, strike: 80, years: 0.09, rate: 0 };
  const vol = 0.85;
  const h = 0.001;

  // The tolerances allow for the normal-distribution approximation (error under 1e-7 in Φ), which a finite
  // difference divides by a small step and so magnifies.
  it("delta is the price's slope against the future", () => {
    const slope = (black76({ ...terms, future: 72 + h }, vol).price - black76({ ...terms, future: 72 - h }, vol).price) / (2 * h);

    expect(black76(terms, vol).delta).toBeCloseTo(slope, 5);
  });

  it("gamma is delta's slope against the future", () => {
    const slope = (black76({ ...terms, future: 72 + h }, vol).delta - black76({ ...terms, future: 72 - h }, vol).delta) / (2 * h);

    expect(black76(terms, vol).gamma).toBeCloseTo(slope, 6);
  });

  it("vega is the price's slope against one point of vol", () => {
    const slope = (black76(terms, vol + 0.01).price - black76(terms, vol - 0.01).price) / 2;

    expect(black76(terms, vol).vega).toBeCloseTo(slope, 4);
  });

  it("theta is the price's slope against time, per calendar day, and negative", () => {
    const step = 0.001;
    const slopePerYear = (black76({ ...terms, years: terms.years - step }, vol).price - black76({ ...terms, years: terms.years + step }, vol).price) / (2 * step);

    expect(black76(terms, vol).theta).toBeCloseTo(slopePerYear / 365, 4);
    expect(black76(terms, vol).theta).toBeLessThan(0);
  });

  it("a put's delta is the call's less one, at the same strike", () => {
    expect(black76({ ...terms, right: "P" }, vol).delta).toBeCloseTo(black76(terms, vol).delta - 1, 9);
  });
});

describe("impliedVol", () => {
  it("finds the vol that reproduces a price, to well within a basis point", () => {
    const terms: OptionTerms = { right: "P", future: 71.535, strike: 65, years: 0.17 };
    const price = black76(terms, 0.9123).price;

    expect(impliedVol(terms, price)).toBeCloseTo(0.9123, 6);
  });

  it("has no answer for a price below intrinsic value, at expiry, or for no price at all", () => {
    expect(impliedVol({ right: "C", future: 100, strike: 90, years: 0.5 }, 9.5)).toBeUndefined();
    expect(impliedVol({ right: "C", future: 100, strike: 90, years: 0 }, 12)).toBeUndefined();
    expect(impliedVol({ right: "C", future: 100, strike: 90, years: 0.5 }, 0)).toBeUndefined();
  });

  it("reads a straddle's one vol off the price of the call and put together", () => {
    const terms = { future: 71.535, strike: 71.5, years: 0.17 };
    const price = black76({ ...terms, right: "C" }, 0.88).price + black76({ ...terms, right: "P" }, 0.88).price;

    expect(straddleVol(terms, price)).toBeCloseTo(0.88, 6);
  });
});

describe("intrinsic", () => {
  it("is what exercising would be worth, never negative", () => {
    expect(intrinsic({ right: "C", future: 72, strike: 60 })).toBe(12);
    expect(intrinsic({ right: "P", future: 72, strike: 60 })).toBe(0);
    expect(intrinsic({ right: "P", future: 72, strike: 80 })).toBe(8);
  });
});

describe("yearsToExpiry", () => {
  it("counts to 14:00 Amsterdam on the last trading day, in years of 365 days, and never below zero", () => {
    const now = new Date("2026-09-25T10:00:00Z");

    // 27 Oct 2026 is winter time: 14:00 in Amsterdam is 13:00 UTC, 32 days and 3 hours away.
    expect(yearsToExpiry("2026-10-27", now)).toBeCloseTo((32 + 3 / 24) / 365, 9);
    // 25 Sep is summer time: 14:00 in Amsterdam is 12:00 UTC, two hours away.
    expect(yearsToExpiry("2026-09-25", now)).toBeCloseTo((2 / 24) / 365, 9);
    expect(yearsToExpiry("2026-09-24", now)).toBe(0);
  });
});
