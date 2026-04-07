<template>
  <div v-if="open" class="jw-modal-backdrop" @click="handleOverlayClick">
    <div class="jw-modal" @click.stop>
      <header class="jw-header">
        <div class="jw-header-inner">
          <div class="jw-stepper">
            <template v-for="(step, index) in steps" :key="step.key">
              <div class="jw-step">
                <div
                  class="jw-step-indicator"
                  :class="{
                    'jw-active': index === currentStep - 1,
                    'jw-completed': index < currentStep - 1,
                  }"
                >
                  <span v-if="index >= currentStep - 1">{{ index + 1 }}</span>
                  <span v-else class="jw-check">✔</span>
                </div>
                <div class="jw-step-label">{{ step.title }}</div>
              </div>
              <div
                v-if="index < steps.length - 1"
                class="jw-step-line"
                :class="{
                  'jw-line-active': index < currentStep - 1,
                  'jw-line-inactive': index >= currentStep - 1,
                }"
              ></div>
            </template>
          </div>
          <button type="button" class="jw-icon-button" @click="showCancelConfirmation">✕</button>
        </div>
      </header>

      <main class="jw-content">
        <IntegrationDetailsStep
          v-if="currentStep === 1"
          :model-value="unifiedIntegrationStepData"
          :config="CONFLUENCE_UNIFIED_STEP_CONFIG"
          :edit-mode="props.mode === 'edit'"
          :original-name="originalName"
          :normalized-names="nameValidation.allNormalizedNames.value"
          @update:model-value="updateUnifiedIntegrationStepData"
          @validation-change="setStepValidation(1, $event)"
        />
        <ScheduleStep
          v-if="currentStep === 2"
          :model-value="scheduleData"
          @update:model-value="updateScheduleData"
          @validation-change="setStepValidation(2, $event)"
        />
        <ConnectionStep
          v-if="currentStep === 3"
          :model-value="genericConnectionData"
          :config="CONFLUENCE_CONNECTION_CONFIG"
          @update:model-value="handleConnectionDataUpdate"
          @connection-success="handleConnectionSuccess"
          @validation-change="setStepValidation(3, $event)"
        />
        <ConfluenceConfigStep
          v-if="currentStep === 4"
          :model-value="pageConfigData"
          :connection-id="formData.existingConnectionId || formData.createdConnectionId"
          @update:model-value="updatePageConfigData"
          @validation-change="setStepValidation(4, $event)"
        />
        <ReviewSummary
          v-if="currentStep === 5"
          :form-data="formData"
          :is-duplicate-name="nameValidation.isDuplicateName.value"
          @validation-change="setStepValidation(5, $event)"
        />
      </main>

      <footer class="jw-footer">
        <div class="jw-footer-left">
          <button
            v-if="currentStep > 1"
            type="button"
            class="jw-btn jw-btn-outlined"
            @click="previousStep"
          >
            ← Previous
          </button>
        </div>
        <div class="jw-footer-right">
          <button
            type="button"
            class="jw-btn jw-btn-outlined jw-btn-neutral"
            @click="showCancelConfirmation"
          >
            Cancel
          </button>
          <button
            v-if="currentStep < steps.length"
            type="button"
            class="jw-btn jw-btn-primary"
            :disabled="!isStepValid"
            @click="nextStep"
          >
            Next →
          </button>
          <button
            v-else
            type="button"
            class="jw-btn jw-btn-primary"
            :disabled="isSubmitDisabled"
            @click="handleSubmit"
          >
            <span v-if="loading" class="loading-spinner"></span>
            {{ submitLabel }}
          </button>
        </div>
      </footer>
    </div>

    <div v-if="showCancelDialog" class="jw-modal-backdrop">
      <div class="jw-cancel-dialog">
        <div class="jw-cancel-header">
          <div class="jw-cancel-icon">!</div>
          <div class="jw-cancel-title">Warning</div>
        </div>
        <div class="jw-cancel-body">
          <p>Are you sure you want to cancel?</p>
          <p class="jw-cancel-subtitle">All unsaved data will be lost.</p>
        </div>
        <div class="jw-cancel-actions">
          <button
            type="button"
            class="jw-btn jw-btn-outlined jw-btn-neutral"
            @click="confirmCancel"
          >
            Yes, Cancel
          </button>
          <button type="button" class="jw-btn jw-btn-warning" @click="showCancelDialog = false">
            No, Go Back
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref, toRef, watch } from 'vue';
import { useToastStore } from '@/store/toast';
import {
  useConfluenceIntegrationActions,
  useConfluenceIntegrationEditor,
} from '@/composables/useConfluenceIntegrationActions';
import { useConfluenceWizardState } from '@/composables/useConfluenceWizardState';
import { useConfluenceNameValidation } from '@/composables/useConfluenceNameValidation';
import { ConfluenceIntegrationService } from '@/api/services/ConfluenceIntegrationService';
import type { ConfluenceIntegrationResponse } from '@/api/models/ConfluenceIntegrationResponse';
import { createSteps } from './confluenceWizardConfig';
import { CONFLUENCE_CONNECTION_CONFIG } from '@/utils/connectionStepConfig';
import { mapMonthSchedule } from '@/utils/arcgisWizardMappingHelpers';
import { convertUtcTimeToUserTimezone } from '@/utils/scheduleDisplayUtils';
import { CONFLUENCE_UNIFIED_STEP_CONFIG } from '@/utils/unifiedIntegrationStepConfig';

