import { describe, it, expect } from "vitest";
import { CoalDeskKey, DeskPages, NaturalGasDeskKey, pagesOf } from "@/application/registries/nav-tabs";

describe("desk pages", () => {
  it("gives the Coal desk its three screens, in WebICE's order", () => {
    expect(pagesOf(CoalDeskKey).map(p => p.label)).toEqual(["API2 (Rotterdam)", "Newcastle", "Spreads"]);
    expect(pagesOf(CoalDeskKey).map(p => p.to)).toEqual(["/coal/api2", "/coal/newcastle", "/coal/spreads"]);
  });

  it("still gives the Natural Gas desk its three", () => {
    expect(pagesOf(NaturalGasDeskKey).map(p => p.to)).toEqual(["/natgas", "/ttf-time-spread", "/nbp"]);
  });

  it("gives a desk without screens no pages, rather than failing", () => {
    expect(pagesOf("power")).toEqual([]);
    expect(pagesOf("no-such-desk")).toEqual([]);
  });

  it("keys the pages by the desk keys the API seeds", () => {
    expect(Object.keys(DeskPages).sort()).toEqual(["coal", "natural-gas"]);
  });

  it("gives every page across every desk its own address", () => {
    const addresses = Object.values(DeskPages).flatMap(pages => pages.map(p => p.to));

    expect(new Set(addresses).size).toBe(addresses.length);
    expect(addresses.every(a => a.startsWith("/"))).toBe(true);
  });
});
