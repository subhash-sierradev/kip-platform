<template>
  <div class="uis-root">
    <h2 class="uis-title">{{ config.integrationTitle }}</h2>

    <div class="uis-container">
      <section class="uis-panel">
        <div class="uis-identity-grid">
          <div class="uis-field">
            <label class="uis-label">
              {{ config.integrationNameLabel }}
              <span class="uis-required">*</span>
            </label>

            <input
              class="uis-input"
              :class="{ 'uis-input-error': validationError }"
              type="text"
              v-model="localData.name"
              :placeholder="config.integrationNamePlaceholder"
              maxlength="100"
              @input="onNameInput"
            />
            <div
              v-if="validationHelperText"
              class="uis-helper-text"
              :class="{ 'uis-helper-error': validationError }"
            >
              {{ validationHelperText }}
            </div>
          </div>

          <div class="uis-field">
            <div class="uis-desc-header">
              <label class="uis-label">Description</label>
              <span class="uis-desc-optional">(Optional)</span>
              <span class="uis-desc-counter" :class="counterClass">{{ counterText }}</span>
            </div>

            <textarea
              class="uis-textarea"
              v-model="localData.description"
              :placeholder="config.descriptionPlaceholder"
              rows="3"
              maxlength="500"
            ></textarea>
          </div>
        </div>

        <h3 class="uis-panel-title">{{ config.documentTitle }}</h3>

        <div class="uis-field-row">
          <div class="uis-field">
            <label class="uis-label">{{ config.itemTypeLabel }}</label>
            <select class="uis-input" disabled>
              <option :value="localData.itemType || 'DOCUMENT'">Document</option>
            </select>
          </div>

          <div class="uis-field">
            <label class="uis-label">
              {{ config.itemSubtypeLabel }}
              <span class="uis-required">*</span>
            </label>
            <div class="uis-select-wrapper">
              <select
                class="uis-input"
                v-model="localData.subType"
                :disabled="subTypesApi.loading.value"
                @change="handleSubtypeChange"
              >
                <option value="" disabled hidden>
                  {{
                    subTypesApi.loading.value ? 'Loading Item subtypes...' : 'Select Item Subtype'
                  }}
                </option>
                <option
                  v-for="subtype in subTypesApi.data.value"
                  :key="subtype.code"
                  :value="subtype.code"
                >
                  {{ subtype.displayValue }}
                </option>
              </select>
              <div v-if="subTypesApi.loading.value" class="uis-loading-indicator">
                <span class="uis-spinner"></span>
              </div>
            </div>
            <div v-if="subTypesApi.hasError.value" class="uis-error-row">
              <span class="uis-error-text">{{ subTypesApi.error.value }}</span>
              <button type="button" class="uis-retry-btn" @click="loadSubTypes">Retry</button>
            </div>
          </div>
        </div>

        <div v-if="isDynamicSubtype(localData.subType)" class="uis-field">
          <label class="uis-label">
            {{ config.dynamicDocumentLabel }}
            <span class="uis-required">*</span>
          </label>
          <div class="uis-select-wrapper">
            <select
              class="uis-input"
              :value="localData.dynamicDocument"
              :disabled="dynamicDocsApi.loading.value"
              @change="handleDynamicDocChange"
            >
              <option value="" disabled hidden>
                {{
                  dynamicDocsApi.loading.value ? 'Loading documents...' : 'Select Dynamic Document'
                }}
              </option>
              <option v-for="doc in dynamicDocsApi.data.value" :key="doc.id" :value="doc.id">
                {{ doc.title }}
              </option>
            </select>
            <div v-if="dynamicDocsApi.loading.value" class="uis-loading-indicator">
              <span class="uis-spinner"></span>
            </div>
          </div>
          <div v-if="dynamicDocsApi.hasError.value" class="uis-error-row">
            <span class="uis-error-text">{{ dynamicDocsApi.error.value }}</span>
            <button type="button" class="uis-retry-btn" @click="reloadDynamicDocuments">
              Retry
            </button>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';

import { KwDocService } from '@/api/services/KwIntegrationService';
import type { KwDynamicDocType } from '@/api/models/KwDynamicDocType';
import type { KwItemSubtypeDto } from '@/api';
import { useApiData } from '@/composables/useApiData';
import { useCharacterCounter } from '@/composables/useCharacterCounter';
import type {
  UnifiedIntegrationStepConfig,
  UnifiedIntegrationStepData,
} from '@/types/UnifiedIntegrationStep';
import { checkDuplicateName } from '@/utils/globalNormalizedUtils';
import { syncTextInputValue } from '@/utils/textInputUtils';

defineOptions({ name: 'IntegrationDetailsStep' });

interface Props {
  modelValue: UnifiedIntegrationStepData;
  config: UnifiedIntegrationStepConfig;
  editMode?: boolean;
  originalName?: string;
  normalizedNames?: string[];
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:modelValue', value: UnifiedIntegrationStepData): void;
  (e: 'validation-change', isValid: boolean): void;
}>();

const localData = ref<UnifiedIntegrationStepData>({ ...props.modelValue });

const INTEGRATION_NAME_MAX_LENGTH = 100;

const validationError = ref(false);
const validationHelperText = ref(' ');
const hasInteracted = ref(false);
let validateTimer: number | undefined;

const subTypesApi = useApiData<KwItemSubtypeDto>();
const dynamicDocsApi = useApiData<KwDynamicDocType>();

const dynamicSubtypeCodes = computed(
  () => props.config.dynamicSubtypeCodes || ['DOCUMENT_DRAFT_DYNAMIC', 'DOCUMENT_FINAL_DYNAMIC']
);
const isDynamicSubtype = (subType: string) => dynamicSubtypeCodes.value.includes(subType);

