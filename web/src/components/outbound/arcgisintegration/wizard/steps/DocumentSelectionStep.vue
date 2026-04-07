<template>
  <div class="ds-root">
    <h2 class="ds-title">Document Selection</h2>

    <div class="ds-container">
      <section class="ds-section">
        <div class="ds-field-row">
          <div class="ds-field">
            <label class="ds-label">
              Item Type
              <span class="ds-required">*</span>
            </label>
            <select class="ds-input" disabled>
              <option value="DOCUMENT">Document</option>
            </select>
          </div>
          <div class="ds-field">
            <label class="ds-label">
              Item Subtype
              <span class="ds-required">*</span>
            </label>
            <div class="ds-select-wrapper">
              <select
                class="ds-input"
                v-model="localData.subType"
                :disabled="subTypesApi.loading.value"
                @change="updateSubtypeLabel"
                required
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
              <div v-if="subTypesApi.loading.value" class="ds-loading-indicator">
                <span class="ds-spinner"></span>
              </div>
            </div>
            <!-- Error state for Subtype -->
            <div v-if="subTypesApi.hasError.value" class="ds-error">
              <span class="ds-error-icon">⚠️</span>
              <span class="ds-error-text">
                {{ subTypesApi.error.value }}
              </span>
              <button type="button" class="ds-retry-btn" @click="loadSubTypes">Retry</button>
            </div>

            <!-- Dynamic Documents loading / empty / error states (dropdown remains hidden unless data exists) -->
            <!-- Note: Dynamic docs loading is handled by global loader; no local spinner shown -->

            <div v-if="dynamicDocsEnabled && dynamicDocsApi.hasError.value" class="ds-error">
              <span class="ds-error-icon">⚠️</span>
              <span class="ds-error-text">
                {{ dynamicDocsApi.error.value }}
              </span>
              <button type="button" class="ds-retry-btn" @click="reloadDynamicDocuments">
                Retry
              </button>
            </div>
          </div>
        </div>

        <!-- Conditional Dynamic Document field -->
        <div v-if="dynamicDocsEnabled" class="ds-field">
          <label class="ds-label">
            Dynamic Document
            <span class="ds-required">*</span>
          </label>
          <div class="ds-select-wrapper ds-dynamic-doc-wrapper">
            <select
              class="ds-input"
              v-model="localData.dynamicDocument"
              :disabled="dynamicDocsApi.loading.value || dynamicDocsApi.hasError.value"
              @change="handleDynamicDocumentChange"
              required
            >
              <option value="" disabled>Select Dynamic Document</option>
              <option v-for="doc in dynamicDocsApi.data.value" :key="doc.id" :value="doc.id">
                {{ doc.title }}
              </option>
            </select>

            <!-- Some browsers render a disabled placeholder option as blank; overlay ensures the placeholder is visible. -->
            <span v-if="!localData.dynamicDocument" class="ds-placeholder-overlay"
              >Select Dynamic Document</span
            >
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue';
import type { KwDynamicDocType } from '@/api/models/KwDynamicDocType';
import { KwDocService } from '@/api/services/KwIntegrationService';
import { useApiData } from '@/composables/useApiData';
import type { DocumentSelectionData } from '../../../../../types/ArcGISFormData';
import { KwItemSubtypeDto } from '@/api';

defineOptions({ name: 'DocumentSelectionStep' });

interface Props {
  modelValue: DocumentSelectionData;
}

