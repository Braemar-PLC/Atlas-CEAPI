import { createFileRoute, Outlet } from "@tanstack/react-router";

// The chooser (/desks) and a desk's own page (/desks/{key}) live under here.
export const Route = createFileRoute("/desks")({
  component: () => <Outlet />,
});
