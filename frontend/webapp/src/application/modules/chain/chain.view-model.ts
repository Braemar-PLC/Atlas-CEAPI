import { useMemo } from "react";
import { useNatGas } from "@atlas/data";
import type { NatGasCurveMap, NatGasQuote, Screen } from "@atlas/data";
import type { RowClassRules } from "ag-grid-community";
import { chainView } from "@/application/registries/chain-views";
import { delayMinutesOf } from "@/application/modules/screen/screen.view-model";
import { buildChainColumnDefs } from "./chain.col-defs";

type Quote = Partial<NatGasQuote>;

/**
 * One grid row of an option chain. The API sends a chain as a flat list of rows - each expiry's future, then one
 * row per option ("Nov26 80.00 C", "Nov26 80.00 P", ...) - and the desk reads it as a ladder: the future at the
 * head of each expiry, then one row per strike with the call's figures on the left and the put's on the right.
 * `symbol` (the expiry) and `tenor` (the strike, or "future") identify the row to the grid, as on the other screens.
 * On the future row the future's own figures sit in `call`, so they show in the left-hand columns.
 */
export type ChainGridRow = {
  kind: "future" | "strike";
  symbol: string;
  tenor: string;
  /** The middle column: the strike ("80.00"), or on the future row a heading ("Nov26 options on Nov26 TFM 26X-ICN"). */
  label: string;
  call?: Quote;
  put?: Quote;
};

/** Marks the future rows so the stylesheet can set them apart. A constant, as the grid component requires. */
export const chainRowClassRules: RowClassRules<ChainGridRow> = {
  "ice-chain-future": p => p.data?.kind === "future",
};

/**
 * Store → grid rows for one chain. The rows come in the API's order (expiries by date, strikes rising), so the
 * ladder needs no sorting here: a strike's first row (call or put) opens its grid row and the other fills it in.
 * Every strike the API lists is shown even before it has a price, as on the other screens.
 */
export function buildChainRows(curves: NatGasCurveMap, screen: Screen): ChainGridRow[] {
  const rows: ChainGridRow[] = [];
  const byStrike = new Map<string, ChainGridRow>();

  for (const row of screen.rows) {
    const quote = curves[row.hub]?.[row.label] ?? {};

    if (!row.option) {
      rows.push({
        kind: "future",
        symbol: row.group,
        tenor: "future",
        label: `${row.group} options on ${row.label} ${row.symbol ?? ""}`.trim(),
        call: quote,
      });
      continue;
    }

    const strike = row.option.strike.toFixed(2);
    const key = `${row.group}|${strike}`;
    let ladder = byStrike.get(key);
    if (!ladder) {
      ladder = { kind: "strike", symbol: row.group, tenor: strike, label: strike };
      byStrike.set(key, ladder);
      rows.push(ladder);
    }
    if (row.option.right === "C") {
      ladder.call = quote;
    } else {
      ladder.put = quote;
    }
  }

  return rows;
}

/** Every quote on the chain, for the "Prices as of" clock: the screen is as delayed as its most delayed row. */
function quotesOf(rows: ChainGridRow[]): Quote[] {
  return rows.flatMap(r => [r.call, r.put]).filter((q): q is Quote => q != null);
}

/**
 * VM = domain state → UI props, as the screen module's. `screen` is undefined until the API has answered, and the
 * grid is empty until then.
 */
export function useChainViewModel(screenKey: string, screen: Screen | undefined) {
  const curves = useNatGas(s => s.curves);
  const view = chainView(screenKey);
  // Built once per chain, not per render: a new array on every price tick would make AG Grid rebuild its columns.
  const colDefs = useMemo(() => buildChainColumnDefs(view), [view]);
  const rows = screen ? buildChainRows(curves, screen) : [];

  return { title: screen?.title ?? "", rows, colDefs, delayMinutes: delayMinutesOf(quotesOf(rows)) };
}
