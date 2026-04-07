import { computed, type Ref } from 'vue';

import { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import { JiraWebhookCreateUpdateRequest } from '@/api/models/jirawebhook/JiraWebhookCreateUpdateRequest';
import { MappingData } from '@/api/models/jirawebhook/MappingData';
import { WebhookCreationResponse } from '@/api/models/jirawebhook/WebhookCreationResponse';
import type {
  JiraFieldMapping as ServiceJiraFieldMapping,
  JiraWebhookDetail,
} from '@/api/services/JiraWebhookService';
import { JiraWebhookService } from '@/api/services/JiraWebhookService';
import type { JiraWebhook } from '@/types/JiraWebhook';
import { normalizeIntegrationNameForCompare } from '@/utils/globalNormalizedUtils';
import { createFieldMappings } from '@/utils/jiraFieldMappingUtils';
import { isSubtaskIssueType } from '@/utils/subtaskIssueType';

interface UseJiraWebhookSubmitOptions {
  editMode?: boolean;
  cloneMode?: boolean;
  editingWebhookData?: JiraWebhook | JiraWebhookDetail;
  integrationName: Ref<string>;
  webhookDescription: Ref<string>;
  isBasicDetailsValid: Ref<boolean>;
  jsonSample: Ref<string>;
  connectionId: Ref<string>;
  mappingData: Ref<MappingData>;
  projects: Ref<JiraProject[]>;
  issueTypes: Ref<JiraIssueType[]>;
  users: Ref<JiraUser[]>;
  isCreating: Ref<boolean>;
  showSuccess: Ref<boolean>;
  createdWebhook: Ref<WebhookCreationResponse | null>;
  isDuplicateName: Ref<boolean>;
  originalNameForValidation: Ref<string | undefined>;
  resetAllFields: () => void;
  showToast: (message: string, type: 'success' | 'error' | 'warning') => void;
  emitClose: () => void;
  emitCreated: () => void;
}

export function useJiraWebhookSubmit(options: UseJiraWebhookSubmitOptions) {
  const canCreateWebhook = buildCanCreateWebhook(options);
  const isSubmitDisabled = buildSubmitDisabled(options, canCreateWebhook);
  const submitButtonLabel = buildSubmitLabel(options);

  const handleCreateWebhook = createSubmitHandler(options, canCreateWebhook);

  return {
    canCreateWebhook,
    isSubmitDisabled,
    submitButtonLabel,
    handleCreateWebhook,
    handleSuccessClose: () => handleSuccessClose(options),
    handleCopyWebhookUrl: () => handleCopyWebhookUrl(options),
  };
}

const buildCanCreateWebhook = (options: UseJiraWebhookSubmitOptions) =>
  computed(
    () =>
      options.integrationName.value.trim() !== '' &&
      options.isBasicDetailsValid.value &&
      options.connectionId.value.trim() !== '' &&
      options.mappingData.value.selectedProject.trim() !== '' &&
      options.mappingData.value.selectedIssueType.trim() !== '' &&
      options.mappingData.value.summary.trim() !== '' &&
      options.jsonSample.value.trim() !== ''
  );

const buildSubmitDisabled = (
  options: UseJiraWebhookSubmitOptions,
  canCreateWebhook: ReturnType<typeof buildCanCreateWebhook>
) =>
  computed(
    () =>
      !canCreateWebhook.value ||
      options.isCreating.value ||
      (!options.editMode && options.isDuplicateName.value)
  );

const buildSubmitLabel = (options: UseJiraWebhookSubmitOptions) =>
  computed(() => {
    if (options.isCreating.value) {
      return options.editMode && !options.cloneMode ? 'Updating...' : 'Creating...';
    }
    if (options.editMode && !options.cloneMode) return 'Update Webhook';
    if (options.cloneMode) return 'Create Webhook';
    return 'Create Webhook';
  });

const buildFieldMappings = (options: UseJiraWebhookSubmitOptions) =>
  createFieldMappings(
    options.mappingData.value,
    options.projects.value,
    options.issueTypes.value,
    options.users.value
  );

const ensureValidMappings = (
  options: UseJiraWebhookSubmitOptions,
  fieldMappings: ServiceJiraFieldMapping[]
) => {
  if (!fieldMappings || fieldMappings.length < 3) {
    options.showToast('At least 3 field mappings are required.', 'error');
    return false;
  }
  return true;
};

const isSubtaskSelected = (options: UseJiraWebhookSubmitOptions) => {
  return isSubtaskIssueType(options.issueTypes.value, options.mappingData.value.selectedIssueType);
};

const ensureValidSubtaskParent = (
  options: UseJiraWebhookSubmitOptions,
  fieldMappings: ServiceJiraFieldMapping[]
) => {
  if (!isSubtaskSelected(options)) {
    return true;
  }

  const parentMapping = fieldMappings.find(
    mapping =>
      String(mapping.jiraFieldId || '')
        .trim()
        .toLowerCase() === 'parent'
  );

  if (!parentMapping?.template?.trim()) {
    options.showToast('Parent field is required for Subtask issue type.', 'error');
    return false;
  }

  return true;
};

const ensureValidJson = (options: UseJiraWebhookSubmitOptions) => {
  try {
    JSON.parse(options.jsonSample.value);
    return true;
  } catch {
    options.showToast('Invalid JSON payload.', 'error');
    return false;
  }
};

const buildWebhookRequest = (
  options: UseJiraWebhookSubmitOptions,
  fieldMappings: ServiceJiraFieldMapping[]
): JiraWebhookCreateUpdateRequest => ({
  name: options.integrationName.value.trim(),
  connectionId: options.connectionId.value,
  description: options.webhookDescription.value.trim() || undefined,
  fieldsMapping: fieldMappings,
  samplePayload: options.jsonSample.value,
});

const hasDuplicateName = async (options: UseJiraWebhookSubmitOptions) => {
  const currentDbNames = await JiraWebhookService.getAllJiraNormalizedNames();
  const normalizedCurrentName = normalizeIntegrationNameForCompare(
    options.integrationName.value.trim()
  );
  const normalizedOriginalName = normalizeIntegrationNameForCompare(
    options.originalNameForValidation.value || ''
  );

  return currentDbNames.some((dbName: string) => {
    const normalizedDbName = normalizeIntegrationNameForCompare(dbName);
    if (options.editMode && normalizedDbName === normalizedOriginalName) {
      return false;
    }
    return normalizedDbName === normalizedCurrentName;
  });
};

const handleEdit = async (
  options: UseJiraWebhookSubmitOptions,
  webhookRequest: JiraWebhookCreateUpdateRequest
) => {
  const editId = String((options.editingWebhookData as JiraWebhook).id || '');
  await JiraWebhookService.updateWebhook(editId, webhookRequest);

  options.showToast(`Jira Webhook "${webhookRequest.name}" updated successfully`, 'success');
  options.resetAllFields();
  options.emitClose();
  options.emitCreated();
};

const handleCreate = async (
  options: UseJiraWebhookSubmitOptions,
  webhookRequest: JiraWebhookCreateUpdateRequest
) => {
  const created = await JiraWebhookService.createWebhook(webhookRequest);
  const result: WebhookCreationResponse = {
    name: created?.name ?? webhookRequest.name,
    webhookUrl: created?.webhookUrl ?? '',
  };

  options.createdWebhook.value = result;
  options.showSuccess.value = true;
};

const createSubmitHandler =
  (
    options: UseJiraWebhookSubmitOptions,
    canCreateWebhook: ReturnType<typeof buildCanCreateWebhook>
  ) =>
  async () => {
    try {
      if (!canCreateWebhook.value) {
        options.showToast('Validation failed.', 'error');
        return;
      }

      const fieldMappings = buildFieldMappings(options);
      if (!ensureValidMappings(options, fieldMappings)) return;
      if (!ensureValidSubtaskParent(options, fieldMappings)) return;
      if (!ensureValidJson(options)) return;

      const webhookRequest = buildWebhookRequest(options, fieldMappings);
      options.isCreating.value = true;

      const isEdit = options.editMode && !options.cloneMode && !!options.editingWebhookData?.id;

      if (await hasDuplicateName(options)) {
        options.isDuplicateName.value = true;
        options.showToast('An integration with this name already exists', 'error');
        return;
      }

      if (isEdit) {
        await handleEdit(options, webhookRequest);
      } else {
        await handleCreate(options, webhookRequest);
      }
    } catch (err) {
      console.error('Webhook creation failed', err);
      options.showToast(
        options.editMode ? 'Failed to update webhook' : 'Failed to create webhook',
        'error'
      );
    } finally {
      options.isCreating.value = false;
    }
  };

const handleSuccessClose = (options: UseJiraWebhookSubmitOptions) => {
  options.showSuccess.value = false;
  options.resetAllFields();
  options.emitClose();
  options.emitCreated();
};

const handleCopyWebhookUrl = async (options: UseJiraWebhookSubmitOptions) => {
  if (!options.createdWebhook.value?.webhookUrl) return;
  await navigator.clipboard.writeText(options.createdWebhook.value.webhookUrl);
  options.showToast('Webhook URL copied', 'success');
};
