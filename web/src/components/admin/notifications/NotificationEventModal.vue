<template>
  <AppModal
    :open="show"
    title="Add Event to Catalog"
    size="sm"
    @update:open="v => !v && $emit('close')"
  >
    <form class="modal-form" @submit.prevent="handleSubmit">
      <div class="form-group">
        <label class="form-label required">Event Key</label>
        <input
          v-model="form.eventKey"
          type="text"
          class="form-input"
          placeholder="e.g. INTEGRATION_FAILED"
          required
        />
      </div>

      <div class="form-group">
        <label class="form-label required">Entity Type</label>
        <DxSelectBox
          v-model="form.entityType"
          :items="entityTypeOptions"
          placeholder="Select entity type"
          :show-clear-button="false"
        />
      </div>

      <div class="form-group">
        <label class="form-label required">Display Name</label>
        <input
          v-model="form.displayName"
          type="text"
          class="form-input"
          placeholder="e.g. Integration Failed"
          required
        />
      </div>

      <div class="form-group">
        <label class="form-label">Description</label>
        <textarea
          v-model="form.description"
          class="form-textarea"
          rows="3"
          placeholder="Optional description for this event"
        />
      </div>
    </form>
    <template #footer>
      <DxButton text="Cancel" styling-mode="outlined" type="normal" @click="$emit('close')" />
      <DxButton
        text="Save"
        styling-mode="contained"
        type="default"
        :disabled="isSaving"
        @click="handleSubmit"
      />
    </template>
  </AppModal>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue';
import { DxButton } from 'devextreme-vue/button';
import { DxSelectBox } from 'devextreme-vue/select-box';
import AppModal from '@/components/common/AppModal.vue';
import type { CreateEventCatalogRequest } from '@/api';
import { NotificationEntityType } from '@/api';

interface Props {
  show: boolean;
  isSaving: boolean;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  close: [];
  save: [payload: CreateEventCatalogRequest];
}>();

const entityTypeOptions = Object.values(NotificationEntityType);

interface EventForm {
  eventKey: string;
  entityType: NotificationEntityType | '';
  displayName: string;
  description: string;
}

const form = reactive<EventForm>({
  eventKey: '',
  entityType: '',
  displayName: '',
  description: '',
});

function resetForm() {
  form.eventKey = '';
  form.entityType = '';
  form.displayName = '';
  form.description = '';
}

watch(
  () => props.show,
  val => {
    if (!val) resetForm();
  }
);

function handleSubmit() {
  const payload: CreateEventCatalogRequest = {
    eventKey: form.eventKey.trim().toUpperCase(),
    entityType: form.entityType as NotificationEntityType,
    displayName: form.displayName.trim(),
    description: form.description?.trim() || undefined,
  };
  emit('save', payload);
}
</script>

<style scoped>
.modal-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #1a1a2e !important;
}

.form-label.required::after {
  content: ' *';
  color: var(--kw-error, #ef4444);
}

.form-input,
.form-textarea {
  padding: 8px 12px;
  border: 1px solid var(--kw-border, #e5e7eb);
  border-radius: 6px;
  font-size: 0.875rem;
  width: 100%;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.15s;
}

.form-input:focus,
.form-textarea:focus {
  border-color: var(--kw-primary, #3b82f6);
}

.form-textarea {
  resize: vertical;
}
</style>
