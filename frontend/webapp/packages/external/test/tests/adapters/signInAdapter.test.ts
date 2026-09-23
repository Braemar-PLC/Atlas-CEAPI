import { describe, it, expect } from "vitest";
import { fetchSignIn } from "../../../src/adapters/sign-in.adapter";

// What Azure App Service Authentication answers at GET /.auth/me for a signed-in user (fields we do not use omitted).
const signedInBody = [
  {
    provider_name: "aad",
    user_id: "sean.hays@braemar.com",
    user_claims: [
      { typ: "name", val: "Sean Hays" },
      { typ: "preferred_username", val: "sean.hays@braemar.com" },
    ],
  },
];

// Stands in for the network: records the address asked for and answers with `body`.
const fakeFetch = (body: unknown, status = 200, contentType = "application/json; charset=utf-8") => {
  const asked: string[] = [];
  const fn = (async (url: RequestInfo | URL) => {
    asked.push(String(url));
    return {
      ok: status >= 200 && status < 300,
      status,
      headers: { get: (name: string) => (name.toLowerCase() === "content-type" ? contentType : null) },
      json: async () => body,
    } as unknown as Response;
  }) as typeof fetch;
  return { fn, asked };
};

describe("fetchSignIn (external)", () => {
  it("asks the platform who is signed in", async () => {
    const { fn, asked } = fakeFetch(signedInBody);

    await fetchSignIn(fn);

    expect(asked).toEqual(["/.auth/me"]);
  });

  it("reports the signed-in user's name and email", async () => {
    const { fn } = fakeFetch(signedInBody);

    expect(await fetchSignIn(fn)).toEqual({ state: "signedIn", name: "Sean Hays", email: "sean.hays@braemar.com" });
  });

  it("falls back to the email when there is no name claim", async () => {
    const { fn } = fakeFetch([{ provider_name: "aad", user_id: "sean.hays@braemar.com", user_claims: [] }]);

    expect(await fetchSignIn(fn)).toEqual({ state: "signedIn", name: "sean.hays@braemar.com", email: "sean.hays@braemar.com" });
  });

  it("reports signed out when the platform answers 401", async () => {
    const { fn } = fakeFetch("", 401, "text/plain");

    expect(await fetchSignIn(fn)).toEqual({ state: "signedOut" });
  });

  it("reports signed out when the platform answers with nobody", async () => {
    const { fn } = fakeFetch([]);

    expect(await fetchSignIn(fn)).toEqual({ state: "signedOut" });
  });

  it("reports not configured when nothing answers /.auth/me (the dev server returns the page itself)", async () => {
    const { fn } = fakeFetch("<!doctype html><title>webapp</title>", 200, "text/html");

    expect(await fetchSignIn(fn)).toEqual({ state: "notConfigured" });
  });

  it("refuses an answer that is not the platform's shape", async () => {
    const { fn } = fakeFetch([{ something: "else" }]);

    await expect(fetchSignIn(fn)).rejects.toThrow("not the expected shape");
  });
});
