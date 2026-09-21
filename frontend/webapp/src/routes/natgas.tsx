import { createFileRoute } from "@tanstack/react-router";
import NatGasModule from "@/application/modules/natgas";

export const Route = createFileRoute("/natgas")({
  component: NatGasModule,
});