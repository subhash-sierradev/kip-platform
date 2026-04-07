import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import StepToolbar from '@/components/common/StepToolbar.vue';

describe('StepToolbar', () => {
  it('renders step info and slot actions; compact toggles class', () => {
    const wrapper = mount(StepToolbar, {
      props: { stepName: 'Connect', stepNumber: 2, subtitle: 'Optional', compact: true },
      slots: { default: '<button>Next</button>' },
    });

    expect(wrapper.find('.step-number').text()).toContain('2');
    expect(wrapper.find('.step-name').text()).toContain('Connect');
    expect(wrapper.find('.step-subtitle').text()).toContain('Optional');
    expect(wrapper.classes()).toContain('compact');
    expect(wrapper.find('.step-actions').text()).toContain('Next');
  });

  it('does not render subtitle element when subtitle prop is not provided', () => {
    const wrapper = mount(StepToolbar, {
      props: { stepName: 'Basic Details', stepNumber: 1, compact: false },
      slots: { default: '<button>Continue</button>' },
    });

    expect(wrapper.find('.step-number').text()).toContain('1');
    expect(wrapper.find('.step-name').text()).toContain('Basic Details');
    expect(wrapper.find('.step-subtitle').exists()).toBe(false);
    expect(wrapper.classes()).not.toContain('compact');
    expect(wrapper.find('.step-actions').text()).toContain('Continue');
  });

  it('handles string step numbers correctly', () => {
    const wrapper = mount(StepToolbar, {
      props: { stepName: 'Review', stepNumber: '3' },
    });

    expect(wrapper.find('.step-number').text()).toContain('3');
    expect(wrapper.find('.step-name').text()).toContain('Review');
  });
});
