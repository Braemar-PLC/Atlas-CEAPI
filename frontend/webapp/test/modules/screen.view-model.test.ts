import { describe, it, expect } from "vitest";
import type { NatGasCurveMap, Screen, ScreenRow } from "@atlas/data";
import { CoalView } from "@/application/registries/screen-views";
import {
  buildScreenRows, computeSpreadQuote, delayMinutesOf, formatPricesAsOf,
} from "@/application/modules/screen/screen.view-model";

// Rows as the API describes them (GET /api/screens/{key}).
const quoted = (label: string, symbol: string, hub: ScreenRow["hub"] = "TTF", group = "Months"): ScreenRow =>
  ({ hub, label, group, source: "quoted", symbol, near: null, far: null });

const computed = (near: string, far: string, hub: ScreenRow["hub"] = "TTF"): ScreenRow => ({
  hub, label: `${near}/${far}`, group: "Spreads", source: "computed", symbol: null,
  near: { label: near, symbol: `sym ${near}` }, far: { label: far, symbol: `sym ${far}` },
});

const screenOf = (...rows: ScreenRow[]): Screen => ({ key: "k", title: "t", rows });

describe("buildScreenRows", () => {
  it("shows the screen's rows in the screen's order, however the prices arrived", () => {
    const screen = screenOf(quoted("Oct26", "TFM 26V-ICN"), quoted("Nov26", "TFM 26X-ICN"), quoted("Q4 26", "TFMQ 26V-ICN"));
    // Deliberately arriving out of order.
    const curves: NatGasCurveMap = {
      TTF: { "Q4 26": { last: 77.55 }, "Nov26": { last: 77.54 }, "Oct26": { last: 77.895 } },
    };

    expect(buildScreenRows(curves, screen).map(r => r.tenor)).toEqual(["Oct26", "Nov26", "Q4 26"]);
  });

  it("leaves out strips the screen does not list, such as expired months", () => {
    const curves: NatGasCurveMap = {
      TTF: { "Apr26": { last: 64.665 }, "Oct26": { last: 77.895 } },
    };

    expect(buildScreenRows(curves, screenOf(quoted("Oct26", "TFM 26V-ICN"))).map(r => r.tenor)).toEqual(["Oct26"]);
  });

  it("takes each row's figures from its own hub", () => {
    const curves: NatGasCurveMap = {
      NBP: { "Oct26": { last: 190.1 } },
      TTF: { "Oct26": { last: 77.895 } },
    };

    expect(buildScreenRows(curves, screenOf(quoted("Oct26", "GWM 26V-ICE", "NBP"))))
      .toEqual([{ symbol: "NBP", tenor: "Oct26", last: 190.1 }]);
  });

  it("carries the quote's figures onto the row", () => {
    const curves: NatGasCurveMap = { TTF: { "Oct26": { bid: 77.85, ask: 77.89, tick: "up" } } };

    expect(buildScreenRows(curves, screenOf(quoted("Oct26", "TFM 26V-ICN")))[0])
      .toEqual({ symbol: "TTF", tenor: "Oct26", bid: 77.85, ask: 77.89, tick: "up" });
  });

  it("shows every row of the screen even before it has a price, as ICE does", () => {
    const screen = screenOf(quoted("Oct26", "TFM 26V-ICN"), quoted("Cal 31", "TFMY 31F-ICN"));

    expect(buildScreenRows({}, screen)).toEqual([
      { symbol: "TTF", tenor: "Oct26" },
      { symbol: "TTF", tenor: "Cal 31" },
    ]);
  });

  it("uses ICE's own figures for a spread ICE quotes, volume and settlement included", () => {
    const screen = screenOf(quoted("Oct26/Nov26", "TFM 26V:TFM26X-ICN"));
    const curves: NatGasCurveMap = {
      TTF: { "Oct26/Nov26": { bid: 0.34, ask: 0.36, last: 0.35, settle: 0.355, volume: 2370 } },
    };

    expect(buildScreenRows(curves, screen)[0])
      .toEqual({ symbol: "TTF", tenor: "Oct26/Nov26", bid: 0.34, ask: 0.36, last: 0.35, settle: 0.355, volume: 2370 });
  });

  it("works a spread out from its two legs when ICE has no such contract", () => {
    const screen = screenOf(computed("Oct26", "Q1 27"));
    const curves: NatGasCurveMap = {
      TTF: {
        "Oct26": { bid: 75.925, ask: 75.965, last: 75.945, volume: 53440 },
        "Q1 27": { bid: 73.64, ask: 73.74, last: 73.7 },
      },
    };

    // A worked-out spread has a bid and an offer only: nothing traded, so no last, change, settle or volume.
    expect(buildScreenRows(curves, screen)[0])
      .toEqual({ symbol: "TTF", tenor: "Oct26/Q1 27", bid: 2.185, ask: 2.325 });
  });

  it("can work out a spread whose legs are not rows of the screen themselves", () => {
    // Sep29/Sep30 on the desk's spreads screen: neither month is shown, both are listened to.
    const curves: NatGasCurveMap = { TTF: { "Sep29": { bid: 30, ask: 30.4 }, "Sep30": { bid: 27.5, ask: 27.9 } } };

    expect(buildScreenRows(curves, screenOf(computed("Sep29", "Sep30")))[0])
      .toEqual({ symbol: "TTF", tenor: "Sep29/Sep30", bid: 2.1, ask: 2.9 });
  });
});

