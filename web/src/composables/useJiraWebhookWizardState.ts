import { computed, reactive, ref } from 'vue';

import { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import { MappingData } from '@/api/models/jirawebhook/MappingData';
import { WebhookCreationResponse } from '@/api/models/jirawebhook/WebhookCreationResponse';
import {
  createDefaultConnectionData,
  createDefaultMappingData,
} from '@/components/outbound/jirawebhooks/utils/jiraWebhookWizardConfig';
import type { ConnectionStepData } from '@/types/ConnectionStepData';

interface UseJiraWebhookWizardStateOptions {
  editMode?: boolean;
}

const createBaseState = () => ({
  activeStep: ref(0),
  isCreating: ref(false),
  showSuccess: ref(false),
  createdWebhook: ref<WebhookCreationResponse | null>(null),
  showCancelConfirm: ref(false),
});

const createBasicDetailsState = () => ({
  integrationName: ref(''),
  webhookDescription: ref(''),
  isBasicDetailsValid: ref(false),
});

const createSampleState = () => ({
  verifyHmac: ref(false),
  hmacKey: ref(''),
  sampleCaptured: ref(false),
  showJsonInput: ref(false),
  jsonSample: ref(''),
  isJsonValid: ref(false),
});

const createConnectionState = () => ({
  connectionData: ref<ConnectionStepData>(createDefaultConnectionData()),
  jiraConnected: ref(false),
  connectionId: ref(''),
});

const createMappingState = (editMode?: boolean) => {
  const mappingDataState = reactive(createDefaultMappingData());
  const mappingDataRef = ref<MappingData>({ ...mappingDataState });
  const mappingData = computed(() => mappingDataRef.value);
  const isMappingValid = ref(false);
  const projects = ref<JiraProject[]>([]);
  const issueTypes = ref<JiraIssueType[]>([]);
  const users = ref<JiraUser[]>([]);
  const mappingModeEdit = ref<boolean>(!!editMode);
  const originalConnectionIdRef = ref('');
  const lastResetConnectionId = ref('');
  const syncMappingDataRef = () => {
    mappingDataRef.value = { ...mappingDataState };
  };

  return {
    mappingDataState,
    mappingDataRef,
    mappingData,
    isMappingValid,
    projects,
    issueTypes,
    users,
    mappingModeEdit,
    originalConnectionIdRef,
    lastResetConnectionId,
    syncMappingDataRef,
  };
};

const createSetters = (
  basic: ReturnType<typeof createBasicDetailsState>,
  sample: ReturnType<typeof createSampleState>,
  mapping: ReturnType<typeof createMappingState>
) => ({
  setIntegrationName: (value: string) => {
    basic.integrationName.value = value;
  },
  setWebhookDescription: (value: string) => {
    basic.webhookDescription.value = value;
  },
  setBasicDetailsValid: (value: boolean) => {
    basic.isBasicDetailsValid.value = value;
  },
  setVerifyHmac: (value: boolean) => {
    sample.verifyHmac.value = value;
  },
  setHmacKey: (value: string) => {
    sample.hmacKey.value = value;
  },
  setSampleCaptured: (value: boolean) => {
    sample.sampleCaptured.value = value;
  },
  setShowJsonInput: (value: boolean) => {
    sample.showJsonInput.value = value;
  },
  setJsonSample: (value: string) => {
    sample.jsonSample.value = value;
  },
  setJsonValid: (value: boolean) => {
    sample.isJsonValid.value = value;
  },
  setMappingValid: (value: boolean) => {
    mapping.isMappingValid.value = value;
  },
  setProjects: (value: JiraProject[]) => {
    mapping.projects.value = value;
  },
  setIssueTypes: (value: JiraIssueType[]) => {
    mapping.issueTypes.value = value;
  },
  setUsers: (value: JiraUser[]) => {
    mapping.users.value = value;
  },
});

const createMappingActions = (
  mapping: ReturnType<typeof createMappingState>,
  connectionId: ReturnType<typeof createConnectionState>['connectionId']
) => ({
  updateMappingData: (value: MappingData) => {
    Object.assign(mapping.mappingDataState, value);
    mapping.syncMappingDataRef();
  },
  resetMappingData: () => {
    Object.assign(mapping.mappingDataState, createDefaultMappingData());
    mapping.mappingDataState.customFields = undefined;
    mapping.projects.value = [];
    mapping.issueTypes.value = [];
    mapping.users.value = [];
    mapping.isMappingValid.value = false;
    mapping.lastResetConnectionId.value = connectionId.value || '';
    mapping.syncMappingDataRef();
  },
});

const createResetAllFields =
  (
    base: ReturnType<typeof createBaseState>,
    basic: ReturnType<typeof createBasicDetailsState>,
    sample: ReturnType<typeof createSampleState>,
    connection: ReturnType<typeof createConnectionState>,
    mapping: ReturnType<typeof createMappingState>
  ) =>
  (editMode?: boolean) => {
    base.activeStep.value = 0;
    basic.integrationName.value = '';
    basic.webhookDescription.value = '';
    sample.verifyHmac.value = false;
    sample.hmacKey.value = '';
    sample.sampleCaptured.value = false;
    sample.showJsonInput.value = false;
    sample.jsonSample.value = '';
    sample.isJsonValid.value = false;

    connection.connectionData.value = createDefaultConnectionData();
    connection.jiraConnected.value = false;
    connection.connectionId.value = '';

    Object.assign(mapping.mappingDataState, createDefaultMappingData());
    mapping.mappingDataState.customFields = undefined; // Remove customFields when resetting to default
    mapping.syncMappingDataRef();

    mapping.projects.value = [];
    mapping.issueTypes.value = [];
    mapping.users.value = [];

    mapping.originalConnectionIdRef.value = '';
    mapping.mappingModeEdit.value = !!editMode;

    basic.isBasicDetailsValid.value = false;
    mapping.isMappingValid.value = false;
  };

const createIsStepValid =
  (
    basic: ReturnType<typeof createBasicDetailsState>,
    sample: ReturnType<typeof createSampleState>,
    connection: ReturnType<typeof createConnectionState>,
    mapping: ReturnType<typeof createMappingState>
  ) =>
  (stepIndex: number): boolean => {
    const validators = [
      () => basic.integrationName.value.trim() !== '' && basic.isBasicDetailsValid.value,
      () => sample.jsonSample.value.trim() !== '' && sample.isJsonValid.value,
      () => connection.jiraConnected.value && connection.connectionId.value.trim() !== '',
      () => mapping.isMappingValid.value,
      () =>
        connection.jiraConnected.value &&
        connection.connectionId.value !== '' &&
        mapping.mappingData.value.selectedProject !== '' &&
        mapping.mappingData.value.selectedIssueType !== '' &&
        mapping.mappingData.value.summary.trim() !== '',
    ];

    return validators[stepIndex]?.() ?? false;
  };

export function useJiraWebhookWizardState(options: UseJiraWebhookWizardStateOptions) {
  const base = createBaseState();
  const basic = createBasicDetailsState();
  const sample = createSampleState();
  const connection = createConnectionState();
  const mapping = createMappingState(options.editMode);
  const setters = createSetters(basic, sample, mapping);
  const mappingActions = createMappingActions(mapping, connection.connectionId);
  const resetAllFields = createResetAllFields(base, basic, sample, connection, mapping);
  const isStepValid = createIsStepValid(basic, sample, connection, mapping);

  return {
    ...base,
    ...basic,
    ...sample,
    ...connection,
    ...mapping,
    ...setters,
    ...mappingActions,
    resetAllFields,
    isStepValid,
  };
}
