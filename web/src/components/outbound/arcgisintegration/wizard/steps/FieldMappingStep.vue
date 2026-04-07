<template>
  <div class="fm-root">
    <div class="fm-container">
      <section class="fm-section">
        <div class="fm-mapping-table">
          <div class="fm-mapping-header">
            <div class="fm-header-source">Document Field</div>
            <div class="fm-header-transform">Transformation</div>
            <div class="fm-header-arrow"></div>
            <div class="fm-header-target">ArcGIS Layer Field</div>
            <div class="fm-header-mandatory">Mandatory</div>
            <div class="fm-header-actions"></div>
          </div>

          <div class="fm-mapping-body">
            <div
              v-for="(mapping, index) in sortedFieldMappings"
              :key="`mapping-${index}`"
              class="fm-mapping-row"
            >
              <div class="fm-source-field">
                <select
                  v-model="mapping.sourceField"
                  class="fm-input"
                  :disabled="sourceFieldsLoading || isDefaultMapping(mapping)"
                >
                  <option value="">
                    {{ sourceFieldsLoading ? 'Loading document fields...' : 'Select' }}
                  </option>
                  <option
                    v-for="field in getAvailableSourceFields(index)"
                    :key="field.id"
                    :value="field.fieldName"
                  >
                    {{ field.fieldName }} ({{ field.fieldType }})
                  </option>
                </select>
                <div v-if="sourceFieldsError" class="fm-error-message">
                  Failed to load source fields: {{ sourceFieldsError }}
                </div>
              </div>
              <div class="fm-transform-field">
                <select
                  v-model="mapping.transformationType"
                  class="fm-input"
                  :disabled="isDefaultMapping(mapping)"
                >
                  <option v-for="opt in transformationOptions" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                  </option>
                </select>
              </div>
              <div class="fm-mapping-arrow">→</div>
              <div class="fm-target-field">
                <select
                  v-model="mapping.targetField"
                  class="fm-input"
                  :disabled="isDefaultMapping(mapping)"
                  @change="onTargetFieldChange(index)"
                >
                  <option value="">Select</option>
                  <option
                    v-for="field in getAvailableTargetFields(index)"
                    :key="field"
                    :value="field"
                  >
                    {{ field }}
                  </option>
                </select>
              </div>
              <div class="fm-mandatory-field">
                <input
                  type="checkbox"
                  class="fm-checkbox"
                  v-model="mapping.isMandatory"
                  :disabled="isMandatoryDisabled(mapping.targetField) || isDefaultMapping(mapping)"
                />
              </div>
              <div class="fm-action-field">
                <button
                  v-if="sortedFieldMappings.length > 1 && !isDefaultMapping(mapping)"
                  @click="removeFieldMapping(index)"
                  class="fm-btn-remove"
                  type="button"
                  title="Remove field mapping"
                >
                  <i class="dx-icon-remove"></i>
                </button>
                <button
                  v-if="index === sortedFieldMappings.length - 1 && isCurrentMappingComplete(index)"
                  @click="addFieldMapping"
                  class="fm-btn-add"
                  type="button"
                  title="Add new field mapping"
                >
                  <i class="dx-icon-add"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch, computed, onMounted } from 'vue';
import type { FieldMappingData } from '../../../../../types/ArcGISFormData';
import { useSourceFields } from '@/composables/useSourceFields';
import { useArcgisFeatures } from '@/composables/useArcgisFeatures';
import { FieldTransformationType } from '@/api/models/FieldTransformationType';
import type { ArcGISFeatureField } from '@/api/models/ArcGISFeatureField';
import type { KwDocField } from '@/api/models/KwDocField';
import {
  DEFAULT_SOURCE_FIELD,
  DEFAULT_TARGET_FIELD,
  isDefaultMapping,
} from '../../utils/fieldMappingConstants';

defineOptions({ name: 'FieldMappingStep' });

interface Props {
  modelValue: FieldMappingData;
  connectionId: string | null;
  mode?: 'create' | 'edit' | 'clone';
}

const props = withDefaults(defineProps<Props>(), {
  mode: 'create',
});
const emit = defineEmits<{
  'update:modelValue': [value: FieldMappingData];
  'validation-change': [isValid: boolean];
}>();

const localData = reactive({ ...props.modelValue });

// Mandatory default field mapping that cannot be removed or modified
const MANDATORY_DEFAULT_MAPPING = {
  sourceField: DEFAULT_SOURCE_FIELD,
  targetField: DEFAULT_TARGET_FIELD,
  transformationType: FieldTransformationType.PASSTHROUGH,
  isMandatory: true,
  displayOrder: 0,
  isDefault: true,
};

