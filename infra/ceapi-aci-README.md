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

## Deployment flow

The GitHub Action does the following:

1. logs in to Azure
2. builds the `CEAPI/Dockerfile.windows` image in ACR
3. deploys `infra/ceapi-aci.bicep` to create or update the container group
4. injects the ICE credentials and symbol list as runtime environment variables

## Important

- The real ICE credentials must not be stored in GitHub source code.
- Keep them in Azure Key Vault or GitHub Actions secrets only.
- The repo notes also warn to avoid putting live secrets in chat or tracked files.
