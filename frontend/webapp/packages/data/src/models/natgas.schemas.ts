import { Type, type Static } from "@sinclair/typebox";

export const ExchangeSchema = Type.Union([
  Type.Literal("ICE"),
  Type.Literal("EEX"),
]);

export const CommoditySchema = Type.Union([
  Type.Literal("GAS"),
  Type.Literal("POWER"),
  Type.Literal("OIL"),
]);

export const InstrumentSchema = Type.Union([
  Type.Literal("NBP"),
  Type.Literal("TTF"),
  Type.Literal("PSV"),
  Type.Literal("ZTP"),
  Type.Literal("CEGH"),
]);

export const TickSchema = Type.Union([
  Type.Literal("up"),
  Type.Literal("down"),
]);

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