import { describe, it, expect } from "vitest";
import { CoalView, GasView, ScreenFieldKeys, screenView } from "@/application/registries/screen-views";

describe("screen views", () => {
  it("lays every gas screen out like ICE's TTF flat price screen: Strip and the 12 columns, prices to 3 decimals", () => {
    for (const key of ["ttf-flat", "ttf-spreads", "nbp"]) {
      expect(screenView(key)).toBe(GasView);
    }
    expect(GasView.fields).toEqual([
      "tenor", "bidBar", "bidSize", "bid", "ask", "askSize", "askBar",
      "last", "netChange", "settle", "high", "low", "volume", "blockVolume",
    ]);
    expect(GasView.priceDecimals).toBe(3);
    expect(GasView.impliedFrom).toBeUndefined();
  });

  it("lays the coal screens out like WebICE's Coal tab: Product and Hub first, OI, WAP and the implied block, prices to 2 decimals", () => {
    expect(CoalView.fields).toEqual([
      "product", "hub", "tenor", "bidBar", "bidSize", "bid", "ask", "askSize", "askBar",
      "last", "netChange", "settle", "openInterest", "high", "low", "wap", "volume", "blockVolume",
      "impliedBidSize", "impliedBid", "impliedAsk", "impliedAskSize",
    ]);
    for (const key of ["coal-api2", "coal-newcastle", "coal-spreads"]) {
      expect(screenView(key).fields).toEqual(CoalView.fields);
      expect(screenView(key).priceDecimals).toBe(2);
    }
  });

  it("works the coal flat screens' implied prices out from the coal spreads screen, which has none of its own", () => {
    expect(screenView("coal-api2").impliedFrom).toBe("coal-spreads");
    expect(screenView("coal-newcastle").impliedFrom).toBe("coal-spreads");
    expect(screenView("coal-spreads").impliedFrom).toBeUndefined();
  });

  it("lays a screen it has never heard of out like gas, so a new gas screen needs no entry", () => {
    expect(screenView("ttf-something-new")).toBe(GasView);
  });

  it("answers the same view object for the same key every time, so the grid's columns are built once", () => {
    expect(screenView("coal-api2")).toBe(screenView("coal-api2"));
  });

  it("uses only fields the column builder knows", () => {
    for (const view of [GasView, CoalView]) {
      expect(view.fields.every(f => (ScreenFieldKeys as readonly string[]).includes(f))).toBe(true);
    }
  });
});
