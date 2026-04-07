import { BaseCredentialStrategy } from './BaseCredentialStrategy';

/**
 * Strategy for Jira Basic Authentication.
 * Handles username/password credentials for Jira integrations.
 */
export class JiraBasicAuthStrategy extends BaseCredentialStrategy {
  readonly id = 'JIRA_BASIC_AUTH';
  readonly serviceType = 'JIRA';
  readonly credentialType = 'BASIC_AUTH';
  readonly displayName = 'Jira Basic Authentication';

  getFields() {
    return [
      {
        key: 'username',
        label: 'Email Address',
        type: 'text',
        required: true,
        placeholder: 'your-email@example.com',
        helpText: 'Your Jira account email address (required for API token authentication)',
      },
      {
        key: 'password',
        label: 'API Token or Password',
        type: 'password',
        required: true,
        placeholder: 'Enter API token (recommended) or password',
        helpText: 'API token (recommended) - Generate from Atlassian account security settings',
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
      errors.push('Email address is required');
    }

    if (!data.password?.trim()) {
      errors.push('API token or password is required');
    }

    return errors;
  }
}
