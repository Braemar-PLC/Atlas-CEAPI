import { describe, it, expect } from "vitest";
import type { ColDef } from "ag-grid-community";
import { buildNatGasColumnDefs } from "@/application/modules/natgas/natgas.col-defs";
import { NatGasViews } from "@/application/registries/natgas-views";

type Formatter = (p: { value: unknown }) => string;

const format = (col: ColDef | undefined, value: unknown) =>
  (col?.valueFormatter as Formatter)({ value });

describe("natgas column definitions (ICE screen replica)", () => {
  const view = NatGasViews[0];
  const colDefs = buildNatGasColumnDefs(view.fields ?? [], false);
  const byField = (field: string) => colDefs.find(c => c.field === field);

  it("is titled like the ICE screen", () => {
    expect(view.title).toBe("Nat Gas TTF Flat Price");
  });

  it("shows ICE's 12 columns in ICE's order", () => {
    const headers = colDefs.map(c => c.headerName).filter(h => h !== "");

    expect(headers).toEqual([
      "Strip", "B Qty", "Bid", "Offer", "O Qty", "Last",
      "Change", "Settle", "High", "Low", "Volume", "Block Vol",
    ]);
  });

  it("puts the red bar before the bid side and the green bar after the offer side", () => {
    const ids = colDefs.map(c => c.colId ?? c.field);

    expect(ids.indexOf("bidBar")).toBe(ids.indexOf("bidSize") - 1);
    expect(ids.indexOf("askBar")).toBe(ids.indexOf("askSize") + 1);
    expect(ids.indexOf("askBar")).toBe(ids.indexOf("last") - 1);
  });

  it("shows prices to 3 decimals and quantities as whole numbers", () => {
    expect(format(byField("bid"), 77.85)).toBe("77.850");
    expect(format(byField("netChange"), -0.135)).toBe("-0.135");
    expect(format(byField("volume"), 53440)).toBe("53440");
    expect(format(byField("bid"), undefined)).toBe("");
  });

  it("colours the tick square on Last from the row's tick", () => {
    const rules = byField("last")!.cellClassRules as Record<string, (p: { data: unknown }) => boolean>;

    expect(rules["ice-tick-up"]({ data: { tick: "up" } })).toBe(true);
    expect(rules["ice-tick-down"]({ data: { tick: "up" } })).toBe(false);
    expect(rules["ice-tick-down"]({ data: { tick: "down" } })).toBe(true);
    expect(rules["ice-tick-up"]({ data: {} })).toBe(false);
  });

  it("flashes Last when it changes, and no other column", () => {
    expect(colDefs.filter(c => c.enableCellChangeFlash).map(c => c.field)).toEqual(["last"]);
  });

  it("never flashes the red and green bars, which would turn them grey", () => {
    // AG Grid treats a column with no field (the bars) as changed on every refresh,
    // and paints the flash with !important over any background we set.
    const bars = colDefs.filter(c => c.colId === "bidBar" || c.colId === "askBar");

    expect(bars).toHaveLength(2);
    expect(bars.some(c => c.enableCellChangeFlash)).toBe(false);
  });

  it("keeps the bars thin and fixed while the data columns share the width", () => {
    const bars = colDefs.filter(c => c.colId === "bidBar" || c.colId === "askBar");
    const dataCols = colDefs.filter(c => c.field);

    expect(bars.map(c => [c.width, c.minWidth, c.maxWidth, c.initialFlex ?? c.flex])).toEqual([
      [14, 14, 14, undefined],
      [14, 14, 14, undefined],
    ]);
    // initialFlex, never flex: a plain flex is re-applied on every price tick and undoes a dragged width.
    expect(dataCols.every(c => (c.initialFlex ?? 0) > 0)).toBe(true);
    expect(dataCols.every(c => c.flex === undefined)).toBe(true);
  });

  it("makes Strip the widest column and the quantity columns the narrowest, as on ICE", () => {
    const flex = (field: string) => byField(field)!.initialFlex!;
    const others = colDefs.filter(c => c.field && c.field !== "tenor").map(c => c.initialFlex!);

    expect(flex("tenor")).toBeGreaterThan(Math.max(...others));
    expect(flex("askSize")).toBe(Math.min(...others));
    expect(flex("bidSize")).toBeLessThan(flex("bid"));
  });

  it("lets every data column be dragged wider or narrower, but never the bars", () => {
    // AG Grid resizes a column unless it is told not to, so "resizable" is simply never set to false on data.
    const bars = colDefs.filter(c => c.colId === "bidBar" || c.colId === "askBar");
    const dataCols = colDefs.filter(c => c.field);

    expect(dataCols.every(c => c.resizable !== false)).toBe(true);
    expect(bars.every(c => c.resizable === false)).toBe(true);
  });

  it("gives every data column an id the saved widths can find it by", () => {
    // Widths are remembered by column id, which AG Grid takes from `field`. Two columns on one field would clash.
    const fields = colDefs.filter(c => c.field).map(c => c.field);

    expect(new Set(fields).size).toBe(fields.length);
  });

  it("adds the hub column only when asked", () => {
    expect(byField("symbol")).toBeUndefined();
    expect(buildNatGasColumnDefs(["bid"], true)[0].field).toBe("symbol");
  });
});
