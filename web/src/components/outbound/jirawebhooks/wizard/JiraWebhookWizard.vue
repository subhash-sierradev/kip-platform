<!-- JiraWebhookWizard.vue -->
<template>
  <div v-if="open">
    <!-- Main overlay dialog: hide when showing success -->
    <div v-if="!showSuccess" class="jw-modal-backdrop">
      <div class="jw-modal">
        <!-- Sticky Stepper Header -->
        <JiraWizardStepperHeader
          :steps="steps"
          :active-step="activeStep"
          @close="showCancelConfirm = true"
        />

        <!-- Scrollable Content -->
        <main class="jw-content">
          <!-- Step 0: Basic Details -->
          <BasicDetailsStep
            v-if="activeStep === 0"
            :integrationName="integrationName"
            :description="webhookDescription"
            :editMode="editMode"
            :cloneMode="cloneMode"
            :mode="editMode ? 'edit' : 'add'"
            :originalName="originalNameForValidation"
            :webhookUrl="editWebhookUrl"
            :normalizedNames="allWebhookNormalizedNames"
            @update:integrationName="onIntegrationNameUpdate"
            @update:description="onDescriptionUpdate"
            @validation-change="onBasicValidationChange"
          />

          <!-- Step 1: Sample Payload -->
          <SampleDataStep
            v-else-if="activeStep === 1"
            :webhookUrl="connectionData.baseUrl"
            :verifyHmac="verifyHmac"
            :hmacKey="hmacKey"
            :sampleCaptured="sampleCaptured"
            :showJsonInput="showJsonInput"
            :jsonSample="jsonSample"
            @update:verifyHmac="onVerifyHmacUpdate"
            @update:hmacKey="onHmacKeyUpdate"
            @update:sampleCaptured="onSampleCapturedUpdate"
            @update:showJsonInput="onShowJsonInputUpdate"
            @update:jsonSample="onJsonSampleUpdate"
            @json-validity-change="onJsonValidityChange"
          />

          <!-- Step 2: Connect Jira -->
          <ConnectionStep
            v-else-if="activeStep === 2"
            v-model="connectionData"
            :config="JIRA_CONNECTION_CONFIG"
            @validation-change="handleConnectionValidation"
            @connection-success="handleConnectionSuccess"
          />

          <!-- Step 3: Field Mapping -->
          <MappingStep
            v-else-if="activeStep === 3"
            :jsonSample="jsonSample"
            :jiraConnectionRequest="mappingModeEdit ? undefined : (jiraConnectionRequest as any)"
            :connectionId="connectionId"
            :mappingData="mappingData"
            :projects="mappingModeEdit ? projects : undefined"
            :issueTypes="mappingModeEdit ? issueTypes : undefined"
            :users="mappingModeEdit ? users : undefined"
            :editMode="mappingModeEdit"
            :cloneMode="false"
            @validation-change="onMappingValidationUpdate"
            @update:mappingData="onMappingDataUpdate"
            @projects-change="onProjectsChange"
            @issueTypes-change="onIssueTypesChange"
            @users-change="onUsersChange"
          />

          <!-- Step 4: Review & Create -->
          <PreviewStep
            v-else-if="activeStep === 4"
            :isDuplicateName="isDuplicateName"
            :integrationName="integrationName"
            :description="webhookDescription"
            :jsonSample="jsonSample"
            :mappingData="mappingData"
            :projects="projects"
            :issueTypes="issueTypes"
            :users="users"
            :sprints="previewSprintOptions"
          />
        </main>

        <!-- Sticky Footer -->
        <JiraWizardFooterActions
          :active-step="activeStep"
          :total-steps="steps.length"
          :next-disabled="!isStepValid(activeStep)"
          :submit-disabled="isSubmitDisabled"
          :submit-label="submitButtonLabel"
          @previous="handlePrevious"
          @next="handleNext"
          @cancel="handleCancel"
        />
      </div>
    </div>

    <!-- Success Dialog -->
    <WebhookSuccessDialog
      v-if="showSuccess"
      :open="showSuccess"
      :webhook="createdWebhook ?? undefined"
      :editMode="editMode && !cloneMode"
      @close="handleSuccessClose"
      @copy-url="handleCopyWebhookUrl"
    />

    <!-- Cancel Confirmation Dialog -->
    <JiraWizardCancelDialog
      :open="showCancelConfirm"
      @confirm="confirmCancel"
      @close="showCancelConfirm = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, toRef, watch } from 'vue';

