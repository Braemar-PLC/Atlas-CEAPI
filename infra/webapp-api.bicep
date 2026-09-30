// Deploys Atlas.Web.Api (the .NET API) as a single Linux App Service that also serves the built frontend
// SPA (frontend/webapp/dist, copied into wwwroot/ before publish) — one App Service, one URL, no CORS.
@description('Azure region for the deployment.')
param location string = resourceGroup().location

@description('Existing VNet used for private access to the CEAPI container.')
param vnetName string = 'BraemarSecurities-Development-VNET'

@description('Dedicated App Service regional VNet integration subnet.')
param appServiceIntegrationSubnetName string = 'AppService-Integration'

@description('Address prefix for the App Service integration subnet.')
param appServiceIntegrationSubnetPrefix string = '10.10.13.0/26'

@description('Short prefix used in resource names.')
param prefix string = 'atlas'

@description('Name of the App Service to create or update.')
param appServiceName string = '${prefix}-api'

@description('Name of the existing App Service Plan to deploy into. The subscription is at its UK South "Total VMs" quota limit, so this must be an existing Linux plan with spare capacity rather than a newly created one.')
param appServicePlanName string

@description('Resource group containing the existing App Service Plan (defaults to this deployment\'s resource group).')
param appServicePlanResourceGroup string = resourceGroup().name

@description('Private IP address of the CEAPI WebSocket endpoint from the ACI deployment.')
param iceEndpoint string

@description('Port of the CEAPI WebSocket endpoint.')
param icePort int = 9002

@description('ASP.NET Core environment name.')
param aspNetCoreEnvironment string = 'Production'

@description('The Entra app role value used for desk administration.')
param adminRole string = 'Admin'

@description('Microsoft Entra tenant ID used by App Service Authentication.')
@secure()
param entraTenantId string

@description('Microsoft Entra app registration client ID used by App Service Authentication.')
@secure()
param entraClientId string

@description('Client secret for the Microsoft Entra app registration. Supply through a GitHub Actions secret.')
@secure()
param entraIdSecret string

@description('Absolute path for the SQLite database on App Service persistent storage.')
param databasePath string = '/home/data/atlas.db'

var appInsightsName = '${prefix}-api-ai'

resource vnet 'Microsoft.Network/virtualNetworks@2024-05-01' existing = {
  name: vnetName
}

resource appServiceIntegrationSubnet 'Microsoft.Network/virtualNetworks/subnets@2024-05-01' = {
  parent: vnet
  name: appServiceIntegrationSubnetName
  properties: {
    addressPrefixes: [
      appServiceIntegrationSubnetPrefix
    ]
    delegations: [
      {
        name: 'appService'
        properties: {
          serviceName: 'Microsoft.Web/serverFarms'
        }
      }
    ]
  }
}

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
    virtualNetworkSubnetId: appServiceIntegrationSubnet.id
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
          name: 'Database__Path'
          value: databasePath
        }
        {
          name: 'Auth__AdminRole'
          value: adminRole
        }
        {
          name: 'WEBSITE_ENABLE_APP_SERVICE_STORAGE'
          value: 'true'
        }
        {
          name: 'ENTRA_TENANT_ID'
          value: entraTenantId
        }
        {
          name: 'ENTRA_CLIENT_ID'
          value: entraClientId
        }
        {
          name: 'ENTRA_ID_SECRET'
          value: entraIdSecret
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

resource appServiceAuthSettings 'Microsoft.Web/sites/config@2024-11-01' = {
  parent: appService
  name: 'authsettingsV2'
  properties: {
    platform: {
      enabled: true
      runtimeVersion: '~1'
    }
    globalValidation: {
      // Keep the SPA sign-in page public; the API's ASP.NET fallback policy protects API endpoints.
      requireAuthentication: false
      unauthenticatedClientAction: 'AllowAnonymous'
    }
    identityProviders: {
      azureActiveDirectory: {
        enabled: true
        registration: {
          clientId: entraClientId
          clientSecretSettingName: 'ENTRA_ID_SECRET'
          openIdIssuer: 'https://login.microsoftonline.com/${entraTenantId}/v2.0'
        }
        validation: {
          allowedAudiences: [
            entraClientId
            'api://${entraClientId}'
          ]
        }
      }
    }
    login: {
      tokenStore: {
        enabled: true
      }
    }
    httpSettings: {
      requireHttps: true
    }
  }
}

output appServiceName string = appService.name
output appServiceHostName string = appService.properties.defaultHostName
output appServiceUrl string = 'https://${appService.properties.defaultHostName}'
