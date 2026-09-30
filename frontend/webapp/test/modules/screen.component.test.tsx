import { describe, it, expect, beforeEach, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { GridApi } from "ag-grid-community";
import "@/ag-grid-setup";
import { ScreenComponent } from "@/application/modules/screen/screen.component";
import { buildScreenColumnDefs } from "@/application/modules/screen/screen.col-defs";
import { GasView } from "@/application/registries/screen-views";

// Built once and shared between renders, exactly as the view-model does it.
const colDefs = buildScreenColumnDefs(GasView);

const rows = (bid: number) => [
  { symbol: "TTF", tenor: "Oct26", bid, ask: bid + 0.04, last: bid + 0.02 },
  { symbol: "TTF", tenor: "Nov26", bid: bid - 0.4, ask: bid - 0.35, last: bid - 0.38 },
];

/** Renders the screen and hands back the grid's API once the grid is ready, plus a way to send a new tick. */
async function renderScreen() {
  let api: GridApi | undefined;
  const props = { screenKey: "ttf-flat", title: "Nat Gas TTF Flat Price", colDefs, asOf: "" };
  const view = render(<ScreenComponent {...props} data={rows(77.85)} onGridReady={a => { api = a; }} />);
  await waitFor(() => expect(api).toBeDefined());

  return {
    api: api!,
    // What the live screen does several times a second: same screen, new figures.
    tick: (bid: number, asOf = "") =>
      view.rerender(<ScreenComponent {...props} asOf={asOf} data={rows(bid)} onGridReady={a => { api = a; }} />),
  };
}

const stateOf = (api: GridApi, colId: string) => api.getColumnState().find(c => c.colId === colId)!;

describe("ScreenComponent column widths", () => {
  beforeEach(() => window.localStorage.clear());

  it("keeps a width the user set when the next price tick arrives", async () => {
    // Reported by Sean, 21 Sep 2026: drag a column wider and "in a fraction of a second it reverts".
    const { api, tick } = await renderScreen();

    // What a drag does: the column takes a fixed width and stops sharing the leftover space (flex off).
    api.applyColumnState({ state: [{ colId: "bid", width: 180, flex: null }] });
    expect(stateOf(api, "bid").width).toBe(180);

    tick(77.9);
    tick(77.95, "Prices as of 15:10:01");

    await waitFor(() => expect(api.getDisplayedRowAtIndex(0)?.data.bid).toBe(77.95));
    expect(stateOf(api, "bid").width).toBe(180);
    expect(stateOf(api, "bid").flex ?? null).toBeNull();
  });

  it("leaves the columns nobody touched sharing the width", async () => {
    const { api, tick } = await renderScreen();
    api.applyColumnState({ state: [{ colId: "bid", width: 180, flex: null }] });

    tick(77.9);

    await waitFor(() => expect(api.getDisplayedRowAtIndex(0)?.data.bid).toBe(77.9));
    expect(stateOf(api, "ask").flex).toBeGreaterThan(0);
  });

  it("opens with the widths remembered for that screen, and keeps them through a tick", async () => {
    window.localStorage.setItem("atlas.column-widths.ttf-flat", JSON.stringify({ tenor: 260 }));

    const { api, tick } = await renderScreen();
    expect(stateOf(api, "tenor").width).toBe(260);

    tick(77.9);

    await waitFor(() => expect(api.getDisplayedRowAtIndex(0)?.data.bid).toBe(77.9));
    expect(stateOf(api, "tenor").width).toBe(260);
    expect(stateOf(api, "tenor").flex ?? null).toBeNull();
  });

  it("offers Reset columns only when a width is remembered, and Reset puts the columns back", async () => {
    window.localStorage.setItem("atlas.column-widths.ttf-flat", JSON.stringify({ bid: 180 }));
    const { api } = await renderScreen();
    expect(stateOf(api, "bid").flex ?? null).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "Reset columns" }));

    // Back to sharing the width as designed, nothing remembered, and the link gone.
    expect(stateOf(api, "bid").flex).toBe(65);
    expect(window.localStorage.getItem("atlas.column-widths.ttf-flat")).toBeNull();
    expect(screen.queryByRole("button", { name: "Reset columns" })).toBeNull();
  });

  it("does not offer Reset columns on a screen nobody has adjusted", async () => {
    await renderScreen();

    expect(screen.queryByRole("button", { name: "Reset columns" })).toBeNull();
  });
});

describe("ScreenComponent row selection", () => {
  const selectedStrips = (api: GridApi) => api.getSelectedNodes().map(n => n.data.tenor);
  const cellOfRow = (index: number) =>
    document.querySelector(`.ag-center-cols-container .ag-row[row-index="${index}"] .ag-cell`) as HTMLElement;

  it("a click selects the row, and a second click on the same row clears it", async () => {
    // Sean, 23 Sep 2026: "want to be able to get rid of a blue click".
    const { api } = await renderScreen();
    await waitFor(() => expect(cellOfRow(0)).not.toBeNull());

    fireEvent.click(cellOfRow(0));
    expect(selectedStrips(api)).toEqual(["Oct26"]);

    fireEvent.click(cellOfRow(0));
    expect(selectedStrips(api)).toEqual([]);
  });

  it("clicking another row moves the selection rather than adding to it", async () => {
    const { api } = await renderScreen();
    await waitFor(() => expect(cellOfRow(1)).not.toBeNull());

    fireEvent.click(cellOfRow(0));
    fireEvent.click(cellOfRow(1));

    expect(selectedStrips(api)).toEqual(["Nov26"]);
  });
});

// Each refresh of a row or cell that changes nothing on screen leaves React holding a small record until that row or
// cell is next redrawn, so rows that never change collected them without end (about 1.6 MB a minute on TTF Flat).
describe("ScreenComponent memory on a screen left open", () => {
  const props = { screenKey: "ttf-flat", title: "Nat Gas TTF Flat Price", colDefs, asOf: "" };
  const figures = (octBid: number) => [
    { symbol: "TTF", tenor: "Oct26", bid: octBid, ask: octBid + 0.04 },
    { symbol: "TTF", tenor: "Nov26", bid: 77.1, ask: 77.15 },
  ];

  it("leaves a row whose figures did not change alone when another row ticks", async () => {
    let api: GridApi | undefined;
    const view = render(<ScreenComponent {...props} data={figures(77.85)} onGridReady={a => { api = a; }} />);
    await waitFor(() => expect(api).toBeDefined());
    const novBefore = api!.getRowNode("TTF|Nov26")!.data;

    view.rerender(<ScreenComponent {...props} data={figures(77.9)} onGridReady={a => { api = a; }} />);

    await waitFor(() => expect(api!.getRowNode("TTF|Oct26")!.data.bid).toBe(77.9));
    expect(api!.getRowNode("TTF|Nov26")!.data).toBe(novBefore);
  });

  it("redraws its rows on a timer, which lets React drop what it held for them, and stops when closed", async () => {
    let api: GridApi | undefined;
    const view = render(<ScreenComponent {...props} data={figures(77.85)} rowRedrawIntervalMs={20} onGridReady={a => { api = a; }} />);
    await waitFor(() => expect(api).toBeDefined());
    const redraw = vi.spyOn(api!, "redrawRows");

    await waitFor(() => expect(redraw).toHaveBeenCalled());

    view.unmount();
    const callsWhenClosed = redraw.mock.calls.length;
    await new Promise(resolve => setTimeout(resolve, 100));
    expect(redraw.mock.calls.length).toBe(callsWhenClosed);
  });
});
