
import type { Static } from "@sinclair/typebox";
import { NatGasQuoteSchema } from "./natgas-quote.schema";

export type NatGasQuoteDto = Static<typeof NatGasQuoteSchema>;
