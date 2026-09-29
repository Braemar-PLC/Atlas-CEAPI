import { describe, it, expect, vi } from "vitest";
import { devSignInMiddleware } from "../vite.dev-sign-in";

const sean = { name: "Sean Hays", email: "sean.hays@braemar.com", roles: ["Atlas.Admin"] };

type FakeResponse = { statusCode: number; headers: Record<string, string>; body: string; ended: boolean };

// Stands in for Node's request and response: enough of each for the middleware to answer.
function call(middleware: ReturnType<typeof devSignInMiddleware>, method: string, url: string) {
  const res: FakeResponse & { setHeader: (k: string, v: string) => void; end: (b?: string) => void } = {
    statusCode: 200, headers: {}, body: "", ended: false,
    setHeader(k, v) { this.headers[k.toLowerCase()] = v; },
    end(b) { this.body = b ?? ""; this.ended = true; },
  };
  const next = vi.fn();
  middleware({ method, url }, res, next);
  return { res, next };
}

describe("dev sign-in middleware (stands in for Azure's /.auth on the laptop)", () => {
  it("answers /.auth/me the way the platform does, for the stand-in user", () => {
    const { res } = call(devSignInMiddleware(sean), "GET", "/.auth/me");

    expect(res.statusCode).toBe(200);
    expect(res.headers["content-type"]).toContain("application/json");
    expect(JSON.parse(res.body)).toEqual([
      {
        provider_name: "aad",
        user_id: "sean.hays@braemar.com",
        user_claims: [
          { typ: "name", val: "Sean Hays" },
          { typ: "preferred_username", val: "sean.hays@braemar.com" },
          { typ: "roles", val: "Atlas.Admin" },
        ],
      },
    ]);
  });

  it("leaves every other address alone while signed in", () => {
    const { res, next } = call(devSignInMiddleware(sean), "GET", "/api/me");

    expect(next).toHaveBeenCalled();
    expect(res.ended).toBe(false);
  });

  it("after signing out, /.auth/me is 401, the API is 401, and a page visit still gets the app (which shows its sign-in page)", () => {
    const middleware = devSignInMiddleware(sean);
    expect(call(middleware, "POST", "/.auth/dev/sign-out").res.statusCode).toBe(204);

    expect(call(middleware, "GET", "/.auth/me").res.statusCode).toBe(401);
    expect(call(middleware, "GET", "/api/me").res.statusCode).toBe(401);
    const page = call(middleware, "GET", "/natgas");
    expect(page.next).toHaveBeenCalled();
    expect(page.res.ended).toBe(false);
  });

  it("signing out through /.auth/logout redirects where asked, like the platform", () => {
    const { res } = call(devSignInMiddleware(sean), "GET", "/.auth/logout?post_logout_redirect_uri=/");

    expect(res.statusCode).toBe(302);
    expect(res.headers["location"]).toBe("/");
  });

  it("signing back in through /.auth/login/aad restores the user and returns to the page", () => {
    const middleware = devSignInMiddleware(sean);
    call(middleware, "POST", "/.auth/dev/sign-out");

    const { res } = call(middleware, "GET", "/.auth/login/aad?post_login_redirect_uri=%2Fnatgas");

    expect(res.statusCode).toBe(302);
    expect(res.headers["location"]).toBe("/natgas");
    expect(call(middleware, "GET", "/.auth/me").res.statusCode).toBe(200);
  });

  it("never intercepts the dev server's own files, even when signed out", () => {
    const middleware = devSignInMiddleware(sean);
    call(middleware, "POST", "/.auth/dev/sign-out");

    for (const url of ["/@vite/client", "/src/main.tsx", "/node_modules/.vite/deps/react.js"]) {
      expect(call(middleware, "GET", url).next).toHaveBeenCalled();
    }
  });
});