// Ensure field mappings are properly initialized for edit mode
const initializeFieldMappings = () => {
  // Ensure at least one field mapping exists
  if (!localData.fieldMappings || localData.fieldMappings.length === 0) {
    localData.fieldMappings = [];
  }

  // Check if mandatory default mapping exists
  const hasDefaultMapping = localData.fieldMappings.some(isDefaultMapping);

  // If default mapping doesn't exist, add it at the beginning
  if (!hasDefaultMapping) {
    localData.fieldMappings.unshift({
      id: ``,
      ...MANDATORY_DEFAULT_MAPPING,
    });
  } else {
    // Ensure existing default mapping is first and has correct properties
    const defaultIndex = localData.fieldMappings.findIndex(isDefaultMapping);
    if (defaultIndex > 0) {
      const defaultMapping = localData.fieldMappings.splice(defaultIndex, 1)[0];
      localData.fieldMappings.unshift(defaultMapping);
    }
    // Update the default mapping properties
    const defaultMapping = localData.fieldMappings[0];
    defaultMapping.transformationType = FieldTransformationType.PASSTHROUGH;
    defaultMapping.isMandatory = true;
    defaultMapping.displayOrder = 0;
    defaultMapping.isDefault = true;
  }

  // Normalize all field mappings
  localData.fieldMappings.forEach((m: any, index: number) => {
    if (
      m.transformationType === undefined ||
      m.transformationType === null ||
      m.transformationType === ''
    ) {
      m.transformationType = FieldTransformationType.PASSTHROUGH;
    }
    if (m.sourceField === undefined || m.sourceField === null) {
      m.sourceField = '';
    }
    if (m.targetField === undefined || m.targetField === null) {
      m.targetField = '';
    }
    if (m.isMandatory === undefined || m.isMandatory === null) {
      m.isMandatory = false;
    }
    if (m.displayOrder === undefined || m.displayOrder === null) {
      m.displayOrder = index;
    }
  });

  // Add an empty mapping if only default exists (only for create mode)
  if (localData.fieldMappings.length === 1 && props.mode === 'create') {
    localData.fieldMappings.push({
      id: `field-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`,
      sourceField: '',
      targetField: '',
      transformationType: FieldTransformationType.PASSTHROUGH,
      isMandatory: false,
      displayOrder: 1,
    });
  }
};

// Ensure transformation dropdown defaults to PASSTHROUGH
const normalizeFieldMappings = () => {
  localData.fieldMappings.forEach(m => {
    if (m.transformationType === undefined || m.transformationType === null) {
      m.transformationType = FieldTransformationType.PASSTHROUGH;
    }
  });
};

initializeFieldMappings();
normalizeFieldMappings();

const {
  fields: apiSourceFields,
  loading: sourceFieldsLoading,
  error: sourceFieldsError,
  load: loadSourceFields,
} = useSourceFields();
const { fields, load } = useArcgisFeatures();

// Computed property to sort field mappings by displayOrder
const sortedFieldMappings = computed(() => {
  return [...localData.fieldMappings].sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
});
const arcgisFieldNames = computed(() => {
  const fieldNames = fields.value
    .map(f => {
      // Handle different possible field name properties
      const name = f.name || f.alias || '';
      return name;
    })
    .filter(name => name); // Filter out empty names
  return fieldNames;
});
// Ensure PASSTHROUGH is first option by explicitly ordering the transformation options
const transformationOptions = [
  FieldTransformationType.PASSTHROUGH,
  ...Object.values(FieldTransformationType).filter(v => v !== FieldTransformationType.PASSTHROUGH),
].map(value => ({
  value,
  label: value,
}));

const sourceFieldOptions = computed(() => {
  if (apiSourceFields.value && apiSourceFields.value.length > 0) {
    return apiSourceFields.value;
  }
  return [];
});

// Filtered source field options - exclude already selected source fields
const getAvailableSourceFields = (currentIndex: number) => {
  // Get the actual mapping from sorted array
  const currentMapping = sortedFieldMappings.value[currentIndex];
  const usedSourceFields = localData.fieldMappings
    .filter(mapping => mapping !== currentMapping)
    .map(mapping => mapping.sourceField)
    .filter(field => field);

  return sourceFieldOptions.value.filter(
    (field: KwDocField) => !usedSourceFields.includes(field.fieldName)
  );
};

// Filtered target field options - exclude already selected target fields
const getAvailableTargetFields = (currentIndex: number) => {
  // Get the actual mapping from sorted array
  const currentMapping = sortedFieldMappings.value[currentIndex];
  const usedTargetFields = localData.fieldMappings
    .filter(mapping => mapping !== currentMapping)
    .map(mapping => mapping.targetField)
    .filter(field => field);

  return arcgisFieldNames.value.filter(field => !usedTargetFields.includes(field));
};

