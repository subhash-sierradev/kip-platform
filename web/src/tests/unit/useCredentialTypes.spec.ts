import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CredentialTypeService } from '@/api/services/CredentialTypeService';
import { useCredentialTypes } from '@/composables/useCredentialTypes';

// Mock the CredentialTypeService
vi.mock('@/api/services/CredentialTypeService', () => ({
  CredentialTypeService: {
    getCredentialTypes: vi.fn(),
  },
}));

describe('useCredentialTypes', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Fixed credential types (Jira)', () => {
    it('should return BASIC_AUTH credential type for fixed services', async () => {
      const { credentialTypes, fetchCredentialTypes } = useCredentialTypes('JIRA', false);

      await fetchCredentialTypes();

      expect(credentialTypes.value).toHaveLength(1);
      expect(credentialTypes.value[0].value).toBe('BASIC_AUTH');
      expect(credentialTypes.value[0].label).toBe('Jira Basic Authentication');
      expect(credentialTypes.value[0].fields).toHaveLength(2);
      expect(credentialTypes.value[0].fields[0].key).toBe('username');
      expect(credentialTypes.value[0].fields[1].key).toBe('password');
    });

    it('should not call API for fixed credential types', async () => {
      const { fetchCredentialTypes } = useCredentialTypes('JIRA', false);

      await fetchCredentialTypes();

      expect(CredentialTypeService.getCredentialTypes).not.toHaveBeenCalled();
    });

    it('should include help text for password field', async () => {
      const { credentialTypes, fetchCredentialTypes } = useCredentialTypes('JIRA', false);

      await fetchCredentialTypes();

      const passwordField = credentialTypes.value[0].fields.find(f => f.key === 'password');
      expect(passwordField?.helpText).toBe(
        'API token (recommended) - Generate from Atlassian account security settings'
      );
    });
  });

  describe('Dynamic credential types (ArcGIS)', () => {
    it('should fetch credential types from API', async () => {
      const mockCredentialTypes = [
        { value: 'BASIC_AUTH', label: 'Basic Authentication' },
        { value: 'OAUTH2', label: 'OAuth 2.0' },
      ];

      vi.mocked(CredentialTypeService.getCredentialTypes).mockResolvedValue(
        mockCredentialTypes as any
      );

      const { credentialTypes, loading, fetchCredentialTypes } = useCredentialTypes('ARCGIS', true);

      expect(loading.value).toBe(false);

      const promise = fetchCredentialTypes();
      expect(loading.value).toBe(true);

      await promise;

      expect(loading.value).toBe(false);
      expect(credentialTypes.value).toHaveLength(2);
      expect(credentialTypes.value[0].value).toBe('BASIC_AUTH');
      expect(credentialTypes.value[1].value).toBe('OAUTH2');
    });

    it('should transform BASIC_AUTH with correct fields', async () => {
      vi.mocked(CredentialTypeService.getCredentialTypes).mockResolvedValue([
        { value: 'BASIC_AUTH', label: 'Basic Authentication' },
      ] as any);

      const { credentialTypes, fetchCredentialTypes } = useCredentialTypes('ARCGIS', true);

      await fetchCredentialTypes();

      const basicAuth = credentialTypes.value[0];
      expect(basicAuth.fields).toHaveLength(2);
      expect(basicAuth.fields[0].key).toBe('username');
      expect(basicAuth.fields[0].type).toBe('text');
      expect(basicAuth.fields[1].key).toBe('password');
      expect(basicAuth.fields[1].type).toBe('password');
    });

    it('should transform OAUTH2 with correct fields', async () => {
      vi.mocked(CredentialTypeService.getCredentialTypes).mockResolvedValue([
        { value: 'OAUTH2', label: 'OAuth 2.0' },
      ] as any);

      const { credentialTypes, fetchCredentialTypes } = useCredentialTypes('ARCGIS', true);

      await fetchCredentialTypes();

      const oauth2 = credentialTypes.value[0];
      expect(oauth2.fields).toHaveLength(4);
      expect(oauth2.fields[0].key).toBe('clientId');
      expect(oauth2.fields[1].key).toBe('clientSecret');
      expect(oauth2.fields[2].key).toBe('tokenUrl');
      expect(oauth2.fields[3].key).toBe('scope');
      expect(oauth2.fields[3].required).toBe(false);
    });

    it('should handle API errors', async () => {
      vi.mocked(CredentialTypeService.getCredentialTypes).mockRejectedValue(
        new Error('Network error')
      );

      const { credentialTypes, error, loading, fetchCredentialTypes } = useCredentialTypes(
        'ARCGIS',
        true
      );

      await fetchCredentialTypes();

      expect(loading.value).toBe(false);
      expect(error.value).toBe('Network error');
      expect(credentialTypes.value).toHaveLength(0);
    });

    it('should handle empty API response', async () => {
      vi.mocked(CredentialTypeService.getCredentialTypes).mockResolvedValue([] as any);

      const { credentialTypes, fetchCredentialTypes } = useCredentialTypes('ARCGIS', true);

      await fetchCredentialTypes();

      expect(credentialTypes.value).toHaveLength(0);
    });

    it('should handle non-Error exceptions', async () => {
      vi.mocked(CredentialTypeService.getCredentialTypes).mockRejectedValue('String error');

      const { error, loading, fetchCredentialTypes } = useCredentialTypes('ARCGIS', true);

      await fetchCredentialTypes();

      expect(loading.value).toBe(false);
      expect(error.value).toBe('Failed to load credential types');
    });

    it('should handle null error', async () => {
      vi.mocked(CredentialTypeService.getCredentialTypes).mockRejectedValue(null);

      const { error, fetchCredentialTypes } = useCredentialTypes('ARCGIS', true);

      await fetchCredentialTypes();

      expect(error.value).toBe('Failed to load credential types');
    });

    it('should transform API_KEY with correct fields', async () => {
      vi.mocked(CredentialTypeService.getCredentialTypes).mockResolvedValue([
        { value: 'API_KEY', label: 'API Key' },
      ] as any);

      const { credentialTypes, fetchCredentialTypes } = useCredentialTypes('ARCGIS', true);

      await fetchCredentialTypes();

      const apiKey = credentialTypes.value[0];
      expect(apiKey.fields).toHaveLength(1);
      expect(apiKey.fields[0].key).toBe('apiKey');
      expect(apiKey.fields[0].type).toBe('password');
      expect(apiKey.fields[0].required).toBe(true);
    });

    it('should clear previous error on successful fetch', async () => {
      vi.mocked(CredentialTypeService.getCredentialTypes).mockRejectedValueOnce(
        new Error('First error')
      );

      const { error, fetchCredentialTypes } = useCredentialTypes('ARCGIS', true);

      await fetchCredentialTypes();
      expect(error.value).toBe('First error');

      // Second call succeeds
      vi.mocked(CredentialTypeService.getCredentialTypes).mockResolvedValue([
        { value: 'BASIC_AUTH', label: 'Basic' },
      ] as any);
      await fetchCredentialTypes();

      expect(error.value).toBeNull();
    });
  });
});
