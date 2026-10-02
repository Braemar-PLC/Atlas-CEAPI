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
| SYMBOLS       | Yes      | —         | Operator-configured comma-separated subscriptions. Azure supplies the `ICE_SYMBOLS` GitHub secret as `SYMBOLS`; blanks and duplicates are removed |
| WS_PORT       | No       | 9001      | WebSocket server port            |

CEAPI publishes feed health every five seconds. The Web API and browser use the generation and timestamp to
reject old status events and mark displayed prices as non-current if the relay becomes silent.

Subscriptions are environment-variable-only. Legacy `SYMBOLS_FILE` and `SYMBOLS_URL` values
are ignored, and the images no longer embed the full 4,056-symbol list. `run-live.ps1` uses
the configured `SYMBOLS` instead of querying the Web API. Update the operator list and restart
CEAPI when contracts roll; the Web API's screen-symbol endpoint does not expand relay subscriptions.
There is no arbitrary 203-symbol cap: the actual count is determined by `ICE_SYMBOLS`.

If ICE drops an established session, the JStandard SDK's default automatic recovery owns the reconnect and CEAPI
resubscribes when that same session reconnects. CEAPI does not create a competing `QuoteManager` while the SDK is
recovering. For the connection-lifecycle diagnostic rollback, application-managed startup retry timers
have also been removed: a startup failure is logged, published as disconnected, and propagated.
Another explicit client connection is needed to initiate a new session.
Disconnect status codes are included in the feed detail and container logs. Statuses that ICE marks
as non-reconnectable, including address changes, entitlement failures, required upgrades, and invalid credentials,
are not overridden by CEAPI. Credential rejection is published as `AUTHENTICATION_FAILED` rather than leaving the
last prices looking live.

The rollback restores the pre-September-30 connect/teardown sequence while retaining health heartbeats,
generation checks for stale callbacks, and multi-client protection. The host, credentials and
`SOCKTYPE_LEGACY` setting are unchanged from the earlier version. It does not disable the vendor
SDK's own recovery or claim to resolve `DBCAPI_ERROR_ADDRESS_CHANGE`.

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

### Linux image validation for Azure Container Apps

Run the manual **CEAPI - Validate Linux image** GitHub workflow on `Dev`. It runs Java tests,
builds `Dockerfile.linux` for Linux amd64, checks native shared-library dependencies, and loads
the actual ICE native API and creates a QuoteManager with container networking disabled.
It also verifies that the relay starts on port 9002. It does not use real ICE credentials
or connect to ICE. By default it does not publish an image or change Azure resources.
To publish, manually run the workflow with `publish_image=true`. After validation succeeds,
it pushes `ceapi-linux:<commit SHA>` to the existing ACR, verifies the manifest digest, and
repeats the native SDK smoke test against the published image. The immutable image reference
is recorded in the workflow summary. Publishing never deploys or restarts ACI or ACA.

The Linux image uses `/app/native` for both JNA loading and native dependency resolution,
and requires `SYMBOLS` at runtime. It runs as a non-root user.
Passing this workflow establishes offline runtime compatibility, not ICE login or sustained
market-data delivery. Those require a separately approved live trial.

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