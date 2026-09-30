import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearPerformanceRecordsEvery } from "@/application/dev/performance-records";

// A stand-in for the browser's performance timeline: counts how often each kind of record is cleared.
const timeline = () => ({ clearMeasures: vi.fn(), clearMarks: vi.fn() });

describe("clearPerformanceRecordsEvery", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  // React's development build records every re-render with changed props, with a copy of the props, and the
  // browser keeps every record: the calculator page grew by some 20 MB a second until the tab froze.
  it("clears the timeline's measures and marks once per interval", () => {
    const performance = timeline();
    clearPerformanceRecordsEvery(1000, performance);

    vi.advanceTimersByTime(999);
    expect(performance.clearMeasures).not.toHaveBeenCalled();

    vi.advanceTimersByTime(1);
    expect(performance.clearMeasures).toHaveBeenCalledTimes(1);
    expect(performance.clearMarks).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(3000);
    expect(performance.clearMeasures).toHaveBeenCalledTimes(4);
    expect(performance.clearMarks).toHaveBeenCalledTimes(4);
  });

  it("stops clearing once stopped", () => {
    const performance = timeline();
    const stop = clearPerformanceRecordsEvery(1000, performance);

    vi.advanceTimersByTime(1000);
    stop();
    vi.advanceTimersByTime(5000);

    expect(performance.clearMeasures).toHaveBeenCalledTimes(1);
  });
});
