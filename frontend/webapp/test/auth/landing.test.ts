import { describe, it, expect } from "vitest";
import { landingFor } from "@/application/auth/landing";
import type { Me } from "@atlas/data";

const me = (homeDesk: string | null): Me => ({
  name: "Sean Hays",
  email: "sean.hays@braemar.com",
  isAdmin: false,
  desks: [],
  homeDesk,
});

describe("landingFor", () => {
  it("lands a member of exactly one desk on that desk: its first screen, or its own page while it has none", () => {
    expect(landingFor(me("natural-gas"))).toBe("/natgas");
    expect(landingFor(me("coal"))).toBe("/coal/api2");
    expect(landingFor(me("power"))).toBe("/desks/power");
  });

  it("lands everyone else on the desk chooser", () => {
    expect(landingFor(me(null))).toBe("/desks");
  });
});
