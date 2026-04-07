<template>
  <div>
    <GenericDataGrid
      ref="recipientsGridRef"
      :data="filteredPolicies"
      :columns="recipientColumns"
      :page-size="10"
      :enable-export="false"
    >
      <template #recipientTypeTemplate="{ data: row }">
        <div class="recipient-type-cell">
          <StatusChipForDataTable
            v-if="row.recipientType"
            :status="row.recipientType"
            :label="row.recipientType.replace('_', ' ')"
          />
          <span v-else class="unset-label">Not set</span>
          <div
            v-if="row.recipientType === 'SELECTED_USERS' && row.users?.length"
            class="user-chips-list"
          >
            <span v-for="user in row.users.slice(0, 2)" :key="user.userId" class="user-chip">
              <span
                class="user-chip__avatar"
                :style="{ background: getChipColor(user.userId ?? '') }"
                >{{ getInitials(user.displayName) }}</span
              >
              <span class="user-chip__name">{{ user.displayName ?? user.userId }}</span>
            </span>
            <button
              v-if="row.users.length > 2"
              type="button"
              class="user-chip user-chip--overflow"
              :aria-describedby="usersTooltipId"
              @mouseenter="onUsersTooltipEnter($event, row)"
              @mousemove="moveTooltip"
              @mouseleave="hideTooltipWithReset"
              @focus="onUsersTooltipFocus($event, row)"
              @blur="hideTooltipWithReset"
            >
              +{{ row.users.length - 2 }}
            </button>
          </div>
        </div>
      </template>
      <template #recipientUpdatedByTemplate="{ data: row }">
        <span class="truncated-cell">{{ row.lastModifiedBy || '\u2014' }}</span>
      </template>
      <template #recipientDateTemplate="{ data: row }">
        {{ formatDate(row.lastModifiedDate, { includeTime: true }) }}
      </template>
      <template #recipientActionTemplate="{ data: row }">
        <div class="rule-action-cell">
          <DxButton
            icon="edit"
            styling-mode="text"
            type="normal"
            hint="Edit Recipients"
            @click="openPolicyModal(row.ruleId)"
          />
          <DxButton
            icon="trash"
            styling-mode="text"
            type="normal"
            hint="Delete"
            @click="handleDeletePolicy(row.id)"
          />
        </div>
      </template>
    </GenericDataGrid>

    <NotificationPolicyModal
      :show="showPolicyModal"
      :rules="rulesForPolicyModal"
      :users="users"
      :is-saving="saving"
      :preselected-rule-id="preselectedRuleId"
      :existing-policy="existingPolicyForModal"
      @close="closePolicyModal"
      @save="handleSavePolicy"
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

    <CommonTooltip :id="usersTooltipId" :visible="tooltip.visible" :x="tooltip.x" :y="tooltip.y">
      <div class="recipient-users-tooltip-content">
        <div class="recipient-users-tooltip-title">
          Selected Users ({{ usersTooltipNames.length }})
        </div>
        <ul class="recipient-users-tooltip-list">
          <li v-for="name in usersTooltipNames" :key="name" class="recipient-users-tooltip-item">
            {{ name }}
          </li>
        </ul>
      </div>
    </CommonTooltip>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue';
import { DxButton } from 'devextreme-vue/button';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import CommonTooltip from '@/components/common/Tooltip.vue';
import NotificationPolicyModal from './NotificationPolicyModal.vue';
import { useConfirmationDialog } from '@/composables/useConfirmationDialog';
import { useTooltip } from '@/composables/useTooltip';
import { formatDate } from '@/utils/dateUtils';
import { getInitials } from '@/utils/stringFormatUtils';
import type {
  RecipientPolicyResponse,
  NotificationRuleResponse,
  UserProfileResponse,
  CreateRecipientPolicyRequest,
} from '@/api';

import { recipientColumns } from './notificationsPageColumns';

const props = defineProps<{
  filteredPolicies: RecipientPolicyResponse[];
  policies: RecipientPolicyResponse[];
  availableRulesForPolicy: NotificationRuleResponse[];
  rules: NotificationRuleResponse[];
  users: UserProfileResponse[];
  saving: boolean;
  fetchUsers: () => Promise<void>;
  upsertPolicy: (payload: CreateRecipientPolicyRequest, existingId?: string) => Promise<boolean>;
  deletePolicy: (id: string) => Promise<void>;
}>();

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
    desc: 'Are you sure you want to delete this recipient policy? This action cannot be undone.',
    label: 'Delete',
  },
});

