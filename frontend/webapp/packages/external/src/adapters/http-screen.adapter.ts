import { ScreenSchema } from "@atlas/data";
import type { Screen } from "@atlas/data";
import { Value } from "@sinclair/typebox/value";

/**
 * Asks the Atlas API which rows a desk screen shows today (GET /api/screens/{key}).
 * Like everything that arrives from the network, the answer is checked against its schema before it is used.
 * `fetchFn` is only there so a test can stand in for the network.
 */
export async function fetchScreen(key: string, fetchFn: typeof fetch = fetch): Promise<Screen> {
  const response = await fetchFn(`/api/screens/${encodeURIComponent(key)}`);

  if (!response.ok) {
    throw new Error(`The API has no screen "${key}" (HTTP ${response.status}).`);
  }

  const raw: unknown = await response.json();

  if (!Value.Check(ScreenSchema, raw)) {
    const first = [...Value.Errors(ScreenSchema, raw)][0];
    throw new Error(`The API's answer for screen "${key}" is not the expected shape: ${first?.path} ${first?.message}`);
  }

  return raw;
}
