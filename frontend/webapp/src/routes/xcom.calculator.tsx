import { createFileRoute } from "@tanstack/react-router";
import CalculatorModule from "@/application/modules/calculator";

// The options calculator: a strategy on one expiry priced off traded prices, and each expiry's straddle.
export const Route = createFileRoute("/xcom/calculator")({
  component: () => <CalculatorModule />,
});
