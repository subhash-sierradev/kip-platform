import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import JiraWizardStepperHeader from '@/components/outbound/jirawebhooks/wizard/JiraWizardStepperHeader.vue';

describe('JiraWizardStepperHeader', () => {
  it('renders steps and active/completed classes', () => {
    const wrapper = mount(JiraWizardStepperHeader, {
      props: {
        steps: ['One', 'Two', 'Three'],
        activeStep: 1,
      },
    });

    const indicators = wrapper.findAll('.jw-step-indicator');
    expect(indicators.length).toBe(3);
    expect(indicators[0].classes()).toContain('jw-completed');
    expect(indicators[1].classes()).toContain('jw-active');
  });

  it('emits close on icon click', async () => {
    const wrapper = mount(JiraWizardStepperHeader, {
      props: {
        steps: ['One'],
        activeStep: 0,
      },
    });

    await wrapper.find('.jw-icon-button').trigger('click');
    expect(wrapper.emitted('close')).toBeTruthy();
  });
});
