import { createFileRoute } from "@tanstack/react-router";
import ChainModule from "@/application/modules/chain";

// WTI crude options: the chain, expiry by expiry, calls beside puts (GET /api/screens/xcom-wti).
export const Route = createFileRoute("/xcom/wti")({
  component: () => <ChainModule screenKey="xcom-wti" />,
});
