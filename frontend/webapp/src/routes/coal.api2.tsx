import { createFileRoute } from "@tanstack/react-router";
import ScreenModule from "@/application/modules/screen";

// Rotterdam coal (the API2 contract): flat prices, as on WebICE's Coal tab (GET /api/screens/coal-api2).
export const Route = createFileRoute("/coal/api2")({
  component: () => <ScreenModule screenKey="coal-api2" />,
});
