import { describe, it, expect } from "vitest";
import { fetchMe } from "../../../src/adapters/http-me.adapter";

const sean = {
  name: "Sean Hays",
  email: "sean.hays@braemar.com",
  isAdmin: true,
  desks: [{ key: "natural-gas", name: "Natural Gas", description: "TTF, NBP and JKM" }],
  homeDesk: "natural-gas",
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

describe("fetchMe (external)", () => {
  it("asks the API who is signed in", async () => {
    const { fn, asked } = fakeFetch(sean);

    await fetchMe(fn);

    expect(asked).toEqual(["/api/me"]);
  });

  it("returns the user when the answer has the expected shape", async () => {
    const { fn } = fakeFetch(sean);

    expect(await fetchMe(fn)).toEqual(sean);
  });

  it("accepts a user with no home desk", async () => {
    const { fn } = fakeFetch({ ...sean, desks: [], homeDesk: null });

    expect((await fetchMe(fn)).homeDesk).toBeNull();
  });

  it("says so when the API answers 401", async () => {
    const { fn } = fakeFetch("", 401);

    await expect(fetchMe(fn)).rejects.toThrow("HTTP 401");
  });

  it("refuses an answer that is not the expected shape", async () => {
    const { fn } = fakeFetch({ name: "Sean Hays" });

    await expect(fetchMe(fn)).rejects.toThrow("not the expected shape");
  });
});
