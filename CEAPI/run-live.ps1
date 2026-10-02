# Uses SYMBOLS from the environment or CEAPI\.env; never fetches the full screen list.
# Connects to real ICE when a WebSocket client connects.
$ErrorActionPreference = "Stop"
& "$PSScriptRoot\gradlew.bat" --project-dir "$PSScriptRoot" run
if ($LASTEXITCODE -ne 0) {
    throw "CEAPI exited with code $LASTEXITCODE"
}
