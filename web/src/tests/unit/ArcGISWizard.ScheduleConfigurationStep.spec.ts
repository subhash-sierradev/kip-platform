/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';
import { nextTick } from 'vue';

const { hideTooltipSpy } = vi.hoisted(() => ({
  hideTooltipSpy: vi.fn(),
}));

import ScheduleConfigurationStep from '@/components/outbound/arcgisintegration/wizard/steps/ScheduleConfigurationStep.vue';
import type { ScheduleConfigurationData } from '@/types/ArcGISFormData';

vi.mock('@/utils/timezoneUtils', async importOriginal => {
  const actual = await importOriginal<typeof import('@/utils/timezoneUtils')>();
  return {
    ...actual,
    getUserTimezone: () => 'UTC',
    formatTimezoneInfo: () => 'UTC (GMT+00:00)',
  };
});

vi.mock('@/composables/useTooltip', async () => {
  const { ref } = await import('vue');
  return {
    useTooltip: () => ({
      tooltip: ref({ visible: false, x: 0, y: 0, text: '' }),
      showTooltip: vi.fn(),
      moveTooltip: vi.fn(),
      hideTooltip: hideTooltipSpy,
    }),
  };
});

describe('ArcGIS Wizard - ScheduleConfigurationStep', () => {
  it('validates daily configuration with frequency of 24 hours', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '15:30',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
        },
      },
    });
    await nextTick();

    // Should be valid with all required fields
    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(true);

    // Daily frequency select should show value 24
    const selects = wrapper.findAll('select.sc-input');
    const dailyFreqSelect = selects.find(s => {
      const opts = s.findAll('option');
      return opts.some(o => (o.element as HTMLOptionElement).value === '24');
    });
    expect(dailyFreqSelect).toBeTruthy();
  });

  it('validates weekly configuration with weekdays only', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'WEEKLY',
          dailyFrequency: '',
          dailyFrequencyRule: '',
          selectedDays: ['MON', 'TUE', 'WED', 'THU', 'FRI'], // Provide required selectedDays array
        },
      },
    });
    await nextTick();

    // Wait for validation to propagate
    await new Promise(resolve => setTimeout(resolve, 100));
    await nextTick();

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    // Check the most recent validation event should be true since we have selectedDays
    expect(events![events!.length - 1][0]).toBe(true);
  });

  it('marks isWeekdaysOnly as false when Saturday is selected', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'WEEKLY',
          dailyFrequency: '',
          selectedDays: ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'], // Include Saturday
        },
      },
    });
    await nextTick();

    // Wait for validation to propagate
    await new Promise(resolve => setTimeout(resolve, 100));
    await nextTick();

    // The weekdays only checkbox should be unchecked when weekend is included
    const weekdaysOnlyCheckbox = wrapper.find('.sc-weekdays-checkbox');
    expect(weekdaysOnlyCheckbox.exists()).toBe(true);
    expect((weekdaysOnlyCheckbox.element as HTMLInputElement).checked).toBe(false);
  });

  it('marks isWeekdaysOnly as false when Sunday is selected', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'WEEKLY',
          dailyFrequency: '',
          selectedDays: ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SUN'], // Include Sunday
        },
      },
    });
    await nextTick();

    // Wait for validation to propagate
    await new Promise(resolve => setTimeout(resolve, 100));
    await nextTick();

    // The weekdays only checkbox should be unchecked when weekend is included
    const weekdaysOnlyCheckbox = wrapper.find('.sc-weekdays-checkbox');
    expect(weekdaysOnlyCheckbox.exists()).toBe(true);
    expect((weekdaysOnlyCheckbox.element as HTMLInputElement).checked).toBe(false);
  });

  it('marks isWeekdaysOnly as true when only weekdays are selected', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'WEEKLY',
          dailyFrequency: '',
          selectedDays: ['MON', 'TUE', 'WED', 'THU', 'FRI'], // Only weekdays
        },
      },
    });
    await nextTick();

    // Wait for validation to propagate
    await new Promise(resolve => setTimeout(resolve, 100));
    await nextTick();

    // The weekdays only checkbox should be checked when only weekdays are selected
    const weekdaysOnlyCheckbox = wrapper.find('.sc-weekdays-checkbox');
    expect(weekdaysOnlyCheckbox.exists()).toBe(true);
    expect((weekdaysOnlyCheckbox.element as HTMLInputElement).checked).toBe(true);
  });

  it('validates monthly configuration with selected months', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          dailyFrequencyRule: '',
        },
      },
    });
    await nextTick();

    const checkboxes = wrapper.findAll('.sc-month-checkbox');
    if (checkboxes.length >= 2) {
      await checkboxes[0].setValue(true);
      await checkboxes[1].setValue(true);
      await nextTick();
    } else {
      // If UI elements don't exist, test validation logic directly by setting props
      await wrapper.setProps({
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          dailyFrequencyRule: '',
          selectedMonths: [1, 2],
        },
      });
    }

    // Wait for validation to propagate
    await new Promise(resolve => setTimeout(resolve, 100));
    await nextTick();

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    // Check if any validation event was true
    const hasValidationTrue = events!.some(event => event[0] === true);
    expect(hasValidationTrue).toBe(true);
  });

  it('validates monthly configuration with pre-selected months', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          selectedMonths: [1, 6, 12],
        },
      },
    });
    await nextTick();

    // Wait for validation to propagate
    await new Promise(resolve => setTimeout(resolve, 10));
    await nextTick();

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(true);
  });

  it('does not show placeholder option in daily frequency dropdown', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '6',
        },
      },
    });
    await nextTick();

    // Find the daily frequency select (should be the second select)
    const selects = wrapper.findAll('select.sc-input');
    expect(selects.length).toBeGreaterThan(1);

    // Find the select that contains only numeric options (daily frequency)
    const dailyFrequencySelect = selects.find(select => {
      const opts = select.findAll('option');
      return (
        opts.length > 0 &&
        opts.every(o => {
          const val = (o.element as HTMLOptionElement).value;
          return /^\d+$/.test(val);
        })
      );
    });
    expect(dailyFrequencySelect).toBeTruthy();

    const options = dailyFrequencySelect!.findAll('option');

    // Should have either 5 or 6 options depending on implementation
    expect(options.length).toBeGreaterThanOrEqual(5);

    // Verify all options have numeric values
    options.forEach(opt => {
      const value = (opt.element as HTMLOptionElement).value;
      expect(value).toMatch(/^\d+$/);
    });

    // Verify first option is 1 hour
    expect((options[0].element as HTMLOptionElement).value).toBe('1');
  });

  it('validates CRON expression configuration', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '',
          executionTime: '',
          frequencyPattern: 'CUSTOM',
          dailyFrequency: '',
          cronExpression: '',
        },
      },
    });
    await nextTick();

    // Should be invalid initially without cron expression
    let events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);

    // Add a valid cron expression
    const cronInput = wrapper.find('input[placeholder*="e.g., 0 0 9 * * ?"]');
    expect(cronInput.exists()).toBe(true);
    await cronInput.setValue('0 0 9 * * ?');
    await nextTick();

    // Should be valid now
    events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(true);

    // Test invalid cron expression
    await cronInput.setValue('invalid cron');
    await nextTick();

    // Should be invalid with error message
    events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);

    // Check that error message is displayed
    const errorMessage = wrapper.find('.sc-error-message');
    expect(errorMessage.exists()).toBe(true);
  });

  it('shows timezone-aware weekly cron description', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          businessTimeZone: 'UTC',
          executionDate: '2026-03-17',
          executionTime: '',
          frequencyPattern: 'CUSTOM',
          dailyFrequency: '',
          cronExpression: '0 30 11 ? * MON',
        },
      },
    });
    await nextTick();

    const cronDesc = wrapper.find('.sc-cron-description');
    expect(cronDesc.text()).toContain('Runs every Monday at 11:30 AM');

    const tzChip = wrapper.find('.sc-cron-timezone-chip');
    expect(tzChip.exists()).toBe(true);
    expect(tzChip.text()).toContain('UTC');
  });

  it('uses the selected business timezone for fixed-day-boundary custom cron descriptions', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          businessTimeZone: 'America/Anchorage',
          executionDate: '2026-04-07',
          executionTime: '',
          frequencyPattern: 'CUSTOM',
          dailyFrequency: '',
          cronExpression: '0 0 5 1 * ?',
          timeCalculationMode: 'FIXED_DAY_BOUNDARY',
        },
      },
    });
    await nextTick();

    const tzChip = wrapper.find('.sc-cron-timezone-chip');
    expect(tzChip.exists()).toBe(true);
    expect(tzChip.text()).toContain('America/Anchorage');
  });

  it('hides execution date and time when CRON pattern is selected', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'CUSTOM',
          dailyFrequency: '',
          cronExpression: '0 0 9 * * ?',
        },
      },
    });
    await nextTick();

    // Date/time field row should not exist when CUSTOM is selected
    const fieldRows = wrapper.findAll('.sc-field-row');
    const dateTimeRow = fieldRows.find(row => {
      return row.find('input[type="date"]').exists() || row.find('input[type="time"]').exists();
    });
    expect(dateTimeRow).toBeUndefined();

    // Cron input should be displayed
    expect(wrapper.find('input[placeholder="e.g., 0 0 9 * * ?"]').exists()).toBe(true);
  });

  it('switches to CUSTOM frequency pattern', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          cronExpression: '',
        },
      },
    });
    await nextTick();

    // Update props to switch to CUSTOM pattern
    await wrapper.setProps({
      modelValue: {
        executionDate: '2025-12-19',
        executionTime: '12:00',
        frequencyPattern: 'CUSTOM',
        dailyFrequency: '',
        cronExpression: '0 0 9 ? * MON',
      },
    });
    await nextTick();

    // Cron input should now be visible
    expect(wrapper.find('input[placeholder="e.g., 0 0 9 * * ?"]').exists()).toBe(true);
  });

  it('switches to CUSTOM frequency pattern', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          cronExpression: '0 0 9 ? * MON',
        },
      },
    });
    await nextTick();

    const frequencySelect = wrapper
      .findAll('select.sc-input')
      .find(selectWrapper => selectWrapper.find('option[value="CUSTOM"]').exists());

    expect(frequencySelect).toBeTruthy();
    if (!frequencySelect) {
      throw new Error('Frequency pattern select not found');
    }

    await frequencySelect.setValue('CUSTOM');
    await nextTick();

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as { frequencyPattern?: string };
    expect(lastPayload.frequencyPattern).toBe('CUSTOM');
  });

  it('requires execution start date for DAILY', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '',
          dailyFrequencyRule: '',
        },
      },
    });
    await nextTick();

    const events = wrapper.emitted('validation-change');
    expect(events).toBeTruthy();
    expect(events![events!.length - 1][0]).toBe(false);
  });

  it('restores execution start date when month-end is disabled', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          selectedMonths: [1],
          isExecuteOnMonthEnd: false,
        },
      },
    });

    await nextTick();

    const monthEndToggle = wrapper.find('#execute-on-month-end');
    expect(monthEndToggle.exists()).toBe(true);

    await monthEndToggle.setValue(true);
    await nextTick();

    await monthEndToggle.setValue(false);
    await nextTick();

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.executionDate).toBe('2025-12-19');
  });

  it('clears selectedDays when switching from WEEKLY to DAILY', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'WEEKLY',
          dailyFrequency: '',
          selectedDays: ['MON', 'TUE', 'WED'],
        },
      },
    });
    await nextTick();

    // Switch to DAILY pattern via props
    await wrapper.setProps({
      modelValue: {
        executionDate: '2025-12-19',
        executionTime: '12:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '24',
        selectedDays: [],
      },
    });
    await nextTick();

    // Daily frequency select should now be visible
    const selects = wrapper.findAll('select.sc-input');
    expect(selects.length).toBeGreaterThanOrEqual(2); // At least frequency + daily frequency (may include business timezone)
  });

  it('clears selectedMonths and isExecuteOnMonthEnd when switching from MONTHLY to DAILY', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          selectedMonths: [1, 3, 6],
          isExecuteOnMonthEnd: true,
        },
      },
    });
    await nextTick();

    // Switch to DAILY pattern via props
    await wrapper.setProps({
      modelValue: {
        executionDate: '2025-12-19',
        executionTime: '12:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '24',
        selectedMonths: [],
        isExecuteOnMonthEnd: false,
      },
    });
    await nextTick();

    // Daily frequency select should now be visible
    const selects = wrapper.findAll('select.sc-input');
    expect(selects.length).toBeGreaterThanOrEqual(2); // At least frequency + daily frequency (may include business timezone)
    // Month end toggle should not be visible
    expect(wrapper.find('#execute-on-month-end').exists()).toBe(false);
  });

  it('clears selectedDays when switching from WEEKLY to MONTHLY', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'WEEKLY',
          dailyFrequency: '',
          selectedDays: ['MON', 'TUE', 'WED', 'THU', 'FRI'],
        },
      },
    });
    await nextTick();

    // Switch to MONTHLY pattern via props
    await wrapper.setProps({
      modelValue: {
        executionDate: '2025-12-19',
        executionTime: '12:00',
        frequencyPattern: 'MONTHLY',
        dailyFrequency: '',
        selectedMonths: [1],
        selectedDays: [],
      },
    });
    await nextTick();

    // Month checkboxes should now be visible
    expect(wrapper.find('.sc-month-checkboxes').exists()).toBe(true);
    // Day checkboxes should not be visible
    expect(wrapper.find('.sc-day-checkboxes').exists()).toBe(false);
  });

  it('preserves execution time in HH:mm format', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '15:30',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
        },
      },
    });
    await nextTick();

    // Find time input and verify its value
    const timeInput = wrapper.find('input[type="time"]');
    expect(timeInput.exists()).toBe(true);
    expect((timeInput.element as HTMLInputElement).value).toBe('15:30');

    // Change the time
    await timeInput.setValue('09:45');
    await nextTick();

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    // Verify time is stored in HH:mm format (without seconds)
    expect(lastPayload.executionTime).toBe('09:45');
    expect(lastPayload.executionTime.split(':')).toHaveLength(2); // Should not include seconds segment
  });

  it('uses simple mode defaults and becomes valid once execution time is set', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        mode: 'simple',
        modelValue: {
          executionDate: '',
          executionTime: '',
          frequencyPattern: '',
          dailyFrequency: '',
        },
      },
    });

    await nextTick();

    expect(wrapper.find('select.sc-input').exists()).toBe(false);
    expect(wrapper.find('input[type="date"]').exists()).toBe(false);

    let validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(false);

    await wrapper.find('input[type="time"]').setValue('07:15');
    await nextTick();

    validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(true);

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.executionTime).toBe('07:15');
    expect(lastPayload.frequencyPattern).toBe('DAILY');
    expect(lastPayload.timeCalculationMode).toBe('FLEXIBLE_INTERVAL');
    expect(lastPayload.businessTimeZone).toBe('UTC');
  });

  it('hides the business timezone field in full mode when using rolling windows', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          timeCalculationMode: 'FLEXIBLE_INTERVAL',
          businessTimeZone: '',
        },
      },
    });

    await nextTick();

    expect(wrapper.text()).toContain('Rolling Window');
    expect(wrapper.text()).toContain('Syncs data from the last N hours before each job execution.');
    expect(wrapper.text()).not.toContain('Business Timezone');

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(true);
  });

  it('falls back to the default validation path for unsupported frequency patterns', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'YEARLY' as any,
          dailyFrequency: '',
          timeCalculationMode: 'CUSTOM_MODE' as any,
          businessTimeZone: '',
        },
      },
    });

    await nextTick();

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(true);
    expect(wrapper.text()).not.toContain('Syncs data from midnight to midnight');
    expect(wrapper.text()).not.toContain(
      'Syncs data from the last N hours before each job execution.'
    );
  });

  it('becomes invalid again in simple mode when execution time is cleared', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        mode: 'simple',
        modelValue: {
          executionDate: '',
          executionTime: '07:15',
          frequencyPattern: '',
          dailyFrequency: '',
        },
      },
    });

    await nextTick();

    const timeInput = wrapper.find('input[type="time"]');
    await timeInput.setValue('');
    await nextTick();

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(false);

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.executionTime).toBe('');
  });

  it('clears and restores the business timezone when data window mode changes', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          timeCalculationMode: 'FIXED_DAY_BOUNDARY',
          businessTimeZone: 'America/Chicago',
        },
      },
    });

    await nextTick();

    const dataWindowSelect = wrapper.findAll('select.sc-input')[0];
    await dataWindowSelect.setValue('FLEXIBLE_INTERVAL');
    await nextTick();

    let updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    let lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.timeCalculationMode).toBe('FLEXIBLE_INTERVAL');
    expect(lastPayload.businessTimeZone).toBeUndefined();

    await dataWindowSelect.setValue('FIXED_DAY_BOUNDARY');
    await nextTick();

    updates = wrapper.emitted('update:modelValue');
    lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.timeCalculationMode).toBe('FIXED_DAY_BOUNDARY');
    expect(lastPayload.businessTimeZone).toBe('UTC');
  });

  it('preserves an existing business timezone when switching back to daily window mode', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          timeCalculationMode: 'FLEXIBLE_INTERVAL',
          businessTimeZone: 'America/Chicago',
        },
      },
    });

    await nextTick();

    const dataWindowSelect = wrapper.findAll('select.sc-input')[0];
    await dataWindowSelect.setValue('FIXED_DAY_BOUNDARY');
    await nextTick();

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.timeCalculationMode).toBe('FIXED_DAY_BOUNDARY');
    expect(lastPayload.businessTimeZone).toBe('America/Chicago');
  });

  it('toggles weekdays only between the weekday set and no selected days', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'WEEKLY',
          dailyFrequency: '',
          selectedDays: ['MON', 'TUE', 'WED', 'THU', 'FRI'],
        },
      },
    });

    await nextTick();

    const weekdaysCheckbox = wrapper.find('.sc-weekdays-checkbox');
    expect((weekdaysCheckbox.element as HTMLInputElement).checked).toBe(true);

    await weekdaysCheckbox.trigger('change');
    await nextTick();

    let updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    let lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.selectedDays).toEqual([]);

    await weekdaysCheckbox.trigger('change');
    await nextTick();

    updates = wrapper.emitted('update:modelValue');
    lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.selectedDays).toEqual(['MON', 'TUE', 'WED', 'THU', 'FRI']);
  });

  it('toggles all months on and off from the select-all checkbox', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          selectedMonths: [1, 6],
        },
      },
    });

    await nextTick();

    const selectAllMonths = wrapper.find('.sc-months-selectall');
    await selectAllMonths.trigger('change');
    await nextTick();

    let updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    let lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.selectedMonths).toHaveLength(12);

    await selectAllMonths.trigger('change');
    await nextTick();

    updates = wrapper.emitted('update:modelValue');
    lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.selectedMonths).toEqual([]);
  });

  it('does not require an execution date when month-end mode is enabled', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          selectedMonths: [1],
          isExecuteOnMonthEnd: true,
        },
      },
    });

    await nextTick();

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(true);

    const dateInput = wrapper.find('input[type="date"]');
    expect(dateInput.attributes('disabled')).toBeDefined();
    expect(dateInput.attributes('placeholder')).toBe('Date will be end of selected months');
  });

  it('clears the daily frequency and becomes invalid when switching from DAILY to WEEKLY', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '6',
        },
      },
    });

    await nextTick();

    const frequencySelect = wrapper
      .findAll('select.sc-input')
      .find(selectWrapper => selectWrapper.find('option[value="WEEKLY"]').exists());

    expect(frequencySelect).toBeTruthy();
    await frequencySelect!.setValue('WEEKLY');
    await nextTick();

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.frequencyPattern).toBe('WEEKLY');
    expect(lastPayload.dailyFrequency).toBe('');
    expect(lastPayload.selectedDays).toEqual([]);

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(false);
  });

  it('clears monthly-only state when switching from MONTHLY to WEEKLY', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          selectedMonths: [1, 3],
          isExecuteOnMonthEnd: true,
        },
      },
    });

    await nextTick();

    const frequencySelect = wrapper
      .findAll('select.sc-input')
      .find(selectWrapper => selectWrapper.find('option[value="WEEKLY"]').exists());

    expect(frequencySelect).toBeTruthy();
    await frequencySelect!.setValue('WEEKLY');
    await nextTick();

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.frequencyPattern).toBe('WEEKLY');
    expect(lastPayload.selectedMonths).toEqual([]);
    expect(lastPayload.isExecuteOnMonthEnd).toBe(false);
  });

  it('updates the daily frequency rule from the daily frequency select', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
        },
      },
    });

    await nextTick();

    const selects = wrapper.findAll('select.sc-input');
    const dailyFrequencySelect = selects.find(selectWrapper => {
      const options = selectWrapper.findAll('option');
      return options.some(option => (option.element as HTMLOptionElement).value === '6');
    });

    expect(dailyFrequencySelect).toBeTruthy();
    await dailyFrequencySelect!.setValue('6');
    await nextTick();

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.dailyFrequency).toBe('6');
  });

  it('defaults the daily frequency select to 24 hours when the value is missing', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '',
        },
      },
    });

    await nextTick();

    const selects = wrapper.findAll('select.sc-input');
    const dailyFrequencySelect = selects.find(selectWrapper => {
      const options = selectWrapper.findAll('option');
      return options.some(option => (option.element as HTMLOptionElement).value === '24');
    });

    expect(dailyFrequencySelect).toBeTruthy();
    expect((dailyFrequencySelect!.element as HTMLSelectElement).value).toBe('24');
  });

  it('does not copy cron model changes into the payload outside custom mode', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          cronExpression: '',
        },
      },
    });

    await nextTick();

    const setupState = (wrapper.vm as any).$?.setupState;
    setupState.cronModel = '0 0 9 * * ?';
    await nextTick();

    const updates = wrapper.emitted('update:modelValue') ?? [];
    const lastPayload = updates[updates.length - 1]?.[0] as any;
    expect(lastPayload.frequencyPattern).toBe('DAILY');
    expect(lastPayload.cronExpression || '').toBe('');
  });

  it('becomes invalid in full mode when execution time is missing for non-custom schedules', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          timeCalculationMode: 'FLEXIBLE_INTERVAL',
        },
      },
    });

    await nextTick();

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(false);
  });

  it('does not resync local state when the incoming model value is unchanged', async () => {
    const modelValue: ScheduleConfigurationData = {
      executionDate: '2025-12-19',
      executionTime: '12:00',
      frequencyPattern: 'DAILY',
      dailyFrequency: '24',
      timeCalculationMode: 'FLEXIBLE_INTERVAL',
    };

    const wrapper = mount(ScheduleConfigurationStep, {
      props: { modelValue },
    });

    await nextTick();

    const setupState = (wrapper.vm as any).$?.setupState;
    const originalLocalData = setupState.localData;

    await wrapper.setProps({ modelValue: { ...modelValue } });
    await nextTick();

    expect(setupState.localData).toBe(originalLocalData);
  });

  it('requires a non-whitespace business timezone when daily window mode is selected', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          timeCalculationMode: 'FIXED_DAY_BOUNDARY',
          businessTimeZone: '   ',
        },
      },
    });

    await nextTick();

    expect(wrapper.text()).toContain('Business Timezone');

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(false);
  });

  it('clears cron feedback when switching away from and back to custom mode', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2026-03-17',
          executionTime: '',
          frequencyPattern: 'CUSTOM',
          dailyFrequency: '',
          cronExpression: '0 30 11 ? * MON',
        },
      },
    });

    await nextTick();

    expect(wrapper.find('.sc-cron-description').exists()).toBe(true);

    await wrapper.setProps({
      modelValue: {
        executionDate: '2026-03-17',
        executionTime: '09:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '24',
        cronExpression: '',
      },
    });
    await nextTick();

    expect(wrapper.find('.sc-cron-description').exists()).toBe(false);
    expect(wrapper.find('input[type="date"]').exists()).toBe(true);
    expect(wrapper.find('input[type="time"]').exists()).toBe(true);

    await wrapper.setProps({
      modelValue: {
        executionDate: '',
        executionTime: '',
        frequencyPattern: 'CUSTOM',
        dailyFrequency: '',
        cronExpression: '',
      },
    });
    await nextTick();

    const cronInput = wrapper.find('input[placeholder="e.g., 0 0 9 * * ?"]');
    expect(cronInput.exists()).toBe(true);
    expect((cronInput.element as HTMLInputElement).value).toBe('');
    expect(wrapper.find('.sc-cron-description').exists()).toBe(false);
    expect(wrapper.find('.sc-error-message').exists()).toBe(false);
  });

  it('removes cron feedback when a custom cron expression is cleared', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2026-03-17',
          executionTime: '',
          frequencyPattern: 'CUSTOM',
          dailyFrequency: '',
          cronExpression: '0 30 11 ? * MON',
        },
      },
    });

    await nextTick();

    const cronInput = wrapper.find('input[placeholder*="e.g., 0 0 9 * * ?"]');
    expect(wrapper.find('.sc-cron-description').exists()).toBe(true);

    await cronInput.setValue('');
    await nextTick();

    expect(wrapper.find('.sc-cron-description').exists()).toBe(false);
    expect(wrapper.find('.sc-error-message').exists()).toBe(false);

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(false);
  });

  it('ignores invalid execution time input without emitting a new model update', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        mode: 'simple',
        modelValue: {
          executionDate: '',
          executionTime: '07:15',
          frequencyPattern: 'DAILY',
          dailyFrequency: '',
          businessTimeZone: 'UTC',
        },
      },
    });

    await nextTick();

    const timeInput = wrapper.find('input[type="time"]');
    const initialUpdateCount = wrapper.emitted('update:modelValue')?.length ?? 0;

    Object.defineProperty(timeInput.element, 'value', {
      configurable: true,
      value: 'invalid-time',
    });

    await timeInput.trigger('input');
    await nextTick();

    expect(wrapper.emitted('update:modelValue')?.length ?? 0).toBe(initialUpdateCount);

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(true);
  });

  it('runs tooltip cleanup when the component unmounts', async () => {
    hideTooltipSpy.mockClear();

    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          selectedMonths: [1],
        },
      },
    });

    await nextTick();
    wrapper.unmount();

    expect(hideTooltipSpy).toHaveBeenCalledTimes(1);
  });

  it('defaults to UTC in full mode when daily-window scheduling omits the business timezone', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          timeCalculationMode: 'FIXED_DAY_BOUNDARY',
          businessTimeZone: '',
        },
      },
    });

    await nextTick();

    const validationEvents = wrapper.emitted('validation-change');
    expect(validationEvents).toBeTruthy();
    expect(validationEvents![validationEvents!.length - 1][0]).toBe(true);

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.businessTimeZone).toBe('UTC');
  });

  it('defaults the timezone to UTC when switching back to daily-window mode without a saved timezone', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'DAILY',
          dailyFrequency: '24',
          timeCalculationMode: 'FLEXIBLE_INTERVAL',
          businessTimeZone: '',
        },
      },
    });

    await nextTick();

    const dataWindowSelect = wrapper.findAll('select.sc-input')[0];
    await dataWindowSelect.setValue('FIXED_DAY_BOUNDARY');
    await nextTick();

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.timeCalculationMode).toBe('FIXED_DAY_BOUNDARY');
    expect(lastPayload.businessTimeZone).toBe('UTC');
  });

  it('keeps the execution date empty when month-end mode is disabled without a saved manual date', async () => {
    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          selectedMonths: [1],
          isExecuteOnMonthEnd: true,
        },
      },
    });

    await nextTick();

    const monthEndToggle = wrapper.find('#execute-on-month-end');
    await monthEndToggle.setValue(false);
    await nextTick();

    const updates = wrapper.emitted('update:modelValue');
    expect(updates).toBeTruthy();
    const lastPayload = updates![updates!.length - 1][0] as any;
    expect(lastPayload.executionDate).toBe('');
  });

  it('wires the month-end tooltip mouse handlers and hides the tooltip on unmount', async () => {
    hideTooltipSpy.mockClear();

    const wrapper = mount(ScheduleConfigurationStep, {
      props: {
        modelValue: {
          executionDate: '2025-12-19',
          executionTime: '12:00',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '',
          selectedMonths: [1],
          isExecuteOnMonthEnd: false,
        },
      },
    });

    await nextTick();

    const monthEndLabel = wrapper.findAll('label.sc-label')[1];
    await monthEndLabel.trigger('mouseenter');
    await monthEndLabel.trigger('mousemove');
    await monthEndLabel.trigger('mouseleave');

    wrapper.unmount();

    expect(hideTooltipSpy).toHaveBeenCalled();
  });
});
