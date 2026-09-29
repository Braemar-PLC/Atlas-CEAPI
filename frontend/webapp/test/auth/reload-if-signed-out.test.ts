import { describe, it, expect } from "vitest";
import { reloadIfSignedOut } from "@/application/auth/reload-if-signed-out";
import type { SignIn } from "@atlas/external";

const harness = (signIn: SignIn) => {
  let reloads = 0;
  const run = () => reloadIfSignedOut(async () => signIn, () => { reloads++; });
  return { run, reloaded: () => reloads };
};

describe("reloadIfSignedOut", () => {
  it("reloads the page when the sign-in has expired, so the sign-in page can be shown", async () => {
    const h = harness({ state: "signedOut" });

    expect(await h.run()).toBe(true);
    expect(h.reloaded()).toBe(1);
  });

  it("does nothing while signed in: the fault is the API's, and reloading would not help", async () => {
    const h = harness({ state: "signedIn", name: "Sean Hays", email: "sean.hays@braemar.com" });

    expect(await h.run()).toBe(false);
    expect(h.reloaded()).toBe(0);
  });

  it("does nothing on the laptop, where there is no sign-in to expire", async () => {
    const h = harness({ state: "notConfigured" });

    expect(await h.run()).toBe(false);
    expect(h.reloaded()).toBe(0);
  });
});
