import { createFileRoute, redirect } from "@tanstack/react-router";
import { landingFor } from "@/application/auth/landing";

// There is no home page: the address on its own (and the logo) lands on your desk, or on the desk chooser.
export const Route = createFileRoute("/")({
  beforeLoad: ({ context }) => {
    throw redirect({ href: landingFor(context.session.me) });
  },
});
