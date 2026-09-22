import { describe, it, expect } from "vitest";
import { mapQuote } from "../../../src/mapping/natgas-quote.mapper";

describe("mapQuote (external)", () => {
  it("validates and maps a correct DTO", () => {
    const dto = {
      exchange: "ICE",
      commodity: "GAS",
      hub: "NBP",
      tenor: "Mar26",
      bid: 10.5,
      ask: 11.0,
      last: null,
      netChange: 0.2,
      prevSettle: 10.3,
      volume: 1000,
    };

    const q = mapQuote(dto); // throws if invalid
    expect(q.exchange).toBe("ICE");
    expect(q.bid).toBe(10.5);
    expect(q.last).toBeUndefined(); // null -> undefined normalized
  });

  it("throws for an invalid DTO", () => {
    const bad = { exchange: "XXX" }; // missing required props
    expect(() => mapQuote(bad as any)).toThrowError();
  });
});