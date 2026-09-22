
import { Value } from "@sinclair/typebox/value";

import { EventMessageTypes, InstrumentSchema } from "@atlas/data";
import type { NatGasEventMessage, NatGasQuote, StripIdentity } from "@atlas/data";
import type { PricingStreamEventDto } from "../dto/pricing-stream-event.dto";

// ICE field IDs, as named by the LRT_TYPE_* constants in the JStandard SDK javadoc
// (docs/javadoc/constant-values.html).
export const IceFieldIds = {
  last: "19",        // LRT_TYPE_LAST
  bid: "20",         // LRT_TYPE_BID
  ask: "21",         // LRT_TYPE_ASK
  high: "22",        // LRT_TYPE_HIGH
  low: "23",         // LRT_TYPE_LOW
  prevSettle: "25",  // LRT_TYPE_PREV
  netChange: "26",   // LRT_TYPE_CHANGE
  volume: "29",      // LRT_TYPE_TOTALVOL
  delayMinutes: "47",// LRT_TYPE_MINUTESDELAYED — 10 on the Sep 2026 trial, which is a delayed feed
  bidSize: "30",     // LRT_TYPE_BIDSIZE — absent from the recorded (delayed) feed
  askSize: "31",     // LRT_TYPE_ASKSIZE — absent from the recorded (delayed) feed
  settle: "273",     // LRT_TYPE_SETTLEMENT
  blockVolume: "924",// LRT_TYPE_BLOCK_VOL
  hub: "951",        // LRT_TYPE_HUB, e.g. "TTF"
  stripName: "971",  // LRT_TYPE_STRIP_NAME, e.g. "Apr26"
} as const;

// Mirrors the API's PricingEventActions enum, which is serialized as an integer.
export const PricingEventActions = {
  Update: 0,
  Reset: 1,
  Remove: 2,
} as const;

const NumericQuoteKeys = [
  "bid", "ask", "last", "netChange", "prevSettle", "volume",
  "bidSize", "askSize", "high", "low", "settle", "blockVolume", "delayMinutes",
] as const;

const toNumber = (v: string | undefined): number | undefined => {
  if (v == null || v.trim() === "") return undefined;
  const n = Number(v);
  return Number.isFinite(n) ? n : undefined;
};

/**
 * Maps one API snapshot event to a store message.
 * Returns undefined when there is nothing to apply: a Reset/Remove event, or a snapshot that does
 * not yet say which hub and month it belongs to (the API sends an empty one before data arrives).
 *
 * `strips` says which hub and strip each symbol feeds (see stripsBySymbol in @atlas/data). When the symbol is
 * in it, that answer wins: it comes from ICE's own contract list, so it is right even for spread contracts,
 * where ICE may not send fields 951/971. Without it, or for a symbol it does not know, the hub and strip name
 * are read from those two fields as before.
 */
export function mapPricingStreamEvent(
  dto: PricingStreamEventDto,
  strips?: ReadonlyMap<string, StripIdentity>
): NatGasEventMessage | undefined {
  if (dto.metadata.action !== PricingEventActions.Update) return undefined;

  const { fields } = dto.data;
  const known = strips?.get(dto.data.symbol);
  const hub = known?.hub ?? fields[IceFieldIds.hub];
  const stripName = known?.label ?? fields[IceFieldIds.stripName];
  if (!Value.Check(InstrumentSchema, hub) || !stripName) return undefined;

  // The API sends an empty snapshot for a symbol it has no prices for yet. Nothing to apply.
  if (Object.keys(fields).length === 0) return undefined;

  const quote: Partial<NatGasQuote> = {
    exchange: "ICE",
    commodity: "GAS",
    instrument: hub,
    // ICE's strip name, verbatim ("Oct26", "Winter26", "Q4 26") — it is the row label on the ICE screen.
    tenor: stripName,
  };

  for (const key of NumericQuoteKeys) {
    const n = toNumber(fields[IceFieldIds[key]]);
    if (n !== undefined) quote[key] = n;
  }

  return { type: EventMessageTypes.NatGasDelta, data: quote };
}
