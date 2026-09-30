import { describe, it, expect } from "vitest";
import { Value } from "@sinclair/typebox/value";
import { commodityOf, InstrumentSchema, NatGasQuoteSchema } from "../../../src/models/natgas.schemas";

describe("commodityOf", () => {
  it("calls the gas hubs gas and the coal hubs coal", () => {
    expect(commodityOf("TTF")).toBe("GAS");
    expect(commodityOf("NBP")).toBe("GAS");
    expect(commodityOf("ARA")).toBe("COAL");
    expect(commodityOf("Newcastle")).toBe("COAL");
  });

  it("calls EUA carbon and Brent and WTI oil - the futures the options desk prices off", () => {
    expect(commodityOf("EUA")).toBe("CARBON");
    expect(commodityOf("Brent")).toBe("OIL");
    expect(commodityOf("WTI")).toBe("OIL");
  });
});

describe("InstrumentSchema", () => {
  it("knows the two coal hubs by the names WebICE uses", () => {
    expect(Value.Check(InstrumentSchema, "ARA")).toBe(true);
    expect(Value.Check(InstrumentSchema, "Newcastle")).toBe(true);
    expect(Value.Check(InstrumentSchema, "API2")).toBe(false);
  });

  it("knows the options desk's products by the names the API's catalogue uses", () => {
    for (const hub of ["EUA", "Brent", "WTI"]) {
      expect(Value.Check(InstrumentSchema, hub)).toBe(true);
    }
    expect(Value.Check(InstrumentSchema, "BRENT")).toBe(false);
  });
});

describe("NatGasQuoteSchema", () => {
  const coal = { exchange: "ICE", commodity: "COAL", instrument: "ARA", tenor: "Oct26" };

  it("carries open interest, WAP and the implied sizes the coal screen shows, as numbers", () => {
    expect(Value.Check(NatGasQuoteSchema, { ...coal, openInterest: 60954, wap: 95.2, impliedBidSize: 5, impliedAskSize: 10 })).toBe(true);
    expect(Value.Check(NatGasQuoteSchema, { ...coal, openInterest: "many" })).toBe(false);
    expect(Value.Check(NatGasQuoteSchema, { ...coal, wap: "n/a" })).toBe(false);
    expect(Value.Check(NatGasQuoteSchema, { ...coal, impliedBidSize: "5" })).toBe(false);
    expect(Value.Check(NatGasQuoteSchema, { ...coal, impliedAskSize: "10" })).toBe(false);
  });
});
