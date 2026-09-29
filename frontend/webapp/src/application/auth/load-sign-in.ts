import { fetchSignIn } from "@atlas/external";
import type { SignIn } from "@atlas/external";

let pending: Promise<SignIn> | undefined;

/**
 * Asks the platform once who is signed in and hands the same answer to every caller (the gate in front of the app
 * and the top bar both ask). A failed ask is forgotten, so the next caller tries again.
 */
export function loadSignIn(ask: () => Promise<SignIn> = fetchSignIn): Promise<SignIn> {
  pending ??= ask().catch((error: unknown) => {
    pending = undefined;
    throw error;
  });
  return pending;
}

/** Forgets the answer, so the next caller asks again. Tests only. */
export function resetSignIn(): void {
  pending = undefined;
}
