# Private CEAPI ACA trial

The manual `CEAPI - Deploy private ACA trial` workflow deploys the validated Linux image by
immutable ACR digest. It does not rebuild the image or delete the existing ACI/NAT resources.

## Network and runtime

- Existing VNet: `BraemarSecurities-Development-VNET`.
- Dedicated subnet: `CEAPI-ACA`, proposed `10.10.15.0/27`. Preflight rejects overlaps.
- Internal workload-profile environment: `atlas-ceapi-aca-env`.
- App: `atlas-ceapi-aca`, TCP target/exposed port 9002. App ingress is external to the
  environment boundary, but that boundary has only a private IP, not a public relay endpoint.
- Web App uses `Ice__Endpoint=<environment private ingress IP>` and `Ice__Port=9002`.
  No replica IP or private DNS is required for this TCP/IP endpoint.
- One replica, 2 vCPU/4 GiB. No custom NAT Gateway is attached to the ACA subnet.
  ACA still translates outbound connections; its managed egress is not a permanent single-IP guarantee.
- Credentials are secret references. The existing registry credential is used for this
  isolated trial; it is never emitted in deployment outputs.
- `ICE_SYMBOLS` supplies the operator's comma-separated list through the `SYMBOLS` secret
  reference. No full-list fallback or automatic Web API fetching is enabled. The workflow
  reports the unique count and rejects an empty list; it does not impose a numeric cap.
- Rebuild and publish the Linux image after the environment-only rollback. Supply its
  validated digest in the manual deployment's `image_digest` input; do not reuse the old
  trial image. If ACA was deleted, the workflow permits recreation despite a stale
  `CEAPI_BACKEND=aca` setting and uses the retained ACI endpoint for failure rollback.

## Cutover and safety

The environment is created first; the app is deployed only after ACI is confirmed stopped.
TCP readiness probes do not perform WebSocket upgrades or open ICE sessions. The Web App
endpoint switch starts the actual ICE connection. A failed cutover deactivates ACA revisions
before starting ACI and restoring the previous endpoint.

`CEAPI_BACKEND=aca` selects ACA endpoint discovery in future Web App deployments and blocks
the ACI build/deploy workflow. These workflow changes must be merged to `Dev` before later
production deployments. Keep ACI stopped while ACA is selected; never restart it in parallel.
Single-revision mode and maxReplicas=1 are not a guarantee against overlap during revision
replacement. Future ACA updates require an explicit stop/deactivate-before-start procedure.

## Verification

Infrastructure success is not feed success. Verify a current relay heartbeat, `state=Live`,
advancing `lastQuoteAt`, the trial subscription count, and stability for at least ten minutes.
The trial does not bypass ICE terminal errors such as `DBCAPI_ERROR_ADDRESS_CHANGE`.
Retain ACI for rollback until sustained live verification is complete.

## Trial result (2026-10-02)

Deployment run `37015126359` completed the private cutover successfully. The Web App
was configured for `10.10.15.17:9002`, using the environment ingress IP; ACI was stopped
and retained, together with its NAT resources.

ACA subscribed to all 4,056 symbols at 13:53:01 UTC, then reported
`DBCAPI_ERROR_ADDRESS_CHANGE` at 13:53:46 UTC (approximately 45 seconds later).
The Linux image and private relay path reached ICE subscription, but the migration
did not resolve the recurring ICE session rejection. Sustained live feed verification
has therefore failed; do not remove the rollback resources or treat deployment
success as resolution of the feed issue.

The deployment workflow is manual-only after the initial trial. Backend-aware deployment
and ACI restart safeguards remain on `ceapi-linux-validation` until merged into `Dev`.
Do not run the old `Dev` ACI deployment or restart workflows while ACA is active.
