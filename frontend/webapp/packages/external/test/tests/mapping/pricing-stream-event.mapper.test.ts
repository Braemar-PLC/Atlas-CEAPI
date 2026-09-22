import { describe, it, expect } from "vitest";
import { EventMessageTypes } from "@atlas/data";
import { mapPricingStreamEvent } from "../../../src/mapping/pricing-stream-event.mapper";
import type { PricingStreamEventDto } from "../../../src/dto/pricing-stream-event.dto";

// Cut down from a real API event for TFM 26J-ICN (webapp/test-data/ceapidata.json).
const makeEvent = (
  fields: Record<string, string>,
  action = 0,
  symbol = "TFM 26J-ICN"
): PricingStreamEventDto => ({
  metadata: { key: symbol, type: 0, action, serverTimestamp: "2026-09-18T11:54:30.5177083Z" },
  data: { symbol, fields },
});

const ttfApr26 = {
  "13": "TFM 26J-ICN",
  "19": "64.665",
  "20": "64.61",
  "21": "64.725",
  "25": "54.662",
  "26": "10.003000000000007",
  "29": "125384.0",
  "951": "TTF",
  "971": "Apr26",
};

describe("mapPricingStreamEvent (external)", () => {
  it("maps an update to a delta for the right hub and month", () => {
    const msg = mapPricingStreamEvent(makeEvent(ttfApr26));

    expect(msg).toEqual({
      type: EventMessageTypes.NatGasDelta,
      data: {
        exchange: "ICE",
        commodity: "GAS",
        instrument: "TTF",
        tenor: "Apr26",
        bid: 64.61,
        ask: 64.725,
        last: 64.665,
        prevSettle: 54.662,
        netChange: 10.003000000000007,
        volume: 125384,
      },
    });
  });

  it("maps the extra columns of the ICE screen", () => {
    const msg = mapPricingStreamEvent(makeEvent({
      ...ttfApr26,
      "22": "74.0",
      "23": "61.58",
      "30": "5",
      "31": "10",
      "273": "54.662",
      "924": "10804.0",
    }));

    expect(msg?.data).toMatchObject({
      high: 74,
      low: 61.58,
      bidSize: 5,
      askSize: 10,
      settle: 54.662,
      blockVolume: 10804,
    });
  });

  it("maps ICE's minutes-delayed flag, so the screen can say how old the prices are", () => {
    expect(mapPricingStreamEvent(makeEvent({ ...ttfApr26, "47": "10" }))?.data.delayMinutes).toBe(10);
    expect(mapPricingStreamEvent(makeEvent({ ...ttfApr26, "47": "0" }))?.data.delayMinutes).toBe(0);
    expect(mapPricingStreamEvent(makeEvent(ttfApr26))?.data).not.toHaveProperty("delayMinutes");
  });

  it("never sets the tick, which belongs to the store", () => {
    const msg = mapPricingStreamEvent(makeEvent({ ...ttfApr26, "40": "+-+-" }));

    expect(msg?.data).not.toHaveProperty("tick");
  });

  it("maps NBP from its own hub field", () => {
    const msg = mapPricingStreamEvent(
      makeEvent({ "20": "164.49", "951": "NBP", "971": "Apr26" }, 0, "GWM 26J-ICE")
    );

    expect(msg?.data).toMatchObject({ instrument: "NBP", tenor: "Apr26", bid: 164.49 });
  });

  it("keeps ICE's strip name as it is, for seasons and quarters too", () => {
    const tenorOf = (stripName: string) =>
      mapPricingStreamEvent(makeEvent({ "951": "TTF", "971": stripName }))?.data.tenor;

    expect(tenorOf("Oct26")).toBe("Oct26");
    expect(tenorOf("Winter26")).toBe("Winter26");
    expect(tenorOf("Q4 26")).toBe("Q4 26");
  });

  it("leaves out prices that are missing or not numbers", () => {
    const msg = mapPricingStreamEvent(makeEvent({ "20": "", "21": "n/a", "951": "TTF", "971": "May26" }));

    expect(msg?.data).toEqual({ exchange: "ICE", commodity: "GAS", instrument: "TTF", tenor: "May26" });
  });

  it("returns undefined for the empty snapshot the API sends before data arrives", () => {
    expect(mapPricingStreamEvent(makeEvent({}))).toBeUndefined();
  });

  it("returns undefined for a hub the grid does not know", () => {
    expect(mapPricingStreamEvent(makeEvent({ "951": "JKM", "971": "Apr26" }))).toBeUndefined();
  });

  it("returns undefined for reset and remove events", () => {
    expect(mapPricingStreamEvent(makeEvent(ttfApr26, 1))).toBeUndefined();
    expect(mapPricingStreamEvent(makeEvent(ttfApr26, 2))).toBeUndefined();
  });
});

describe("mapPricingStreamEvent with the screen's symbols", () => {
  // Which hub and strip each symbol feeds, as stripsBySymbol builds it from the API's screen.
  const strips = new Map([
    ["TFM 26V:TFM26X-ICN", { hub: "TTF" as const, label: "Oct26/Nov26" }],
    ["GWM 26V-ICE", { hub: "NBP" as const, label: "Oct26" }],
  ]);

  it("files a spread contract under its row even if ICE sends no hub or strip name with it", () => {
    const msg = mapPricingStreamEvent(makeEvent({ "20": "0.34", "21": "0.36" }, 0, "TFM 26V:TFM26X-ICN"), strips);

    expect(msg?.data).toEqual({
      exchange: "ICE", commodity: "GAS", instrument: "TTF", tenor: "Oct26/Nov26", bid: 0.34, ask: 0.36,
    });
  });

  it("trusts the screen over ICE's own strip name, so a row and its price can never disagree", () => {
    const msg = mapPricingStreamEvent(
      makeEvent({ "19": "190.1", "951": "NBP", "971": "OCT 26" }, 0, "GWM 26V-ICE"), strips);

    expect(msg?.data).toMatchObject({ instrument: "NBP", tenor: "Oct26", last: 190.1 });
  });

  it("still reads ICE's fields for a symbol the screen does not list", () => {
    const msg = mapPricingStreamEvent(makeEvent(ttfApr26), strips);

    expect(msg?.data).toMatchObject({ instrument: "TTF", tenor: "Apr26" });
  });

  it("returns undefined for the empty snapshot the API sends before data arrives", () => {
    expect(mapPricingStreamEvent(makeEvent({}, 0, "GWM 26V-ICE"), strips)).toBeUndefined();
  });
});
