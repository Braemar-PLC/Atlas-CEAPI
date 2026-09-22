import { describe, it, expect, vi } from "vitest";
import { EventMessageTypes } from "@atlas/data";
import { makeSsePricingAdapter } from "@atlas/external";

type EventHandler = (evt: { data: string }) => void;

// ---- ensure adapter uses OUR EventSource mock ----
class MockEventSource {
  // The instance most recently created inside adapter.connect()
  static last: MockEventSource | null = null;

  onopen?: () => void;
  onerror?: (err: unknown) => void;
  closed = false;
  listeners: Record<string, EventHandler> = {};
  url: string;

  constructor(url: string) {
    this.url = url;
    MockEventSource.last = this;
  }

  addEventListener(name: string, handler: EventHandler) {
    this.listeners[name] = handler;
  }

  // Simulates the server sending a named SSE event.
  emit(name: string, data: string) {
    this.listeners[name]?.({ data });
  }

  close() {
    this.closed = true;
  }
}

vi.stubGlobal("EventSource", MockEventSource);
// --------------------------------------------------

describe("makeSsePricingAdapter", () => {

  const snapshotEvent = {
    metadata: { key: "TFM 26J-ICN", type: 0, action: 0, serverTimestamp: "2026-09-18T11:54:30.5177083Z" },
    data: { symbol: "TFM 26J-ICN", fields: { "20": "64.61", "21": "64.725", "951": "TTF", "971": "Apr26" } },
  };

  it("connects to the given url", () => {
    const adapter = makeSsePricingAdapter("/api/pricing/stream?symbol=X");

    adapter.connect(() => {}, () => {});

    expect(MockEventSource.last!.url).toBe("/api/pricing/stream?symbol=X");
  });

  it("resolves on open", async () => {
    const adapter = makeSsePricingAdapter("url");

    const p = adapter.connect(() => {}, () => {});
    MockEventSource.last!.onopen?.();

    const { unsubscribe } = await p;
    expect(typeof unsubscribe).toBe("function");
  });

  it("delivers a named snapshot event as a mapped delta", async () => {
    const adapter = makeSsePricingAdapter("url");

    const onMessage = vi.fn();
    const onError = vi.fn();

    const p = adapter.connect(onMessage, onError);
    const es = MockEventSource.last!;

    es.onopen?.();
    es.emit("snapshot", JSON.stringify(snapshotEvent));

    await p;
    expect(onMessage).toHaveBeenCalledWith({
      type: EventMessageTypes.NatGasDelta,
      data: { exchange: "ICE", commodity: "GAS", instrument: "TTF", tenor: "Apr26", bid: 64.61, ask: 64.725 },
    });
    expect(onError).not.toHaveBeenCalled();
  });

  it("does not listen for heartbeat events", async () => {
    const adapter = makeSsePricingAdapter("url");

    const p = adapter.connect(() => {}, () => {});
    MockEventSource.last!.onopen?.();
    await p;

    expect(MockEventSource.last!.listeners["heartbeat"]).toBeUndefined();
  });

  it("stays quiet for a snapshot with nothing to apply", async () => {
    const adapter = makeSsePricingAdapter("url");

    const onMessage = vi.fn();
    const onError = vi.fn();

    const p = adapter.connect(onMessage, onError);
    const es = MockEventSource.last!;

    es.onopen?.();
    es.emit("snapshot", JSON.stringify({ ...snapshotEvent, data: { symbol: "TFM 26J-ICN", fields: {} } }));

    await p;
    expect(onMessage).not.toHaveBeenCalled();
    expect(onError).not.toHaveBeenCalled();
  });

  it("invalid schema triggers onError", async () => {
    const adapter = makeSsePricingAdapter("url");

    const onMessage = vi.fn();
    const onError = vi.fn();

    const p = adapter.connect(onMessage, onError);
    const es = MockEventSource.last!;

    es.onopen?.();
    es.emit("snapshot", JSON.stringify({ type: "FUTURES_SNAPSHOT", data: {} }));

    await p;
    expect(onMessage).not.toHaveBeenCalled();
    expect(onError).toHaveBeenCalled();
  });

  it("JSON parse error", async () => {
    const adapter = makeSsePricingAdapter("url");

    const onMessage = vi.fn();
    const onError = vi.fn();

    const p = adapter.connect(onMessage, onError);
    const es = MockEventSource.last!;

    es.onopen?.();
    es.emit("snapshot", "{not json}");

    await p;
    expect(onMessage).not.toHaveBeenCalled();
    expect(onError).toHaveBeenCalled();
  });

  it("early error rejects promise and closes ES", async () => {
    const adapter = makeSsePricingAdapter("url");

    const p = adapter.connect(() => {}, () => {});
    const es = MockEventSource.last!;

    es.onerror?.(new Error("connect fail"));

    await expect(p).rejects.toThrow("connect fail");
    expect(es.closed).toBe(true);
  });

  it("late error calls onError", async () => {
    const adapter = makeSsePricingAdapter("url");

    const onError = vi.fn();

    const p = adapter.connect(() => {}, onError);
    const es = MockEventSource.last!;

    es.onopen?.();
    await p;

    const err = new Error("later");
    es.onerror?.(err);

    expect(onError).toHaveBeenCalledWith(err);
  });

  it("unsubscribe closes connection", async () => {
    const adapter = makeSsePricingAdapter("url");

    const p = adapter.connect(() => {}, () => {});
    const es = MockEventSource.last!;

    es.onopen?.();

    const { unsubscribe } = await p;

    unsubscribe();
    expect(es.closed).toBe(true);
  });
});
