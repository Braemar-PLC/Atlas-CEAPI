import { createFileRoute, redirect } from "@tanstack/react-router";
import { landingFor } from "@/application/auth/landing";

// There is no home page: the address on its own (and the logo) lands on your desk, or on the desk chooser.
// The redirect names its destination with `to`, never `href`: hovering a link preloads its page, and the router
// follows a redirect met while preloading by building a location from `to` alone - given only an `href` it built
// this address again and preloaded it for ever, freezing the tab (test/routes/home.test.ts).
export const Route = createFileRoute("/")({
  beforeLoad: ({ context }) => {
    throw redirect({ to: landingFor(context.session.me) });
  },
});
