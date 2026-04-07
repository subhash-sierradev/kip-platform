<template>
  <div class="webhook-history-tab">
    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner">
        <i class="dx-icon dx-icon-refresh spinning"></i>
      </div>
      <p>Loading webhook history...</p>
    </div>

    <template v-else>
      <!-- History DataGrid -->
      <div v-if="executionHistory.length > 0" class="history-content">
        <GenericDataGrid
          ref="dataGridRef"
          :data="executionHistory"
          :columns="gridColumns"
          :page-size="10"
          :enable-export="false"
          :enable-clear-filters="true"
          class="history-grid"
        >
          <template #triggeredAtTemplate="{ data: row }">
            {{ formatDate(row.triggeredAt) }}
          </template>
          <template #statusTemplate="{ data: row }">
            <StatusChipForDataTable :status="row.status" :label="row.status" />
          </template>
          <template #actionsTemplate="{ data: row }">
            <div class="actions-container">
              <!-- Jira Link -->
              <a
                v-if="getJiraLink(row) !== '#'"
                :href="getJiraLink(row)"
                class="action-link jira-link"
                target="_blank"
                :title="`Open ${getJiraTicket(row)} in Jira`"
              >
                {{ getJiraTicket(row) }}
              </a>
              <!-- Failed Status Actions -->
              <div v-if="row.status === 'FAILED'" class="failed-actions">
                <!-- Troubleshoot Button with Lucide Icon -->
                <button
                  type="button"
                  class="dx-widget dx-button dx-button-mode-outlined dx-button-normal dx-button-has-text troubleshoot-btn"
                  :title="'View detailed error information and retry options'"
                  @click="showTroubleshootDialog(row)"
                >
                  <div class="dx-button-content">
                    <Bug class="lucide-icon" :size="18" />
                    <span class="dx-button-text">Troubleshoot</span>
                  </div>
                </button>
                <!-- Retry Badge (if retry attempt > 0) -->
              </div>
              <span
                v-show="Number(row.retryAttempt) > 0"
                class="retry-badge"
                :title="`This is retry attempt #${row.retryAttempt}`"
                style="margin-left: -0.25rem"
              >
                Retry #{{ row.retryAttempt }}
              </span>
            </div>
          </template>
        </GenericDataGrid>
      </div>
      <!-- Empty State -->
      <div v-else class="empty-state">
        <i class="dx-icon dx-icon-clock empty-icon"></i>
        <h3>No Execution History</h3>
        <p>No webhook executions found for this configuration.</p>
      </div>
    </template>

    <!-- Troubleshoot Dialog -->
    <AppModal
      :open="troubleshootDialogVisible"
      title="Webhook Execution Error Details"
      size="lg"
      @update:open="v => !v && closeTroubleshootDialog()"
    >
      <div class="troubleshoot-content">
        <!-- Error Summary -->
        <div class="content-section">
          <h4>Response Body</h4>
          <p class="content-subtitle">
            The response from the target system that caused the failure:
          </p>
          <span v-if="currentExecution?.responseStatusCode" class="http-badge"
            >HTTP {{ currentExecution.responseStatusCode }}</span
          >
          <span v-else class="http-badge">HTTP 400</span>
          <pre v-if="currentExecution?.responseBody" class="code-content error-content">{{
            formatErrorContent(currentExecution.responseBody)
          }}</pre>
          <pre v-else class="code-content error-content">
{\n  \"errorMessages\": [],\n  \"errors\": {\n    \"summary\": \"You must specify a summary of the issue.\"\n  }\n}</pre
          >
        </div>

        <!-- Incoming Payload -->
        <div class="content-section">
          <h4>Incoming Payload</h4>
          <p class="content-subtitle">The original webhook payload that was being processed:</p>
          <pre v-if="currentExecution?.incomingPayload" class="code-content payload-content">{{
            formatErrorContent(currentExecution.incomingPayload)
          }}</pre>
          <pre v-else class="code-content payload-content">
{\n  \"widget\": {\n    \"debug\": \"on\",\n    \"window\": {\n      \"title\": \"Sample Konfabulator Widget\",\n      \"name\": \"main_window\",\n      \"width\": 500,\n      \"height\": 500\n    }\n  }\n}</pre
          >
        </div>
      </div>
      <template #footer>
        <div class="modal-footer-actions">
          <DxButton
            text="Close"
            type="normal"
            styling-mode="outlined"
            @click="closeTroubleshootDialog"
          />
          <DxButton
            text="Retry Webhook"
            type="default"
            styling-mode="contained"
            @click="handleRetryExecution"
          />
        </div>
      </template>
    </AppModal>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue';
import { type WebhookExecution } from '@/api/services/JiraWebhookService';
import { useWebhookActions } from '@/composables/useWebhookActions';
import { useWebhookHistoryData } from '@/composables/useWebhookHistoryData';
import { useTroubleshootDialog } from '@/composables/useTroubleshootDialog';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import { DxButton } from 'devextreme-vue/button';
import AppModal from '@/components/common/AppModal.vue';
import { formatMetadataDate } from '@/utils/dateUtils';
import './WebhookHistoryTab.css';
import { Bug } from 'lucide-vue-next';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';

// Helper functions
function getJiraLink(execution: WebhookExecution): string {
  return execution.jiraIssueUrl || '#';
}

function getJiraTicket(execution: WebhookExecution): string {
  if (execution.jiraIssueUrl) {
    const match = execution.jiraIssueUrl.match(/\/([A-Z]+-\d+)$/);
    return match ? match[1] : `KPC-${execution.id.slice(-2)}`;
  }
  return `KPC-${execution.id.slice(-2)}`;
}

function formatErrorContent(content: string): string {
  try {
    const parsed = JSON.parse(content);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return content;
  }
}

function formatDate(dateString: string) {
  return formatMetadataDate(dateString);
}

const gridColumns = [
  {
    dataField: 'triggeredAt',
    caption: 'Triggered At',
    template: 'triggeredAtTemplate',
    allowSorting: true,
    sortOrder: 'desc',
    minWidth: 180,
    dataType: 'date' as const,
    filterEditorOptions: { type: 'date' },
  },
  {
    dataField: 'triggeredBy',
    caption: 'Triggered By',
    minWidth: 150,
  },
  {
    dataField: 'status',
    caption: 'Status',
    template: 'statusTemplate',
    minWidth: 100,
  },
  {
    caption: 'Actions',
    template: 'actionsTemplate',
    allowSorting: false,
    minWidth: 100,
  },
];

export default defineComponent({
  name: 'WebhookHistoryTab',
  components: {
    GenericDataGrid,
    DxButton,
    AppModal,
    Bug,
    StatusChipForDataTable,
  },
  props: {
    webhookId: {
      type: String,
      required: true,
    },
  },
  setup(props: { webhookId: string }) {
    const dataGridRef = ref();
    const pageSize = ref(10);

    const { loading, executionHistory, fetchWebhookHistory } = useWebhookHistoryData(
      props.webhookId
    );
    const {
      troubleshootDialogVisible,
      selectedExecution,
      currentExecution,
      showTroubleshootDialog,
      closeTroubleshootDialog,
    } = useTroubleshootDialog();

    const { retryWebhookExecution, isExecutionRetrying, canRetryExecution } = useWebhookActions();

    const handleRetryExecution = async () => {
      if (!selectedExecution.value) return;
      const success = await retryWebhookExecution(selectedExecution.value);
      if (success) {
        closeTroubleshootDialog();
        await fetchWebhookHistory();
      }
    };

    onMounted(() => {
      fetchWebhookHistory();
    });

    return {
      loading,
      executionHistory,
      pageSize,
      troubleshootDialogVisible,
      selectedExecution,
      currentExecution,
      formatDate,
      getJiraLink,
      getJiraTicket,
      showTroubleshootDialog,
      closeTroubleshootDialog,
      formatErrorContent,
      handleRetryExecution,
      isExecutionRetrying,
      canRetryExecution,
      gridColumns,
      dataGridRef,
    };
  },
});
</script>
