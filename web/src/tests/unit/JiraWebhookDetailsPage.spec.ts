import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import JiraWebhookDetailsPage from '@/components/outbound/jirawebhooks/details/JiraWebhookDetailsPage.vue';

const hoisted = vi.hoisted(() => ({
  routeId: 'w1',
  backMock: vi.fn(),
  getWebhookByIdMock: vi.fn(),
  getProjectsByConnectionIdMock: vi.fn(),
  getSprintsByConnectionIdMock: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { id: hoisted.routeId } }),
  useRouter: () => ({ back: hoisted.backMock }),
}));

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    getWebhookById: hoisted.getWebhookByIdMock,
  },
}));

vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getProjectsByConnectionId: hoisted.getProjectsByConnectionIdMock,
    getSprintsByConnectionId: hoisted.getSprintsByConnectionIdMock,
  },
}));

vi.mock('@/components/common/WebhookIcon.vue', () => ({
  default: { name: 'WebhookIcon', template: '<div class="webhook-icon-stub" />' },
}));

vi.mock('@/components/common/StandardDetailPageLayout.vue', () => ({
  default: {
    name: 'StandardDetailPageLayout',
    props: [
      'title',
      'loading',
      'error',
      'status',
      'version',
      'activeTab',
      'tabs',
      'componentProps',
      'componentEvents',
    ],
    emits: ['back', 'retry', 'tab-change', 'status-updated', 'refresh'],
    template: `
      <div class="layout-stub">
        <div class="title">{{ title }}</div>
        <div class="error">{{ error }}</div>
        <div class="status">{{ status }}</div>
        <div class="active-tab">{{ activeTab }}</div>
        <button class="emit-back" @click="$emit('back')">back</button>
        <button class="emit-retry" @click="$emit('retry')">retry</button>
        <button class="emit-tab-mapping" @click="$emit('tab-change', 'mapping')">mapping</button>
        <button class="emit-status-updated" @click="$emit('status-updated', false)">disable</button>
      </div>
    `,
  },
}));

const defaultWebhook = {
  id: 'w1',
  name: 'Webhook One',
  isEnabled: true,
  version: 2,
  connectionId: 'conn-1',
  jiraFieldMappings: [
    {
      jiraFieldId: 'project',
      jiraFieldName: 'Project',
      displayLabel: 'Project One',
      defaultValue: '',
    },
  ],
};

