<template>
  <div>
    <GenericDataGrid
      ref="rulesGridRef"
      :data="filteredRules"
      :columns="ruleColumns"
      :page-size="10"
      :enable-export="false"
    >
      <template #severityTemplate="{ data: row }">
        <StatusChipForDataTable :status="row.severity" :label="row.severity" />
      </template>
      <template #enabledTemplate="{ data: row }">
        <StatusChipForDataTable
          :status="getRuleEnabled(row.id) ? 'ACTIVE' : 'INACTIVE'"
          :label="getRuleEnabled(row.id) ? 'Enabled' : 'Disabled'"
        />
      </template>
      <template #dateTemplate="{ data: row }">
        {{ formatDate(row.lastModifiedDate, { includeTime: true }) }}
      </template>
      <template #ruleUpdatedByTemplate="{ data: row }">
        <span class="truncated-cell">{{ row.lastModifiedBy || '—' }}</span>
      </template>
      <template #actionTemplate="{ data: row }">
        <div class="rule-action-cell">
          <div
            class="toggle-interceptor"
            :class="{ 'toggle-interceptor--disabled': !!rowToggleLoading[row.id] }"
            @click.stop.prevent="handleToggleClick(row.id)"
          >
            <DxSwitch :value="getRuleEnabled(row.id)" :disabled="!!rowToggleLoading[row.id]" />
          </div>
          <DxButton
            icon="edit"
            styling-mode="text"
            type="normal"
            hint="Edit Alert Type"
            @click="openRuleModal(row.id)"
          />
          <DxButton
            icon="trash"
            styling-mode="text"
            type="normal"
            hint="Delete Rule"
            @click="handleDeleteRule(row.id)"
          />
        </div>
      </template>
    </GenericDataGrid>

    <NotificationRuleModal
      :show="showRuleModal"
      :events="events"
      :rules="rules"
      :is-saving="saving"
      :existing-rule="existingRuleForModal"
      @close="closeRuleModal"
      @save="handleSaveRule"
      @update="handleUpdateRule"
    />

    <ConfirmationDialog
      :open="toggleDialogOpen"
      :type="togglePendingAction || 'custom'"
      :title="toggleDialogTitle"
      :description="toggleDialogDescription"
      :confirm-label="toggleDialogConfirmLabel"
      :loading="toggleActionLoading"
      @cancel="handleToggleCancel"
      @confirm="handleToggleConfirm"
    />

    <ConfirmationDialog
      :open="deleteDialogOpen"
      :type="deletePendingAction || 'custom'"
      :title="deleteDialogTitle"
      :description="deleteDialogDescription"
      :confirm-label="deleteDialogConfirmLabel"
      :loading="deleteActionLoading"
      @cancel="closeDeleteDialog"
      @confirm="handleConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, toRef } from 'vue';
import { DxButton } from 'devextreme-vue/button';
import { DxSwitch } from 'devextreme-vue/switch';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import NotificationRuleModal from './NotificationRuleModal.vue';
import { useNotificationRulesToggle } from '@/composables/useNotificationRulesToggle';
import { useConfirmationDialog } from '@/composables/useConfirmationDialog';
import { formatDate } from '@/utils/dateUtils';
import type {
  NotificationRuleResponse,
  NotificationEventCatalogResponse,
  CreateNotificationRuleRequest,
} from '@/api';

import { ruleColumns } from './notificationsPageColumns';

const props = defineProps<{
  filteredRules: NotificationRuleResponse[];
  rules: NotificationRuleResponse[];
  events: NotificationEventCatalogResponse[];
  saving: boolean;
  toggleRule: (id: string) => Promise<boolean>;
  createRule: (payload: CreateNotificationRuleRequest) => Promise<boolean>;
  updateRule: (id: string, severity: string) => Promise<boolean>;
  deleteRule: (id: string) => Promise<void>;
}>();

const {
  rulesGridRef,
  rowToggleLoading,
  getRuleEnabled,
  handleToggleClick,
  handleToggleCancel,
  handleToggleConfirm,
  toggleDialogOpen,
  toggleActionLoading,
  togglePendingAction,
  toggleDialogTitle,
  toggleDialogDescription,
  toggleDialogConfirmLabel,
} = useNotificationRulesToggle(props.toggleRule, toRef(props, 'rules'));

const {
  dialogOpen: deleteDialogOpen,
  actionLoading: deleteActionLoading,
  pendingAction: deletePendingAction,
  dialogTitle: deleteDialogTitle,
  dialogDescription: deleteDialogDescription,
  dialogConfirmLabel: deleteDialogConfirmLabel,
  openDialog: openDeleteDialog,
  closeDialog: closeDeleteDialog,
  confirmWithHandlers,
} = useConfirmationDialog({
  delete: {
    title: 'Confirm Delete',
    desc: 'Are you sure you want to delete this rule? This action cannot be undone.',
    label: 'Delete',
  },
});

const showRuleModal = ref(false);
const existingRuleForModal = ref<NotificationRuleResponse | undefined>(undefined);
const pendingDeleteRuleId = ref<string | null>(null);

function openRuleModal(ruleId?: string): void {
  existingRuleForModal.value = ruleId ? props.rules.find(r => r.id === ruleId) : undefined;
  showRuleModal.value = true;
}

function closeRuleModal(): void {
  showRuleModal.value = false;
  existingRuleForModal.value = undefined;
}

async function handleSaveRule(payload: CreateNotificationRuleRequest): Promise<void> {
  const ok = await props.createRule(payload);
  if (ok) closeRuleModal();
}

async function handleUpdateRule(severity: string): Promise<void> {
  if (!existingRuleForModal.value?.id) return;
  const ok = await props.updateRule(existingRuleForModal.value.id, severity);
  if (ok) closeRuleModal();
}

function handleDeleteRule(id: string): void {
  pendingDeleteRuleId.value = id;
  openDeleteDialog('delete');
}

async function handleConfirm(): Promise<void> {
  await confirmWithHandlers({
    delete: async () => {
      if (pendingDeleteRuleId.value) {
        await props.deleteRule(pendingDeleteRuleId.value);
        pendingDeleteRuleId.value = null;
        return true;
      }
      return false;
    },
  });
}

defineExpose({ openRuleModal });
</script>

<style src="./NotificationsPage.css" scoped></style>
