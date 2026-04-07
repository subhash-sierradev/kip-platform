import { flushPromises } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { reactive, ref } from 'vue';

import type { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import type { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import type { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import type { MappingData } from '@/api/models/jirawebhook/MappingData';
import { useJiraWebhookConnection } from '@/composables/useJiraWebhookConnection';
import type { ConnectionStepData } from '@/types/ConnectionStepData';

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

const createOptions = () => {
  const connectionData = ref<ConnectionStepData>({
    connectionMethod: 'existing',
    baseUrl: '',
    credentialType: 'BASIC_AUTH',
    username: '',
    password: '',
    connectionName: '',
    connected: false,
    existingConnectionId: 'conn-1',
    createdConnectionId: undefined,
  });
  const connectionId = ref('');
  const jiraConnected = ref(false);
  const mappingModeEdit = ref(true);
  const originalConnectionIdRef = ref('');
  const lastResetConnectionId = ref('');
  const mappingDataState = reactive<MappingData>({
    selectedProject: 'P1',
    selectedIssueType: '',
    selectedAssignee: '',
    summary: 'Summary',
    descriptionFieldMapping: 'Desc',
  });
  const projects = ref<JiraProject[]>([]);
  const issueTypes = ref<JiraIssueType[]>([]);
  const users = ref<JiraUser[]>([]);
  const isMappingValid = ref(true);
  const syncMappingDataRef = vi.fn();

  return {
    connectionData,
    connectionId,
    jiraConnected,
    mappingModeEdit,
    originalConnectionIdRef,
    lastResetConnectionId,
    mappingDataState,
    projects,
    issueTypes,
    users,
    isMappingValid,
    syncMappingDataRef,
  };
};

describe('useJiraWebhookConnection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('updates connectionId when validation succeeds', () => {
    const options = createOptions();
    const { handleConnectionValidation } = useJiraWebhookConnection({
      editMode: false,
      cloneMode: false,
      ...options,
    });

    handleConnectionValidation(true);

    expect(options.jiraConnected.value).toBe(true);
    expect(options.connectionId.value).toBe('conn-1');
  });

  it('hydrates metadata on connection success in edit mode', async () => {
    serviceHoisted.getProjectsByConnectionId.mockResolvedValueOnce([
      { key: 'P1', name: 'Project 1' },
    ]);
    serviceHoisted.getProjectIssueTypesByConnectionId.mockResolvedValueOnce([
      { id: 'I1', name: 'Bug' },
    ]);
    serviceHoisted.getProjectUsersByConnectionId.mockResolvedValueOnce([
      { accountId: 'U1', displayName: 'User 1' },
    ]);

    const options = createOptions();
    options.mappingDataState.selectedProject = 'P1';

    const { handleConnectionSuccess } = useJiraWebhookConnection({
      editMode: true,
      cloneMode: false,
      ...options,
    });

    handleConnectionSuccess('conn-2');
    await flushPromises();

    expect(options.connectionId.value).toBe('conn-2');
    expect(serviceHoisted.getProjectsByConnectionId).toHaveBeenCalledWith('conn-2');
    expect(options.projects.value).toEqual([{ key: 'P1', name: 'Project 1' }]);
    expect(options.issueTypes.value).toEqual([{ id: 'I1', name: 'Bug' }]);
    expect(options.users.value).toEqual([{ accountId: 'U1', displayName: 'User 1' }]);
  });

  it('resets mapping when connection changes', () => {
    const options = createOptions();
    options.connectionId.value = 'new-conn';
    options.originalConnectionIdRef.value = 'old-conn';

    const { handleConnectionChangeCheck } = useJiraWebhookConnection({
      editMode: true,
      cloneMode: false,
      ...options,
    });

    handleConnectionChangeCheck();

    expect(options.mappingModeEdit.value).toBe(false);
    expect(options.mappingDataState.selectedProject).toBe('');
    expect(options.mappingDataState.summary).toBe('');
    expect(options.projects.value).toEqual([]);
    expect(options.lastResetConnectionId.value).toBe('new-conn');
    expect(options.syncMappingDataRef).toHaveBeenCalled();
  });
});
