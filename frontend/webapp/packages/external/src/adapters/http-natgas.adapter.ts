import { NatGasEventMessageSchema, type NatGasEventMessage, type NatGasSource } from "@atlas/data";
import { Value } from "@sinclair/typebox/value";

export function makeHttpNatGasAdapter(apiUrl: string): NatGasSource {
  return {
    connect(
      onMessage: (msg: NatGasEventMessage) => void,
      onError: (err: unknown) => void
    ) {
      return new Promise<{ unsubscribe: () => void }>((resolve, reject) => {
        const ES: typeof EventSource = (globalThis as any).EventSource ?? EventSource;
        const es = new ES(apiUrl);

        let resolved = false;

        // Resolve as soon as the connection opens…
        es.onopen = () => {
          if (!resolved) {
            resolved = true;
            resolve({ unsubscribe: () => es.close() });
          }
        };

        es.onmessage = evt => {
          if (!resolved) {
            resolved = true;
            resolve({ unsubscribe: () => es.close() });
          }

          try {
            const raw = JSON.parse(evt.data);

            if (!Value.Check(NatGasEventMessageSchema, raw)) {
              const err = [...Value.Errors(NatGasEventMessageSchema, raw)][0];
              onError(err);
              return;
            }

            onMessage(raw);

          } catch (e) {
            onError(e);
          }
        };

        es.onerror = (err: any) => {
          // If we haven't resolved yet, treat early errors as connection failure.
          if (!resolved) {
            resolved = true;
            reject(err);
            try { es.close(); } catch { }
            return;
          }

          onError(err);
        };
      });
    },
  };
}
