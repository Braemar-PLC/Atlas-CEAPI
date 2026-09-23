import { makeSsePricingAdapter } from "@atlas/external";
import { EventMessageTypes, stripsBySymbol, useNatGas } from "@atlas/data";
import type { Screen } from "@atlas/data";
import { reloadIfSignedOut } from "@/application/auth/reload-if-signed-out";

// One shared connection per desk screen, counted: the first acquire() for a screen connects,
// the last release() disconnects. Different screens have different rows, so each has its own stream.
type Shared = { refCount: number; connecting: Promise<{ unsubscribe: () => void }> };
const streams = new Map<string, Shared>();

/**
 * Acquire the price stream for one desk screen.
 * The API works out which symbols the screen needs (GET /api/screens/{key}/stream), including the legs of
 * worked-out spreads, so the address stays short however many rows there are.
 */
export function acquireScreenStream(screen: Screen) {
  let shared = streams.get(screen.key);

  if (!shared) {
    const adapter = makeSsePricingAdapter(
      `/api/screens/${encodeURIComponent(screen.key)}/stream`,
      stripsBySymbol(screen)
    );

    const push = useNatGas.getState().push;
    const patch = useNatGas.getState().patch;

    const connecting = adapter.connect(
      msg => {
        switch (msg.type) {
          case EventMessageTypes.NatGasSnapshot:
            push(msg.data);
            break;
          case EventMessageTypes.NatGasDelta:
            patch(msg.data);
            break;
        }
      },
      err => {
        console.error(`NatGas SSE error (${screen.key})`, err);
        // A stream that dies because the sign-in expired is brought back by reloading into sign-in.
        void reloadIfSignedOut();
      }
    );

    shared = { refCount: 0, connecting };
    streams.set(screen.key, shared);
  }

  shared.refCount++;
  const mine = shared;
  let released = false;

  return {
    release() {
      if (released) return;
      released = true;

      mine.refCount--;
      if (mine.refCount === 0) {
        streams.delete(screen.key);
        // Wait for the connection before closing it, so a release that arrives while still connecting
        // (leaving a tab straight away) does not leave the stream open behind us.
        mine.connecting.then(conn => conn.unsubscribe()).catch(() => { /* never connected; nothing to close */ });
      }
    }
  };
}
