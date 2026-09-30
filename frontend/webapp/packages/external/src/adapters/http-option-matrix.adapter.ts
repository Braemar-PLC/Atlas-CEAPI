import { OptionMatrixSchema } from "@atlas/data";
import type { OptionMatrix } from "@atlas/data";
import { Value } from "@sinclair/typebox/value";

/**
 * Asks the Atlas API for a product's option matrix (GET /api/options/{product}): the expiries the desk's rules
 * pick and every strike ICE lists at each. Like everything that arrives from the network, the answer is checked
 * against its schema before it is used. `fetchFn` is only there so a test can stand in for the network.
 */
export async function fetchOptionMatrix(product: string, fetchFn: typeof fetch = fetch): Promise<OptionMatrix> {
  const response = await fetchFn(`/api/options/${encodeURIComponent(product)}`);

  if (!response.ok) {
    throw new Error(`The API has no option matrix for "${product}" (HTTP ${response.status}).`);
  }

  const raw: unknown = await response.json();

  if (!Value.Check(OptionMatrixSchema, raw)) {
    const first = [...Value.Errors(OptionMatrixSchema, raw)][0];
    throw new Error(`The API's option matrix for "${product}" is not the expected shape: ${first?.path} ${first?.message}`);
  }

  return raw;
}
