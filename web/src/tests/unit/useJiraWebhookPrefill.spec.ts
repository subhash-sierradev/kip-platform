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
});
