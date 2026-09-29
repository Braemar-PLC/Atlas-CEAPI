import { Type, type Static } from "@sinclair/typebox";
import { DeskSchema } from "./desk.schemas";

// The signed-in user as the API describes them (GET /api/me). homeDesk is the desk they land on: set only when
// they are a member of exactly one desk, null otherwise (then they get the desk chooser).
export const MeSchema = Type.Object({
  name: Type.String(),
  email: Type.String(),
  isAdmin: Type.Boolean(),
  desks: Type.Array(DeskSchema),
  homeDesk: Type.Union([Type.String(), Type.Null()]),
});

// GENERATED TYPES
export type Me = Static<typeof MeSchema>;
