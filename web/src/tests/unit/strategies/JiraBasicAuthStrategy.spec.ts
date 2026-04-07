import { describe, expect, it } from 'vitest';

import { JiraBasicAuthStrategy } from '@/strategies/JiraBasicAuthStrategy';

describe('JiraBasicAuthStrategy', () => {
  const strategy = new JiraBasicAuthStrategy();

  describe('properties', () => {
    it('should have correct id', () => {
      expect(strategy.id).toBe('JIRA_BASIC_AUTH');
    });

    it('should have correct service type', () => {
      expect(strategy.serviceType).toBe('JIRA');
    });

    it('should have correct credential type', () => {
      expect(strategy.credentialType).toBe('BASIC_AUTH');
    });

    it('should have correct display name', () => {
      expect(strategy.displayName).toBe('Jira Basic Authentication');
    });
  });

  describe('getFields', () => {
    it('should return username and password fields', () => {
      const fields = strategy.getFields();

      expect(fields).toHaveLength(2);
      expect(fields[0].key).toBe('username');
      expect(fields[0].required).toBe(true);
      expect(fields[1].key).toBe('password');
      expect(fields[1].required).toBe(true);
    });

    it('should have proper field types', () => {
      const fields = strategy.getFields();

      expect(fields[0].type).toBe('text');
      expect(fields[1].type).toBe('password');
    });
  });

  describe('buildTestPayload', () => {
    it('should build correct payload with valid credentials', () => {
      const payload = strategy.buildTestPayload({
        baseUrl: 'https://example.atlassian.net',
        connectionName: 'My Jira',
        credentialType: 'BASIC_AUTH',
        username: 'user@example.com',
        password: 'apitoken123',
      });

      expect(payload).toEqual({
        name: 'My Jira',
        baseUrl: 'https://example.atlassian.net',
        credentialType: 'BASIC_AUTH',
        credentials: {
          username: 'user@example.com',
          password: 'apitoken123',
        },
      });
    });
  });

  describe('hasCredentials', () => {
    it('should return true when username and password are provided', () => {
      expect(
        strategy.hasCredentials({
          username: 'user',
          password: 'pass',
        })
      ).toBe(true);
    });

    it('should return false when username is missing', () => {
      expect(
        strategy.hasCredentials({
          username: '',
          password: 'pass',
        })
      ).toBe(false);
    });

    it('should return false when password is missing', () => {
      expect(
        strategy.hasCredentials({
          username: 'user',
          password: '',
        })
      ).toBe(false);
    });

    it('should return false when both are missing', () => {
      expect(
        strategy.hasCredentials({
          username: '',
          password: '',
        })
      ).toBe(false);
    });
  });

  describe('validate', () => {
    it('should return no errors for valid data', () => {
      const errors = strategy.validate({
        baseUrl: 'https://example.atlassian.net',
        connectionName: 'My Jira',
        credentialType: 'BASIC_AUTH',
        username: 'user@example.com',
        password: 'apitoken123',
      });

      expect(errors).toEqual([]);
    });

    it('should return error when base URL is missing', () => {
      const errors = strategy.validate({
        baseUrl: '',
        connectionName: 'My Jira',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
      });

      expect(errors).toContain('Base URL is required');
    });

    it('should return error when connection name is missing', () => {
      const errors = strategy.validate({
        baseUrl: 'https://example.atlassian.net',
        connectionName: '',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
      });

      expect(errors).toContain('Connection name is required');
    });

    it('should return error when username is missing', () => {
      const errors = strategy.validate({
        baseUrl: 'https://example.atlassian.net',
        connectionName: 'My Jira',
        credentialType: 'BASIC_AUTH',
        username: '',
        password: 'pass',
      });

      expect(errors).toContain('Email address is required');
    });

    it('should return error when password is missing', () => {
      const errors = strategy.validate({
        baseUrl: 'https://example.atlassian.net',
        connectionName: 'My Jira',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: '',
      });

      expect(errors).toContain('API token or password is required');
    });

    it('should return multiple errors for multiple missing fields', () => {
      const errors = strategy.validate({
        baseUrl: '',
        connectionName: '',
        credentialType: 'BASIC_AUTH',
        username: '',
        password: '',
      });

      expect(errors.length).toBeGreaterThan(1);
      expect(errors).toContain('Base URL is required');
      expect(errors).toContain('Connection name is required');
      expect(errors).toContain('Email address is required');
      expect(errors).toContain('API token or password is required');
    });
  });
});
