import { useEffect, useState } from "react";

export type FeedState =
  | "Connecting"
  | "Live"
  | "Reconnecting"
  | "AuthenticationFailed"
  | "Disconnected"
  | "Stale";

export type FeedHealth = {
  state: FeedState;
  generation: number;
  relayTimestamp: string | null;
  lastQuoteAt: string | null;
  detail: string;
  subscribedSymbols: number;
};

const initialHealth: FeedHealth = {
  state: "Connecting",
  generation: 0,
  relayTimestamp: null,
  lastQuoteAt: null,
  detail: "Connecting to the market-data feed",
  subscribedSymbols: 0,
};

function isFeedHealth(value: unknown): value is FeedHealth {
  if (!value || typeof value !== "object") return false;
  const candidate = value as Partial<FeedHealth>;
  const states: FeedState[] = [
    "Connecting", "Live", "Reconnecting", "AuthenticationFailed", "Disconnected", "Stale",
  ];
  return states.includes(candidate.state as FeedState)
    && typeof candidate.generation === "number"
    && typeof candidate.detail === "string"
    && typeof candidate.subscribedSymbols === "number";
}

export function useFeedHealth(): FeedHealth {
  const [health, setHealth] = useState(initialHealth);

  useEffect(() => {
    if (typeof EventSource === "undefined") return;

    const source = new EventSource("/api/feed/status/stream");
    source.addEventListener("feed-status", event => {
      try {
        const parsed: unknown = JSON.parse((event as MessageEvent).data);
        if (isFeedHealth(parsed)) setHealth(parsed);
      } catch (error) {
        console.error("Invalid feed-health event", error);
      }
    });
    source.onerror = () => {
      setHealth(current => ({
        ...current,
        state: "Reconnecting",
        detail: "The browser is reconnecting to the Atlas feed-status stream",
      }));
    };

    return () => source.close();
  }, []);

  return health;
}
