<template>
  <AppModal
    :open="show"
    :title="existingRule ? 'Edit Notification Rule' : 'Add Notification Rule'"
    size="sm"
    @update:open="v => !v && $emit('close')"
  >
    <form class="modal-form" @submit.prevent="handleSubmit">
      <div v-if="!existingRule" class="form-group">
        <label class="form-label required">Event</label>
        <select v-model="form.eventId" class="form-select" required>
          <option value="" disabled>Select an event</option>
          <option v-for="event in activeEvents" :key="event.id" :value="event.id">
            {{ event.displayName }} ({{ event.eventKey }})
          </option>
        </select>
      </div>

      <div v-if="existingRule" class="form-group">
        <label class="form-label">Event</label>
        <div class="form-readonly">{{ existingRule.eventKey }}</div>
      </div>

      <div class="form-group">
        <label class="form-label required">Alert Type</label>
        <select v-model="form.severity" class="form-select" required>
          <option value="" disabled>Select alert type</option>
          <option v-for="sev in severities" :key="sev" :value="sev">{{ sev }}</option>
        </select>
      </div>
    </form>
    <template #footer>
      <DxButton text="Cancel" styling-mode="outlined" type="normal" @click="$emit('close')" />
      <DxButton
        :text="existingRule ? 'Update' : 'Save'"
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
import { NotificationSeverity } from '@/api';
import type {
  NotificationEventCatalogResponse,
  CreateNotificationRuleRequest,
  NotificationRuleResponse,
} from '@/api';

interface Props {
  show: boolean;
  isSaving: boolean;
  events: NotificationEventCatalogResponse[];
  rules: NotificationRuleResponse[];
  existingRule?: NotificationRuleResponse;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  close: [];
  save: [payload: CreateNotificationRuleRequest];
  update: [severity: string];
}>();

const severities = Object.values(NotificationSeverity);

const activeEvents = computed(() => {
  const usedEventIds = new Set(props.rules.map(r => r.eventId).filter(Boolean));
  return props.events.filter(e => e.isEnabled && !usedEventIds.has(e.id));
});

const form = reactive({
  eventId: '',
  severity: '' as NotificationSeverity | '',
});

function resetForm() {
  form.eventId = '';
  form.severity = (props.existingRule?.severity as NotificationSeverity | undefined) ?? '';
}

watch(
  () => props.show,
  val => {
    if (val) resetForm();
    else {
      form.eventId = '';
      form.severity = '';
    }
  }
);

function handleSubmit() {
  if (props.existingRule) {
    emit('update', form.severity as string);
  } else {
    const payload: CreateNotificationRuleRequest = {
      eventId: form.eventId,
      severity: form.severity as NotificationSeverity,
      isEnabled: true,
    };
    emit('save', payload);
  }
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
.form-select {
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
.form-select:focus {
  border-color: var(--kw-primary, #3b82f6);
}

.form-readonly {
  padding: 8px 12px;
  border: 1px solid var(--kw-border, #e5e7eb);
  border-radius: 6px;
  font-size: 0.875rem;
  background: var(--kw-surface-secondary, #f9fafb);
  color: var(--kw-text-secondary, #6b7280);
}
</style>
