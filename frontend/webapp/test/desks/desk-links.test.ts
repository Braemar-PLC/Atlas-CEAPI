import { describe, it, expect } from "vitest";
import { activeDeskKey, deskHome } from "@/application/desks/desk-links";
import { AdminKey, CoalDeskKey, NaturalGasDeskKey, NaturalGasPages } from "@/application/registries/nav-tabs";

describe("desk links", () => {
  it("sends a desk with screens to its first one and every other desk to its own page", () => {
    expect(deskHome(NaturalGasDeskKey)).toBe("/natgas");
    expect(deskHome(CoalDeskKey)).toBe("/coal/api2");
    expect(deskHome("power")).toBe("/desks/power");
  });

  it("knows which desk a page belongs to", () => {
    for (const page of NaturalGasPages) {
      expect(activeDeskKey(page.to)).toBe(NaturalGasDeskKey);
    }
    expect(activeDeskKey("/coal/spreads")).toBe(CoalDeskKey);
    expect(activeDeskKey("/desks/power")).toBe("power");
    expect(activeDeskKey("/admin")).toBe(AdminKey);
  });

  it("belongs to no desk on the chooser and on unknown pages", () => {
    expect(activeDeskKey("/desks")).toBeUndefined();
    expect(activeDeskKey("/forbidden")).toBeUndefined();
  });
});
