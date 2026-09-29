// Which columns each desk screen shows and how it formats its prices. Which ROWS a screen shows is the API's
// business (GET /api/screens/{key}); this file only says what to show for each row, by the API's screen key.
//
// Gas screens replicate ICE's "Nat Gas TTF Flat Price" screen: Strip and its 12 columns, prices to 3 decimals.
// Coal screens replicate WebICE's Coal tab: Product and Hub in front of Strip, OI and WAP among the figures,
// prices to 2 decimals ($/t), and the implied block at the right - the bid and offer the spread markets imply
// for an outright, worked out in the browser from the spreads screen (screen/implied.ts) with the sizes ICE sends.
// "bidBar" and "askBar" are the red and green bars, not data (see screen.col-defs.ts).

export const ScreenFieldKeys = [
  "product", "hub", "tenor",
  "bidBar", "bidSize", "bid", "ask", "askSize", "askBar",
  "last", "netChange", "settle", "openInterest", "high", "low", "wap", "volume", "blockVolume",
  "impliedBidSize", "impliedBid", "impliedAsk", "impliedAskSize",
] as const;

export type ScreenFieldKey = (typeof ScreenFieldKeys)[number];

export interface ScreenView {
  fields: readonly ScreenFieldKey[];
  /** Decimals for every price column: ICE's finest gas tick is 0.005 (3), coal trades in cents (2). */
  priceDecimals: number;
  /** The spreads screen whose rows imply prices for this screen's outrights. Only the coal flat screens have one. */
  impliedFrom?: string;
}

export const GasView: ScreenView = {
  fields: [
    "tenor", "bidBar", "bidSize", "bid", "ask", "askSize", "askBar",
    "last", "netChange", "settle", "high", "low", "volume", "blockVolume",
  ],
  priceDecimals: 3,
};

export const CoalView: ScreenView = {
  fields: [
    "product", "hub", "tenor", "bidBar", "bidSize", "bid", "ask", "askSize", "askBar",
    "last", "netChange", "settle", "openInterest", "high", "low", "wap", "volume", "blockVolume",
    "impliedBidSize", "impliedBid", "impliedAsk", "impliedAskSize",
  ],
  priceDecimals: 2,
};

// The screens laid out differently from gas, by the API's screen key.
const ScreenViews: Record<string, ScreenView> = {
  "coal-api2": { ...CoalView, impliedFrom: "coal-spreads" },
  "coal-newcastle": { ...CoalView, impliedFrom: "coal-spreads" },
  "coal-spreads": CoalView,
};

/** The view for a screen. Anything not listed is laid out like the gas screens, so a new gas screen needs no entry. */
export function screenView(key: string): ScreenView {
  return ScreenViews[key] ?? GasView;
}
