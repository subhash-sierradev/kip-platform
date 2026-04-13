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

  it('does not overwrite connectionId when validation fails or connectionId is already set', () => {
    const options = createOptions();
    options.connectionId.value = 'existing-id';

    const { handleConnectionValidation } = useJiraWebhookConnection({
      editMode: false,
      cloneMode: false,
      ...options,
    });

    handleConnectionValidation(false);
    expect(options.jiraConnected.value).toBe(false);
    expect(options.connectionId.value).toBe('existing-id');

    handleConnectionValidation(true);
    expect(options.jiraConnected.value).toBe(true);
    expect(options.connectionId.value).toBe('existing-id');
  });

  it('stores created connection ids for new connections without hydrating outside edit mode', async () => {
    const options = createOptions();
    options.connectionData.value.connectionMethod = 'new';
    options.connectionData.value.createdConnectionId = '';
    options.mappingDataState.selectedProject = 'P1';

    const { handleConnectionSuccess } = useJiraWebhookConnection({
      editMode: false,
      cloneMode: false,
      ...options,
    });

    handleConnectionSuccess('created-9');
    await flushPromises();

    expect(options.connectionId.value).toBe('created-9');
    expect(options.connectionData.value.createdConnectionId).toBe('created-9');
    expect(serviceHoisted.getProjectsByConnectionId).not.toHaveBeenCalled();
  });

  it('skips hydration in edit mode when clone mode is enabled or mapping edit is disabled', async () => {
    const optionsClone = createOptions();
    const cloneComposable = useJiraWebhookConnection({
      editMode: true,
      cloneMode: true,
      ...optionsClone,
    });
    cloneComposable.handleConnectionSuccess('conn-clone');

    const optionsNoEdit = createOptions();
    optionsNoEdit.mappingModeEdit.value = false;
    const noEditComposable = useJiraWebhookConnection({
      editMode: true,
      cloneMode: false,
      ...optionsNoEdit,
    });
    noEditComposable.handleConnectionSuccess('conn-no-edit');

    await flushPromises();
    expect(serviceHoisted.getProjectsByConnectionId).not.toHaveBeenCalled();
  });

  it('skips hydration when dropdown collections are already loaded', async () => {
    const options = createOptions();
    options.projects.value = [{ key: 'P1', name: 'Existing Project' } as JiraProject];
    options.issueTypes.value = [{ id: 'I1', name: 'Bug' } as JiraIssueType];
    options.users.value = [{ accountId: 'U1', displayName: 'User 1' } as JiraUser];

    const { handleConnectionSuccess } = useJiraWebhookConnection({
      editMode: true,
      cloneMode: false,
      ...options,
    });

    handleConnectionSuccess('conn-loaded');
    await flushPromises();

    expect(serviceHoisted.getProjectsByConnectionId).not.toHaveBeenCalled();
  });

  it('hydrates only projects when no project is selected and normalizes non-string responses', async () => {
    serviceHoisted.getProjectsByConnectionId.mockResolvedValueOnce([
      { key: 'P1', name: 'Project 1' },
      { key: 99, name: null },
    ]);

    const options = createOptions();
    options.mappingDataState.selectedProject = '';

    const { hydrateDropdownsForConnection, isHydrating } = useJiraWebhookConnection({
      editMode: true,
      cloneMode: false,
      ...options,
    });

    const promise = hydrateDropdownsForConnection('conn-projects-only');
    expect(isHydrating.value).toBe(true);
    await promise;

    expect(options.projects.value).toEqual([
      { key: 'P1', name: 'Project 1' },
      { key: '', name: '' },
    ]);
    expect(serviceHoisted.getProjectIssueTypesByConnectionId).not.toHaveBeenCalled();
    expect(serviceHoisted.getProjectUsersByConnectionId).not.toHaveBeenCalled();
    expect(isHydrating.value).toBe(false);
  });

  it('handles hydration failures and always clears the hydrating flag', async () => {
    serviceHoisted.getProjectsByConnectionId.mockRejectedValueOnce(new Error('boom'));
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    const options = createOptions();
    const { hydrateDropdownsForConnection, isHydrating } = useJiraWebhookConnection({
      editMode: true,
      cloneMode: false,
      ...options,
    });

    await hydrateDropdownsForConnection('conn-error');

    expect(errorSpy).toHaveBeenCalled();
    expect(isHydrating.value).toBe(false);

    errorSpy.mockRestore();
  });

  it('tracks original connection ids without resetting when first assigned or unchanged', () => {
    const initialOptions = createOptions();
    initialOptions.connectionId.value = 'conn-1';

    const initialComposable = useJiraWebhookConnection({
      editMode: true,
      cloneMode: false,
      ...initialOptions,
    });
    initialComposable.handleConnectionChangeCheck();

    expect(initialOptions.originalConnectionIdRef.value).toBe('conn-1');
    expect(initialOptions.syncMappingDataRef).not.toHaveBeenCalled();

    const unchangedOptions = createOptions();
    unchangedOptions.connectionId.value = 'conn-2';
    unchangedOptions.originalConnectionIdRef.value = 'conn-2';

    const unchangedComposable = useJiraWebhookConnection({
      editMode: true,
      cloneMode: false,
      ...unchangedOptions,
    });
    unchangedComposable.handleConnectionChangeCheck();

    expect(unchangedOptions.originalConnectionIdRef.value).toBe('conn-2');
    expect(unchangedOptions.syncMappingDataRef).not.toHaveBeenCalled();
  });

  it('restores mapping edit mode when switching back to the last reset connection', () => {
    const options = createOptions();
    options.connectionId.value = 'conn-reset';
    options.originalConnectionIdRef.value = 'old-conn';
    options.lastResetConnectionId.value = 'conn-reset';
    options.mappingModeEdit.value = false;

    const { handleConnectionChangeCheck, resetMappingData } = useJiraWebhookConnection({
      editMode: true,
      cloneMode: false,
      ...options,
    });

    options.mappingDataState.customFields = { custom: 'value' } as any;
    resetMappingData();
    expect(options.mappingDataState.customFields).toBeUndefined();

    options.syncMappingDataRef.mockClear();
    handleConnectionChangeCheck();

    expect(options.mappingModeEdit.value).toBe(true);
    expect(options.originalConnectionIdRef.value).toBe('conn-reset');
    expect(options.syncMappingDataRef).not.toHaveBeenCalled();
  });
});
