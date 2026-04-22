<template>
  <div class="bd-root">
    <h2 class="bd-title">ArcGIS Integration Details</h2>

    <div class="bd-container">
      <!-- NAME FIELD -->
      <div class="bd-field">
        <label class="bd-label">
          ArcGIS Integration Name
          <span class="bd-required">*</span>
        </label>

        <input
          class="bd-input"
          :class="{ 'bd-input-error': validationError }"
          type="text"
          v-model="localData.name"
          maxlength="100"
          placeholder="Enter ArcGIS integration name"
          @input="onNameInput"
        />
        <div
          v-if="validationHelperText"
          class="bd-helper-text"
          :class="{ 'bd-helper-error': validationError }"
        >
          {{ validationHelperText }}
        </div>
      </div>

      <!-- DESCRIPTION -->
      <div class="bd-field">
        <div class="bd-desc-header">
          <label class="bd-label">Description</label>
          <span class="bd-desc-optional">(Optional)</span>

          <span class="bd-desc-counter" :class="counterClass">
            {{ counterText }}
          </span>
        </div>

        <textarea
          class="bd-textarea"
          v-model="localData.description"
          placeholder="Provide a brief description of this integration's purpose and functionality..."
          rows="4"
          maxlength="500"
        ></textarea>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import type { BasicDetailsData } from '@/types/ArcGISFormData';
import { useCharacterCounter } from '@/composables/useCharacterCounter';
import { checkDuplicateName } from '@/utils/globalNormalizedUtils';
import { syncTextInputValue } from '@/utils/textInputUtils';

defineOptions({ name: 'BasicDetailsStep' });

interface Props {
  modelValue: BasicDetailsData;
  editMode?: boolean;
  originalName?: string;
  normalizedNames?: string[];
}

interface Emits {
  (e: 'update:modelValue', value: BasicDetailsData): void;
  (e: 'validation-change', isValid: boolean): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

// Local data that syncs with parent
const localData = ref<BasicDetailsData>({ ...props.modelValue });

// Validation state
const validationError = ref(false);
const validationHelperText = ref(' ');
let validateTimer: number | undefined;
const hasInteracted = ref(false); // Track if user has touched the field

// Store all integration names as-is (NOT normalized) - the checkDuplicateName function will normalize them
const allIntegrationNormalizedNames = ref<string[]>(props.normalizedNames || []);

// Create reactive reference for description
const descriptionRef = computed(() => localData.value.description || '');

// Character counter using reusable composable
const { counterClass, counterText } = useCharacterCounter(descriptionRef);

// Name validation with debounce
function validateName(name: string) {
  const trimmed = (name || '').trim();

  // Clear previous timer
  if (validateTimer) window.clearTimeout(validateTimer);

  // Don't show validation errors until user has interacted with the field
  if (!hasInteracted.value && !trimmed) {
    emit('validation-change', false);
    return;
  }

  // Immediate validation for empty names (only after interaction)
  if (!trimmed) {
    validationError.value = true;
    validationHelperText.value = 'Integration name is required';
    emit('validation-change', false);
    return;
  }

  // Clear error state for non-empty names
  validationError.value = false;
  validationHelperText.value = ' ';

  // Debounced duplicate check
  validateTimer = window.setTimeout(() => {
    const isDuplicate = checkDuplicateName(
      trimmed,
      allIntegrationNormalizedNames.value,
      props.originalName,
      props.editMode
    );

    if (isDuplicate) {
      validationError.value = true;
      validationHelperText.value = 'ArcGIS Integration name already exists';
      emit('validation-change', false);
    } else {
      validationError.value = false;
      validationHelperText.value = ' ';
      emit('validation-change', true);
    }
  }, 300);
}

// Input handler
function onNameInput(event: Event) {
  hasInteracted.value = true; // Mark field as touched
  const value = syncTextInputValue(event);
  if (localData.value.name !== value) {
    localData.value.name = value;
  }
  validateName(value);
}

// Validation
const isValid = computed(() => {
  const hasName = !!localData.value.name.trim();
  return hasName && !validationError.value;
});

// Watch for changes and emit to parent
watch(
  localData,
  newValue => {
    emit('update:modelValue', { ...newValue });
  },
  { deep: true }
);

// Watch for validation changes
watch(
  isValid,
  newValid => {
    emit('validation-change', newValid);
  },
  { immediate: true }
);

// Watch for name changes to trigger validation (only after interaction or in edit mode)
watch(
  () => localData.value.name,
  newName => {
    if (hasInteracted.value || (props.editMode && newName)) {
      validateName(newName);
    }
  }
);

// Watch for prop changes from parent
watch(
  () => props.modelValue,
  newValue => {
    localData.value = { ...newValue };
    // In edit mode with existing name, mark as interacted
    if (props.editMode && newValue.name) {
      hasInteracted.value = true;
    }
  },
  { deep: true }
);

// Watch for normalizedNames prop changes
watch(
  () => props.normalizedNames,
  newNames => {
    // Store as-is, checkDuplicateName will normalize
    allIntegrationNormalizedNames.value = newNames || [];
  }
);
</script>

<style scoped>
/* Root Box */
.bd-root {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 10px;
}

/* Title */
.bd-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 30px;
  color: #060606;
}

/* Container */
.bd-container {
  width: 100%;
  max-width: 600px;
}

/* Field wrapper */
.bd-field {
  margin-bottom: 28px;
}

/* Labels */
.bd-label {
  font-size: 15px;
  color: #000000;
  font-weight: 600;
  display: inline-flex;
  gap: 4px;
}

.bd-required {
  color: #dc2626;
}

/* Input */
.bd-input {
  width: 100%;
  margin-top: 6px;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background-color: #fff;
}

.bd-input:focus {
  border-color: #2563eb;
  outline: none;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.2);
}

.bd-input-error {
  border-color: #dc2626;
}

.bd-input-error:focus {
  border-color: #dc2626;
  box-shadow: 0 0 0 1px rgba(220, 38, 38, 0.2);
}

/* Helper text */
.bd-helper-text {
  margin-top: 4px;
  font-size: 12px;
  min-height: 16px;
}

.bd-helper-error {
  color: #dc2626;
}

/* Description */
.bd-desc-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.bd-desc-optional {
  color: #6b7280;
  font-size: 12px;
}

/* Character counter */
.bd-desc-counter {
  margin-left: auto;
  font-size: 12px;
  font-weight: 500;
  transition: color 0.2s;
}

.counter-normal {
  color: #6b7280;
}

.counter-orange {
  color: #f59e0b;
}

.counter-red {
  color: #dc2626;
}

/* Textarea */
.bd-textarea {
  width: 100%;
  padding: 10px 12px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  font-size: 14px;
  resize: vertical;
  min-height: 110px;
}

.bd-textarea:focus {
  border-color: #2563eb;
  outline: none;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.2);
}

.bd-input::placeholder,
.bd-textarea::placeholder {
  color: var(--kw-placeholder-color);
  font-weight: 400;
  font-style: italic;
  opacity: 1;
}

.bd-input:focus::placeholder,
.bd-textarea:focus::placeholder {
  color: var(--kw-placeholder-focus-color);
}
</style>
