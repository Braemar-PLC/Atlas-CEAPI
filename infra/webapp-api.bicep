// Deploys Atlas.Web.Api (the .NET API) as a single Linux App Service that also serves the built frontend
// SPA (frontend/webapp/dist, copied into wwwroot/ before publish) — one App Service, one URL, no CORS.
@description('Azure region for the deployment.')
param location string = resourceGroup().location

@description('Short prefix used in resource names.')
param prefix string = 'atlas'

@description('Name of the App Service to create or update.')
param appServiceName string = '${prefix}-api'

@description('Name of the App Service Plan to create or use.')
param appServicePlanName string = '${prefix}-api-plan'

@description('App Service Plan SKU. Single instance only — Atlas.Web.Api holds in-memory price state and an ICE websocket connection, so it must not scale out to multiple instances. Defaults to F1 (Free) because the subscription is currently at its "Total VMs" quota limit for dedicated (Basic/Standard) plans in this region; F1/D1 run on shared, multi-tenant compute and are not counted against that quota. NOTE: F1 has no Always On support and a 60 CPU-minute/day cap, so the persistent ICE websocket connection may be recycled when the app idles — move to B1+ once quota is increased.')
param appServicePlanSku string = 'F1'

@description('Hostname of the CEAPI WebSocket endpoint, e.g. the ACI FQDN from ceapi-aci.bicep output ceapiContainerGroupFqdn.')
param iceEndpoint string

@description('Port of the CEAPI WebSocket endpoint.')
param icePort int = 9002

@description('ASP.NET Core environment name.')
param aspNetCoreEnvironment string = 'Production'

var appInsightsName = '${prefix}-api-ai'

// F1/D1 (Free/Shared) plans don't support the "Always On" setting.
var supportsAlwaysOn = !(appServicePlanSku == 'F1' || appServicePlanSku == 'D1')

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: appInsightsName
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
    Flow_Type: 'Redfield'
    Request_Source: 'IbizaWebAppExtensionCreate'
  }
}

resource appServicePlan 'Microsoft.Web/serverfarms@2024-04-01' = {
  name: appServicePlanName
  location: location
  sku: {
    name: appServicePlanSku
  }
  kind: 'linux'
  properties: {
    reserved: true
  }
}

resource appService 'Microsoft.Web/sites@2024-04-01' = {
  name: appServiceName
  location: location
  kind: 'app,linux'
  properties: {
    serverFarmId: appServicePlan.id
    httpsOnly: true
    siteConfig: {
      linuxFxVersion: 'DOTNETCORE|8.0'
      alwaysOn: supportsAlwaysOn
      ftpsState: 'Disabled'
      minTlsVersion: '1.2'
      appSettings: [
        {
          name: 'ASPNETCORE_ENVIRONMENT'
          value: aspNetCoreEnvironment
        }
        {
          name: 'Ice__Endpoint'
          value: iceEndpoint
        }
        {
          name: 'Ice__Port'
          value: string(icePort)
        }
        {
          name: 'APPLICATIONINSIGHTS_CONNECTION_STRING'
          value: appInsights.properties.ConnectionString
        }
        {
          name: 'ApplicationInsightsAgent_EXTENSION_VERSION'
          value: '~3'
        }
        {
          name: 'WEBSITE_RUN_FROM_PACKAGE'
          value: '1'
        }
      ]
    }
  }
}

output appServiceName string = appService.name
output appServiceHostName string = appService.properties.defaultHostName
output appServiceUrl string = 'https://${appService.properties.defaultHostName}'
