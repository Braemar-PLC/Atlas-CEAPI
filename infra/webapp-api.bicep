// Deploys Atlas.Web.Api (the .NET API) as a single Linux App Service that also serves the built frontend
// SPA (frontend/webapp/dist, copied into wwwroot/ before publish) — one App Service, one URL, no CORS.
@description('Azure region for the deployment.')
param location string = resourceGroup().location

@description('Short prefix used in resource names.')
param prefix string = 'atlas'

@description('Name of the App Service to create or update.')
param appServiceName string = '${prefix}-api'

@description('Name of the existing App Service Plan to deploy into. The subscription is at its UK South "Total VMs" quota limit, so this must be an existing Linux plan with spare capacity rather than a newly created one.')
param appServicePlanName string

@description('Resource group containing the existing App Service Plan (defaults to this deployment\'s resource group).')
param appServicePlanResourceGroup string = resourceGroup().name

@description('Hostname of the CEAPI WebSocket endpoint, e.g. the ACI FQDN from ceapi-aci.bicep output ceapiContainerGroupFqdn.')
param iceEndpoint string

@description('Port of the CEAPI WebSocket endpoint.')
param icePort int = 9002

@description('ASP.NET Core environment name.')
param aspNetCoreEnvironment string = 'Production'

var appInsightsName = '${prefix}-api-ai'

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

// Existing Linux App Service Plan (e.g. BraemarLens-Dev-ASP) — not created here. Multiple Web Apps can share one
// plan without consuming extra "Total VMs" quota, since the quota applies to the plan, not to each app on it.
resource appServicePlan 'Microsoft.Web/serverfarms@2024-04-01' existing = {
  name: appServicePlanName
  scope: resourceGroup(appServicePlanResourceGroup)
}

resource appService 'Microsoft.Web/sites@2024-04-01' = {
  name: appServiceName
  location: location
  kind: 'app,linux'
  properties: {
    serverFarmId: appServicePlan.id
    httpsOnly: true
    siteConfig: {
      linuxFxVersion: 'DOTNETCORE|10.0'
      alwaysOn: true
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
