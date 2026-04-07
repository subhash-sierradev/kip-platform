import { beforeEach, describe, expect, it } from 'vitest';

import { ArcGISAPIKeyStrategy } from '@/strategies/ArcGISAPIKeyStrategy';
import { ArcGISBasicAuthStrategy } from '@/strategies/ArcGISBasicAuthStrategy';
import { ArcGISOAuth2Strategy } from '@/strategies/ArcGISOAuth2Strategy';
import { ConfluenceBasicAuthStrategy } from '@/strategies/ConfluenceBasicAuthStrategy';
import { CredentialStrategyRegistry } from '@/strategies/CredentialStrategyRegistry';
import { JiraBasicAuthStrategy } from '@/strategies/JiraBasicAuthStrategy';

describe('CredentialStrategyRegistry', () => {
  let registry: CredentialStrategyRegistry;

  beforeEach(() => {
    // Get fresh instance for each test
    registry = CredentialStrategyRegistry.getInstance();
  });

  describe('singleton pattern', () => {
    it('should return same instance on multiple calls', () => {
      const instance1 = CredentialStrategyRegistry.getInstance();
      const instance2 = CredentialStrategyRegistry.getInstance();

      expect(instance1).toBe(instance2);
    });
  });

  describe('default registrations', () => {
    it('should have Jira Basic Auth strategy registered', () => {
      const strategy = registry.getStrategy('JIRA', 'BASIC_AUTH');

      expect(strategy).toBeDefined();
      expect(strategy?.id).toBe('JIRA_BASIC_AUTH');
      expect(strategy).toBeInstanceOf(JiraBasicAuthStrategy);
    });

    it('should have ArcGIS Basic Auth strategy registered', () => {
      const strategy = registry.getStrategy('ARCGIS', 'BASIC_AUTH');

      expect(strategy).toBeDefined();
      expect(strategy?.id).toBe('ARCGIS_BASIC_AUTH');
      expect(strategy).toBeInstanceOf(ArcGISBasicAuthStrategy);
    });

    it('should have ArcGIS OAuth2 strategy registered', () => {
      const strategy = registry.getStrategy('ARCGIS', 'OAUTH2');

      expect(strategy).toBeDefined();
      expect(strategy?.id).toBe('ARCGIS_OAUTH2');
      expect(strategy).toBeInstanceOf(ArcGISOAuth2Strategy);
    });

    it('should have ArcGIS API Key strategy registered', () => {
      const strategy = registry.getStrategy('ARCGIS', 'API_KEY');

      expect(strategy).toBeDefined();
      expect(strategy?.id).toBe('ARCGIS_API_KEY');
      expect(strategy).toBeInstanceOf(ArcGISAPIKeyStrategy);
    });

    it('should have Confluence Basic Auth strategy registered', () => {
      const strategy = registry.getStrategy('CONFLUENCE', 'BASIC_AUTH');

      expect(strategy).toBeDefined();
      expect(strategy?.id).toBe('CONFLUENCE_BASIC_AUTH');
      expect(strategy).toBeInstanceOf(ConfluenceBasicAuthStrategy);
    });
  });

  describe('getStrategy', () => {
    it('should return strategy by service type and credential type', () => {
      const strategy = registry.getStrategy('JIRA', 'BASIC_AUTH');

      expect(strategy).toBeDefined();
      expect(strategy?.serviceType).toBe('JIRA');
      expect(strategy?.credentialType).toBe('BASIC_AUTH');
    });

    it('should return undefined for non-existent strategy', () => {
      const strategy = registry.getStrategy('SALESFORCE', 'BASIC_AUTH');

      expect(strategy).toBeUndefined();
    });

    it('should be case-sensitive for service type', () => {
      const strategy = registry.getStrategy('jira', 'BASIC_AUTH');

      expect(strategy).toBeUndefined();
    });
  });

  describe('getStrategiesForService', () => {
    it('should return all Jira strategies', () => {
      const strategies = registry.getStrategiesForService('JIRA');

      expect(strategies).toHaveLength(1);
      expect(strategies[0].serviceType).toBe('JIRA');
    });

    it('should return all ArcGIS strategies', () => {
      const strategies = registry.getStrategiesForService('ARCGIS');

      expect(strategies).toHaveLength(3);
      expect(strategies.every(s => s.serviceType === 'ARCGIS')).toBe(true);
    });

    it('should return empty array for non-existent service', () => {
      const strategies = registry.getStrategiesForService('SALESFORCE');

      expect(strategies).toEqual([]);
    });
  });

  describe('getCredentialTypes', () => {
    it('should return credential types for Jira', () => {
      const types = registry.getCredentialTypes('JIRA');

      expect(types).toEqual(['BASIC_AUTH']);
    });

    it('should return credential types for ArcGIS', () => {
      const types = registry.getCredentialTypes('ARCGIS');

      expect(types).toHaveLength(3);
      expect(types).toContain('BASIC_AUTH');
      expect(types).toContain('OAUTH2');
      expect(types).toContain('API_KEY');
    });

    it('should return empty array for non-existent service', () => {
      const types = registry.getCredentialTypes('SALESFORCE');

      expect(types).toEqual([]);
    });
  });

  describe('hasStrategy', () => {
    it('should return true for existing strategy', () => {
      expect(registry.hasStrategy('JIRA', 'BASIC_AUTH')).toBe(true);
      expect(registry.hasStrategy('ARCGIS', 'OAUTH2')).toBe(true);
    });

    it('should return false for non-existent strategy', () => {
      expect(registry.hasStrategy('SALESFORCE', 'BASIC_AUTH')).toBe(false);
      expect(registry.hasStrategy('JIRA', 'OAUTH2')).toBe(false);
    });
  });

  describe('getAllStrategyIds', () => {
    it('should return all registered strategy IDs', () => {
      const ids = registry.getAllStrategyIds();

      expect(ids).toHaveLength(5);
      expect(ids).toContain('JIRA_BASIC_AUTH');
      expect(ids).toContain('ARCGIS_BASIC_AUTH');
      expect(ids).toContain('ARCGIS_OAUTH2');
      expect(ids).toContain('ARCGIS_API_KEY');
      expect(ids).toContain('CONFLUENCE_BASIC_AUTH');
    });
  });

  describe('extensibility', () => {
    it('should support registering new custom strategy', () => {
      // Create a custom strategy
      const customStrategy = {
        id: 'CUSTOM_TEST',
        serviceType: 'CUSTOM',
        credentialType: 'TEST',
        displayName: 'Custom Test',
        getFields: () => [],
        buildTestPayload: () => ({}),
        validate: () => [],
        hasCredentials: () => true,
      };

      registry.register(customStrategy);

      const retrieved = registry.getStrategy('CUSTOM', 'TEST');
      expect(retrieved).toBe(customStrategy);
    });
  });
});
