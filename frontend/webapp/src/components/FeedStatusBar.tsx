import type { FeedHealth } from "@/application/feed/feed-health";
import "./feed-status-bar.css";

const labelByState: Record<FeedHealth["state"], string> = {
  Connecting: "CONNECTING",
  Live: "LIVE",
  Reconnecting: "RECONNECTING",
  AuthenticationFailed: "AUTHENTICATION FAILED",
  Disconnected: "DISCONNECTED",
  Stale: "STALE",
};

function formatLastQuote(value: string | null): string {
  if (!value) return "No prices received";
  const ageSeconds = Math.max(0, Math.floor((Date.now() - Date.parse(value)) / 1000));
  return `Last price ${ageSeconds}s ago`;
}

export function FeedStatusBar({ health }: { health: FeedHealth }) {
  const current = health.state === "Live";
  const detail = current
    ? `${health.subscribedSymbols.toLocaleString()} symbols · ${formatLastQuote(health.lastQuoteAt)}`
    : `${health.detail} · PRICES SHOWN ARE NOT CURRENT`;

  return (
    <div
      className={`feed-status feed-status-${health.state.toLowerCase()}`}
      role="status"
      aria-live="polite"
      title={`Feed generation ${health.generation}`}
    >
      <strong>{labelByState[health.state]}</strong>
      <span>{detail}</span>
    </div>
  );
}
