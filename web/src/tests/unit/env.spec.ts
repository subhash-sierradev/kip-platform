import { beforeEach, describe, expect, it, vi } from 'vitest';

import { env, getApiBaseUrl, getEnvironmentConfig } from '@/config/env';

describe('env', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  describe('getEnvironmentConfig', () => {
    it('returns config with replaced build-time variables', () => {
      const config = getEnvironmentConfig();

      expect(config).toHaveProperty('API_BASE_URL');
      expect(config).toHaveProperty('KEYCLOAK_URL');
      expect(config).toHaveProperty('KEYCLOAK_REALM');
      expect(config).toHaveProperty('KEYCLOAK_CLIENT_ID');
    });

    it('falls back to import.meta.env when build-time vars not replaced', () => {
      // Build-time vars start with '__VITE_' indicating they weren't replaced
      const config = getEnvironmentConfig();

      // These should fall back to defaults or import.meta.env
      expect(config.API_BASE_URL).toBeTruthy();
      expect(config.KEYCLOAK_URL).toBeTruthy();
      expect(config.KEYCLOAK_REALM).toBeTruthy();
      expect(config.KEYCLOAK_CLIENT_ID).toBeTruthy();
    });

    it('uses default values when environment variables are not set', () => {
      const config = getEnvironmentConfig();

      // Should use defaults from the fallback logic
      expect(config.API_BASE_URL).toBeTruthy();
      expect(config.KEYCLOAK_URL).toBeTruthy();
      expect(config.KEYCLOAK_REALM).toBeTruthy();
      expect(config.KEYCLOAK_CLIENT_ID).toBeTruthy();
    });
  });

  describe('getApiBaseUrl', () => {
    it('returns API base URL with /api suffix', () => {
      const apiBaseUrl = getApiBaseUrl();

      expect(apiBaseUrl).toContain('/api');
      expect(apiBaseUrl).toMatch(/^https?:\/\/.+\/api$/);
    });

    it('formats URL correctly from config', () => {
      const apiBaseUrl = getApiBaseUrl();

      expect(apiBaseUrl).toBe('http://localhost:8085/api');
    });
  });

  describe('env object (legacy export)', () => {
    it('provides apiBaseUrl getter', () => {
      expect(env.apiBaseUrl).toBeDefined();
      expect(env.apiBaseUrl).toContain('/api');
    });

    it('apiBaseUrl getter returns same as getApiBaseUrl', () => {
      expect(env.apiBaseUrl).toBe(getApiBaseUrl());
    });

    it('provides keycloak config getter', () => {
      const keycloakConfig = env.keycloak;

      expect(keycloakConfig).toHaveProperty('url');
      expect(keycloakConfig).toHaveProperty('realm');
      expect(keycloakConfig).toHaveProperty('clientId');
    });

    it('keycloak getter returns correct structure', () => {
      const keycloakConfig = env.keycloak;
      const envConfig = getEnvironmentConfig();

      expect(keycloakConfig.url).toBe(envConfig.KEYCLOAK_URL);
      expect(keycloakConfig.realm).toBe(envConfig.KEYCLOAK_REALM);
      expect(keycloakConfig.clientId).toBe(envConfig.KEYCLOAK_CLIENT_ID);
    });

    it('keycloak getter returns consistent values on multiple calls', () => {
      const config1 = env.keycloak;
      const config2 = env.keycloak;

      expect(config1.url).toBe(config2.url);
      expect(config1.realm).toBe(config2.realm);
      expect(config1.clientId).toBe(config2.clientId);
    });
  });

  describe('build-time replacement detection', () => {
    it('detects when build-time variables are replaced', () => {
      const config = getEnvironmentConfig();

      // In test environment, build-time vars won't be replaced
      // So we're testing the fallback behavior
      expect(config.API_BASE_URL).not.toMatch(/^__VITE_/);
      expect(config.KEYCLOAK_URL).not.toMatch(/^__VITE_/);
      expect(config.KEYCLOAK_REALM).not.toMatch(/^__VITE_/);
      expect(config.KEYCLOAK_CLIENT_ID).not.toMatch(/^__VITE_/);
    });
  });

  describe('EnvironmentConfig interface compliance', () => {
    it('returns object matching EnvironmentConfig interface', () => {
      const config = getEnvironmentConfig();

      const expectedKeys = ['API_BASE_URL', 'KEYCLOAK_URL', 'KEYCLOAK_REALM', 'KEYCLOAK_CLIENT_ID'];
      const actualKeys = Object.keys(config);

      expect(actualKeys.sort()).toEqual(expectedKeys.sort());
    });

    it('all config values are non-empty strings', () => {
      const config = getEnvironmentConfig();

      expect(typeof config.API_BASE_URL).toBe('string');
      expect(config.API_BASE_URL.length).toBeGreaterThan(0);

      expect(typeof config.KEYCLOAK_URL).toBe('string');
      expect(config.KEYCLOAK_URL.length).toBeGreaterThan(0);

      expect(typeof config.KEYCLOAK_REALM).toBe('string');
      expect(config.KEYCLOAK_REALM.length).toBeGreaterThan(0);

      expect(typeof config.KEYCLOAK_CLIENT_ID).toBe('string');
      expect(config.KEYCLOAK_CLIENT_ID.length).toBeGreaterThan(0);
    });
  });
});
