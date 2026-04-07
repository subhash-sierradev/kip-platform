import { BaseCredentialStrategy } from './BaseCredentialStrategy';

/**
 * Strategy for ArcGIS OAuth2 Authentication.
 * Handles OAuth2 client credentials flow for ArcGIS integrations.
 */
export class ArcGISOAuth2Strategy extends BaseCredentialStrategy {
  readonly id = 'ARCGIS_OAUTH2';
  readonly serviceType = 'ARCGIS';
  readonly credentialType = 'OAUTH2';
  readonly displayName = 'ArcGIS OAuth2';

  getFields() {
    return [
      {
        key: 'clientId',
        label: 'Client ID',
        type: 'text',
        required: true,
        placeholder: 'Enter OAuth2 Client ID',
        helpText: 'OAuth2 application client identifier',
      },
      {
        key: 'clientSecret',
        label: 'Client Secret',
        type: 'password',
        required: true,
        placeholder: 'Enter OAuth2 Client Secret',
        helpText: 'OAuth2 application client secret',
      },
      {
        key: 'tokenUrl',
        label: 'Token URL',
        type: 'url',
        required: true,
        placeholder: 'https://www.arcgis.com/sharing/rest/oauth2/token',
        helpText: 'OAuth2 token endpoint URL',
      },
      {
        key: 'scope',
        label: 'Scope',
        type: 'text',
        required: false,
        placeholder: 'Enter OAuth2 scope (optional)',
        helpText: 'OAuth2 permission scope (optional)',
      },
    ];
  }

  buildTestPayload(data: {
    baseUrl: string;
    connectionName: string;
    credentialType: string;
    clientId?: string;
    clientSecret?: string;
    tokenUrl?: string;
    scope?: string;
  }) {
    const credentials: {
      clientId?: string;
      clientSecret?: string;
      tokenUrl?: string;
      scope?: string;
    } = {
      clientId: data.clientId,
      clientSecret: data.clientSecret,
      tokenUrl: data.tokenUrl,
    };

    // Only include scope if it has a value
    if (data.scope?.trim()) {
      credentials.scope = data.scope;
    }

    return {
      name: data.connectionName,
      baseUrl: data.baseUrl,
      credentialType: data.credentialType,
      credentials,
    };
  }

  hasCredentials(data: { clientId?: string; clientSecret?: string; tokenUrl?: string }): boolean {
    return Boolean(data.clientId?.trim() && data.clientSecret?.trim() && data.tokenUrl?.trim());
  }

  protected validateCredentials(data: {
    clientId?: string;
    clientSecret?: string;
    tokenUrl?: string;
  }): string[] {
    const errors: string[] = [];

    if (!data.clientId?.trim()) {
      errors.push('Client ID is required');
    }

    if (!data.clientSecret?.trim()) {
      errors.push('Client Secret is required');
    }

    if (!data.tokenUrl?.trim()) {
      errors.push('Token URL is required');
    } else {
      // Validate URL format
      try {
        new URL(data.tokenUrl);
      } catch {
        errors.push('Token URL must be a valid URL');
      }
    }

    return errors;
  }
}
