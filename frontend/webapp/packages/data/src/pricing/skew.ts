// A vol skew: how implied volatility varies with the strike at one expiry. The market rarely prices every
// strike at one vol - for TTF the out-of-the-money calls trade at a higher vol than the puts (the market pays up
// for upside) - and the desk's calculator shows a vol per strike. Atlas streams quotes only for a band of strikes
// around the money, so this fits a curve through the band's implied vols and reads it off for every other strike.
//
// The curve is a quadratic in log-moneyness, x = ln(K / F): the simplest shape with a level, a slope and a smile.
// A quadratic runs away in the far wings, so the answer is clamped to a band around the vols it was fitted to.
// Pure arithmetic, no dependency.

export interface SkewPoint {
  strike: number;
  /** As a fraction: 0.85 for 85%. */
  vol: number;
}

export interface SkewFit {
  future: number;
  /** vol(x) = a + b·x + c·x², x = ln(strike / future). */
  a: number;
  b: number;
  c: number;
  /** The vols the fit may answer: never below `low`, never above `high`. */
  low: number;
  high: number;
}

/**
 * Fits the skew through the band's points. Three or more distinct strikes give the least-squares quadratic; two
 * give the line through them; one gives that vol flat; none gives `atmVol` flat, and without that there is no fit.
 * A point with a non-finite vol is ignored.
 */
export function fitSkew(points: SkewPoint[], future: number, atmVol?: number): SkewFit | undefined {
  const byStrike = new Map<number, number>();
  for (const p of points) {
    if (Number.isFinite(p.vol) && p.vol > 0 && Number.isFinite(p.strike) && p.strike > 0) byStrike.set(p.strike, p.vol);
  }
  const xs = [...byStrike].map(([strike, vol]) => ({ x: Math.log(strike / future), vol }));

  if (xs.length === 0) {
    if (atmVol == null || !(atmVol > 0)) return undefined;
    return { future, a: atmVol, b: 0, c: 0, ...clamp([atmVol]) };
  }
  const vols = xs.map(p => p.vol);
  if (xs.length === 1) {
    return { future, a: vols[0], b: 0, c: 0, ...clamp(vols) };
  }
  if (xs.length === 2) {
    const [p, q] = xs;
    const b = (q.vol - p.vol) / (q.x - p.x);
    return { future, a: p.vol - b * p.x, b, c: 0, ...clamp(vols) };
  }

  // Least squares: the normal equations of vol ≈ a + b·x + c·x², solved for (a, b, c).
  let s1 = 0, s2 = 0, s3 = 0, s4 = 0, t0 = 0, t1 = 0, t2 = 0;
  for (const { x, vol } of xs) {
    s1 += x; s2 += x * x; s3 += x * x * x; s4 += x * x * x * x;
    t0 += vol; t1 += x * vol; t2 += x * x * vol;
  }
  const [a, b, c] = solve3([
    [xs.length, s1, s2, t0],
    [s1, s2, s3, t1],
    [s2, s3, s4, t2],
  ]);
  return { future, a, b, c, ...clamp(vols) };
}

/** The fitted vol at a strike, kept within the fit's band. */
export function volOnSkew(fit: SkewFit, strike: number): number {
  const x = Math.log(strike / fit.future);
  const vol = fit.a + fit.b * x + fit.c * x * x;
  return Math.min(fit.high, Math.max(fit.low, vol));
}

/** Half the lowest fitted vol to twice the highest, and never below 1%: room to move, no nonsense in the wings. */
function clamp(vols: number[]): { low: number; high: number } {
  return { low: Math.max(0.01, 0.5 * Math.min(...vols)), high: 2 * Math.max(...vols) };
}

/** Gaussian elimination with partial pivoting on an augmented 3×4 matrix. */
function solve3(m: number[][]): [number, number, number] {
  for (let col = 0; col < 3; col++) {
    let pivot = col;
    for (let row = col + 1; row < 3; row++) {
      if (Math.abs(m[row][col]) > Math.abs(m[pivot][col])) pivot = row;
    }
    [m[col], m[pivot]] = [m[pivot], m[col]];
    for (let row = col + 1; row < 3; row++) {
      const factor = m[row][col] / m[col][col];
      for (let k = col; k < 4; k++) m[row][k] -= factor * m[col][k];
    }
  }
  const out = [0, 0, 0];
  for (let row = 2; row >= 0; row--) {
    let sum = m[row][3];
    for (let k = row + 1; k < 3; k++) sum -= m[row][k] * out[k];
    out[row] = sum / m[row][row];
  }
  return [out[0], out[1], out[2]];
}
