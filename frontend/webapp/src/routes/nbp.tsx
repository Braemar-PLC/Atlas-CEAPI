import { createFileRoute } from "@tanstack/react-router";
import NatGasModule from "@/application/modules/natgas";

// NBP on one screen, as the desk asked: flat prices, then the spreads below (GET /api/screens/nbp).
export const Route = createFileRoute("/nbp")({
  component: () => <NatGasModule screenKey="nbp" />,
});
