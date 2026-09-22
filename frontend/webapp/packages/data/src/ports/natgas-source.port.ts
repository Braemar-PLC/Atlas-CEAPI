import type { NatGasEventMessage } from "../models/eventMessage.schemas";

export interface NatGasSource {
  connect(
    onMessage: (msg: NatGasEventMessage) => void,
    onError: (err: unknown) => void 
  ): Promise<{
    unsubscribe: () => void;
  }>;
}
