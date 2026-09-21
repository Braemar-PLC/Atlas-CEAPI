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
| SYMBOLS       | No       | TFM 26J-ICN | Comma-separated symbol list    |
| WS_PORT       | No       | 9001      | WebSocket server port            |

## Setup

1. Copy all required ICE/JStandard JARs into the existing `lib/` folder next to `build.gradle`
2. Stage the native ICE SDK DLLs into `CEAPI/bin/` so `PortLib_64VC17.dll` and `dbcapi_64VC17.dll` sit directly under that folder
3. Set environment variables - create or edit `.env` file in the CEAPI root with the following:

    ICE_HOST=cm*.dataservices.theice.com
    ICE_USERNAME=Braemar_CEAPI
    ICE_PASSWORD=[Password]
    SYMBOLS="GWM 26J-ICE,TFM 26J-ICN,TFM 26K-ICN,TFM 26M-ICN,TFM 26N-ICN,TFM 26Q-ICN,TFM 26U-ICN,TFM 26V-ICN,TFM 26X-ICN,TFM 26Z-ICN,TFM 27F-ICN,TFM 27G-ICN,TFM 27H-ICN"
    WS_PORT=9002

4. Run:

```bash / powershell
./gradlew run
```

If you build the Windows container in GitHub Actions, the checkout must contain:

- `CEAPI/gradle/wrapper/gradle-wrapper.jar`
- `CEAPI/lib/ICEesig-jstandard-api-1.31.1.1.jar`
- `CEAPI/lib/ICEesig-jstandard-dbcjna-1.31.1.1.jar`
- `CEAPI/lib/jna-3.5.2.jar`
- `CEAPI/lib/platform-3.5.2.jar`
- `CEAPI/bin/PortLib_64VC17.dll`
- `CEAPI/bin/dbcapi_64VC17.dll`

The ICE SDK license must permit storing its JARs and DLLs in the GitHub repository. If it does not, provision them from an approved private artifact store during the workflow instead. The workflow and container build fail fast when an artifact is absent.

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