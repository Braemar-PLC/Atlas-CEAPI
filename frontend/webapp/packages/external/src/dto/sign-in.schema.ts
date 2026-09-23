import { Type } from "@sinclair/typebox";

// What Azure App Service Authentication answers at GET /.auth/me: one entry per identity provider the user is
// signed in with (Atlas has one), `user_id` being the account's email/UPN. Fields we do not read are not listed.
export const SignInResponseSchema = Type.Array(
  Type.Object({
    user_id: Type.String(),
    user_claims: Type.Array(Type.Object({ typ: Type.String(), val: Type.String() })),
  }),
);