describe("computeSpreadQuote", () => {
  // The desk's Edgeview screen, 21 Sep 2026. It shows these spreads at exactly these prices.
  const oct26 = { bid: 75.925, ask: 75.965 };
  const dec26 = { bid: 75.2, ask: 75.245 };
  const q1_27 = { bid: 73.64, ask: 73.74 };
  const mar27 = { bid: 71.85, ask: 71.94 };

  it("crosses the legs: bid = near bid - far offer, offer = near offer - far bid", () => {
    expect(computeSpreadQuote(oct26, q1_27)).toEqual({ bid: 2.185, ask: 2.325 });
    expect(computeSpreadQuote(dec26, q1_27)).toEqual({ bid: 1.46, ask: 1.605 });
    expect(computeSpreadQuote(q1_27, mar27)).toEqual({ bid: 1.7, ask: 1.89 });
  });

  it("can be negative, when the far leg is dearer", () => {
    expect(computeSpreadQuote({ bid: 192.63, ask: 194.45 }, { bid: 196.05, ask: 196.26 }))
      .toEqual({ bid: -3.63, ask: -1.6 });
  });

  it("shows only the side it can price when a leg is one-sided", () => {
    // The far leg has a bid but no offer: the spread can be bought, not sold.
    expect(computeSpreadQuote({ bid: 32.08, ask: 32.5 }, { bid: 27.7 })).toEqual({ ask: 4.8 });
  });

  it("is blank until both legs have prices", () => {
    expect(computeSpreadQuote(oct26, undefined)).toEqual({});
    expect(computeSpreadQuote(undefined, undefined)).toEqual({});
  });

  it("is as delayed as its more delayed leg", () => {
    expect(computeSpreadQuote({ ...oct26, delayMinutes: 10 }, { ...q1_27, delayMinutes: 0 }).delayMinutes).toBe(10);
  });

  it("does not show floating-point dust", () => {
    // 75.925 - 73.74 is 2.1850000000000023 in binary arithmetic.
    expect(computeSpreadQuote(oct26, q1_27).bid).toBe(2.185);
  });
});

