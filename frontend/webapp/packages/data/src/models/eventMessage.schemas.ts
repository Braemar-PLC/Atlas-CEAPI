import { Type, type Static } from "@sinclair/typebox";
import { NatGasQuoteSchema, NatGasCurveMapSchema } from "./natgas.schemas";

export const EventMessageTypes = {
  NatGasSnapshot: "FUTURES_SNAPSHOT",
  NatGasDelta: "FUTURES_DELTA",
} as const;

export const SnapshotMessageSchema = Type.Object({
  type: Type.Literal(EventMessageTypes.NatGasSnapshot),
  data: NatGasCurveMapSchema,
});

export const DeltaMessageSchema = Type.Object({
  type: Type.Literal(EventMessageTypes.NatGasDelta),
  data: Type.Partial(NatGasQuoteSchema),
});

export const NatGasEventMessageSchema = Type.Union([
  SnapshotMessageSchema,
  DeltaMessageSchema,
]);

export type NatGasSnapshotMessage = Static<typeof SnapshotMessageSchema>;
export type NatGasDeltaMessage = Static<typeof DeltaMessageSchema>;
export type NatGasEventMessage = Static<typeof NatGasEventMessageSchema>;