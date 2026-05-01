<!-- Jira Basic Details Step -->
<template>
  <div class="bd-root">
    <h2 class="bd-title">{{ titleText }}</h2>

    <div class="bd-container">
      <!-- NAME FIELD -->
      <div class="bd-field">
        <label class="bd-label">
          Jira Webhook Name
          <span class="bd-required">*</span>
        </label>

        <input
          class="bd-input"
          :class="{ 'bd-input-error': validationError }"
          type="text"
          :value="integrationName"
          :maxlength="WEBHOOK_NAME_MAX_LENGTH"
          placeholder="Jira Webhook Name"
          @input="onNameInput"
        />

        <div class="bd-helper" :class="validationError ? 'bd-helper-error' : 'bd-helper-normal'">
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
          :value="description"
          placeholder="Provide a brief description of this webhook's purpose and functionality..."
          @input="onDescriptionInput"
          rows="4"
          maxlength="500"
        ></textarea>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue';
import { useCharacterCounter } from '../../../../composables/useCharacterCounter';
import { checkDuplicateName } from '@/utils/globalNormalizedUtils';
import { syncTextInputValue } from '@/utils/textInputUtils';
import { WEBHOOK_NAME_MAX_LENGTH } from '../utils/jiraWebhookConstants';

const props = defineProps<{
  integrationName: string;
  description?: string;
  editMode?: boolean;
  cloneMode?: boolean;
  mode?: 'add' | 'edit';
  originalName?: string;
  webhookUrl?: string;
  normalizedNames?: string[];
}>();

const emit = defineEmits<{
  (e: 'update:integrationName', v: string): void;
  (e: 'update:description', v: string): void;
  (e: 'validation-change', valid: boolean): void;
}>();

const internalDescription = ref(props.description || '');
const validationError = ref(false);
const validationHelperText = ref(' ');
let validateTimer: number | undefined;
const hasInteracted = ref(false); // Track if user has touched the field

// Store all webhook names as-is (NOT normalized) - the checkDuplicateName function will normalize them
const allWebhookNormalizedNames = ref<string[]>(props.normalizedNames || []);

/* ---------- VALIDATION ---------- */
function validateName(name: string) {
  const trimmed = (name || '').trim();

  // Clear any pending validation
  if (validateTimer) window.clearTimeout(validateTimer);

  // Don't show validation errors until user has interacted with the field
  if (!hasInteracted.value && !trimmed) {
    emit('validation-change', false);
    return;
  }

  // Immediate validation for empty names (only after interaction)
  if (!trimmed) {
    validationError.value = true;
    validationHelperText.value = 'Jira webhook name is required';
    emit('validation-change', false);
    hasInteracted.value = false; // Reset interaction flag
    return;
  }

  // Immediate max-length validation (covers clone/programmatic prefill scenarios)
  if (trimmed.length > WEBHOOK_NAME_MAX_LENGTH) {
    validationError.value = true;
    validationHelperText.value = `Maximum ${WEBHOOK_NAME_MAX_LENGTH} characters allowed`;
    emit('validation-change', false);
    return;
  }

  // Debounced duplicate check for non-empty names
  validateTimer = window.setTimeout(() => {
    const isDuplicate = checkDuplicateName(
      trimmed,
      allWebhookNormalizedNames.value,
      props.originalName,
      props.editMode
    );
    if (isDuplicate) {
      validationError.value = true;
      validationHelperText.value = 'Jira webhook name already exists';
      emit('validation-change', false);
    } else {
      validationError.value = false;
      validationHelperText.value = ' ';
      emit('validation-change', true);
    }
  }, 300);
}

function onNameInput(event: Event) {
  hasInteracted.value = true; // Mark field as touched
  const value = syncTextInputValue(event);
  emit('update:integrationName', value);
  validateName(value);
}

watch(
  () => props.integrationName,
  v => validateName(v || '')
);

onMounted(() => {
  validateName(props.integrationName);
});

// Watch for normalizedNames prop changes
watch(
  () => props.normalizedNames,
  newNames => {
    // Store as-is, checkDuplicateName will normalize
    allWebhookNormalizedNames.value = newNames || [];
  }
);

watch(
  () => props.description,
  value => {
    const nextValue = value || '';
    if (nextValue !== internalDescription.value) {
      internalDescription.value = nextValue;
    }
  }
);

/* ---------- DESCRIPTION ---------- */
// Character counter using reusable composable
const { counterClass, counterText, truncateValue } = useCharacterCounter(internalDescription);

function onDescriptionInput(event: Event) {
  const val = (event.target as HTMLTextAreaElement).value;
  internalDescription.value = truncateValue(val);
  emit('update:description', internalDescription.value);
}

const description = computed(() => internalDescription.value);

/* ---------- TITLE ---------- */
const titleText = computed(() => {
  if (props.cloneMode) return 'Clone Jira Webhook';
  if (props.editMode) return 'Update Jira Webhook';
  return 'Create Jira Webhook';
});
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
  border-color: #dc2626 !important;
}

/* Helper text */
.bd-helper {
  margin-top: 4px;
  font-size: 12px;
  color: #dc2626;
}

.bd-helper-normal {
  color: #6b7280;
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
