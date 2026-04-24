import { beforeEach, describe, expect, it, vi } from 'vitest';
import { computed, ref } from 'vue';

import type { MappingData } from '@/api/models/jirawebhook/MappingData';
import { useJiraWebhookSubmit } from '@/composables/useJiraWebhookSubmit';

const serviceHoisted = vi.hoisted(() => ({
  getAllJiraNormalizedNames: vi.fn(),
  createWebhook: vi.fn(),
  updateWebhook: vi.fn(),
}));

const mappingUtilsHoisted = vi.hoisted(() => ({
  createFieldMappings: vi.fn(),
}));

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    getAllJiraNormalizedNames: serviceHoisted.getAllJiraNormalizedNames,
    createWebhook: serviceHoisted.createWebhook,
    updateWebhook: serviceHoisted.updateWebhook,
  },
}));

vi.mock('@/utils/jiraFieldMappingUtils', () => ({
  createFieldMappings: mappingUtilsHoisted.createFieldMappings,
}));

beforeEach(() => {
  vi.clearAllMocks();
  mappingUtilsHoisted.createFieldMappings.mockReturnValue([
    { jiraFieldId: 'project' },
    { jiraFieldId: 'issuetype' },
    { jiraFieldId: 'summary' },
  ]);
});

describe('useJiraWebhookSubmit', () => {
  const baseMapping: MappingData = {
    selectedProject: 'PROJ',
    selectedIssueType: 'ISSUE',
    selectedAssignee: '',
    summary: 'Summary',
    descriptionFieldMapping: '',
  };

  it('creates a webhook and shows success dialog', async () => {
    serviceHoisted.getAllJiraNormalizedNames.mockResolvedValueOnce([]);
    serviceHoisted.createWebhook.mockResolvedValueOnce({ name: 'Test', webhookUrl: 'http://hook' });

    const integrationName = ref('Test');
    const webhookDescription = ref('');
    const jsonSample = ref('{"ok":true}');
    const connectionId = ref('conn-1');
    const mappingData = ref({ ...baseMapping });

    const isCreating = ref(false);
    const showSuccess = ref(false);
    const createdWebhook = ref(null);
    const isDuplicateName = ref(false);

    const showToast = vi.fn();
    const emitClose = vi.fn();
    const emitCreated = vi.fn();
    const resetAllFields = vi.fn();

    const { handleCreateWebhook } = useJiraWebhookSubmit({
      editMode: false,
      cloneMode: false,
      integrationName,
      webhookDescription,
      isBasicDetailsValid: ref(true),
      jsonSample,
      connectionId,
      mappingData,
      projects: ref([]),
      issueTypes: ref([]),
      users: ref([]),
      selectedParentLabel: ref(''),
      isCreating,
      showSuccess,
      createdWebhook,
      isDuplicateName,
      originalNameForValidation: computed(() => undefined),
      resetAllFields,
      showToast,
      emitClose,
      emitCreated,
    });

    await handleCreateWebhook();

    expect(serviceHoisted.createWebhook).toHaveBeenCalledTimes(1);
    expect(showSuccess.value).toBe(true);
    expect(createdWebhook.value).toEqual({ name: 'Test', webhookUrl: 'http://hook' });
    expect(emitClose).not.toHaveBeenCalled();
    expect(resetAllFields).not.toHaveBeenCalled();
  });

  it('updates webhook when in edit mode', async () => {
    serviceHoisted.getAllJiraNormalizedNames.mockResolvedValueOnce([]);
    serviceHoisted.updateWebhook.mockResolvedValueOnce(undefined);

    const integrationName = ref('Test');
    const webhookDescription = ref('');
    const jsonSample = ref('{"ok":true}');
    const connectionId = ref('conn-1');
    const mappingData = ref({ ...baseMapping });

    const isCreating = ref(false);
    const showSuccess = ref(false);
    const createdWebhook = ref(null);
    const isDuplicateName = ref(false);

    const showToast = vi.fn();
    const emitClose = vi.fn();
    const emitCreated = vi.fn();
    const resetAllFields = vi.fn();

    const { handleCreateWebhook } = useJiraWebhookSubmit({
      editMode: true,
      cloneMode: false,
      editingWebhookData: { id: '123' } as any,
      integrationName,
      webhookDescription,
      isBasicDetailsValid: ref(true),
      jsonSample,
      connectionId,
      mappingData,
      projects: ref([]),
      issueTypes: ref([]),
      users: ref([]),
      selectedParentLabel: ref(''),
      isCreating,
      showSuccess,
      createdWebhook,
      isDuplicateName,
      originalNameForValidation: computed(() => 'Old'),
      resetAllFields,
      showToast,
      emitClose,
      emitCreated,
    });

    await handleCreateWebhook();

    expect(serviceHoisted.updateWebhook).toHaveBeenCalledWith('123', expect.any(Object));
    expect(showToast).toHaveBeenCalledWith('Jira Webhook "Test" updated successfully', 'success');
    expect(emitClose).toHaveBeenCalled();
    expect(emitCreated).toHaveBeenCalled();
    expect(showSuccess.value).toBe(false);
  });

  it('blocks duplicate names', async () => {
    serviceHoisted.getAllJiraNormalizedNames.mockResolvedValueOnce(['Test']);

    const integrationName = ref('Test');
    const webhookDescription = ref('');
    const jsonSample = ref('{"ok":true}');
    const connectionId = ref('conn-1');
    const mappingData = ref({ ...baseMapping });

    const isCreating = ref(false);
    const showSuccess = ref(false);
    const createdWebhook = ref(null);
    const isDuplicateName = ref(false);

    const showToast = vi.fn();

    const { handleCreateWebhook } = useJiraWebhookSubmit({
      editMode: false,
      cloneMode: false,
      integrationName,
      webhookDescription,
      isBasicDetailsValid: ref(true),
      jsonSample,
      connectionId,
      mappingData,
      projects: ref([]),
      issueTypes: ref([]),
      users: ref([]),
      selectedParentLabel: ref(''),
      isCreating,
      showSuccess,
      createdWebhook,
      isDuplicateName,
      originalNameForValidation: computed(() => undefined),
      resetAllFields: vi.fn(),
      showToast,
      emitClose: vi.fn(),
      emitCreated: vi.fn(),
    });

    await handleCreateWebhook();

    expect(isDuplicateName.value).toBe(true);
    expect(showToast).toHaveBeenCalledWith('An integration with this name already exists', 'error');
    expect(serviceHoisted.createWebhook).not.toHaveBeenCalled();
  });

  it('requires parent mapping for subtask when flag is true', async () => {
    serviceHoisted.getAllJiraNormalizedNames.mockResolvedValueOnce([]);

    const showToast = vi.fn();
    const { handleCreateWebhook } = useJiraWebhookSubmit({
      integrationName: ref('Test'),
      webhookDescription: ref(''),
      isBasicDetailsValid: ref(true),
      jsonSample: ref('{"ok":true}'),
      connectionId: ref('conn-1'),
      mappingData: ref({ ...baseMapping, selectedIssueType: 'SUB' }),
      projects: ref([]),
      issueTypes: ref([{ id: 'SUB', name: 'Sub-task', subtask: true }] as any),
      users: ref([]),
      selectedParentLabel: ref(''),
      isCreating: ref(false),
      showSuccess: ref(false),
      createdWebhook: ref(null),
      isDuplicateName: ref(false),
      originalNameForValidation: computed(() => undefined),
      resetAllFields: vi.fn(),
      showToast,
      emitClose: vi.fn(),
      emitCreated: vi.fn(),
    });

    await handleCreateWebhook();

    expect(showToast).toHaveBeenCalledWith(
      'Parent field is required for Subtask issue type.',
      'error'
    );
    expect(serviceHoisted.createWebhook).not.toHaveBeenCalled();
  });

  it('uses name fallback for subtask when flag is missing', async () => {
    serviceHoisted.getAllJiraNormalizedNames.mockResolvedValueOnce([]);

    const showToast = vi.fn();
    const { handleCreateWebhook } = useJiraWebhookSubmit({
      integrationName: ref('Test'),
      webhookDescription: ref(''),
      isBasicDetailsValid: ref(true),
      jsonSample: ref('{"ok":true}'),
      connectionId: ref('conn-1'),
      mappingData: ref({ ...baseMapping, selectedIssueType: 'SUB' }),
      projects: ref([]),
      issueTypes: ref([{ id: 'SUB', name: 'Sub Task' }] as any),
      users: ref([]),
      selectedParentLabel: ref(''),
      isCreating: ref(false),
      showSuccess: ref(false),
      createdWebhook: ref(null),
      isDuplicateName: ref(false),
      originalNameForValidation: computed(() => undefined),
      resetAllFields: vi.fn(),
      showToast,
      emitClose: vi.fn(),
      emitCreated: vi.fn(),
    });

    await handleCreateWebhook();

    expect(showToast).toHaveBeenCalledWith(
      'Parent field is required for Subtask issue type.',
      'error'
    );
    expect(serviceHoisted.createWebhook).not.toHaveBeenCalled();
  });
});
