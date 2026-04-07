<template>
  <AppModal
    :open="show"
    :title="modalTitle"
    size="sm"
    :close-on-overlay-click="false"
    @update:open="v => !v && $emit('close')"
  >
    <form class="edit-form" @submit.prevent="$emit('save')">
      <div v-if="saveError" class="error-message">
        {{ saveError }}
      </div>

      <div class="form-group">
        <label class="form-label required">Key</label>
        <input
          :value="formData.id"
          type="text"
          class="form-input"
          disabled
          readonly
          placeholder="Enter configuration key"
          @input="handleIdUpdate"
        />
      </div>

      <div class="form-group">
        <label class="form-label required">Type</label>
        <select :value="formData.type" class="form-select" disabled @change="handleTypeUpdate">
          <option value="STRING">String</option>
          <option value="NUMBER">Number</option>
          <option value="BOOLEAN">Boolean</option>
          <option value="JSON">JSON</option>
          <option value="TIMESTAMP">Timestamp</option>
        </select>
      </div>

      <div class="form-group">
        <label class="form-label required">Value</label>
        <textarea
          v-if="formData.type === 'JSON'"
          :value="formData.value"
          class="form-textarea"
          rows="6"
          placeholder="Enter valid JSON"
          @input="handleValueUpdate"
        ></textarea>
        <DxDateBox
          v-else-if="formData.type === 'TIMESTAMP'"
          :value="formData.value"
          type="datetime"
          display-format="MMM dd, yyyy, hh:mm a"
          :use-mask-behavior="true"
          :show-clear-button="true"
          placeholder="Jan 01, 2025, 12:00 AM"
          @value-changed="handleTimestampUpdate"
        />
        <input
          v-else-if="formData.type === 'BOOLEAN'"
          :value="formData.value"
          type="text"
          class="form-input"
          placeholder="true or false"
          @input="handleValueUpdate"
        />
        <input
          v-else-if="formData.type === 'NUMBER'"
          :value="formData.value"
          type="number"
          class="form-input"
          placeholder="Enter number"
          @input="handleValueUpdate"
        />
        <input
          v-else
          :value="formData.value"
          type="text"
          class="form-input"
          placeholder="Enter text"
          @input="handleValueUpdate"
        />
      </div>

      <div class="form-group">
        <label class="form-label">Description</label>
        <textarea
          :value="formData.description"
          class="form-textarea"
          rows="3"
          placeholder="Description (read-only)"
          disabled
          readonly
        ></textarea>
      </div>
    </form>

    <template #footer>
      <div class="modal-footer-actions">
        <DxButton
          text="Cancel"
          type="normal"
          styling-mode="outlined"
          :disabled="isSaving"
          @click="$emit('close')"
        />
        <DxButton
          :text="isSaving ? 'Saving...' : 'Update Changes'"
          type="default"
          styling-mode="contained"
          :disabled="isSaving || !formData.id?.trim() || !formData.type || !formData.value?.trim()"
          @click="$emit('save')"
        />
      </div>
    </template>
  </AppModal>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { DxDateBox } from 'devextreme-vue/date-box';
import { DxButton } from 'devextreme-vue/button';
import type { ValueChangedEvent } from 'devextreme/ui/date_box';
import AppModal from '@/components/common/AppModal.vue';

// Types
type ConfigType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON' | 'TIMESTAMP';

interface FormData {
  id: string;
  type: ConfigType;
  value: string;
  description: string;
}

interface Props {
  show: boolean;
  isSaving: boolean;
  saveError?: string | null;
  formData: FormData;
}

defineProps<Props>();

const emit = defineEmits<{
  close: [];
  save: [];
  'update:id': [value: string];
  'update:type': [value: ConfigType];
  'update:value': [value: string];
  'update:description': [value: string];
}>();

const modalTitle = computed(() => {
  return 'Edit Configuration';
});

// Event handlers with improved TypeScript safety
const handleIdUpdate = (event: Event): void => {
  if (event.target && 'value' in event.target) {
    emit('update:id', (event.target as { value: string }).value);
  }
};

const handleTypeUpdate = (event: Event): void => {
  if (event.target && 'value' in event.target) {
    emit('update:type', (event.target as { value: string }).value as ConfigType);
  }
};

const handleValueUpdate = (event: Event): void => {
  if (event.target && 'value' in event.target) {
    emit('update:value', (event.target as { value: string }).value);
  }
};

const handleTimestampUpdate = (e: ValueChangedEvent): void => {
  if (e.value) {
    // Convert Date to ISO-8601 UTC string
    const isoString = e.value instanceof Date ? e.value.toISOString() : String(e.value);
    emit('update:value', isoString);
  } else {
    emit('update:value', '');
  }
};
</script>

<script lang="ts">
export default {
  name: 'SiteConfigModal',
};
</script>

<style scoped>
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.error-message {
  padding: 0.5rem 0.75rem;
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  color: #dc2626;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-label {
  font-weight: 600;
  color: #374151;
  font-size: 0.875rem;
}

.form-label.required::after {
  content: ' *';
  color: #dc2626;
}

.form-input,
.form-select,
.form-textarea {
  padding: 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.875rem;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input:disabled,
.form-input:read-only {
  background-color: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}

.form-textarea {
  resize: vertical;
  min-height: 40px;
}

.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}
</style>
