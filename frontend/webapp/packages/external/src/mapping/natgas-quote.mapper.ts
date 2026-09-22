
import { Value } from "@sinclair/typebox/value";

import type { NatGasQuote } from "@atlas/data"; // domain type only
import { NatGasQuoteSchema } from "../dto/natgas-quote.schema";
import { NatGasQuoteDto } from "../dto/natgas-quote.dto";

const toUndef = <T>(v: T | null): T | undefined => (v == null ? undefined : v);

export function mapQuote(raw: unknown): NatGasQuote {
  const dto = Value.Parse(NatGasQuoteSchema, raw) as NatGasQuoteDto;

  return {
    exchange: dto.exchange,
    commodity: dto.commodity,
    instrument: dto.hub,
    tenor: dto.tenor,
    bid: toUndef(dto.bid),
    ask: toUndef(dto.ask),
    last: toUndef(dto.last),
    netChange: toUndef(dto.netChange),
    prevSettle: toUndef(dto.prevSettle),
    volume: toUndef(dto.volume),
  };
}
