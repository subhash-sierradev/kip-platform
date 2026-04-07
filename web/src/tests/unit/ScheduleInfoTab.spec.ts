/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FrequencyPattern } from '@/api/models/FrequencyPattern';
import { nextTick } from 'vue';

let mockedTimezone = 'UTC';

// Module-level mock typed broadly to accept zero or one arg; default resolves to null
const getScheduleMock = vi.fn<(id?: string) => Promise<any>>(async () => null);
vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    getSchedule: (id?: string) => getScheduleMock(id),
  },
}));

// Import component AFTER mocks so it uses the mocked service
vi.mock('@/utils/timezoneUtils', () => ({
  getUserTimezone: () => mockedTimezone,
  formatTimezoneInfo: (tz: string) => `${tz} (IST/UTC style)`,
}));

import ScheduleInfoTab from '@/components/outbound/arcgisintegration/details/ScheduleInfoTab.vue';

describe('ScheduleInfoTab', () => {
  beforeEach(() => {
    mockedTimezone = 'UTC';
  });

  it('shows loading state when loading', () => {
    const wrapper = mount(ScheduleInfoTab, { props: { integrationId: 'abc', loading: true } });
    expect(wrapper.find('.loading-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('Loading schedule information');
  });

  it('shows empty state when no schedule returned', async () => {
    getScheduleMock.mockReset();
    getScheduleMock.mockResolvedValueOnce(null);
    const wrapper = mount(ScheduleInfoTab, { props: { integrationId: 'abc', loading: false } });
    expect(wrapper.find('.empty-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('No Schedule Configured');
  });

  it('renders schedule details when provided by service', async () => {
    const integrationData = {
      schedule: {
        frequencyPattern: FrequencyPattern.DAILY,
        dailyExecutionInterval: 2,
        executionDate: '2025-12-19',
        executionTime: '15:00',
        businessTimeZone: 'UTC',
        cronExpression: '0 0 * * *',
      },
    } as any;
    const wrapper = mount(ScheduleInfoTab, {
      props: { integrationData, integrationId: 'abc', loading: false },
    });
    await nextTick();
    // Pattern label and value
    expect(wrapper.text()).toContain('Pattern:');
    expect(wrapper.text()).toContain('Daily');
    expect(wrapper.text()).toContain('Fri, Dec 19, 2025');
    // Execution time is formatted to 12-hour with AM/PM
    expect(wrapper.text()).toContain('3:00 PM');
    // Timezone displayed within Time & Zone section
    expect(wrapper.text()).toContain('Time & Zone:');
    expect(wrapper.text()).toContain('UTC (IST/UTC style)');
    expect(wrapper.text()).not.toContain('Cron:');
  });

  it('shows cron expression for cron expression pattern', async () => {
    const integrationData = {
      schedule: {
        frequencyPattern: FrequencyPattern.CRON_EXPRESSION,
        executionDate: '2026-01-06',
        executionTime: '03:49',
        businessTimeZone: 'UTC',
        cronExpression: '0 49 3 ? * SUN',
      },
    } as any;

    const wrapper = mount(ScheduleInfoTab, {
      props: { integrationData, integrationId: 'abc', loading: false },
    });
    await nextTick();

    expect(wrapper.text()).toContain('Cron:');
    expect(wrapper.text()).toContain('0 49 3 ? * SUN');
  });

  it('calculates next run correctly for monthly pattern with selected months', async () => {
    const integrationData = {
      nextRunAtUtc: '2026-03-21T11:00:00Z', // Backend-computed next run
      schedule: {
        frequencyPattern: FrequencyPattern.MONTHLY,
        executionDate: '2026-01-21', // Day 21 selected
        executionTime: '11:00',
        monthSchedule: ['March'], // March selected
        businessTimeZone: 'UTC',
        cronExpression: '0 0 11 21 3 ?',
      },
    } as any;

    const wrapper = mount(ScheduleInfoTab, {
      props: { integrationData, integrationId: 'abc', loading: false },
    });
    await nextTick();

    // Should show March in months list (this confirms the component is working)
    expect(wrapper.text()).toContain('Next Run:');
    expect(wrapper.text()).toContain('March'); // Should show March in months list
    expect(wrapper.text()).toContain('Executes on selected months'); // Should show monthly pattern description

    // Validate next run date/time from backend-provided nextRunAtUtc (Mar 21, 2026 11:00 AM UTC)
    expect(wrapper.text()).toContain('Mar 21, 2026');
    expect(wrapper.text()).toContain('11:00 AM');
  });

  it('uses next run date for execution time conversion to avoid DST mismatch', async () => {
    mockedTimezone = 'America/Chicago';

    const integrationData = {
      nextRunAtUtc: '2027-01-20T15:00:00Z',
      schedule: {
        frequencyPattern: FrequencyPattern.DAILY,
        executionDate: '2026-03-20',
        executionTime: '15:00',
        userTimezone: 'America/Chicago',
      },
    } as any;

    const wrapper = mount(ScheduleInfoTab, {
      props: { integrationData, integrationId: 'abc', loading: false },
    });
    await nextTick();

    // 15:00 UTC on Jan 20, 2027 is 9:00 AM in America/Chicago (CST)
    expect(wrapper.text()).toContain('9:00 AM');
  });
});
