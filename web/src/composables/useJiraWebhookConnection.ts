import { type Ref, ref } from 'vue';

import type { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import type { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import type { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import type { MappingData } from '@/api/models/jirawebhook/MappingData';
import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';
import type { ConnectionStepData } from '@/types/ConnectionStepData';

interface UseJiraWebhookConnectionOptions {
  editMode?: boolean;
  cloneMode?: boolean;
  connectionData: Ref<ConnectionStepData>;
  connectionId: Ref<string>;
  jiraConnected: Ref<boolean>;
  mappingModeEdit: Ref<boolean>;
  originalConnectionIdRef: Ref<string>;
  lastResetConnectionId: Ref<string>;
  mappingDataState: MappingData;
  projects: Ref<JiraProject[]>;
  issueTypes: Ref<JiraIssueType[]>;
  users: Ref<JiraUser[]>;
  isMappingValid: Ref<boolean>;
  syncMappingDataRef: () => void;
}

interface JiraProjectResponse {
  key?: unknown;
  name?: unknown;
}
interface JiraIssueTypeResponse {
  id?: unknown;
  name?: unknown;
}
interface JiraUserResponse {
  accountId?: unknown;
  displayName?: unknown;
}

const asString = (value: unknown): string => (typeof value === 'string' ? value : '');

const mapProjects = (items: JiraProjectResponse[]): JiraProject[] =>
  items.map(p => ({ key: asString(p.key), name: asString(p.name) }));

const mapIssueTypes = (items: JiraIssueTypeResponse[]): JiraIssueType[] =>
  items.map(it => ({ id: asString(it.id), name: asString(it.name) }));

const mapUsers = (items: JiraUserResponse[]): JiraUser[] =>
  items.map(u => ({ accountId: asString(u.accountId), displayName: asString(u.displayName) }));

const updateConnectionIdFromData = (options: UseJiraWebhookConnectionOptions) => {
  options.connectionId.value =
    options.connectionData.value.connectionMethod === 'existing'
      ? (options.connectionData.value.existingConnectionId ?? '')
      : (options.connectionData.value.createdConnectionId ?? '');
};

const resetMappingData = (options: UseJiraWebhookConnectionOptions) => {
  options.mappingDataState.selectedProject = '';
  options.mappingDataState.selectedIssueType = '';
  options.mappingDataState.selectedAssignee = '';
  options.mappingDataState.summary = '';
  options.mappingDataState.descriptionFieldMapping = '';
  delete options.mappingDataState.customFields;
  options.projects.value = [];
  options.issueTypes.value = [];
  options.users.value = [];
  options.isMappingValid.value = false;
  options.lastResetConnectionId.value = options.connectionId.value || '';
  options.syncMappingDataRef();
};

const createHydrator =
  (options: UseJiraWebhookConnectionOptions, isHydrating: Ref<boolean>) =>
  async (connId: string): Promise<void> => {
    try {
      isHydrating.value = true;
      const projectsResp = await JiraIntegrationService.getProjectsByConnectionId(connId);
      options.projects.value = mapProjects(projectsResp as JiraProjectResponse[]);

      const projectKey = options.mappingDataState.selectedProject;
      if (!projectKey) return;

      const [issueTypesResp, usersResp] = await Promise.all([
        JiraIntegrationService.getProjectIssueTypesByConnectionId(connId, projectKey),
        JiraIntegrationService.getProjectUsersByConnectionId(connId, projectKey),
      ]);

      options.issueTypes.value = mapIssueTypes(issueTypesResp as JiraIssueTypeResponse[]);
      options.users.value = mapUsers(usersResp as JiraUserResponse[]);
    } catch (err) {
      console.error('[JiraWebhookWizard] Failed to hydrate dropdowns after Verify', err);
    } finally {
      isHydrating.value = false;
    }
  };

const createValidationHandler =
  (options: UseJiraWebhookConnectionOptions) => (isValid: boolean) => {
    options.jiraConnected.value = isValid;
    if (!isValid || options.connectionId.value.trim() !== '') return;
    updateConnectionIdFromData(options);
  };

const createSuccessHandler =
  (
    options: UseJiraWebhookConnectionOptions,
    hydrateDropdownsForConnection: (connId: string) => Promise<void>
  ) =>
  (connId: string) => {
    options.connectionId.value = connId;

    if (options.connectionData.value.connectionMethod === 'new') {
      options.connectionData.value.createdConnectionId = connId;
    } else {
      options.connectionData.value.existingConnectionId = connId;
    }

    if (!options.editMode || options.cloneMode || !options.mappingModeEdit.value) return;
    if (
      options.projects.value.length &&
      options.issueTypes.value.length &&
      options.users.value.length
    )
      return;

    void hydrateDropdownsForConnection(connId);
  };

const createConnectionChangeHandler = (options: UseJiraWebhookConnectionOptions) => () => {
  const previousId = options.originalConnectionIdRef.value;
  const currentId = options.connectionId.value;

  if (!previousId && currentId) {
    options.originalConnectionIdRef.value = currentId;
    return;
  }

  if (currentId === previousId) {
    options.originalConnectionIdRef.value = currentId;
    return;
  }

  if (currentId && currentId !== options.lastResetConnectionId.value) {
    if (options.editMode && !options.cloneMode) {
      options.mappingModeEdit.value = false;
    }
    resetMappingData(options);
  } else if (options.editMode && !options.cloneMode) {
    options.mappingModeEdit.value = true;
  }

  options.originalConnectionIdRef.value = currentId;
};

export function useJiraWebhookConnection(options: UseJiraWebhookConnectionOptions) {
  const isHydrating = ref(false);
  const hydrateDropdownsForConnection = createHydrator(options, isHydrating);

  return {
    isHydrating,
    handleConnectionValidation: createValidationHandler(options),
    handleConnectionSuccess: createSuccessHandler(options, hydrateDropdownsForConnection),
    handleConnectionChangeCheck: createConnectionChangeHandler(options),
    resetMappingData: () => resetMappingData(options),
    hydrateDropdownsForConnection,
  };
}