const IntegrationDetailsStep = defineAsyncComponent(
  () => import('@/components/common/combinedstep/IntegrationDetailsStep.vue')
);
const ScheduleStep = defineAsyncComponent(() => import('./steps/ScheduleStep.vue'));
const ConfluenceConfigStep = defineAsyncComponent(() => import('./steps/ConfluenceConfigStep.vue'));
const ConnectionStep = defineAsyncComponent(
  () => import('@/components/common/connectionstep/ConnectionStep.vue')
);
const ReviewSummary = defineAsyncComponent(() => import('./steps/ReviewSummary.vue'));

interface Props {
  open: boolean;
  mode?: 'create' | 'edit' | 'clone';
  integrationId?: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'integration-created'): void;
  (e: 'integration-updated'): void;
}>();

const toast = useToastStore();
const loading = ref(false);
const currentStep = ref(1);
const showCancelDialog = ref(false);
const originalName = ref<string | undefined>(undefined);

const { createIntegrationFromWizard } = useConfluenceIntegrationActions();
const { updateIntegrationFromWizard } = useConfluenceIntegrationEditor();

const steps = computed(() => createSteps(props.mode || 'create'));

// Wizard state
const wizardState = useConfluenceWizardState();
const {
  formData,
  stepValidation,
  unifiedIntegrationStepData,
  scheduleData,
  pageConfigData,
  genericConnectionData,
  updateUnifiedIntegrationStepData,
  updateScheduleData,
  updatePageConfigData,
  handleConnectionDataUpdate,
  setStepValidation,
  resetFormData,
} = wizardState;

// Name validation
const integrationNameRef = toRef(() => formData.name);
const originalNameComputed = computed(() => originalName.value);

const nameValidation = useConfluenceNameValidation({
  integrationName: integrationNameRef,
  editMode: props.mode === 'edit',
  originalName: originalNameComputed,
});

// ── Step navigation ────────────────────────────────────────────────────────

const isStepValid = computed(() => stepValidation[currentStep.value] ?? false);

const submitLabel = computed(() => {
  if (props.mode === 'edit') return loading.value ? 'Updating...' : 'Update Confluence Integration';
  if (props.mode === 'clone') return loading.value ? 'Cloning...' : 'Clone Confluence Integration';
  return loading.value ? 'Creating...' : 'Create Confluence Integration';
});

const isSubmitDisabled = computed(
  () =>
    loading.value ||
    !isStepValid.value ||
    (props.mode !== 'edit' && nameValidation.isDuplicateName.value)
);

function nextStep() {
  if (!isStepValid.value) return;
  if (currentStep.value < steps.value.length) currentStep.value++;
}

function previousStep() {
  if (currentStep.value > 1) currentStep.value--;
}

/**
 * Called when the connection step reports a successful test/verification.
 */
function handleConnectionSuccess(_connectionId: string) {
  // Connection base URL is tracked via genericConnectionData.baseUrl
}

// ── Cancel / close ─────────────────────────────────────────────────────────

function resetWizard() {
  resetFormData();
  currentStep.value = 1;
  originalName.value = undefined;
}

function closeWizard() {
  resetWizard();
  emit('close');
}

function showCancelConfirmation() {
  showCancelDialog.value = true;
}

function confirmCancel() {
  showCancelDialog.value = false;
  closeWizard();
}

