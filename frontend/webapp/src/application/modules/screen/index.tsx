import { useEffect } from "react";
import { acquireScreenStream } from "@/application/subscriptions/natgas-subscriptions-manager";
import { screenView } from "@/application/registries/screen-views";
import { ScreenComponent } from "./screen.component";
import { useNow, useScreen } from "./screen.hooks";
import { formatPricesAsOf, useScreenViewModel } from "./screen.view-model";

/** One desk screen: `screenKey` is the API's name for it - "ttf-flat", "ttf-spreads", "nbp", "coal-api2", "coal-newcastle" or "coal-spreads". */
export default function ScreenModule({ screenKey }: { screenKey: string }) {
  const { screen, error } = useScreen(screenKey);
  // The coal flat screens also follow the coal spreads screen: its rows imply their outrights' prices. A fault
  // fetching it is not shown - it only blanks the implied block, and the same API being down fails `screen` too.
  const { screen: spreads } = useScreen(screenView(screenKey).impliedFrom);

  useEffect(() => {
    if (!screen) return;

    const handle = acquireScreenStream(screen);
    return () => handle.release();
  }, [screen]);

  useEffect(() => {
    if (!spreads) return;

    const handle = acquireScreenStream(spreads);
    return () => handle.release();
  }, [spreads]);

  const vm = useScreenViewModel(screenKey, screen, spreads);
  const now = useNow();

  // Nothing to say until the feed has told us how delayed it is.
  const asOf = vm.delayMinutes == null ? "" : formatPricesAsOf(now, vm.delayMinutes);

  // Before the API has answered there is no title yet; say what is happening instead of showing a blank bar.
  const title = vm.title || (error ? `Cannot load this screen: ${error}` : "Loading…");

  return (
    <ScreenComponent
      screenKey={screenKey}
      title={title}
      data={vm.rows}
      colDefs={vm.colDefs}
      asOf={asOf}
    />
  );
}
