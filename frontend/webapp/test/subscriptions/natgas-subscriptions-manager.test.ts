import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { useNatGas } from "@atlas/data";
import type { NatGasEventMessage, Screen } from "@atlas/data";

// The SSE adapter is replaced by a hand-fed one: the test plays the stream's messages itself, with no
// EventSource and no network, and sees what reaches the store and when.
const stream = vi.hoisted(() => ({
  onMessage: undefined as ((message: NatGasEventMessage) => void) | undefined,
  unsubscribe: vi.fn(),
}));
vi.mock("@atlas/external", () => ({
  makeSsePricingAdapter: () => ({
    connect: (onMessage: (message: NatGasEventMessage) => void) => {
      stream.onMessage = onMessage;
      return Promise.resolve({ unsubscribe: stream.unsubscribe });
    },
  }),
}));
vi.mock("@/application/auth/reload-if-signed-out", () => ({ reloadIfSignedOut: () => Promise.resolve(false) }));

import { PriceBatchFallbackMs, acquireScreenStream } from "@/application/subscriptions/natgas-subscriptions-manager";

const screen: Screen = {
  key: "xcom-ttf",
  title: "TTF Options",
  rows: [{ hub: "TTF", label: "Nov26", group: "Nov26", source: "quoted", symbol: "TFM 26X-ICN", near: null, far: null, option: null }],
};

const delta = (tenor: string, last: number): NatGasEventMessage =>
  ({ type: "FUTURES_DELTA", data: { instrument: "TTF", tenor, last } });

const ttf = () => useNatGas.getState().curves.TTF ?? {};

describe("acquireScreenStream", () => {
  let changes = 0;
  let stopCounting: () => void;
  let handle: { release: () => void } | undefined;

  beforeEach(() => {
    vi.useFakeTimers();
    useNatGas.getState().clear();
    stream.onMessage = undefined;
    changes = 0;
    stopCounting = useNatGas.subscribe(() => { changes++; });
  });

  afterEach(() => {
    handle?.release();
    handle = undefined;
    stopCounting();
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  // Prices go on screen at the next frame the browser draws, within about 16 ms, not after a fixed wait.
  it("applies a burst of price messages at the next screen frame", () => {
    handle = acquireScreenStream(screen);

    for (let i = 0; i < 300; i++) stream.onMessage!(delta(`Strip${i}`, 70 + i));
    vi.advanceTimersByTime(20);

    expect(changes).toBe(1);
    expect(Object.keys(ttf())).toHaveLength(300);
  });

  // A tab in the background draws no frames; its prices must still be applied, or they would pile up unread.
  it("still applies prices when the page draws no frames, as in a background tab", () => {
    vi.spyOn(window, "requestAnimationFrame").mockImplementation(() => 0);
    handle = acquireScreenStream(screen);

    stream.onMessage!(delta("Nov26", 72.1));
    vi.advanceTimersByTime(PriceBatchFallbackMs);

    expect(changes).toBe(1);
    expect(ttf()["Nov26"]!.last).toBe(72.1);
  });

  // An option chain's stream carries a few hundred messages a second, and every store change re-renders every
  // open grid. Applied one by one they froze the calculator page (2026-09-25); applied together they are cheap.
  it("applies a burst of price messages to the store as one change, not one per message", () => {
    handle = acquireScreenStream(screen);

    for (let i = 0; i < 300; i++) stream.onMessage!(delta(`Strip${i}`, 70 + i));
    vi.runOnlyPendingTimers();

    expect(changes).toBe(1);
    expect(Object.keys(ttf())).toHaveLength(300);
    expect(ttf()["Strip299"]!.last).toBe(369);
  });

  it("gathers what arrives after a batch into the next one, and waits for its turn", () => {
    handle = acquireScreenStream(screen);

    stream.onMessage!(delta("Nov26", 72.1));
    vi.runOnlyPendingTimers();
    expect(changes).toBe(1);

    stream.onMessage!(delta("Nov26", 72.2));
    stream.onMessage!(delta("Dec26", 73.5));
    expect(changes).toBe(1); // nothing applied until the batch is due
    expect(ttf()["Nov26"]!.last).toBe(72.1);

    vi.runOnlyPendingTimers();
    expect(changes).toBe(2);
    expect(ttf()["Nov26"]!.last).toBe(72.2);
    expect(ttf()["Dec26"]!.last).toBe(73.5);
  });

  it("applies a batch within a fraction of a second of its first message", () => {
    handle = acquireScreenStream(screen);

    stream.onMessage!(delta("Nov26", 72.1));
    vi.advanceTimersByTime(500);

    expect(ttf()["Nov26"]!.last).toBe(72.1);
  });
});
