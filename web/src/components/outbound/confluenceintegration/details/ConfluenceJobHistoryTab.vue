<template>
  <div class="job-history-tab">
    <Tooltip
      :visible="jobIdTipVisible"
      :x="jobIdTipX"
      :y="jobIdTipY"
      :id="jobIdTipId"
      :text="jobIdTipText"
    />

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner">
        <i class="dx-icon dx-icon-refresh spinning"></i>
      </div>
      <p>Loading job history...</p>
    </div>

    <template v-else>
      <ErrorPanel v-if="error" :message="error" />

      <div v-else class="datagrid-container">
        <GenericDataGrid
          :data="rows"
          :columns="gridColumns"
          :page-size="10"
          :enable-clear-filters="true"
        >
          <template #jobIdTemplate="{ data: row }">
            <div class="job-id-cell">
              <span
                class="job-id-text"
                :aria-describedby="jobIdTipId"
                @mouseenter="e => onJobIdMouseEnter(e, row.id)"
                @mouseleave="onJobIdMouseLeave"
                @mousemove="onJobIdMouseMove"
                >{{ truncateJobId(row.id) }}</span
              >
              <button
                class="copy-icon-btn"
                @click.stop="copyJobIdToClipboard(row.id)"
                aria-label="Copy Job ID"
                title="Copy Job ID"
              >
                <svg viewBox="0 0 24 24" class="copy-icon">
                  <path
                    fill="currentColor"
                    d="M16 1H4c-1.1 0-2 .9-2 2v12h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
                  />
                </svg>
              </button>
            </div>
          </template>
          <template #keyTimeTemplate="{ data: row }">
            <span class="key-time">{{ formatMetadataDate(row.startedAt) }}</span>
          </template>
          <template #fetchWindowTemplate="{ data: row }">
            <span class="fetch-window">{{
              formatFetchWindow(row.windowStart, row.windowEnd)
            }}</span>
          </template>
          <template #statusTemplate="{ data: row }">
            <div class="status-cell">
              <StatusChipForDataTable :status="row.status" :label="row.status" />
              <span
                v-if="Number(row.retryAttempt || 0) > 0"
                class="retry-badge"
                :title="`Retry attempt #${row.retryAttempt}`"
              >
                Retry #{{ row.retryAttempt }}
              </span>
            </div>
          </template>
          <template #actionTemplate="{ data: row }">
            <!-- SUCCESS: Show Confluence Page link -->
            <span v-if="row.status === 'SUCCESS' && getConfluencePageUrl(row)">
              <a
                :href="getConfluencePageUrl(row)"
                target="_blank"
                rel="noopener noreferrer"
                class="confluence-page-link"
                @click.stop
                >View Report</a
              >
            </span>
            <!-- FAILED or ABORTED: Show Retry button -->
            <button
              v-else-if="canRetryExecution(row)"
              type="button"
              class="dx-widget dx-button dx-button-mode-outlined dx-button-normal dx-button-has-text retry-btn"
              :disabled="isRetrying(row.id)"
              @click.stop="retryExecution(row)"
            >
              <div class="dx-button-content">
                <RotateCw class="retry-icon" :size="16" />
                <span class="dx-button-text">{{
                  isRetrying(row.id) ? 'Retrying...' : 'Retry'
                }}</span>
              </div>
            </button>
          </template>
          <template #totalProcessedTemplate="{ data: row }">
            <div class="count-cell">
              <span class="count-badge" :class="getProcessedBadgeClass(row)">{{
                getTotalProcessed(row)
              }}</span>
            </div>
          </template>
        </GenericDataGrid>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ConfluenceJobHistoryTab' });
