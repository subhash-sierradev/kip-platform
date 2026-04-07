import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import InsertFieldModal from '@/components/outbound/jirawebhooks/wizard/InsertFieldModal.vue';

describe('InsertFieldModal', () => {
  it('does not render when closed', () => {
    const wrapper = mount(InsertFieldModal, {
      props: { open: false, jsonSample: '{}' },
    });

    expect(wrapper.find('.ms-modal').exists()).toBe(false);
  });

  it('lists fields, filters by search, and emits select and close', async () => {
    const wrapper = mount(InsertFieldModal, {
      props: {
        open: true,
        jsonSample: '{"a":{"b":1},"c":2}',
      },
    });

    const items = wrapper.findAll('.ms-field-item');
    expect(items.length).toBe(3);
    expect(wrapper.text()).toContain('{{a}}');
    expect(wrapper.text()).toContain('{{a.b}}');
    expect(wrapper.text()).toContain('{{c}}');

    await wrapper.find('input.ms-search-field').setValue('a.b');
    const filteredItems = wrapper.findAll('.ms-field-item');
    expect(filteredItems.length).toBe(1);
    expect(filteredItems[0].text()).toContain('{{a.b}}');

    await filteredItems[0].trigger('click');
    expect(wrapper.emitted('select')).toEqual([['{{a.b}}']]);

    await wrapper.find('.ms-btn-cancel').trigger('click');
    expect(wrapper.emitted('close')).toBeTruthy();
  });

  it('shows arrays using [] notation', async () => {
    const wrapper = mount(InsertFieldModal, {
      props: {
        open: true,
        jsonSample: '{"serials":[{"filingNumberDisplay":"A"}],"tags":["x"]}',
      },
    });

    const placeholders = wrapper.findAll('.ms-field-item-placeholder').map(item => item.text());

    expect(placeholders).toContain('{{serials[]}}');
    expect(placeholders).toContain('{{serials[].filingNumberDisplay}}');
    expect(placeholders).toContain('{{tags[]}}');
    expect(placeholders.some(p => p.includes('.0'))).toBe(false);
  });
});
