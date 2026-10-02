param location string = resourceGroup().location
param environmentName string = 'atlas-ceapi-aca-env'
param appName string = 'atlas-ceapi-aca'
param acrName string
param containerImage string
@secure()
param iceHost string
@secure()
param iceUsername string
@secure()
param icePassword string
@description('Operator-configured comma-separated ICE subscriptions; no automatic expansion.')
@secure()
@minLength(1)
param symbols string

resource environment 'Microsoft.App/managedEnvironments@2025-01-01' existing = {
  name: environmentName
}

resource acr 'Microsoft.ContainerRegistry/registries@2023-07-01' existing = {
  name: acrName
}

resource app 'Microsoft.App/containerApps@2025-01-01' = {
  name: appName
  location: location
  properties: {
    managedEnvironmentId: environment.id
    workloadProfileName: 'Consumption'
    configuration: {
      activeRevisionsMode: 'Single'
      // "External" exposes TCP at the internal environment boundary, not the internet.
      ingress: {
        external: true
        targetPort: 9002
        exposedPort: 9002
        transport: 'tcp'
      }
      secrets: [
        {
          name: 'acr-password'
          value: acr.listCredentials().passwords[0].value
        }
        {
          name: 'ice-host'
          value: iceHost
        }
        {
          name: 'ice-username'
          value: iceUsername
        }
        {
          name: 'ice-password'
          value: icePassword
        }
        {
          name: 'ice-symbols'
          value: symbols
        }
      ]
      registries: [
        {
          server: acr.properties.loginServer
          username: acr.listCredentials().username
          passwordSecretRef: 'acr-password'
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'ceapi'
          image: containerImage
          resources: {
            cpu: 2
            memory: '4Gi'
          }
          env: [
            {
              name: 'ICE_HOST'
              secretRef: 'ice-host'
            }
            {
              name: 'ICE_USERNAME'
              secretRef: 'ice-username'
            }
            {
              name: 'ICE_PASSWORD'
              secretRef: 'ice-password'
            }
            {
              name: 'WS_PORT'
              value: '9002'
            }
            {
              name: 'SYMBOLS'
              secretRef: 'ice-symbols'
            }
          ]
          probes: [
            {
              type: 'Startup'
              tcpSocket: {
                port: 9002
              }
              periodSeconds: 5
              failureThreshold: 30
            }
            {
              type: 'Readiness'
              tcpSocket: {
                port: 9002
              }
              periodSeconds: 5
              failureThreshold: 3
            }
          ]
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 1
      }
    }
  }
}

output privateIp string = environment.properties.staticIp
output appName string = app.name
