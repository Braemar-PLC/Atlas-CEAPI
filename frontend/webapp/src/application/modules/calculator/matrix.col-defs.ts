import type { CellClassParams, ColDef, ColGroupDef, ValueFormatterParams } from "ag-grid-community";
import { stripsBySymbol } from "@atlas/data";
import type { Instrument, OptionMatrix, Screen } from "@atlas/data";
import type { ChainView } from "@/application/registries/chain-views";
import { contractCode } from "./contract-code";
import { MatrixBlockHeader } from "./matrix.block-header";
import type { MatrixBlockHeaderParams } from "./matrix.block-header";
import type { MatrixCell, MatrixGridRow } from "./matrix.view-model";

/**
 * The matrix's columns, as the desk's calculator lays them out: one block per expiry - months, then quarters,
 * seasons and cals - each with its own strike column, the call's theo and delta, the put's theo and delta, and the
 * vol at that strike. Scrolling right reaches the later expiries. The block's header is the expiry as the desk
 * names it and its future's live price ("X26 71.691"), drawn by MatrixBlockHeader from the store.
 * Deltas are whole percents in brackets, "(97)"; the vol is a percent to one decimal, "88.6"; a vol read off the
 * market carries `ice-matrix-market-vol`, one borrowed from another expiry `ice-matrix-borrowed-vol`.
 * The columns depend on the matrix's expiries and the chain's row labels, so they are rebuilt when either changes,
 * never on a price tick.
 */
export function buildMatrixColumnDefs(matrix: OptionMatrix, screen: Screen | undefined, view: ChainView): (ColDef | ColGroupDef)[] {
  const price = (p: ValueFormatterParams) => (p.value == null ? "" : Number(p.value).toFixed(view.priceDecimals));
  const delta = (p: ValueFormatterParams) => (p.value == null ? "" : `(${Math.round(Number(p.value))})`);
  const vol = (p: ValueFormatterParams) => (p.value == null ? "" : Number(p.value).toFixed(1));
  const strike = (p: ValueFormatterParams) => (p.value == null ? "" : Number(p.value).toFixed(2));

  const centred: ColDef = { headerClass: "ice-centred-header", cellClass: "ice-centred" };
  const cellOf = (p: CellClassParams<MatrixGridRow>, label: string): MatrixCell | undefined => p.data?.cells?.[label];

  // The store files the future under the label the chain gave it; the product's name is also its hub's.
  const futureLabels = screen ? stripsBySymbol(screen) : undefined;
  const hub = (screen?.rows[0]?.hub ?? matrix.product) as Instrument;

  return matrix.expiries.map((expiry): ColGroupDef => {
    const label = expiry.label;
    const field = (key: keyof MatrixCell) => `cells.${label}.${key}`;
    const headerParams: MatrixBlockHeaderParams = {
      code: contractCode(label),
      hub,
      futureLabel: futureLabels?.get(expiry.underlyingSymbol)?.label ?? expiry.underlyingSymbol,
      decimals: view.priceDecimals,
    };
    return {
      headerName: headerParams.code,
      headerClass: `ice-chain-group ice-matrix-${expiry.kind.toLowerCase()}`,
      headerGroupComponent: MatrixBlockHeader,
      headerGroupComponentParams: headerParams,
      children: [
        { field: field("strike"), headerName: "Strike", width: 64, ...centred, valueFormatter: strike, cellClass: "ice-centred ice-chain-strike ice-matrix-first" },
        { field: field("call"), headerName: "Call", width: 72, ...centred, valueFormatter: price },
        { field: field("callDelta"), headerName: "Δ", width: 52, ...centred, valueFormatter: delta },
        { field: field("put"), headerName: "Put", width: 72, ...centred, valueFormatter: price },
        { field: field("putDelta"), headerName: "Δ", width: 52, ...centred, valueFormatter: delta },
        {
          field: field("vol"),
          headerName: "Vol",
          width: 60,
          ...centred,
          valueFormatter: vol,
          cellClassRules: {
            "ice-matrix-market-vol": (p: CellClassParams<MatrixGridRow>) => cellOf(p, label)?.volSource === "market",
            "ice-matrix-borrowed-vol": (p: CellClassParams<MatrixGridRow>) => cellOf(p, label)?.volSource === "borrowed",
          },
        },
      ],
    };
  });
}
