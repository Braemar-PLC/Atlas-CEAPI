import { createFileRoute } from "@tanstack/react-router";
import NatGasModule from "@/application/modules/natgas";

// "ttf-flat" is the API's name for this screen: GET /api/screens/ttf-flat says which rows it shows today.
export const Route = createFileRoute("/natgas")({
  component: () => <NatGasModule screenKey="ttf-flat" />,
});
