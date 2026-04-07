import { describe, expect, it } from 'vitest';

import { ArcGISAPIKeyStrategy } from '@/strategies/ArcGISAPIKeyStrategy';

describe('ArcGISAPIKeyStrategy', () => {
  const strategy = new ArcGISAPIKeyStrategy();

  describe('properties', () => {
    it('should have correct id', () => {
      expect(strategy.id).toBe('ARCGIS_API_KEY');
    });

    it('should have correct service type', () => {
      expect(strategy.serviceType).toBe('ARCGIS');
    });

    it('should have correct credential type', () => {
      expect(strategy.credentialType).toBe('API_KEY');
    });

    it('should have correct display name', () => {
      expect(strategy.displayName).toBe('ArcGIS API Key');
    });
  });

  describe('getFields', () => {
    it('should return single apiKey field', () => {
      const fields = strategy.getFields();

      expect(fields).toHaveLength(1);
      expect(fields[0].key).toBe('apiKey');
      expect(fields[0].required).toBe(true);
      expect(fields[0].type).toBe('password');
    });
  });

  describe('buildTestPayload', () => {
    it('should build correct payload with API key', () => {
      const payload = strategy.buildTestPayload({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My API Key ArcGIS',
        credentialType: 'API_KEY',
        apiKey: 'my-api-key-12345',
      });

      expect(payload).toEqual({
        name: 'My API Key ArcGIS',
        baseUrl: 'https://services.arcgis.com/xyz',
        credentialType: 'API_KEY',
        credentials: {
          apiKey: 'my-api-key-12345',
        },
      });
    });
  });

  describe('hasCredentials', () => {
    it('should return true when apiKey is provided', () => {
      expect(
        strategy.hasCredentials({
          apiKey: 'my-api-key',
        })
      ).toBe(true);
    });

    it('should return false when apiKey is missing', () => {
      expect(
        strategy.hasCredentials({
          apiKey: '',
        })
      ).toBe(false);
    });

    it('should return false when apiKey is undefined', () => {
      expect(strategy.hasCredentials({})).toBe(false);
    });
  });

  describe('validate', () => {
    it('should return no errors for valid data', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My API Key ArcGIS',
        credentialType: 'API_KEY',
        apiKey: 'my-api-key-12345',
      });

      expect(errors).toEqual([]);
    });

    it('should return error when apiKey is missing', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My API Key ArcGIS',
        credentialType: 'API_KEY',
        apiKey: '',
      });

      expect(errors).toContain('API Key is required');
    });

    it('should return multiple errors when base fields are also missing', () => {
      const errors = strategy.validate({
        baseUrl: '',
        connectionName: '',
        credentialType: 'API_KEY',
        apiKey: '',
      });

      expect(errors.length).toBeGreaterThan(1);
      expect(errors).toContain('Base URL is required');
      expect(errors).toContain('Connection name is required');
      expect(errors).toContain('API Key is required');
    });
  });
});
