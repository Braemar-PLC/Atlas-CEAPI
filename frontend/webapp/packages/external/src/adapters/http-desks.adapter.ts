import { DeskDetailSchema, DeskSchema } from "@atlas/data";
import type { Desk, DeskDetail } from "@atlas/data";
import { Type, type TSchema, type Static } from "@sinclair/typebox";
import { Value } from "@sinclair/typebox/value";

/** What an admin may change about an existing desk; the key is in the address. */
export type DeskWrite = Omit<DeskDetail, "key">;

const DesksSchema = Type.Array(DeskSchema);

/** Every desk, in name order, without members (GET /api/desks). */
export function fetchDesks(fetchFn: typeof fetch = fetch): Promise<Desk[]> {
  return ask(fetchFn, "/api/desks", DesksSchema);
}

/** One desk with its members (GET /api/desks/{key}). */
export function fetchDesk(key: string, fetchFn: typeof fetch = fetch): Promise<DeskDetail> {
  return ask(fetchFn, `/api/desks/${encodeURIComponent(key)}`, DeskDetailSchema);
}

/** Adds a desk (POST /api/desks). The API refuses a taken key or bad input with a reason, passed on as the error. */
export function createDesk(desk: DeskDetail, fetchFn: typeof fetch = fetch): Promise<DeskDetail> {
  return ask(fetchFn, "/api/desks", DeskDetailSchema, { method: "POST", body: desk });
}

/** Changes a desk's name, description and members (PUT /api/desks/{key}). */
export function updateDesk(key: string, changes: DeskWrite, fetchFn: typeof fetch = fetch): Promise<DeskDetail> {
  return ask(fetchFn, `/api/desks/${encodeURIComponent(key)}`, DeskDetailSchema, { method: "PUT", body: changes });
}

async function ask<T extends TSchema>(
  fetchFn: typeof fetch,
  url: string,
  schema: T,
  request?: { method: string; body: unknown },
): Promise<Static<T>> {
  const response = request
    ? await fetchFn(url, { method: request.method, headers: { "content-type": "application/json" }, body: JSON.stringify(request.body) })
    : await fetchFn(url);

  if (!response.ok) {
    const reason = (await response.text()).trim();
    throw new Error(reason || `The API refused ${url} (HTTP ${response.status}).`);
  }

  const raw: unknown = await response.json();

  if (!Value.Check(schema, raw)) {
    const first = [...Value.Errors(schema, raw)][0];
    throw new Error(`The API's answer for ${url} is not the expected shape: ${first?.path} ${first?.message}`);
  }

  return raw;
}
