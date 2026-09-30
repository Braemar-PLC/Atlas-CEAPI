@description('Azure region for the deployment.')
param location string = resourceGroup().location

@description('Short prefix used in resource names.')
param prefix string = 'atlas'

@description('Name of the Azure Container Registry to create or use.')
param acrName string = toLower('${prefix}acr')

@description('The CEAPI container image to run in ACI, for example: myacr.azurecr.io/ceapi:latest.')
param containerImage string

@description('The port used by the Java WebSocket server in CEAPI.')
param ceapiPort int = 9002

@description('The ICE host to connect to, for example cm*.dataservices.theice.com.')
@secure()
param iceHost string

@description('ICE username used by CEAPI.')
@secure()
param iceUsername string

@description('ICE password used by CEAPI.')
@secure()
param icePassword string

@description('Comma-separated list of ICE symbols to subscribe to; only used when symbolsUrl cannot be reached.')
param symbols string = 'TFM 26J-ICN'

@description('The Web API address that lists the symbols its screens need (GET /api/screens/symbols). Empty to use symbols only.')
param symbolsUrl string = ''

@description('The ACI container group name.')
param ceapiContainerGroupName string = '${prefix}-ceapi-aci'

@description('The Key Vault name to create for storing the secret values.')
param keyVaultName string = toLower('${prefix}kv${uniqueString(resourceGroup().id)}')

var logAnalyticsWorkspaceName = '${prefix}-law'
var logAnalyticsWorkspaceSku = 'PerGB2018'
var logAnalyticsRetention = 30

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: acrName
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: true
  }
}

resource logAnalytics 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: logAnalyticsWorkspaceName
  location: location
  properties: {
    sku: {
      name: logAnalyticsWorkspaceSku
    }
    retentionInDays: logAnalyticsRetention
    features: {
      enableLogAccessUsingOnlyResourcePermissions: true
    }
  }
}

resource kv 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: location
  properties: {
    tenantId: subscription().tenantId
    enableRbacAuthorization: true
    sku: {
      family: 'A'
      name: 'standard'
    }
    enableSoftDelete: true
    enablePurgeProtection: true
    publicNetworkAccess: 'Enabled'
  }
}

resource ceapiContainerGroup 'Microsoft.ContainerInstance/containerGroups@2023-05-01' = {
  name: ceapiContainerGroupName
  location: location
  properties: {
    osType: 'Windows'
    restartPolicy: 'Always'
    sku: 'Standard'
    containers: [
      {
        name: 'ceapi'
        properties: {
          image: containerImage
          ports: [
            {
              port: ceapiPort
              protocol: 'TCP'
            }
          ]
          environmentVariables: [
            {
              name: 'ICE_HOST'
              secureValue: iceHost
            }
            {
              name: 'ICE_USERNAME'
              secureValue: iceUsername
            }
            {
              name: 'ICE_PASSWORD'
              secureValue: icePassword
            }
            {
              name: 'SYMBOLS'
              value: symbols
            }
            {
              name: 'SYMBOLS_URL'
              value: symbolsUrl
            }
            {
              name: 'WS_PORT'
              value: string(ceapiPort)
            }
          ]
          resources: {
            requests: {
              cpu: 1
              memoryInGB: 2
            }
          }
        }
      }
    ]
    diagnostics: {
      logAnalytics: {
        workspaceId: logAnalytics.properties.customerId
        workspaceKey: logAnalytics.listKeys().primarySharedKey
      }
    }
    imageRegistryCredentials: [
      {
        server: acr.properties.loginServer
        username: acr.listCredentials().username
        password: acr.listCredentials().passwords[0].value
      }
    ]
    ipAddress: {
      type: 'Public'
      dnsNameLabel: toLower('${prefix}-ceapi-${uniqueString(resourceGroup().id)}')
      ports: [
        {
          port: ceapiPort
          protocol: 'TCP'
        }
      ]
    }
  }
}

output ceapiContainerGroupFqdn string = ceapiContainerGroup.properties.ipAddress.fqdn
output ceapiContainerGroupIp string = ceapiContainerGroup.properties.ipAddress.ip
output acrLoginServer string = acr.properties.loginServer
output keyVaultName string = kv.name
