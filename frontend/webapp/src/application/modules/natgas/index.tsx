import { useEffect, useState } from "react";
import { fetchScreen } from "@atlas/external";
import type { Screen } from "@atlas/data";
import { acquireScreenStream } from "@/application/subscriptions/natgas-subscriptions-manager";
import { reloadIfSignedOut } from "@/application/auth/reload-if-signed-out";
import { NatGasComponent } from "./natgas.component";
import { formatPricesAsOf, useNatGasViewModel } from "./natgas.view-model";

// How often to ask the API whether the screen's rows have changed. Rows only change when a contract expires
// (overnight), so this is just so a wall screen left open picks up the roll without anyone reloading the page.
const ScreenRefreshMs = 15 * 60_000;

// The current time, refreshed once a second, so "Prices as of" keeps ticking even when no price moves.
function useNow() {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  return now;
}

/**
 * Which rows the screen shows, as the API describes them today. Undefined until the first answer.
 * Re-asked every so often; the state only changes when the rows really differ, so an unchanged answer
 * does not reconnect the price stream.
 */
function useScreen(screenKey: string) {
  const [screen, setScreen] = useState<Screen>();
  const [error, setError] = useState<string>();

  useEffect(() => {
    let cancelled = false;

    const load = () =>
      fetchScreen(screenKey)
        .then(next => {
          if (cancelled) return;
          setError(undefined);
          setScreen(current => JSON.stringify(current) === JSON.stringify(next) ? current : next);
        })
        .catch(async e => {
          // An expired sign-in fails here first (the API answers with a login page instead of the screen);
          // the page reloads to sign in again rather than showing the fault.
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

/** One desk screen: `screenKey` is the API's name for it - "ttf-flat", "ttf-spreads" or "nbp". */
export default function NatGasModule({ screenKey }: { screenKey: string }) {
  const { screen, error } = useScreen(screenKey);

  useEffect(() => {
    if (!screen) return;

    const handle = acquireScreenStream(screen);
    return () => handle.release();
  }, [screen]);

  const vm = useNatGasViewModel(screen);
  const now = useNow();

  // Nothing to say until the feed has told us how delayed it is.
  const asOf = vm.delayMinutes == null ? "" : formatPricesAsOf(now, vm.delayMinutes);

  // Before the API has answered there is no title yet; say what is happening instead of showing a blank bar.
  const title = vm.title || (error ? `Cannot load this screen: ${error}` : "Loading…");

  return (
    <NatGasComponent
      screenKey={screenKey}
      title={title}
      data={vm.rows}
      colDefs={vm.colDefs}
      asOf={asOf}
    />
  );
}
