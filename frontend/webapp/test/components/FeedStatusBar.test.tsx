import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FeedStatusBar } from "@/components/FeedStatusBar";
import type { FeedHealth } from "@/application/feed/feed-health";

const health: FeedHealth = {
  state: "Live",
  generation: 3,
  relayTimestamp: "2026-09-30T13:00:00Z",
  lastQuoteAt: "2026-09-30T13:00:00Z",
  detail: "Connected",
  subscribedSymbols: 3866,
};

describe("FeedStatusBar", () => {
  it("shows subscription count while live", () => {
    render(<FeedStatusBar health={health} />);
    expect(screen.getByText("LIVE")).toBeInTheDocument();
    expect(screen.getByText(/3,866 symbols/)).toBeInTheDocument();
  });

  it("warns that displayed prices are not current when authentication fails", () => {
    render(<FeedStatusBar health={{
      ...health,
      state: "AuthenticationFailed",
      detail: "ICE rejected the configured credentials",
    }} />);

    expect(screen.getByText("AUTHENTICATION FAILED")).toBeInTheDocument();
    expect(screen.getByText(/PRICES SHOWN ARE NOT CURRENT/)).toBeInTheDocument();
  });
});
