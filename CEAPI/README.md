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
Status:   ["status","MESSAGE"]
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
| SYMBOLS       | Yes, unless SYMBOLS_URL is set | — | Comma-separated symbol list; the fallback when SYMBOLS_URL cannot be reached |
| SYMBOLS_URL   | No       | —         | Web API address listing the symbols the screens need (`/api/screens/symbols`). Fetched on every ICE connect and re-checked hourly, so the list follows contract rolls and option chains without a restart |
| WS_PORT       | No       | 9001      | WebSocket server port            |

If ICE drops the session, CEAPI reopens it by itself (backing off 5s, 10s, 30s, 60s, then every 120s) for as long as the Web API is connected.

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