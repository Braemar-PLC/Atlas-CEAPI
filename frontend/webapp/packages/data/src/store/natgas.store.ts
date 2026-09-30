// useNatGas.ts
import { create } from "zustand";
import { immer } from "zustand/middleware/immer";
import type { NatGasCurveMap, NatGasQuote, Instrument } from "../models/natgas.schemas";
//export type FxRateProvider = (opts: { pair: string; tenor: string }) => number | undefined;

type Pair = { instrument: Instrument; tenor: string };

interface NatGasState {
  curves: NatGasCurveMap;
  push: (map: NatGasCurveMap) => void;                // full dataset
  patch: (q: Partial<NatGasQuote>) => void;           // delta update
  patchMany: (qs: readonly Partial<NatGasQuote>[]) => void; // a batch of delta updates, as one state change

  clear: () => void;

  getQuote: (inst: Instrument, tenor: string) => Partial<NatGasQuote> | undefined;
  getMany: (pairs: Pair[]) => (Partial<NatGasQuote> | undefined)[];
  getByTenor: (tenor: string, instruments?: Instrument[]) =>
    Record<Instrument, Partial<NatGasQuote> | undefined>;
  // getByTenorFx: (
  //   tenor: string,
  //   instruments: Instrument[] | undefined,
  //   fxPair: string,
  //   fxRate: FxRateProvider
  // ) => Record<Instrument, Partial<NatGasQuote> | undefined>;
}
const initialState = {} as NatGasCurveMap;

/** Merges one delta into the curves (an immer draft): the strip is created if new, and the tick direction set from the last price. */
function applyDelta(curves: NatGasCurveMap, q: Partial<NatGasQuote>) {
  const { instrument, tenor } = q;
  if (!instrument || !tenor) return;

  curves[instrument] ??= {};
  curves[instrument][tenor] ??= {};
  const quote = curves[instrument][tenor]!;

  // Tick direction: compare the incoming last with the one we already hold.
  const previousLast = quote.last;
  Object.assign(quote, q);
  if (q.last != null && previousLast != null && q.last !== previousLast) {
    quote.tick = q.last > previousLast ? "up" : "down";
  }
}

export const useNatGas = create<NatGasState>()(

  immer((set, get) => ({
    curves: initialState,

    push: (map) =>
      set((s) => {
        s.curves = initialState;
        for (const inst of Object.keys(map) as Instrument[]) {
          s.curves[inst] = {};
          const tenors = map[inst]!;
          for (const tenor of Object.keys(tenors)) {
            s.curves[inst][tenor] = { ...tenors[tenor] };
          }
        }
      }),

    patch: (q) => set((s) => { applyDelta(s.curves, q); }),

    // The stream hands over a few hundred deltas a second on an option chain, and every state change re-renders
    // every open grid; applied together (natgas-subscriptions-manager.ts gathers them) a batch is one render.
    patchMany: (qs) =>
      set((s) => {
        for (const q of qs) applyDelta(s.curves, q);
      }),

    clear: () => set((s) => { s.curves = initialState; }),

    getQuote: (inst, tenor) => get().curves[inst]?.[tenor],

    getMany: (pairs) => pairs.map((p) =>
      get().curves[p.instrument]?.[p.tenor]
    ),

    getByTenor: (tenor, instruments) => {
      const src = get().curves;
      const keys = instruments ?? (Object.keys(src) as Instrument[]);
      const out: Record<Instrument, Partial<NatGasQuote> | undefined> = {} as any;
      for (const inst of keys) out[inst] = src[inst]?.[tenor];
      return out;
    },

    // getByTenorFx: (tenor, instruments, fxPair, fxRate) => {
    //   const base = get().getByTenor(tenor, instruments);
    //   const rate = fxRate({ pair: fxPair, tenor });
    //   if (!rate) return base;

    //   const out: Record<Instrument, Partial<NatGasQuote> | undefined> = {} as any;
    //   for (const inst of Object.keys(base) as Instrument[]) {
    //     const q = base[inst];
    //     out[inst] = q
    //       ? {
    //         ...q,
    //         bid: q.bid != null ? q.bid * rate : undefined,
    //         ask: q.ask != null ? q.ask * rate : undefined,
    //         last: q.last != null ? q.last * rate : undefined,
    //       }
    //       : undefined;
    //   }
    //   return out;
    // },
  }))
);