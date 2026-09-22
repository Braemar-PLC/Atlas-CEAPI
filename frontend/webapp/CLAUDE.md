# Atlas frontend — project notes

Browser grid for the Atlas pricing stream. Read the root notes first (`../../.claude/CLAUDE.md`): they cover the
whole system, the wire formats, and the working agreements. This file only adds what is specific to the frontend.
Verified against the code on 2026-09-18.

## Commands — run everything from `frontend/webapp/`, never from `frontend/`

| Task | Command |
|---|---|
| Install | `npm install` (npm workspaces; one lockfile, here) |
| Dev server | `npm run dev` → http://localhost:5173 |
| Tests, once | `npx vitest run` (`npm test` starts watch mode in a terminal) |
| One test file | `npx vitest run packages/data` (any path fragment) |
| Type-check | `npx tsc -b` |
| Lint | `npm run lint` |
| Production build | `npm run build` (type-check, then Vite → `dist/`) |

To see live-looking prices, start the mock feed and the API first (root notes, "Running locally"), then
`npm run dev` and open http://localhost:5173/natgas. `vite.config.ts` forwards `/api/*` to `https://localhost:7001`.

`frontend/package-lock.json` (one directory up) is an empty stray from running `npm install` one folder too high.
It is not used; the real lockfile is the one here. It arrived with the merge of 2026-09-21 (root notes, top).

## Baseline on 2026-09-21 — do not make these numbers worse

- **Tests:** 106 pass in 12 files (67 in 8 before the desk-screens and grid-sizing work of 2026-09-21). A 13th file, `test/pages/NatgasGridPage.test.tsx`, fails to load because it mocks
  `@/application/sources`, which no longer exists. That failure is inherited, not an environment problem.
- **Type-check:** clean.
- **Lint:** 15 errors, all inherited — mostly `no-explicit-any`, plus two `react-hooks/rules-of-hooks`
  (`src/routes/trades/$tradeId/route.tsx`, and the broken test above). New code must not add to them.

## Layout

```
src/routes/            TanStack Router, file-based. One file = one URL.
src/application/
  modules/natgas/      index.tsx (lifecycle) → natgas.view-model.ts (store → rows) → natgas.component.tsx (grid only)
  registries/          Static config: symbols, tenors, fields, view definitions
  subscriptions/       Ref-counted shared SSE connection; writes into the store
packages/data/         @atlas/data — TypeBox schemas, the NatGasSource port, the Zustand store (useNatGas)
packages/external/     @atlas/external — EventSource adapter implementing the port, DTO → domain mapper
```

Dependencies point one way: `src` → `@atlas/external` → `@atlas/data`. `@atlas/data` must never import from
`external` or `src`. This mirrors the hexagonal layout of the .NET side: ports in the core, adapters outside it.

**The API decides which rows a screen shows; the browser draws exactly those** (since 2026-09-21 — root notes,
"Desk screens"). Nothing in the frontend lists strips or ICE symbols any more, so nothing here needs touching when a
contract expires. Each route passes its screen's key to the one module: `/natgas` → `ttf-flat`,
`/ttf-time-spread` → `ttf-spreads`, `/nbp` → `nbp`.

