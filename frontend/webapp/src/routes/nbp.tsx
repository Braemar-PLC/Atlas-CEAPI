import { createFileRoute } from "@tanstack/react-router";
import ScreenModule from "@/application/modules/screen";

// NBP on one screen, as the desk asked: flat prices, then the spreads below (GET /api/screens/nbp).
export const Route = createFileRoute("/nbp")({
  component: () => <ScreenModule screenKey="nbp" />,
});
