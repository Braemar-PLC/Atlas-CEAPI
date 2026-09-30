// The pages of each desk that has screens, in the order the desk asked for, shown as tabs when that desk is
// active. Adding a page is one line here plus its route file in src/routes; `tsc` fails if it points at an
// address with no route. A desk not listed here has no pages yet and shows its own page under /desks/{key}.
export const NaturalGasDeskKey = "natural-gas";
export const CoalDeskKey = "coal";
export const CrossCommoditiesDeskKey = "cross-commodities";

// The admin area sits in the desk bar as a sixth button, for admins only. It is not a desk.
export const AdminKey = "admin";

export const NaturalGasPages = [
  { label: "TTF Flat", to: "/natgas" },
  { label: "TTF Time Spread", to: "/ttf-time-spread" },
  { label: "NBP", to: "/nbp" },
] as const;

// WebICE's Coal tab split three ways: each hub's flat prices, and both hubs' spreads together.
export const CoalPages = [
  { label: "API2 (Rotterdam)", to: "/coal/api2" },
  { label: "Newcastle", to: "/coal/newcastle" },
  { label: "Spreads", to: "/coal/spreads" },
] as const;

// The Cross-Commodities desk ("Xcom") is the options desk: one option chain per product, in the order Sean Hays
// gave on 2026-09-25 (docs/roadmap.md, section 12), then the calculator.
export const CrossCommoditiesPages = [
  { label: "TTF", to: "/xcom/ttf" },
  { label: "EUA", to: "/xcom/eua" },
  { label: "WTI", to: "/xcom/wti" },
  { label: "Brent", to: "/xcom/brent" },
  { label: "Calculator", to: "/xcom/calculator" },
] as const;

// By the desk keys the API seeds (Atlas.Web.Data).
export const DeskPages = {
  [NaturalGasDeskKey]: NaturalGasPages,
  [CoalDeskKey]: CoalPages,
  [CrossCommoditiesDeskKey]: CrossCommoditiesPages,
} as const;

export type DeskPage = (typeof DeskPages)[keyof typeof DeskPages][number];

/** A desk's pages, first page first; empty for a desk that has none. */
export function pagesOf(deskKey: string): readonly DeskPage[] {
  return (DeskPages as Record<string, readonly DeskPage[] | undefined>)[deskKey] ?? [];
}
