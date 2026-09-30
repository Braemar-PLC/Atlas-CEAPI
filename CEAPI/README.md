# CEAPI Runner

Connects to ICE via CEAPI (JStandard SDK) and streams market data over WebSocket.
Pure relay — no field interpretation, no state, no business logic.

## Architecture

```
ICE Feed --> CEAPI callbacks --> IceQuoteListener --> WebSocketServer --> Web API
```

Web API sends `resync` command → CEAPI resubscribes all symbols → fresh REFRESH sent.

## Message Format

All messages are raw arrays (text WebSocket frames):

```
Refresh:  ["refresh","SYMBOL",[[fieldId,"value"],[fieldId,"value"],...]]
Update:   ["update","SYMBOL",[[fieldId,"value"],...]]
Status:   ["status",{"state":"LIVE","generation":3,"timestamp":"2026-09-30T13:00:00Z","detail":"Connected to ICE","subscribedSymbols":3866}]
```

Commands from Web API:
```
resync
```

## Environment Variables

| Variable      | Required | Default   | Description                      |
|---------------|----------|-----------|----------------------------------|
| ICE_HOST      | Yes      | —         | ICE host e.g. cm.dataservices.theice.com |
| ICE_USERNAME  | Yes      | —         | ICE username                     |
| ICE_PASSWORD  | Yes      | —         | ICE password                     |
| SYMBOLS       | Yes, unless SYMBOLS_URL is set | — | Comma-separated fallback list for local runs. The Azure deployment uses `SYMBOLS_FILE` instead because the complete list exceeds GitHub's secret-size limit |
| SYMBOLS_FILE  | No       | —         | Path to a comma-separated fallback list. Takes precedence over `SYMBOLS`; Azure embeds the validated current list at `C:\app\config\symbols.csv` |
| SYMBOLS_URL   | No       | —         | Web API address listing the symbols the screens need (`/api/screens/symbols`). Fetched on every ICE connect and re-checked hourly, so the list follows contract rolls and option chains without a restart |
| WS_PORT       | No       | 9001      | WebSocket server port            |

CEAPI publishes feed health every five seconds. The Web API and browser use the generation and timestamp to
reject old status events and mark displayed prices as non-current if the relay becomes silent.

If ICE drops an established session, the JStandard SDK's default automatic recovery owns the reconnect and CEAPI
resubscribes when that same session reconnects. CEAPI does not create a competing `QuoteManager` while the SDK is
recovering. The 1s, 5s, 10s, 30s, 60s, then 120s backoff is reserved for failures that occur before the SDK starts
a connection. Disconnect status codes are included in the feed detail and container logs. Statuses that ICE marks
as non-reconnectable, including address changes, entitlement failures, required upgrades, and invalid credentials,
are not overridden by CEAPI. Credential rejection is published as `AUTHENTICATION_FAILED` rather than leaving the
last prices looking live.

## Setup

1. Copy all JARs from `jstandard_SDK/lib` into a `libs/` folder next to `build.gradle`
2. Set environment variables - create or edit .env file in root with the following:

    ICE_HOST=cm*.dataservices.theice.com
    ICE_USERNAME=Braemar_CEAPI
    ICE_PASSWORD=[Password]
    SYMBOLS="GWM 26J-ICE,TFM 26J-ICN,TFM 26K-ICN,TFM 26M-ICN,TFM 26N-ICN,TFM 26Q-ICN,TFM 26U-ICN,TFM 26V-ICN,TFM 26X-ICN,TFM 26Z-ICN,TFM 27F-ICN,TFM 27G-ICN,TFM 27H-ICN"
    WS_PORT=9002

3. Run:

```bash / powershell
./gradlew run
```

## Tests

```bash / powershell
./gradlew test
```

Tests cover:
- `MessageBuilder` — message format correctness
- `CommandParser` — command parsing
- `WebSocketServer` — integration test with real socket client
- `Settings` — env var validation
## Integration Tests

```bash / powershell
./gradlew integrationTest --rerun-tasks    
```