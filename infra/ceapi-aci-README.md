# CEAPI ACI deployment

This deployment packages the CEAPI Java service into a Windows container, pushes it to Azure Container Registry, and runs it in Azure Container Instances.

## Why ACI

The CEAPI relies on native ICE DLLs loaded via JNA. Those libraries are Windows-specific and are loaded from the filesystem at runtime. ACI with a Windows container is the simplest Azure target for this service.

## Required Azure setup

Create or use these resources:

- Resource group: `BraemarAtlas-Development-RG`
- Azure Container Registry: one container registry
- Azure Container Instance container group for CEAPI
- VNet: `BraemarSecurities-Development-VNET`
- Dedicated `CEAPI` subnet delegated to `Microsoft.ContainerInstance/containerGroups`
- NAT Gateway with a static public IP attached to the CEAPI subnet

The CEAPI subnet is configured with `defaultOutboundAccess: false`. ACI therefore has no implicit internet egress:
outbound ICE connections use the NAT Gateway's stable public IP, while port 9002 remains private inside the VNet.

The CEAPI container requests 2 vCPU and 4 GB memory. This gives the Java relay headroom to resubscribe and process
the operator-configured symbol set after an ICE reconnect. It does not replace fixing ICE account, entitlement, or
source-IP disconnects; those are reported separately through feed health and container diagnostics.

## Required GitHub secrets

Set these in the GitHub repo before the first run:

- `AZURE_CREDENTIALS`
- `RESOURCE_GROUP`
- `ACR_NAME`
- `CEAPI_CONTAINER_GROUP`
- `ICE_HOST`
- `ICE_USERNAME`
- `ICE_PASSWORD`
- `ICE_SYMBOLS` (the intended small comma-separated subscription list)
Never check these secrets into the repository.

The `ICE_SYMBOLS` secret is supplied as the `SYMBOLS` runtime environment variable.
The workflow logs the unique count, never the symbol values, and rejects an empty list.
There is no 3,800-symbol minimum, embedded full-list fallback, or automatic Web API fetching.
Maintain the operator list as contracts roll. No new Web App access rule is needed for symbol fetching.

## Local build check

Before pushing to Azure, build locally if Docker is available:

```powershell
cd CEAPI
# Ensure the ICE native DLLs are present in CEAPI\bin\
# Example: PortLib_64VC17.dll and dbcapi_64VC17.dll

docker build -f Dockerfile.windows -t ceapi:local .
```

The GitHub workflow also requires these files to be present in the repo checkout before it builds:

- `CEAPI/gradle/wrapper/gradle-wrapper.jar`
- `CEAPI/lib/ICEesig-jstandard-api-1.31.1.1.jar`
- `CEAPI/lib/ICEesig-jstandard-dbcjna-1.31.1.1.jar`
- `CEAPI/lib/jna-3.5.2.jar`
- `CEAPI/lib/platform-3.5.2.jar`
- `CEAPI/bin/PortLib_64VC17.dll`
- `CEAPI/bin/dbcapi_64VC17.dll`

Only store ICE SDK binaries in GitHub if the ICE SDK license permits it. Otherwise, download them from an approved private artifact store before the validation step.

## Deployment flow

The GitHub Action does the following:

1. logs in to Azure
2. validates that the Gradle wrapper, ICE Java SDK JARs, and native DLLs are present in the CEAPI build context
3. builds the CEAPI `installDist` distribution on the Windows GitHub runner
4. uses ACR Tasks with the Windows platform to package the prebuilt distribution and push it to ACR
5. deploys `infra/ceapi-aci.bicep` to create or update the container group
6. injects the ICE credentials and operator-configured `SYMBOLS` as runtime environment variables

To return from a deleted ACA relay, manually dispatch with `rollback_from_aca=true`.
The workflow verifies the ACA app is absent before activating ACI, and sets both the
private endpoint/port and `CEAPI_BACKEND=aci` on the Web App. Normal deploys remain blocked
when ACA is selected. ACI and ACA cutovers share a concurrency group.

## Important

- The real ICE credentials must not be stored in GitHub source code.
- Keep them in Azure Key Vault or GitHub Actions secrets only.
- The repo notes also warn to avoid putting live secrets in chat or tracked files.
