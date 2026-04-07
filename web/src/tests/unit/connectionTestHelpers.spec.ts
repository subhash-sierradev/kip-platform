import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { ConnectionStepData } from '@/types/ConnectionStepData';
import { buildIntegrationSecret, validateConnectionData } from '@/utils/connectionTestHelpers';

// Helper function to create mock ConnectionStepData
const createMockConnectionData = (
  overrides: Partial<ConnectionStepData> = {}
): ConnectionStepData =>
  ({
    baseUrl: 'https://example.com',
    credentialType: 'BASIC_AUTH',
    credentials: {},
    connectionMethod: 'API',
    connected: false,
    ...overrides,
  }) as ConnectionStepData;

// Mock functions for strategy
const mockBuildTestPayload = vi.fn();
const mockValidate = vi.fn();
const mockGetStrategy = vi.fn();

// Mock CredentialStrategyRegistry
vi.mock('@/strategies', () => ({
  CredentialStrategyRegistry: {
    getInstance: () => ({
      getStrategy: mockGetStrategy,
    }),
  },
}));

describe('connectionTestHelpers', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('buildIntegrationSecret', () => {
    it('builds integration secret successfully', () => {
      const mockStrategy = {
        buildTestPayload: mockBuildTestPayload,
        validate: mockValidate,
      };

      mockGetStrategy.mockReturnValue(mockStrategy);
      mockBuildTestPayload.mockReturnValue({
        credentials: {
          username: 'test-user',
          password: 'test-pass',
        },
      });

      const connectionData = createMockConnectionData();

      const result = buildIntegrationSecret('JIRA', connectionData);

      expect(result).toEqual({
        baseUrl: 'https://example.com',
        authType: 'BASIC_AUTH',
        credentials: {
          username: 'test-user',
          password: 'test-pass',
        },
      });

      expect(mockGetStrategy).toHaveBeenCalledWith('JIRA', 'BASIC_AUTH');
      expect(mockBuildTestPayload).toHaveBeenCalledWith(connectionData);
    });

    it('throws error when strategy not found', () => {
      mockGetStrategy.mockReturnValue(null);

      const connectionData = createMockConnectionData({ credentialType: 'INVALID_TYPE' });

      expect(() => {
        buildIntegrationSecret('JIRA', connectionData);
      }).toThrow('No strategy found for JIRA with INVALID_TYPE');
    });

    it('handles different service types', () => {
      const mockStrategy = {
        buildTestPayload: mockBuildTestPayload,
        validate: mockValidate,
      };

      mockGetStrategy.mockReturnValue(mockStrategy);
      mockBuildTestPayload.mockReturnValue({
        credentials: { apiKey: 'test-key' },
      });

      const connectionData = createMockConnectionData({
        baseUrl: 'https://arcgis.example.com',
        credentialType: 'API_KEY',
      });

      const result = buildIntegrationSecret('ARCGIS', connectionData);

      expect(result.authType).toBe('API_KEY');
      expect(mockGetStrategy).toHaveBeenCalledWith('ARCGIS', 'API_KEY');
    });

    it('handles OAuth2 credentials', () => {
      const mockStrategy = {
        buildTestPayload: mockBuildTestPayload,
        validate: mockValidate,
      };

      mockGetStrategy.mockReturnValue(mockStrategy);
      mockBuildTestPayload.mockReturnValue({
        credentials: {
          clientId: 'client-123',
          clientSecret: 'secret-456',
          tokenUrl: 'https://oauth.example.com/token',
          scope: 'read write',
        },
      });

      const connectionData = createMockConnectionData({ credentialType: 'OAUTH2' });

      const result = buildIntegrationSecret('ARCGIS', connectionData);

      expect(result.credentials).toHaveProperty('clientId');
      expect(result.credentials).toHaveProperty('clientSecret');
      expect(result.credentials).toHaveProperty('tokenUrl');
    });
  });

  describe('validateConnectionData', () => {
    it('validates successfully with no errors', () => {
      const mockStrategy = {
        buildTestPayload: mockBuildTestPayload,
        validate: mockValidate,
      };

      mockGetStrategy.mockReturnValue(mockStrategy);
      mockValidate.mockReturnValue([]);

      const connectionData = createMockConnectionData();

      const result = validateConnectionData('JIRA', connectionData);

      expect(result).toEqual({ valid: true, message: '' });
      expect(mockGetStrategy).toHaveBeenCalledWith('JIRA', 'BASIC_AUTH');
      expect(mockValidate).toHaveBeenCalledWith(connectionData);
    });

    it('returns first error message when validation fails', () => {
      const mockStrategy = {
        buildTestPayload: mockBuildTestPayload,
        validate: mockValidate,
      };

      mockGetStrategy.mockReturnValue(mockStrategy);
      mockValidate.mockReturnValue(['Username is required', 'Password is required']);

      const connectionData = createMockConnectionData();

      const result = validateConnectionData('JIRA', connectionData);

      expect(result).toEqual({ valid: false, message: 'Username is required' });
    });

    it('returns error when strategy not found', () => {
      mockGetStrategy.mockReturnValue(null);

      const connectionData = createMockConnectionData({ credentialType: 'INVALID_TYPE' });

      const result = validateConnectionData('JIRA', connectionData);

      expect(result).toEqual({
        valid: false,
        message: 'No strategy found for JIRA with INVALID_TYPE',
      });
    });

    it('handles single validation error', () => {
      const mockStrategy = {
        buildTestPayload: mockBuildTestPayload,
        validate: mockValidate,
      };

      mockGetStrategy.mockReturnValue(mockStrategy);
      mockValidate.mockReturnValue(['Base URL is required']);

      const connectionData = createMockConnectionData({ baseUrl: '' });

      const result = validateConnectionData('JIRA', connectionData);

      expect(result).toEqual({ valid: false, message: 'Base URL is required' });
    });

    it('validates ArcGIS connections', () => {
      const mockStrategy = {
        buildTestPayload: mockBuildTestPayload,
        validate: mockValidate,
      };

      mockGetStrategy.mockReturnValue(mockStrategy);
      mockValidate.mockReturnValue([]);

      const connectionData = createMockConnectionData({
        baseUrl: 'https://arcgis.example.com',
        credentialType: 'OAUTH2',
      });

      const result = validateConnectionData('ARCGIS', connectionData);

      expect(result.valid).toBe(true);
      expect(mockGetStrategy).toHaveBeenCalledWith('ARCGIS', 'OAUTH2');
    });

    it('handles multiple validation errors', () => {
      const mockStrategy = {
        buildTestPayload: mockBuildTestPayload,
        validate: mockValidate,
      };

      mockGetStrategy.mockReturnValue(mockStrategy);
      mockValidate.mockReturnValue([
        'Base URL is required',
        'Credentials are required',
        'Invalid URL format',
      ]);

      const connectionData = createMockConnectionData({ baseUrl: '' });

      const result = validateConnectionData('JIRA', connectionData);

      // Should return first error only
      expect(result).toEqual({ valid: false, message: 'Base URL is required' });
    });
  });
});
