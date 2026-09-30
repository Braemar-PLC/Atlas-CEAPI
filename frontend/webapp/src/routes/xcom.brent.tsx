import { createFileRoute } from "@tanstack/react-router";
import ChainModule from "@/application/modules/chain";

// Brent crude options: the chain, expiry by expiry, calls beside puts (GET /api/screens/xcom-brent).
export const Route = createFileRoute("/xcom/brent")({
  component: () => <ChainModule screenKey="xcom-brent" />,
});
