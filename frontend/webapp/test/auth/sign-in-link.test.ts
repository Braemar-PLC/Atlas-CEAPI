import { describe, it, expect } from "vitest";
import { signInHref } from "@/application/auth/sign-in-link";

describe("signInHref (where the arrow on the sign-in page sends the browser)", () => {
  it("goes to the platform's sign-in and comes back to the page that was asked for", () => {
    expect(signInHref("/natgas")).toBe("/.auth/login/aad?post_login_redirect_uri=%2Fnatgas");
  });

  it("keeps a query string in the return address", () => {
    expect(signInHref("/desks?x=1")).toBe("/.auth/login/aad?post_login_redirect_uri=%2Fdesks%3Fx%3D1");
  });

  it("passes the username on as a hint, so it is not typed twice", () => {
    expect(signInHref("/", "sean.hays@braemar.com"))
      .toBe("/.auth/login/aad?post_login_redirect_uri=%2F&login_hint=sean.hays%40braemar.com");
  });

  it("sends no hint for a blank username", () => {
    expect(signInHref("/", "   ")).toBe("/.auth/login/aad?post_login_redirect_uri=%2F");
  });
});
