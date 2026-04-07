<template>
  <div class="notifications-page">
    <div class="notifications-tabs-wrapper">
      <div class="notifications-tab-header">
        <TabNavigation
          :tabs="notificationsPageTabs"
          :active-tab="selectedTab"
          @tab-change="handleTabChange"
        />
        <div class="tab-header-actions">
          <button
            v-if="selectedTab === 'rules'"
            class="section-primary-btn"
            :disabled="availableEventsForRule.length === 0"
            :title="
              availableEventsForRule.length === 0
                ? 'All events already have a rule configured'
                : 'Add a new notification rule'
            "
            @click="rulesTabRef?.openRuleModal()"
          >
            <svg
              class="section-primary-btn__icon"
              viewBox="0 0 14 14"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              aria-hidden="true"
            >
              <path
                d="M7 1v12M1 7h12"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
              />
            </svg>
            <span>Add Rule</span>
          </button>
          <button
            v-if="selectedTab === 'recipients'"
            class="section-primary-btn"
            :disabled="availableRulesForPolicy.length === 0"
            :title="
              availableRulesForPolicy.length === 0
                ? 'All rules already have recipients configured'
                : 'Add recipients to a notification rule'
            "
            @click="recipientsTabRef?.openPolicyModal(undefined)"
          >
            <svg
              class="section-primary-btn__icon"
              viewBox="0 0 14 14"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              aria-hidden="true"
            >
              <path
                d="M7 1v12M1 7h12"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
              />
            </svg>
            <span>Add Recipients</span>
          </button>
          <button
            :class="['tab-reset-btn', { 'tab-reset-btn--loading': isResetting }]"
            :disabled="isResetting"
            title="Reset all notification rules and recipients to defaults"
            @click="handleResetToDefaults()"
          >
            <svg
              class="tab-reset-btn__icon"
              viewBox="0 0 16 16"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              aria-hidden="true"
            >
              <path
                d="M2 8a6 6 0 1 0 .85-3.15"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
              />
              <path
                d="M2 3.5v4h4"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
            <span>{{ isResetting ? 'Resetting…' : 'Reset to Defaults' }}</span>
          </button>
        </div>
      </div>
      <div class="tab-content">
        <!-- Event Catalog Tab -->
        <div v-show="selectedTab === 'events'">
          <GenericDataGrid
            :data="events"
            :columns="eventColumns"
            :page-size="10"
            :enable-export="false"
          >
            <template #eventKeyTemplate="{ data: row }">
              <span class="truncated-cell">{{ row.eventKey }}</span>
            </template>
            <template #descriptionTemplate="{ data: row }">
              <span class="truncated-cell">{{ row.description }}</span>
            </template>
            <template #notifyInitiatorTemplate="{ data: row }">
              <StatusChipForDataTable
                :status="row.notifyInitiator ? 'ACTIVE' : 'INACTIVE'"
                :label="row.notifyInitiator ? 'Yes' : 'No'"
              />
            </template>
          </GenericDataGrid>
        </div>

        <!-- Notification Rules Tab -->
        <div v-show="selectedTab === 'rules'">
          <NotificationsRulesTab
            ref="rulesTabRef"
            :filtered-rules="filteredRules"
            :rules="rules"
            :events="events"
            :saving="saving"
            :toggle-rule="toggleRule"
            :create-rule="createRule"
            :update-rule="updateRule"
            :delete-rule="deleteRule"
          />
        </div>

        <!-- Templates Tab -->
        <div v-show="selectedTab === 'templates'">
          <GenericDataGrid
            :data="filteredTemplates"
            :columns="templateColumns"
            :page-size="10"
            :enable-export="false"
          />
        </div>

        <!-- Recipients Tab -->
        <div v-show="selectedTab === 'recipients'">
          <NotificationsRecipientsTab
            ref="recipientsTabRef"
            :filtered-policies="filteredPolicies"
            :policies="policies"
            :available-rules-for-policy="availableRulesForPolicy"
            :rules="rules"
            :users="users"
            :saving="saving"
            :fetch-users="fetchUsers"
            :upsert-policy="upsertPolicy"
            :delete-policy="deletePolicy"
          />
        </div>
      </div>
    </div>

    <!-- Modals -->
    <NotificationEventModal
      :show="showEventModal"
      :is-saving="saving"
      @close="closeEventModal"
      @save="handleSaveEvent"
    />

    <ConfirmationDialog
      :open="showConfirmDialog"
      :type="'custom'"
      :confirm-color="'error'"
      :title="'Reset Notification Rules to Defaults'"
      :description="'This will restore all notification rules and recipients to defaults for your assigned features. Custom templates will be preserved. Continue?'"
      :confirm-label="'Reset'"
      :loading="isResetting"
      @cancel="cancelReset"
      @confirm="handleConfirmReset"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import TabNavigation from '@/components/common/TabNavigation.vue';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import NotificationEventModal from './NotificationEventModal.vue';
import NotificationsRulesTab from './NotificationsRulesTab.vue';
import NotificationsRecipientsTab from './NotificationsRecipientsTab.vue';
import { useNotificationAdmin } from '@/composables/useNotificationAdmin';
import { useNotificationReset } from '@/composables/useNotificationReset';
import type { CreateEventCatalogRequest } from '@/api';
import { notificationsPageTabs, eventColumns, templateColumns } from './notificationsPageColumns';

const {
  events,
  rules,
  policies,
  filteredRules,
  filteredTemplates,
  filteredPolicies,
  users,
  saving,
  fetchEvents,
  fetchRules,
  fetchTemplates,
  fetchPolicies,
  fetchUsers,
  createEvent,
  createRule,
  updateRule,
  deleteRule,
  toggleRule,
  upsertPolicy,
  deletePolicy,
} = useNotificationAdmin();

const { showConfirmDialog, isResetting, resetToDefaults, confirmReset, cancelReset } =
  useNotificationReset();

const selectedTab = ref('events');
const rulesTabRef = ref<InstanceType<typeof NotificationsRulesTab> | null>(null);
const recipientsTabRef = ref<InstanceType<typeof NotificationsRecipientsTab> | null>(null);
const showEventModal = ref(false);

const configuredRuleIds = computed(
  () => new Set(filteredPolicies.value.map(p => p.ruleId).filter(Boolean))
);
const availableRulesForPolicy = computed(() =>
  filteredRules.value.filter(r => !configuredRuleIds.value.has(r.id))
);
const usedEventIds = computed(
  () => new Set(filteredRules.value.map(r => r.eventId).filter(Boolean))
);
const availableEventsForRule = computed(() =>
  events.value.filter(e => !usedEventIds.value.has(e.id))
);

onMounted(() => {
  fetchEvents();
  fetchTemplates();
  fetchRules();
});

function handleTabChange(tab: string): void {
  selectedTab.value = tab;
  if (tab === 'rules') {
    fetchRules();
  } else if (tab === 'recipients') {
    fetchPolicies();
  }
}

function handleResetToDefaults(): void {
  resetToDefaults();
}

async function handleConfirmReset(): Promise<void> {
  try {
    await confirmReset();
    await fetchRules();
    await fetchPolicies();
  } catch {
    // error is handled and toasted inside confirmReset
  }
}

function closeEventModal(): void {
  showEventModal.value = false;
}

async function handleSaveEvent(payload: CreateEventCatalogRequest): Promise<void> {
  const ok = await createEvent(payload);
  if (ok) closeEventModal();
}
</script>

<style src="./NotificationsPage.css" scoped></style>
