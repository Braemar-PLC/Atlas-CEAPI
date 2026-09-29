import { useState } from "react";
import type { CSSProperties, FormEvent } from "react";
import { signInHref } from "@/application/auth/sign-in-link";
import "./sign-in-page.css";

type Props = {
  /** The address on this site to come back to after signing in, e.g. "/natgas". */
  returnTo: string;
  /** Sends the browser to an address. Only a test replaces it. */
  go?: (href: string) => void;
};

/**
 * What a signed-out person sees: Braemar Atlas over the world map. It signs nobody in itself: the arrow sends the
 * browser to the platform's sign-in (Microsoft's page) and back to `returnTo`, with the username along as a hint.
 */
export function SignInPage({ returnTo, go = href => window.location.assign(href) }: Props) {
  const [username, setUsername] = useState("");

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    go(signInHref(returnTo, username));
  };

  return (
    <div className="sign-in">
      <div className="sign-in__map" aria-hidden="true">
        {OCEANS.map(({ name, x, y }) => (
          <span key={name} className="sign-in__ocean" style={{ "--x": x, "--y": y } as CSSProperties}>
            {name}
          </span>
        ))}
      </div>
      <form className="sign-in__card" onSubmit={submit}>
        <img className="sign-in__logo" src="/braemar-logo-light.png" alt="Braemar" />
        <div className="sign-in__product">ATLAS</div>
        <label className="sign-in__label" htmlFor="sign-in-username">Username</label>
        <div className="sign-in__field">
          <PersonIcon />
          <input
            id="sign-in-username"
            className="sign-in__input"
            type="text"
            autoComplete="username"
            placeholder="Enter your username"
            value={username}
            onChange={event => setUsername(event.target.value)}
            autoFocus
          />
        </div>
        <button className="sign-in__go" type="submit" aria-label="Sign in">
          <ArrowIcon />
        </button>
      </form>
    </div>
  );
}

/** The ocean labels, drawn as text over the map; x and y are the centre in the 1152x768 design's pixels. */
const OCEANS = [
  { name: "ARCTIC OCEAN", x: 594, y: 70.5 },
  { name: "ATLANTIC\nOCEAN", x: 413.5, y: 306.5 },
  { name: "PACIFIC\nOCEAN", x: 68, y: 406 },
  { name: "PACIFIC\nOCEAN", x: 1100.5, y: 335 },
  { name: "INDIAN\nOCEAN", x: 819, y: 464.5 },
  { name: "SOUTHERN OCEAN", x: 679, y: 624.5 },
];

/** The person outline at the left of the username box. */
function PersonIcon() {
  return (
    <svg className="sign-in__icon" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 20c0-3.3 3.6-5.5 8-5.5s8 2.2 8 5.5" />
    </svg>
  );
}

/** The arrow inside the round button. */
function ArrowIcon() {
  return (
    <svg className="sign-in__arrow" viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M5 12h14" />
      <path d="M13 6l6 6-6 6" />
    </svg>
  );
}