describe("prices as of", () => {
  it("shows the time the prices belong to: now, less the feed's delay", () => {
    const now = new Date(2026, 8, 18, 15, 20, 1); // 15:20:01 local time

    expect(formatPricesAsOf(now, 10)).toBe("Prices as of 15:10:01");
  });

  it("shows the current time for a real-time feed", () => {
    expect(formatPricesAsOf(new Date(2026, 8, 18, 9, 5, 7), 0)).toBe("Prices as of 09:05:07");
  });

  it("goes back over midnight", () => {
    expect(formatPricesAsOf(new Date(2026, 8, 18, 0, 4, 0), 10)).toBe("Prices as of 23:54:00");
  });

  it("takes the delay ICE reports on the rows, the largest if they differ", () => {
    expect(delayMinutesOf([{ delayMinutes: 10 }, { delayMinutes: 10 }, {}])).toBe(10);
    expect(delayMinutesOf([{ delayMinutes: 0 }, { delayMinutes: 10 }])).toBe(10);
    expect(delayMinutesOf([{ delayMinutes: 0 }])).toBe(0);
  });

  it("has no delay to report until a row carries one", () => {
    expect(delayMinutesOf([])).toBeUndefined();
    expect(delayMinutesOf([{}, {}])).toBeUndefined();
  });
});

describe("buildScreenRows for coal", () => {
  it("names the product on every coal row, as WebICE's Product column does", () => {
    const screen = screenOf(
      quoted("Oct26", "ATW 26V-ICE", "ARA"),
      quoted("Oct26/Nov26", "ATW 26V:ATW26X-ICE", "ARA", "Spreads"),
      quoted("Q4 26", "NCFQ 26V-ICE", "Newcastle", "Quarters"),
    );

    expect(buildScreenRows({}, screen)).toEqual([
      { symbol: "ARA", tenor: "Oct26", product: "Rotterdam Coal Futures" },
      { symbol: "ARA", tenor: "Oct26/Nov26", product: "Rotterdam Coal Spr" },
      { symbol: "Newcastle", tenor: "Q4 26", product: "Newcastle Coal Futures" },
    ]);
  });

  it("puts no product on a gas row", () => {
    expect(buildScreenRows({}, screenOf(quoted("Oct26", "TFM 26V-ICN")))[0]).not.toHaveProperty("product");
  });
});

describe("buildScreenRows with implied prices", () => {
  // The coal spreads screen as the API describes it, and the store after both streams have delivered.
  const coalSpreads = screenOf(quoted("Feb26/Mar26", "ATW 26G:ATW26H-ICE", "ARA", "Spreads"));
  const curves: NatGasCurveMap = {
    ARA: {
      "Feb26": { bid: 94.55, ask: 96.95 },
      "Mar26": { bid: 93.05, ask: 96.0, impliedBidSize: 5, impliedAskSize: 10 },
      "Feb26/Mar26": { bid: -1.45, ask: 1.5 },
    },
  };

  it("fills the implied block of an outright row from the spreads screen it is given, to the view's decimals", () => {
    const rows = buildScreenRows(curves, screenOf(quoted("Mar26", "ATW 26H-ICE", "ARA")), CoalView, coalSpreads);

    expect(rows[0]).toEqual({
      symbol: "ARA", tenor: "Mar26", product: "Rotterdam Coal Futures",
      bid: 93.05, ask: 96.0, impliedBidSize: 5, impliedAskSize: 10, impliedBid: 93.05, impliedAsk: 98.4,
    });
  });

  it("uses only the spreads of the row's own hub", () => {
    // A Newcastle Mar26 must not borrow Rotterdam's Feb26/Mar26.
    const rows = buildScreenRows(curves, screenOf(quoted("Mar26", "NCF 26H-ICE", "Newcastle")), CoalView, coalSpreads);

    expect(rows[0]).not.toHaveProperty("impliedBid");
  });

  it("leaves the spread rows themselves without implied prices", () => {
    const rows = buildScreenRows(curves, coalSpreads, CoalView, coalSpreads);

    expect(rows[0]).not.toHaveProperty("impliedBid");
    expect(rows[0]).not.toHaveProperty("impliedAsk");
  });

  it("shows no implied prices when no spreads screen is given, as on the gas screens", () => {
    const rows = buildScreenRows(curves, screenOf(quoted("Mar26", "ATW 26H-ICE", "ARA")));

    expect(rows[0]).not.toHaveProperty("impliedBid");
  });
});