Data flow: `index.tsx` calls `fetchScreen(key)` (`http-screen.adapter.ts` → `GET /api/screens/{key}`, checked against
`ScreenSchema`) and re-asks every 15 minutes so an open wall screen follows a roll by itself → with the screen in
hand, the subscriptions manager opens `EventSource` on `/api/screens/{key}/stream` (one counted connection per
screen) → `sse-pricing.adapter.ts` listens for the API's named `snapshot` events and checks each against
`PricingStreamEventSchema` → `pricing-stream-event.mapper.ts` turns ICE field IDs into a quote (bid 20, ask 21,
last 19, …). **Which row a price belongs to comes from its symbol**, looked up in `stripsBySymbol(screen)`; only
for a symbol the screen does not list does it fall back to ICE's fields 951 (hub) and 971 (strip name). That is
what lets ICE's spread contracts work, whatever they carry in those fields → `patch` on the store →
`buildScreenRows` in the view-model makes one grid row per screen row, in the screen's order, blank until its price
arrives (as on ICE) → AG Grid, which matches rows by `symbol|tenor` (`getRowId`) so a tick updates the changed
cells in place instead of redrawing the grid.
**Two kinds of row.** `source: "quoted"` — ICE quotes it (every flat strip, and every same-kind spread): the row is
the store's quote for that label. `source: "computed"` — no such contract (a month against a quarter):
`computeSpreadQuote` works bid and offer out from the two legs, crossed (`bid = near bid − far offer`,
`offer = near offer − far bid`), rounded to 3 decimals; last, change, settle, high, low and volume stay blank.
The legs are streamed and stored even when they are not rows themselves. The tests use the desk's own Edgeview
figures (Oct26 75.925/75.965 against Q1 27 73.640/73.740 → 2.185/2.325).
All three screens use ICE's 12 columns from `NatGasViews[0].fields`; the other entries in `natgas-views.ts` and
`NatGasDomain.tenors` (hyphenated `Apr-26` style) are leftovers nothing live uses.
Every API snapshot carries the symbol's full state, so the store's `push` (replace everything) is not used.

## The grid replicates the ICE screen (desk's ask, 18 Sep 2026)

"Replicate the ICE screens as close as possible … clean black background with white numbers, a red column next to
the bid and green for the sell … I like the colour change on the price column." Display-only, no logo or colour
scheme inside the grid. The reference is the 17 Sep photo of ICE's "Nat Gas TTF Flat Price" screen.

- `natgas-views.ts` — the first view carries ICE's title and ICE's 12 columns in ICE's order: Strip, B Qty, Bid,
  Offer, O Qty, Last, Change, Settle, High, Low, Volume, Block Vol. `bidBar`/`askBar` in its `fields` list are the
  red and green bars, not data. `test/modules/natgas.col-defs.test.ts` pins the order, headers and formats.
- `natgas.col-defs.ts` — headers, 3-decimal prices, whole-number quantities, right alignment.
- `natgas.theme.ts` — grid-wide colours through AG Grid's Theming API (`themeQuartz.withParams`). The old
  `ag-theme-alpine` class did nothing in AG Grid 35 and is gone.
- `natgas.css` — what a theme cannot express: title bar, the bars, faint row lines, the tick square after Last.
- **Figure size (desk's ask, 21 Sep 2026 — Marc Jarvis: "slightly larger with less space around them … fill the
  space a bit better without becoming overcrowded"):** 18px regular in the same 30px rows (it was 15px), side
  padding 5px (was 8). **Not bold:** Harrison Lee first asked for "thicker numbers", bold was tried, and once the
  team saw it they preferred the figures as they were, only larger — do not bring bold back without asking them.
  Size and padding are in `natgas.theme.ts`; the weight (400) is in `natgas.css`, because the theme has no setting
  for it. Headers stay regular, 15px. The layout is unchanged.
  Strip's starting share of the width went from 115 to 135 so `Winter28/Summer29` still fits on a laptop.
- **Column widths (Harrison Lee, 21 Sep 2026: "column width should be a slider"):** drag the line between two
  headers; double-click it to fit the column to its contents. AG Grid allowed this all along, but nothing showed it
  and nothing was remembered. Now the handle is drawn darker and full height (`headerColumnResizeHandle*` in the
  theme), a dragged column's width is saved per screen in the browser's `localStorage`
  (`natgas.column-widths.ts`, key `atlas.column-widths.<screen>`) and re-applied when the grid opens, and
  **"Reset columns"** appears in the title bar once anything has been dragged. Only dragged columns are pinned to
  a width — the rest keep `flex`, so the grid still fills the window. The bars cannot be resized. Widths live in
  one browser on one machine: the wall screen and a laptop each keep their own, by design.
  **Bug fixed the same day (found by Sean testing it): a dragged width snapped back a fraction of a second
  later, on the next price tick.** Two causes, both fixed, either alone would do it. (1) The grid's `defaultColDef`,
  `rowSelection` and `getRowId` were written inline in the JSX, so every render — four ticks a second, plus the
  clock — handed AG Grid brand-new objects; a new `defaultColDef` makes it re-read the column definitions.
  They are now constants outside the component. (2) The columns used `flex`, which AG Grid re-applies whenever it
  re-reads the definitions; they now use `initialFlex`, which is only used when a column is created.
  **Rules that follow: never pass AG Grid an object or function literal as a prop in a component that re-renders
  on ticks, and never put `flex`/`width` on a column a user may resize — use `initialFlex`/`initialWidth`.**
  `test/modules/natgas.component.test.tsx` renders the real grid, sets a width, sends ticks and checks it held;
  it failed before the fix. The sizes and the look have been seen by eye by Sean; nothing is screenshot-tested.
- **"Freeze panes":** `__root.tsx` makes the page exactly one window tall (top bar + `<main>` filling the rest), the
  ICE screen fills `<main>`, and the grid takes whatever height is left and scrolls its rows inside itself — so the
  title bar and column headers never scroll away. Do not go back to `domLayout="autoHeight"`: that made the whole
  page scroll and the headers with it. Black shows below the rows when they all fit.
- **"Prices as of HH:MM:SS"** sits top right, at the right-hand end of the title bar (`.ice-screen-asof`). It is
  the laptop's local time **minus the delay ICE reports** in field 47 (`delayMinutes` on each quote — 10 on the
  Sep 2026 trial), ticking once a second. It is read from the feed, not hard-coded, so it becomes the current time by
  itself if ICE switches the trial to real-time. Blank until a row has arrived. The clock (`useNow`) lives in the
  module `index.tsx`; the arithmetic (`formatPricesAsOf`, `delayMinutesOf`) is in the view-model and is tested.
