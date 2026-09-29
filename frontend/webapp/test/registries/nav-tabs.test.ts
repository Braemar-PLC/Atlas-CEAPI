import { describe, it, expect } from "vitest";
import { NaturalGasPages } from "@/application/registries/nav-tabs";

describe("the Natural Gas desk's pages", () => {
  it("lists the gas desk's screens in the order the desk asked for", () => {
    expect(NaturalGasPages.map(t => t.label)).toEqual(["TTF Flat", "TTF Time Spread", "NBP"]);
  });

  it("opens the live flat price grid from the first page", () => {
    expect(NaturalGasPages[0].to).toBe("/natgas");
  });

  it("gives every page its own address", () => {
    const addresses = NaturalGasPages.map(t => t.to);

    expect(new Set(addresses).size).toBe(addresses.length);
    expect(addresses.every(a => a.startsWith("/"))).toBe(true);
  });
});
