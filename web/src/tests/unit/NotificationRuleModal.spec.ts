/* eslint-disable simple-import-sort/imports */
import { describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { NotificationSeverity } from '@/api/models/NotificationSeverity';

vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    props: ['text', 'disabled'],
    emits: ['click'],
    template:
      '<button class="dx-button" :disabled="disabled" @click="$emit(\'click\')">{{ text }}</button>',
  },
}));

vi.mock('@/api', async () => {
  const actual = await vi.importActual<object>('@/api');
  return {
    ...actual,
    NotificationSeverity: {
      INFO: 'INFO',
      WARNING: 'WARNING',
      ERROR: 'ERROR',
      CRITICAL: 'CRITICAL',
    },
  };
});

import NotificationRuleModal from '@/components/admin/notifications/NotificationRuleModal.vue';

const AppModalStub = {
  props: ['open', 'title'],
  emits: ['update:open'],
  template: `
    <div>
      <div class="title">{{ title }}</div>
      <slot />
      <slot name="footer" />
      <button class="close-modal" @click="$emit('update:open', false)">close</button>
    </div>
  `,
};

describe('NotificationRuleModal', () => {
  function mountModal(overrides: Record<string, unknown> = {}) {
    return mount(NotificationRuleModal, {
      props: {
        show: true,
        isSaving: false,
        events: [
          { id: 'e1', eventKey: 'A', displayName: 'Event A', isEnabled: true },
          { id: 'e2', eventKey: 'B', displayName: 'Event B', isEnabled: false },
        ],
        rules: [{ id: 'r2', eventId: 'e2', severity: NotificationSeverity.INFO, eventKey: 'B' }],
        ...overrides,
      },
      global: {
        stubs: {
          AppModal: AppModalStub,
        },
      },
    });
  }

  it('creates a new rule in create mode', async () => {
    const wrapper = mountModal();

    const selects = wrapper.findAll('select');
    await selects[0].setValue('e1');
    await selects[1].setValue('ERROR');
    await wrapper.findAll('.dx-button')[1].trigger('click');

    expect(wrapper.emitted('save')).toBeTruthy();
    expect(wrapper.emitted('save')?.[0]?.[0]).toEqual({
      eventId: 'e1',
      severity: 'ERROR',
      isEnabled: true,
    });
  });

  it('emits update in edit mode and handles close event from modal', async () => {
    const wrapper = mountModal({
      existingRule: {
        id: 'r1',
        eventId: 'e1',
        eventKey: 'A',
        severity: NotificationSeverity.WARNING,
      },
    });

    const select = wrapper.find('select');
    await select.setValue('CRITICAL');
    await wrapper.findAll('.dx-button')[1].trigger('click');
    await wrapper.find('.close-modal').trigger('click');

    expect(wrapper.find('.form-readonly').text()).toContain('A');
    expect(wrapper.emitted('update')?.[0]).toEqual(['CRITICAL']);
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('filters active events to enabled and unused ids', () => {
    const wrapper = mountModal({
      events: [
        { id: 'e1', eventKey: 'A', displayName: 'Event A', isEnabled: true },
        { id: 'e2', eventKey: 'B', displayName: 'Event B', isEnabled: true },
        { id: 'e3', eventKey: 'C', displayName: 'Event C', isEnabled: false },
      ],
      rules: [{ id: 'r2', eventId: 'e2', severity: NotificationSeverity.INFO, eventKey: 'B' }],
    });

    const options = wrapper.findAll('select')[0].findAll('option');
    const optionValues = options.map(o => o.attributes('value'));
    expect(optionValues).toContain('e1');
    expect(optionValues).not.toContain('e2');
    expect(optionValues).not.toContain('e3');
  });

  it('resets create-mode form fields when the modal closes and reapplies edit severity on reopen', async () => {
    const wrapper = mountModal({
      existingRule: {
        id: 'r1',
        eventId: 'e1',
        eventKey: 'A',
        severity: NotificationSeverity.WARNING,
      },
    });

    const select = wrapper.find('select');
    await select.setValue('ERROR');
    await wrapper.setProps({ show: false });
    await wrapper.setProps({ show: true });

    expect((wrapper.find('select').element as HTMLSelectElement).value).toBe('WARNING');

    const createWrapper = mountModal();
    await createWrapper.findAll('select')[0].setValue('e1');
    await createWrapper.findAll('select')[1].setValue('ERROR');
    await createWrapper.setProps({ show: false });
    await createWrapper.setProps({ show: true });

    expect((createWrapper.findAll('select')[0].element as HTMLSelectElement).value).toBe('');
    expect((createWrapper.findAll('select')[1].element as HTMLSelectElement).value).toBe('');
  });
});
