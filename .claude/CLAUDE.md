# Atlas — project notes

Real-time ICE natural-gas futures pricing (TTF months, e.g. `TFM 26J-ICN`) streamed to a
browser grid. Written by Simon Horridge, Feb–Mar 2026; inherited Sep 2026 at commit `1c41c83`.
No CI. These notes were verified against the code on 2026-09-15.

## Working agreements

Sean owns this codebase and is new to programming. The goal of every change is that Sean understands it and can
maintain it afterwards, not just that it works.

- **Explain in plain English.** Before changing code, say what you will change and why. Afterwards, say what changed
  and how to check it by hand (a command to run, a URL to open). Define a technical term the first time you use it.
- **One problem per change.** No drive-by renames, reformatting or refactors. If you notice something else that is
  wrong, add it to the relevant "Known problems" list instead of fixing it in passing.
- **Prove it works.** There is no CI, so the local run is the only safety net. Before saying a change is done, run the
  tests for the service you touched (for the frontend also `npm run lint` and `npx tsc -b`) and report the real
  numbers against the recorded baselines. Never make a test pass by deleting it, skipping it or loosening its assertion.
- **Bug fixes start with a failing test** that reproduces the bug; then make it pass.
- **Follow the conventions already in the code** (see Conventions, and `frontend/webapp/CLAUDE.md`). Ask before adding
  a dependency, and say what it is for and what the alternative would be.
- **Git:** work on a short-lived branch named for the change (`fix/ice-parser-refresh`), not on `main`. Commit small
  and often, with a message that says why. Commit or push only when Sean asks. The remote is the company's GitHub
  (`Braemar-PLC/Atlas`), so a push is visible to colleagues.
- **Secrets stay out of git and out of chat:** `CEAPI/.env`, ICE credentials, the ICE SDK DLLs. Check `git status`
  before every commit for files that should not be there.
- **Ask first** before deleting files, rewriting git history (`reset --hard`, force-push), or running anything that
  connects to real ICE (`gradlew run` with live credentials, `gradlew integrationTest`).
- **Keep these notes true.** If a change makes a line in a CLAUDE.md wrong, fix that line in the same change, and
  strike through fixed problems with the date rather than deleting them.

## Layout — three independent services

```
ICE feed ──► CEAPI/ (Java 17) ──WebSocket :9002──► webapp/ (.NET) ──SSE──► frontend/ (React)
```

| Dir | What | Build / test |
|---|---|---|
| `CEAPI/` | Stateless relay over ICE's JStandard SDK. Runs a WebSocket *server* on 9002 and only dials ICE while a WS client is connected. | `.\gradlew build` (unit tests), `.\gradlew run`, `.\gradlew integrationTest` (hits real ICE) |
| `webapp/src/` | `Atlas.sln`, hexagonal: `Atlas.Web.Ice` (WS client → `IceParser` → `IceInterpreter` → `PriceMessageHandler`), `Atlas.Web.Core` (`PricingStore`: one Rx `BehaviorSubject<PricingSnapshot>` per symbol; knows nothing of ICE or HTTP), `Atlas.Web.Api` (`GET /api/pricing/stream?symbol=A,B` as SSE). One test project each. | `cd webapp/src; dotnet test Atlas.sln` |
| `tools/ceapi-replay/` | Node stand-in for CEAPI (`ws`); replays `webapp/test-data/ceapidata.json`. Honours `resync`. | `npm install; npm start` (`--port`, `--interval`, `--loop=false`) |
| `frontend/webapp/` | Vite + React 19 + TanStack Router + Zustand + AG Grid. npm workspaces: `packages/data` (schemas, store), `packages/external` (SSE adapter). Own notes: `frontend/webapp/CLAUDE.md`. | `npm install; npm run dev` (:5173), `npx vitest run` |

## Toolchain
- JDK 17 with `JAVA_HOME` set. Gradle comes from the wrapper (9.0) — no global install needed.
- .NET SDK in the 9.0.3xx band (`webapp/src/global.json` pins `9.0.307`, default latestPatch
  roll-forward) **plus** the .NET 8 runtime: app projects target `net8.0`, `Atlas.Web.Api.Tests` targets `net9.0`.
- Node 24 / npm 11.
- **Defender ASR blocks freshly built `.exe` files** in the user profile on Braemar laptops (Defender event 1121,
  rule `01443614-CD74-433A-B99E-2ECDC07BFC25`: "block executables unless prevalence/age/trusted"). `dotnet run`
  failed with "Access is denied" launching `Atlas.Web.Api.exe`. `Atlas.Web.Api.csproj` therefore sets
  `UseAppHost=false`, so it runs as `dotnet Atlas.Web.Api.dll` through the signed host. Test projects are
  unaffected (testhost is signed). Any new exe-producing project needs the same setting.
