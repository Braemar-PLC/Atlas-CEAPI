import { Type, type Static } from "@sinclair/typebox";

// A desk as the API describes it (GET /api/desks): the key is the address-safe form ("natural-gas") and never
// changes; the name is what people see.
export const DeskSchema = Type.Object({
  key: Type.String(),
  name: Type.String(),
  description: Type.String(),
});

// A desk with its members (GET /api/desks/{key}; what POST and PUT send and return). Members are email addresses.
export const DeskDetailSchema = Type.Object({
  key: Type.String(),
  name: Type.String(),
  description: Type.String(),
  members: Type.Array(Type.String()),
});

// GENERATED TYPES
export type Desk = Static<typeof DeskSchema>;
export type DeskDetail = Static<typeof DeskDetailSchema>;
