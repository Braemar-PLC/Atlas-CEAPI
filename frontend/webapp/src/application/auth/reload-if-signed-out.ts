import { fetchSignIn } from "@atlas/external";
import type { SignIn } from "@atlas/external";

/**
 * Called when a request to the API has failed. If the reason is that the sign-in has expired, reload the page:
 * the gate then shows the sign-in page, whose arrow takes the user through the platform's sign-in and back to the
 * same address. Any other state - signed in
 * (so the fault is the API's) or no sign-in at all (the laptop) - leaves the page alone, which is what keeps a
 * dead API from causing a reload loop. Returns whether a reload was started.
 */
export async function reloadIfSignedOut(
  getSignIn: () => Promise<SignIn> = fetchSignIn,
  reload: () => void = () => window.location.reload(),
): Promise<boolean> {
  const signIn = await getSignIn();
  if (signIn.state !== "signedOut") {
    return false;
  }
  reload();
  return true;
}
