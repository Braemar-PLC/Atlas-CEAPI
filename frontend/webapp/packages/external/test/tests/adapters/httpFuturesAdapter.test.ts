// makeHttpNatGasAdapter.spec.ts
import { describe, it, expect, vi } from "vitest";
import { EventMessageTypes } from "@atlas/data";
import { makeHttpNatGasAdapter } from "@atlas/external";

// Track the instance created inside adapter.connect()
let lastES: MockEventSource | null = null;

// ---- ensure adapter uses OUR EventSource mock ----
class MockEventSource {
  onopen?: () => void;
  onmessage?: (evt: any) => void;
  onerror?: (err: any) => void;
  closed = false;

  constructor(public url: string) {
    lastES = this; // capture the instance used by adapter
  }

  close() {
    this.closed = true;
  }
}

vi.stubGlobal("EventSource", MockEventSource);
// --------------------------------------------------

describe("makeHttpNatGasAdapter", () => {

  const validSnapshot = {
    type: EventMessageTypes.NatGasSnapshot,
    data: { NBP: {} },
  };

  it("resolves on open", async () => {
    const adapter = makeHttpNatGasAdapter("url");

    const p = adapter.connect(() => {}, () => {});
    const es = lastES!;

    es.onopen?.();

    const { unsubscribe } = await p;
    expect(typeof unsubscribe).toBe("function");
  });

  it("resolves if first message arrives before open", async () => {
    const adapter = makeHttpNatGasAdapter("url");

    const p = adapter.connect(() => {}, () => {});
    const es = lastES!;

    es.onmessage?.({ data: JSON.stringify(validSnapshot) });

    const { unsubscribe } = await p;
    expect(unsubscribe).toBeDefined();
  });

  it("delivers valid schema message", async () => {
    const adapter = makeHttpNatGasAdapter("url");

    const onMessage = vi.fn();
    const onError = vi.fn();

    const p = adapter.connect(onMessage, onError);
    const es = lastES!;

    es.onopen?.();
    es.onmessage?.({ data: JSON.stringify(validSnapshot) });

    await p;
    expect(onMessage).toHaveBeenCalledWith(validSnapshot);
    expect(onError).not.toHaveBeenCalled();
  });

  it("invalid schema triggers onError", async () => {
    const adapter = makeHttpNatGasAdapter("url");

    const onMessage = vi.fn();
    const onError = vi.fn();

    const p = adapter.connect(onMessage, onError);
    const es = lastES!;

    es.onopen?.();
    es.onmessage?.({ data: JSON.stringify({ type: "BAD", data: {} }) });

    await p;
    expect(onMessage).not.toHaveBeenCalled();
    expect(onError).toHaveBeenCalled();
  });

  it("JSON parse error", async () => {
    const adapter = makeHttpNatGasAdapter("url");

    const onMessage = vi.fn();
    const onError = vi.fn();

    const p = adapter.connect(onMessage, onError);
    const es = lastES!;

    es.onopen?.();
    es.onmessage?.({ data: "{not json}" });

    await p;
    expect(onMessage).not.toHaveBeenCalled();
    expect(onError).toHaveBeenCalled();
  });

  it("early error rejects promise and closes ES", async () => {
    const adapter = makeHttpNatGasAdapter("url");

    const onMessage = vi.fn();
    const onError = vi.fn();

    const p = adapter.connect(onMessage, onError);
    const es = lastES!;

    es.onerror?.(new Error("connect fail"));

    await expect(p).rejects.toThrow("connect fail");
    expect(es.closed).toBe(true);
  });

  it("late error calls onError", async () => {
    const adapter = makeHttpNatGasAdapter("url");

    const onMessage = vi.fn();
    const onError = vi.fn();

    const p = adapter.connect(onMessage, onError);
    const es = lastES!;

    es.onopen?.();
    await p;

    const err = new Error("later");
    es.onerror?.(err);

    expect(onError).toHaveBeenCalledWith(err);
  });

  it("unsubscribe closes connection", async () => {
    const adapter = makeHttpNatGasAdapter("url");

    const p = adapter.connect(() => {}, () => {});
    const es = lastES!;

    es.onopen?.();

    const { unsubscribe } = await p;

    unsubscribe();
    expect(es.closed).toBe(true);
  });
});