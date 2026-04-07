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
          :config="ARCGIS_UNIFIED_STEP_CONFIG"
          :editMode="props.mode === 'edit'"
          :originalName="originalIntegrationName"
          :normalizedNames="nameValidation.allNormalizedNames.value"
          @update:model-value="updateUnifiedIntegrationStepData"
          @validation-change="setStepValidation(1, $event)"
        />
        <ScheduleConfigurationStep
          v-if="currentStep === 2"
          :model-value="scheduleConfigurationData"
          @update:model-value="updateScheduleConfigurationData"
          @validation-change="setStepValidation(2, $event)"
        />

        <ConnectionStep
          v-if="currentStep === 3"
          :model-value="genericConnectionData"
          :config="ARCGIS_CONNECTION_CONFIG"
          @update:model-value="handleConnectionDataUpdate"
          @validation-change="setStepValidation(3, $event)"
        />
        <FieldMappingStep
          v-if="currentStep === 4"
          :model-value="fieldMappingData"
          :connection-id="activeConnectionId"
          :mode="props.mode"
          @update:model-value="updateFieldMappingData"
          @validation-change="setStepValidation(4, $event)"
        />
        <ReviewSummary
          v-if="currentStep === 5"
          :form-data="formData"
          :is-active="true"
          :is-duplicate-name="isDuplicateName"
          @update:is-active="() => {}"
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
import { ref, watch, computed, nextTick, defineAsyncComponent } from 'vue';
import { useToastStore } from '../../../../store/toast';
import {
  useArcGISIntegrationActions,
  useArcGISIntegrationEditor,
} from '../../../../composables/useArcGISIntegrationActions';
import { ArcGISIntegrationService } from '../../../../api/services/ArcGISIntegrationService';
import { transformFieldMappingsForEdit, ensureEmptyMappingSlot } from '@/utils/fieldMappingHelpers';
import {
  mapBasicDetails,
  mapDocumentSelection,
  mapScheduleData,
  mapConnectionData,
} from '../../../../utils/arcgisWizardMappingHelpers';
import { useIntegrationNameValidation } from '@/composables/useIntegrationNameValidation';
import { useArcGISWizardState } from '@/composables/useArcGISWizardState';
import { ARCGIS_UNIFIED_STEP_CONFIG } from '@/utils/unifiedIntegrationStepConfig';

import { createSteps } from './arcgisWizardConfig';
import { ARCGIS_CONNECTION_CONFIG } from '@/utils/connectionStepConfig';

const IntegrationDetailsStep = defineAsyncComponent(
  () => import('@/components/common/combinedstep/IntegrationDetailsStep.vue')
);
const ScheduleConfigurationStep = defineAsyncComponent(
  () => import('./steps/ScheduleConfigurationStep.vue')
);
const ConnectionStep = defineAsyncComponent(
  () => import('@/components/common/connectionstep/ConnectionStep.vue')
);
const FieldMappingStep = defineAsyncComponent(() => import('./steps/FieldMappingStep.vue'));
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

const { createIntegrationFromWizard } = useArcGISIntegrationActions();
const { updateIntegrationFromWizard } = useArcGISIntegrationEditor();

const steps = computed(() => createSteps(props.mode || 'create'));

// Wizard state management
const wizardState = useArcGISWizardState();
const {
  formData,
  stepValidation,
  unifiedIntegrationStepData,
  scheduleConfigurationData,
  genericConnectionData,
  fieldMappingData,
  activeConnectionId,
  updateUnifiedIntegrationStepData,
  updateScheduleConfigurationData,
  handleConnectionDataUpdate,
  updateFieldMappingData,
  setStepValidation,
  resetFormData,
} = wizardState;
// Integration name validation
const nameValidation = useIntegrationNameValidation({
  mode: props.mode,
  integrationNameGetter: () => formData.name,
  getAllNamesFunction: ArcGISIntegrationService.getAllArcGISNormalizedNames,
});

