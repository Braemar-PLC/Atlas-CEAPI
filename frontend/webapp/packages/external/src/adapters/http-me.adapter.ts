import { MeSchema } from "@atlas/data";
import type { Me } from "@atlas/data";
import { Value } from "@sinclair/typebox/value";

/**
 * Asks the Atlas API who is signed in (GET /api/me). The API answers 401 when nobody is, which arrives here as an
 * error naming the status. `fetchFn` is only there so a test can stand in for the network.
 */
export async function fetchMe(fetchFn: typeof fetch = fetch): Promise<Me> {
  const response = await fetchFn("/api/me");

  if (!response.ok) {
    throw new Error(`The API would not say who is signed in (HTTP ${response.status}).`);
  }

  const raw: unknown = await response.json();

  if (!Value.Check(MeSchema, raw)) {
    const first = [...Value.Errors(MeSchema, raw)][0];
    throw new Error(`The API's answer for /api/me is not the expected shape: ${first?.path} ${first?.message}`);
  }

  return raw;
}