interface Emits {
  (e: 'update:modelValue', value: DocumentSelectionData): void;
  (e: 'validation-change', isValid: boolean): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

// API state management for subtypes
const subTypesApi = useApiData<KwItemSubtypeDto>();
const dynamicDocsApi = useApiData<KwDynamicDocType>();

const DYNAMIC_SUBTYPES = new Set(['DOCUMENT_DRAFT_DYNAMIC', 'DOCUMENT_FINAL_DYNAMIC']);
const isDynamicSubtype = (subType: string) => DYNAMIC_SUBTYPES.has(subType);

// Local data that syncs with parent
const localData = ref<DocumentSelectionData>({
  itemType: props.modelValue.itemType || 'DOCUMENT',
  subType: props.modelValue.subType || '',
  subTypeLabel: props.modelValue.subTypeLabel || '',
  dynamicDocument: props.modelValue.dynamicDocument || '',
  dynamicDocumentLabel: props.modelValue.dynamicDocumentLabel || '',
});

const dynamicDocsEnabled = computed(() => isDynamicSubtype(localData.value.subType));
const dynamicDocsFetchCompleted = ref(false);

const reloadDynamicDocuments = async () => {
  if (!localData.value.subType || !dynamicDocsEnabled.value) return;
  await loadDynamicDocuments(localData.value.subType);
};

const normalizeDynamicDocumentSelection = () => {
  const current = localData.value.dynamicDocument;
  const currentLabel = localData.value.dynamicDocumentLabel;
  if (!current || !dynamicDocsApi.data.value?.length) return;

  // Preferred: current selection is doc.id
  const matchById = dynamicDocsApi.data.value.find(d => d.id === current);
  if (matchById) {
    localData.value.dynamicDocumentLabel = matchById.title;
    return;
  }

  // Backward compatibility: if an existing integration stored doc.title in dynamicDocument, convert it to doc.id
  const matchByTitle = dynamicDocsApi.data.value.find(d => d.title === current);
  if (matchByTitle) {
    localData.value.dynamicDocument = matchByTitle.id;
    localData.value.dynamicDocumentLabel = matchByTitle.title;
    return;
  }

  // When switching between dynamic subtypes, preserve selection if a doc with the same title exists.
  if (currentLabel) {
    const matchByLabel = dynamicDocsApi.data.value.find(d => d.title === currentLabel);
    if (matchByLabel) {
      localData.value.dynamicDocument = matchByLabel.id;
      localData.value.dynamicDocumentLabel = matchByLabel.title;
      return;
    }
  }

  // If the current selection doesn't exist in the available docs for this subtype,
  // clear it so the step can't proceed with an invalid value.
  localData.value.dynamicDocument = '';
  localData.value.dynamicDocumentLabel = '';
};

const handleDynamicDocumentChange = (event: Event) => {
  const selectedValue = (event.target as HTMLSelectElement | null)?.value;
  if (typeof selectedValue === 'string') {
    localData.value.dynamicDocument = selectedValue;
  }

  // Ensure label is computed synchronously on user selection.
  // This prevents the label from being lost if the user navigates away immediately.
  normalizeDynamicDocumentSelection();
};

/**
 * Update subtype label based on current subtype selection
 */
const updateSubtypeLabel = () => {
  if (localData.value.subType && subTypesApi.data.value) {
    const selectedSubtype = subTypesApi.data.value.find(s => s.code === localData.value.subType);
    localData.value.subTypeLabel = selectedSubtype?.displayValue || '';
  }
};

/**
 * Load subtypes from API
 */
const loadSubTypes = async () => {
  try {
    subTypesApi.setLoading(true);
    const result = await KwDocService.getSubItemTypes();
    subTypesApi.setData(result);
    // Update the subtype label if we already have a subtype selected
    updateSubtypeLabel();
  } catch (error: any) {
    const errorMessage = error?.message || error?.body?.message || 'Failed to load subtypes';
    subTypesApi.setError(errorMessage);
  } finally {
    subTypesApi.setLoading(false);
  }
};

/**
 * Load dynamic documents for a selected subtype.
 * Dropdown is always shown for dynamic subtypes (disabled when no data).
 */
const loadDynamicDocuments = async (subType: string) => {
  dynamicDocsApi.reset();
  dynamicDocsFetchCompleted.value = false;
  if (!subType || !isDynamicSubtype(subType)) return;

  try {
    dynamicDocsApi.setLoading(true);
    const result = await KwDocService.getDynamicDocuments('DOCUMENT', subType);
    dynamicDocsApi.setData(Array.isArray(result) ? result : []);
    normalizeDynamicDocumentSelection();
  } catch (error: any) {
    const errorMessage =
      error?.message || error?.body?.message || 'Unable to load dynamic documents.';
    dynamicDocsApi.setError(errorMessage);
  } finally {
    dynamicDocsFetchCompleted.value = true;
    dynamicDocsApi.setLoading(false);
  }
};

// Validation
const isValid = computed(() => {
  const basicValid = !!(localData.value.itemType && localData.value.subType);

  if (!basicValid) return false;

  // Dynamic docs are only relevant for dynamic subtypes.
  if (!dynamicDocsEnabled.value) return true;

  // While loading (global loader is active), keep step invalid.
  if (dynamicDocsApi.loading.value || !dynamicDocsFetchCompleted.value) return false;
  if (dynamicDocsApi.hasError.value) return false;

  // If API returns no dynamic docs, keep Next disabled.
  if (!dynamicDocsApi.hasData.value) return false;

  // If docs exist, require the user to select one.
  return !!localData.value.dynamicDocument;
});

// Watch for changes and emit to parent
watch(
  localData,
  newValue => {
    emit('update:modelValue', { ...newValue });
  },
  { deep: true }
);

// Clear dynamicDocument when subtype changes away from Dynamic Document
watch(
  () => localData.value.subType,
  async (newSubType, oldSubType) => {
    const switchingBetweenDynamicSubtypes =
      !!oldSubType && isDynamicSubtype(oldSubType) && !!newSubType && isDynamicSubtype(newSubType);

    // Preserve selection when switching between dynamic subtypes.
    // Normalization after fetch will clear it if it's not valid for the new subtype.
    if (!switchingBetweenDynamicSubtypes) {
      localData.value.dynamicDocument = '';
      localData.value.dynamicDocumentLabel = '';
    }
    updateSubtypeLabel();

    if (!newSubType) {
      dynamicDocsApi.reset();
      dynamicDocsFetchCompleted.value = false;
      return;
    }

    if (!isDynamicSubtype(newSubType)) {
      dynamicDocsApi.reset();
      dynamicDocsFetchCompleted.value = false;
      return;
    }

    await loadDynamicDocuments(newSubType);
  }
);

// Update dynamicDocumentLabel when dynamicDocument changes
watch(
  () => localData.value.dynamicDocument,
  newDynamicDocument => {
    if (!newDynamicDocument) {
      localData.value.dynamicDocumentLabel = '';
      return;
    }

    // Selected value is doc.id, but tolerate legacy title values.
    // Keep this logic centralized in one place.
    normalizeDynamicDocumentSelection();
  }
);

// Watch for validation changes
watch(
  isValid,
  newValid => {
    emit('validation-change', newValid);
  },
  { immediate: true }
);

// Watch for prop changes from parent with immediate sync
watch(
  () => props.modelValue,
  newValue => {
    localData.value = { ...newValue };
  },
  { deep: true, immediate: true }
);

// Load initial data on component mount
onMounted(() => {
  loadSubTypes();
  if (!localData.value.itemType) {
    localData.value.itemType = 'DOCUMENT';
  }
  if (localData.value.subType && isDynamicSubtype(localData.value.subType)) {
    loadDynamicDocuments(localData.value.subType);
  }
});
</script>

<style scoped src="./DocumentSelectionStep.css"></style>
