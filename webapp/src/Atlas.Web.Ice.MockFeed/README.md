# Atlas.Web.Ice.MockFeed

A standalone WebSocket server that stands in for the Java CEAPI relay
(`CEAPI/`), so the .NET API and its consumers can run against a realistic ICE
feed without credentials, without the eSignal native libraries, and without a
JVM.

The mock is indistinguishable from the real relay at the WebSocket boundary:
same wire format, same connect/subscribe sequence, same commands. Consumers
require no feature flag or code change to switch between them — only the
configured port differs.

## Why this exists

- ICE credentials are not yet issued.
- The real relay cannot run on a clean checkout either way: `CEAPI/build.gradle`
  points `jna.library.path` at `CEAPI/bin`, which is empty. The eSignal native
  libraries JNA needs are absent and undocumented.

## What the real relay does

Established from `CEAPI/src/main/java/com/braemar/ceapi/`, reproduced here
rather than assumed:

- Wire format is newline-delimited JSON arrays:
  `["refresh","SYMBOL",[[id,"value"],...]]` and `["update","SYMBOL",[[id,"value"],...]]`
  (`CEAPI/README.md`). `Atlas.Web.Ice/Adapters/IceReceiver.cs` splits the socket
  stream on `\n` and processes one frame per line.
- ICE is connected **on WebSocket client connect**, not at relay startup, and
  subscribes using the relay's own `SYMBOLS` env var — not a client request
  (`IceWebSocketBridge.java`). The .NET side sends nothing; this is why
  `IWebSocketClient` has no `SendAsync` — by design, not a gap.
- On subscribe, ICE delivers a full snapshot per symbol (`onResponse` →
  `refresh`), then deltas (`onUpdate` → `update`).
- ICE disconnects when the client disconnects.
- The one inbound command is `resync`, forcing a fresh unsubscribe/resubscribe
  snapshot.
- The server is single-client: a new connection displaces the previous one.
- Field `416` set to `"0"` signals a record reset, mapped by
  `Atlas.Web.Ice/Adapters/IceInterpreter.cs` to `DataStatus.Reset`.

## What this mock does

1. Accepts one WebSocket client at a time; a new connection displaces the
   previous one, matching the relay.
2. On connect, sends one `refresh` per symbol, built as the union of every
   field seen for that symbol across the capture (a capture recorded mid-session
   contains updates only — see below).
3. Then streams `update` frames: replays the capture verbatim in order, then
   — once exhausted — keeps going by walking each symbol forward, moving price
   fields only. Movement is bounded to fields that are **not** one of the 11
   metadata IDs in `Atlas.Web.Ice/Domain/Enumeration/IceResponseMetaFieldIds.cs`
   (the same set `IceInterpreter` strips) **and** whose captured value parses
   as a decimal containing a decimal point. This also moves volume-like fields
   (e.g. `266`), which is realistic feed behaviour.
4. On inbound `resync`, re-sends the refresh set.
5. Supports `reset` / `reset SYMBOL` — not a relay command, added so the
   `DataStatus.Reset` path can be exercised, since nothing in the live pipeline
   currently reaches it.
6. Serializes all writes to the socket (tick loop and command replies share a
   lock) and completes the close handshake on client-initiated close.

### Source data

`Atlas.Web.Ice.Tests/TestData/ceapidata.json` — 47 captured `update` lines from
a real relay session, real symbols, real field IDs, real prices. Linked into
this project's build output at `Capture/ceapidata.json`. It contains no
`refresh` lines, which is why the opening snapshot is reconstructed rather than
replayed directly.

## Running

```
dotnet run --project Atlas.Web.Ice.MockFeed
```

| Parameter | Default | Purpose |
|---|---|---|
| `--port` | `9102` | Listen port. Set to `9002` to impersonate the real relay in place. |
| `--interval` | `250ms` | Delay between emitted update frames. |
| `--capture` | bundled `ceapidata.json` | Path to an alternative capture file. |
| `--loop` | `false` | Replay the capture verbatim on exhaustion instead of generating ticks. |
| `--jitter` | `0.15` | Price movement per synthetic tick, as a percentage. `0` disables movement. |

## Mock vs real relay

Ports are kept distinct so both can run at once and the active feed is
explicit rather than implied by whatever happens to be listening:

- Real relay: `ws://localhost:9002` (`Atlas.Web.Api/appsettings.json`, unchanged)
- Mock: `ws://localhost:9102` (default; override with `--port`)

Select via launch profile — `Atlas.Web.Api/Properties/launchSettings.json` has
an `Atlas.Web.Api (mock feed)` profile that sets `Ice__Port=9102`:

```
dotnet run --project Atlas.Web.Api --launch-profile "Atlas.Web.Api (mock feed)"
```

The default `Atlas.Web.Api` profile is unchanged and still points at 9002.

No code in `Atlas.Web.Api` or `Atlas.Web.Ice` branches on which feed is live —
`Ice:Port` is the only thing that changes.

## Verified end-to-end (2026-09-17)

Mock started on 9102; API started under the mock profile; `GET
/api/pricing/stream?symbol=TFM 26J-ICN` returned real `event: snapshot` frames
with genuine ICE field data and moving prices. Killing the mock mid-stream and
restarting it was recovered automatically by the API's existing
`IceReceiver` reconnect/backoff, with no code change needed.
