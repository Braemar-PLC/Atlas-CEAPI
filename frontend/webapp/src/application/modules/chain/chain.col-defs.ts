import type { CellClassParams, ColDef, ColGroupDef, ColSpanParams, ValueFormatterParams } from "ag-grid-community";
import type { ChainView } from "@/application/registries/chain-views";
import type { ChainGridRow } from "./chain.view-model";

/**
 * An option chain's columns, as the desk's calculator lays a ladder out: the calls on the left, the strike in the
 * middle, the puts on the right, each side's bid and offer nearest the strike. The sides read outwards -
 * Vol, Settle, Last, B Qty, Bid, Offer, O Qty | Strike | B Qty, Bid, Offer, O Qty, Last, Settle, Vol - so a
 * reader's eye moves the same way from the strike on either side.
 *
 * The future row at the head of each expiry keeps its figures in the left-hand columns and its heading in the
 * strike column, stretched across the put side (`colSpan`), where a strike row would show puts.
 *
 * Formats follow screen.col-defs.ts: prices to the view's decimals, quantities whole, volumes blank at 0, and only
 * Last flashes. Every column has its own field (call.bid, put.bid) so saved widths and flashes never clash.
 */
export function buildChainColumnDefs(view: ChainView): (ColDef | ColGroupDef)[] {
  const num = (dp: number) =>
    (p: ValueFormatterParams) => (p.value == null ? "" : Number(p.value).toFixed(dp));
  const traded = (p: ValueFormatterParams) =>
    (p.value == null || Number(p.value) === 0 ? "" : Number(p.value).toFixed(0));

  const centred: ColDef = { headerClass: "ice-centred-header", cellClass: "ice-centred" };
  const price: ColDef = { ...centred, valueFormatter: num(view.priceDecimals) };
  const quantity: ColDef = { ...centred, valueFormatter: num(0) };
  const volume: ColDef = { ...centred, valueFormatter: traded };

  const columnsOf = (side: "call" | "put"): Record<string, ColDef> => ({
    bidSize: { field: `${side}.bidSize`, headerName: "B Qty", initialFlex: 50, ...quantity },
    bid:     { field: `${side}.bid`,     headerName: "Bid",   initialFlex: 65, ...price },
    ask:     { field: `${side}.ask`,     headerName: "Offer", initialFlex: 65, ...price },
    askSize: { field: `${side}.askSize`, headerName: "O Qty", initialFlex: 45, ...quantity },
    last: {
      field: `${side}.last`,
      headerName: "Last",
      initialFlex: 70,
      ...price,
      enableCellChangeFlash: true,
      cellClass: "ice-centred ice-last",
      cellClassRules: {
        "ice-tick-up": (p: CellClassParams<ChainGridRow>) => p.data?.[side]?.tick === "up",
        "ice-tick-down": (p: CellClassParams<ChainGridRow>) => p.data?.[side]?.tick === "down",
      },
    },
    settle:  { field: `${side}.settle`,  headerName: "Settle", initialFlex: 65, ...price },
    volume:  { field: `${side}.volume`,  headerName: "Vol",    initialFlex: 50, ...volume },
  });

  const calls = columnsOf("call");
  const puts = columnsOf("put");
  const putColumns = [puts.bidSize, puts.bid, puts.ask, puts.askSize, puts.last, puts.settle, puts.volume];

  const strike: ColDef = {
    field: "label",
    headerName: "Strike",
    initialFlex: 80,
    headerClass: "ice-centred-header",
    cellClass: "ice-centred ice-chain-strike",
    cellClassRules: {
      "ice-chain-heading": (p: CellClassParams<ChainGridRow>) => p.data?.kind === "future",
    },
    // The future's heading takes the strike column and the whole put side.
    colSpan: (p: ColSpanParams<ChainGridRow>) => (p.data?.kind === "future" ? 1 + putColumns.length : 1),
  };

  return [
    {
      headerName: "Calls",
      headerClass: "ice-chain-group",
      children: [calls.volume, calls.settle, calls.last, calls.bidSize, calls.bid, calls.ask, calls.askSize],
    },
    strike,
    { headerName: "Puts", headerClass: "ice-chain-group", children: putColumns },
  ];
}