/* ---------- CHILD COMPONENTS ---------- */
import BasicDetailsStep from './BasicDetailsStep.vue';
import SampleDataStep from './SampleDataStep.vue';
import ConnectionStep from '@/components/common/connectionstep/ConnectionStep.vue';
import MappingStep from './MappingStep.vue';
import PreviewStep from './PreviewStep.vue';
import WebhookSuccessDialog from './WebhookSuccessDialog.vue';
import JiraWizardStepperHeader from './JiraWizardStepperHeader.vue';
import JiraWizardFooterActions from './JiraWizardFooterActions.vue';
import JiraWizardCancelDialog from './JiraWizardCancelDialog.vue';

/* ---------- SERVICES & UTILS ---------- */
import { useToastStore } from '@/store/toast';
import { JIRA_CONNECTION_CONFIG } from '@/utils/connectionStepConfig';
import { getJiraWebhookSteps } from '../utils/jiraWebhookWizardConfig';
import { useJiraWebhookWizardState } from '@/composables/useJiraWebhookWizardState';
import { useJiraWebhookNameValidation } from '@/composables/useJiraWebhookNameValidation';
import { useJiraWebhookPrefill } from '@/composables/useJiraWebhookPrefill';
import { useJiraWebhookConnection } from '@/composables/useJiraWebhookConnection';
import { useJiraWebhookSubmit } from '@/composables/useJiraWebhookSubmit';
import { useJiraSprints } from '@/composables/useJiraSprints';
import type { MappingData } from '@/api/models/jirawebhook/MappingData';
import type { JiraWebhook } from '@/types/JiraWebhook';
import type { JiraWebhookDetail } from '@/api/services/JiraWebhookService';
import { JiraConnectionRequest } from '@/api/models/jirawebhook/JiraConnectionRequest';

const props = defineProps<{
  open: boolean;
  editMode?: boolean;
  editingWebhookData?: JiraWebhook | JiraWebhookDetail;
  cloneMode?: boolean;
  cloningWebhookData?: JiraWebhook | JiraWebhookDetail;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'webhook-created'): void;
}>();

/* ---------- TOAST ---------- */
let toastStore: ReturnType<typeof useToastStore> | null = null;
try {
  toastStore = useToastStore();
} catch {
  toastStore = null;
}

function showToast(message: string, type: 'success' | 'error' | 'warning') {
  if (!toastStore) {
    console.warn(`[${type.toUpperCase()}] ${message}`);
    return;
  }

  if (type === 'success') {
    toastStore.showSuccess(message);
  } else if (type === 'error') {
    toastStore.showError(message);
  } else {
    toastStore.showWarning(message);
  }
}

/* ---------- STATE ---------- */
const wizardState = useJiraWebhookWizardState({ editMode: props.editMode });

const {
  activeStep,
  isCreating,
  showSuccess,
  createdWebhook,
  showCancelConfirm,
  integrationName,
  webhookDescription,
  isBasicDetailsValid,
  verifyHmac,
  hmacKey,
  sampleCaptured,
  showJsonInput,
  jsonSample,
  connectionData,
  jiraConnected,
  connectionId,
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
  updateMappingData,
  setIntegrationName,
  setWebhookDescription,
  setBasicDetailsValid,
  setVerifyHmac,
  setHmacKey,
  setSampleCaptured,
  setShowJsonInput,
  setJsonSample,
  setJsonValid,
  setMappingValid,
  setProjects,
  setIssueTypes,
  setUsers,
  resetAllFields,
  isStepValid,
} = wizardState;

const originalNameForValidation = computed(() => {
  if (!props.editMode || !props.editingWebhookData) return undefined;
  return props.editingWebhookData.name;
});

const editWebhookUrl = computed(() => {
  if (!props.editMode || !props.editingWebhookData) return undefined;
  return props.editingWebhookData.webhookUrl;
});

const { allWebhookNormalizedNames, isDuplicateName, loadNormalizedNames } =
  useJiraWebhookNameValidation({
    integrationName,
    editMode: props.editMode,
    originalName: originalNameForValidation,
  });

const { handleOpenChange } = useJiraWebhookPrefill({
  editMode: toRef(props, 'editMode'),
  cloneMode: toRef(props, 'cloneMode'),
  editingWebhookData: toRef(props, 'editingWebhookData'),
  cloningWebhookData: toRef(props, 'cloningWebhookData'),
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
});

watch(
  () => [props.open, props.editingWebhookData, props.cloningWebhookData],
  ([isOpen]) => {
    void handleOpenChange(!!isOpen);
  },
  { immediate: true }
);

