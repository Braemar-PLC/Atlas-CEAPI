import type { NatGasEventMessage, NatGasSource, StripIdentity } from "@atlas/data";
import { Value } from "@sinclair/typebox/value";
import { PricingStreamEventSchema } from "../dto/pricing-stream-event.schema";
import { mapPricingStreamEvent } from "../mapping/pricing-stream-event.mapper";

// The Atlas API names its SSE events: "snapshot" carries pricing, "heartbeat" keeps the connection alive.
// Named events never reach `onmessage`, so they need addEventListener.
const SnapshotEventName = "snapshot";

// `strips` is optional: which hub and strip each symbol feeds, passed straight to the mapper.
export function makeSsePricingAdapter(apiUrl: string, strips?: ReadonlyMap<string, StripIdentity>): NatGasSource {
  return {
    connect(
      onMessage: (msg: NatGasEventMessage) => void,
      onError: (err: unknown) => void
    ) {
      return new Promise<{ unsubscribe: () => void }>((resolve, reject) => {
        const es = new EventSource(apiUrl);

        let resolved = false;

        es.onopen = () => {
          if (!resolved) {
            resolved = true;
            resolve({ unsubscribe: () => es.close() });
          }
        };

        es.addEventListener(SnapshotEventName, (evt: MessageEvent) => {
          if (!resolved) {
            resolved = true;
            resolve({ unsubscribe: () => es.close() });
          }

          try {
            const raw: unknown = JSON.parse(evt.data);

            if (!Value.Check(PricingStreamEventSchema, raw)) {
              const err = [...Value.Errors(PricingStreamEventSchema, raw)][0];
              onError(err);
              return;
            }

            const msg = mapPricingStreamEvent(raw, strips);
            if (msg) onMessage(msg);

          } catch (e) {
            onError(e);
          }
        });

        es.onerror = (err: Event) => {
          // If we haven't resolved yet, treat early errors as connection failure.
          if (!resolved) {
            resolved = true;
            reject(err);
            try { es.close(); } catch { /* already closed */ }
            return;
          }

          onError(err);
        };
      });
    },
  };
}
