import type { ColDef } from "ag-grid-community";
import type { NatGasFieldKey } from "@/application/registries/natgas-domain";

export function buildNatGasColumnDefs(fields: NatGasFieldKey[]): ColDef[] {
  const num = (dp = 2) =>
    (p: any) => (p.value == null ? "" : Number(p.value).toFixed(dp));

  const base: Record<NatGasFieldKey | "symbol" | "tenor", ColDef> = {
    symbol:    { field: "symbol",    headerName: "Symbol" },
    tenor:     { field: "tenor",     headerName: "Tenor"  },
    bid:       { field: "bid",       headerName: "Bid",       valueFormatter: num(2) },
    ask:       { field: "ask",       headerName: "Ask",       valueFormatter: num(2) },
    last:      { field: "last",      headerName: "Last",      valueFormatter: num(2) },
    volume:    { field: "volume",    headerName: "Volume" },
    netChange: { field: "netChange", headerName: "Net Chg",   valueFormatter: num(2) },
  };

  return [base.symbol, base.tenor, ...fields.map((f) => base[f])];
}
