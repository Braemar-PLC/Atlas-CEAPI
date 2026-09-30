import { createFileRoute } from "@tanstack/react-router";
import ChainModule from "@/application/modules/chain";

// TTF options: the chain, expiry by expiry, calls beside puts (GET /api/screens/xcom-ttf).
export const Route = createFileRoute("/xcom/ttf")({
  component: () => <ChainModule screenKey="xcom-ttf" />,
});
