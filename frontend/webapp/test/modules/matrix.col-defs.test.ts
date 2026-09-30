import { describe, it, expect } from "vitest";
import type { ColDef, ColGroupDef } from "ag-grid-community";
import type { OptionMatrix, Screen } from "@atlas/data";
import { buildMatrixColumnDefs } from "@/application/modules/calculator/matrix.col-defs";
import { MatrixBlockHeader } from "@/application/modules/calculator/matrix.block-header";
import { chainView } from "@/application/registries/chain-views";

type Formatter = (p: { value: unknown }) => string;
type ClassRule = (p: { data?: unknown }) => boolean;

const matrix: OptionMatrix = {
  product: "TTF",
  expiries: [
    { kind: "Month", label: "Nov26", expiryDate: "2026-10-27", underlyingSymbol: "TFM 26X-ICN", strikes: [70, 72] },
    { kind: "Quarter", label: "Q1 27", expiryDate: "2026-12-24", underlyingSymbol: "TFMQ 27F-ICN", strikes: [70] },
  ],
};

// The chain screen, which says what label the store files each future under.
const screen: Screen = {
  key: "xcom-ttf", title: "TTF Options",
  rows: [
    { hub: "TTF", label: "Nov26", group: "Nov26", source: "quoted", symbol: "TFM 26X-ICN", near: null, far: null, option: null },
    { hub: "TTF", label: "Q1 27", group: "Q1 27", source: "quoted", symbol: "TFMQ 27F-ICN", near: null, far: null, option: null },
  ],
};

describe("matrix columns", () => {
  const defs = buildMatrixColumnDefs(matrix, screen, chainView("xcom-ttf")) as ColGroupDef[];
  const children = (block: ColGroupDef) => block.children as ColDef[];
  const format = (col: ColDef, value: unknown) => (col.valueFormatter as Formatter)({ value });

  it("is one block per expiry in the matrix's order, headed by ICE's code, with no pinned column", () => {
    expect(defs.map(c => c.headerName)).toEqual(["X26", "Q1 27"]);
    expect(defs.every(c => "children" in c)).toBe(true);
  });

  it("gives each block its own strike, then the call and its delta, the put and its delta, and the vol", () => {
    expect(children(defs[0]).map(c => c.headerName)).toEqual(["Strike", "Call", "Δ", "Put", "Δ", "Vol"]);
    expect(children(defs[0]).map(c => c.field)).toEqual([
      "cells.Nov26.strike", "cells.Nov26.call", "cells.Nov26.callDelta", "cells.Nov26.put", "cells.Nov26.putDelta", "cells.Nov26.vol",
    ]);
  });

  it("draws the block header with the live future price, watching the label the store files the future under", () => {
    expect(defs[0].headerGroupComponent).toBe(MatrixBlockHeader);
    expect(defs[0].headerGroupComponentParams).toEqual({ code: "X26", hub: "TTF", futureLabel: "Nov26", decimals: 3 });
    expect(defs[1].headerGroupComponentParams).toEqual({ code: "Q1 27", hub: "TTF", futureLabel: "Q1 27", decimals: 3 });
  });

  it("falls back to the future's symbol as its label before the chain screen has arrived", () => {
    const early = buildMatrixColumnDefs(matrix, undefined, chainView("xcom-ttf")) as ColGroupDef[];

    expect(early[0].headerGroupComponentParams).toEqual({ code: "X26", hub: "TTF", futureLabel: "TFM 26X-ICN", decimals: 3 });
  });

  it("formats strikes to 2 decimals, theos to the product's, deltas as whole percents in brackets, vols to one decimal", () => {
    const [strike, call, delta, , , vol] = children(defs[0]);

    expect(format(strike, 72)).toBe("72.00");
    expect(format(call, 25.316)).toBe("25.316");
    expect(format(delta, 96.6)).toBe("(97)");
    expect(format(vol, 88.61)).toBe("88.6");
    expect(format(call, undefined)).toBe("");
    const oil = buildMatrixColumnDefs(matrix, screen, chainView("xcom-brent")) as ColGroupDef[];
    expect(format(children(oil[0])[1], 3.5)).toBe("3.50");
  });

  it("marks a vol read off the market, and one borrowed from another expiry, so each can be shown apart", () => {
    const vol = children(defs[0])[5];
    const market = vol.cellClassRules!["ice-matrix-market-vol"] as ClassRule;
    const borrowed = vol.cellClassRules!["ice-matrix-borrowed-vol"] as ClassRule;

    expect(market({ data: { cells: { Nov26: { volSource: "market" } } } })).toBe(true);
    expect(market({ data: { cells: { Nov26: { volSource: "fit" } } } })).toBe(false);
    expect(borrowed({ data: { cells: { Nov26: { volSource: "borrowed" } } } })).toBe(true);
    expect(borrowed({ data: { cells: {} } })).toBe(false);
  });

  it("carries the expiry's kind on the block header, so months and strips can look different", () => {
    expect(defs[0].headerClass).toContain("ice-matrix-month");
    expect(defs[1].headerClass).toContain("ice-matrix-quarter");
  });
});
