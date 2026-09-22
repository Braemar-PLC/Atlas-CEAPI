import { createFileRoute } from "@tanstack/react-router";
import NatGasModule from "@/application/modules/natgas";

// The desk's "Nat Gas TTF Spreads" screen. The rows come from the API (GET /api/screens/ttf-spreads).
export const Route = createFileRoute("/ttf-time-spread")({
  component: () => <NatGasModule screenKey="ttf-spreads" />,
});
