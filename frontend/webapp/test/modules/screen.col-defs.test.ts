import { describe, it, expect } from "vitest";
import type { ColDef } from "ag-grid-community";
import { buildScreenColumnDefs } from "@/application/modules/screen/screen.col-defs";
import { CoalView, GasView } from "@/application/registries/screen-views";

type Formatter = (p: { value: unknown }) => string;

const format = (col: ColDef | undefined, value: unknown) =>
  (col?.valueFormatter as Formatter)({ value });

describe("gas screen columns (ICE's TTF flat price screen)", () => {
  const colDefs = buildScreenColumnDefs(GasView);
  const byField = (field: string) => colDefs.find(c => c.field === field);

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

  it("centres every figure column and its header; Strip stays left-aligned", () => {
    const figures = colDefs.filter(c => c.field && c.field !== "tenor");

    expect(figures.length).toBeGreaterThan(0);
    expect(figures.every(c => String(c.cellClass).split(" ").includes("ice-centred"))).toBe(true);
    expect(figures.every(c => c.headerClass === "ice-centred-header")).toBe(true);
    expect(figures.some(c => c.type === "rightAligned")).toBe(false);
    expect(byField("tenor")!.cellClass).toBeUndefined();
    expect(byField("tenor")!.headerClass).toBeUndefined();
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

});

describe("coal screen columns (WebICE's Coal tab)", () => {
  const colDefs = buildScreenColumnDefs(CoalView);
  const byField = (field: string) => colDefs.find(c => c.field === field);
  const classes = (value: unknown) => String(value).split(" ");

  it("shows WebICE's columns in WebICE's order, the implied block last", () => {
    const headers = colDefs.map(c => c.headerName).filter(h => h !== "");

    expect(headers).toEqual([
      "Product", "Hub", "Strip", "B Qty", "Bid", "Offer", "O Qty", "Last", "Change", "Settle", "OI",
      "High", "Low", "WAP", "Volume", "Block Vol", "B Qty", "Bid", "Offer", "O Qty",
    ]);
  });

  it("shows the hub in the Hub column, read from the row's symbol; gas screens have no Hub column", () => {
    expect(byField("symbol")!.headerName).toBe("Hub");
    expect(buildScreenColumnDefs(GasView).find(c => c.field === "symbol")).toBeUndefined();
  });

  it("shows coal prices to 2 decimals, dollars and cents a tonne, where gas shows 3", () => {
    expect(format(byField("bid"), 94.5)).toBe("94.50");
    expect(format(byField("wap"), 95.2)).toBe("95.20");
    expect(format(byField("impliedAsk"), 98.4)).toBe("98.40");
    expect(format(byField("openInterest"), 60954)).toBe("60954");
    expect(format(buildScreenColumnDefs(GasView).find(c => c.field === "bid"), 77.85)).toBe("77.850");
  });

  it("keeps Product, Hub and Strip left-aligned and centres every figure", () => {
    for (const text of ["product", "symbol", "tenor"]) {
      expect(byField(text)!.cellClass).toBeUndefined();
      expect(byField(text)!.headerClass).toBeUndefined();
    }
    const figures = colDefs.filter(c => c.field && !["product", "symbol", "tenor"].includes(c.field));
    expect(figures.length).toBe(17);
    expect(figures.every(c => classes(c.cellClass).includes("ice-centred"))).toBe(true);
    expect(figures.every(c => classes(c.headerClass).includes("ice-centred-header"))).toBe(true);
  });

  it("marks the first column of the implied block, cell and header, so a line can set the block apart", () => {
    const marked = colDefs.filter(c => classes(c.cellClass).includes("ice-implied-first")).map(c => c.field);
    const markedHeaders = colDefs.filter(c => classes(c.headerClass).includes("ice-implied-first")).map(c => c.field);

    expect(marked).toEqual(["impliedBidSize"]);
    expect(markedHeaders).toEqual(["impliedBidSize"]);
  });

  it("gives every data column its own id, so the saved widths and the two Bid columns never clash", () => {
    const fields = colDefs.filter(c => c.field).map(c => c.field);

    expect(new Set(fields).size).toBe(fields.length);
  });

  it("still flashes only Last", () => {
    expect(colDefs.filter(c => c.enableCellChangeFlash).map(c => c.field)).toEqual(["last"]);
  });
});
