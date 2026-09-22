
import { Type } from "@sinclair/typebox";

export const NatGasQuoteSchema = Type.Object({
  exchange: Type.Union([Type.Literal("ICE"), Type.Literal("EEX")]),
  commodity: Type.Union([
    Type.Literal("GAS"),
    Type.Literal("POWER"),
    Type.Literal("OIL"),
  ]),
  hub: Type.Union([
    Type.Literal("NBP"),
    Type.Literal("TTF"),
    Type.Literal("PSV"),
    Type.Literal("ZTP"),
    Type.Literal("CEGH"),
  ]),
  tenor: Type.String(),
  bid: Type.Union([Type.Number(), Type.Null()]),
  ask: Type.Union([Type.Number(), Type.Null()]),
  last: Type.Union([Type.Number(), Type.Null()]),
  netChange: Type.Union([Type.Number(), Type.Null()]),
  prevSettle: Type.Union([Type.Number(), Type.Null()]),
  volume: Type.Union([Type.Number(), Type.Null()]),
});