import { onMounted, computed, ref } from 'vue';
import { useRoute } from 'vue-router';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import ErrorPanel from '@/components/common/ErrorPanel.vue';
import Tooltip from '@/components/common/Tooltip.vue';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';
import { ConfluenceIntegrationService } from '@/api/services/ConfluenceIntegrationService';
import type { IntegrationJobExecutionDto } from '@/api/models/IntegrationJobExecutionDto';
import { formatMetadataDate } from '@/utils/dateUtils';
import { useToastStore } from '@/store/toast';
import { RotateCw } from 'lucide-vue-next';

const route = useRoute();
const toast = useToastStore();
const integrationId = computed(() => route.params.id as string);

const history = ref<IntegrationJobExecutionDto[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);
const retryingExecutionIds = ref<string[]>([]);

async function fetchJobHistory(id: string): Promise<void> {
  loading.value = true;
  error.value = null;
  try {
    history.value = await ConfluenceIntegrationService.getJobHistory(id);
  } catch (e: unknown) {
    const message = (e as { message?: string })?.message;
    error.value = message || 'Failed to load job history.';
    history.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  if (integrationId.value) fetchJobHistory(integrationId.value);
});

const rows = history;

function getConfluencePageUrl(row: IntegrationJobExecutionDto): string | undefined {
  return row.executionMetadata?.['confluencePageUrl'] ?? undefined;
}

// Job ID tooltip
const JOB_ID_TRUNCATE_LENGTH = 16;
function truncateJobId(id: string): string {
  if (!id) return '';
  return id.length > JOB_ID_TRUNCATE_LENGTH ? `${id.substring(0, JOB_ID_TRUNCATE_LENGTH)}...` : id;
}

const jobIdTipVisible = ref(false);
const jobIdTipX = ref(0);
const jobIdTipY = ref(0);
const jobIdTipId = 'confluence-job-id-tooltip';
const jobIdTipText = ref('');

function onJobIdMouseEnter(e: MouseEvent, fullId: string): void {
  jobIdTipText.value = fullId;
  jobIdTipVisible.value = true;
  jobIdTipX.value = e.clientX + 12;
  jobIdTipY.value = e.clientY + 12;
}

function onJobIdMouseLeave(): void {
  jobIdTipVisible.value = false;
}

function onJobIdMouseMove(e: MouseEvent): void {
  jobIdTipX.value = e.clientX + 12;
  jobIdTipY.value = e.clientY + 12;
}

async function copyJobIdToClipboard(id: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(id);
    toast.showSuccess('Job ID copied successfully');
  } catch {
    toast.showError('Failed to copy Job ID to clipboard');
  }
}

function formatFetchWindow(windowStart?: string | null, windowEnd?: string | null): string {
  if (!windowStart) return '-';
  if (!windowEnd) return formatMetadataDate(windowStart);

  // Show range for fetch window
  const start = formatMetadataDate(windowStart);
  const end = formatMetadataDate(windowEnd);
  return `${start} → ${end}`;
}

function canRetryExecution(row: IntegrationJobExecutionDto): boolean {
  return (row.status === 'FAILED' || row.status === 'ABORTED') && !!row.id && !!row.originalJobId;
}

function isRetrying(executionId: string): boolean {
  return retryingExecutionIds.value.includes(executionId);
}

async function retryExecution(row: IntegrationJobExecutionDto): Promise<void> {
  if (
    !integrationId.value ||
    !row.id ||
    !row.originalJobId ||
    !canRetryExecution(row) ||
    isRetrying(row.id)
  ) {
    return;
  }

  retryingExecutionIds.value = [...retryingExecutionIds.value, row.id];
  try {
    await ConfluenceIntegrationService.retryJobExecution(integrationId.value, row.originalJobId);
    toast.showSuccess('Retry execution queued successfully');
    await fetchJobHistory(integrationId.value);
  } catch (e: unknown) {
    const message = (e as { message?: string })?.message;
    toast.showError(message || 'Failed to retry execution');
  } finally {
    retryingExecutionIds.value = retryingExecutionIds.value.filter(id => id !== row.id);
  }
}

