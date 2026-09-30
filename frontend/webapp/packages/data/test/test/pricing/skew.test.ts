import { describe, it, expect } from "vitest";
import { fitSkew, volOnSkew } from "../../../src/pricing/skew";

const future = 72;
// A known smile: 85% at the money, sloping up to the calls, curving up in both wings.
const known = (strike: number) => { const x = Math.log(strike / future); return 0.85 + 0.3 * x + 1.2 * x * x; };
const points = [66, 68, 70, 72, 74, 76, 78].map(strike => ({ strike, vol: known(strike) }));

describe("fitSkew", () => {
  it("recovers a quadratic smile from points on it", () => {
    const fit = fitSkew(points, future)!;

    expect(fit.a).toBeCloseTo(0.85, 9);
    expect(fit.b).toBeCloseTo(0.3, 9);
    expect(fit.c).toBeCloseTo(1.2, 9);
    expect(volOnSkew(fit, 80)).toBeCloseTo(known(80), 9);
  });

  it("gives a flat vol back flat", () => {
    const fit = fitSkew([66, 72, 78].map(strike => ({ strike, vol: 0.9 })), future)!;

    expect(volOnSkew(fit, 60)).toBeCloseTo(0.9, 9);
    expect(volOnSkew(fit, 90)).toBeCloseTo(0.9, 9);
  });

  it("draws the line through two points", () => {
    const fit = fitSkew([{ strike: 70, vol: 0.8 }, { strike: 74, vol: 0.9 }], future)!;

    expect(volOnSkew(fit, 70)).toBeCloseTo(0.8, 9);
    expect(volOnSkew(fit, 74)).toBeCloseTo(0.9, 9);
    expect(fit.c).toBe(0);
  });

  it("is flat at one point's vol, and flat at the at-the-money vol with no points at all", () => {
    expect(volOnSkew(fitSkew([{ strike: 72, vol: 0.85 }], future)!, 90)).toBeCloseTo(0.85, 9);
    expect(volOnSkew(fitSkew([], future, 0.8)!, 60)).toBeCloseTo(0.8, 9);
  });

  it("has no fit with no points and no at-the-money vol", () => {
    expect(fitSkew([], future)).toBeUndefined();
    expect(fitSkew([], future, 0)).toBeUndefined();
  });

  it("keeps a far wing within half to twice the fitted vols, so the curve cannot run away", () => {
    const fit = fitSkew(points, future)!;
    const lowest = Math.min(...points.map(p => p.vol));
    const highest = Math.max(...points.map(p => p.vol));

    expect(volOnSkew(fit, 300)).toBeLessThanOrEqual(2 * highest);
    expect(volOnSkew(fit, 10)).toBeLessThanOrEqual(2 * highest);
    expect(volOnSkew(fit, 10)).toBeGreaterThanOrEqual(0.5 * lowest);
  });

  it("ignores a point whose vol is not a number, and a repeated strike keeps the last vol", () => {
    const fit = fitSkew([...points, { strike: 71, vol: Number.NaN }, { strike: 72, vol: known(72) }], future)!;

    expect(fit.a).toBeCloseTo(0.85, 9);
  });
});
