import { makeSsePricingAdapter } from "@atlas/external";
import { EventMessageTypes, stripsBySymbol, useNatGas } from "@atlas/data";
import type { NatGasQuote, Screen } from "@atlas/data";
import { reloadIfSignedOut } from "@/application/auth/reload-if-signed-out";

// One shared connection per desk screen, counted: the first acquire() for a screen connects,
// the last release() disconnects. Different screens have different rows, so each has its own stream.
type Shared = { refCount: number; connecting: Promise<{ unsubscribe: () => void }> };
const streams = new Map<string, Shared>();

/**
 * The prices gathered from the streams are applied to the store at the next frame the browser draws - within about
 * 16 ms - as one change. Every store change re-renders each open grid, and an option chain's stream carries a few
 * hundred messages a second (657 symbols on xcom-ttf), more than a browser can draw one by one; gathered per frame
 * they cost one render each. A tab in the background draws no frames, so a timer applies them there instead, or
 * they would pile up unread.
 */
export const PriceBatchFallbackMs = 250;

// One batch for every stream, and one frame, so a page with two streams still renders once per frame.
let pending: Partial<NatGasQuote>[] = [];
let frame: number | undefined;
let fallbackTimer: ReturnType<typeof setTimeout> | undefined;

function flushPending() {
  if (frame !== undefined) cancelAnimationFrame(frame);
  if (fallbackTimer !== undefined) clearTimeout(fallbackTimer);
  frame = undefined;
  fallbackTimer = undefined;
  if (pending.length === 0) return;
  const batch = pending;
  pending = [];
  useNatGas.getState().patchMany(batch);
}

function enqueue(quote: Partial<NatGasQuote>) {
  pending.push(quote);
  if (fallbackTimer !== undefined) return;
  frame = requestAnimationFrame(flushPending);
  fallbackTimer = setTimeout(flushPending, PriceBatchFallbackMs);
}

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

    const connecting = adapter.connect(
      msg => {
        switch (msg.type) {
          case EventMessageTypes.NatGasSnapshot:
            // A whole dataset replaces everything, so what is waiting goes in first, in its order.
            flushPending();
            push(msg.data);
            break;
          case EventMessageTypes.NatGasDelta:
            enqueue(msg.data);
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
