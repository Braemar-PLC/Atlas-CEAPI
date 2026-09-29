import { createFileRoute } from "@tanstack/react-router";
import ScreenModule from "@/application/modules/screen";

// "ttf-flat" is the API's name for this screen: GET /api/screens/ttf-flat says which rows it shows today.
export const Route = createFileRoute("/natgas")({
  component: () => <ScreenModule screenKey="ttf-flat" />,
});
