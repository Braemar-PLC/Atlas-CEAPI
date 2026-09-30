// Black-76: the price of a European option on a future, and how that price moves (the "Greeks").
//
// Black-76 is the Black-Scholes formula written for a future instead of a share: there is no dividend and no
// cost of carry, because a future costs nothing to hold. It is ICE's own convention for these options (ICE names
// its vol field OPTION_BLACK_76_VOL), and the convention behind the desk's calculator. Inputs: the future's price
// F, the strike K, the time to expiry T in years, the volatility σ as a fraction (0.85 for 85%), and a rate r.
// TTF and EUA options are "futures-style" - the premium is margined, not paid up front - so r is 0 for them;
// Brent and WTI options are paid up front and r discounts them. Nothing here reads a clock or a feed: same
// inputs, same answers, so every function is a pure one with a test.

export type OptionRight = "C" | "P";

export interface OptionTerms {
  right: OptionRight;
  /** The underlying future's price. */
  future: number;
  strike: number;
  /** Time to expiry in years; see yearsToExpiry. */
  years: number;
  /** Discount rate as a fraction; 0 for a futures-style (margined) option. */
  rate?: number;
}

/** What Black-76 says about one option at one volatility. */
export interface Valuation {
  price: number;
  /** Change in price per 1.00 move in the future. A call's is 0 to 1, a put's -1 to 0. */
  delta: number;
  /** Change in delta per 1.00 move in the future. */
  gamma: number;
  /** Change in price per 1 point of volatility (85% to 86%). */
  vega: number;
  /** Change in price per calendar day passing, all else equal. Negative for a bought option: time decay. */
  theta: number;
}

/** The standard normal density. */
export function normalPdf(x: number): number {
  return Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
}

/**
 * The standard normal cumulative distribution, Φ(x). JavaScript has no error function, so this is Abramowitz and
 * Stegun's approximation 26.2.17: its error is below 7.5e-8, which moves a price by far less than a tick.
 */
export function normalCdf(x: number): number {
  const t = 1 / (1 + 0.2316419 * Math.abs(x));
  const poly = t * (0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
  const upperTail = normalPdf(x) * poly;
  return x >= 0 ? 1 - upperTail : upperTail;
}

/** The option's worth if it expired now: what exercising it would be worth, never less than nothing. */
export function intrinsic({ right, future, strike }: Pick<OptionTerms, "right" | "future" | "strike">): number {
  return Math.max(0, right === "C" ? future - strike : strike - future);
}

/** Black-76 at the given volatility (a fraction: 0.85 for 85%). */
export function black76(terms: OptionTerms, vol: number): Valuation {
  const { right, future: F, strike: K, years: T, rate = 0 } = terms;
  const discount = Math.exp(-rate * T);

  if (T <= 0 || vol <= 0) {
    // At expiry, or with no volatility at all, the option is worth exactly its intrinsic value, moves one-for-one
    // with the future when in the money and not at all when out, and has nothing left to decay.
    const inTheMoney = right === "C" ? F > K : F < K;
    const delta = inTheMoney ? (right === "C" ? 1 : -1) : 0;
    return { price: discount * intrinsic(terms), delta: discount * delta, gamma: 0, vega: 0, theta: 0 };
  }

  const sqrtT = Math.sqrt(T);
  const d1 = (Math.log(F / K) + 0.5 * vol * vol * T) / (vol * sqrtT);
  const d2 = d1 - vol * sqrtT;
  const density = normalPdf(d1);

  const price = right === "C"
    ? discount * (F * normalCdf(d1) - K * normalCdf(d2))
    : discount * (K * normalCdf(-d2) - F * normalCdf(-d1));
  const delta = discount * (right === "C" ? normalCdf(d1) : normalCdf(d1) - 1);
  const gamma = (discount * density) / (F * vol * sqrtT);
  const vega = (discount * F * density * sqrtT) / 100;
  // Per year: the rate earns on the premium, time decay costs. Then per calendar day.
  const thetaPerYear = rate * price - (discount * F * density * vol) / (2 * sqrtT);

  return { price, delta, gamma, vega, theta: thetaPerYear / 365 };
}

/**
 * The volatility at which Black-76 gives `price`: the "implied vol" - the market's own forecast, read off the
 * price it is trading at. Undefined when no volatility does: at or after expiry, or a price below what the option
 * is worth if exercised now (a price no model can explain, usually a stale quote). Found by bisection between 0.01%
 * and 1,000%, which always converges because a higher vol always means a higher price; 60 halvings leave an error
 * far below a basis point.
 */
export function impliedVol(terms: OptionTerms, price: number): number | undefined {
  return solveVol(price, vol => black76(terms, vol).price, terms);
}

/**
 * One volatility for a straddle - a call and a put at the same strike, bought together - from the straddle's
 * price. The desk quotes vol this way: at the money, a straddle's price is almost purely volatility.
 */
export function straddleVol(terms: Omit<OptionTerms, "right">, price: number): number | undefined {
  const call = { ...terms, right: "C" as const };
  const put = { ...terms, right: "P" as const };
  return solveVol(price, vol => black76(call, vol).price + black76(put, vol).price, call, put);
}

function solveVol(price: number, priceAt: (vol: number) => number, ...legs: OptionTerms[]): number | undefined {
  if (legs[0].years <= 0 || !(price > 0)) {
    return undefined;
  }
  const discount = Math.exp(-(legs[0].rate ?? 0) * legs[0].years);
  const floor = discount * legs.reduce((sum, leg) => sum + intrinsic(leg), 0);
  if (price < floor) {
    return undefined;
  }

  let low = 0.0001;
  let high = 10;
  if (priceAt(high) < price) {
    return undefined;
  }
  for (let i = 0; i < 60; i++) {
    const mid = (low + high) / 2;
    if (priceAt(mid) > price) {
      high = mid;
    } else {
      low = mid;
    }
  }
  return (low + high) / 2;
}

/**
 * Time to expiry in years, from "now" to 14:00 Amsterdam on the option's last trading day - the moment ICE's
 * options stop trading and roll (the same cut-off the API's screens use). Never negative: an option past its
 * last day has no time left.
 */
export function yearsToExpiry(expiryDate: string, now: Date): number {
  const close = new Date(`${expiryDate}T14:00:00Z`).getTime() - amsterdamOffsetHours(expiryDate) * 60 * 60 * 1000;
  const msPerYear = 365 * 24 * 60 * 60 * 1000;
  return Math.max(0, (close - now.getTime()) / msPerYear);
}

/** Amsterdam's hours ahead of UTC on a date: 2 in summer time, 1 in winter, read from the browser's own zone tables. */
function amsterdamOffsetHours(date: string): number {
  const noonUtc = new Date(`${date}T12:00:00Z`);
  const localHour = Number(new Intl.DateTimeFormat("en-GB", { timeZone: "Europe/Amsterdam", hour: "2-digit", hour12: false }).format(noonUtc));
  return localHour - 12;
}
