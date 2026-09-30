import { black76, impliedVol, straddleVol, yearsToExpiry } from "@atlas/data";
import type { NatGasQuote, OptionRight, OptionTerms, Screen, Valuation } from "@atlas/data";
import type { ChainGridRow } from "@/application/modules/chain/chain.view-model";

type Quote = Partial<NatGasQuote>;

/**
 * The price an option or a future is taken to be trading at, for pricing off TRADED prices rather than the live
 * screen (the desk's ask, docs/roadmap.md section 12): the last trade first, then the day's settlement, and only
 * then the middle of bid and offer. `basis` says which one was used, so the screen can show it.
 */
export type ReferencePrice = { price: number; basis: "last" | "settle" | "mid" };

export function referencePriceOf(quote: Quote | undefined): ReferencePrice | undefined {
  if (quote?.last != null && quote.last > 0) return { price: quote.last, basis: "last" };
  if (quote?.settle != null && quote.settle > 0) return { price: quote.settle, basis: "settle" };
  if (quote?.bid != null && quote?.ask != null && quote.bid > 0 && quote.ask > 0) {
    return { price: (quote.bid + quote.ask) / 2, basis: "mid" };
  }
  return undefined;
}

/** The strategies the calculator prices, and how many strikes each needs, lowest first. The butterfly is in calls. */
export const Strategies = {
  call: { label: "Call", strikes: 1 },
  put: { label: "Put", strikes: 1 },
  straddle: { label: "Straddle", strikes: 1 },
  callSpread: { label: "Call spread", strikes: 2 },
  putSpread: { label: "Put spread", strikes: 2 },
  butterfly: { label: "Butterfly", strikes: 3 },
} as const;

export type Strategy = keyof typeof Strategies;

/** The strategy written as the desk's calculator writes it: "TFO Z26 60.00/55.00/50.00 Butterfly", strikes high to low. */
export function strategyTitle(root: string, code: string, strikes: number[], strategy: Strategy): string {
  const legs = [...strikes].sort((a, b) => b - a).map(k => k.toFixed(2)).join("/");
  return `${root} ${code} ${legs} ${Strategies[strategy].label}`.replace(/\s+/g, " ").trim();
}

/** One option in a strategy: bought (+) or sold (-) `quantity` times. */
export type Leg = { right: OptionRight; strike: number; quantity: number };

/**
 * The legs of a strategy at the given strikes (lowest first). A butterfly is +1 / -2 / +1 across three strikes;
 * a call spread buys the lower strike and sells the higher; a put spread buys the higher and sells the lower.
 */
export function legsOf(strategy: Strategy, strikes: number[]): Leg[] {
  const [k1, k2, k3] = [...strikes].sort((a, b) => a - b);
  switch (strategy) {
    case "call": return [{ right: "C", strike: k1, quantity: 1 }];
    case "put": return [{ right: "P", strike: k1, quantity: 1 }];
    case "straddle": return [{ right: "C", strike: k1, quantity: 1 }, { right: "P", strike: k1, quantity: 1 }];
    case "callSpread": return [{ right: "C", strike: k1, quantity: 1 }, { right: "C", strike: k2, quantity: -1 }];
    case "putSpread": return [{ right: "P", strike: k2, quantity: 1 }, { right: "P", strike: k1, quantity: -1 }];
    case "butterfly": return [
      { right: "C", strike: k1, quantity: 1 }, { right: "C", strike: k2, quantity: -2 }, { right: "C", strike: k3, quantity: 1 },
    ];
  }
}

/** What the market says about one leg, and what Black-76 makes of it. */
export type LegValuation = Leg & {
  /** The option's traded price and where it came from; undefined when nothing has traded or been quoted. */
  market?: ReferencePrice;
  /** The vol that reproduces the market price; undefined when there is no price or no vol explains it. */
  impliedVol?: number;
  /** The vol the leg was priced at: the override if one was typed, else the implied vol. */
  vol?: number;
  /** Black-76 at `vol`, for one option; undefined without a vol. */
  valuation?: Valuation;
};

export type StrategyValuation = {
  legs: LegValuation[];
  /** The legs' valuations, each times its quantity, added up; undefined if any leg could not be priced. */
  net?: Valuation;
};

export type PricingInputs = {
  /** The future's price the options are priced against (the desk's "RefPr"). */
  future: number;
  /** Time to expiry in years. */
  years: number;
  /** Discount rate; 0 for futures-style (margined) options. */
  rate: number;
  /** A vol to price every leg at instead of each leg's own implied vol; undefined uses the implied vols. */
  volOverride?: number;
};

