import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import type { SignIn } from "@atlas/external";
import { loadSignIn } from "@/application/auth/load-sign-in";
import { SignInPage } from "./SignInPage";

type Props = {
  children: ReactNode;
  /** Asks the platform who is signed in. Only a test replaces it. */
  getSignIn?: () => Promise<SignIn>;
  /** Where to come back to after signing in; by default the address the browser is showing. */
  returnTo?: string;
};

type Answer = { signIn: SignIn } | { failed: Error };

/**
 * Stands in front of the whole app. Until the platform has said who is signed in, nothing is drawn; while nobody
 * is, the sign-in page is shown instead of the app. Where there is no sign-in at all (`notConfigured`: the
 * platform's sign-in is not switched on, so `/.auth/me` returns the page itself rather than an answer) the app
 * opens as if the gate were not there, and the API decides what it will answer. If the platform cannot be asked at
 * all, the fault is shown with a link that tries the same address again.
 */
export function SignInGate({
  children,
  getSignIn = loadSignIn,
  returnTo = window.location.pathname + window.location.search,
}: Props) {
  const [answer, setAnswer] = useState<Answer>();

  useEffect(() => {
    let cancelled = false;
    getSignIn().then(
      signIn => {
        if (!cancelled) {
          setAnswer({ signIn });
        }
      },
      (error: unknown) => {
        if (!cancelled) {
          setAnswer({ failed: error instanceof Error ? error : new Error(String(error)) });
        }
      },
    );
    return () => {
      cancelled = true;
    };
  }, [getSignIn]);

  if (answer === undefined) {
    return null;
  }
  if ("failed" in answer) {
    return (
      <div style={{ padding: 24, fontFamily: "Arial, Helvetica, sans-serif" }}>
        <p>Atlas could not load: {answer.failed.message}</p>
        <a href={returnTo}>Try again</a>
      </div>
    );
  }
  if (answer.signIn.state === "signedOut") {
    return <SignInPage returnTo={returnTo} />;
  }
  return children;
}
