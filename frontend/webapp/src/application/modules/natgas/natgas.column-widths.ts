// Remembers the column widths a user has dragged, per desk screen, in the browser's own storage (localStorage).
// That storage belongs to one browser on one machine: the wall screen keeps its widths, a laptop keeps its own,
// and nothing is sent to the API.
//
// Only the columns someone actually dragged are stored. The rest keep sharing the leftover width between them
// (AG Grid's `flex`), so the grid still fills the window whatever size it is.

/** Column id (its field name: "bid", "tenor" …) → width in pixels. */
export type ColumnWidths = Record<string, number>;

/** The part of `localStorage` used here, so a test can pass in a stand-in. */
type WidthStorage = Pick<Storage, "getItem" | "setItem" | "removeItem">;

const storageKey = (screenKey: string) => `atlas.column-widths.${screenKey}`;

// Storage can be switched off (private windows, company policy) and then throws when touched.
// Remembering widths is a convenience, so every failure just means "nothing remembered".
const browserStorage = (): WidthStorage | undefined => {
  try {
    return window.localStorage;
  } catch {
    return undefined;
  }
};

/** The widths saved for a screen. Empty if nothing is saved, or if what is saved cannot be trusted. */
export function loadColumnWidths(screenKey: string, storage = browserStorage()): ColumnWidths {
  try {
    const parsed: unknown = JSON.parse(storage?.getItem(storageKey(screenKey)) ?? "{}");
    if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) return {};

    // Keep only sensible entries, so a damaged value can never give the grid a zero-width or NaN column.
    const widths: ColumnWidths = {};
    for (const [colId, width] of Object.entries(parsed)) {
      if (typeof width === "number" && Number.isFinite(width) && width > 0) widths[colId] = Math.round(width);
    }
    return widths;
  } catch {
    return {};
  }
}

/** Adds the newly dragged columns to what is already saved for the screen, and returns the result. */
export function saveColumnWidths(screenKey: string, changed: ColumnWidths, storage = browserStorage()): ColumnWidths {
  const widths = { ...loadColumnWidths(screenKey, storage), ...changed };
  try {
    storage?.setItem(storageKey(screenKey), JSON.stringify(widths));
  } catch {
    // Storage full or blocked: the widths still apply until the page is reloaded.
  }
  return widths;
}

/** Forgets a screen's widths, so its columns go back to sharing the window as designed. */
export function clearColumnWidths(screenKey: string, storage = browserStorage()): void {
  try {
    storage?.removeItem(storageKey(screenKey));
  } catch {
    // Nothing to do: if storage cannot be reached, nothing was remembered either.
  }
}
