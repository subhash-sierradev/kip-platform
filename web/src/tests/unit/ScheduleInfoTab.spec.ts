/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FrequencyPattern } from '@/api/models/FrequencyPattern';
import { nextTick } from 'vue';

let mockedTimezone = 'UTC';
const { formatDateMock } = vi.hoisted(() => ({
  formatDateMock: vi.fn(),
}));

vi.mock('@/utils/dateUtils', async () => {
  const actual = await vi.importActual<typeof import('@/utils/dateUtils')>('@/utils/dateUtils');
  formatDateMock.mockImplementation(actual.formatDate);

  return {
    ...actual,
    formatDate: (...args: Parameters<typeof actual.formatDate>) => formatDateMock(...args),
  };
});

// Module-level mock typed broadly to accept zero or one arg; default resolves to null
const getScheduleMock = vi.fn<(id?: string) => Promise<any>>(async () => null);
vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    getSchedule: (id?: string) => getScheduleMock(id),
  },
}));

// Import component AFTER mocks so it uses the mocked service
vi.mock('@/utils/timezoneUtils', async () => {
  const actual =
    await vi.importActual<typeof import('@/utils/timezoneUtils')>('@/utils/timezoneUtils');
  return {
    ...actual,
    getUserTimezone: () => mockedTimezone,
    formatTimezoneInfo: (tz: string) => `${tz} (IST/UTC style)`,
  };
});

import ScheduleInfoTab from '@/components/outbound/arcgisintegration/details/ScheduleInfoTab.vue';

