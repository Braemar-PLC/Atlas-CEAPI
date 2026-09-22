# Atlas web app + frontend deployment (single App Service)

This deployment publishes `Atlas.Web.Api` (the .NET API) to a single Linux App Service that also serves the
built frontend SPA (`frontend/webapp`). The browser and the API share one origin/hostname, so no CORS or
reverse-proxy configuration is required.

## Why one App Service

`Atlas.Web.Api` keeps pricing state in memory (`PricingStore`) and holds a single outbound WebSocket connection
to CEAPI (`IceReceiver`). Combining the SPA into the same app avoids standing up a second resource (Static Web
App / separate Web App) purely to serve static files, at the cost of coupling the frontend and API release
cadence together. The App Service Plan must stay at a single instance — do not enable autoscale/scale-out,
since a second instance would hold its own independent in-memory state and its own ICE connection.

## How it works

1. GitHub Actions builds the frontend (`npm ci && npm run build` in `frontend/webapp`, output `dist/`).
2. The build output is copied into `webapp/src/Atlas.Web.Api/wwwroot/` (git-ignored; regenerated on every run).
3. `Atlas.Web.Api` is published (`dotnet publish`) and zip-deployed to the App Service.
4. `Program.cs` serves `wwwroot/` via `UseStaticFiles()` and falls back unmatched routes to `index.html` via
   `MapFallbackToFile`, so client-side routes (e.g. `/admin`, `/natgas`) work on a full page load/refresh.
5. `Ice__Endpoint` / `Ice__Port` app settings are set to the CEAPI ACI container's public FQDN/port, looked up
   at deploy time with `az container show`.

## Required Azure setup

- Resource group: `BraemarAtlas-Development-RG` (same as CEAPI)
- The CEAPI ACI container group must already be deployed (see `infra/ceapi-aci-README.md`) — this workflow reads
  its FQDN to configure the API's ICE endpoint.
- App Service Plan (Linux) + App Service (.NET 8) — created/updated by `infra/webapp-api.bicep`.

## Required GitHub secrets

In addition to the secrets already used by the CEAPI workflow (`AZURE_CREDENTIALS`, `RESOURCE_GROUP`,
`CEAPI_CONTAINER_GROUP`), add:

- `APPSERVICE_NAME` — e.g. `atlas-api`
- `APPSERVICE_PLAN` — e.g. `atlas-api-plan`

## Local build check

```powershell
# Frontend
cd frontend/webapp
npm ci
npm run build

# Copy into wwwroot, then run the API to serve both
Remove-Item -Recurse -Force ..\..\webapp\src\Atlas.Web.Api\wwwroot -ErrorAction SilentlyContinue
Copy-Item -Recurse dist ..\..\webapp\src\Atlas.Web.Api\wwwroot
cd ..\..\webapp\src\Atlas.Web.Api
dotnet run
```

Then browse to the API's URL (e.g. `https://localhost:7001`) — it should serve the SPA at `/` and the API under
`/api/*`.

## Notes

- `WEBSITE_RUN_FROM_PACKAGE=1` runs the app directly from the deployed zip (read-only, faster cold start).
- `ForwardedHeaders` middleware is configured in `Program.cs` because App Service terminates TLS at its edge and
  forwards plain HTTP to the container; without it, `UseHttpsRedirection` would redirect-loop.
- Application Insights is provisioned automatically (`${prefix}-api-ai`) and wired via
  `APPLICATIONINSIGHTS_CONNECTION_STRING`.

## App Service Plan SKU: temporarily F1 (Free)

`appServicePlanSku` defaults to `F1` because the subscription is currently at its "Total VMs" quota limit for
dedicated (Basic/Standard/Premium) App Service Plans in this region. F1/D1 run on shared, multi-tenant compute
and are not counted against that quota, so they deploy without needing a quota increase.

**Trade-offs to be aware of on F1:**
- No "Always On" support — the app can idle/unload after ~20 minutes with no requests, which will drop the
  persistent ICE websocket connection (`IceReceiver`) until the next request wakes the app back up.
- Capped at 60 CPU-minutes/day; a long-lived websocket connection processing continuous price ticks can exhaust
  this quickly.
- 1 GB storage, no custom domain SSL binding, no scaling.

**Once the UK South "Total VMs" quota is increased (or an existing dedicated plan is freed up)**, redeploy with
`appServicePlanSku` set to `B1` or higher (e.g. pass `-p appServicePlanSku=B1` to the Bicep deployment, or add an
`APPSERVICE_SKU` GitHub secret/workflow input) to restore Always On and remove the CPU cap.
