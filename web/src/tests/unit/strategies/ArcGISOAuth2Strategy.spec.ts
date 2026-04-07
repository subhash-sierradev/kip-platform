import { describe, expect, it } from 'vitest';

import { ArcGISOAuth2Strategy } from '@/strategies/ArcGISOAuth2Strategy';

describe('ArcGISOAuth2Strategy', () => {
  const strategy = new ArcGISOAuth2Strategy();

  describe('properties', () => {
    it('should have correct id', () => {
      expect(strategy.id).toBe('ARCGIS_OAUTH2');
    });

    it('should have correct service type', () => {
      expect(strategy.serviceType).toBe('ARCGIS');
    });

    it('should have correct credential type', () => {
      expect(strategy.credentialType).toBe('OAUTH2');
    });

    it('should have correct display name', () => {
      expect(strategy.displayName).toBe('ArcGIS OAuth2');
    });
  });

  describe('getFields', () => {
    it('should return 4 OAuth2 fields', () => {
      const fields = strategy.getFields();

      expect(fields).toHaveLength(4);
      expect(fields.map(f => f.key)).toEqual(['clientId', 'clientSecret', 'tokenUrl', 'scope']);
    });

    it('should have proper field requirements', () => {
      const fields = strategy.getFields();

      expect(fields[0].required).toBe(true); // clientId
      expect(fields[1].required).toBe(true); // clientSecret
      expect(fields[2].required).toBe(true); // tokenUrl
      expect(fields[3].required).toBe(false); // scope
    });

    it('should have proper field types', () => {
      const fields = strategy.getFields();

      expect(fields[0].type).toBe('text'); // clientId
      expect(fields[1].type).toBe('password'); // clientSecret
      expect(fields[2].type).toBe('url'); // tokenUrl
      expect(fields[3].type).toBe('text'); // scope
    });
  });

  describe('buildTestPayload', () => {
    it('should build correct payload with all fields', () => {
      const payload = strategy.buildTestPayload({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My OAuth2 ArcGIS',
        credentialType: 'OAUTH2',
        clientId: 'client123',
        clientSecret: 'secret456',
        tokenUrl: 'https://www.arcgis.com/sharing/rest/oauth2/token',
        scope: 'openid profile',
      });

      expect(payload).toEqual({
        name: 'My OAuth2 ArcGIS',
        baseUrl: 'https://services.arcgis.com/xyz',
        credentialType: 'OAUTH2',
        credentials: {
          clientId: 'client123',
          clientSecret: 'secret456',
          tokenUrl: 'https://www.arcgis.com/sharing/rest/oauth2/token',
          scope: 'openid profile',
        },
      });
    });

    it('should build correct payload without optional scope', () => {
      const payload = strategy.buildTestPayload({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My OAuth2 ArcGIS',
        credentialType: 'OAUTH2',
        clientId: 'client123',
        clientSecret: 'secret456',
        tokenUrl: 'https://www.arcgis.com/sharing/rest/oauth2/token',
      });

      expect(payload).toEqual({
        name: 'My OAuth2 ArcGIS',
        baseUrl: 'https://services.arcgis.com/xyz',
        credentialType: 'OAUTH2',
        credentials: {
          clientId: 'client123',
          clientSecret: 'secret456',
          tokenUrl: 'https://www.arcgis.com/sharing/rest/oauth2/token',
          scope: undefined,
        },
      });
    });
  });

  describe('hasCredentials', () => {
    it('should return true when all required fields are provided', () => {
      expect(
        strategy.hasCredentials({
          clientId: 'client123',
          clientSecret: 'secret456',
          tokenUrl: 'https://example.com/token',
        })
      ).toBe(true);
    });

    it('should return false when clientId is missing', () => {
      expect(
        strategy.hasCredentials({
          clientId: '',
          clientSecret: 'secret456',
          tokenUrl: 'https://example.com/token',
        })
      ).toBe(false);
    });

    it('should return false when clientSecret is missing', () => {
      expect(
        strategy.hasCredentials({
          clientId: 'client123',
          clientSecret: '',
          tokenUrl: 'https://example.com/token',
        })
      ).toBe(false);
    });

    it('should return false when tokenUrl is missing', () => {
      expect(
        strategy.hasCredentials({
          clientId: 'client123',
          clientSecret: 'secret456',
          tokenUrl: '',
        })
      ).toBe(false);
    });
  });

  describe('validate', () => {
    it('should return no errors for valid data', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My OAuth2 ArcGIS',
        credentialType: 'OAUTH2',
        clientId: 'client123',
        clientSecret: 'secret456',
        tokenUrl: 'https://www.arcgis.com/sharing/rest/oauth2/token',
      });

      expect(errors).toEqual([]);
    });

    it('should return error when clientId is missing', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My OAuth2 ArcGIS',
        credentialType: 'OAUTH2',
        clientId: '',
        clientSecret: 'secret456',
        tokenUrl: 'https://example.com/token',
      });

      expect(errors).toContain('Client ID is required');
    });

    it('should return error when clientSecret is missing', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My OAuth2 ArcGIS',
        credentialType: 'OAUTH2',
        clientId: 'client123',
        clientSecret: '',
        tokenUrl: 'https://example.com/token',
      });

      expect(errors).toContain('Client Secret is required');
    });

    it('should return error when tokenUrl is missing', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My OAuth2 ArcGIS',
        credentialType: 'OAUTH2',
        clientId: 'client123',
        clientSecret: 'secret456',
        tokenUrl: '',
      });

      expect(errors).toContain('Token URL is required');
    });

    it('should return error when tokenUrl is invalid', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My OAuth2 ArcGIS',
        credentialType: 'OAUTH2',
        clientId: 'client123',
        clientSecret: 'secret456',
        tokenUrl: 'not-a-valid-url',
      });

      expect(errors).toContain('Token URL must be a valid URL');
    });
  });
});
