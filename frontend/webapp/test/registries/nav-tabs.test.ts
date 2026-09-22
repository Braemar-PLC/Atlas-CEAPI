import { describe, it, expect } from "vitest";
import { NavTabs } from "@/application/registries/nav-tabs";

describe("top bar tabs", () => {
  it("lists the gas desk's screens in the order the desk asked for", () => {
    expect(NavTabs.map(t => t.label)).toEqual(["TTF Flat", "TTF Time Spread", "NBP"]);
  });

  it("opens the live flat price grid from the first tab", () => {
    expect(NavTabs[0].to).toBe("/natgas");
  });

  it("gives every tab its own address", () => {
    const addresses = NavTabs.map(t => t.to);

    expect(new Set(addresses).size).toBe(addresses.length);
    expect(addresses.every(a => a.startsWith("/"))).toBe(true);
  });
});
