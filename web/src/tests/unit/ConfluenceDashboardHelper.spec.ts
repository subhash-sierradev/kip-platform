import { describe, expect, it } from 'vitest';

import type { ConfluenceIntegrationSummaryResponse } from '@/api/models/ConfluenceIntegrationSummaryResponse';
import {
  createConfluenceDialogConfig,
  getIntegrationMenuItems,
} from '@/components/outbound/confluenceintegration/utils/ConfluenceDashboardHelper';

function makeIntegration(
  overrides: Partial<ConfluenceIntegrationSummaryResponse> = {}
): ConfluenceIntegrationSummaryResponse {
  return {
    id: 'ci-1',
    name: 'Test Confluence',
    itemType: 'DOCUMENT',
    itemSubtype: 'DESIGN',
    languageCodes: ['en'],
    confluenceSpaceKey: 'TEST',
    reportNameTemplate: 'Report - {date}',
    frequencyPattern: 'DAILY',
    executionTime: '08:00',
    createdDate: '2026-01-01T00:00:00Z',
    createdBy: 'admin',
    lastModifiedDate: '2026-01-02T00:00:00Z',
    lastModifiedBy: 'admin',
    isEnabled: true,
    ...overrides,
  };
}

describe('ConfluenceDashboardHelper – getIntegrationMenuItems', () => {
  it('returns edit, clone, disable, delete items when integration is enabled', () => {
    const items = getIntegrationMenuItems(makeIntegration({ isEnabled: true }));
    const ids = items.map(i => i.id);
    expect(ids).toContain('edit');
    expect(ids).toContain('clone');
    expect(ids).toContain('disable');
    expect(ids).toContain('delete');
    expect(ids).not.toContain('enable');
  });

  it('returns enable item (not disable) when integration is disabled', () => {
    const items = getIntegrationMenuItems(makeIntegration({ isEnabled: false }));
    const ids = items.map(i => i.id);
    expect(ids).toContain('enable');
    expect(ids).not.toContain('disable');
  });

  it('returns exactly 4 menu items', () => {
    expect(getIntegrationMenuItems(makeIntegration())).toHaveLength(4);
  });
});

describe('ConfluenceDashboardHelper – createConfluenceDialogConfig', () => {
  it('returns config with enable, disable, and delete keys', () => {
    const config = createConfluenceDialogConfig();
    expect(config).toHaveProperty('enable');
    expect(config).toHaveProperty('disable');
    expect(config).toHaveProperty('delete');
  });

  it('each config entry has title, desc, and label', () => {
    const config = createConfluenceDialogConfig();
    for (const key of ['enable', 'disable', 'delete'] as const) {
      expect(config[key].title).toBeTruthy();
      expect(config[key].desc).toBeTruthy();
      expect(config[key].label).toBeTruthy();
    }
  });
});
