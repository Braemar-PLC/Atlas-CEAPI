import { Type, type Static } from "@sinclair/typebox";

export const ExchangeSchema = Type.Union([
  Type.Literal("ICE"),
  Type.Literal("EEX"),
]);

export const CommoditySchema = Type.Union([
  Type.Literal("GAS"),
  Type.Literal("POWER"),
  Type.Literal("OIL"),
  Type.Literal("COAL"),
  Type.Literal("CARBON"),
]);

// The hubs, spelt as ICE spells them (field 951 and the catalogue): the gas hubs; since 2026-09-24 the two coal
// hubs - ARA (Rotterdam, the API2 contract) and Newcastle; and since 2026-09-25 the futures the options desk
// prices off - EUA (carbon), Brent and WTI (oil) - whose option chains file under the same names.
export const InstrumentSchema = Type.Union([
  Type.Literal("NBP"),
  Type.Literal("TTF"),
  Type.Literal("PSV"),
  Type.Literal("ZTP"),
  Type.Literal("CEGH"),
  Type.Literal("ARA"),
  Type.Literal("Newcastle"),
  Type.Literal("EUA"),
  Type.Literal("Brent"),
  Type.Literal("WTI"),
]);

export const TickSchema = Type.Union([
  Type.Literal("up"),
  Type.Literal("down"),
]);

// One quote shape serves every hub, coal included, whatever the name says.
export const NatGasQuoteSchema = Type.Object({
  exchange: ExchangeSchema,
  commodity: CommoditySchema,
  instrument: InstrumentSchema,
  tenor: Type.String(),

  bid: Type.Optional(Type.Number()),
  ask: Type.Optional(Type.Number()),
  last: Type.Optional(Type.Number()),
  netChange: Type.Optional(Type.Number()),
  prevSettle: Type.Optional(Type.Number()),
  volume: Type.Optional(Type.Number()),

  bidSize: Type.Optional(Type.Number()),
  askSize: Type.Optional(Type.Number()),
  high: Type.Optional(Type.Number()),
  low: Type.Optional(Type.Number()),
  settle: Type.Optional(Type.Number()),
  blockVolume: Type.Optional(Type.Number()),

  // Open interest and the volume-weighted average price: "OI" and "WAP" on WebICE's coal screen.
  openInterest: Type.Optional(Type.Number()),
  wap: Type.Optional(Type.Number()),

  // What the spread markets make available at the implied bid and offer (ICE fields 581 and 582). The implied
  // prices themselves are worked out in the browser from the spread rows - ICE sends no field for them.
  impliedBidSize: Type.Optional(Type.Number()),
  impliedAskSize: Type.Optional(Type.Number()),

  // How many minutes behind the exchange this quote is, as the source reports it (0 = real-time).
  delayMinutes: Type.Optional(Type.Number()),

  // Direction of the latest change in `last`. Worked out by the store, never sent by a source.
  tick: Type.Optional(TickSchema),
});

export const NatGasCurveMapSchema = Type.Record(
  InstrumentSchema,
  Type.Optional(Type.Record(Type.String(), Type.Partial(NatGasQuoteSchema)))
);

// GENERATED TYPES
export type NatGasQuote = Static<typeof NatGasQuoteSchema>;
export type NatGasCurveMap = Static<typeof NatGasCurveMapSchema>;
export type Exchange = Static<typeof ExchangeSchema>;
export type Commodity = Static<typeof CommoditySchema>;
export type Instrument = Static<typeof InstrumentSchema>;
export type Tick = Static<typeof TickSchema>;

// Which commodity each hub trades. Exhaustive on purpose: a hub added to InstrumentSchema without a line here
// fails the type-check.
export const CommodityByHub: Record<Instrument, Commodity> = {
  NBP: "GAS",
  TTF: "GAS",
  PSV: "GAS",
  ZTP: "GAS",
  CEGH: "GAS",
  ARA: "COAL",
  Newcastle: "COAL",
  EUA: "CARBON",
  Brent: "OIL",
  WTI: "OIL",
};

export const commodityOf = (hub: Instrument): Commodity => CommodityByHub[hub];
