import type { IHeaderGroupParams } from "ag-grid-community";
import { useNatGas } from "@atlas/data";
import type { Instrument } from "@atlas/data";
import { referencePriceOf } from "./calculator.view-model";

export type MatrixBlockHeaderParams = {
  /** The expiry as the desk names it: "X26". */
  code: string;
  hub: Instrument;
  /** What the chain files the expiry's future under in the store: "Nov26", "Q1 27", or the symbol itself. */
  futureLabel: string;
  decimals: number;
};

/**
 * The header of one expiry block: "X26 71.691" - the expiry and its future's traded price, as the desk's
 * calculator shows it. It watches that one future in the store, so it re-renders when that future ticks and the
 * grid's columns never have to be rebuilt for a price.
 */
export function MatrixBlockHeader(p: IHeaderGroupParams & MatrixBlockHeaderParams) {
  const quote = useNatGas(s => s.curves[p.hub]?.[p.futureLabel]);
  const price = referencePriceOf(quote)?.price;

  return (
    <span className="ice-matrix-block-header">
      {p.code}{price == null ? "" : ` ${price.toFixed(p.decimals)}`}
    </span>
  );
}