const { handleConnectionValidation, handleConnectionSuccess, handleConnectionChangeCheck } =
  useJiraWebhookConnection({
    editMode: props.editMode,
    cloneMode: props.cloneMode,
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
  });

const {
  isSubmitDisabled,
  submitButtonLabel,
  handleCreateWebhook,
  handleSuccessClose,
  handleCopyWebhookUrl,
} = useJiraWebhookSubmit({
  editMode: props.editMode,
  cloneMode: props.cloneMode,
  editingWebhookData: props.editingWebhookData,
  integrationName,
  webhookDescription,
  isBasicDetailsValid,
  jsonSample,
  connectionId,
  mappingData,
  projects,
  issueTypes,
  users,
  isCreating,
  showSuccess,
  createdWebhook,
  isDuplicateName,
  originalNameForValidation,
  resetAllFields: () => resetAllFields(props.editMode),
  showToast,
  emitClose: () => emit('close'),
  emitCreated: () => emit('webhook-created'),
});

const steps = computed(() => {
  if (props.cloneMode) return getJiraWebhookSteps('clone');
  if (props.editMode) return getJiraWebhookSteps('edit');
  return getJiraWebhookSteps('create');
});

const sprintLookupOpts = {
  connectionId: '',
  projectKey: '',
  state: 'active,future',
};
const sprintLookup = useJiraSprints(sprintLookupOpts);
const previewSprintOptions = computed(() => sprintLookup.options.value);

watch(
  () => ({
    connectionId: connectionId.value,
    projectKey: mappingData.value.selectedProject,
  }),
  async ({ connectionId: nextConnectionId, projectKey: nextProjectKey }) => {
    sprintLookupOpts.connectionId = nextConnectionId ?? '';
    sprintLookupOpts.projectKey = nextProjectKey ?? '';

    if (!sprintLookupOpts.connectionId || !sprintLookupOpts.projectKey) {
      return;
    }

    await sprintLookup.search('');
  },
  { immediate: true }
);

const jiraConnectionRequest = computed<JiraConnectionRequest | undefined>(() => {
  if (
    !connectionData.value.baseUrl ||
    !connectionData.value.username ||
    !connectionData.value.password
  )
    return undefined;

  return {
    jiraBaseUrl: connectionData.value.baseUrl,
    integrationSecret: {
      baseUrl: connectionData.value.baseUrl,
      authType: 'BASIC_AUTH',
      credentials: {
        authType: 'BASIC_AUTH',
        username: connectionData.value.username,
        password: connectionData.value.password,
      },
    },
    organizationKey: '',
  };
});

/* ---------- EVENT HANDLERS ---------- */
const onIntegrationNameUpdate = (val: string) => setIntegrationName(val);
const onDescriptionUpdate = (val: string) => setWebhookDescription(val);
const onBasicValidationChange = (val: boolean) => setBasicDetailsValid(val);

const onVerifyHmacUpdate = (val: boolean) => setVerifyHmac(val);
const onHmacKeyUpdate = (val: string) => setHmacKey(val);
const onSampleCapturedUpdate = (val: boolean) => setSampleCaptured(val);
const onShowJsonInputUpdate = (val: boolean) => setShowJsonInput(val);
const onJsonSampleUpdate = (val: string) => setJsonSample(val);
const onJsonValidityChange = (val: boolean) => setJsonValid(val);

const onMappingValidationUpdate = (val: boolean) => setMappingValid(val);
const onMappingDataUpdate = (val: MappingData) => updateMappingData(val);
const onProjectsChange = (val: Array<{ key: string; name: string }>) => setProjects(val);
const onIssueTypesChange = (val: Array<{ id: string; name: string }>) => setIssueTypes(val);
const onUsersChange = (val: Array<{ accountId: string; displayName: string }>) => setUsers(val);

/* ---------- NAVIGATION ---------- */
const handlePrevious = () => {
  if (activeStep.value > 0) activeStep.value -= 1;
};

const handleNext = async () => {
  if (activeStep.value === 2) handleConnectionChangeCheck();

  if (activeStep.value === steps.value.length - 1) {
    await handleCreateWebhook();
    return;
  }

  activeStep.value += 1;
};

const handleCancel = () => {
  showCancelConfirm.value = true;
};

const confirmCancel = () => {
  showCancelConfirm.value = false;
  resetAllFields(props.editMode);
  emit('close');
};
</script>

<style src="./JiraWebhookWizard.css"></style>
