import { describe, expect, it } from "vitest";
import { keepUnchangedRows, sameContent } from "@/application/modules/screen/unchanged-rows";

type Row = { symbol: string; tenor: string; bid?: number; call?: { bid?: number; ask?: number }; cells?: Record<string, { vol?: number }> };
const idOf = (row: Row) => `${row.symbol}|${row.tenor}`;

describe("keepUnchangedRows", () => {
  it("keeps the object the grid already has for a row whose figures did not change", () => {
    const previous: Row[] = [{ symbol: "TTF", tenor: "Oct26", bid: 77.85 }, { symbol: "TTF", tenor: "Nov26", bid: 77.1 }];
    const next: Row[] = [{ symbol: "TTF", tenor: "Oct26", bid: 77.9 }, { symbol: "TTF", tenor: "Nov26", bid: 77.1 }];

    const rows = keepUnchangedRows(previous, next, idOf);

    expect(rows[0]).toBe(next[0]);
    expect(rows[1]).toBe(previous[1]);
  });

  it("hands back the previous list itself when no row changed, so the grid has nothing to do", () => {
    const previous: Row[] = [{ symbol: "TTF", tenor: "Oct26", bid: 77.85 }];
    const next: Row[] = [{ symbol: "TTF", tenor: "Oct26", bid: 77.85 }];

    expect(keepUnchangedRows(previous, next, idOf)).toBe(previous);
  });

  it("compares nested figures by value: a chain's quotes and a matrix's cells", () => {
    const previous: Row[] = [
      { symbol: "Nov26", tenor: "80", call: { bid: 1.2, ask: 1.3 } },
      { symbol: "matrix", tenor: "80.00", cells: { Nov26: { vol: 55.1 }, Dec26: { vol: 50.2 } } },
    ];
    const next: Row[] = [
      { symbol: "Nov26", tenor: "80", call: { bid: 1.2, ask: 1.3 } },
      { symbol: "matrix", tenor: "80.00", cells: { Nov26: { vol: 55.1 }, Dec26: { vol: 50.3 } } },
    ];

    const rows = keepUnchangedRows(previous, next, idOf);

    expect(rows[0]).toBe(previous[0]);
    expect(rows[1]).toBe(next[1]);
  });

  it("follows the new order, and adds and drops rows, reusing the ones that stay the same", () => {
    const oct = { symbol: "TTF", tenor: "Oct26", bid: 77.85 };
    const nov = { symbol: "TTF", tenor: "Nov26", bid: 77.1 };
    const previous: Row[] = [oct, nov];
    const next: Row[] = [{ ...nov }, { symbol: "TTF", tenor: "Dec26", bid: 78 }];

    const rows = keepUnchangedRows(previous, next, idOf);

    expect(rows).toHaveLength(2);
    expect(rows[0]).toBe(nov);
    expect(rows[1]).toBe(next[1]);
  });

  it("takes the new list as it is when there was nothing before", () => {
    const next: Row[] = [{ symbol: "TTF", tenor: "Oct26", bid: 77.85 }];

    expect(keepUnchangedRows([], next, idOf)).toBe(next);
  });
});

describe("sameContent", () => {
  it("treats a field that appears or disappears as a change", () => {
    expect(sameContent({ bid: 1 }, { bid: 1, ask: undefined })).toBe(false);
    expect(sameContent({ bid: 1, ask: 2 }, { bid: 1 })).toBe(false);
  });

  it("compares lists element by element", () => {
    expect(sameContent([1, 2], [1, 2])).toBe(true);
    expect(sameContent([1, 2], [2, 1])).toBe(false);
    expect(sameContent([1], { 0: 1 })).toBe(false);
  });

  it("counts a blank-number (NaN) as equal to itself, like any other unchanged figure", () => {
    expect(sameContent({ bid: NaN }, { bid: NaN })).toBe(true);
  });

  it("never calls two different non-plain objects, such as dates, the same", () => {
    expect(sameContent(new Date(0), new Date(1000))).toBe(false);
  });
});
