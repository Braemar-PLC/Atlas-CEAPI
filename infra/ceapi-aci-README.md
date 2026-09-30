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
the current 4,000+ symbol set after an ICE reconnect. It does not replace fixing ICE account, entitlement, or
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
Never check these secrets into the repository.

The complete symbol list is not stored in a GitHub secret: the current list is larger than GitHub's 48 KB secret
limit. The repository contains the non-secret fallback at `CEAPI/config/symbols.csv`; the workflow rejects deployment
if it contains fewer than 3,800 unique symbols and embeds it as `C:\app\config\symbols.csv`. `SYMBOLS_URL` remains the
live source so contract rolls are picked up without rebuilding. Deployment also allowlists the CEAPI NAT Gateway's
static public IP on the Web App without removing its existing access restrictions.

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
6. injects the ICE credentials, live symbol URL, and embedded symbol-file path as runtime environment variables

## Important

- The real ICE credentials must not be stored in GitHub source code.
- Keep them in Azure Key Vault or GitHub Actions secrets only.
- The repo notes also warn to avoid putting live secrets in chat or tracked files.