const descriptionRef = computed(() => localData.value.description || '');
const { counterClass, counterText } = useCharacterCounter(descriptionRef);

function validateName(name: string) {
  const trimmed = (name || '').trim();
  if (validateTimer) {
    window.clearTimeout(validateTimer);
  }

  if (!hasInteracted.value && !trimmed) {
    emit('validation-change', false);
    return;
  }

  if (!trimmed) {
    validationError.value = true;
    validationHelperText.value = 'Integration name is required';
    emit('validation-change', false);
    return;
  }

  if (trimmed.length > INTEGRATION_NAME_MAX_LENGTH) {
    validationError.value = true;
    validationHelperText.value = `Maximum ${INTEGRATION_NAME_MAX_LENGTH} characters allowed`;
    emit('validation-change', false);
    return;
  }

  validationError.value = false;
  validationHelperText.value = ' ';

  validateTimer = window.setTimeout(() => {
    const isDuplicate = checkDuplicateName(
      trimmed,
      props.normalizedNames || [],
      props.originalName,
      props.editMode
    );

    if (isDuplicate) {
      validationError.value = true;
      validationHelperText.value = props.config.duplicateNameMessage;
      emit('validation-change', false);
      return;
    }

    validationError.value = false;
    validationHelperText.value = ' ';
    emit('validation-change', isValid.value);
  }, 300);
}

function onNameInput(event: Event) {
  hasInteracted.value = true;
  const value = syncTextInputValue(event);
  if (localData.value.name !== value) {
    localData.value.name = value;
  }
  validateName(value);
}

const updateSubtypeLabel = () => {
  if (!localData.value.subType || !subTypesApi.data.value) {
    return;
  }

  const selectedSubtype = subTypesApi.data.value.find(s => s.code === localData.value.subType);
  localData.value.subTypeLabel = selectedSubtype?.displayValue || '';
};

const normalizeDynamicDocSelection = () => {
  const current = localData.value.dynamicDocument;
  if (!current || !dynamicDocsApi.data.value?.length) {
    return;
  }

  const matchById = dynamicDocsApi.data.value.find(d => d.id === current);
  if (matchById) {
    localData.value.dynamicDocumentLabel = matchById.title;
    return;
  }

  const matchByTitle = dynamicDocsApi.data.value.find(d => d.title === current);
  if (matchByTitle) {
    localData.value.dynamicDocument = matchByTitle.id;
    localData.value.dynamicDocumentLabel = matchByTitle.title;
    return;
  }

  localData.value.dynamicDocument = '';
  localData.value.dynamicDocumentLabel = '';
};

const loadSubTypes = async () => {
  try {
    subTypesApi.setLoading(true);
    const result = await KwDocService.getSubItemTypes();
    subTypesApi.setData(result);
    updateSubtypeLabel();
  } catch (error: any) {
    subTypesApi.setError(error?.message || error?.body?.message || 'Failed to load subtypes');
  } finally {
    subTypesApi.setLoading(false);
  }
};

const loadDynamicDocuments = async (subType: string) => {
  dynamicDocsApi.reset();
  if (!subType || !isDynamicSubtype(subType)) {
    return;
  }

  try {
    dynamicDocsApi.setLoading(true);
    const result = await KwDocService.getDynamicDocuments('DOCUMENT', subType);
    dynamicDocsApi.setData(Array.isArray(result) ? result : []);
    normalizeDynamicDocSelection();
  } catch (error: any) {
    dynamicDocsApi.setError(
      error?.message || error?.body?.message || 'Failed to load dynamic documents'
    );
  } finally {
    dynamicDocsApi.setLoading(false);
  }
};

const reloadDynamicDocuments = async () => {
  await loadDynamicDocuments(localData.value.subType);
};

const handleSubtypeChange = async () => {
  updateSubtypeLabel();
  localData.value.dynamicDocument = '';
  localData.value.dynamicDocumentLabel = '';
  await loadDynamicDocuments(localData.value.subType);
};

const handleDynamicDocChange = (event: Event) => {
  const value = (event.target as HTMLSelectElement).value;
  localData.value.dynamicDocument = value;
  normalizeDynamicDocSelection();
};

const isValid = computed(() => {
  const trimmedName = localData.value.name.trim();
  const hasName = !!trimmedName;
  const nameNotTooLong = trimmedName.length <= INTEGRATION_NAME_MAX_LENGTH;
  const hasSubtype = !!localData.value.subType;
  const hasDynamicDoc =
    !isDynamicSubtype(localData.value.subType) || !!localData.value.dynamicDocument;
  return hasName && nameNotTooLong && !validationError.value && hasSubtype && hasDynamicDoc;
});

watch(
  localData,
  newValue => {
    emit('update:modelValue', { ...newValue });
  },
  { deep: true }
);

watch(
  isValid,
  newValid => {
    emit('validation-change', newValid);
  },
  { immediate: true }
);

watch(
  () => props.modelValue,
  newValue => {
    if (JSON.stringify(newValue) !== JSON.stringify(localData.value)) {
      localData.value = { ...newValue };
      if (newValue.name.trim()) {
        validateName(newValue.name);
      }
      if (newValue.subType && isDynamicSubtype(newValue.subType)) {
        void loadDynamicDocuments(newValue.subType);
      }
    }
  },
  { deep: true }
);

watch(
  () => props.normalizedNames,
  () => {
    if (localData.value.name.trim()) {
      validateName(localData.value.name);
    }
  }
);

onMounted(async () => {
  await loadSubTypes();
  if (localData.value.subType && isDynamicSubtype(localData.value.subType)) {
    await loadDynamicDocuments(localData.value.subType);
  }
});
</script>

<style scoped src="./IntegrationDetailsStep.css"></style>
