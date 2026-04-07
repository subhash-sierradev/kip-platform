import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mock before imports to ensure service uses the mocked request
vi.mock('@/api/core/request', () => ({ request: vi.fn() }));

import { request as coreRequest } from '@/api/core/request';
import { CredentialTypeService } from '@/api/services/CredentialTypeService';

beforeEach(() => {
  (coreRequest as any).mockReset?.();
});

describe('CredentialTypeService', () => {
  it('transforms credential types for dropdown', async () => {
    (coreRequest as any).mockResolvedValueOnce([
      {
        credentialAuthType: 'BASIC_AUTH',
        displayName: 'Basic Auth',
        isEnabled: true,
        requiredFields: ['username', 'password'],
      },
      {
        credentialAuthType: 'OAUTH2',
        displayName: 'OAuth2',
        isEnabled: false,
        requiredFields: ['clientId'],
      },
    ]);

    const options = await CredentialTypeService.getCredentialTypes();
    expect(options[0]).toMatchObject({
      value: 'BASIC_AUTH',
      label: 'Basic Auth',
      isEnabled: true,
      code: 'BASIC_AUTH',
    });
    expect(options[1]).toMatchObject({
      value: 'OAUTH2',
      label: 'OAuth2',
      isEnabled: false,
      code: 'OAUTH2',
    });

    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/credential-types',
      })
    );
  });

  it('filters enabled credential types', async () => {
    (coreRequest as any).mockResolvedValueOnce([
      {
        credentialAuthType: 'BASIC_AUTH',
        displayName: 'Basic Auth',
        isEnabled: true,
        requiredFields: [],
      },
      { credentialAuthType: 'OAUTH2', displayName: 'OAuth2', isEnabled: false, requiredFields: [] },
    ]);
    const enabled = await CredentialTypeService.getEnabledCredentialTypes();
    expect(enabled.map(e => e.code)).toEqual(['BASIC_AUTH']);
  });
});
