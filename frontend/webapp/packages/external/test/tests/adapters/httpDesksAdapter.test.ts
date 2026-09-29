import { describe, it, expect } from "vitest";
import { createDesk, fetchDesk, fetchDesks, updateDesk } from "../../../src/adapters/http-desks.adapter";

const naturalGas = { key: "natural-gas", name: "Natural Gas", description: "TTF, NBP and JKM" };
const coal = { key: "coal", name: "Coal", description: "API2 and API4" };
const naturalGasDetail = { ...naturalGas, members: ["sean.hays@braemar.com"] };

// Stands in for the network: records every request (address, method, body) and answers with `body`.
const fakeFetch = (body: unknown, status = 200) => {
  const requests: { url: string; method: string; body: unknown }[] = [];
  const fn = (async (url: RequestInfo | URL, init?: RequestInit) => {
    requests.push({ url: String(url), method: init?.method ?? "GET", body: init?.body ? JSON.parse(String(init.body)) : undefined });
    return {
      ok: status >= 200 && status < 300,
      status,
      json: async () => body,
      text: async () => (typeof body === "string" ? body : JSON.stringify(body)),
    } as Response;
  }) as typeof fetch;
  return { fn, requests };
};

describe("desk adapters (external)", () => {
  it("fetchDesks asks for every desk and returns them", async () => {
    const { fn, requests } = fakeFetch([coal, naturalGas]);

    const desks = await fetchDesks(fn);

    expect(requests).toEqual([{ url: "/api/desks", method: "GET", body: undefined }]);
    expect(desks).toEqual([coal, naturalGas]);
  });

  it("fetchDesk asks for one desk with its members", async () => {
    const { fn, requests } = fakeFetch(naturalGasDetail);

    const desk = await fetchDesk("natural-gas", fn);

    expect(requests[0].url).toBe("/api/desks/natural-gas");
    expect(desk).toEqual(naturalGasDetail);
  });

  it("createDesk posts the desk and returns what was stored", async () => {
    const { fn, requests } = fakeFetch(naturalGasDetail, 201);

    const stored = await createDesk(naturalGasDetail, fn);

    expect(requests).toEqual([{ url: "/api/desks", method: "POST", body: naturalGasDetail }]);
    expect(stored).toEqual(naturalGasDetail);
  });

  it("updateDesk puts the changes to the desk's address", async () => {
    const { fn, requests } = fakeFetch(naturalGasDetail);
    const changes = { name: "Natural Gas", description: "TTF, NBP and JKM", members: ["sean.hays@braemar.com"] };

    await updateDesk("natural-gas", changes, fn);

    expect(requests).toEqual([{ url: "/api/desks/natural-gas", method: "PUT", body: changes }]);
  });

  it("passes the API's reason on when it refuses a change", async () => {
    const { fn } = fakeFetch("\"sean.hays\" is not an email address.", 400);

    await expect(updateDesk("natural-gas", { name: "Natural Gas", description: "", members: ["sean.hays"] }, fn))
      .rejects.toThrow("\"sean.hays\" is not an email address.");
  });

  it("refuses an answer that is not the expected shape", async () => {
    const { fn } = fakeFetch([{ key: "coal" }]);

    await expect(fetchDesks(fn)).rejects.toThrow("not the expected shape");
  });
});
