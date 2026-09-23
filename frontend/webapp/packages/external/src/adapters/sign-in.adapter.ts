import { Value } from "@sinclair/typebox/value";
import { SignInResponseSchema } from "../dto/sign-in.schema";

export type SignIn =
  | { state: "signedIn"; name: string; email: string }
  | { state: "signedOut" }
  | { state: "notConfigured" };

/**
 * Who is signed in, from the platform's own endpoint (GET /.auth/me, Azure App Service Authentication).
 * "notConfigured" means nothing answered as that platform would: on the laptop there is no sign-in at all and the
 * dev server returns the page itself for that address. The app then behaves as if sign-in did not exist.
 * `fetchFn` is only there so a test can stand in for the network.
 */
export async function fetchSignIn(fetchFn: typeof fetch = fetch): Promise<SignIn> {
  const response = await fetchFn("/.auth/me");

  if (response.status === 401) {
    return { state: "signedOut" };
  }
  const contentType = response.headers.get("content-type") ?? "";
  if (!response.ok || !contentType.includes("application/json")) {
    return { state: "notConfigured" };
  }

  const raw: unknown = await response.json();

  if (!Value.Check(SignInResponseSchema, raw)) {
    const first = [...Value.Errors(SignInResponseSchema, raw)][0];
    throw new Error(`The platform's answer for /.auth/me is not the expected shape: ${first?.path} ${first?.message}`);
  }
  if (raw.length === 0) {
    return { state: "signedOut" };
  }

  const email = raw[0].user_id;
  const name = raw[0].user_claims.find(c => c.typ === "name")?.val ?? email;
  return { state: "signedIn", name, email };
}
