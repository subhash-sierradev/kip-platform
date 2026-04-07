import { describe, expect, it } from 'vitest';

import { ConfluenceBasicAuthStrategy } from '@/strategies/ConfluenceBasicAuthStrategy';

describe('ConfluenceBasicAuthStrategy', () => {
  const strategy = new ConfluenceBasicAuthStrategy();

  it('has the expected static metadata', () => {
    expect(strategy.id).toBe('CONFLUENCE_BASIC_AUTH');
    expect(strategy.serviceType).toBe('CONFLUENCE');
    expect(strategy.credentialType).toBe('BASIC_AUTH');
    expect(strategy.displayName).toBe('Confluence Basic Authentication');
  });

  it('returns username and password fields for basic auth', () => {
    const fields = strategy.getFields();

    expect(fields).toHaveLength(2);
    expect(fields[0]).toMatchObject({
      key: 'username',
      label: 'Email Address',
      type: 'text',
      required: true,
    });
    expect(fields[1]).toMatchObject({
      key: 'password',
      label: 'API Token',
      type: 'password',
      required: true,
    });
  });

  it('builds payload using provided credentials', () => {
    const payload = strategy.buildTestPayload({
      baseUrl: 'https://example.atlassian.net/wiki',
      connectionName: 'Confluence Connection',
      credentialType: 'BASIC_AUTH',
      username: 'user@example.com',
      password: 'token-value',
    });

    expect(payload).toEqual({
      name: 'Confluence Connection',
      baseUrl: 'https://example.atlassian.net/wiki',
      credentialType: 'BASIC_AUTH',
      credentials: {
        username: 'user@example.com',
        password: 'token-value',
      },
    });
  });

  it('checks credential presence using trimmed values', () => {
    expect(strategy.hasCredentials({ username: ' user ', password: ' token ' })).toBe(true);
    expect(strategy.hasCredentials({ username: ' ', password: 'token' })).toBe(false);
    expect(strategy.hasCredentials({ username: 'user', password: '' })).toBe(false);
  });

  it('returns no validation errors for complete payload', () => {
    const errors = strategy.validate({
      baseUrl: 'https://example.atlassian.net/wiki',
      connectionName: 'Confluence Connection',
      credentialType: 'BASIC_AUTH',
      username: 'user@example.com',
      password: 'token-value',
    });

    expect(errors).toEqual([]);
  });

  it('returns required field errors for missing values', () => {
    const errors = strategy.validate({
      baseUrl: '',
      connectionName: '',
      credentialType: '',
      username: '',
      password: '',
    });

    expect(errors).toContain('Base URL is required');
    expect(errors).toContain('Connection name is required');
    expect(errors).toContain('Credential type is required');
    expect(errors).toContain('Email address is required');
    expect(errors).toContain('API token is required');
  });
});
