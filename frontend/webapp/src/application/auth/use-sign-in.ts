import { useEffect, useState } from "react";
import type { SignIn } from "@atlas/external";
import { loadSignIn } from "./load-sign-in";

/** Who is signed in: the answer the gate already has (`loadSignIn`). Undefined until it arrives. */
export function useSignIn(): SignIn | undefined {
  const [signIn, setSignIn] = useState<SignIn>();

  useEffect(() => {
    let cancelled = false;
    loadSignIn().then(next => {
      if (!cancelled) {
        setSignIn(next);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  return signIn;
}
