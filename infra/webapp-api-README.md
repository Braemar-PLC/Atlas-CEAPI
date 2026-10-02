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
4. `Program.cs` processes App Service forwarded headers before HTTPS redirection, serves `wwwroot/` via
   `UseStaticFiles()`, and falls back unmatched routes to `index.html` via
   `MapFallbackToFile`, so client-side routes (e.g. `/admin`, `/natgas`) work on a full page load/refresh.
5. `Ice__Endpoint` / `Ice__Port` app settings are set to the CEAPI ACI container's private IP/port, looked up
   at deploy time with `az container show`.
6. App Service Authentication (Easy Auth) is configured for the single-tenant Entra app. Anonymous requests can
   load the SPA sign-in page; the API applies its own authenticated-user fallback policy.
7. SQLite is stored at `/home/data/atlas.db`, outside the run-from-package application directory. This is a
   single-instance persistence choice; do not scale this App Service out while SQLite and in-memory price state
   are in use. Back up the database before deployments that change its schema.

## Required Azure setup

- Resource group for the App Service (Site) itself: `BraemarAtlas-Development-RG` (same as CEAPI) — passed as
  `RESOURCE_GROUP`.
- The CEAPI ACI container group must already be deployed (see `infra/ceapi-aci-README.md`) — this workflow reads
  its private IP to configure the API's ICE endpoint. App Service VNet integration connects to the CEAPI subnet;
  `vnetRouteAllEnabled=true` preserves outbound application routing through the VNet.
- The Entra app registration must assign its admin app role to users who manage desks. The Bicep default role
  value is `Admin` (`Auth__AdminRole`); it must match the role's **value**, not just its display name, or sign-in
  can succeed while admin operations return `403`.
- The Entra app registration must include the App Service callback URI
  `https://<APPSERVICE_NAME>.azurewebsites.net/.auth/login/aad/callback` as a Web redirect URI (and the custom
  hostname callback too, if the app is opened through a custom domain).
- Under the app registration's **Authentication > Implicit grant and hybrid flows**, **ID tokens** must be enabled.
  App Service Authentication requests `code id_token`; without it sign-in fails with "response_type 'id_token' is
  not enabled for the application" and the callback returns 401.
- The app must sign its tokens with the tenant's standard keys. Do not configure SAML single sign-on (a SAML
  signing certificate) or a claims-mapping policy on its Enterprise application: Entra then signs ID tokens with an
  app-specific key that App Service Authentication does not trust, and the callback returns "400 Invalid ID Token".
- App Service Plan: this deployment **does not create its own plan**. It attaches the `atlas-api` Web App to an
  existing Linux App Service Plan — `BraemarLens-Dev-ASP` (Basic B2) in resource group
  `BraemarLens-Development-RG` — because the subscription's UK South "Total VMs" quota is fully consumed and no
  new dedicated plan can be created there. Attaching a Web App to an existing plan does not consume additional
  quota; the quota applies to the plan (the underlying VM(s)), not to each app hosted on it.
- App Service (.NET 10) — created/updated by `infra/webapp-api.bicep` in `RESOURCE_GROUP`, referencing the
  existing plan cross-resource-group via `resourceId(appServicePlanResourceGroup, 'Microsoft.Web/serverfarms', appServicePlanName)`.

## Required GitHub secrets

In addition to the secrets already used by the CEAPI workflow (`AZURE_CREDENTIALS`, `RESOURCE_GROUP`,
`CEAPI_CONTAINER_GROUP`), add:

- `APPSERVICE_NAME` — e.g. `atlas-api`
- `APPSERVICE_PLAN` — name of the **existing** App Service Plan, e.g. `BraemarLens-Dev-ASP`
- `APPSERVICE_PLAN_RESOURCE_GROUP` — resource group containing that existing plan, e.g. `BraemarLens-Development-RG`
- `ENTRA_TENANT_ID` — Microsoft Entra tenant ID for the Atlas sign-in app
- `ENTRA_CLIENT_ID` — client ID of the Entra app registration used by App Service Authentication
- `ENTRA_ID_SECRET` — client secret for that app registration

The web deployment workflow passes these GitHub Actions secrets into Bicep as secure parameters. Bicep also
sets them as App Service application settings (`ENTRA_TENANT_ID`, `ENTRA_CLIENT_ID`, and `ENTRA_ID_SECRET`);
changing a value means updating the GitHub secret and rerunning the web deployment workflow. Do not commit
the values to the repository or paste the client secret into a deployment file.

Desk membership is enforced by the API for desk lists, screen data, and price streams; admins can access all
desks. A fresh database starts with the desk catalogue but no members, so an assigned `Atlas.Admin` must add
members through the admin page before regular users can see or open any desk.

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

- CEAPI session generation numbers are local to the Java process, not globally increasing. On every relay
  connection attempt, the Web App resets its health generation baseline and clears previous relay metadata,
  while retaining the last quote timestamp. This allows health messages from a restarted container to be accepted
  without allowing older generations within the same connection to overwrite newer status. ICE terminal errors
  remain visible; resetting the health baseline does not retry an ICE session rejected with `DBCAPI_ERROR_ADDRESS_CHANGE`.
- `WEBSITE_RUN_FROM_PACKAGE=1` runs the app directly from the deployed zip (read-only, faster cold start).
- `WEBSITE_ENABLE_APP_SERVICE_STORAGE=true` and `Database__Path=/home/data/atlas.db` keep the SQLite file on
  App Service's persistent `/home` storage instead of the read-only package.
- `ForwardedHeaders` middleware is configured in `Program.cs` because App Service terminates TLS at its edge and
  forwards plain HTTP to the container; without it, `UseHttpsRedirection` would redirect-loop.
- Application Insights is provisioned automatically (`${prefix}-api-ai`) and wired via
  `APPLICATIONINSIGHTS_CONNECTION_STRING`.
- The App Service Plan (`BraemarLens-Dev-ASP`, Basic B2) supports "Always On", so the persistent ICE websocket
  connection (`IceReceiver`) stays alive without idling out, unlike the previously-attempted F1 (Free) tier.
- Since the plan is shared with other apps, keep an eye on its overall CPU/memory usage — `atlas-api` must stay
  a single instance (no scale-out), but other apps on the same plan scaling up could still affect its available
  compute.
