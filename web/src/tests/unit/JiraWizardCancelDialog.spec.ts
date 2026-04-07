import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import JiraWizardCancelDialog from '@/components/outbound/jirawebhooks/wizard/JiraWizardCancelDialog.vue';

describe('JiraWizardCancelDialog', () => {
  it('emits confirm and close', async () => {
    const wrapper = mount(JiraWizardCancelDialog, {
      props: { open: true },
    });

    await wrapper.find('.jw-btn-neutral').trigger('click');
    expect(wrapper.emitted('confirm')).toBeTruthy();

    await wrapper.find('.jw-btn-warning').trigger('click');
    expect(wrapper.emitted('close')).toBeTruthy();
  });
});
