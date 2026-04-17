import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockFormatScheduleByPattern = vi.fn();
const mockFormatScheduleTime = vi.fn();
const mockGetLocalDateString = vi.fn();
const mockGetScheduleTextForCustomPattern = vi.fn();

vi.mock('@/utils/scheduleFormatUtils', () => ({
  formatScheduleByPattern: (...args: unknown[]) => mockFormatScheduleByPattern(...args),
  formatTime: (...args: unknown[]) => mockFormatScheduleTime(...args),
}));

vi.mock('@/utils/dateUtils', () => ({
  formatMetadataDate: () => 'created-date',
  formatDate: (value: string) => `formatted:${value}`,
}));

vi.mock('@/utils/timezoneUtils', async () => {
  const actual =
    await vi.importActual<typeof import('@/utils/timezoneUtils')>('@/utils/timezoneUtils');
  return {
    ...actual,
    getUserTimezone: () => 'UTC',
    getLocalDateString: () => mockGetLocalDateString(),
  };
});

vi.mock('@/utils/cronTextFormatter', () => ({
  getScheduleTextForCustomPattern: (...args: unknown[]) =>
    mockGetScheduleTextForCustomPattern(...args),
}));

import type { ArcGISIntegrationSummaryResponse } from '@/api/models/ArcGISIntegrationSummaryResponse';
import ArcGISIntegrationCard from '@/components/outbound/arcgisintegration/ArcGISIntegrationCard.vue';

function makeIntegration(
  overrides: Partial<ArcGISIntegrationSummaryResponse> = {}
): ArcGISIntegrationSummaryResponse {
  return {
    id: 'int-1',
    name: 'Branch Test Integration',
    createdDate: '2026-01-01T12:00:00Z',
    itemSubtype: 'ArcGIS',
    itemSubtypeLabel: 'ArcGIS',
    itemType: 'DOCUMENT',
    dynamicDocumentType: '',
    dynamicDocumentTypeLabel: null,
    isEnabled: true,
    frequencyPattern: 'DAILY',
    executionDate: '2026-01-20',
    executionTime: '09:00:00',
    dailyExecutionInterval: 24,
    cronExpression: undefined,
    nextRunAtUtc: '2026-01-21T09:00:00Z',
    ...overrides,
  } as ArcGISIntegrationSummaryResponse;
}

describe('ArcGISIntegrationCard branch coverage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockFormatScheduleByPattern.mockReturnValue('formatted schedule');
    mockFormatScheduleTime.mockReturnValue('formatted fallback time');
    mockGetLocalDateString.mockReturnValue('2026-02-14');
    mockGetScheduleTextForCustomPattern.mockReturnValue('custom cron schedule');
  });

  it('uses the custom cron formatter when the schedule pattern is CUSTOM', () => {
    const integration = makeIntegration({
      frequencyPattern: 'CUSTOM',
      cronExpression: '0 0 12 * * ?',
    });

    const wrapper = mount(ArcGISIntegrationCard, {
      props: { integration, menuItems: [] },
    });

    expect(wrapper.text()).toContain('custom cron schedule');
    expect(mockGetScheduleTextForCustomPattern).toHaveBeenCalledWith(integration);
    expect(mockFormatScheduleByPattern).not.toHaveBeenCalled();
  });

  it('falls back to the local date and default execution time when schedule fields are missing', () => {
    mount(ArcGISIntegrationCard, {
      props: {
        integration: makeIntegration({ executionDate: undefined, executionTime: undefined }),
        menuItems: [],
      },
    });

    expect(mockGetLocalDateString).toHaveBeenCalledTimes(1);
    expect(mockFormatScheduleByPattern).toHaveBeenCalledWith(
      'DAILY',
      expect.objectContaining({ frequencyPattern: 'DAILY' }),
      '9:00 AM'
    );
  });

  it('renders the disabled chip state when the integration is disabled', () => {
    const wrapper = mount(ArcGISIntegrationCard, {
      props: {
        integration: makeIntegration({ isEnabled: false }),
        menuItems: [],
      },
    });

    const chip = wrapper.find('.status-chip');
    expect(chip.text()).toBe('Disabled');
    expect(chip.classes()).toContain('status-disabled');
  });

  it('returns empty status presentation values when no execution status exists', () => {
    const wrapper = mount(ArcGISIntegrationCard, {
      props: {
        integration: makeIntegration({ lastStatus: undefined, execution: undefined } as any),
        menuItems: [],
      },
    });

    expect(wrapper.find('i.dx-icon-check').exists()).toBe(false);
    expect(wrapper.find('.custom-tooltip').exists()).toBe(false);
    expect(wrapper.text()).toContain('Never Run');
  });
});
