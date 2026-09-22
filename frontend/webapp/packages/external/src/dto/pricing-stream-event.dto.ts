
import type { Static } from "@sinclair/typebox";
import { PricingStreamEventSchema } from "./pricing-stream-event.schema";

export type PricingStreamEventDto = Static<typeof PricingStreamEventSchema>;
