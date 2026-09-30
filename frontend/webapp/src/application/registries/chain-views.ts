// How each option chain (the Cross-Commodities desk) formats its prices, by the API's screen key. Which rows a
// chain shows - the expiries, and the strikes around the future - is the API's business (GET /api/screens/{key}).

export interface ChainView {
  /** Decimals for every price column. TTF and EUA options quote in cents of a euro (3, as the gas screens); oil in cents of a dollar (2). */
  priceDecimals: number;
  /**
   * The step between the strikes the calculator's matrix prices: half a point, as the desk's calculator lays its
   * ladder out (46.50, 47.00, 47.50 ...). The model prices any strike, listed by ICE or not.
   */
  strikeStep: number;
}

const Euro: ChainView = { priceDecimals: 3, strikeStep: 0.5 };
const Dollar: ChainView = { priceDecimals: 2, strikeStep: 0.5 };

const ChainViews: Record<string, ChainView> = {
  "xcom-ttf": Euro,
  "xcom-eua": Euro,
  "xcom-brent": Dollar,
  "xcom-wti": Dollar,
};

/** The view for a chain. A key not listed is shown to 2 decimals. Same object for the same key, so the grid's columns are built once. */
export function chainView(key: string): ChainView {
  return ChainViews[key] ?? Dollar;
}