const {
  isDuplicateName,
  originalName: originalIntegrationName,
  loadAllNames,
  validateBeforeSubmit,
  setupNameWatcher,
} = nameValidation;

const isStepValid = computed(() => stepValidation[currentStep.value] ?? false);

const submitLabel = computed(() => {
  if (props.mode === 'edit') return loading.value ? 'Updating...' : 'Update ArcGIS Integration';
  if (props.mode === 'clone') return loading.value ? 'Cloning...' : 'Clone ArcGIS Integration';
  return loading.value ? 'Creating...' : 'Create ArcGIS Integration';
});

const isSubmitDisabled = computed(() => {
  return loading.value || !isStepValid.value || (props.mode !== 'edit' && isDuplicateName.value);
});

function resetWizard() {
  isDuplicateName.value = false;
  resetFormData();
  currentStep.value = 1;
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
function nextStep() {
  if (!isStepValid.value) {
    return;
  }

  const totalSteps = steps.value.length;

  // If the steps array has changed and currentStep is now out of range,
  // clamp it back into the valid range instead of advancing.
  if (totalSteps <= 0) {
    return;
  }

  if (currentStep.value > totalSteps) {
    currentStep.value = totalSteps;
    return;
  }

  if (currentStep.value < totalSteps) {
    currentStep.value++;
  }
}
function previousStep() {
  if (currentStep.value > 1) currentStep.value--;
}
function handleOverlayClick(e: Event) {
  if (e.target === e.currentTarget) closeWizard();
}

async function handleSubmit() {
  if (!isStepValid.value) return;
  loading.value = true;
  try {
    // Re-validate name against current DB state before submit
    const hasDuplicate = await validateBeforeSubmit(formData.name);

    if (hasDuplicate) {
      isDuplicateName.value = true;
      toast.showError('An integration with this name already exists');
      return;
    }

    if (props.mode === 'edit' && props.integrationId) {
      await updateIntegrationFromWizard(props.integrationId, formData as any);
      emit('integration-updated');
    } else {
      await createIntegrationFromWizard(formData as any);
      emit('integration-created');
    }
    closeWizard();
  } catch (e: any) {
    toast.showError(e?.message || 'Operation failed');
  } finally {
    loading.value = false;
  }
}

async function prefill(integrationId: string, clone: boolean) {
  loading.value = true;
  try {
    const detail = await ArcGISIntegrationService.getArcGISIntegrationById(integrationId);
    mapBasicDetails(detail, formData);

    originalIntegrationName.value = detail.name || '';

    mapDocumentSelection(detail, formData);
    mapScheduleData((detail as any).schedule, formData as any);
    mapConnectionData(detail, formData as any);

    // Fetch field mappings separately using the new endpoint
    const originalFieldMappings = await ArcGISIntegrationService.getFieldMappings(integrationId);
    formData.fieldMappings = transformFieldMappingsForEdit(originalFieldMappings);
    await nextTick();
    Object.keys(stepValidation).forEach(k => (stepValidation[+k] = true));
    if (clone) formData.name = `Copy of ${formData.name || 'Integration'}`;
  } catch (error: any) {
    console.error('Failed to prefill integration data:', error);
    toast.showError(error?.message || 'Failed to load integration details');
    throw error;
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.open,
  async v => {
    document.body.style.overflow = v ? 'hidden' : '';
    if (v && props.integrationId && props.mode !== 'create') {
      await prefill(props.integrationId, props.mode === 'clone');
      stepValidation[3] = false;
    } else if (v && props.mode === 'create') {
      // For new integrations, add empty slot to allow users to add field mappings
      formData.fieldMappings = ensureEmptyMappingSlot(formData.fieldMappings);
    }

    // Load all integration names when wizard opens
    if (v) {
      try {
        await loadAllNames();
      } catch (error) {
        console.error('Failed to load integration names:', error);
        toast.showError('Failed to load integration names');
      }
    }
  }
);

// Setup name validation watcher
setupNameWatcher();
</script>

<style src="./ArcGISIntegrationWizard.css"></style>
