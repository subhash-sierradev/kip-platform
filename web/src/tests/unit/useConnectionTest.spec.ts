import { beforeEach, describe, expect, it, vi } from 'vitest';

import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import { useConnectionTest } from '@/composables/useConnectionTest';
import type { ConnectionStepData } from '@/types/ConnectionStepData';

// Mock the IntegrationConnectionService
vi.mock('@/api/services/IntegrationConnectionService', () => ({
  IntegrationConnectionService: {
    testAndCreateConnection: vi.fn(),
  },
}));

describe('useConnectionTest', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('testConnection - BASIC_AUTH', () => {
    it('should test connection with basic auth credentials', async () => {
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockResolvedValue({
        id: 'new-conn-id',
        lastConnectionStatus: 'SUCCESS',
        lastConnectionMessage: 'Connection successful',
      } as any);

      const { testConnection, testSuccess, testMessage, createdConnectionId } =
        useConnectionTest('JIRA');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test Jira',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: 'user@example.com',
        password: 'password123',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      const result = await testConnection(connectionData);

      expect(result).toBe(true);
      expect(testSuccess.value).toBe(true);
      expect(testMessage.value).toBe('Connection successful');
      expect(createdConnectionId.value).toBe('new-conn-id');
    });

    it('should handle failed connection test', async () => {
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockResolvedValue({
        id: 'conn-id',
        lastConnectionStatus: 'FAILED',
        lastConnectionMessage: 'Authentication failed',
      } as any);

      const { testConnection, testSuccess, testMessage } = useConnectionTest('JIRA');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test Jira',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: 'user@example.com',
        password: 'wrong-password',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      const result = await testConnection(connectionData);

      expect(result).toBe(false);
      expect(testSuccess.value).toBe(false);
      expect(testMessage.value).toBe('Authentication failed');
    });
  });

  describe('testConnection - OAUTH2', () => {
    it('should test connection with OAuth2 credentials', async () => {
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockResolvedValue({
        id: 'oauth-conn-id',
        lastConnectionStatus: 'SUCCESS',
        lastConnectionMessage: 'OAuth2 connection successful',
      } as any);

      const { testConnection, testSuccess, testMessage, createdConnectionId } =
        useConnectionTest('ARCGIS');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test ArcGIS',
        existingConnectionId: '',
        baseUrl: 'https://arcgis.example.com',
        credentialType: 'OAUTH2',
        username: '',
        password: '',
        clientId: 'client-123',
        clientSecret: 'secret-456',
        tokenUrl: 'https://arcgis.example.com/oauth/token',
        scope: 'read write',
      };

      const result = await testConnection(connectionData);

      expect(result).toBe(true);
      expect(testSuccess.value).toBe(true);
      expect(testMessage.value).toBe('OAuth2 connection successful');
      expect(createdConnectionId.value).toBe('oauth-conn-id');
    });
  });

  describe('testConnection - API_KEY', () => {
    it('should test connection with API key credentials', async () => {
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockResolvedValue({
        id: 'apikey-conn-id',
        lastConnectionStatus: 'SUCCESS',
        lastConnectionMessage: 'API key connection successful',
      } as any);

      const { testConnection, testSuccess, testMessage, createdConnectionId } =
        useConnectionTest('ARCGIS');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test ArcGIS API Key',
        existingConnectionId: '',
        baseUrl: 'https://arcgis.example.com',
        credentialType: 'API_KEY',
        username: '',
        password: 'api-key-12345',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      const result = await testConnection(connectionData);

      expect(result).toBe(true);
      expect(testSuccess.value).toBe(true);
      expect(testMessage.value).toBe('API key connection successful');
      expect(createdConnectionId.value).toBe('apikey-conn-id');
    });
  });

  describe('testConnection - error handling', () => {
    it('should handle API error', async () => {
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockRejectedValue(
        new Error('Network error')
      );

      const { testConnection, testSuccess, testMessage } = useConnectionTest('JIRA');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      const result = await testConnection(connectionData);

      expect(result).toBe(false);
      expect(testSuccess.value).toBe(false);
      expect(testMessage.value).toContain('Network error');
    });

    it('should strip HTML tags from backend error message', async () => {
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockRejectedValue({
        body: {
          message: 'A connection named <strong>test Api</strong> already exists for this service.',
        },
      });

      const { testConnection, testSuccess, testMessage } = useConnectionTest('ARCGIS');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'test Api',
        existingConnectionId: '',
        baseUrl: 'https://arcgis.example.com',
        credentialType: 'OAUTH2',
        username: '',
        password: '',
        clientId: 'client',
        clientSecret: 'secret',
        tokenUrl: 'https://arcgis.example.com/oauth/token',
        scope: '',
      };

      const result = await testConnection(connectionData);

      expect(result).toBe(false);
      expect(testSuccess.value).toBe(false);
      expect(testMessage.value).toBe(
        'A connection named test Api already exists for this service.'
      );
    });
  });

  describe('testButtonText', () => {
    it('should show "Testing..." during test', async () => {
      let resolveTest: any;
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockReturnValue(
        new Promise(resolve => {
          resolveTest = resolve;
        }) as any
      );

      const { testConnection, testButtonText } = useConnectionTest('JIRA');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      const promise = testConnection(connectionData);

      await new Promise(resolve => setTimeout(resolve, 0));
      expect(testButtonText.value).toBe('Testing...');

      resolveTest({ id: 'id', lastConnectionStatus: 'SUCCESS' });
      await promise;
    });

    it('should show "Test Connection" after successful test', async () => {
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockResolvedValue({
        id: 'conn-id',
        lastConnectionStatus: 'SUCCESS',
      } as any);

      const { testConnection, testButtonText } = useConnectionTest('JIRA');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      await testConnection(connectionData);

      expect(testButtonText.value).toBe('Test Connection');
    });

    it('should show "Test Connection" after failed test', async () => {
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockResolvedValue({
        id: 'conn-id',
        lastConnectionStatus: 'FAILED',
      } as any);

      const { testConnection, testButtonText } = useConnectionTest('JIRA');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      await testConnection(connectionData);

      expect(testButtonText.value).toBe('Test Connection');
    });
  });

  describe('validateForTest', () => {
    it('should validate required fields for BASIC_AUTH', () => {
      const { validateForTest } = useConnectionTest('JIRA');

      const validData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      const result = validateForTest(validData);
      expect(result.valid).toBe(true);
      expect(result.message).toBe('');
    });

    it('should fail validation when base URL is missing', () => {
      const { validateForTest } = useConnectionTest('JIRA');

      const invalidData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: '',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      const result = validateForTest(invalidData);
      expect(result.valid).toBe(false);
      expect(result.message).toBe('Base URL is required');
    });

    it('should fail validation when username is missing for BASIC_AUTH', () => {
      const { validateForTest } = useConnectionTest('JIRA');

      const invalidData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: '',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      const result = validateForTest(invalidData);
      expect(result.valid).toBe(false);
      expect(result.message).toBe('Email address is required');
    });
  });

  describe('resetTest', () => {
    it('should reset test state', async () => {
      vi.mocked(IntegrationConnectionService.testAndCreateConnection).mockResolvedValue({
        id: 'conn-id',
        lastConnectionStatus: 'SUCCESS',
        lastConnectionMessage: 'Success',
      } as any);

      const { testConnection, resetTest, tested, testSuccess, testMessage, createdConnectionId } =
        useConnectionTest('JIRA');

      const connectionData: ConnectionStepData = {
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      };

      await testConnection(connectionData);

      expect(tested.value).toBe(true);
      expect(testSuccess.value).toBe(true);

      resetTest();

      expect(tested.value).toBe(false);
      expect(testSuccess.value).toBe(false);
      expect(testMessage.value).toBe('');
      expect(createdConnectionId.value).toBeNull();
    });
  });
});
