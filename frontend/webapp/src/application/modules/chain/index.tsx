import { useEffect } from "react";
import { acquireScreenStream } from "@/application/subscriptions/natgas-subscriptions-manager";
import { useNow, useScreen } from "@/application/modules/screen/screen.hooks";
import { ScreenComponent } from "@/application/modules/screen/screen.component";
import { formatPricesAsOf } from "@/application/modules/screen/screen.view-model";
import { chainRowClassRules, useChainViewModel } from "./chain.view-model";

/**
 * One option chain of the Cross-Commodities desk: `screenKey` is the API's name for it - "xcom-ttf", "xcom-eua",
 * "xcom-brent" or "xcom-wti". A chain is a desk screen like any other (same fetch, same stream, same grid); only
 * the rows are laid out differently, as a ladder of strikes - see chain.view-model.ts.
 */
export default function ChainModule({ screenKey }: { screenKey: string }) {
  const { screen, error } = useScreen(screenKey);

  useEffect(() => {
    if (!screen) return;

    const handle = acquireScreenStream(screen);
    return () => handle.release();
  }, [screen]);

  const vm = useChainViewModel(screenKey, screen);
  const now = useNow();

  const asOf = vm.delayMinutes == null ? "" : formatPricesAsOf(now, vm.delayMinutes);
  const title = vm.title || (error ? `Cannot load this screen: ${error}` : "Loading…");

  return (
    <ScreenComponent
      screenKey={screenKey}
      title={title}
      data={vm.rows}
      colDefs={vm.colDefs}
      asOf={asOf}
      rowClassRules={chainRowClassRules}
    />
  );
}
