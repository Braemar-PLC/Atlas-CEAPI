import { describe, expect, it } from "vitest";
import { createMemoryHistory, createRouter, isRedirect } from "@tanstack/react-router";
import type { Me } from "@atlas/data";
import { routeTree } from "@/routes/routeTree.gen";
import { Route as HomeRoute } from "@/routes/index";

const me = (homeDesk: string | null): Me => ({ name: "Sean Hays", email: "sean.hays@braemar.com", isAdmin: true, desks: [], homeDesk });

/** The redirect the home address answers with, for a user. */
function landingRedirect(user: Me) {
  try {
    HomeRoute.options.beforeLoad!({ context: { session: { me: user, desks: [] } } } as never);
  } catch (thrown) {
    if (isRedirect(thrown)) return thrown;
    throw thrown;
  }
  throw new Error("the home address did not redirect");
}

describe("the home address (the Braemar logo)", () => {
  // Hovering a link preloads its page. A redirect met while preloading is followed by building a location from the
  // redirect's options, starting from the redirecting address. The router builds locations from `to`; given only an
  // `href`, it built the home address again, preloaded it, met the redirect again, and so on without end: hovering
  // the logo froze the tab, its memory climbing some 200 MB a second (2026-09-28).
  it("redirects in a form the router can follow from the home address itself", () => {
    const router = createRouter({ routeTree, history: createMemoryHistory({ initialEntries: ["/"] }) });

    const cases: [string | null, string][] = [["natural-gas", "/natgas"], ["power", "/desks/power"], [null, "/desks"]];
    for (const [homeDesk, landing] of cases) {
      const redirect = landingRedirect(me(homeDesk));
      expect(router.buildLocation(redirect.options).pathname).toBe(landing);
    }
  });
});