function getTotalProcessed(row: IntegrationJobExecutionDto): number {
  return row.totalRecords || 0;
}

function getProcessedBadgeClass(row: IntegrationJobExecutionDto): string {
  const total = row.totalRecords || 0;
  if (total > 0) return 'success';
  return '';
}

type GridColumn = {
  dataField?: string;
  caption?: string;
  dataType?: 'string' | 'number' | 'date' | 'boolean';
  width?: number | string;
  minWidth?: number;
  allowSorting?: boolean;
  allowFiltering?: boolean;
  template?: string;
  alignment?: 'left' | 'center' | 'right';
};

const gridColumns: GridColumn[] = [
  { dataField: 'id', caption: 'Job ID', template: 'jobIdTemplate', minWidth: 100 },
  { dataField: 'status', caption: 'Status', template: 'statusTemplate', minWidth: 100 },
  {
    caption: 'Reports',
    template: 'totalProcessedTemplate',
    dataType: 'number',
    minWidth: 90,
    alignment: 'right',
    allowSorting: false,
  },
  {
    dataField: 'startedAt',
    caption: 'Started At',
    template: 'keyTimeTemplate',
    minWidth: 180,
    allowFiltering: false,
  },
  { dataField: 'triggeredByUser', caption: 'Triggered By', minWidth: 120 },
  {
    dataField: 'windowStart',
    caption: 'Fetch Window',
    template: 'fetchWindowTemplate',
    minWidth: 240,
    allowFiltering: false,
  },
  {
    caption: 'Action',
    template: 'actionTemplate',
    minWidth: 130,
    allowFiltering: false,
    allowSorting: false,
  },
];
</script>

<style scoped>
.loading-state {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #475569;
}
.loading-spinner {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.spinning {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.job-id-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}
.job-id-text {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.8125rem;
  color: #334155;
  font-weight: 500;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.copy-icon-btn {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}
.copy-icon-btn:hover {
  background: #f1f5f9;
  color: #475569;
}
.copy-icon {
  width: 14px;
  height: 14px;
}

.count-cell {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}
.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  background-color: #f5f5f5;
  color: #666;
}
.count-badge.success {
  background-color: #e8f5e9;
  color: #2e7d32;
}
.count-badge.info {
  background-color: #e8f5e9;
  color: #2e7d32;
}
.count-badge.error {
  background-color: #ffebee;
  color: #c62828;
}

.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.retry-badge {
  display: inline-block;
  padding: 0.125rem 0.375rem;
  border: 1px solid #e2e8f0;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
  /* Colors removed - using GenericDataGrid universal styling */
}

.retry-btn {
  font-size: 0.8125rem !important;
  padding: 2px 8px !important;
  min-height: 28px !important;
  height: 28px !important;
  border-radius: 0.75rem !important;
  background: #fff7ed !important;
  border: 1px solid #fb923c !important;
  box-shadow: 0 2px 6px rgba(251, 146, 60, 0.15) !important;
}

.retry-btn .dx-button-content {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: #ea580c;
}

.retry-btn .retry-icon {
  flex-shrink: 0;
  color: #ea580c !important;
}

.retry-btn:hover {
  background: #ffedd5 !important;
  border-color: #f97316 !important;
}

.retry-btn:active {
  background: #fed7aa !important;
  border-color: #ea580c !important;
}

.retry-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  background: #f5f5f5 !important;
  border-color: #d1d5db !important;
}

.retry-btn:disabled .dx-button-content,
.retry-btn:disabled .retry-icon {
  color: #9ca3af !important;
}

.confluence-page-link {
  color: #000000;
  text-decoration: underline;
  font-weight: 500;
  font-size: 0.875rem;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  transition: all 0.2s ease;
}
.confluence-page-link:hover {
  background: #f3f4f6;
  color: #374151;
  text-decoration: underline;
}
.text-muted {
  color: #94a3b8;
}
</style>
