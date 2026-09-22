import type { ColDef } from "ag-grid-community";
import type { NatGasFieldKey } from "@/application/registries/natgas-domain";

/**
 * Columns follow the ICE screen: its header names, 3-decimal prices, right-aligned figures.
 * "bidBar" and "askBar" are not data: they are the thin red and green bars ICE draws beside the
 * bid side and the offer side. A view lists them in `fields` wherever the bar should sit.
 * Styling for the `ice-*` classes is in natgas.css.
 *
 * `flex` is each column's share of the width, measured off the mock the desk approved (its pixel
 * widths at 800px across), so Strip is wide and the quantity columns are narrow, as on ICE.
 * It is `initialFlex`, NOT `flex`, on purpose: "initial" values are used once, when the column is created.
 * A plain `flex` is re-applied every time AG Grid re-reads the column definitions, which wiped out a width the
 * user had just dragged a fraction of a second later, on the next price tick (bug found by Sean, 21 Sep 2026;
 * test/modules/natgas.component.test.tsx reproduces it).
 * Strip was 115; it is 135 since the figures went to 18px (21 Sep 2026), so the longest spread label
 * ("Winter28/Summer29") still fits on a laptop. These are only the starting widths: a user can drag any column
 * (not the bars), and the grid remembers it - see natgas.component.tsx.
 *
 * Only Last flashes when it changes (yellow, ~0.3s — the desk's ask; colour and timing are in
 * natgas.theme.ts and natgas.component.tsx). Never put `enableCellChangeFlash` in the grid's
 * defaultColDef or on the bars: AG Grid treats a column with no `field` as changed on every refresh and
 * paints the flash with `!important`, which turned both bars permanently grey (18 Sep 2026).
 */
export function buildNatGasColumnDefs(fields: NatGasFieldKey[], showSymbol = true): ColDef[] {
  const num = (dp = 2) =>
    (p: any) => (p.value == null ? "" : Number(p.value).toFixed(dp));

  const price: ColDef = { type: "rightAligned", valueFormatter: num(3) };
  const quantity: ColDef = { type: "rightAligned", valueFormatter: num(0) };

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

  const base: Record<NatGasFieldKey | "symbol" | "tenor", ColDef> = {
    symbol:      { field: "symbol",      headerName: "Symbol",    initialFlex: 60 },
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
      // Setting cellClass replaces the one "rightAligned" supplies, so it is repeated here.
      cellClass: "ag-right-aligned-cell ice-last",
      cellClassRules: {
        "ice-tick-up": p => p.data?.tick === "up",
        "ice-tick-down": p => p.data?.tick === "down",
      },
    },
    netChange:   { field: "netChange",   headerName: "Change",    initialFlex: 60, ...price },
    settle:      { field: "settle",      headerName: "Settle",    initialFlex: 65, ...price },
    high:        { field: "high",        headerName: "High",      initialFlex: 70, ...price },
    low:         { field: "low",         headerName: "Low",       initialFlex: 70, ...price },
    volume:      { field: "volume",      headerName: "Volume",    initialFlex: 50, ...quantity },
    blockVolume: { field: "blockVolume", headerName: "Block Vol", initialFlex: 55, ...quantity },
  };

  return [
    ...(showSymbol ? [base.symbol] : []),
    base.tenor,
    ...fields.map((f) => base[f]),
  ];
}
