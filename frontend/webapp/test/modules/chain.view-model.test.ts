import { describe, it, expect } from "vitest";
import type { NatGasCurveMap, Screen, ScreenRow } from "@atlas/data";
import { buildChainRows, chainRowClassRules } from "@/application/modules/chain/chain.view-model";

// Rows as the API describes an option chain (GET /api/screens/xcom-ttf): each expiry's future, then its options.
const future = (expiry: string, label: string, symbol: string, hub: ScreenRow["hub"] = "TTF"): ScreenRow =>
  ({ hub, label, group: expiry, source: "quoted", symbol, near: null, far: null, option: null });

const option = (expiry: string, strike: number, right: "C" | "P", hub: ScreenRow["hub"] = "TTF"): ScreenRow => ({
  hub,
  label: `${expiry} ${strike.toFixed(2)} ${right}`,
  group: expiry,
  source: "quoted",
  symbol: `sym ${expiry} ${strike} ${right}`,
  near: null,
  far: null,
  option: { expiryDate: "2026-10-27", underlyingSymbol: "TFM 26X-ICN", strike, right },
});

const screenOf = (...rows: ScreenRow[]): Screen => ({ key: "xcom-ttf", title: "TTF Options", rows });

const nov26 = screenOf(
  future("Nov26", "Nov26", "TFM 26X-ICN"),
  option("Nov26", 79, "C"), option("Nov26", 79, "P"),
  option("Nov26", 80, "C"), option("Nov26", 80, "P"),
);

describe("buildChainRows", () => {
  it("lays each expiry out as the future, then one row per strike, in the API's order", () => {
    const rows = buildChainRows({}, nov26);

    expect(rows.map(r => [r.kind, r.symbol, r.tenor])).toEqual([
      ["future", "Nov26", "future"],
      ["strike", "Nov26", "79.00"],
      ["strike", "Nov26", "80.00"],
    ]);
  });

  it("puts the call's figures on the left of a strike row and the put's on the right", () => {
    const curves: NatGasCurveMap = {
      TTF: {
        "Nov26 80.00 C": { bid: 3.5, ask: 3.7, last: 3.6 },
        "Nov26 80.00 P": { bid: 4.1, ask: 4.3 },
      },
    };

    const row = buildChainRows(curves, nov26).find(r => r.tenor === "80.00")!;

    expect(row.call).toEqual({ bid: 3.5, ask: 3.7, last: 3.6 });
    expect(row.put).toEqual({ bid: 4.1, ask: 4.3 });
    expect(row.label).toBe("80.00");
  });

  it("heads the expiry with the future: its figures on the left, a heading in the strike column", () => {
    const curves: NatGasCurveMap = { TTF: { Nov26: { bid: 71.64, ask: 71.715, last: 71.691 } } };

    const row = buildChainRows(curves, nov26)[0];

    expect(row.kind).toBe("future");
    expect(row.label).toBe("Nov26 options on Nov26 TFM 26X-ICN");
    expect(row.call).toEqual({ bid: 71.64, ask: 71.715, last: 71.691 });
    expect(row.put).toBeUndefined();
  });

  it("says which future an expiry is priced off when it is not its own month - EUA's options sit on December", () => {
    const eua = screenOf(future("Oct26", "Dec26", "ECF 26Z-ICN", "EUA"), option("Oct26", 85, "C", "EUA"));

    expect(buildChainRows({}, eua)[0].label).toBe("Oct26 options on Dec26 ECF 26Z-ICN");
  });

  it("shows every strike before it has a price, and a strike with only one side listed", () => {
    const lopsided = screenOf(future("Nov26", "Nov26", "TFM 26X-ICN"), option("Nov26", 78.5, "P"));

    const rows = buildChainRows({}, lopsided);

    expect(rows[1]).toEqual({ kind: "strike", symbol: "Nov26", tenor: "78.50", label: "78.50", put: {} });
  });

  it("keeps the strikes of different expiries apart", () => {
    const two = screenOf(
      future("Nov26", "Nov26", "TFM 26X-ICN"), option("Nov26", 80, "C"),
      future("Dec26", "Dec26", "TFM 26Z-ICN"), option("Dec26", 80, "C"),
    );
    const curves: NatGasCurveMap = { TTF: { "Nov26 80.00 C": { last: 3.6 }, "Dec26 80.00 C": { last: 4.9 } } };

    const rows = buildChainRows(curves, two);

    expect(rows.map(r => [r.symbol, r.tenor, r.call?.last])).toEqual([
      ["Nov26", "future", undefined],
      ["Nov26", "80.00", 3.6],
      ["Dec26", "future", undefined],
      ["Dec26", "80.00", 4.9],
    ]);
  });
});

describe("chainRowClassRules", () => {
  it("marks the future rows and nothing else", () => {
    const rule = chainRowClassRules["ice-chain-future"] as (p: { data?: { kind: string } }) => boolean;

    expect(rule({ data: { kind: "future" } })).toBe(true);
    expect(rule({ data: { kind: "strike" } })).toBe(false);
    expect(rule({})).toBe(false);
  });
});
