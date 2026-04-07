import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ScheduleStep from '@/components/outbound/confluenceintegration/wizard/steps/ScheduleStep.vue';

const ScheduleConfigurationStepStub = {
  name: 'ScheduleConfigurationStep',
  props: ['modelValue', 'mode'],
  emits: ['update:modelValue', 'validation-change'],
  template: '<div class="schedule-config-step-stub" />',
};

describe('Confluence ScheduleStep', () => {
  it('passes modelValue and mode="full" to ScheduleConfigurationStep', () => {
    const modelValue = {
      executionTime: '10:00',
      frequencyPattern: 'DAILY',
      executionDate: null,
      selectedDays: [],
      selectedMonths: [],
      isExecuteOnMonthEnd: false,
      dailyFrequency: '24',
      cronExpression: undefined,
    } as any;

    const wrapper = mount(ScheduleStep, {
      props: { modelValue },
      global: {
        stubs: {
          ScheduleConfigurationStep: ScheduleConfigurationStepStub,
        },
      },
    });

    const child = wrapper.findComponent({ name: 'ScheduleConfigurationStep' });
    expect(child.exists()).toBe(true);
    expect((child.props() as any).mode).toBe('full');
    expect((child.props() as any).modelValue).toEqual(modelValue);
  });

  it('re-emits update:modelValue and validation-change from child', async () => {
    const wrapper = mount(ScheduleStep, {
      props: {
        modelValue: {
          executionTime: '08:30',
          frequencyPattern: 'DAILY',
          executionDate: null,
          selectedDays: [],
          selectedMonths: [],
          isExecuteOnMonthEnd: false,
          dailyFrequency: '24',
          cronExpression: undefined,
        } as any,
      },
      global: {
        stubs: {
          ScheduleConfigurationStep: ScheduleConfigurationStepStub,
        },
      },
    });

    const child = wrapper.findComponent({ name: 'ScheduleConfigurationStep' });
    child.vm.$emit('update:modelValue', { frequencyPattern: 'WEEKLY' });
    child.vm.$emit('validation-change', false);

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([{ frequencyPattern: 'WEEKLY' }]);
    expect(wrapper.emitted('validation-change')?.[0]).toEqual([false]);
  });
});
