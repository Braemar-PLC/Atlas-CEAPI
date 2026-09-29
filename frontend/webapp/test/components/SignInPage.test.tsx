import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SignInPage } from "@/components/SignInPage";

describe("SignInPage (what a signed-out person sees)", () => {
  it("shows Braemar Atlas, a username box and an arrow to sign in", () => {
    render(<SignInPage returnTo="/natgas" go={() => {}} />);

    expect(screen.getByRole("img", { name: "Braemar" })).toHaveAttribute("src", "/braemar-logo-light.png");
    expect(screen.getByText("ATLAS")).toBeInTheDocument();
    expect(screen.getByLabelText("Username")).toHaveAttribute("placeholder", "Enter your username");
    expect(screen.getByRole("button", { name: "Sign in" })).toBeInTheDocument();
  });

  it("the arrow sends the browser to the platform's sign-in, with the username as a hint, and back to the page", async () => {
    const go = vi.fn();
    render(<SignInPage returnTo="/natgas" go={go} />);

    await userEvent.type(screen.getByLabelText("Username"), "sean.hays@braemar.com");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(go).toHaveBeenCalledWith("/.auth/login/aad?post_login_redirect_uri=%2Fnatgas&login_hint=sean.hays%40braemar.com");
  });

  it("Enter in the username box does the same as the arrow", async () => {
    const go = vi.fn();
    render(<SignInPage returnTo="/" go={go} />);

    await userEvent.type(screen.getByLabelText("Username"), "sean.hays@braemar.com{Enter}");

    expect(go).toHaveBeenCalledTimes(1);
  });

  it("signs in without a hint when the box is left empty", async () => {
    const go = vi.fn();
    render(<SignInPage returnTo="/coal/api2" go={go} />);

    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(go).toHaveBeenCalledWith("/.auth/login/aad?post_login_redirect_uri=%2Fcoal%2Fapi2");
  });
});
