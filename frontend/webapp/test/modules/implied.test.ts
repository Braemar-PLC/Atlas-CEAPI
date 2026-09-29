import { describe, it, expect } from "vitest";
import { impliedQuote } from "@/application/modules/screen/implied";

// WebICE's Coal tab, 24 Sep 2026 (Rotterdam): Feb26 94.55/96.95, Mar26 93.05/96.00 and the Feb26/Mar26 spread
// -1.45/1.50. WebICE showed Mar26's implied bid as 93.05.
const strips = {
  "Feb26": { bid: 94.55, ask: 96.95 },
  "Mar26": { bid: 93.05, ask: 96.0 },
  "Feb26/Mar26": { bid: -1.45, ask: 1.5 },
};
const spreads = ["Feb26/Mar26"];

describe("impliedQuote", () => {
  it("implies the far leg from the near leg: bid = near bid - spread offer, offer = near offer - spread bid", () => {
    expect(impliedQuote("Mar26", spreads, strips, 2)).toEqual({ impliedBid: 93.05, impliedAsk: 98.4 });
  });

  it("implies the near leg from the far leg: bid = far bid + spread bid, offer = far offer + spread offer", () => {
    expect(impliedQuote("Feb26", spreads, strips, 2)).toEqual({ impliedBid: 91.6, impliedAsk: 97.5 });
  });

  it("takes the best of several candidates: the highest bid and the lowest offer", () => {
    const many = {
      ...strips,
      "Jan26": { bid: 96.0, ask: 97.0 },
      "Jan26/Feb26": { bid: 1.0, ask: 1.2 }, // Feb26 as the far leg: 96.00 - 1.20 = 94.80 bid, 97.00 - 1.00 = 96.00 offer
    };

    expect(impliedQuote("Feb26", ["Jan26/Feb26", "Feb26/Mar26"], many, 2)).toEqual({ impliedBid: 94.8, impliedAsk: 96.0 });
  });

  it("is blank for a strip no listed spread touches", () => {
    expect(impliedQuote("Apr26", spreads, strips, 2)).toEqual({});
  });

  it("gives only the side it can work out when a leg is one-sided", () => {
    // Feb26 has a bid but no offer: Mar26's implied bid needs Feb26's bid (there), its offer needs Feb26's offer (not).
    const oneSided = { "Feb26": { bid: 94.55 }, "Feb26/Mar26": { bid: -1.45, ask: 1.5 } };

    expect(impliedQuote("Mar26", spreads, oneSided, 2)).toEqual({ impliedBid: 93.05 });
  });

  it("is blank while the spread itself has no prices yet", () => {
    expect(impliedQuote("Mar26", spreads, { "Feb26": { bid: 94.55, ask: 96.95 } }, 2)).toEqual({});
  });

  it("rounds to the screen's decimals, so binary arithmetic leaves no dust", () => {
    // 96.95 + 1.45 is 98.39999999999999 in floating point.
    expect(impliedQuote("Mar26", spreads, strips, 2).impliedAsk).toBe(98.4);
    expect(impliedQuote("Mar26", spreads, strips, 3).impliedAsk).toBe(98.4);
  });

  it("ignores a spread label that is not two legs", () => {
    expect(impliedQuote("Mar26", ["Mar26"], strips, 2)).toEqual({});
  });
});
