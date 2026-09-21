import { useNatGas } from "@atlas/data";
import { NatGasViews } from "@/application/registries/natgas-views";
import { buildNatGasColumnDefs } from "./natgas.col-defs";


/**
 * VM = domain state → UI props
 * No SSE here. No lifecycle. No React other than selectors.
 */
export function useNatGasViewModel() {
  const curves = useNatGas(s => s.curves);

  // TEMP: use first view until routing drives view selection
  const view = NatGasViews[0];

  const rows = Object.entries(curves)
    .filter(([symbol]) => view.symbols.includes(symbol))
    .flatMap(([symbol, tenors]) =>
      Object.entries(tenors ?? {}).map(([tenor, q]) => ({
        symbol,
        tenor,
        ...q
      }))
    );

  const colDefs = buildNatGasColumnDefs(view.fields ?? []);

  return { rows, colDefs };
}