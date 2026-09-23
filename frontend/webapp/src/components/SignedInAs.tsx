import type { SignIn } from "@atlas/external";

/** The signed-in user's name and a sign-out link for the top bar. Nothing at all unless someone is signed in. */
export function SignedInAs({ signIn }: { signIn: SignIn | undefined }) {
  if (signIn?.state !== "signedIn") {
    return null;
  }
  return (
    <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: 14, fontSize: 14, color: "#c8ccd2", whiteSpace: "nowrap" }}>
      <span>Signed in as {signIn.name}</span>
      <a href="/.auth/logout?post_logout_redirect_uri=/" style={{ color: "#e6e6e6" }}>Sign out</a>
    </div>
  );
}
