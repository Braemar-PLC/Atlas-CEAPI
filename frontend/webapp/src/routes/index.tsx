
import { createFileRoute, redirect } from "@tanstack/react-router";

// There is no home page: the address on its own (and the logo) opens the first tab.
// It used to render StreamViewer, an empty shell, which is still in src/components but no longer used.
export const Route = createFileRoute("/")({
  beforeLoad: () => {
    throw redirect({ to: "/natgas" });
  },
});
