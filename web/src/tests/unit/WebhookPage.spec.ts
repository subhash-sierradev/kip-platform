/* eslint-disable simple-import-sort/imports */
import { mount as baseMount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { nextTick } from 'vue';

// Mock JiraWebhookService
vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    listJiraWebhooks: vi.fn(),
    getAllJiraNormalizedNames: vi.fn().mockResolvedValue([]),
  },
}));
import { JiraWebhookService } from '@/api/services/JiraWebhookService';
const mockListJiraWebhooks = JiraWebhookService.listJiraWebhooks as ReturnType<typeof vi.fn>;

// Mock clipboard API
const mockWriteText = vi.fn().mockResolvedValue(undefined);
Object.assign(navigator, {
  clipboard: {
    writeText: mockWriteText,
  },
});

// Stub WebhookIcon and JiraWebhookWizard
vi.mock('@/components/common/WebhookIcon.vue', () => ({
  default: { name: 'WebhookIcon', template: '<span class="icon-stub" />' },
}));
vi.mock('@/components/outbound/jirawebhooks/wizard/JiraWebhookWizard.vue', () => ({
  default: {
    name: 'JiraWebhookWizard',
    props: ['open'],
    emits: ['close'],
    template: '<div class="wizard-stub" v-if="open">Wizard</div>',
  },
}));

import WebhookPage from '@/components/outbound/jirawebhooks/WebhookPage.vue';

const mount = (...args: Parameters<typeof baseMount>) => baseMount(...args) as any;

