export const NatGasDomain = {
  symbols: ["NBP", "TTF", "ZTP", "PSV", "CEGH"] as const,

  tenors: [
    "Apr-26",
    "May-26",
    "Jun-26",
    "Jul-26"
  ],

  fields: [
    { key: "bid", label: "Bid", type: "number" },
    { key: "ask", label: "Ask", type: "number" },
    { key: "last", label: "Last", type: "number" },
    { key: "volume", label: "Volume", type: "number" },
    { key: "netChange", label: "Net Change", type: "number" }
  ]
};

export type NatGasSymbol = typeof NatGasDomain.symbols[number];
export type NatGasFieldKey = typeof NatGasDomain.fields[number]["key"];