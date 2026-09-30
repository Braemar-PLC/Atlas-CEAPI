import { useEffect, useRef, useState } from "react";
import { fetchScreen } from "@atlas/external";
import type { Screen } from "@atlas/data";
import { reloadIfSignedOut } from "@/application/auth/reload-if-signed-out";

// The hooks the screen module and the option chains share. They live apart from the components so that each
// module file exports only its component (React's fast refresh asks for that).

// How often to ask the API whether the screen's rows have changed. Rows only change when a contract expires
// (overnight) or, on an option chain, when the future moves - so this is mostly so a wall screen left open picks
// up the roll without anyone reloading the page.
const ScreenRefreshMs = 15 * 60_000;

/** The current time, refreshed once a second, so "Prices as of" keeps ticking even when no price moves. */
export function useNow() {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  return now;
}

/**
 * `value`, passed on at most once per `intervalMs`: a change after a quiet spell shows straight away, and changes
 * that come faster are held back and the latest shown when the interval is up. For a page whose work per change is
 * large, while the prices themselves arrive every frame.
 */
export function useThrottled<T>(value: T, intervalMs: number): T {
  const [shown, setShown] = useState(value);
  const shownAt = useRef(0);

  useEffect(() => {
    if (Object.is(value, shown)) return;
    const wait = Math.max(0, shownAt.current + intervalMs - Date.now());
    const timer = setTimeout(() => {
      shownAt.current = Date.now();
      setShown(() => value);
    }, wait);
    return () => clearTimeout(timer);
  }, [value, shown, intervalMs]);

  return shown;
}

/**
 * Which rows the screen shows, as the API describes them today. Undefined until the first answer.
 * Re-asked every so often; the state only changes when the rows really differ, so an unchanged answer
 * does not reconnect the price stream. With no key there is no screen to ask for, and nothing is asked.
 */
export function useScreen(screenKey: string | undefined) {
  const [screen, setScreen] = useState<Screen>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    if (!screenKey) {
      return;
    }
    let cancelled = false;

    const load = () =>
      fetchScreen(screenKey)
        .then(next => {
          if (cancelled) return;
          setError(undefined);
          setScreen(current => JSON.stringify(current) === JSON.stringify(next) ? current : next);
        })
        .catch(async e => {
          // An expired sign-in fails here first (the API answers 401 instead of the screen); the page reloads
          // into the sign-in page rather than showing the fault.
          if (cancelled || await reloadIfSignedOut()) {
            return;
          }
          setError(e instanceof Error ? e.message : String(e));
        });

    load();
    const id = setInterval(load, ScreenRefreshMs);

    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [screenKey]);

  return { screen, error };
}
