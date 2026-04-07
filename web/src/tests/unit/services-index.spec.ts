import { describe, expect, it, vi } from 'vitest';

vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {},
}));

vi.mock('@/api/services/CredentialTypeService', () => ({
  CredentialTypeService: {},
}));

import * as services from '@/api/services';

describe('services index exports', () => {
  it('exports all service classes', () => {
    expect(services.ArcGISIntegrationService).toBeTruthy();
    expect(services.CredentialTypeService).toBeTruthy();
    expect(services.IntegrationConnectionService).toBeTruthy();
    expect(services.JiraIntegrationService).toBeTruthy();
    expect(services.JiraWebhookService).toBeTruthy();
    expect(services.SettingsService).toBeTruthy();
    expect(services.KwDocService).toBeTruthy();
  });
});
