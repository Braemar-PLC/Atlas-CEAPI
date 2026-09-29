import { fetchDesks, fetchMe } from "@atlas/external";
import type { Desk, Me } from "@atlas/data";
import { reloadIfSignedOut } from "./reload-if-signed-out";

/** Who is signed in and the desks that exist: what every page needs before it can draw the bar or decide where to go. */
export type Session = { me: Me; desks: Desk[] };

type Dependencies = {
  fetchMe: () => Promise<Me>;
  fetchDesks: () => Promise<Desk[]>;
  reloadIfSignedOut: () => Promise<boolean>;
};

const live: Dependencies = {
  fetchMe: () => fetchMe(),
  fetchDesks: () => fetchDesks(),
  reloadIfSignedOut: () => reloadIfSignedOut(),
};

let pending: Promise<Session> | undefined;

/**
 * Loads the session once and hands the same answer to every caller (the root route asks on every navigation).
 * If the API cannot answer, the usual reason is a sign-in that has expired, which reloads the page into sign-in;
 * otherwise the fault is reported and forgotten, so the next caller tries again.
 */
export function loadSession(deps: Dependencies = live): Promise<Session> {
  pending ??= load(deps).catch(async (error: unknown) => {
    pending = undefined;
    await deps.reloadIfSignedOut();
    throw error;
  });
  return pending;
}

/** Forgets the loaded session, e.g. after an admin changes a desk, so the next load asks the API again. */
export function resetSession(): void {
  pending = undefined;
}

async function load(deps: Dependencies): Promise<Session> {
  const [me, desks] = await Promise.all([deps.fetchMe(), deps.fetchDesks()]);
  return { me, desks };
}
