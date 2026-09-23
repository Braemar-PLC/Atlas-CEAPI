import { useEffect, useState } from "react";
import { fetchSignIn } from "@atlas/external";
import type { SignIn } from "@atlas/external";

/** Who is signed in, asked once when the page opens. Undefined until the answer arrives. */
export function useSignIn(): SignIn | undefined {
  const [signIn, setSignIn] = useState<SignIn>();

  useEffect(() => {
    let cancelled = false;
    fetchSignIn().then(next => {
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
