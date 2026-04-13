import { describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

import type { ConnectionStepData, CredentialTypeOption } from '@/types/ConnectionStepData';

// Mock dependencies before importing the composable
vi.mock('@/composables/useCredentialTypes', () => ({
  useCredentialTypes: vi.fn(() => ({
    credentialTypes: ref<CredentialTypeOption[]>([]),
    loading: ref(false),
    error: ref(null),
    fetchCredentialTypes: vi.fn().mockResolvedValue(undefined),
  })),
}));

vi.mock('@/composables/useConnectionValidation', () => ({
  useConnectionValidation: vi.fn(() => ({
    canTestConnection: ref(true),
    isStepValid: ref(false),
    getRequiredFields: ref([]),
    getMissingFields: ref([]),
    getValidationMessage: vi.fn().mockReturnValue(''),
    isFieldFilled: vi.fn().mockReturnValue(true),
  })),
}));

vi.mock('@/api/models/enums', () => ({
  ServiceType: {
    ARCGIS: 'ARCGIS',
    JIRA: 'JIRA',
  },
}));

vi.mock('@/utils/stringFormatUtils', () => ({
  toPlainText: vi.fn((text: string) => text),
}));

import { useCreateConnectionForm } from '@/composables/useCreateConnectionForm';
import { useCredentialTypes } from '@/composables/useCredentialTypes';

const DEFAULT_ARCGIS_TOKEN_URL = 'https://www.arcgis.com/sharing/rest/oauth2/token';

function makeSubmitMock(response: Record<string, unknown> = {}) {
  return vi.fn().mockResolvedValue({ lastConnectionStatus: 'SUCCESS', ...response });
}

const BASE_CONFIG = {
  baseUrlLabel: 'Base URL',
  baseUrlPlaceholder: 'https://example.com',
  supportsDynamicCredentials: true,
  showCredentialTypeSelector: true,
  requiresConnectionName: true,
} as const;

describe('useCreateConnectionForm', () => {
  describe('getDefaultTokenUrl (via buildDefaultCreateConnectionData)', () => {
    it('should set DEFAULT_ARCGIS_TOKEN_URL for ARCGIS service type', () => {
      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'ARCGIS', defaultCredentialType: 'OAUTH2' },
        submitConnection: makeSubmitMock(),
      });

      expect(form.connectionData.value.tokenUrl).toBe(DEFAULT_ARCGIS_TOKEN_URL);
    });

    it('should set empty tokenUrl for non-ARCGIS service type', () => {
      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'BASIC_AUTH' },
        submitConnection: makeSubmitMock(),
      });

      expect(form.connectionData.value.tokenUrl).toBe('');
    });
  });

  describe('buildDefaultCreateConnectionData — defaultCredentialType fallback', () => {
    it('should use empty string for credentialType when defaultCredentialType is not set', () => {
      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA' },
        submitConnection: makeSubmitMock(),
      });

      expect(form.connectionData.value.credentialType).toBe('');
    });
  });

  describe('onCredentialTypeChange / updateCredentialTypeData', () => {
    it('should reset credential fields and test state when credential type changes', () => {
      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'BASIC_AUTH' },
        submitConnection: makeSubmitMock(),
      });

      // Set some credential data first
      form.connectionData.value.username = 'user';
      form.connectionData.value.password = 'pass';
      form.connectionData.value.clientId = 'cid';
      form.connectionData.value.clientSecret = 'csecret';

      form.onCredentialTypeChange();

      expect(form.connectionData.value.username).toBe('');
      expect(form.connectionData.value.password).toBe('');
      expect(form.connectionData.value.clientId).toBe('');
      expect(form.connectionData.value.clientSecret).toBe('');
    });
  });

  describe('handleSubmit', () => {
    it('should return success false and set testMessage when no lastConnectionMessage and isSuccess is false', async () => {
      const submitConnection = vi.fn().mockResolvedValue({
        lastConnectionStatus: 'FAILED',
        lastConnectionMessage: undefined,
      });

      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'BASIC_AUTH' },
        submitConnection,
      });

      const result = await form.handleSubmit();

      expect(result.success).toBe(false);
      expect(form.testMessage.value).toBe('Connection test failed');
      expect(form.testSuccess.value).toBe(false);
    });

    it('should use apiError.message when body.message is absent during catch', async () => {
      const submitConnection = vi
        .fn()
        .mockRejectedValue({ message: 'Socket timeout', body: undefined });

      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'BASIC_AUTH' },
        submitConnection,
      });

      const result = await form.handleSubmit();

      expect(result.success).toBe(false);
      expect(form.testMessage.value).toBe('Socket timeout');
    });

    it('should use final fallback message when both body.message and apiError.message are absent', async () => {
      const submitConnection = vi.fn().mockRejectedValue({});

      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'BASIC_AUTH' },
        submitConnection,
      });

      const result = await form.handleSubmit();

      expect(result.success).toBe(false);
      expect(form.testMessage.value).toBe('Failed to test connection');
    });
  });

  describe('currentCredentialFields computed', () => {
    it('should return empty array when no matching credential type is found', () => {
      vi.mocked(useCredentialTypes).mockReturnValue({
        credentialTypes: ref<CredentialTypeOption[]>([
          {
            value: 'BASIC_AUTH',
            label: 'Basic Auth',
            fields: [{ key: 'username', label: 'Username', type: 'text', required: true }],
          },
        ]),
        loading: ref(false),
        error: ref(null),
        fetchCredentialTypes: vi.fn().mockResolvedValue(undefined),
      });

      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'OAUTH2' },
        submitConnection: makeSubmitMock(),
      });

      // credentialType is 'OAUTH2' but only 'BASIC_AUTH' exists in credentialTypes
      expect(form.currentCredentialFields.value).toEqual([]);
    });
  });

  describe('useTwoColCredentialGrid computed', () => {
    it('should return false when clientId is present but clientSecret is absent', () => {
      vi.mocked(useCredentialTypes).mockReturnValue({
        credentialTypes: ref<CredentialTypeOption[]>([
          {
            value: 'PARTIAL_OAUTH',
            label: 'Partial OAuth',
            fields: [{ key: 'clientId', label: 'Client ID', type: 'text', required: true }],
          },
        ]),
        loading: ref(false),
        error: ref(null),
        fetchCredentialTypes: vi.fn().mockResolvedValue(undefined),
      });

      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'PARTIAL_OAUTH' },
        submitConnection: makeSubmitMock(),
      });

      expect(form.useTwoColCredentialGrid.value).toBe(false);
    });

    it('should return true when both clientId and clientSecret are present', () => {
      vi.mocked(useCredentialTypes).mockReturnValue({
        credentialTypes: ref<CredentialTypeOption[]>([
          {
            value: 'OAUTH2',
            label: 'OAuth2',
            fields: [
              { key: 'clientId', label: 'Client ID', type: 'text', required: true },
              { key: 'clientSecret', label: 'Client Secret', type: 'password', required: true },
            ],
          },
        ]),
        loading: ref(false),
        error: ref(null),
        fetchCredentialTypes: vi.fn().mockResolvedValue(undefined),
      });

      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'OAUTH2' },
        submitConnection: makeSubmitMock(),
      });

      expect(form.useTwoColCredentialGrid.value).toBe(true);
    });
  });

  describe('resetFormState', () => {
    it('should reset connectionData and test state to defaults', () => {
      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'BASIC_AUTH' },
        submitConnection: makeSubmitMock(),
      });

      // Mutate state
      form.connectionData.value.username = 'dirty';
      form.tested.value = true;
      form.testSuccess.value = true;
      form.testMessage.value = 'some message';

      form.resetFormState();

      expect(form.connectionData.value.username).toBe('');
      expect(form.tested.value).toBe(false);
      expect(form.testSuccess.value).toBe(false);
      expect(form.testMessage.value).toBe('');
    });
  });

  describe('updateConnectionData', () => {
    it('should update connectionData and reset test state', () => {
      const form = useCreateConnectionForm({
        config: { ...BASE_CONFIG, serviceType: 'JIRA', defaultCredentialType: 'BASIC_AUTH' },
        submitConnection: makeSubmitMock(),
      });

      form.tested.value = true;
      form.testSuccess.value = true;

      const newData: ConnectionStepData = {
        connectionMethod: 'new',
        baseUrl: 'https://new.example.com',
        credentialType: 'BASIC_AUTH',
        connectionName: 'Updated',
        username: 'newuser',
        password: 'newpass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
        connected: false,
      };

      form.updateConnectionData(newData);

      expect(form.connectionData.value.baseUrl).toBe('https://new.example.com');
      expect(form.tested.value).toBe(false);
      expect(form.testSuccess.value).toBe(false);
    });
  });
});
