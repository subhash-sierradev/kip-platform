import { flushPromises } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { reactive, ref } from 'vue';

import type { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import type { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import type { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import type { MappingData } from '@/api/models/jirawebhook/MappingData';
import { useJiraWebhookPrefill } from '@/composables/useJiraWebhookPrefill';
import type { ConnectionStepData } from '@/types/ConnectionStepData';
import type { JiraWebhook } from '@/types/JiraWebhook';

const serviceHoisted = vi.hoisted(() => ({
  getProjectsByConnectionId: vi.fn(),
  getProjectIssueTypesByConnectionId: vi.fn(),
  getProjectUsersByConnectionId: vi.fn(),
}));

vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getProjectsByConnectionId: serviceHoisted.getProjectsByConnectionId,
    getProjectIssueTypesByConnectionId: serviceHoisted.getProjectIssueTypesByConnectionId,
    getProjectUsersByConnectionId: serviceHoisted.getProjectUsersByConnectionId,
  },
}));

describe('useJiraWebhookPrefill', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const createOptions = () => {
    const editMode = ref(false);
    const cloneMode = ref(false);
    const editingWebhookData = ref<JiraWebhook | undefined>(undefined);
    const cloningWebhookData = ref<JiraWebhook | undefined>(undefined);
    const integrationName = ref('Old Name');
    const webhookDescription = ref('Old Desc');
    const jsonSample = ref('');
    const connectionData = ref<ConnectionStepData>({
      connectionMethod: 'existing',
      baseUrl: '',
      credentialType: 'BASIC_AUTH',
      username: '',
      password: '',
      connectionName: '',
      connected: false,
      existingConnectionId: undefined,
      createdConnectionId: undefined,
    });
    const connectionId = ref('');
    const mappingDataState = reactive<MappingData>({
      selectedProject: 'OLD',
      selectedIssueType: 'OLD',
      selectedAssignee: 'OLD',
      summary: 'Old Summary',
      descriptionFieldMapping: 'Old Description',
    });
    const mappingDataRef = ref<MappingData>({ ...mappingDataState });
    const projects = ref<JiraProject[]>([]);
    const issueTypes = ref<JiraIssueType[]>([]);
    const users = ref<JiraUser[]>([]);
    const activeStep = ref(2);
    const isBasicDetailsValid = ref(true);
    const isMappingValid = ref(true);
    const originalConnectionIdRef = ref('');
    const mappingModeEdit = ref(false);
    const resetAllFields = vi.fn();
    const loadNormalizedNames = vi.fn().mockResolvedValue(undefined);

    return {
      editMode,
      cloneMode,
      editingWebhookData,
      cloningWebhookData,
      integrationName,
      webhookDescription,
      jsonSample,
      connectionData,
      connectionId,
      mappingDataState,
      mappingDataRef,
      projects,
      issueTypes,
      users,
      activeStep,
      isBasicDetailsValid,
      isMappingValid,
      originalConnectionIdRef,
      mappingModeEdit,
      resetAllFields,
      loadNormalizedNames,
    };
  };

  it('initializes create mode when no data is provided', async () => {
    const options = createOptions();

    const { handleOpenChange } = useJiraWebhookPrefill({
      ...options,
    });

    await handleOpenChange(true);

    expect(options.integrationName.value).toBe('');
    expect(options.webhookDescription.value).toBe('');
    expect(options.mappingDataState.selectedProject).toBe('');
    expect(options.isBasicDetailsValid.value).toBe(false);
    expect(options.isMappingValid.value).toBe(false);
  });

  it('prefills edit data and custom fields', async () => {
    serviceHoisted.getProjectsByConnectionId.mockResolvedValueOnce([
      { key: 'PROJ', name: 'Project' },
    ]);
    serviceHoisted.getProjectIssueTypesByConnectionId.mockResolvedValueOnce([
      { id: 'IT', name: 'Bug' },
    ]);
    serviceHoisted.getProjectUsersByConnectionId.mockResolvedValueOnce([
      { accountId: 'U1', displayName: 'User 1' },
    ]);

    const options = createOptions();

    const editingWebhookData: JiraWebhook = {
      id: '1',
      name: 'Webhook One',
      webhookUrl: 'https://example.test/webhook',
      jiraFieldMappings: [],
      isEnabled: true,
      isDeleted: false,
      createdBy: 'tester',
      createdDate: '2024-01-01T00:00:00Z',
      lastEventHistory: {
        eventId: 'e1',
        eventType: 'TEST',
        timestamp: '2024-01-01T00:00:00Z',
        status: 'SUCCESS',
      },
      description: 'Desc',
      samplePayload: '{"ok":true}',
      connectionId: 'conn-1',
      fieldsMapping: [
        { jiraFieldId: 'project', sourceField: '', targetField: '', template: 'PROJ' },
        { jiraFieldId: 'issuetype', sourceField: '', targetField: '', template: 'IT' },
        { jiraFieldId: 'assignee', sourceField: '', targetField: '', template: 'U1' },
        { jiraFieldId: 'summary', sourceField: '', targetField: '', template: 'Summary' },
        { jiraFieldId: 'description', sourceField: '', targetField: '', template: 'Description' },
        {
          jiraFieldId: 'customfield_1',
          jiraFieldName: 'CF1',
          sourceField: '',
          targetField: '',
          template: '{{foo}}',
          dataType: 'string',
        },
      ],
    };

    options.editMode.value = true;
    options.editingWebhookData.value = editingWebhookData;

    const { handleOpenChange } = useJiraWebhookPrefill({
      ...options,
    });

    await handleOpenChange(true);
    await flushPromises();

    expect(options.integrationName.value).toBe('Webhook One');
    expect(options.webhookDescription.value).toBe('Desc');
    expect(options.mappingDataState.selectedProject).toBe('PROJ');
    expect(options.mappingDataState.summary).toBe('Summary');
    expect(options.mappingDataState.customFields?.length).toBe(1);
    expect(options.mappingDataState.customFields?.[0].jiraFieldKey).toBe('customfield_1');
    expect(options.mappingDataState.customFields?.[0].valueSource).toBe('json');
    expect(options.isBasicDetailsValid.value).toBe(true);
    expect(options.isMappingValid.value).toBe(true);
  });

  it('prefills clone data with copied name and falls back to jiraFieldMappings', async () => {
    serviceHoisted.getProjectsByConnectionId.mockResolvedValueOnce([
      { key: 'PROJ', name: 'Project' },
    ]);
    serviceHoisted.getProjectIssueTypesByConnectionId.mockResolvedValueOnce([
      { id: 'IT', name: 'Bug' },
    ]);
    serviceHoisted.getProjectUsersByConnectionId.mockResolvedValueOnce([
      { accountId: 'U1', displayName: 'User 1' },
    ]);

    const options = createOptions();
    options.cloneMode.value = true;
    options.cloningWebhookData.value = {
      id: '2',
      name: 'Clone Source',
      webhookUrl: 'https://example.test/webhook',
      jiraFieldMappings: [
        { sourceField: '', targetField: '', jiraFieldId: 'project', template: 'PROJ' },
        { sourceField: '', targetField: '', jiraFieldId: 'issuetype', template: 'IT' },
        { sourceField: '', targetField: '', jiraFieldId: 'assignee', template: 'U1' },
        { sourceField: '', targetField: '', jiraFieldId: 'summary', template: 'Clone Summary' },
        {
          sourceField: '',
          targetField: '',
          jiraFieldId: 'description',
          template: 'Clone Description',
        },
        {
          sourceField: '',
          targetField: '',
          jiraFieldId: 'customfield_7',
          jiraFieldName: 'Literal Field',
          template: 'literal value',
          dataType: 'unsupported-type',
          required: true,
        },
      ],
      isEnabled: true,
      isDeleted: false,
      createdBy: 'tester',
      createdDate: '2024-01-01T00:00:00Z',
      lastEventHistory: {
        eventId: 'e1',
        eventType: 'TEST',
        timestamp: '2024-01-01T00:00:00Z',
        status: 'SUCCESS',
      },
      description: 'Clone Desc',
      samplePayload: '{"clone":true}',
      connectionId: 'conn-1',
    } as JiraWebhook;

    const { handleOpenChange } = useJiraWebhookPrefill({
      ...options,
    });

    await handleOpenChange(true);
    await flushPromises();

    expect(options.integrationName.value).toBe('Copy of Clone Source');
    expect(options.mappingDataState.summary).toBe('Clone Summary');
    expect(options.mappingDataState.customFields?.[0]).toMatchObject({
      jiraFieldKey: 'customfield_7',
      type: 'string',
      valueSource: 'literal',
      required: true,
    });
  });

  it('resets fields when the wizard closes', async () => {
    const options = createOptions();
    options.editMode.value = true;

    const { handleOpenChange } = useJiraWebhookPrefill({
      ...options,
    });

    await handleOpenChange(false);

    expect(options.resetAllFields).toHaveBeenCalledWith(true);
  });

  it('skips unmatched hydrated dropdown values after loading remote metadata', async () => {
    serviceHoisted.getProjectsByConnectionId.mockResolvedValueOnce([
      { key: 'OTHER', name: 'Other' },
    ]);
    serviceHoisted.getProjectIssueTypesByConnectionId.mockResolvedValueOnce([
      { id: 'OTHER_IT', name: 'Task' },
    ]);
    serviceHoisted.getProjectUsersByConnectionId.mockResolvedValueOnce([
      { accountId: 'OTHER_USER', displayName: 'Other User' },
    ]);

    const options = createOptions();
    options.editMode.value = true;
    options.editingWebhookData.value = {
      id: '3',
      name: 'Webhook Mismatch',
      webhookUrl: 'https://example.test/webhook',
      jiraFieldMappings: [],
      isEnabled: true,
      isDeleted: false,
      createdBy: 'tester',
      createdDate: '2024-01-01T00:00:00Z',
      lastEventHistory: {
        eventId: 'e1',
        eventType: 'TEST',
        timestamp: '2024-01-01T00:00:00Z',
        status: 'SUCCESS',
      },
      description: 'Desc',
      samplePayload: '{}',
      connectionId: 'conn-2',
      fieldsMapping: [
        { sourceField: '', targetField: '', jiraFieldId: 'project', template: 'PROJ' },
        { sourceField: '', targetField: '', jiraFieldId: 'issuetype', template: 'IT' },
        { sourceField: '', targetField: '', jiraFieldId: 'assignee', template: 'U1' },
      ],
    } as JiraWebhook;

    const { handleOpenChange } = useJiraWebhookPrefill({
      ...options,
    });

    await handleOpenChange(true);
    await flushPromises();

    expect(options.mappingDataState.selectedProject).toBe('');
    expect(options.mappingDataState.selectedIssueType).toBe('');
    expect(options.mappingDataState.selectedAssignee).toBe('');
  });

  it('swallows metadata hydration failures and still finalizes validation state', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    serviceHoisted.getProjectsByConnectionId.mockRejectedValueOnce(new Error('projects failed'));

    const options = createOptions();
    options.editMode.value = true;
    options.editingWebhookData.value = {
      id: '4',
      name: 'Webhook Error',
      webhookUrl: 'https://example.test/webhook',
      jiraFieldMappings: [],
      isEnabled: true,
      isDeleted: false,
      createdBy: 'tester',
      createdDate: '2024-01-01T00:00:00Z',
      lastEventHistory: {
        eventId: 'e1',
        eventType: 'TEST',
        timestamp: '2024-01-01T00:00:00Z',
        status: 'SUCCESS',
      },
      description: 'Desc',
      samplePayload: '{}',
      connectionId: 'conn-3',
      fieldsMapping: [
        { sourceField: '', targetField: '', jiraFieldId: 'project', template: 'PROJ' },
      ],
    } as JiraWebhook;

    const { handleOpenChange } = useJiraWebhookPrefill({
      ...options,
    });

    await handleOpenChange(true);
    await flushPromises();

    expect(consoleErrorSpy).toHaveBeenCalled();
    expect(options.isBasicDetailsValid.value).toBe(true);
    expect(options.isMappingValid.value).toBe(true);

    consoleErrorSpy.mockRestore();
  });

  // ── Bug fix: Clone flow — Next button must be disabled when name exceeds 100 chars ──

  it('sets isBasicDetailsValid to false when clone name exceeds 100 characters', async () => {
    const options = createOptions();
    options.cloneMode.value = true;
    // 95-char original name → "Copy of " (8) + 95 = 103 chars → exceeds 100
    options.cloningWebhookData.value = {
      id: '5',
      name: 'A'.repeat(95),
      webhookUrl: 'https://example.test/webhook',
      jiraFieldMappings: [],
      isEnabled: true,
      isDeleted: false,
      createdBy: 'tester',
      createdDate: '2024-01-01T00:00:00Z',
      lastEventHistory: { eventId: 'e1', eventType: 'TEST', timestamp: '2024-01-01T00:00:00Z', status: 'SUCCESS' },
      description: '',
      samplePayload: '{}',
      connectionId: 'conn-1',
    } as JiraWebhook;

    const { handleOpenChange } = useJiraWebhookPrefill({ ...options });
    await handleOpenChange(true);
    await flushPromises();

    expect(options.integrationName.value.length).toBeGreaterThan(100);
    expect(options.isBasicDetailsValid.value).toBe(false);
  });

  it('sets isBasicDetailsValid to true when clone name is within 100 characters', async () => {
    const options = createOptions();
    options.cloneMode.value = true;
    // 88-char original name → "Copy of " (8) + 88 = 96 chars → valid
    options.cloningWebhookData.value = {
      id: '6',
      name: 'A'.repeat(88),
      webhookUrl: 'https://example.test/webhook',
      jiraFieldMappings: [],
      isEnabled: true,
      isDeleted: false,
      createdBy: 'tester',
      createdDate: '2024-01-01T00:00:00Z',
      lastEventHistory: { eventId: 'e1', eventType: 'TEST', timestamp: '2024-01-01T00:00:00Z', status: 'SUCCESS' },
      description: '',
      samplePayload: '{}',
      connectionId: 'conn-1',
    } as JiraWebhook;

    const { handleOpenChange } = useJiraWebhookPrefill({ ...options });
    await handleOpenChange(true);
    await flushPromises();

    expect(options.integrationName.value.length).toBeLessThanOrEqual(100);
    expect(options.isBasicDetailsValid.value).toBe(true);
  });
});