- **Banded rows:** every other row is dark grey (`oddRowBackgroundColor` in `natgas.theme.ts`), as on the ICE wall
  screens — Sean's ask, to help read across. The bars and the blue selected row keep their own colours.
- **Logo:** `public/braemar-logo-light.png` is the official file from braemar.com (`/media/veng2tex/…`, 2026×610,
  transparent, off-white wordmark — the "light" version is the one for dark backgrounds; the site also has a
  "dark" one for white pages). It sits at the left of the black top bar in `src/routes/__root.tsx`, outside the
  grid — the desk asked for nothing branded *inside* the grid. Do not redraw or recolour it.
- **Tick colour** (green square = Last rose, red = fell) is worked out by the store's `patch`, comparing each new
  Last with the previous one; a row has no square until its Last has changed once. ICE field 40
  (`LRT_TYPE_UPDOWNTICKS`, e.g. `+-+-`) might give it directly, but it never changes in the recording and the SDK
  docs do not say which end is newest — check against a live feed before using it.

## Conventions

- File names are kebab-case with a role suffix: `natgas.view-model.ts`, `natgas.col-defs.ts`, `natgas-source.port.ts`,
  `http-natgas.adapter.ts`, `natgas-quote.mapper.ts`. Components that are not part of a module are `PascalCase.tsx`.
- Define a TypeBox schema first and derive the type with `Static<typeof Schema>`. Do not hand-write a type that
  duplicates a schema. Anything arriving from the network is checked with `Value.Check` before it reaches the store.
- The store uses Zustand with the Immer middleware, so "mutating" the draft inside `set` is correct there and
  nowhere else.
- Components stay dumb: props in, JSX out. Connections and effects live in the module `index.tsx`; shaping data
  lives in the view-model.
- Imports: `@/` is `src/`. `@atlas/data` and `@atlas/external` resolve through npm workspaces in the app and through
  explicit aliases in `vitest.config.ts` — a new package needs adding in both places.
- TypeScript is strict with `verbatimModuleSyntax` (use `import type` for types) and `erasableSyntaxOnly`
  (no `enum`, no constructor parameter properties — use `as const` objects, as `EventMessageTypes` does).
- Tests: Vitest with globals, jsdom, Testing Library. They live in `test/` folders beside each package's `src/`,
  not next to the source files.

## Gotchas

- `src/routes/routeTree.gen.ts` is generated by the TanStack Router Vite plugin whenever the dev server or a build
  runs. It is committed. Never edit it by hand; add or rename a route file and let it regenerate.
