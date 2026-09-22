
import { Type } from "@sinclair/typebox";

// One `event: snapshot` from the Atlas API (GET /api/pricing/stream).
// `type` and `action` are .NET enums, which the API serializes as integers.
// `fields` is keyed by ICE field ID; every value arrives as a string.
export const PricingStreamEventSchema = Type.Object({
  metadata: Type.Object({
    key: Type.String(),
    type: Type.Number(),
    action: Type.Number(),
    serverTimestamp: Type.String(),
  }),
  data: Type.Object({
    symbol: Type.String(),
    fields: Type.Record(Type.String(), Type.String()),
  }),
});
