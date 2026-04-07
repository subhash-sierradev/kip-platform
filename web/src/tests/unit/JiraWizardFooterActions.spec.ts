import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import JiraWizardFooterActions from '@/components/outbound/jirawebhooks/wizard/JiraWizardFooterActions.vue';

describe('JiraWizardFooterActions', () => {
  it('renders next button disabled and no previous on first step', () => {
    const wrapper = mount(JiraWizardFooterActions, {
      props: {
        activeStep: 0,
        totalSteps: 3,
        nextDisabled: true,
        submitDisabled: false,
        submitLabel: 'Create',
      },
    });

    expect(wrapper.find('.jw-footer-left .jw-btn-outlined').exists()).toBe(false);
    const nextButton = wrapper.find('.jw-footer-right .jw-btn-primary');
    expect(nextButton.attributes('disabled')).toBeDefined();
  });

  it('emits previous and cancel actions', async () => {
    const wrapper = mount(JiraWizardFooterActions, {
      props: {
        activeStep: 1,
        totalSteps: 3,
        nextDisabled: false,
        submitDisabled: false,
        submitLabel: 'Create',
      },
    });

    await wrapper.find('.jw-footer-left .jw-btn-outlined').trigger('click');
    expect(wrapper.emitted('previous')).toBeTruthy();

    await wrapper.find('.jw-btn-neutral').trigger('click');
    expect(wrapper.emitted('cancel')).toBeTruthy();
  });

  it('renders submit button on last step', () => {
    const wrapper = mount(JiraWizardFooterActions, {
      props: {
        activeStep: 2,
        totalSteps: 3,
        nextDisabled: false,
        submitDisabled: true,
        submitLabel: 'Create',
      },
    });

    const submitButton = wrapper.find('.jw-footer-right .jw-btn-primary');
    expect(submitButton.text()).toBe('Create');
    expect(submitButton.attributes('disabled')).toBeDefined();
  });
});