// Validation errors
const validationErrors = computed(() => {
  const errors: string[] = [];

  // Check if all field mappings are complete
  const incompleteMappings = localData.fieldMappings.filter(
    mapping => !mapping.sourceField || !mapping.targetField || !mapping.transformationType
  );

  if (incompleteMappings.length > 0) {
    errors.push(
      'All field mappings must be completed (source field, target field, and transformation type are required)'
    );
  }

  // Check if at least one complete field mapping exists
  const completeMappings = localData.fieldMappings.filter(
    mapping => mapping.sourceField && mapping.targetField && mapping.transformationType
  );

  if (completeMappings.length === 0) {
    errors.push('At least one complete field mapping is required');
  }

  // Check for duplicate source fields
  const sourceFieldsUsed = localData.fieldMappings
    .filter(mapping => mapping.sourceField)
    .map(mapping => mapping.sourceField);

  const duplicateSources = sourceFieldsUsed.filter(
    (field, index) => sourceFieldsUsed.indexOf(field) !== index
  );

  if (duplicateSources.length > 0) {
    errors.push('Each source field can only be mapped once');
  }

  // Check for duplicate target fields
  const targetFieldsUsed = localData.fieldMappings
    .filter(mapping => mapping.targetField)
    .map(mapping => mapping.targetField);

  const duplicateTargets = targetFieldsUsed.filter(
    (field, index) => targetFieldsUsed.indexOf(field) !== index
  );

  if (duplicateTargets.length > 0) {
    errors.push('Each target field can only be mapped once');
  }

  return errors;
});

// Inline field-level error calculation removed from UI; hidden validation retained at step level.

// Watch for external changes
watch(
  () => props.modelValue,
  newValue => {
    Object.assign(localData, newValue);
    initializeFieldMappings();
    normalizeFieldMappings();
  },
  { deep: true }
);

// Watch for local changes and emit
watch(
  localData,
  newValue => {
    emit('update:modelValue', { ...newValue });

    // Validate and emit validation status
    const isValid = validateStep();
    emit('validation-change', isValid);
  },
  { deep: true }
);

const addFieldMapping = () => {
  const newIndex = localData.fieldMappings.length;
  // Default transformationType to PASSTHROUGH
  localData.fieldMappings.push({
    id: `field-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`,
    sourceField: '',
    targetField: '',
    transformationType: FieldTransformationType.PASSTHROUGH,
    isMandatory: false,
    displayOrder: newIndex,
  });
};

const removeFieldMapping = (index: number) => {
  const mappingToRemove = sortedFieldMappings.value[index];

  // Prevent removal of mandatory default mapping
  if (isDefaultMapping(mappingToRemove)) {
    return; // Silently ignore - UI won't show remove button for default mapping
  }

  if (localData.fieldMappings.length > 1) {
    // Find and remove it from the original array
    const originalIndex = localData.fieldMappings.findIndex(m => m === mappingToRemove);
    if (originalIndex !== -1) {
      localData.fieldMappings.splice(originalIndex, 1);
      // Reorder displayOrder after removal
      localData.fieldMappings.forEach((mapping, idx) => {
        mapping.displayOrder = idx;
      });
    }
  }
};

const validateStep = (): boolean => {
  return validationErrors.value.length === 0;
};

// Emit initial validation state when component mounts
onMounted(async () => {
  const isValid = validateStep();
  emit('validation-change', isValid);

  // Load both source fields and ArcGIS field names
  await Promise.all([
    loadSourceFields(),
    props.connectionId ? load(props.connectionId) : Promise.resolve(),
  ]);
});

// Helpers for ArcGIS field lookups and mandatory handling
const getArcgisFieldByName = (name?: string | null): ArcGISFeatureField | undefined => {
  if (!name) return undefined;
  return fields.value.find(f => f.name === name);
};

const isMandatoryDisabled = (targetField?: string) => {
  const fld = getArcgisFieldByName(targetField);
  return fld?.nullable === false;
};

const onTargetFieldChange = (index: number) => {
  const mapping = sortedFieldMappings.value[index];
  const fld = getArcgisFieldByName(mapping.targetField);
  if (fld?.nullable === false) {
    // Force mandatory when ArcGIS field is non-nullable
    mapping.isMandatory = true;
  }
};

// Helper to check if current mapping is complete
const isCurrentMappingComplete = (index: number): boolean => {
  const mapping = sortedFieldMappings.value[index];
  return !!(mapping.sourceField && mapping.targetField && mapping.transformationType);
};

// When fields load/update, reconcile mandatory flags for selected targets
watch(fields, () => {
  localData.fieldMappings.forEach(m => {
    const fld = getArcgisFieldByName(m.targetField);
    if (fld?.nullable === false) {
      m.isMandatory = true;
    }
  });
});
</script>

<style scoped src="./FieldMappingStep.css"></style>