describe('WebhookPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '1',
        name: 'Webhook A',
        createdDate: '2024-01-01T12:00:00Z',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook1',
        jiraFieldMappings: [
          { jiraFieldId: 'project', displayLabel: 'Project Alpha' },
          { jiraFieldId: 'issuetype', displayLabel: 'Bug' },
          { jiraFieldId: 'assignee', displayLabel: 'John Doe' },
        ],
        lastEventHistory: {
          status: 'SUCCESS',
          triggeredAt: '2024-01-15T10:30:00Z',
        },
      },
    ]);
  });

  it('renders webhook cards after loading', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    expect(wrapper.findAll('.webhook-card').length).toBe(1);
  });

  it('displays webhook name and created date', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    expect(wrapper.find('.title-text').text()).toBe('Webhook A');
    expect(wrapper.find('.created-text').text()).toContain('Created:');
  });

  it('shows enabled status chip for enabled webhooks', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const statusChip = wrapper.find('.status-chip');
    expect(statusChip.text()).toBe('Enabled');
    expect(statusChip.classes()).toContain('status-active');
  });

  it('shows disabled status chip for disabled webhooks', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '2',
        name: 'Webhook B',
        createdDate: '2024-02-01T08:00:00Z',
        isEnabled: false,
        webhookUrl: 'http://example.com/hook2',
        jiraFieldMappings: [],
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const statusChip = wrapper.find('.status-chip');
    expect(statusChip.text()).toBe('Disabled');
    expect(statusChip.classes()).toContain('status-disabled');
  });

  it('displays project label from field mappings', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    expect(wrapper.find('.project-chip').text()).toBe('Project Alpha');
  });

  it('returns empty string when no project mapping exists', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '3',
        name: 'Webhook C',
        createdDate: '2024-03-01',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook3',
        jiraFieldMappings: [],
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    expect(wrapper.find('.project-chip').exists()).toBe(false);
  });

  it('finds project label by displayLabel containing project', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '4',
        name: 'Webhook D',
        createdDate: '2024-04-01',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook4',
        jiraFieldMappings: [{ jiraFieldId: 'custom', displayLabel: 'My Project Name' }],
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    expect(wrapper.find('.project-chip').text()).toBe('My Project Name');
  });

  it('displays issue type label from field mappings', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const issueTypeCell = wrapper.findAll('.info-cell')[0];
    expect(issueTypeCell.find('.value').text()).toBe('Bug');
  });

  it('returns empty string when no issue type mapping exists', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '5',
        name: 'Webhook E',
        createdDate: '2024-05-01',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook5',
        jiraFieldMappings: [],
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const issueTypeCell = wrapper.findAll('.info-cell')[0];
    expect(issueTypeCell.find('.value').text()).toBe('');
  });

  it('displays assignee from displayLabel', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const assigneeCell = wrapper.findAll('.info-cell')[1];
    expect(assigneeCell.find('.value').text()).toBe('John Doe');
  });

  it('displays assignee from defaultValue when displayLabel absent', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '6',
        name: 'Webhook F',
        createdDate: '2024-06-01',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook6',
        jiraFieldMappings: [{ jiraFieldName: 'assignee', defaultValue: 'Jane Smith' }],
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const assigneeCell = wrapper.findAll('.info-cell')[1];
    expect(assigneeCell.find('.value').text()).toBe('Jane Smith');
  });

  it('displays "Unassigned" when no assignee mapping', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '7',
        name: 'Webhook G',
        createdDate: '2024-07-01',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook7',
        jiraFieldMappings: [],
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const assigneeCell = wrapper.findAll('.info-cell')[1];
    expect(assigneeCell.find('.value').text()).toBe('Unassigned');
  });

  it('displays SUCCESS trigger status with correct class', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const triggerChip = wrapper.find('.trigger-chip');
    expect(triggerChip.text()).toBe('SUCCESS');
    expect(triggerChip.classes()).toContain('trigger-success');
  });

  it('displays PENDING trigger status when lastEventHistory is null', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '8',
        name: 'Webhook H',
        createdDate: '2024-08-01',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook8',
        jiraFieldMappings: [],
        lastEventHistory: null,
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const triggerChip = wrapper.find('.trigger-chip');
    expect(triggerChip.text()).toBe('PENDING');
    expect(triggerChip.classes()).toContain('trigger-pending');
  });

  it('displays FAILURE trigger status with correct class', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '9',
        name: 'Webhook I',
        createdDate: '2024-09-01',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook9',
        jiraFieldMappings: [],
        lastEventHistory: {
          status: 'FAILURE',
          triggeredAt: '2024-09-10T14:20:00Z',
        },
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const triggerChip = wrapper.find('.trigger-chip');
    expect(triggerChip.text()).toBe('FAILURE');
    expect(triggerChip.classes()).toContain('trigger-failure');
  });

  it('displays trigger date when lastEventHistory has triggeredAt', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const triggerDate = wrapper.find('.trigger-date');
    expect(triggerDate.text()).not.toBe('No triggers yet');
  });

  it('displays "No triggers yet" when triggeredAt is absent', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '10',
        name: 'Webhook J',
        createdDate: '2024-10-01',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook10',
        jiraFieldMappings: [],
        lastEventHistory: {
          status: 'PENDING',
        },
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const triggerDate = wrapper.find('.trigger-date');
    expect(triggerDate.text()).toBe('No triggers yet');
  });

  it('displays webhook URL in url-row', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const urlValue = wrapper.find('.url-value');
    expect(urlValue.text()).toBe('http://example.com/hook1');
    expect(urlValue.attributes('title')).toBe('http://example.com/hook1');
  });

  it('copies webhook URL to clipboard on copy button click', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const copyBtn = wrapper.find('.copy-icon-btn');
    await copyBtn.trigger('click');

    expect(mockWriteText).toHaveBeenCalledWith('http://example.com/hook1');
  });

  it('handles clipboard copy error gracefully', async () => {
    mockWriteText.mockRejectedValueOnce(new Error('Clipboard error'));
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    const copyBtn = wrapper.find('.copy-icon-btn');
    await copyBtn.trigger('click');

    await nextTick();
    expect(consoleErrorSpy).toHaveBeenCalledWith('Failed to copy text:', expect.any(Error));

    consoleErrorSpy.mockRestore();
  });

  it('opens wizard when Create Webhook button is clicked', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    expect(wrapper.vm.wizardOpen).toBe(false);

    const createBtn = wrapper.find('button.btn.primary');
    await createBtn.trigger('click');

    expect(wrapper.vm.wizardOpen).toBe(true);
    expect(wrapper.find('.wizard-stub').exists()).toBe(true);
  });

  it('closes wizard when wizard emits close event', async () => {
    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    wrapper.vm.wizardOpen = true;
    await nextTick();

    expect(wrapper.find('.wizard-stub').exists()).toBe(true);

    wrapper.vm.wizardOpen = false;
    await nextTick();

    expect(wrapper.find('.wizard-stub').exists()).toBe(false);
  });

  it('handles API error when loading webhooks', async () => {
    mockListJiraWebhooks.mockRejectedValueOnce(new Error('API Error'));
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    expect(wrapper.findAll('.webhook-card').length).toBe(0);
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      'Failed to load Jira webhooks:',
      expect.any(Error)
    );

    consoleErrorSpy.mockRestore();
  });

  it('displays empty list when API returns non-array', async () => {
    mockListJiraWebhooks.mockResolvedValue(null);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    expect(wrapper.findAll('.webhook-card').length).toBe(0);
  });

  it('renders multiple webhook cards', async () => {
    mockListJiraWebhooks.mockResolvedValue([
      {
        id: '1',
        name: 'Webhook 1',
        createdDate: '2024-01-01',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook1',
        jiraFieldMappings: [],
      },
      {
        id: '2',
        name: 'Webhook 2',
        createdDate: '2024-01-02',
        isEnabled: false,
        webhookUrl: 'http://example.com/hook2',
        jiraFieldMappings: [],
      },
      {
        id: '3',
        name: 'Webhook 3',
        createdDate: '2024-01-03',
        isEnabled: true,
        webhookUrl: 'http://example.com/hook3',
        jiraFieldMappings: [],
      },
    ]);

    const wrapper = mount(WebhookPage);

    await vi.waitFor(() => {
      expect(mockListJiraWebhooks).toHaveBeenCalled();
    });
    await nextTick();

    expect(wrapper.findAll('.webhook-card').length).toBe(3);
  });
});
