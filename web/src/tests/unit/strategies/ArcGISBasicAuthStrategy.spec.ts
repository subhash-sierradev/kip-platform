import { describe, expect, it } from 'vitest';

import { ArcGISBasicAuthStrategy } from '@/strategies/ArcGISBasicAuthStrategy';

describe('ArcGISBasicAuthStrategy', () => {
  const strategy = new ArcGISBasicAuthStrategy();

  describe('properties', () => {
    it('should have correct id', () => {
      expect(strategy.id).toBe('ARCGIS_BASIC_AUTH');
    });

    it('should have correct service type', () => {
      expect(strategy.serviceType).toBe('ARCGIS');
    });

    it('should have correct credential type', () => {
      expect(strategy.credentialType).toBe('BASIC_AUTH');
    });

    it('should have correct display name', () => {
      expect(strategy.displayName).toBe('ArcGIS Basic Authentication');
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
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My ArcGIS',
        credentialType: 'BASIC_AUTH',
        username: 'arcgis_user',
        password: 'arcgis_pass',
      });

      expect(payload).toEqual({
        name: 'My ArcGIS',
        baseUrl: 'https://services.arcgis.com/xyz',
        credentialType: 'BASIC_AUTH',
        credentials: {
          username: 'arcgis_user',
          password: 'arcgis_pass',
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
  });

  describe('validate', () => {
    it('should return no errors for valid data', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My ArcGIS',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
      });

      expect(errors).toEqual([]);
    });

    it('should return error when username is missing', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My ArcGIS',
        credentialType: 'BASIC_AUTH',
        username: '',
        password: 'pass',
      });

      expect(errors).toContain('Username is required');
    });

    it('should return error when password is missing', () => {
      const errors = strategy.validate({
        baseUrl: 'https://services.arcgis.com/xyz',
        connectionName: 'My ArcGIS',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: '',
      });

      expect(errors).toContain('Password is required');
    });
  });
});