describe('JiraWebhookDetailsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    hoisted.routeId = 'w1';

    hoisted.getWebhookByIdMock.mockResolvedValue({ ...defaultWebhook });
    hoisted.getProjectsByConnectionIdMock.mockResolvedValue([
      { key: 'P1', name: 'Project One' },
      { key: 'P2', name: 'Project Two' },
    ]);
    hoisted.getSprintsByConnectionIdMock.mockResolvedValue([
      { id: 101, name: 'Sprint A', state: 'active' },
    ]);
  });

  it('loads details, sets title/status, and cleans breadcrumb on unmount', async () => {
    const setBreadcrumbTitle = vi.fn();

    const wrapper = mount(JiraWebhookDetailsPage, {
      global: {
        provide: { setBreadcrumbTitle },
      },
    });

    await flushPromises();

    expect(hoisted.getWebhookByIdMock).toHaveBeenCalledWith('w1');
    expect(wrapper.find('.title').text()).toContain('Webhook One');
    expect(wrapper.find('.status').text()).toBe('enabled');
    expect(setBreadcrumbTitle).toHaveBeenCalledWith('Jira Webhook Details');

    wrapper.unmount();
    expect(setBreadcrumbTitle).toHaveBeenLastCalledWith(null);
  });

  it('handles back and tab-change events and exposes mapping props only on mapping tab', async () => {
    const wrapper = mount(JiraWebhookDetailsPage);
    await flushPromises();

    await wrapper.find('.emit-back').trigger('click');
    expect(hoisted.backMock).toHaveBeenCalledTimes(1);

    const layout = wrapper.findComponent({ name: 'StandardDetailPageLayout' });
    expect((layout.props() as any).activeTab).toBe('details');
    expect((layout.props() as any).componentProps.sprints).toBeUndefined();

    await wrapper.find('.emit-tab-mapping').trigger('click');
    await flushPromises();

    expect((layout.props() as any).activeTab).toBe('mapping');
    expect((layout.props() as any).componentProps.sprints).toBeDefined();
  });

  it('updates status on status-updated event', async () => {
    const wrapper = mount(JiraWebhookDetailsPage);
    await flushPromises();

    const layout = wrapper.findComponent({ name: 'StandardDetailPageLayout' });
    expect((layout.props() as any).status).toBe('enabled');

    await wrapper.find('.emit-status-updated').trigger('click');
    await flushPromises();

    expect((layout.props() as any).status).toBe('disabled');
  });

  it('ignores status updates before webhook data has loaded', async () => {
    hoisted.getWebhookByIdMock.mockReturnValueOnce(new Promise(() => {}));

    const wrapper = mount(JiraWebhookDetailsPage);
    await wrapper.find('.emit-status-updated').trigger('click');
    await flushPromises();

    const layout = wrapper.findComponent({ name: 'StandardDetailPageLayout' });
    expect((layout.props() as any).status).toBe('disabled');
  });

  it('maps webhook fetch errors to user-friendly messages', async () => {
    hoisted.getWebhookByIdMock.mockRejectedValueOnce(new Error('403 forbidden'));
    const wrapper403 = mount(JiraWebhookDetailsPage);
    await flushPromises();
    expect(wrapper403.find('.error').text()).toContain('Access denied');
    wrapper403.unmount();

    hoisted.getWebhookByIdMock.mockRejectedValueOnce(new Error('404 missing'));
    const wrapper404 = mount(JiraWebhookDetailsPage);
    await flushPromises();
    expect(wrapper404.find('.error').text()).toContain('Webhook not found');
    wrapper404.unmount();

    hoisted.getWebhookByIdMock.mockRejectedValueOnce(new Error('boom'));
    const wrapperGeneral = mount(JiraWebhookDetailsPage);
    await flushPromises();
    expect(wrapperGeneral.find('.error').text()).toContain('boom');
  });

  it('loads sprints using one resolved project key lookup', async () => {
    mount(JiraWebhookDetailsPage);
    await flushPromises();

    expect(hoisted.getProjectsByConnectionIdMock).toHaveBeenCalledTimes(1);
    expect(hoisted.getProjectsByConnectionIdMock).toHaveBeenCalledWith('conn-1');
    expect(hoisted.getSprintsByConnectionIdMock).toHaveBeenCalledWith(
      'conn-1',
      expect.objectContaining({ projectKey: 'P1', state: 'active,future' })
    );
  });

  it('shows a required-id error and skips fetching when the route id is missing', async () => {
    hoisted.routeId = '';

    const wrapper = mount(JiraWebhookDetailsPage);
    await flushPromises();

    expect(hoisted.getWebhookByIdMock).not.toHaveBeenCalled();
    expect(wrapper.find('.error').text()).toContain('Webhook ID is required');
  });

  it('falls back to a generic fetch error message when no error message is available', async () => {
    hoisted.getWebhookByIdMock.mockRejectedValueOnce({});
    const wrapper = mount(JiraWebhookDetailsPage);
    await flushPromises();

    expect(wrapper.find('.error').text()).toContain('Failed to load webhook details');
  });

  it('keeps the details tab active for unknown tab ids', async () => {
    const wrapper = mount(JiraWebhookDetailsPage);
    await flushPromises();

    const layout = wrapper.findComponent({ name: 'StandardDetailPageLayout' });
    await layout.vm.$emit('tab-change', 'missing-tab');
    await flushPromises();

    expect((layout.props() as any).activeTab).toBe('details');
  });

  it('resolves the project key from defaultValue when displayLabel is empty', async () => {
    hoisted.getWebhookByIdMock.mockResolvedValueOnce({
      ...defaultWebhook,
      jiraFieldMappings: [
        {
          jiraFieldId: 'project',
          jiraFieldName: 'Project',
          displayLabel: '',
          defaultValue: 'P2',
        },
      ],
    });

    mount(JiraWebhookDetailsPage);
    await flushPromises();

    expect(hoisted.getSprintsByConnectionIdMock).toHaveBeenCalledWith(
      'conn-1',
      expect.objectContaining({ projectKey: 'P2' })
    );
  });

  it('resolves the project key from the project name when the mapping label is not already a key', async () => {
    hoisted.getWebhookByIdMock.mockResolvedValueOnce({
      ...defaultWebhook,
      jiraFieldMappings: [
        {
          jiraFieldId: 'project',
          jiraFieldName: 'Project',
          displayLabel: 'Project Two',
          defaultValue: '',
        },
      ],
    });

    mount(JiraWebhookDetailsPage);
    await flushPromises();

    expect(hoisted.getSprintsByConnectionIdMock).toHaveBeenCalledWith(
      'conn-1',
      expect.objectContaining({ projectKey: 'P2' })
    );
  });

  it('clears sprint options when mapping context cannot be resolved or sprint loading fails', async () => {
    hoisted.getWebhookByIdMock.mockResolvedValueOnce({
      ...defaultWebhook,
      connectionId: '',
      jiraFieldMappings: [],
    });
    const wrapperNoContext = mount(JiraWebhookDetailsPage);
    await flushPromises();

    let layout = wrapperNoContext.findComponent({ name: 'StandardDetailPageLayout' });
    await wrapperNoContext.find('.emit-tab-mapping').trigger('click');
    await flushPromises();
    expect((layout.props() as any).componentProps.sprints).toEqual([]);

    wrapperNoContext.unmount();

    hoisted.getWebhookByIdMock.mockResolvedValueOnce({
      ...defaultWebhook,
      jiraFieldMappings: [
        {
          jiraFieldId: 'project',
          jiraFieldName: 'Project',
          displayLabel: 'Project One',
          defaultValue: '',
        },
      ],
    });
    hoisted.getSprintsByConnectionIdMock.mockRejectedValueOnce(new Error('bad sprints'));

    const wrapperSprintFailure = mount(JiraWebhookDetailsPage);
    await flushPromises();

    layout = wrapperSprintFailure.findComponent({ name: 'StandardDetailPageLayout' });
    await wrapperSprintFailure.find('.emit-tab-mapping').trigger('click');
    await flushPromises();

    expect((layout.props() as any).componentProps.sprints).toEqual([]);
    expect((layout.props() as any).componentProps.sprintsLoading).toBe(false);
  });

  it('clears sprint options when the mapped project cannot be resolved or the project lookup fails', async () => {
    hoisted.getWebhookByIdMock.mockResolvedValueOnce({
      ...defaultWebhook,
      jiraFieldMappings: [
        {
          jiraFieldId: 'project',
          jiraFieldName: 'Project',
          displayLabel: 'Missing Project',
          defaultValue: '',
        },
      ],
    });

    const wrapperNoProjectMatch = mount(JiraWebhookDetailsPage);
    await flushPromises();

    let layout = wrapperNoProjectMatch.findComponent({ name: 'StandardDetailPageLayout' });
    await wrapperNoProjectMatch.find('.emit-tab-mapping').trigger('click');
    await flushPromises();

    expect(hoisted.getSprintsByConnectionIdMock).not.toHaveBeenCalled();
    expect((layout.props() as any).componentProps.sprints).toEqual([]);

    wrapperNoProjectMatch.unmount();
    vi.clearAllMocks();

    hoisted.getWebhookByIdMock.mockResolvedValueOnce({ ...defaultWebhook });
    hoisted.getProjectsByConnectionIdMock.mockRejectedValueOnce(new Error('projects failed'));

    const wrapperProjectFailure = mount(JiraWebhookDetailsPage);
    await flushPromises();

    layout = wrapperProjectFailure.findComponent({ name: 'StandardDetailPageLayout' });
    await wrapperProjectFailure.find('.emit-tab-mapping').trigger('click');
    await flushPromises();

    expect(hoisted.getProjectsByConnectionIdMock).toHaveBeenCalledWith('conn-1');
    expect(hoisted.getSprintsByConnectionIdMock).not.toHaveBeenCalled();
    expect((layout.props() as any).componentProps.sprints).toEqual([]);
    expect((layout.props() as any).componentProps.sprintsLoading).toBe(false);
  });

  it('skips project lookup when the project mapping trims to an empty value', async () => {
    hoisted.getWebhookByIdMock.mockResolvedValueOnce({
      ...defaultWebhook,
      jiraFieldMappings: [
        {
          jiraFieldId: 'project',
          jiraFieldName: 'Project',
          displayLabel: '   ',
          defaultValue: '   ',
        },
      ],
    });

    const wrapper = mount(JiraWebhookDetailsPage);
    await flushPromises();

    const layout = wrapper.findComponent({ name: 'StandardDetailPageLayout' });
    await wrapper.find('.emit-tab-mapping').trigger('click');
    await flushPromises();

    expect(hoisted.getProjectsByConnectionIdMock).not.toHaveBeenCalled();
    expect(hoisted.getSprintsByConnectionIdMock).not.toHaveBeenCalled();
    expect((layout.props() as any).componentProps.sprints).toEqual([]);
  });
});
