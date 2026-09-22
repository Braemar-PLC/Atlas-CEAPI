import { describe, it, expect } from "vitest";
import {
  clearColumnWidths, loadColumnWidths, saveColumnWidths,
} from "@/application/modules/natgas/natgas.column-widths";

// A stand-in for the browser's localStorage.
const fakeStorage = (initial: Record<string, string> = {}) => {
  const items = new Map(Object.entries(initial));
  return {
    items,
    getItem: (k: string) => items.get(k) ?? null,
    setItem: (k: string, v: string) => { items.set(k, v); },
    removeItem: (k: string) => { items.delete(k); },
  };
};

// What a browser with storage switched off does.
const brokenStorage = {
  getItem: () => { throw new Error("blocked"); },
  setItem: () => { throw new Error("blocked"); },
  removeItem: () => { throw new Error("blocked"); },
};

describe("remembered column widths", () => {
  it("has nothing to give back for a screen nobody has adjusted", () => {
    expect(loadColumnWidths("ttf-flat", fakeStorage())).toEqual({});
  });

  it("gives back what was saved", () => {
    const storage = fakeStorage();

    saveColumnWidths("ttf-flat", { bid: 120, tenor: 200 }, storage);

    expect(loadColumnWidths("ttf-flat", storage)).toEqual({ bid: 120, tenor: 200 });
  });

  it("keeps each screen's widths apart", () => {
    const storage = fakeStorage();

    saveColumnWidths("ttf-flat", { bid: 120 }, storage);
    saveColumnWidths("nbp", { bid: 90 }, storage);

    expect(loadColumnWidths("ttf-flat", storage)).toEqual({ bid: 120 });
    expect(loadColumnWidths("nbp", storage)).toEqual({ bid: 90 });
  });

  it("adds a newly dragged column to the ones already remembered", () => {
    const storage = fakeStorage();
    saveColumnWidths("ttf-flat", { bid: 120 }, storage);

    const all = saveColumnWidths("ttf-flat", { ask: 110 }, storage);

    expect(all).toEqual({ bid: 120, ask: 110 });
    expect(loadColumnWidths("ttf-flat", storage)).toEqual({ bid: 120, ask: 110 });
  });

  it("lets a later drag of the same column win", () => {
    const storage = fakeStorage();
    saveColumnWidths("ttf-flat", { bid: 120 }, storage);

    saveColumnWidths("ttf-flat", { bid: 150 }, storage);

    expect(loadColumnWidths("ttf-flat", storage)).toEqual({ bid: 150 });
  });

  it("stores whole pixels", () => {
    const storage = fakeStorage();

    saveColumnWidths("ttf-flat", { bid: 120.6 }, storage);

    expect(loadColumnWidths("ttf-flat", storage)).toEqual({ bid: 121 });
  });

  it("forgets a screen's widths on reset, and only that screen's", () => {
    const storage = fakeStorage();
    saveColumnWidths("ttf-flat", { bid: 120 }, storage);
    saveColumnWidths("nbp", { bid: 90 }, storage);

    clearColumnWidths("ttf-flat", storage);

    expect(loadColumnWidths("ttf-flat", storage)).toEqual({});
    expect(loadColumnWidths("nbp", storage)).toEqual({ bid: 90 });
  });

  it("ignores saved values that could break the grid", () => {
    const damaged = JSON.stringify({ bid: 120, ask: 0, last: -5, high: "wide", low: null });
    const storage = fakeStorage({ "atlas.column-widths.ttf-flat": damaged });

    expect(loadColumnWidths("ttf-flat", storage)).toEqual({ bid: 120 });
  });

  it("treats unreadable saved text as nothing saved", () => {
    expect(loadColumnWidths("ttf-flat", fakeStorage({ "atlas.column-widths.ttf-flat": "not json" }))).toEqual({});
    expect(loadColumnWidths("ttf-flat", fakeStorage({ "atlas.column-widths.ttf-flat": "[1,2]" }))).toEqual({});
  });

  it("carries on without remembering when the browser blocks storage", () => {
    expect(loadColumnWidths("ttf-flat", brokenStorage)).toEqual({});
    expect(saveColumnWidths("ttf-flat", { bid: 120 }, brokenStorage)).toEqual({ bid: 120 });
    expect(() => clearColumnWidths("ttf-flat", brokenStorage)).not.toThrow();
  });
});
