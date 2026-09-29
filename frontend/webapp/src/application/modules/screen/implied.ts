import type { NatGasQuote } from "@atlas/data";

type Quote = Partial<NatGasQuote>;
type Strips = Record<string, Quote | undefined>;

export type ImpliedQuote = { impliedBid?: number; impliedAsk?: number };

/**
 * The bid and offer the spread markets imply for one outright strip - WebICE's implied block.
 *
 * A calendar spread is priced near minus far, so for an outright X:
 *   as the FAR leg of N/X:  sell X = sell N and buy the spread  → implied bid   = bid(N)   - offer(N/X);
 *                           buy X  = buy N and sell the spread  → implied offer = offer(N) - bid(N/X).
 *   as the NEAR leg of X/F: sell X = sell F and sell the spread → implied bid   = bid(F)   + bid(X/F);
 *                           buy X  = buy F and buy the spread   → implied offer = offer(F) + offer(X/F).
 * Every listed spread with X as a leg is a candidate, and the best wins on each side: the highest bid, the
 * lowest offer. Checked against WebICE's Coal tab on 24 Sep 2026: Feb26 94.55/96.95 and Feb26/Mar26 -1.45/1.50
 * give Mar26 an implied bid of 93.05, as WebICE showed. ICE sends no implied prices, only the sizes (581/582).
 *
 * `spreadLabels` are the spread rows of the strip's own hub ("Feb26/Mar26"); `strips` is that hub's quotes,
 * outrights and spreads alike, as the store keeps them. Results are rounded to the screen's decimals.
 */
export function impliedQuote(label: string, spreadLabels: readonly string[], strips: Strips, decimals: number): ImpliedQuote {
  const bids: number[] = [];
  const asks: number[] = [];

  for (const spreadLabel of spreadLabels) {
    const legs = spreadLabel.split("/");
    const spread = strips[spreadLabel];
    if (legs.length !== 2 || !spread) {
      continue;
    }
    const [near, far] = legs;

    if (far === label) {
      const nearLeg = strips[near];
      if (nearLeg?.bid != null && spread.ask != null) {
        bids.push(nearLeg.bid - spread.ask);
      }
      if (nearLeg?.ask != null && spread.bid != null) {
        asks.push(nearLeg.ask - spread.bid);
      }
    } else if (near === label) {
      const farLeg = strips[far];
      if (farLeg?.bid != null && spread.bid != null) {
        bids.push(farLeg.bid + spread.bid);
      }
      if (farLeg?.ask != null && spread.ask != null) {
        asks.push(farLeg.ask + spread.ask);
      }
    }
  }

  const tidy = (n: number) => Number(n.toFixed(decimals));
  const quote: ImpliedQuote = {};
  if (bids.length > 0) {
    quote.impliedBid = tidy(Math.max(...bids));
  }
  if (asks.length > 0) {
    quote.impliedAsk = tidy(Math.min(...asks));
  }
  return quote;
}
