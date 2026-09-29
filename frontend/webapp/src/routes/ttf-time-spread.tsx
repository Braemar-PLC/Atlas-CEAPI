import { createFileRoute } from "@tanstack/react-router";
import ScreenModule from "@/application/modules/screen";

// The desk's "Nat Gas TTF Spreads" screen. The rows come from the API (GET /api/screens/ttf-spreads).
export const Route = createFileRoute("/ttf-time-spread")({
  component: () => <ScreenModule screenKey="ttf-spreads" />,
});
