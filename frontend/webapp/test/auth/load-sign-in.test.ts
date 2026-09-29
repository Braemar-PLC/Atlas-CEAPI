import { describe, it, expect, vi } from "vitest";
import { loadSignIn, resetSignIn } from "@/application/auth/load-sign-in";
import type { SignIn } from "@atlas/external";

const signedIn: SignIn = { state: "signedIn", name: "Sean Hays", email: "sean.hays@braemar.com" };

describe("loadSignIn", () => {
  it("asks the platform once and hands the same answer to every caller", async () => {
    resetSignIn();
    const ask = vi.fn(async () => signedIn);

    const first = await loadSignIn(ask);
    const second = await loadSignIn(ask);

    expect(first).toEqual(signedIn);
    expect(second).toBe(first);
    expect(ask).toHaveBeenCalledTimes(1);
  });

  it("does not keep a failure: the next caller asks again", async () => {
    resetSignIn();
    const failing = vi.fn(async () => { throw new Error("down"); });
    await expect(loadSignIn(failing)).rejects.toThrow("down");

    const ask = vi.fn(async () => signedIn);
    await expect(loadSignIn(ask)).resolves.toEqual(signedIn);
  });
});