- `src/ag-grid-setup.ts` must be imported before any grid renders (`main.tsx` does this). It registers every
  Community and Enterprise module and sets Braemar's AG Grid Enterprise licence key. The key only covers AG Grid
  versions released before **24 April 2026** and has no deployment add-on. `package.json` says `^35.1.0`, so a casual
  `npm update` can pull a release the key does not cover, which puts a watermark on the grid. Do not bump
  `ag-grid-*` without a renewed key, and do not paste the key anywhere outside this repo.
- **Run `npm install` from a terminal whose path is spelled exactly `D:\Source\Atlas\frontend\webapp`, capitals
  included.** npm links `packages/data` and `packages/external` into `node_modules/@atlas/` using the path as typed.
  Windows ignores case but Vite does not: with a link to `D:\Source\atlas\…` it treats the packages as outside the
  project, bundles Zustand with its own private copy of React, and `/natgas` dies with **"Cannot read properties of
  null (reading 'useCallback')"** (hit 2026-09-18; tests, type-check and lint all still pass, so only the browser
  shows it). Check with `Get-Item node_modules\@atlas\data | Select Target`. Fix: remove the two links
  (`[IO.Directory]::Delete(path, $false)` — not `Remove-Item -Recurse`, which would follow the link and delete the
  source), `npm install`, then `npm run dev -- --force` to rebuild Vite's cache.
- Vite is pinned to an **8.0 beta** through `overrides`. Odd build-tool behaviour may be the beta, not your change.
- `vite.config.ts` uses `@vitejs/plugin-react-swc`; `vitest.config.ts` uses `@vitejs/plugin-react`. Both are
  installed. Config changes usually need making in both files.
- **Top bar tabs** come from `src/application/registries/nav-tabs.ts` (label + address). Adding a screen is one
  line there plus a route file in `src/routes/`; `tsc` fails if a tab points at an address with no route. Today:
  TTF Flat (`/natgas`), TTF Time Spread (`/ttf-time-spread`) and NBP (`/nbp`) — all three are live grids since
  2026-09-21, the same module with a different screen key. `ComingSoon` ("not built yet" rather than a faked
  screen) is no longer used by any route; keep it for the next desk's tab. The bar is meant to take the other desks'
  screens too (Coal, …). **RBAC is planned, not started:** tabs will be filtered by desk/group.
- `/` redirects to `/natgas`; there is no home page. Left on disk but no longer linked from anywhere:
  `StreamViewer` (an empty shell), `/about`, `/trades` (router demo code), and `/admin`, which always redirects to
  `/forbidden` (`isAuthorized()` is hard-coded `false`). Decide whether to delete them.
- `src/flash.css` defines `.flash-cell` but nothing uses it — the Last-cell flash uses AG Grid's own mechanism.
- **AG Grid's `enableCellChangeFlash` goes on individual data columns only — never in `defaultColDef`, never on
  the bars.** AG Grid treats a column with no `field` (the red and green bars) as changed on every refresh, and its
  flash rule is `background-color … !important`, so with the flash on everywhere both bars turned permanently grey
  and the grid looked patchy at four ticks a second (seen 2026-09-18). Today only **Last** flashes — the desk's
  ask: highlighter yellow (`valueChangeValueHighlightBackgroundColor` in `natgas.theme.ts`) for 0.3 s with no fade
  (`cellFlashDuration` / `cellFadeDuration` in `natgas.component.tsx`), figure shown in black meanwhile
  (`natgas.css`). Two tests guard this: only Last flashes; the bars never do.

## Known problems (unfixed)

1. ~~**Not wired to the real backend** — the URL, the message shape and the SSE event name all differed, the dev
   server had no `/api` proxy, and the backend's model (one ICE symbol with numeric field IDs) needed mapping to the
   frontend's (instrument + tenor).~~ **Fixed 2026-09-18** with a new adapter, schema and mapper in
   `packages/external` (see Data flow). Checked by unit tests and by reading the stream through the dev-server proxy;
   the grid itself was checked by eye only. What is left:
   - Reset and Remove events from the API are ignored (`mapPricingStreamEvent` returns `undefined`), so a row is
     never cleared or removed. The store has no verb for removing a quote yet.
   - `http-natgas.adapter.ts` (the old adapter for `/api/natgas/stream`, which no backend serves) is now unused by
     the app but still exported and tested. Decide whether to delete it.
   - ~~`TtfFlatPriceStrips` in `natgas-domain.ts` is a hard-coded copy of the API's symbol list and needs rolling by
     hand as contracts expire.~~ **Fixed 2026-09-21:** the list is gone; the API says which rows to draw.
