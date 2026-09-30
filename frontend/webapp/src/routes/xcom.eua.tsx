import { createFileRoute } from "@tanstack/react-router";
import ChainModule from "@/application/modules/chain";

// EUA (carbon) options: the chain, expiry by expiry, calls beside puts (GET /api/screens/xcom-eua).
export const Route = createFileRoute("/xcom/eua")({
  component: () => <ChainModule screenKey="xcom-eua" />,
});