/** The quote of the call or put at a strike, from the chain's ladder rows. */
function quoteAt(ladder: ChainGridRow[], strike: number, right: OptionRight): Quote | undefined {
  const row = ladder.find(r => r.kind === "strike" && Number(r.tenor) === strike);
  return right === "C" ? row?.call : row?.put;
}

/** Prices a strategy off the chain's traded prices, leg by leg, then nets the legs. */
export function valueStrategy(legs: Leg[], inputs: PricingInputs, ladder: ChainGridRow[]): StrategyValuation {
  const valued = legs.map((leg): LegValuation => {
    const terms: OptionTerms = { right: leg.right, strike: leg.strike, future: inputs.future, years: inputs.years, rate: inputs.rate };
    const market = referencePriceOf(quoteAt(ladder, leg.strike, leg.right));
    const implied = market ? impliedVol(terms, market.price) : undefined;
    const vol = inputs.volOverride ?? implied;
    return { ...leg, market, impliedVol: implied, vol, valuation: vol == null ? undefined : black76(terms, vol) };
  });

  const net = valued.every(l => l.valuation)
    ? valued.reduce<Valuation>((sum, l) => {
        const v = l.valuation!;
        return {
          price: sum.price + l.quantity * v.price,
          delta: sum.delta + l.quantity * v.delta,
          gamma: sum.gamma + l.quantity * v.gamma,
          vega: sum.vega + l.quantity * v.vega,
          theta: sum.theta + l.quantity * v.theta,
        };
      }, { price: 0, delta: 0, gamma: 0, vega: 0, theta: 0 })
    : undefined;

  return { legs: valued, net };
}

/**
 * The strategy's settlement value - the desk's "Sett": each leg's settlement price times its quantity, added up.
 * Undefined if any leg has no settlement.
 */
export function settlementValueOf(legs: Leg[], ladder: ChainGridRow[]): number | undefined {
  let total = 0;
  for (const leg of legs) {
    const settle = quoteAt(ladder, leg.strike, leg.right)?.settle;
    if (settle == null) return undefined;
    total += leg.quantity * settle;
  }
  return total;
}

/** One expiry's at-the-money straddle: the desk's quickest read of where vol is trading. */
export type Straddle = {
  expiry: string;
  expiryDate: string;
  /** The future's traded price the strike was chosen against. */
  future?: ReferencePrice;
  /** The listed strike nearest the future. */
  strike?: number;
  /** Call plus put at that strike, each at its traded price. */
  price?: number;
  /** The one vol that reproduces the straddle's price. */
  vol?: number;
  /** Where the future would have to be at expiry for the straddle to break even: the strike less and plus its price. */
  breakEvens?: [number, number];
};

/** The expiries of a chain, in the screen's order, with the date each one stops trading. */
export function expiriesOf(screen: Screen): { expiry: string; expiryDate: string }[] {
  const seen = new Map<string, string>();
  for (const row of screen.rows) {
    if (row.option && !seen.has(row.group)) seen.set(row.group, row.option.expiryDate);
  }
  return [...seen].map(([expiry, expiryDate]) => ({ expiry, expiryDate }));
}

/** The strikes on the ladder for one expiry, rising. */
export function strikesOf(ladder: ChainGridRow[], expiry: string): number[] {
  return ladder.filter(r => r.kind === "strike" && r.symbol === expiry).map(r => Number(r.tenor));
}

/** The at-the-money straddle of every expiry on the chain, priced off traded prices. */
export function straddlesOf(screen: Screen, ladder: ChainGridRow[], now: Date, rate = 0): Straddle[] {
  return expiriesOf(screen).map(({ expiry, expiryDate }) => {
    const futureRow = ladder.find(r => r.kind === "future" && r.symbol === expiry);
    const future = referencePriceOf(futureRow?.call);
    const strikes = strikesOf(ladder, expiry);
    if (!future || strikes.length === 0) {
      return { expiry, expiryDate, future };
    }

    const strike = strikes.reduce((best, k) => Math.abs(k - future.price) < Math.abs(best - future.price) ? k : best);
    const ofExpiry = ladder.filter(r => r.symbol === expiry);
    const call = referencePriceOf(quoteAt(ofExpiry, strike, "C"));
    const put = referencePriceOf(quoteAt(ofExpiry, strike, "P"));
    if (!call || !put) {
      return { expiry, expiryDate, future, strike };
    }

    const price = call.price + put.price;
    const vol = straddleVol({ future: future.price, strike, years: yearsToExpiry(expiryDate, now), rate }, price);
    return { expiry, expiryDate, future, strike, price, vol, breakEvens: [strike - price, strike + price] };
  });
}
