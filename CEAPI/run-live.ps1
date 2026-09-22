# Starts CEAPI (the ICE feed relay) subscribed to exactly the symbols the desk screens need.
#
# Why this exists: CEAPI reads its symbol list once, when it starts, from the SYMBOLS setting. That list used to
# be typed by hand into CEAPI\.env and went stale every time a contract expired. The Atlas API now works the list
# out by rule (GET /api/screens/symbols), so this script asks the API for it and hands it to CEAPI.
# A real environment variable wins over the .env file, so .env is left alone - ICE_HOST, ICE_USERNAME,
# ICE_PASSWORD and WS_PORT still come from there.
#
# THIS CONNECTS TO REAL ICE with the live credentials as soon as the API connects to CEAPI.
#
# Order:
#   1. Start the API first, with the DEFAULT profile (it points at CEAPI on port 9002):
#        cd webapp\src ; dotnet run --project Atlas.Web.Api
#      It keeps retrying until CEAPI is up, so it is fine that CEAPI is not running yet.
#   2. Then, from the CEAPI folder:   .\run-live.ps1
#
# The list includes the strips that roll in at the next expiry, so CEAPI keeps working across a roll.
# Restart it (run this script again) about once a month so it picks up the strips after that.
param(
    # Where the Atlas API is listening.
    [string]$ApiUrl = "https://localhost:7001"
)

$ErrorActionPreference = "Stop"

try {
    $symbols = Invoke-RestMethod -Uri "$ApiUrl/api/screens/symbols"
}
catch {
    Write-Host "Could not reach the Atlas API at $ApiUrl - start it first (see the top of this script)." -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit 1
}

if ([string]::IsNullOrWhiteSpace($symbols)) {
    Write-Host "The API returned no symbols, so there is nothing to subscribe to. Check the 'Screens' section of appsettings.json." -ForegroundColor Red
    exit 1
}

$count = ($symbols -split ",").Count
Write-Host "Subscribing CEAPI to $count symbols from $ApiUrl/api/screens/symbols"

$env:SYMBOLS = $symbols
& "$PSScriptRoot\gradlew.bat" run
