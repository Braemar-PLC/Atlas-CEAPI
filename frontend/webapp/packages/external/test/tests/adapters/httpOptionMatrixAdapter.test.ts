import { describe, it, expect } from "vitest";
import { fetchOptionMatrix } from "../../../src/adapters/http-option-matrix.adapter";

const ttf = {
  product: "TTF",
  expiries: [
    { kind: "Month", label: "Nov26", expiryDate: "2026-10-27", underlyingSymbol: "TFM 26X-ICN", strikes: [79, 80, 81] },
    { kind: "Quarter", label: "Q1 27", expiryDate: "2026-12-24", underlyingSymbol: "TFMQ 27F-ICN", strikes: [80] },
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

describe("fetchOptionMatrix (external)", () => {
  it("asks the API for the product's matrix", async () => {
    const { fn, asked } = fakeFetch(ttf);

    await fetchOptionMatrix("TTF", fn);

    expect(asked).toEqual(["/api/options/TTF"]);
  });

  it("returns the matrix when the answer has the expected shape", async () => {
    const { fn } = fakeFetch(ttf);

    expect(await fetchOptionMatrix("TTF", fn)).toEqual(ttf);
  });

  it("says which product is missing when the API answers 404", async () => {
    const { fn } = fakeFetch("There are no options rules for 'JKM'.", 404);

    await expect(fetchOptionMatrix("JKM", fn)).rejects.toThrow('The API has no option matrix for "JKM" (HTTP 404).');
  });

  it("refuses an answer of the wrong shape", async () => {
    const { fn } = fakeFetch({ product: "TTF", expiries: [{ kind: "Weekly", label: "x", expiryDate: "d", underlyingSymbol: "u", strikes: [] }] });

    await expect(fetchOptionMatrix("TTF", fn)).rejects.toThrow(/not the expected shape/);
  });
});
