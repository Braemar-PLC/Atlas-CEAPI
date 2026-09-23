import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { SignedInAs } from "@/components/SignedInAs";

describe("SignedInAs", () => {
  it("shows the signed-in user's name and a way to sign out", () => {
    render(<SignedInAs signIn={{ state: "signedIn", name: "Sean Hays", email: "sean.hays@braemar.com" }} />);

    expect(screen.getByText("Signed in as Sean Hays")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Sign out" })).toHaveAttribute("href", "/.auth/logout?post_logout_redirect_uri=/");
  });

  it("shows nothing on the laptop, where there is no sign-in", () => {
    const { container } = render(<SignedInAs signIn={{ state: "notConfigured" }} />);

    expect(container).toBeEmptyDOMElement();
  });

  it("shows nothing while the answer is still on its way", () => {
    const { container } = render(<SignedInAs signIn={undefined} />);

    expect(container).toBeEmptyDOMElement();
  });
});
