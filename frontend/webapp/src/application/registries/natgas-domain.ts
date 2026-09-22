// Which strips each screen shows is no longer written here. The API works it out by rule from ICE's contract
// list and the desk's counts, and rolls it as contracts expire: GET /api/screens/{key}
// (webapp/src/Atlas.Web.Core/Domain/Logic/ScreenBuilder.cs; counts in Atlas.Web.Api/appsettings.json, "Screens").
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
    { key: "netChange", label: "Net Change", type: "number" },
    { key: "bidSize", label: "Bid Qty", type: "number" },
    { key: "askSize", label: "Offer Qty", type: "number" },
    { key: "settle", label: "Settle", type: "number" },
    { key: "high", label: "High", type: "number" },
    { key: "low", label: "Low", type: "number" },
    { key: "blockVolume", label: "Block Volume", type: "number" }
  ]
};

export type NatGasSymbol = typeof NatGasDomain.symbols[number];
export type NatGasFieldKey = typeof NatGasDomain.fields[number]["key"];