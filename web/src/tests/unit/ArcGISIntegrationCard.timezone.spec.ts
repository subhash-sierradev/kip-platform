import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

import type { ArcGISIntegrationSummaryResponse } from '@/api/models/ArcGISIntegrationSummaryResponse';
import ArcGISIntegrationCard from '@/components/outbound/arcgisintegration/ArcGISIntegrationCard.vue';

// Mock user timezone to a deterministic value
vi.mock('@/utils/timezoneUtils', async () => {
  const actual = await vi.importActual<any>('@/utils/timezoneUtils');
  return { ...actual, getUserTimezone: () => 'Asia/Kolkata' };
});

function makeIntegration(
  overrides: Partial<ArcGISIntegrationSummaryResponse> = {}
): ArcGISIntegrationSummaryResponse {
  return {
    id: 'int-1',
    name: 'Timezone Test Integration',
    itemType: 'OUTBOUND',
    itemSubtype: 'ArcGIS',
    itemSubtypeLabel: 'ArcGIS',
    dynamicDocumentType: 'DOC',
    frequencyPattern: 'DAILY',
    dailyExecutionInterval: 24,
    executionDate: '2026-01-16',
    executionTime: '07:50:00',
    daySchedule: '',
    monthSchedule: '',
    isExecuteOnMonthEnd: false,
    cronExpression: '',
    createdDate: '2026-01-01T12:00:00Z',
    createdBy: 'tester',
    lastModifiedDate: '2026-01-10T12:00:00Z',
    lastModifiedBy: 'tester',
    isEnabled: true,
    ...overrides,
  } as ArcGISIntegrationSummaryResponse;
}

describe('ArcGISIntegrationCard timezone formatting', () => {
  it('converts schedule execution time from UTC to user timezone', () => {
    const integration = makeIntegration();
    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });
    const text = wrapper.text();
    // For Asia/Kolkata (UTC+5:30), 07:50 UTC -> 1:20 PM
    // Daily phrasing updated to: "Runs every {interval} hours starting at {time}"
    expect(text).toContain('Runs every 24 hours starting at 1:20 PM');
  });
});