const recipientsGridRef = ref<{ instance?: { refresh(): void } } | null>(null);
const showPolicyModal = ref(false);
const preselectedRuleId = ref<string | undefined>(undefined);
const existingPolicyForModal = ref<RecipientPolicyResponse | undefined>(undefined);
const pendingDeletePolicyId = ref<string | null>(null);
const usersTooltipId = 'recipients-users-tooltip';
const { tooltip, showTooltip, moveTooltip, hideTooltip } = useTooltip();
const usersTooltipNames = ref<string[]>([]);

const CHIP_COLORS = ['#2c4a7c', '#1e6b5a', '#5c3d7a', '#7a4a1e', '#1e4a5c', '#4a2c7a'];

function getChipColor(userId: string): string {
  let hash = 0;
  for (let i = 0; i < userId.length; i++) hash = (hash + userId.charCodeAt(i)) % CHIP_COLORS.length;
  return CHIP_COLORS[hash];
}

function onUsersTooltipEnter(event: MouseEvent, policy: RecipientPolicyResponse): void {
  const users = policy.users ?? [];
  usersTooltipNames.value = users.length
    ? users.map(user => user.displayName || user.userId || 'Unknown User')
    : ['No users configured'];
  showTooltip(event, `Selected Users (${usersTooltipNames.value.length})`);
}

function onUsersTooltipFocus(event: FocusEvent, policy: RecipientPolicyResponse): void {
  const target = event.currentTarget as HTMLElement | null;
  if (!target) {
    return;
  }
  const rect = target.getBoundingClientRect();
  const syntheticMouseEvent = {
    clientX: rect.left + rect.width / 2,
    clientY: rect.top + rect.height / 2,
  } as MouseEvent;
  onUsersTooltipEnter(syntheticMouseEvent, policy);
}

function hideTooltipWithReset(): void {
  usersTooltipNames.value = [];
  hideTooltip();
}

const rulesForPolicyModal = computed(() => {
  if (!preselectedRuleId.value) return props.availableRulesForPolicy;
  const preselected = props.rules.find(r => r.id === preselectedRuleId.value);
  const alreadyIn = props.availableRulesForPolicy.some(r => r.id === preselectedRuleId.value);
  return preselected && !alreadyIn
    ? [preselected, ...props.availableRulesForPolicy]
    : props.availableRulesForPolicy;
});

async function openPolicyModal(ruleId?: string): Promise<void> {
  preselectedRuleId.value = ruleId;
  existingPolicyForModal.value = ruleId ? props.policies.find(p => p.ruleId === ruleId) : undefined;
  if (!props.users.length) {
    await props.fetchUsers();
  }
  showPolicyModal.value = true;
}

function closePolicyModal(): void {
  showPolicyModal.value = false;
  preselectedRuleId.value = undefined;
  existingPolicyForModal.value = undefined;
}

async function handleSavePolicy(payload: CreateRecipientPolicyRequest): Promise<void> {
  const ok = await props.upsertPolicy(payload, existingPolicyForModal.value?.id);
  if (ok) {
    closePolicyModal();
    await nextTick();
    recipientsGridRef.value?.instance?.refresh();
  }
}

function handleDeletePolicy(id: string): void {
  pendingDeletePolicyId.value = id;
  openDeleteDialog('delete');
}

async function handleConfirm(): Promise<void> {
  await confirmWithHandlers({
    delete: async () => {
      if (pendingDeletePolicyId.value) {
        await props.deletePolicy(pendingDeletePolicyId.value);
        pendingDeletePolicyId.value = null;
        return true;
      }
      return false;
    },
  });
}

defineExpose({ openPolicyModal });
</script>

<style src="./NotificationsPage.css" scoped></style>

<style scoped>
.recipient-users-tooltip-content {
  min-width: 220px;
  max-width: 320px;
}

.recipient-users-tooltip-title {
  font-size: 12px;
  font-weight: 700;
  margin-bottom: 6px;
}

.recipient-users-tooltip-list {
  margin: 0;
  padding-left: 16px;
}

.recipient-users-tooltip-item {
  font-size: 12px;
  line-height: 1.4;
  margin: 0 0 2px;
}
</style>
