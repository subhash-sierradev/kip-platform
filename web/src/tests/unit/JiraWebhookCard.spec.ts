import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import JiraWebhookCard from '@/components/outbound/jirawebhooks/JiraWebhookCard.vue';
import type { JiraWebhook } from '@/types/JiraWebhook';

describe('JiraWebhookCard', () => {
  const baseWebhook = (overrides: Partial<JiraWebhook> = {}): JiraWebhook =>
    ({
      id: overrides.id ?? 'id-1',
      name: overrides.name ?? 'Hook One',
      webhookUrl: overrides.webhookUrl ?? 'http://example.com',
      createdBy: overrides.createdBy ?? 'user',
      createdDate: overrides.createdDate ?? '2024-01-01T00:00:00Z',
      updatedDate: overrides.updatedDate ?? undefined,
      isEnabled: overrides.isEnabled ?? true,
      jiraFieldMappings: overrides.jiraFieldMappings ?? [],
      lastEventHistory: overrides.lastEventHistory ?? undefined,
    }) as JiraWebhook;

  it('renders loading skeletons', () => {
    const wrapper = mount(JiraWebhookCard, {
      props: {
        loading: true,
        search: '',
        webhooks: [],
        totalCount: 0,
        viewMode: 'grid',
        getProjectLabel: () => '',
        getIssueTypeLabel: () => '',
        getAssignee: () => '',
        formatMetadataDate: () => '',
      },
      global: {
        stubs: {
          WebhookIcon: { template: '<span class="icon-stub" />' },
          ActionMenu: { template: '<button class="action-menu-stub" />' },
        },
      },
    });

    expect(wrapper.findAll('.webhook-card').length).toBe(6);
  });

  it('emits open, action, and copy events', async () => {
    const wrapper = mount(JiraWebhookCard, {
      props: {
        loading: false,
        search: '',
        webhooks: [baseWebhook()],
        totalCount: 1,
        viewMode: 'grid',
        getProjectLabel: () => 'Project',
        getIssueTypeLabel: () => 'Issue',
        getAssignee: () => 'Assignee',
        formatMetadataDate: () => '2024-01-01',
      },
      global: {
        stubs: {
          WebhookIcon: { template: '<span class="icon-stub" />' },
          ActionMenu: {
            props: ['items'],
            emits: ['action'],
            template:
              '<button class="action-menu-stub" @click="$emit(\'action\', items[0]?.id)">Menu</button>',
          },
        },
      },
    });

    await wrapper.find('.webhook-card').trigger('click');
    expect(wrapper.emitted('open')).toEqual([['id-1']]);

    await wrapper.find('.copy-icon-btn').trigger('click');
    expect(wrapper.emitted('copy')).toEqual([['http://example.com']]);

    await wrapper.find('.action-menu-stub').trigger('click');
    expect(wrapper.emitted('action')).toEqual([['edit', baseWebhook()]]);
  });
});
