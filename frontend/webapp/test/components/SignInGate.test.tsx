import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { SignInGate } from "@/components/SignInGate";
import type { SignIn } from "@atlas/external";

const app = <div>the app</div>;

describe("SignInGate (in front of the whole app)", () => {
  it("shows the sign-in page, not the app, while nobody is signed in", async () => {
    const signedOut: SignIn = { state: "signedOut" };
    render(<SignInGate getSignIn={async () => signedOut} returnTo="/natgas">{app}</SignInGate>);

    expect(await screen.findByRole("button", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByText("the app")).not.toBeInTheDocument();
  });

  it("shows the app once someone is signed in", async () => {
    const signedIn: SignIn = { state: "signedIn", name: "Sean Hays", email: "sean.hays@braemar.com" };
    render(<SignInGate getSignIn={async () => signedIn}>{app}</SignInGate>);

    expect(await screen.findByText("the app")).toBeInTheDocument();
  });

  it("shows the app where there is no sign-in at all, as if the gate were not there", async () => {
    const none: SignIn = { state: "notConfigured" };
    render(<SignInGate getSignIn={async () => none}>{app}</SignInGate>);

    expect(await screen.findByText("the app")).toBeInTheDocument();
  });

  it("shows nothing while the answer is still on its way", () => {
    const { container } = render(<SignInGate getSignIn={() => new Promise(() => {})}>{app}</SignInGate>);

    expect(container).toBeEmptyDOMElement();
  });

  it("says so when the platform could not be asked, rather than staying blank, and tries again at the same address", async () => {
    render(<SignInGate getSignIn={async () => { throw new Error("no answer from /.auth/me"); }} returnTo="/coal/spreads">{app}</SignInGate>);

    expect(await screen.findByText("Atlas could not load: no answer from /.auth/me")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Try again" })).toHaveAttribute("href", "/coal/spreads");
  });
});