describe('ScheduleInfoTab', () => {
  beforeEach(() => {
    mockedTimezone = 'UTC';
    formatDateMock.mockClear();
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
    expect(formatDateMock).toHaveBeenCalledWith('2026-03-21T11:00:00Z', {
      includeTime: true,
      includeWeekday: true,
      includeSeconds: false,
      includeTimezone: false,
      format: 'medium',
    });
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

  it('shows weekly labels and normalizes day strings', async () => {
    const wrapper = mount(ScheduleInfoTab, {
      props: {
        integrationData: {
          schedule: {
            frequencyPattern: FrequencyPattern.WEEKLY,
            daySchedule: ['MON', 'thursday', 'UNKNOWN'],
            isExecuteOnMonthEnd: false,
            executionDate: '2026-02-01',
            executionTime: '10:00',
          },
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await nextTick();
    expect(wrapper.text()).toContain('Weekly');
    expect(wrapper.text()).toContain('Executes on selected weekdays');
    expect(wrapper.text()).toContain('Monday');
    expect(wrapper.text()).toContain('Thursday');
    expect(wrapper.text()).toContain('Unknown');
  });

  it('parses comma-delimited day schedules and preserves custom window labels', async () => {
    const wrapper = mount(ScheduleInfoTab, {
      props: {
        integrationData: {
          schedule: {
            frequencyPattern: FrequencyPattern.WEEKLY,
            daySchedule: ' mon,FRIDAY , holiday ' as any,
            executionDate: '2026-02-01',
            executionTime: '10:00',
            timeCalculationMode: 'CUSTOM_WINDOW' as any,
            isExecuteOnMonthEnd: false,
          },
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await nextTick();
    expect(wrapper.text()).toContain('Monday');
    expect(wrapper.text()).toContain('Friday');
    expect(wrapper.text()).toContain('Holiday');
    expect(wrapper.text()).toContain('CUSTOM_WINDOW');
  });

  it('shows monthly labels, sorted months, and month-end flag', async () => {
    const wrapper = mount(ScheduleInfoTab, {
      props: {
        integrationData: {
          schedule: {
            frequencyPattern: FrequencyPattern.MONTHLY,
            monthSchedule: ['December', 'February'],
            isExecuteOnMonthEnd: true,
            executionDate: '2026-02-28',
            executionTime: '20:15',
          },
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await nextTick();
    const text = wrapper.text();
    expect(text).toContain('Monthly');
    expect(text).toContain('Executes on selected months');
    expect(text).toContain('Run on Last Day of Month:');
    expect(text).toContain('Enabled');
    expect(text.indexOf('February')).toBeLessThan(text.indexOf('December'));
  });

  it('parses comma-delimited month schedules before sorting them', async () => {
    const wrapper = mount(ScheduleInfoTab, {
      props: {
        integrationData: {
          schedule: {
            frequencyPattern: FrequencyPattern.MONTHLY,
            monthSchedule: ' December,March , January ' as any,
            executionDate: '2026-01-15',
            executionTime: '08:30',
            isExecuteOnMonthEnd: false,
          },
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await nextTick();
    const text = wrapper.text();
    expect(text.indexOf('January')).toBeLessThan(text.indexOf('March'));
    expect(text.indexOf('March')).toBeLessThan(text.indexOf('December'));
  });

  it('shows fixed day boundary labels and business timezone chip', async () => {
    const wrapper = mount(ScheduleInfoTab, {
      props: {
        integrationData: {
          schedule: {
            frequencyPattern: FrequencyPattern.DAILY,
            dailyExecutionInterval: 24,
            isExecuteOnMonthEnd: false,
            executionDate: '2026-02-10',
            executionTime: '00:30',
            timeCalculationMode: 'FIXED_DAY_BOUNDARY',
            businessTimeZone: 'America/Denver',
          },
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await nextTick();
    expect(wrapper.text()).toContain('Once daily');
    expect(wrapper.text()).toContain('Daily Window');
    expect(wrapper.text()).toContain('America/Denver');
  });

  it('shows rolling window label without a business timezone row', async () => {
    const wrapper = mount(ScheduleInfoTab, {
      props: {
        integrationData: {
          schedule: {
            frequencyPattern: FrequencyPattern.DAILY,
            dailyExecutionInterval: 3,
            isExecuteOnMonthEnd: false,
            executionDate: '2026-02-10',
            executionTime: '12:00',
            timeCalculationMode: 'FLEXIBLE_INTERVAL',
            businessTimeZone: 'America/New_York',
          },
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await nextTick();
    expect(wrapper.text()).toContain('Rolling Window');
    expect(wrapper.text()).not.toContain('Business Timezone:');
  });

  it('falls back for invalid execution time and missing schedule values', async () => {
    const wrapper = mount(ScheduleInfoTab, {
      props: {
        integrationData: {
          schedule: {
            frequencyPattern: 'SOMETHING_ELSE',
            executionDate: 'bad-date',
            executionTime: 'not-a-time',
          } as any,
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await nextTick();
    expect(wrapper.text()).toContain('Something_else');
    expect(wrapper.text()).toContain('not-a-time');
    expect(wrapper.text()).toContain('Invalid Date');
  });

  it('returns the raw execution time when timezone formatting throws', async () => {
    mockedTimezone = 'Invalid/Timezone';

    const wrapper = mount(ScheduleInfoTab, {
      props: {
        integrationData: {
          schedule: {
            frequencyPattern: FrequencyPattern.DAILY,
            executionDate: '2026-02-10',
            executionTime: '12:30',
            isExecuteOnMonthEnd: false,
          },
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await nextTick();
    expect(wrapper.text()).toContain('12:30');
  });

  it('renders not-set and not-configured fallbacks when schedule is sparse', async () => {
    const wrapper = mount(ScheduleInfoTab, {
      props: {
        integrationData: {
          schedule: {
            frequencyPattern: FrequencyPattern.DAILY,
            executionTime: '',
            executionDate: null,
            isExecuteOnMonthEnd: false,
            timeCalculationMode: undefined,
          },
        },
        integrationId: 'abc',
        loading: false,
      },
    });

    await nextTick();
    expect(wrapper.text()).toContain('Daily');
    expect(wrapper.text()).toContain('Not configured');
    expect(wrapper.text()).toContain('Not set');
  });
});
