import { describe, it, expect } from "vitest";
import type { ColDef, ColGroupDef } from "ag-grid-community";
import { buildChainColumnDefs } from "@/application/modules/chain/chain.col-defs";
import { chainView } from "@/application/registries/chain-views";

type Formatter = (p: { value: unknown }) => string;
type ClassRule = (p: { data?: unknown }) => boolean;
type Span = (p: { data?: unknown }) => number;

const isGroup = (c: ColDef | ColGroupDef): c is ColGroupDef => "children" in c;

describe("option chain columns", () => {
  const defs = buildChainColumnDefs(chainView("xcom-ttf"));
  const leaves = defs.flatMap(c => (isGroup(c) ? (c.children as ColDef[]) : [c]));
  const byField = (field: string) => leaves.find(c => c.field === field);
  const format = (col: ColDef | undefined, value: unknown) => (col?.valueFormatter as Formatter)({ value });

  it("is the calls, then the strike, then the puts", () => {
    expect(defs.map(c => (isGroup(c) ? c.headerName : (c as ColDef).headerName))).toEqual(["Calls", "Strike", "Puts"]);
  });

  it("reads outwards from the strike on both sides, bid and offer nearest it", () => {
    const [calls, , puts] = defs as [ColGroupDef, ColDef, ColGroupDef];

    expect((calls.children as ColDef[]).map(c => c.headerName)).toEqual(["Vol", "Settle", "Last", "B Qty", "Bid", "Offer", "O Qty"]);
    expect((puts.children as ColDef[]).map(c => c.headerName)).toEqual(["B Qty", "Bid", "Offer", "O Qty", "Last", "Settle", "Vol"]);
  });

  it("reads each side's figures from its own quote on the row", () => {
    expect((defs[0] as ColGroupDef).children.map(c => (c as ColDef).field)).toEqual([
      "call.volume", "call.settle", "call.last", "call.bidSize", "call.bid", "call.ask", "call.askSize",
    ]);
    expect((defs[2] as ColGroupDef).children.map(c => (c as ColDef).field)).toEqual([
      "put.bidSize", "put.bid", "put.ask", "put.askSize", "put.last", "put.settle", "put.volume",
    ]);
  });

  it("gives every column its own field, so widths and flashes never clash between the sides", () => {
    const fields = leaves.map(c => c.field);

    expect(new Set(fields).size).toBe(fields.length);
  });

  it("formats TTF prices to 3 decimals and oil to 2, quantities whole, volumes blank at 0", () => {
    expect(format(byField("call.bid"), 3.5)).toBe("3.500");
    expect(format(byField("put.bidSize"), 25)).toBe("25");
    expect(format(byField("call.volume"), 0)).toBe("");
    expect(format(byField("put.volume"), 140)).toBe("140");

    const oil = buildChainColumnDefs(chainView("xcom-brent")).flatMap(c => (isGroup(c) ? (c.children as ColDef[]) : [c]));
    expect(format(oil.find(c => c.field === "call.bid"), 3.5)).toBe("3.50");
  });

  it("flashes Last on both sides and nothing else, and colours each side's tick square from its own quote", () => {
    expect(leaves.filter(c => c.enableCellChangeFlash).map(c => c.field)).toEqual(["call.last", "put.last"]);

    const up = byField("put.last")!.cellClassRules!["ice-tick-up"] as ClassRule;
    expect(up({ data: { put: { tick: "up" }, call: { tick: "down" } } })).toBe(true);
    expect(up({ data: { put: { tick: "down" }, call: { tick: "up" } } })).toBe(false);
  });

  it("stretches the future's heading from the strike column across the put side", () => {
    const span = byField("label")!.colSpan as Span;

    expect(span({ data: { kind: "future" } })).toBe(8);
    expect(span({ data: { kind: "strike" } })).toBe(1);
    const heading = byField("label")!.cellClassRules!["ice-chain-heading"] as ClassRule;
    expect(heading({ data: { kind: "future" } })).toBe(true);
    expect(heading({ data: { kind: "strike" } })).toBe(false);
  });

  it("uses initialFlex, never flex, so a dragged width holds", () => {
    expect(leaves.every(c => (c.initialFlex ?? 0) > 0 && c.flex === undefined)).toBe(true);
  });
});
