import { describe, it, expect } from "vitest";
import { Value } from "@sinclair/typebox/value";
import { ScreenSchema, stripsBySymbol } from "../../../src/models/screen.schemas";
import type { Screen } from "../../../src/models/screen.schemas";

// Shaped exactly as the API sends it (GET /api/screens/nbp), nulls included.
const screen: Screen = {
  key: "nbp",
  title: "Nat Gas NBP",
  rows: [
    { hub: "NBP", label: "Oct26", group: "Months", source: "quoted", symbol: "GWM 26V-ICE", near: null, far: null },
    { hub: "NBP", label: "Oct26/Nov26", group: "Spreads", source: "quoted", symbol: "GWM 26V:GWM26X-ICE", near: null, far: null },
    {
      hub: "NBP", label: "Q4 26/Q2 27", group: "Spreads", source: "computed", symbol: null,
      near: { label: "Q4 26", symbol: "GWMQ 26V-ICE" }, far: { label: "Q2 27", symbol: "GWMQ 27J-ICE" },
    },
  ],
};

describe("ScreenSchema", () => {
  it("accepts a screen as the API sends it", () => {
    expect(Value.Check(ScreenSchema, screen)).toBe(true);
  });

  it("rejects a hub the grid does not know", () => {
    const bad = { ...screen, rows: [{ ...screen.rows[0], hub: "HENRY" }] };

    expect(Value.Check(ScreenSchema, bad)).toBe(false);
  });

  it("rejects a source other than quoted or computed", () => {
    const bad = { ...screen, rows: [{ ...screen.rows[0], source: "guessed" }] };

    expect(Value.Check(ScreenSchema, bad)).toBe(false);
  });
});

describe("stripsBySymbol", () => {
  const strips = stripsBySymbol(screen);

  it("files a quoted row's symbol under that row", () => {
    expect(strips.get("GWM 26V-ICE")).toEqual({ hub: "NBP", label: "Oct26" });
  });

  it("files a spread ICE quotes under its own row, not under a leg", () => {
    expect(strips.get("GWM 26V:GWM26X-ICE")).toEqual({ hub: "NBP", label: "Oct26/Nov26" });
  });

  it("files both legs of a worked-out spread under their own strip names", () => {
    expect(strips.get("GWMQ 26V-ICE")).toEqual({ hub: "NBP", label: "Q4 26" });
    expect(strips.get("GWMQ 27J-ICE")).toEqual({ hub: "NBP", label: "Q2 27" });
  });

  it("lists each symbol once", () => {
    expect([...strips.keys()]).toHaveLength(4);
  });
});
