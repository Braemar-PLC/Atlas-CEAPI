import { NatGasDomain } from "./natgas-domain";

export interface NatGasViewDefinition {
  key: string;
  title: string;
  route: string;
  type: "grid" | "chart" | "widget";
  symbols: string[];
  tenors?: string[];
  fields?: string[];
}

export const NatGasViews: NatGasViewDefinition[] = [
  // First on purpose: every desk screen takes its columns from NatGasViews[0] (see natgas.view-model.ts).
  // It replicates ICE's "Nat Gas TTF Flat Price" screen: same title, same 12 columns in the same order.
  // Which ROWS a screen shows is not decided here any more - the API says (GET /api/screens/{key}).
  // "bidBar" and "askBar" are the red and green bars, not data (see natgas.col-defs.ts).
  {
    key: "natgas-ttf-flat-price",
    title: "Nat Gas TTF Flat Price",
    route: "/natgas",
    type: "grid",
    symbols: ["TTF"],
    fields: [
      "bidBar", "bidSize", "bid", "ask", "askSize",
      "askBar", "last", "netChange", "settle", "high", "low", "volume", "blockVolume"
    ]
  },

  {
    key: "natgas-nbp-overview",
    title: "NBP Overview",
    route: "/natgas/nbp",
    type: "grid",
    symbols: ["NBP"],
    fields: ["bid", "ask", "last", "volume"]
  },

  {
    key: "natgas-ttf-curve",
    title: "TTF Curve",
    route: "/natgas/ttf",
    type: "chart",
    symbols: ["TTF"],
    tenors: NatGasDomain.tenors,
    fields: ["last"]
  },

  {
    key: "natgas-multi-gas-widget",
    title: "Gas Front Months",
    route: "/natgas/front",
    type: "widget",
    symbols: ["NBP", "TTF", "ZTP"],
    tenors: ["Apr-26"],
    fields: ["last", "netChange"]
  }
];