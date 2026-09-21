# Deploy notes — build CEAPI Windows container, push to ACR, and deploy infra

Prerequisites

- `az` CLI logged in
- `docker` (Windows) capable of building Windows containers
- Access to the ICE native DLLs (copy them into `CEAPI/bin/` before building)

Build and push CEAPI image (example)

```powershell
# from repo root
cd CEAPI
# ensure DLLs are in CEAPI\bin\
docker build -f Dockerfile.windows -t <acrName>.azurecr.io/ceapi:latest .
docker push <acrName>.azurecr.io/ceapi:latest
```

Deploy infra (ACR + ACI + App Service)

```bash
# from infra/ folder
az group create -n rg-atlas-prod -l eastus
az deployment group create -g rg-atlas-prod --template-file main.bicep --parameters prefix='atlas' location='eastus'

# After ACR created, push image (or use az acr build)
az acr login --name <acrName>
docker tag ceapi:latest <acrName>.azurecr.io/ceapi:latest
docker push <acrName>.azurecr.io/ceapi:latest

# Update the containerGroup resource to reference the image URL, then redeploy main.bicep
```

Notes

- ACI Windows containers require a Windows-capable Docker host to build the image. Consider using `az acr build` or a Windows build agent in your CI.
- Ensure ACI and App Service are in the same VNet or use VNet Integration for App Service so the API can reach the ACI private IP.
- Store ICE credentials in Key Vault and grant managed identity access to the secret.
