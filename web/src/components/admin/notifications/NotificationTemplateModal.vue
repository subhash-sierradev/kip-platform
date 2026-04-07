<template>
  <AppModal
    :open="show"
    title="Add Notification Template"
    size="md"
    @update:open="v => !v && $emit('close')"
  >
    <form class="modal-form" @submit.prevent="handleSubmit">
      <div class="form-group">
        <label class="form-label required">Event</label>
        <select v-model="form.eventId" class="form-select" required>
          <option value="" disabled>Select an event</option>
          <option v-for="event in activeEvents" :key="event.id" :value="event.id">
            {{ event.displayName }} ({{ event.eventKey }})
          </option>
        </select>
      </div>

      <div class="form-group">
        <label class="form-label required">Title Template</label>
        <input
          v-model="form.titleTemplate"
          type="text"
          class="form-input"
          placeholder="e.g. Integration {{name}} has failed"
          required
        />
        <span class="form-hint" v-pre>Use {{ variableName }} for dynamic values</span>
      </div>

      <div class="form-group">
        <label class="form-label required">Message Template</label>
        <textarea
          v-model="form.messageTemplate"
          class="form-textarea"
          rows="5"
          placeholder="e.g. The integration {{name}} failed at {{timestamp}} with error: {{errorMessage}}"
          required
        />
        <span class="form-hint" v-pre>Use {{ variableName }} for dynamic values</span>
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
import { reactive, computed, watch } from 'vue';
import { DxButton } from 'devextreme-vue/button';
import AppModal from '@/components/common/AppModal.vue';
import type { NotificationEventCatalogResponse, CreateNotificationTemplateRequest } from '@/api';

interface Props {
  show: boolean;
  isSaving: boolean;
  events: NotificationEventCatalogResponse[];
}

const props = defineProps<Props>();
const emit = defineEmits<{
  close: [];
  save: [payload: CreateNotificationTemplateRequest];
}>();

const activeEvents = computed(() => props.events.filter(e => e.isEnabled));

const form = reactive<CreateNotificationTemplateRequest>({
  eventId: '',
  titleTemplate: '',
  messageTemplate: '',
});

function resetForm() {
  form.eventId = '';
  form.titleTemplate = '';
  form.messageTemplate = '';
}

watch(
  () => props.show,
  val => {
    if (!val) resetForm();
  }
);

function handleSubmit() {
  const payload: CreateNotificationTemplateRequest = {
    eventId: form.eventId,
    titleTemplate: form.titleTemplate.trim(),
    messageTemplate: form.messageTemplate.trim(),
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
.form-select,
.form-textarea {
  padding: 8px 12px;
  border: 1px solid var(--kw-border, #e5e7eb);
  border-radius: 6px;
  font-size: 0.875rem;
  width: 100%;
  box-sizing: border-box;
  outline: none;
  background: #fff;
  transition: border-color 0.15s;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: var(--kw-primary, #3b82f6);
}

.form-textarea {
  resize: vertical;
}

.form-hint {
  font-size: 0.75rem;
  color: var(--kw-text-secondary, #6b7280);
}
</style>
