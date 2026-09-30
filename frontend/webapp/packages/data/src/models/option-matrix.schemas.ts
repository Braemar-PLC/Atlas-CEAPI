import { Type, type Static } from "@sinclair/typebox";

// The calculator's matrix for one product, as the API describes it (GET /api/options/{product}): every expiry the
// desk's rules pick - 12 months, 8 quarters, 8 seasons, 5 cals - and every strike ICE lists at each. No prices:
// the browser prices the matrix with Black-76, taking the futures' prices and the near-the-money quotes from the
// product's chain stream.

export const OptionMatrixExpirySchema = Type.Object({
  kind: Type.Union([Type.Literal("Month"), Type.Literal("Quarter"), Type.Literal("Season"), Type.Literal("Cal")]),
  label: Type.String(),            // "Nov26", "Q1 27", "Winter26", "Cal 27"
  expiryDate: Type.String(),       // the option's last trading day, "2026-10-27"
  underlyingSymbol: Type.String(), // the future it is priced against, "TFM 26X-ICN" or the strip future "TFMQ 27F-ICN"
  strikes: Type.Array(Type.Number()),
});

export const OptionMatrixSchema = Type.Object({
  product: Type.String(),
  expiries: Type.Array(OptionMatrixExpirySchema),
});

// GENERATED TYPES
export type OptionMatrixExpiry = Static<typeof OptionMatrixExpirySchema>;
export type OptionMatrix = Static<typeof OptionMatrixSchema>;
