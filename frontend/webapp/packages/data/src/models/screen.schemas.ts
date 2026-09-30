import { Type, type Static } from "@sinclair/typebox";
import { InstrumentSchema } from "./natgas.schemas";

// A desk screen as the API describes it (GET /api/screens/{key}): a title and its rows, top to bottom.
// The API decides which strips are on a screen - it knows ICE's expiry dates and the desk's counts - so the
// browser draws exactly these rows and nothing here needs changing when a contract expires and the list rolls.

export const ScreenLegSchema = Type.Object({
  label: Type.String(),   // the leg's strip name, e.g. "Oct26"
  symbol: Type.String(),  // the ICE symbol its prices arrive under
});

// What makes a row an option, on an options screen (the Cross-Commodities desk's chains). The one row per
// expiry without it is the future the options are priced against.
export const ScreenOptionSchema = Type.Object({
  expiryDate: Type.String(),       // the option's last trading day, "2026-10-27"
  underlyingSymbol: Type.String(), // the future it is priced against, "TFM 26X-ICN"
  strike: Type.Number(),
  right: Type.Union([Type.Literal("C"), Type.Literal("P")]),
});

export const ScreenRowSchema = Type.Object({
  hub: InstrumentSchema,
  label: Type.String(),   // what the Strip column shows: "Oct26", "Q4 26", "Oct26/Nov26", "Nov26 80.00 C"
  group: Type.String(),   // "Months" | "Quarters" | "Seasons" | "Cals" | "Spreads", or the expiry on an options screen
  // "quoted": ICE quotes this as a contract; its prices arrive under `symbol`.
  // "computed": no such contract exists (a month against a quarter, say); the row is worked out from `near` and `far`.
  source: Type.Union([Type.Literal("quoted"), Type.Literal("computed")]),
  symbol: Type.Union([Type.String(), Type.Null()]),
  near: Type.Union([ScreenLegSchema, Type.Null()]),
  far: Type.Union([ScreenLegSchema, Type.Null()]),
  // Absent or null on every row but an option's. Optional as well as nullable so older screens still check.
  option: Type.Optional(Type.Union([ScreenOptionSchema, Type.Null()])),
});

export const ScreenSchema = Type.Object({
  key: Type.String(),
  title: Type.String(),
  rows: Type.Array(ScreenRowSchema),
});

// GENERATED TYPES
export type ScreenLeg = Static<typeof ScreenLegSchema>;
export type ScreenOption = Static<typeof ScreenOptionSchema>;
export type ScreenRow = Static<typeof ScreenRowSchema>;
export type Screen = Static<typeof ScreenSchema>;

/** Which hub and strip a symbol's prices belong to. */
export type StripIdentity = { hub: ScreenRow["hub"]; label: string };

/**
 * Every symbol a screen listens to, with the hub and strip it feeds - quoted rows and the legs of worked-out rows.
 * The stream uses this to file each price under the right row. It comes from ICE's own contract list, so it also
 * works for spread contracts, where ICE may not send a strip name with the price.
 */
export function stripsBySymbol(screen: Screen): Map<string, StripIdentity> {
  const strips = new Map<string, StripIdentity>();
  for (const row of screen.rows) {
    if (row.symbol) strips.set(row.symbol, { hub: row.hub, label: row.label });
    for (const leg of [row.near, row.far]) {
      if (leg && !strips.has(leg.symbol)) strips.set(leg.symbol, { hub: row.hub, label: leg.label });
    }
  }
  return strips;
}

/** The API's group name for spread rows (ScreenBuilder.SpreadsGroup); every other group is an outright. */
export const SpreadsGroup = "Spreads";
