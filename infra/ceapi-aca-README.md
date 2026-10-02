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
- The embedded 4,056-symbol list is used deliberately for the trial. Live `SYMBOLS_URL`
  is not enabled until the Web App access restrictions and ACA egress are validated.

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
