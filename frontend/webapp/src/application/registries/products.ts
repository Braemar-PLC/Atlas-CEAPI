import { SpreadsGroup } from "@atlas/data";
import type { Instrument } from "@atlas/data";

// Product names as WebICE's Product column shows them. The API does not carry them: a hub's outrights are one
// product and its spreads another, named as ICE describes the contracts ("Rotterdam Coal Futures - ARA - Jan27",
// "Newcastle Coal Futures Spr - Newcastle - Jan27/Feb27"). Only the coal screens have a Product column.

const Products: Partial<Record<Instrument, { outrights: string; spreads: string }>> = {
  ARA: { outrights: "Rotterdam Coal Futures", spreads: "Rotterdam Coal Spr" },
  Newcastle: { outrights: "Newcastle Coal Futures", spreads: "Newcastle Coal Futures Spr" },
};

/** The product a row belongs to; undefined for a hub whose screens show no Product column. */
export function productOf(hub: Instrument, group: string): string | undefined {
  const names = Products[hub];
  if (!names) {
    return undefined;
  }
  return group === SpreadsGroup ? names.spreads : names.outrights;
}
