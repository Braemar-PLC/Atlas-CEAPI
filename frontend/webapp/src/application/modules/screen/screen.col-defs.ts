import type { ColDef, ValueFormatterParams } from "ag-grid-community";
import type { ScreenFieldKey, ScreenView } from "@/application/registries/screen-views";

/**
 * Columns follow the ICE screen the view replicates: its header names, and prices to the view's decimals (3 for
 * gas, 2 for coal). Figures and their headers are centred (the desk's choice, 23 Sep 2026); Product, Hub and
 * Strip are the left-aligned text columns.
 * "bidBar" and "askBar" are not data: they are the thin red and green bars ICE draws beside the
 * bid side and the offer side. A view lists them in `fields` wherever the bar should sit.
 * Styling for the `ice-*` classes is in screen.css.
 *
 * `flex` is each column's share of the width, measured off the mock the desk approved (its pixel
 * widths at 800px across), so Strip is wide and the quantity columns are narrow, as on ICE.
 * It is `initialFlex`, NOT `flex`, on purpose: "initial" values are used once, when the column is created.
 * A plain `flex` is re-applied every time AG Grid re-reads the column definitions, which wiped out a width the
 * user had just dragged a fraction of a second later, on the next price tick (bug found by Sean, 21 Sep 2026;
 * test/modules/screen.component.test.tsx reproduces it).
 * Strip was 115; it is 135 since the figures went to 18px (21 Sep 2026), so the longest spread label
 * ("Winter28/Summer29") still fits on a laptop. These are only the starting widths: a user can drag any column
 * (not the bars), and the grid remembers it - see screen.component.tsx.
 *
 * Only Last flashes when it changes (yellow, ~0.3s — the desk's ask; colour and timing are in
 * screen.theme.ts and screen.component.tsx). Never put `enableCellChangeFlash` in the grid's
 * defaultColDef or on the bars: AG Grid treats a column with no `field` as changed on every refresh and
 * paints the flash with `!important`, which turned both bars permanently grey (18 Sep 2026).
 *
 * The implied block (coal): the first of its four columns carries `ice-implied-first`, which screen.css draws as
 * a line down its left side, so the block reads apart from the traded market, as on WebICE.
 */
export function buildScreenColumnDefs(view: ScreenView): ColDef[] {
  const num = (dp: number) =>
    (p: any) => (p.value == null ? "" : Number(p.value).toFixed(dp));

  const centred: ColDef = { headerClass: "ice-centred-header", cellClass: "ice-centred" };
  const price: ColDef = { ...centred, valueFormatter: num(view.priceDecimals) };
  const quantity: ColDef = { ...centred, valueFormatter: num(0) };
  // A volume of 0 means nothing has traded, and the desk reads a blank cell faster than a column of zeros
  // (Marc Jarvis, 25 Sep 2026). Bid and offer sizes keep their 0: whether those should go blank is not decided.
  const traded: ColDef = {
    ...centred,
    valueFormatter: (p: ValueFormatterParams) => (p.value == null || Number(p.value) === 0 ? "" : Number(p.value).toFixed(0)),
  };

  const bar = (colId: string, cellClass: string): ColDef => ({
    colId,
    headerName: "",
    headerClass: "ice-bar-header",
    cellClass: `ice-bar ${cellClass}`,
    width: 14,
    minWidth: 14,
    maxWidth: 14,
    resizable: false,
  });

  const columns: Record<ScreenFieldKey, ColDef> = {
    product:     { field: "product",     headerName: "Product",   initialFlex: 150 },
    // A row's `symbol` is its hub (screen.view-model.ts), so the Hub column reads that field.
    hub:         { field: "symbol",      headerName: "Hub",       initialFlex: 75 },
    tenor:       { field: "tenor",       headerName: "Strip",     initialFlex: 135 },
    bidBar:      bar("bidBar", "ice-bar-bid"),
    bidSize:     { field: "bidSize",     headerName: "B Qty",     initialFlex: 50, ...quantity },
    bid:         { field: "bid",         headerName: "Bid",       initialFlex: 65, ...price },
    ask:         { field: "ask",         headerName: "Offer",     initialFlex: 65, ...price },
    askSize:     { field: "askSize",     headerName: "O Qty",     initialFlex: 45, ...quantity },
    askBar:      bar("askBar", "ice-bar-ask"),
    last:        {
      field: "last",
      headerName: "Last",
      initialFlex: 70,
      ...price,
      enableCellChangeFlash: true,
      // Setting cellClass replaces the one `price` supplies, so it is repeated here.
      cellClass: "ice-centred ice-last",
      cellClassRules: {
        "ice-tick-up": p => p.data?.tick === "up",
        "ice-tick-down": p => p.data?.tick === "down",
      },
    },
    netChange:   { field: "netChange",   headerName: "Change",    initialFlex: 60, ...price },
    settle:      { field: "settle",      headerName: "Settle",    initialFlex: 65, ...price },
    openInterest: { field: "openInterest", headerName: "OI",      initialFlex: 60, ...quantity },
    high:        { field: "high",        headerName: "High",      initialFlex: 70, ...price },
    low:         { field: "low",         headerName: "Low",       initialFlex: 70, ...price },
    wap:         { field: "wap",         headerName: "WAP",       initialFlex: 65, ...price },
    volume:      { field: "volume",      headerName: "Volume",    initialFlex: 50, ...traded },
    blockVolume: { field: "blockVolume", headerName: "Block Vol", initialFlex: 55, ...traded },
    impliedBidSize: {
      field: "impliedBidSize",
      headerName: "B Qty",
      initialFlex: 50,
      ...quantity,
      headerClass: "ice-centred-header ice-implied-first",
      cellClass: "ice-centred ice-implied-first",
    },
    impliedBid:  { field: "impliedBid",  headerName: "Bid",       initialFlex: 65, ...price },
    impliedAsk:  { field: "impliedAsk",  headerName: "Offer",     initialFlex: 65, ...price },
    impliedAskSize: { field: "impliedAskSize", headerName: "O Qty", initialFlex: 45, ...quantity },
  };

  return view.fields.map(f => columns[f]);
}
