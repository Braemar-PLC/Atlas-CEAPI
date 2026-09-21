# CEAPI ACI deployment

This deployment packages the CEAPI Java service into a Windows container, pushes it to Azure Container Registry, and runs it in Azure Container Instances.

## Why ACI

The CEAPI relies on native ICE DLLs loaded via JNA. Those libraries are Windows-specific and are loaded from the filesystem at runtime. ACI with a Windows container is the simplest Azure target for this service.

## Required Azure setup

Create or use these resources:

- Resource group: `BraemarAtlas-Development-RG`
- Azure Container Registry: one container registry
- Azure Container Instance container group for CEAPI

## Required GitHub secrets

Set these in the GitHub repo before the first run:

- `AZURE_CREDENTIALS`
- `RESOURCE_GROUP`
- `ACR_NAME`
- `CEAPI_CONTAINER_GROUP`
- `ICE_HOST`
- `ICE_USERNAME`
- `ICE_PASSWORD`
- `ICE_SYMBOLS`

Never check these secrets into the repository.

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
6. injects the ICE credentials and symbol list as runtime environment variables

## Important

- The real ICE credentials must not be stored in GitHub source code.
- Keep them in Azure Key Vault or GitHub Actions secrets only.
- The repo notes also warn to avoid putting live secrets in chat or tracked files.
