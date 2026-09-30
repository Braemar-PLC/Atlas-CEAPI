import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, renderHook } from "@testing-library/react";
import { useThrottled } from "@/application/modules/screen/screen.hooks";

describe("useThrottled", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  const render = () => renderHook(({ value }) => useThrottled(value, 200), { initialProps: { value: 1 } });

  it("shows the first value at once", () => {
    const { result } = render();

    expect(result.current).toBe(1);
  });

  it("shows a change after a quiet spell straight away", () => {
    const { result, rerender } = render();

    rerender({ value: 2 });
    act(() => { vi.advanceTimersByTime(0); });

    expect(result.current).toBe(2);
  });

  // The calculator reads the prices through this: its matrix prices some four thousand cells per build.
  it("shows at most one new value per interval, and always the latest", () => {
    const { result, rerender } = render();
    rerender({ value: 2 });
    act(() => { vi.advanceTimersByTime(0); });

    act(() => { vi.advanceTimersByTime(10); });
    rerender({ value: 3 });
    act(() => { vi.advanceTimersByTime(40); });
    rerender({ value: 4 });
    act(() => { vi.advanceTimersByTime(100); });
    rerender({ value: 5 });
    act(() => { vi.advanceTimersByTime(49); });
    expect(result.current).toBe(2);

    act(() => { vi.advanceTimersByTime(1); });
    expect(result.current).toBe(5);
  });
});