2. `packages/external/test/mocks/mockConnectServer.ts` reads `dummyData/dummyNatGas.*.json`, but the files are
   `dummy-data/dummyFutures.*.json` (missed in the futures → natgas rename). It throws on start. It also emits named
   `snapshot`/`update` events, which the adapter's `onmessage` would never receive.
3. `test/pages/NatgasGridPage.test.tsx` is stale — see Baseline.
4. `natgas-quote.mapper.ts` and its DTO schema are tested but not used by the running app.
5. ~~The view-model always uses `NatGasViews[0]`, so the NBP data that arrives is not shown anywhere.~~
   **Fixed 2026-09-21:** each tab draws the screen the API describes. `NatGasViews[0]` now only supplies the column
   list; the `route`, `type`, `symbols` and other entries in `natgas-views.ts` are unused leftovers.
   New, small: every screen shows 3 decimals, as the desk's wall screens do, although NBP's tick is 0.01; the
   `group` each row carries ("Months", "Spreads" …) is not drawn yet, so NBP's flat block and spread block run on
   without a divider; and a computed spread row shows no tick square or yellow flash, because it has no Last.
6. **Store bug, reproduced 2026-09-18:** `push()` and `clear()` in `natgas.store.ts` reset `curves` to a shared
   module-level `initialState` object, and from the second `push()` onwards that object gets written into. Result:
   `clear()` restores the second snapshot's data instead of emptying the store, and a snapshot that omits a symbol
   leaves the old symbol in place (push NBP, NBP, then TTF-only → store holds NBP and TTF). The 10 store tests miss
   it because `beforeEach` calls `clear()`, so no test ever pushes twice in a row — which is exactly what a live
   stream does on reconnect. Fix: assign a fresh `{}` each time, and add a test that pushes twice.
7. `README.md` is the untouched Vite template and says nothing about Atlas.
8. **ICE-screen replica, gaps against the desk's photo** (2026-09-18):
   - **B Qty and O Qty are blank, on the mock feed and on live ICE data.** Fields 30/31
     (`LRT_TYPE_BIDSIZE`/`ASKSIZE`) are mapped but the live Level 1 feed does not send them (confirmed 2026-09-18);
     they probably need ICE's market-depth entitlement — root notes, "ICE access". The columns stay, as on ICE.
   - **On the mock feed only 6 rows have prices** (TTF Oct26–Mar27, at March 2026 prices); every other row is drawn
     but blank, and the NBP and spreads tabs are blank throughout: the recording has nothing else. On live ICE data
     the old 20-row screen filled completely (confirmed 2026-09-18, field 971 reading exactly `Winter26`, `Q4 26` …).
     **The new 43-row screens, NBP and the spread contracts have not been seen on live data yet** — root notes,
     Known problems #9. A fresh recording for the mock is still to do. Spread rows the desk can add and remove
     themselves are V2.
   - Block Vol (field 924) is present on only some recorded lines, so some rows show it blank.
   - The look has been checked by eye only — headless Edge is blocked by company policy, so there is no screenshot test.
9. `src/index.css` is still mostly the Vite template. On 2026-09-18 its `body { display: flex; place-items:
   center }` was removed, because it floated the ICE screen in the middle of the page with a gap above. The page
   around the grid is still the template's dark grey.
   **Sizing is fixed, not scaled to the screen:** 30px rows and 18px figures suit a laptop; the desk's wall screen
   shows the same 20 rows filling a whole TV, so a wall-screen mode would need sizes tied to the viewport height.
10. The dev server logs "Route file …/routeTree.gen.ts does not export a Route" on start. Inherited and harmless.
