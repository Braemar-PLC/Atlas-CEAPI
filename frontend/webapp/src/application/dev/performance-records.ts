/** How often the development build empties the browser's performance timeline. */
export const PerformanceRecordsClearIntervalMs = 1000;

type Timeline = Pick<Performance, "clearMeasures" | "clearMarks">;

/**
 * Development only: empties the browser's performance timeline on a timer, and returns a function that stops it.
 * React's development build records every re-render whose props changed as a performance measure carrying a copy
 * of the changed props, and the browser keeps every measure until the tab closes. The grids get new rows several
 * times a second, so the records grow without end (about 20 MB a second on the calculator page) until the tab runs
 * the machine out of memory. The production build writes no such records. The browser's own profiler still sees
 * every measure while it is recording; clearing only drops the kept copies.
 */
export function clearPerformanceRecordsEvery(intervalMs: number, timeline: Timeline = performance): () => void {
  const timer = setInterval(() => {
    timeline.clearMeasures();
    timeline.clearMarks();
  }, intervalMs);
  return () => clearInterval(timer);
}