function handleOverlayClick(e: MouseEvent) {
  if (e.target === e.currentTarget) closeWizard();
}

// ── Submit ─────────────────────────────────────────────────────────────────

async function handleSubmit() {
  if (!isStepValid.value) return;
  loading.value = true;
  try {
    if (props.mode === 'edit' && props.integrationId) {
      const success = await updateIntegrationFromWizard(
        props.integrationId,
        formData,
        genericConnectionData.value.baseUrl
      );
      if (success) {
        emit('integration-updated');
        closeWizard();
      }
    } else {
      const newId = await createIntegrationFromWizard(
        formData,
        genericConnectionData.value.baseUrl
      );
      if (newId) {
        emit('integration-created');
        closeWizard();
      }
    }
  } catch (e: unknown) {
    const msg = (e as { message?: string })?.message || 'Operation failed';
    toast.showError(msg);
  } finally {
    loading.value = false;
  }
}

// eslint-disable-next-line complexity
function buildPrefillFormData(detail: ConfluenceIntegrationResponse, clone: boolean) {
  return {
    name: clone ? `Copy of ${detail.name}` : detail.name,
    description: detail.description || '',
    itemType: detail.itemType || 'DOCUMENT',
    subType: detail.itemSubtype || '',
    subTypeLabel: detail.itemSubtypeLabel || '',
    dynamicDocument: detail.dynamicDocumentType || '',
    dynamicDocumentLabel: detail.dynamicDocumentTypeLabel || '',
    languageCodes: detail.languageCodes || [],
    reportNameTemplate: detail.reportNameTemplate || '',
    includeTableOfContents: detail.includeTableOfContents ?? true,
    ...buildPrefillSchedule(detail),
    confluenceSpaceKey: detail.confluenceSpaceKey || '',
    confluenceSpaceKeyFolderKey: detail.confluenceSpaceKeyFolderKey || 'ROOT',
    connectionMethod: 'existing' as const,
    existingConnectionId: detail.connectionId || '',
    createdConnectionId: '',
  };
}

// eslint-disable-next-line complexity
function buildPrefillSchedule(detail: ConfluenceIntegrationResponse) {
  return {
    executionTime: detail.schedule?.executionTime
      ? convertUtcTimeToUserTimezone(
          detail.schedule.executionTime,
          detail.schedule.executionDate ?? undefined
        )
      : '02:00',
    frequencyPattern: detail.schedule?.frequencyPattern || 'DAILY',
    executionDate: detail.schedule?.executionDate || null,
    dailyFrequency: detail.schedule?.dailyExecutionInterval?.toString() || '24',
    selectedDays: detail.schedule?.daySchedule || [],
    selectedMonths: detail.schedule?.monthSchedule
      ? mapMonthSchedule(detail.schedule.monthSchedule)
      : [],
    isExecuteOnMonthEnd: detail.schedule?.isExecuteOnMonthEnd || false,
    cronExpression: detail.schedule?.cronExpression || undefined,
    businessTimeZone: detail.schedule?.businessTimeZone || 'UTC',
    timeCalculationMode: detail.schedule?.timeCalculationMode || 'FIXED_DAY_BOUNDARY',
  };
}

// ── Prefill for edit / clone ───────────────────────────────────────────────

async function prefill(integrationId: string, clone: boolean) {
  loading.value = true;
  try {
    const detail = await ConfluenceIntegrationService.getConfluenceIntegrationById(integrationId);
    originalName.value = detail.name;
    Object.assign(formData, buildPrefillFormData(detail, clone));
    Object.keys(stepValidation).forEach(k => {
      stepValidation[+k] = true;
    });
    if (!clone) setStepValidation(3, !!detail.connectionId);
  } catch (error: unknown) {
    const msg = (error as { message?: string })?.message || 'Failed to load integration details';
    toast.showError(msg);
  } finally {
    loading.value = false;
  }
}

// ── Watchers ───────────────────────────────────────────────────────────────

watch(
  () => props.open,
  async isOpen => {
    document.body.style.overflow = isOpen ? 'hidden' : '';
    if (isOpen) {
      if (props.integrationId && props.mode !== 'create') {
        await prefill(props.integrationId, props.mode === 'clone');
      }
      await nameValidation.loadNormalizedNames();
    }
  }
);
</script>

<style src="@/components/outbound/arcgisintegration/wizard/ArcGISIntegrationWizard.css"></style>
