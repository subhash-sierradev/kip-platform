<template>
  <AppModal
    :open="show"
    :title="existingPolicy ? 'Edit Recipient Policy' : 'Set Recipient Policy'"
    size="sm"
    @update:open="v => !v && $emit('close')"
  >
    <form class="modal-form" @submit.prevent="handleSubmit">
      <div class="form-group">
        <label class="form-label required">Rule</label>
        <select v-model="form.ruleId" class="form-select" :disabled="!!preselectedRuleId" required>
          <option value="" disabled>Select a rule</option>
          <option v-for="rule in rules" :key="rule.id" :value="rule.id">
            {{ rule.eventKey }} — {{ rule.severity }}
          </option>
        </select>
      </div>

      <div class="form-group">
        <label class="form-label required">Recipient Type</label>
        <select v-model="form.recipientType" class="form-select" required>
          <option value="" disabled>Select recipient type</option>
          <option v-for="type in recipientTypes" :key="type.value" :value="type.value">
            {{ type.label }}
          </option>
        </select>
        <span class="form-hint">Determines who receives notifications triggered by this rule</span>
      </div>

      <div v-if="form.recipientType === 'SELECTED_USERS'" class="form-group">
        <label class="form-label required">Select Users</label>
        <DxTagBox
          v-model:value="form.userIds"
          :data-source="users"
          display-expr="displayName"
          value-expr="keycloakUserId"
          :search-enabled="true"
          :drop-down-options="{ wrapperAttr: { class: 'recipient-users-dropdown' } }"
          placeholder="Search and select users..."
          :show-selection-controls="true"
          :max-display-tags="5"
          styling-mode="outlined"
        />
        <span class="form-hint">Only selected users will receive notifications</span>
      </div>
    </form>
    <template #footer>
      <DxButton text="Cancel" styling-mode="outlined" type="normal" @click="$emit('close')" />
      <DxButton
        :text="existingPolicy ? 'Update' : 'Save'"
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
import { DxTagBox } from 'devextreme-vue/tag-box';
import AppModal from '@/components/common/AppModal.vue';
import type {
  NotificationRuleResponse,
  RecipientPolicyResponse,
  CreateRecipientPolicyRequest,
  UserProfileResponse,
} from '@/api';

interface Props {
  show: boolean;
  isSaving: boolean;
  rules: NotificationRuleResponse[];
  users?: UserProfileResponse[];
  preselectedRuleId?: string;
  existingPolicy?: RecipientPolicyResponse;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  close: [];
  save: [payload: CreateRecipientPolicyRequest];
}>();

const recipientTypes = [
  { value: 'ALL_USERS', label: 'All Users' },
  { value: 'ADMINS_ONLY', label: 'Admins Only' },
  { value: 'SELECTED_USERS', label: 'Selected Users' },
];

const form = reactive({
  ruleId: '',
  recipientType: '',
  userIds: [] as string[],
});

watch(
  () => props.show,
  val => {
    if (val) {
      form.ruleId = props.preselectedRuleId ?? '';
      form.recipientType = props.existingPolicy?.recipientType ?? '';
      form.userIds = props.existingPolicy?.users
        ? props.existingPolicy.users.map(u => u.userId).filter((id): id is string => !!id)
        : [];
    } else {
      form.ruleId = '';
      form.recipientType = '';
      form.userIds = [];
    }
  }
);

watch(
  () => props.preselectedRuleId,
  val => {
    if (val) form.ruleId = val;
  }
);

function handleSubmit() {
  if (!form.ruleId || !form.recipientType) return;
  const payload: CreateRecipientPolicyRequest = {
    ruleId: form.ruleId,
    recipientType: form.recipientType,
  };
  if (form.recipientType === 'SELECTED_USERS' && form.userIds.length > 0) {
    payload.userIds = form.userIds;
  }
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

.form-select:focus {
  border-color: var(--kw-primary, #3b82f6);
}

.form-select:disabled {
  background: #f3f4f6;
  color: #6b7280;
  cursor: not-allowed;
}

.form-hint {
  font-size: 0.75rem;
  color: var(--kw-text-secondary, #6b7280);
}
</style>

<style>
.recipient-users-dropdown .dx-list-select-all,
.recipient-users-dropdown .dx-list-item {
  min-height: 34px;
}

.recipient-users-dropdown .dx-list-item-content {
  padding-top: 6px;
  padding-bottom: 6px;
  line-height: 1.25;
}

.recipient-users-dropdown .dx-list-select-all .dx-list-item-content {
  padding-top: 6px;
  padding-bottom: 6px;
}
</style>
