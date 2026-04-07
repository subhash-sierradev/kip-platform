/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect } from 'vitest';
import AuditLogFilters from '@/components/admin/audit/AuditLogFilters.vue';

const baseProps = {
  selectedEntityType: null,
  selectedUserId: null,
  selectedActivity: null,
  entityTypes: [
    { value: 'JIRA_WEBHOOK', label: 'Jira Webhook' },
    { value: 'INTEGRATION', label: 'Integration' },
  ],
  userIds: [
    { value: 'u1', label: 'User 1' },
    { value: 'u2', label: 'User 2' },
  ],
  activities: [
    { value: 'CREATE', label: 'Create' },
    { value: 'UPDATE', label: 'Update' },
  ],
};

describe('AuditLogFilters', () => {
  it('emits updates on select changes', async () => {
    const wrapper = mount(AuditLogFilters, {
      props: baseProps,
    });

    const entity = wrapper.find('select[aria-label="Filter by entity type"]');
    const user = wrapper.find('select[aria-label="Filter by user"]');
    const activity = wrapper.find('select[aria-label="Filter by activity type"]');

    await entity.setValue('JIRA_WEBHOOK');
    await user.setValue('u1');
    await activity.setValue('CREATE');

    expect(wrapper.emitted('update:selectedEntityType')).toBeTruthy();
    expect(wrapper.emitted('update:selectedUserId')).toBeTruthy();
    expect(wrapper.emitted('update:selectedActivity')).toBeTruthy();
  });

  it('emits clear-filters when clicking Clear All', async () => {
    const wrapper = mount(AuditLogFilters, { props: { ...baseProps, selectedActivity: 'CREATE' } });
    const btn = wrapper.find('button[aria-label="Clear all filters"]');
    expect(btn.attributes('disabled')).toBeUndefined();
    await btn.trigger('click');
    expect(wrapper.emitted('clear-filters')).toBeTruthy();
  });

  it('disables clear button when no filters are active', () => {
    const wrapper = mount(AuditLogFilters, {
      props: {
        ...baseProps,
        selectedEntityType: null,
        selectedUserId: null,
        selectedActivity: null,
      },
    });
    const btn = wrapper.find('button[aria-label="Clear all filters"]');
    expect(btn.attributes('disabled')).toBeDefined();
  });

  it('emits null when selecting empty option for entity type', async () => {
    const wrapper = mount(AuditLogFilters, {
      props: { ...baseProps, selectedEntityType: 'JIRA_WEBHOOK' as any },
    });
    const select = wrapper.find('select[aria-label="Filter by entity type"]');
    await select.setValue('');
    expect(wrapper.emitted('update:selectedEntityType')?.[0]).toEqual([null]);
  });

  it('emits null when selecting empty option for user', async () => {
    const wrapper = mount(AuditLogFilters, {
      props: { ...baseProps, selectedUserId: 'u1' },
    });
    const select = wrapper.find('select[aria-label="Filter by user"]');
    await select.setValue('');
    expect(wrapper.emitted('update:selectedUserId')?.[0]).toEqual([null]);
  });

  it('emits null when selecting empty option for activity', async () => {
    const wrapper = mount(AuditLogFilters, {
      props: { ...baseProps, selectedActivity: 'CREATE' },
    });
    const select = wrapper.find('select[aria-label="Filter by activity type"]');
    await select.setValue('');
    expect(wrapper.emitted('update:selectedActivity')?.[0]).toEqual([null]);
  });
});
