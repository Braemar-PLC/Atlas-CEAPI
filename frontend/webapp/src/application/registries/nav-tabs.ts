// The tabs in the top bar, left to right. Adding a screen is one line here plus its route file in src/routes.
// The bar is laid out to take many more: the other desks (Coal, …) will add their screens to this list.
// Later, RBAC will decide which tabs a user sees, by desk/group. Nothing of that is built yet.
export const NavTabs = [
  { label: "TTF Flat", to: "/natgas" },
  { label: "TTF Time Spread", to: "/ttf-time-spread" },
  { label: "NBP", to: "/nbp" },
] as const;
