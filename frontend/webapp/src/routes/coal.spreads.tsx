import { createFileRoute } from "@tanstack/react-router";
import ScreenModule from "@/application/modules/screen";

// Both coal hubs' calendar spreads, Rotterdam's then Newcastle's (GET /api/screens/coal-spreads).
export const Route = createFileRoute("/coal/spreads")({
  component: () => <ScreenModule screenKey="coal-spreads" />,
});
