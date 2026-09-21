import { makeHttpNatGasAdapter } from "@atlas/external";
import { EventMessageTypes, useNatGas } from "@atlas/data";


let unsubscribeFn: null | (() => void) = null;
let refCount = 0;

/**
 * Acquire a shared natgas stream subscription.
 * First acquire() connects; last release() disconnects.
 */
export async function acquireNatGasStream() {
  refCount++;

  if (refCount === 1) {
    const adapter = makeHttpNatGasAdapter("/api/natgas/stream");

    const push = useNatGas.getState().push;
    const patch = useNatGas.getState().patch;

    const conn = await adapter.connect(
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
      err => console.error("NatGas SSE error", err)
    );

    unsubscribeFn = conn.unsubscribe;
  }

  return {
    release() {
      refCount--;
      if (refCount === 0) {
        unsubscribeFn?.();
        unsubscribeFn = null;
      }
    }
  };
}