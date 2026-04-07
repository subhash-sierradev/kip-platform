import { BaseCredentialStrategy } from './BaseCredentialStrategy';

/**
 * Strategy for ArcGIS Basic Authentication.
 * Handles username/password credentials for ArcGIS integrations.
 */
export class ArcGISBasicAuthStrategy extends BaseCredentialStrategy {
  readonly id = 'ARCGIS_BASIC_AUTH';
  readonly serviceType = 'ARCGIS';
  readonly credentialType = 'BASIC_AUTH';
  readonly displayName = 'ArcGIS Basic Authentication';

  getFields() {
    return [
      {
        key: 'username',
        label: 'Username',
        type: 'text',
        required: true,
        placeholder: 'Enter ArcGIS username',
        helpText: 'Your ArcGIS account username',
      },
      {
        key: 'password',
        label: 'Password',
        type: 'password',
        required: true,
        placeholder: 'Enter ArcGIS password',
        helpText: 'Your ArcGIS account password',
      },
    ];
  }

  buildTestPayload(data: {
    baseUrl: string;
    connectionName: string;
    credentialType: string;
    username?: string;
    password?: string;
  }) {
    return {
      name: data.connectionName,
      baseUrl: data.baseUrl,
      credentialType: data.credentialType,
      credentials: {
        username: data.username,
        password: data.password,
      },
    };
  }

  hasCredentials(data: { username?: string; password?: string }): boolean {
    return Boolean(data.username?.trim() && data.password?.trim());
  }

  protected validateCredentials(data: { username?: string; password?: string }): string[] {
    const errors: string[] = [];

    if (!data.username?.trim()) {
      errors.push('Username is required');
    }

    if (!data.password?.trim()) {
      errors.push('Password is required');
    }

    return errors;
  }
}
