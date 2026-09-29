import { createFileRoute } from "@tanstack/react-router";
import ScreenModule from "@/application/modules/screen";

// Newcastle coal: flat prices, as on WebICE's Coal tab (GET /api/screens/coal-newcastle).
export const Route = createFileRoute("/coal/newcastle")({
  component: () => <ScreenModule screenKey="coal-newcastle" />,
});
