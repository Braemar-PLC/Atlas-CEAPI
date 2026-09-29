import { describe, it, expect, vi } from "vitest";
import { loadSession, resetSession } from "@/application/auth/session";
import type { Desk, Me } from "@atlas/data";

const sean: Me = { name: "Sean Hays", email: "sean.hays@braemar.com", isAdmin: true, desks: [], homeDesk: null };
const desks: Desk[] = [{ key: "coal", name: "Coal", description: "API2 and API4" }];

const deps = (overrides: Partial<Parameters<typeof loadSession>[0]> = {}) => ({
  fetchMe: vi.fn(async () => sean),
  fetchDesks: vi.fn(async () => desks),
  reloadIfSignedOut: vi.fn(async () => false),
  ...overrides,
});

describe("loadSession", () => {
  it("fetches who is signed in and the desks, once, and hands the same answer to every caller", async () => {
    resetSession();
    const d = deps();

    const first = await loadSession(d);
    const second = await loadSession(d);

    expect(first).toEqual({ me: sean, desks });
    expect(second).toBe(first);
    expect(d.fetchMe).toHaveBeenCalledTimes(1);
    expect(d.fetchDesks).toHaveBeenCalledTimes(1);
  });

  it("forgets the answer on resetSession, so the next caller asks again", async () => {
    resetSession();
    const d = deps();

    await loadSession(d);
    resetSession();
    await loadSession(d);

    expect(d.fetchMe).toHaveBeenCalledTimes(2);
  });

  it("when the API cannot answer, checks whether the sign-in has expired, then reports the fault", async () => {
    resetSession();
    const d = deps({ fetchMe: vi.fn(async () => { throw new Error("The API would not say who is signed in (HTTP 401)."); }) });

    await expect(loadSession(d)).rejects.toThrow("HTTP 401");

    expect(d.reloadIfSignedOut).toHaveBeenCalledTimes(1);
  });

  it("does not keep a failure: the next caller tries again", async () => {
    resetSession();
    const failing = vi.fn(async () => { throw new Error("down"); });
    const d = deps({ fetchMe: failing });
    await expect(loadSession(d)).rejects.toThrow("down");

    d.fetchMe = vi.fn(async () => sean);
    await expect(loadSession(d)).resolves.toEqual({ me: sean, desks });
  });
});
