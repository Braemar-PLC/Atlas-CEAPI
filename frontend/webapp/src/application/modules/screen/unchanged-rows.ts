/**
 * The rows to hand the grid next: `next`, except that a row whose figures are the same as before is replaced by
 * the object the grid already has, and the previous list itself comes back when no row changed at all.
 *
 * The view-models build fresh row objects on every price batch. AG Grid treats a new object as a changed row and
 * refreshes it, and each refresh that changes nothing on screen leaves React holding a small update record until
 * that row or cell is next drawn - for a row that never changes, never (React queues an update even when it skips
 * the redraw; ScreenComponent's timed redraw releases what still builds up). Handing back the same object tells the
 * grid there is nothing to do for that row.
 */
export function keepUnchangedRows<T>(previous: readonly T[], next: readonly T[], idOf: (row: T) => string): T[] {
  if (previous.length === 0) return next as T[];

  const before = new Map(previous.map(row => [idOf(row), row]));
  let nothingChanged = previous.length === next.length;

  const rows = next.map((row, index) => {
    const old = before.get(idOf(row));
    if (old !== undefined && sameContent(old, row)) {
      if (previous[index] !== old) nothingChanged = false;
      return old;
    }
    nothingChanged = false;
    return row;
  });

  return nothingChanged ? (previous as T[]) : rows;
}

const isPlainObject = (value: object) => {
  const prototype = Object.getPrototypeOf(value);
  return prototype === Object.prototype || prototype === null;
};

/**
 * True when two rows' figures are the same: numbers, text and flags compared by value, lists element by element,
 * plain objects field by field (a chain's quotes, a matrix's cells). Anything else, such as a date, only equals
 * itself.
 */
export function sameContent(a: unknown, b: unknown): boolean {
  if (Object.is(a, b)) return true;
  if (typeof a !== "object" || typeof b !== "object" || a === null || b === null) return false;

  if (Array.isArray(a) || Array.isArray(b)) {
    if (!Array.isArray(a) || !Array.isArray(b) || a.length !== b.length) return false;
    return a.every((value, index) => sameContent(value, b[index]));
  }

  if (!isPlainObject(a) || !isPlainObject(b)) return false;
  const aFields = a as Record<string, unknown>;
  const bFields = b as Record<string, unknown>;
  const keys = Object.keys(aFields);
  if (keys.length !== Object.keys(bFields).length) return false;
  return keys.every(key => Object.prototype.hasOwnProperty.call(bFields, key) && sameContent(aFields[key], bFields[key]));
}
