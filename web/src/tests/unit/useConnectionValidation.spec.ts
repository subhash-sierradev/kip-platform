import { describe, expect, it } from 'vitest';
import { ref } from 'vue';

import { useConnectionValidation } from '@/composables/useConnectionValidation';
import type { ConnectionStepData, CredentialTypeOption } from '@/types/ConnectionStepData';

describe('useConnectionValidation', () => {
  const createBasicAuthFields = (): CredentialTypeOption[] => [
    {
      value: 'BASIC_AUTH',
      label: 'Basic Authentication',
      fields: [
        { key: 'username', label: 'Username', type: 'text', required: true },
        { key: 'password', label: 'Password', type: 'password', required: true },
      ],
    },
  ];

  const createOAuth2Fields = (): CredentialTypeOption[] => [
    {
      value: 'OAUTH2',
      label: 'OAuth2',
      fields: [
        { key: 'clientId', label: 'Client ID', type: 'text', required: true },
        { key: 'clientSecret', label: 'Client Secret', type: 'password', required: true },
        { key: 'tokenUrl', label: 'Token URL', type: 'url', required: true },
        { key: 'scope', label: 'Scope', type: 'text', required: false },
      ],
    },
  ];

  describe('canTestConnection', () => {
    it('should return true when all BASIC_AUTH fields are filled', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: 'user@example.com',
        password: 'password123',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { canTestConnection } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(canTestConnection.value).toBe(true);
    });

    it('should return false when base URL is empty', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: '',
        credentialType: 'BASIC_AUTH',
        username: 'user@example.com',
        password: 'password123',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { canTestConnection } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(canTestConnection.value).toBe(false);
    });

    it('should return false when username is empty for BASIC_AUTH', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://jira.example.com',
        credentialType: 'BASIC_AUTH',
        username: '',
        password: 'password123',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { canTestConnection } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(canTestConnection.value).toBe(false);
    });

    it('should return true when all OAUTH2 required fields are filled', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://arcgis.example.com',
        credentialType: 'OAUTH2',
        username: '',
        password: '',
        clientId: 'client-123',
        clientSecret: 'secret-456',
        tokenUrl: 'https://arcgis.example.com/token',
        scope: 'read write',
      });

      const { canTestConnection } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createOAuth2Fields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'ARCGIS',
      });

      expect(canTestConnection.value).toBe(true);
    });

    it('should return true when optional field is empty', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://arcgis.example.com',
        credentialType: 'OAUTH2',
        username: '',
        password: '',
        clientId: 'client-123',
        clientSecret: 'secret-456',
        tokenUrl: 'https://arcgis.example.com/token',
        scope: '',
      });

      const { canTestConnection } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createOAuth2Fields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'ARCGIS',
      });

      expect(canTestConnection.value).toBe(true);
    });

    it('should return true for existing connection when connection ID is present', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'existing',
        connected: false,
        connectionName: '',
        existingConnectionId: 'conn-123',
        baseUrl: '',
        credentialType: '',
        username: '',
        password: '',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { canTestConnection } = useConnectionValidation({
        connectionData,
        credentialTypes: ref([]),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(canTestConnection.value).toBe(true);
    });
  });

  describe('isStepValid', () => {
    it('should return true for new connection after successful test', () => {
      const connectionData = ref<ConnectionStepData>({
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
      });

      const { isStepValid } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(true),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(isStepValid.value).toBe(true);
    });

    it('should return false for new connection without test', () => {
      const connectionData = ref<ConnectionStepData>({
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
      });

      const { isStepValid } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(isStepValid.value).toBe(false);
    });

    it('should return true for existing connection after successful verification', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'existing',
        connected: false,
        connectionName: '',
        existingConnectionId: 'conn-123',
        baseUrl: '',
        credentialType: '',
        username: '',
        password: '',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { isStepValid } = useConnectionValidation({
        connectionData,
        credentialTypes: ref([]),
        testSuccess: ref(false),
        existingTestSuccess: ref(true),
        serviceType: 'JIRA',
      });

      expect(isStepValid.value).toBe(true);
    });

    it('should return false for existing connection without selection', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'existing',
        connected: false,
        connectionName: '',
        existingConnectionId: '',
        baseUrl: '',
        credentialType: '',
        username: '',
        password: '',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { isStepValid } = useConnectionValidation({
        connectionData,
        credentialTypes: ref([]),
        testSuccess: ref(false),
        existingTestSuccess: ref(true),
        serviceType: 'JIRA',
      });

      expect(isStepValid.value).toBe(false);
    });
  });

  describe('getValidationMessage', () => {
    it('should return empty string when step is valid', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://test.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { getValidationMessage } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(true),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getValidationMessage()).toBe('');
    });

    it('should return message for new connection without test', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://test.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { getValidationMessage } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getValidationMessage()).toBe('Please test the connection');
    });

    it('should return message for existing connection without selection', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'existing',
        connected: false,
        connectionName: '',
        existingConnectionId: '',
        baseUrl: '',
        credentialType: '',
        username: '',
        password: '',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { getValidationMessage } = useConnectionValidation({
        connectionData,
        credentialTypes: ref([]),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getValidationMessage()).toBe('Please select a connection');
    });

    it('should return message when base URL is missing', () => {
      const connectionData = ref<ConnectionStepData>({
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
      });

      const { getValidationMessage } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getValidationMessage()).toBe('Base URL is required');
    });
  });

  describe('getMissingFields', () => {
    it('should return empty array when all required fields are filled', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://test.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { getMissingFields } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getMissingFields.value).toEqual([]);
    });

    it('should return missing fields for BASIC_AUTH', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://test.com',
        credentialType: 'BASIC_AUTH',
        username: '',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { getMissingFields } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getMissingFields.value).toContain('username');
    });

    it('should not include optional fields in missing fields', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://test.com',
        credentialType: 'OAUTH2',
        username: '',
        password: '',
        clientId: 'client-123',
        clientSecret: 'secret-456',
        tokenUrl: 'https://test.com/token',
        scope: '',
      });

      const { getMissingFields } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createOAuth2Fields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getMissingFields.value).not.toContain('scope');
      expect(getMissingFields.value).toEqual([]);
    });
  });

  describe('canTestConnection — strategy not found', () => {
    it('should return false when no strategy exists for the given credential type', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://test.com',
        credentialType: 'UNKNOWN_CRED_TYPE_XYZ',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { canTestConnection } = useConnectionValidation({
        connectionData,
        credentialTypes: ref(createBasicAuthFields()),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(canTestConnection.value).toBe(false);
    });
  });

  describe('getValidationMessage — additional branches', () => {
    it('should return "Please verify the selected connection" when existing connection selected but not verified', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'existing',
        connected: false,
        connectionName: '',
        existingConnectionId: 'conn-123',
        baseUrl: '',
        credentialType: '',
        username: '',
        password: '',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { getValidationMessage } = useConnectionValidation({
        connectionData,
        credentialTypes: ref([]),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getValidationMessage()).toBe('Please verify the selected connection');
    });

    it('should return empty string when existing connection is selected and verified', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'existing',
        connected: false,
        connectionName: '',
        existingConnectionId: 'conn-123',
        baseUrl: '',
        credentialType: '',
        username: '',
        password: '',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { getValidationMessage } = useConnectionValidation({
        connectionData,
        credentialTypes: ref([]),
        testSuccess: ref(false),
        existingTestSuccess: ref(true),
        serviceType: 'JIRA',
      });

      expect(getValidationMessage()).toBe('');
    });

    it('should return "Invalid credential type" message when strategy not found for new connection', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://test.com',
        credentialType: 'NONEXISTENT_TYPE',
        username: '',
        password: '',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { getValidationMessage } = useConnectionValidation({
        connectionData,
        credentialTypes: ref([]),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getValidationMessage()).toBe('Invalid credential type: NONEXISTENT_TYPE');
    });
  });

  describe('getRequiredFields — credential type not found', () => {
    it('should return empty array when credential type is not in credentialTypes list', () => {
      const connectionData = ref<ConnectionStepData>({
        connectionMethod: 'new',
        connected: false,
        connectionName: 'Test',
        existingConnectionId: '',
        baseUrl: 'https://test.com',
        credentialType: 'MISSING_TYPE',
        username: '',
        password: '',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });

      const { getRequiredFields } = useConnectionValidation({
        connectionData,
        credentialTypes: ref([]),
        testSuccess: ref(false),
        existingTestSuccess: ref(false),
        serviceType: 'JIRA',
      });

      expect(getRequiredFields.value).toEqual([]);
    });
  });
});
