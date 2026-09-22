import { describe, it, expect } from "vitest";
import { fetchScreen } from "../../../src/adapters/http-screen.adapter";

const ttfFlat = {
  key: "ttf-flat",
  title: "Nat Gas TTF Flat Price",
  rows: [
    { hub: "TTF", label: "Oct26", group: "Months", source: "quoted", symbol: "TFM 26V-ICN", near: null, far: null },
  ],
};

// Stands in for the network: records the address asked for and answers with `body`.
const fakeFetch = (body: unknown, status = 200) => {
  const asked: string[] = [];
  const fn = (async (url: RequestInfo | URL) => {
    asked.push(String(url));
    return { ok: status >= 200 && status < 300, status, json: async () => body } as Response;
  }) as typeof fetch;
  return { fn, asked };
};

describe("fetchScreen (external)", () => {
  it("asks the API for the screen by its key", async () => {
    const { fn, asked } = fakeFetch(ttfFlat);

    await fetchScreen("ttf-flat", fn);

    expect(asked).toEqual(["/api/screens/ttf-flat"]);
  });

  it("returns the screen when the answer has the expected shape", async () => {
    const { fn } = fakeFetch(ttfFlat);

    expect(await fetchScreen("ttf-flat", fn)).toEqual(ttfFlat);
  });

  it("says which screen is missing when the API answers 404", async () => {
    const { fn } = fakeFetch("There is no screen called 'coal'.", 404);

    await expect(fetchScreen("coal", fn)).rejects.toThrow('The API has no screen "coal" (HTTP 404).');
  });

  it("refuses an answer of the wrong shape instead of passing it to the grid", async () => {
    const { fn } = fakeFetch({ key: "ttf-flat", title: "x", rows: [{ label: "Oct26" }] });

    await expect(fetchScreen("ttf-flat", fn)).rejects.toThrow(/not the expected shape/);
  });
});
