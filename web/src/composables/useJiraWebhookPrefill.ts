import type { Ref } from 'vue';

import { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import type { JiraWebhookData } from '@/api/models/jirawebhook/JiraWebhookData';
import { MappingData } from '@/api/models/jirawebhook/MappingData';
import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';
import type { CustomFieldMapping } from '@/api/services/JiraWebhookService';
import { generateUid } from '@/components/outbound/jirawebhooks/utils/CustomFieldHelper';
import type { ConnectionStepData } from '@/types/ConnectionStepData';

interface UseJiraWebhookPrefillOptions {
  editMode: Ref<boolean | undefined>;
  cloneMode: Ref<boolean | undefined>;
  editingWebhookData: Ref<JiraWebhookData | undefined>;
  cloningWebhookData: Ref<JiraWebhookData | undefined>;
  integrationName: Ref<string>;
  webhookDescription: Ref<string>;
  jsonSample: Ref<string>;
  connectionData: Ref<ConnectionStepData>;
  connectionId: Ref<string>;
  mappingDataState: MappingData;
  mappingDataRef: Ref<MappingData>;
  projects: Ref<JiraProject[]>;
  issueTypes: Ref<JiraIssueType[]>;
  users: Ref<JiraUser[]>;
  activeStep: Ref<number>;
  isBasicDetailsValid: Ref<boolean>;
  isMappingValid: Ref<boolean>;
  originalConnectionIdRef: Ref<string>;
  mappingModeEdit: Ref<boolean>;
  resetAllFields: (editMode?: boolean) => void;
  loadNormalizedNames: () => Promise<void>;
}

interface FieldMappingEntry {
  jiraFieldId?: string;
  template?: string;
  jiraFieldName?: string;
  displayLabel?: string;
  dataType?: string;
  required?: boolean;
}

interface WebhookMappingPayload {
  fieldsMapping?: FieldMappingEntry[];
  jiraFieldMappings?: FieldMappingEntry[];
}

interface ConnectionDataPayload {
  connectionId?: string;
  name?: string;
  description?: string;
  samplePayload?: string;
}

const getPrefillData = (options: UseJiraWebhookPrefillOptions): JiraWebhookData | null => {
  if (options.cloneMode.value && options.cloningWebhookData.value)
    return options.cloningWebhookData.value;
  if (options.editMode.value && options.editingWebhookData.value)
    return options.editingWebhookData.value;
  return null;
};

const extractFieldsMapping = (data: WebhookMappingPayload): FieldMappingEntry[] => {
  if (Array.isArray(data.fieldsMapping) && data.fieldsMapping.length > 0) return data.fieldsMapping;
  if (Array.isArray(data.jiraFieldMappings) && data.jiraFieldMappings.length > 0)
    return data.jiraFieldMappings;
  return [];
};

const populateStandardFields = (mappingDataState: MappingData, fields: FieldMappingEntry[]) => {
  mappingDataState.selectedProject = String(
    fields.find(f => f.jiraFieldId === 'project')?.template ?? ''
  );
  mappingDataState.selectedIssueType = String(
    fields.find(f => f.jiraFieldId === 'issuetype')?.template ?? ''
  );
  mappingDataState.selectedAssignee = String(
    fields.find(f => f.jiraFieldId === 'assignee')?.template ?? ''
  );
  mappingDataState.summary = String(fields.find(f => f.jiraFieldId === 'summary')?.template ?? '');
  mappingDataState.descriptionFieldMapping = String(
    fields.find(f => f.jiraFieldId === 'description')?.template ?? ''
  );
};

const toCustomFieldType = (value: string): CustomFieldMapping['type'] => {
  switch (value) {
    case 'string':
    case 'number':
    case 'boolean':
    case 'option':
    case 'array':
    case 'datetime':
    case 'date':
    case 'user':
    case 'multiuser':
    case 'url':
      return value;
    default:
      return 'string';
  }
};

const populateCustomFields = (
  mappingDataState: MappingData,
  fieldsMapping: FieldMappingEntry[]
) => {
  try {
    const customFieldCandidates: CustomFieldMapping[] = fieldsMapping
      .filter(
        f =>
          !!f &&
          !['project', 'issuetype', 'assignee', 'summary', 'description'].includes(
            String(f.jiraFieldId)
          )
      )
      .map(f => ({
        _id: generateUid(),
        jiraFieldKey: String(f.jiraFieldId),
        jiraFieldLabel: String(f.jiraFieldName || f.displayLabel || f.jiraFieldId || ''),
        type: toCustomFieldType(
          typeof f.dataType === 'string' ? String(f.dataType).toLowerCase() : 'string'
        ),
        required: !!f.required,
        valueSource: /{{\s*.+\s*}}/.test(String(f.template || '')) ? 'json' : 'literal',
        value: String(f.template ?? ''),
      }));
    (mappingDataState as MappingData & { customFields?: CustomFieldMapping[] }).customFields =
      customFieldCandidates;
  } catch (error) {
    console.error('[JiraWebhookWizard] Failed to populate custom fields from mapping', {
      error,
      fieldsMapping,
    });
  }
};

const fetchProjects = async (connectionId: string) => {
  const projectsResp = await JiraIntegrationService.getProjectsByConnectionId(connectionId);
  return projectsResp.map(p => ({ key: String(p.key), name: String(p.name) }));
};

const fetchIssueTypes = async (connectionId: string, projectKey: string) => {
  const issueTypesResp = await JiraIntegrationService.getProjectIssueTypesByConnectionId(
    connectionId,
    projectKey
  );
  return issueTypesResp.map(it => ({ id: String(it.id), name: String(it.name) }));
};

const fetchUsers = async (connectionId: string, projectKey: string) => {
  const usersResp = await JiraIntegrationService.getProjectUsersByConnectionId(
    connectionId,
    projectKey
  );
  return usersResp.map(u => ({
    accountId: String(u.accountId),
    displayName: String(u.displayName),
  }));
};

const getFieldTemplate = (fields: FieldMappingEntry[], fieldId: string) =>
  String(fields.find(f => f.jiraFieldId === fieldId)?.template ?? '');

const hydrateDropdownsAndReapplySelections = async (
  connectionId: string,
  mappingDataState: MappingData,
  projects: Ref<JiraProject[]>,
  issueTypes: Ref<JiraIssueType[]>,
  users: Ref<JiraUser[]>,
  fields: FieldMappingEntry[]
) => {
  try {
    projects.value = await fetchProjects(connectionId);
    issueTypes.value = await fetchIssueTypes(connectionId, mappingDataState.selectedProject);
    users.value = await fetchUsers(connectionId, mappingDataState.selectedProject);

    const projectValue = getFieldTemplate(fields, 'project');
    const issueTypeValue = getFieldTemplate(fields, 'issuetype');
    const assigneeValue = getFieldTemplate(fields, 'assignee');

    mappingDataState.selectedProject = projects.value.some(
      p => String(p.key) === String(projectValue)
    )
      ? projectValue
      : '';
    mappingDataState.selectedIssueType = issueTypes.value.some(
      it => String(it.id) === String(issueTypeValue)
    )
      ? issueTypeValue
      : '';
    mappingDataState.selectedAssignee = users.value.some(
      u => String(u.accountId) === String(assigneeValue)
    )
      ? assigneeValue
      : '';

    mappingDataState.summary = getFieldTemplate(fields, 'summary');
    mappingDataState.descriptionFieldMapping = getFieldTemplate(fields, 'description');
  } catch (err) {
    console.error('[Wizard] Failed to fetch projects/issueTypes/users', err);
  }
};

const initCreateMode = (options: UseJiraWebhookPrefillOptions) => {
  options.mappingDataState.selectedProject = '';
  options.mappingDataState.selectedIssueType = '';
  options.mappingDataState.selectedAssignee = '';
  options.mappingDataState.summary = '';
  options.mappingDataState.descriptionFieldMapping = '';
  delete options.mappingDataState.customFields;
  options.mappingDataRef.value = { ...options.mappingDataState };
  options.integrationName.value = '';
  options.webhookDescription.value = '';
  options.activeStep.value = 0;
  options.isBasicDetailsValid.value = false;
  options.isMappingValid.value = false;
};

const prefillBasics = (options: UseJiraWebhookPrefillOptions, data: ConnectionDataPayload) => {
  options.integrationName.value = options.cloneMode.value
    ? `Copy of ${data.name || ''}`
    : data.name || '';
  options.webhookDescription.value = data.description || '';
  options.jsonSample.value = data.samplePayload || '';
  if (data.connectionId) {
    options.originalConnectionIdRef.value = data.connectionId;
    options.connectionData.value.connectionMethod = 'existing';
    options.connectionData.value.existingConnectionId = data.connectionId;
    options.connectionId.value = data.connectionId;
  }
};

const applyMappings = (options: UseJiraWebhookPrefillOptions, data: WebhookMappingPayload) => {
  const fieldsMapping = extractFieldsMapping(data);
  const fields = fieldsMapping as FieldMappingEntry[];
  populateStandardFields(options.mappingDataState, fields);
  populateCustomFields(options.mappingDataState, fieldsMapping);
  options.mappingDataRef.value = { ...options.mappingDataState };
  return fields;
};

const hydrateIfNeeded = async (
  options: UseJiraWebhookPrefillOptions,
  connectionId: string | undefined,
  fields: FieldMappingEntry[]
) => {
  if (!connectionId || !options.mappingDataState.selectedProject) return;
  if (!options.editMode.value && !options.cloneMode.value) return;

  await hydrateDropdownsAndReapplySelections(
    connectionId,
    options.mappingDataState,
    options.projects,
    options.issueTypes,
    options.users,
    fields
  );
  options.mappingDataRef.value = { ...options.mappingDataState };
};

const finalizePrefill = (options: UseJiraWebhookPrefillOptions) => {
  options.isBasicDetailsValid.value = true;
  options.isMappingValid.value = true;
};

const handleClosed = (options: UseJiraWebhookPrefillOptions) => {
  options.resetAllFields(options.editMode.value);
};

const resolvePrefillData = async (options: UseJiraWebhookPrefillOptions) => {
  await options.loadNormalizedNames();
  return getPrefillData(options);
};

const applyPrefill = async (
  options: UseJiraWebhookPrefillOptions,
  data: ConnectionDataPayload & WebhookMappingPayload
) => {
  options.resetAllFields(options.editMode.value);
  prefillBasics(options, data);

  const fields = applyMappings(options, data);
  await hydrateIfNeeded(options, data.connectionId, fields);
  finalizePrefill(options);
};

const handleOpened = async (options: UseJiraWebhookPrefillOptions) => {
  const data = await resolvePrefillData(options);
  if (!data) {
    initCreateMode(options);
    return;
  }

  await applyPrefill(options, data as ConnectionDataPayload & WebhookMappingPayload);
};

export function useJiraWebhookPrefill(options: UseJiraWebhookPrefillOptions) {
  const handleOpenChange = async (isOpen: boolean) => {
    if (!isOpen) {
      handleClosed(options);
      return;
    }
    await handleOpened(options);
  };

  return { handleOpenChange };
}
