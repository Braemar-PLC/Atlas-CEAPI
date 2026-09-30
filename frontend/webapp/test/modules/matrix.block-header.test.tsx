import { describe, it, expect, beforeEach } from "vitest";
import { act, render, screen } from "@testing-library/react";
import type { IHeaderGroupParams } from "ag-grid-community";
import { useNatGas } from "@atlas/data";
import { MatrixBlockHeader } from "@/application/modules/calculator/matrix.block-header";

// AG Grid hands a header component its own params; the block header only reads the ones it was given.
const params = { code: "X26", hub: "TTF", futureLabel: "Nov26", decimals: 3 } as unknown as IHeaderGroupParams & {
  code: string; hub: "TTF"; futureLabel: string; decimals: number;
};

describe("MatrixBlockHeader", () => {
  beforeEach(() => useNatGas.getState().clear());

  it("shows the expiry's code alone until its future has a price", () => {
    render(<MatrixBlockHeader {...params} />);

    expect(screen.getByText("X26")).toBeTruthy();
  });

  it("shows the future's traded price beside the code, and follows it as it ticks", () => {
    render(<MatrixBlockHeader {...params} />);

    act(() => useNatGas.getState().patch({ instrument: "TTF", tenor: "Nov26", last: 71.691 }));
    expect(screen.getByText("X26 71.691")).toBeTruthy();

    act(() => useNatGas.getState().patch({ instrument: "TTF", tenor: "Nov26", last: 71.72 }));
    expect(screen.getByText("X26 71.720")).toBeTruthy();
  });

  it("ignores other futures", () => {
    render(<MatrixBlockHeader {...params} />);

    act(() => useNatGas.getState().patch({ instrument: "TTF", tenor: "Dec26", last: 70 }));
    expect(screen.getByText("X26")).toBeTruthy();
  });
});
