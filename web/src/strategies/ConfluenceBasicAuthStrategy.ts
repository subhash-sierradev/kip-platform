import { BaseCredentialStrategy } from './BaseCredentialStrategy';

export class ConfluenceBasicAuthStrategy extends BaseCredentialStrategy {
  readonly id = 'CONFLUENCE_BASIC_AUTH';
  readonly serviceType = 'CONFLUENCE';
  readonly credentialType = 'BASIC_AUTH';
  readonly displayName = 'Confluence Basic Authentication';

  getFields() {
    return [
      {
        key: 'username',
        label: 'Email Address',
        type: 'text',
        required: true,
        placeholder: 'your-email@example.com',
        helpText: 'Your Atlassian account email address',
      },
      {
        key: 'password',
        label: 'API Token',
        type: 'password',
        required: true,
        placeholder: 'Enter your Atlassian API token',
        helpText: 'Generate from id.atlassian.com → Security → API tokens',
      },
    ];
  }

  buildTestPayload(data: {
    baseUrl: string;
    connectionName?: string;
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
      errors.push('Email address is required');
    }
    if (!data.password?.trim()) {
      errors.push('API token is required');
    }
    return errors;
  }
}
