import "@testing-library/jest-dom";
import React from "react";
import { describe, it, expect, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { act } from "@testing-library/react";

import type { NatGasCurveMap, NatGasQuote } from "@atlas/data";

// --- Mock the module the component imports ---
vi.mock("@/application/sources", () => {
  const ReactNs = React;

  const initialCurves: NatGasCurveMap = {
    NBP: {
      Mar26: {
        exchange: "ICE",
        commodity: "GAS",
        instrument: "NBP",
        tenor: "Mar26",
        bid: 10,
        ask: 11,
        last: undefined,
        netChange: 0.1,
        prevSettle: 9.9,
        volume: 100,
      },
    },
  };

  let setCurvesRef:
    | React.Dispatch<React.SetStateAction<NatGasCurveMap>>
    | null = null;

  const stop = vi.fn();

  function getNatGas() {
    const [curves, setCurves] = ReactNs.useState<NatGasCurveMap>(initialCurves);

    ReactNs.useEffect(() => {
      setCurvesRef = setCurves;
      return () => {
        setCurvesRef = null;
        stop();
      };
    }, []);

    return { curves, stop };
  }

  function __emitCurve(
    instrument: string,
    tenor: string,
    patch: Partial<NatGasQuote>
  ) {
    if (!setCurvesRef) return;
    setCurvesRef((prev) => {
      const instMap = prev[instrument] ?? {};
      const existing = instMap[tenor] ?? {};
      return {
        ...prev,
        [instrument]: {
          ...instMap,
          [tenor]: { ...existing, ...patch },
        },
      };
    });
  }

  return { getNatGas, __emitCurve };
}, { virtual: true });

// Import AFTER mock
import { NatGasGridPage } from "../../src/routes/NatGasGridPage";

describe("NatGasGridPage", () => {
  it("renders initial curves and updates on delta", async () => {
    render(<NatGasGridPage />);

    await waitFor(() => {
      expect(screen.getByText(/NatGas/i)).toBeInTheDocument();
      expect(screen.getByText(/"bid":\s*10/)).toBeInTheDocument();
      expect(screen.getByText(/"ask":\s*11/)).toBeInTheDocument();
    });

    const { __emitCurve } = await import("@/application/sources");
    act(() => {
      __emitCurve("NBP", "Mar26", {
        last: 10.6,
        bid: 10.5,
        ask: 11.2,
        volume: 120,
      });
    })

    await waitFor(() => {
      expect(screen.getByText(/"last":\s*10\.6/)).toBeInTheDocument();
      expect(screen.getByText(/"bid":\s*10\.5/)).toBeInTheDocument();
      expect(screen.getByText(/"ask":\s*11\.2/)).toBeInTheDocument();
      expect(screen.getByText(/"volume":\s*120/)).toBeInTheDocument();
    });
  });
});