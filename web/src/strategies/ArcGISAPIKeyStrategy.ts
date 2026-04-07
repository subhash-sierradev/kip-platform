import { BaseCredentialStrategy } from './BaseCredentialStrategy';

/**
 * Strategy for ArcGIS API Key Authentication.
 * Handles API key credentials for ArcGIS integrations.
 */
export class ArcGISAPIKeyStrategy extends BaseCredentialStrategy {
  readonly id = 'ARCGIS_API_KEY';
  readonly serviceType = 'ARCGIS';
  readonly credentialType = 'API_KEY';
  readonly displayName = 'ArcGIS API Key';

  getFields() {
    return [
      {
        key: 'apiKey',
        label: 'API Key',
        type: 'password',
        required: true,
        placeholder: 'Enter ArcGIS API Key',
        helpText: 'Your ArcGIS API key for authentication',
      },
    ];
  }

  buildTestPayload(data: {
    baseUrl: string;
    connectionName: string;
    credentialType: string;
    apiKey?: string;
  }) {
    return {
      name: data.connectionName,
      baseUrl: data.baseUrl,
      credentialType: data.credentialType,
      credentials: {
        apiKey: data.apiKey,
      },
    };
  }

  hasCredentials(data: { apiKey?: string }): boolean {
    return Boolean(data.apiKey?.trim());
  }

  protected validateCredentials(data: { apiKey?: string }): string[] {
    const errors: string[] = [];

    if (!data.apiKey?.trim()) {
      errors.push('API Key is required');
    }

    return errors;
  }
}
