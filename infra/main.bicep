// Minimal Bicep: ACR, ACI (Windows), App Service Plan + Web App (Linux), Key Vault, App Insights
// NOTE: This is a starting point. Adjust SKUs, locations and names before deploying.

param location string = resourceGroup().location
param prefix string = 'atlas'
param containerImage string = '' // e.g. '<acrLoginServer>/ceapi:tag' - pass after building the image

var acrName = toLower('${prefix}acr')
var aciName = '${prefix}-ceapi-aci'
var webAppName = '${prefix}-api'
var planName = '${prefix}-plan'
var kvName = toLower('${prefix}kv')
var appInsightsName = '${prefix}-ai'

resource acr 'Microsoft.ContainerRegistry/registries@2022-02-01' = {
  name: acrName
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {}
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: appInsightsName
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
  }
}

resource kv 'Microsoft.KeyVault/vaults@2022-07-01' = {
  name: kvName
  location: location
  properties: {
    tenantId: subscription().tenantId
    sku: {
      family: 'A'
      name: 'standard'
    }
    accessPolicies: []
    enabledForDeployment: true
    enabledForTemplateDeployment: true
  }
}

resource plan 'Microsoft.Web/serverfarms@2021-02-01' = {
  name: planName
  location: location
  sku: {
    name: 'P1v2'
    tier: 'PremiumV2'
  }
  properties: {
    reserved: true // Linux
  }
}

resource webapp 'Microsoft.Web/sites@2021-02-01' = {
  name: webAppName
  location: location
  properties: {
    serverFarmId: plan.id
    siteConfig: {
      linuxFxVersion: 'DOTNETCORE|8.0'
      appSettings: [
        {
          name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
          value: appInsights.properties.ConnectionString
        }
      ]
    }
  }
}

// Container Group (ACI) using Windows container for CEAPI
resource containerGroup 'Microsoft.ContainerInstance/containerGroups@2021-03-01' = {
  name: aciName
  location: location
  properties: {
    osType: 'Windows'
    containers: [
      {
        name: 'ceapi'
        properties: {
          image: containerImage
          resources: {
            requests: {
              cpu: 1.0
              memoryInGb: 2.0
            }
          }
          ports: [
            {
              port: 9002
            }
          ]
        }
      }
    ]
    ipAddress: {
      type: 'Private'
      ports: [
        {
          port: 9002
          protocol: 'TCP'
        }
      ]
    }
  }
}

output acrLoginServer string = acr.properties.loginServer
output webAppUrl string = webapp.properties.defaultHostName
