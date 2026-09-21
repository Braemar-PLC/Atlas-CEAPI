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