- On Windows use `curl.exe` for SSE testing; PowerShell's `curl` is an alias for Invoke-WebRequest.
- **VS Code (Java):** root `.vscode/launch.json` has a "Run CEAPI" config (F5). Needs the Extension Pack for Java.
  The Java extension picks whatever `java` is first on PATH to run Gradle — on this machine that was Oracle Java 8,
  which Gradle 9 refuses — so `java.import.gradle.java.home` and `java.configuration.runtimes` are pinned to the
  JDK 17 path in *user* settings (machine-specific, not in the repo). The Gradle build-server importer registers the
  project by folder name, `CEAPI`, not the `settings.gradle` name `ceapi`; `projectName` in the launch config
  must match. Warnings about unused imports in the tests are normal.

## Running locally
1. `CEAPI/.env` (git-ignored via root `.gitignore`): `ICE_HOST`, `ICE_USERNAME`, `ICE_PASSWORD`,
   `SYMBOLS` (comma-separated, **unquoted** — the loader in `build.gradle` does not strip quotes),
   `WS_PORT=9002`. The port must match `Port` in `Atlas.Web.Api/appsettings.json`. The file exists on Sean's machine with the
   17 Sep 2026 trial credentials (see "ICE access"); `ICE_HOST` still needs confirming.
2. Put the ICE SDK native DLLs in `CEAPI/bin/` (see Known problems #7) — nothing works without them.
   **No ICE credentials or DLLs?** `cd tools/ceapi-replay; npm install; npm start` fakes CEAPI on 9002 by replaying
   the recorded feed (47 messages, all 13 symbols, looping); the API cannot tell the difference. Then skip to step 4.
3. `cd CEAPI; .\gradlew run` → logs "waiting for WebSocket client" and idles until the API connects.
4. `cd webapp/src; dotnet run --project Atlas.Web.Api` → `https://localhost:7001` (Swagger at `/swagger`).
   Run `dotnet dev-certs https --trust` once first (UAC prompt); until then use `curl.exe -k`.
   `UseHttpsRedirection` is on, so use https.
5. `curl.exe -N "https://localhost:7001/api/pricing/stream?symbol=TFM%2026J-ICN"`.
   The symbol must be in the configured `Symbols` list or the request 500s.

## ICE access (Connect Enterprise API trial)

Source: email from ICE pre-sales (Francesca Sdanga), 17 Sep 2026, thread "RE: URGENT! Web ICE API" in Sean's
mailbox. **The password and the SDK download access code live only in `CEAPI/.env`** (git-ignored; the download
link is in comment lines at the bottom). Never copy them into a tracked file, a commit message or chat.

- **Status:** trial provisioned 2026-09-17 for user `Braemar_CEAPI`. Braemar asked for 60 days, so expect it to
  lapse around **2026-11-16** — the email does not state an end date; confirm with ICE. Two earlier trials (under
  Simon, then Mizanur Rahman) expired unused, and ICE took weeks to reinstate each time, so do not let this one lapse
  silently. A June 2026 trial was *delayed* data; whether this one is real-time is not stated.
- **Entitled sources** — a symbol outside these will not stream:

  | Source ID | Service | Description |
  |---|---|---|
  | 270 | ICE Endex | Market Data Level 1 (TTF — the `TFM …-ICN` symbols) |
  | 756 | ICE Futures Europe | Commodity Futures Level 1 (NBP — `GWM …-ICE`) |
  | 1321 | OTC Data Services | FX Professional Streaming |

  The source-to-symbol mapping in brackets is inferred from the symbol suffixes, not confirmed by ICE.
- **Symbology:** CEAPI symbols are the **E-Signal ticker** — column `<SYMBOL.ESIGNAL.TICKER>` in ICE's static reference
  files `FTPCSD_<sourceId>.csv.bz2` (unzip first; one row per listed instrument as of the file date). Look symbols up
  there rather than guessing month codes. `frontend/webapp/packages/external/test/dummy-data/FTPCSD_1321.csv` is an
  older copy of the 1321 (FX) file. Files for 270 and 756 come from the share link.
- **Where things are:** SDK, user guide ("Connect Enterprise API – Java API User Guide") and reference files are on
  the share link (see `.env`) and at https://developer.theice.com/ (log in with the trial credentials).
  Wider documentation and developer notices: https://service.ice.com/ (self-registration).
- **ICE contacts:** Francesca Sdanga (pre-sales, real-time feeds — sent the credentials), Malorie Han (covers the
  Connect Enterprise API commercially). Braemar side: Marc Jarvis (sponsor), Mizanur Rahman (CTO).
- **Open question:** `ICE_HOST` in `.env` is `cm*.dataservices.theice.com`, copied from the README example. The `*` is
  almost certainly a placeholder for a server number, not a real hostname. The email gives no host — take it from the
  user guide before the first live run.

## Wire formats
- CEAPI → .NET, one JSON array per line: `["refresh"|"update", "SYMBOL", [[fieldId,"value"],...]]`
  and `["status","MSG"]`. `refresh` = full snapshot on subscribe, `update` = delta. Client may send `resync`.
- .NET → browser: `event: snapshot` / `data: {metadata:{key,type,action,serverTimestamp}, data:{symbol,fields}}`
  (camelCase, `System.Text.Json` web defaults; `type`/`action` enums serialize as **integers** — no
  `JsonStringEnumConverter`), plus `event: heartbeat` every `Sse:HeartbeatInterval` (30s).
- Metadata field IDs stripped by `IceInterpreter` live in `IceResponseMetaFieldIds`. Field 416 == "0" means Reset.
- `webapp/test-data/ceapidata.json` is 46 lines of recorded real CEAPI output — use it to fake CEAPI without ICE.

## Known problems at inheritance (unfixed unless noted)
1. ~~API will not start: `IceOptions.Section` is `"Atlas.Web.Ice"` but `appsettings.json` named the section `"Ice"`.~~
   **Fixed 2026-09-15** by renaming the JSON key. Kept here so the history makes sense.
2. `IceParser.TryParse` accepts only `"update"`, so CEAPI's initial `"refresh"` snapshot is discarded.
3. `IceMessageProcessor.Process` has no try/catch. One malformed line propagates to `IceReceiver`, which drops
   the WS connection; CEAPI then disconnects from ICE, so every bad line costs a full ICE reconnect. The four
   `*_DoesNotThrow` tests in `IceMessageProcessorTests` are `throw new NotImplementedException()` stubs with the
   intended tests commented out — **4 failures out of 116 is the baseline**, not an environment problem.
4. The frontend is not wired to this backend: it calls `/api/natgas/stream` (backend serves `/api/pricing/stream`),
   expects `{type: "FUTURES_SNAPSHOT"|"FUTURES_DELTA", data}`, and listens with `es.onmessage`, which never fires
   for named `snapshot` events. `vite.config.ts` has no `/api` proxy; only `vitest.config.ts` has one, aimed at a
   :9999 mock. Reconciling this is real work.
5. `IceSymbolListParser` lives in `Atlas.Web.Ice/Adapters` but declares `namespace Atlas.Web.Core.Domain.Logic`.
   An unknown symbol throws `new Exception("")`, which surfaces as a bare 500.
6. Nothing in .NET ever sends CEAPI the `resync` command, and nothing produces `DataStatus.Remove` outside tests.
7. **CEAPI cannot start without the ICE SDK native DLLs, which are not in the repo** (root `.gitignore` ignores
   `[Bb]in/`, and they were never committed). `Main` calls `ResourceManagerFactory.getFactory()` on startup, which
   loads `PortLib_64VC17.dll` and `dbcapi_64VC17.dll` via JNA from `-Djna.library.path` = `CEAPI/bin` (set in
   `build.gradle` and `.vscode/launch.json`). Without them it dies immediately with
   `UnsatisfiedLinkError: Unable to load library 'PortLib_64VC17'`. They ship with ICE's eSignal JStandard SDK
   1.31.1.1 (the `jstandard_SDK` folder the README mentions) — copy them into `CEAPI/bin/`. **As of 2026-09-17 the
   SDK can be downloaded again** (see "ICE access"); still not done on 2026-09-18. The SDK's JARs are already
   committed in `CEAPI/lib/` (the README wrongly says `libs/`), so only the two DLLs are needed — take the ones
   matching version 1.31.1.1, or replace the JARs and DLLs together. They need the
   VC++ 2015–2022 x64 runtime, which is installed.
8. CEAPI `WebSocketServer.onOpen` closes the previous client when a new one connects. That close probably raises
   `ClientDisconnectedEvent` and tears down ICE under the new client. Unverified.

## Docs
- `CEAPI/README.md` — accurate.
- `webapp/Documentation/` (`Atlas_Reference_v1.1.docx`, `Atlas_Architecture_v1.1.svg`, `Atlas_UML.puml`) — good
  design overview but predates the last five commits. It describes `Publish`/`Invalidate` and `Active`/`Stale`;
  the code is `Update`/`Reset`/`Remove` and `Active`/`Reset`/`Remove`. It omits `IceMessageProcessor`,
  `WebSocketClient(Factory)` and `ISymbolListParser`, and says `IceReceiver` is a stub (it is implemented).

## Conventions
- .NET: `sealed` classes, `record`s for models, ports as interfaces under `Application/Ports`, DI wired by hand in
  `Program.cs`. Tests: xUnit + FluentAssertions + Moq, named `Method_Scenario_Expectation`, one class per store verb
  (`PricingStore_Reset_Tests`, …), fakes under `Fakes/`.
- Java: JUnit 5 + Mockito; integration tests tagged `integration` and excluded from `gradlew test`.
