<template>
  <div class="job-history-tab">
    <!-- Tooltip for Job ID -->
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
      <!-- Show error panel if data fetch failed -->
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
            <span class="key-time">{{ formatKeyTime(row.startedAt, row.completedAt) }}</span>
          </template>
          <template #statusTemplate="{ data: row }">
            <StatusChipForDataTable :status="row.status" :label="row.status" />
          </template>
          <template #windowStartTemplate="{ data: row }">
            <span class="window-time" :title="'Data fetch time window'">
              {{ formatKeyTime(row.windowStart, row.windowEnd) }}
            </span>
          </template>
          <template #addedTemplate="{ data: row }">
            <div class="count-cell">
              <span
                v-if="row.addedRecords > 0"
                class="count-badge clickable success"
                @click="showMetadataDetails('added', row)"
              >
                {{ row.addedRecords }}
              </span>
              <span v-else class="count-badge">0</span>
            </div>
          </template>
          <template #updatedTemplate="{ data: row }">
            <div class="count-cell">
              <span
                v-if="row.updatedRecords > 0"
                class="count-badge clickable info"
                @click="showMetadataDetails('updated', row)"
              >
                {{ row.updatedRecords }}
              </span>
              <span v-else class="count-badge">0</span>
            </div>
          </template>
          <template #failedTemplate="{ data: row }">
            <div class="count-cell">
              <span
                v-if="row.failedRecords > 0"
                class="count-badge clickable error"
                @click="showMetadataDetails('failed', row)"
                :title="row.errorMessage || ''"
              >
                {{ row.failedRecords }}
              </span>
              <span v-else class="count-badge">0</span>
            </div>
          </template>
          <template #totalTemplate="{ data: row }">
            <div class="count-cell">
              <span
                v-if="row.totalRecords > 0"
                class="count-badge clickable total"
                @click="showMetadataDetails('all', row)"
              >
                {{ row.totalRecords }}
              </span>
              <span v-else class="count-badge">0</span>
            </div>
          </template>
        </GenericDataGrid>
      </div>
    </template>

    <JobExecutionMetadataModal
      v-model:visible="metadataModal.visible"
      :record-type="metadataModal.type"
      :records="metadataModal.records"
      :job-id="metadataModal.jobId"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'JobHistoryTab' });
import { onMounted, computed, ref, reactive } from 'vue';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import ErrorPanel from '@/components/common/ErrorPanel.vue';
import Tooltip from '@/components/common/Tooltip.vue';
import JobExecutionMetadataModal from './JobExecutionMetadataModal.vue';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';
import { useArcGISJobHistory } from '@/composables/useArcGISJobHistory';
import { useRoute } from 'vue-router';
import { formatKeyTime } from '@/utils/dateUtils';
import { useToastStore } from '@/store/toast';
import type { RecordMetadata, FailedRecordMetadata } from '@/api/models/RecordMetadata';

// Job execution metadata interface for type safety
interface JobExecutionMetadata {
  id?: string;
  addedRecords?: number;
  updatedRecords?: number;
  failedRecords?: number;
  totalRecords?: number;
  addedRecordsMetadata?: RecordMetadata[];
  updatedRecordsMetadata?: RecordMetadata[];
  failedRecordsMetadata?: FailedRecordMetadata[];
  totalRecordsMetadata?: RecordMetadata[];
}

// Mirror GenericDataGrid's GridColumn typing locally for prop compatibility
type GridColumn = {
  dataField?: string;
  caption?: string;
  dataType?: 'string' | 'number' | 'date' | 'boolean';
  width?: number | string;
  minWidth?: number;
  allowSorting?: boolean;
  allowFiltering?: boolean;
  visible?: boolean;
  template?: string; // slot name
  alignment?: 'left' | 'center' | 'right';
};

const route = useRoute();
const integrationId = computed(() => route.params.id as string);
const toast = useToastStore();

const { history, loading, error, fetchJobHistory } = useArcGISJobHistory();

onMounted(() => {
  if (integrationId.value) fetchJobHistory(integrationId.value);
});

const rows = history;

// Job ID display configuration
const JOB_ID_TRUNCATE_LENGTH = 16;

function truncateJobId(id: string): string {
  if (!id) return '';
  return id.length > JOB_ID_TRUNCATE_LENGTH ? `${id.substring(0, JOB_ID_TRUNCATE_LENGTH)}...` : id;
}

// Job ID tooltip state
const jobIdTipVisible = ref(false);
const jobIdTipX = ref(0);
const jobIdTipY = ref(0);
const jobIdTipId = 'job-id-tooltip';
const jobIdTipText = ref('');

const metadataModal = reactive<{
  visible: boolean;
  type: 'added' | 'updated' | 'failed' | 'all';
  records: RecordMetadata[] | FailedRecordMetadata[];
  jobId: string;
}>({
  visible: false,
  type: 'added',
  records: [],
  jobId: '',
});

function onJobIdMouseEnter(e: MouseEvent, fullId: string) {
  jobIdTipText.value = fullId;
  jobIdTipVisible.value = true;
  jobIdTipX.value = e.clientX + 12;
  jobIdTipY.value = e.clientY + 12;
}

function onJobIdMouseLeave() {
  jobIdTipVisible.value = false;
}

function onJobIdMouseMove(e: MouseEvent) {
  jobIdTipX.value = e.clientX + 12;
  jobIdTipY.value = e.clientY + 12;
}

async function copyJobIdToClipboard(id: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(id);
    toast.showSuccess('Job ID copied successfully');
  } catch (err) {
    console.error('Failed to copy Job ID:', err);
    toast.showError('Failed to copy Job ID to clipboard');
  }
}

function showMetadataDetails(
  type: 'added' | 'updated' | 'failed' | 'all',
  jobExecution: JobExecutionMetadata
) {
  metadataModal.type = type;
  metadataModal.jobId = jobExecution.id || '';

  switch (type) {
    case 'added':
      metadataModal.records = jobExecution.addedRecordsMetadata || [];
      break;
    case 'updated':
      metadataModal.records = jobExecution.updatedRecordsMetadata || [];
      break;
    case 'failed':
      metadataModal.records = jobExecution.failedRecordsMetadata || [];
      break;
    case 'all':
      metadataModal.records = jobExecution.totalRecordsMetadata || [];
      break;
  }

  metadataModal.visible = true;
}

const gridColumns: GridColumn[] = [
  { dataField: 'id', caption: 'Job ID', template: 'jobIdTemplate', minWidth: 100 },
  {
    dataField: 'startedAt',
    caption: 'Time Window',
    template: 'keyTimeTemplate',
    minWidth: 240,
    allowFiltering: false,
  },
  {
    dataField: 'windowStart',
    caption: 'Fetch Window',
    template: 'windowStartTemplate',
    minWidth: 240,
    allowFiltering: false,
  },
  { dataField: 'triggeredByUser', caption: 'Triggered By', minWidth: 120 },
  { dataField: 'status', caption: 'Job Status', template: 'statusTemplate', minWidth: 100 },
  {
    dataField: 'totalRecords',
    caption: 'Total',
    template: 'totalTemplate',
    dataType: 'number',
    minWidth: 80,
    alignment: 'right',
  },
  {
    dataField: 'addedRecords',
    caption: 'Added',
    template: 'addedTemplate',
    dataType: 'number',
    minWidth: 80,
    alignment: 'right',
  },
  {
    dataField: 'updatedRecords',
    caption: 'Updated',
    template: 'updatedTemplate',
    dataType: 'number',
    minWidth: 80,
    alignment: 'right',
  },
  {
    dataField: 'failedRecords',
    caption: 'Failed',
    template: 'failedTemplate',
    minWidth: 80,
    alignment: 'right',
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

.empty-state {
  text-align: center;
  padding: 2rem 1rem;
  color: #475569;
}
.empty-icon {
  font-size: 28px;
  color: #94a3b8;
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
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.copy-icon-btn:hover {
  background: #f3f4f6;
  color: #374151;
}

.copy-icon {
  width: 18px;
  height: 18px;
}

.window-time {
  color: #475569;
  font-size: 0.8125rem;
}

.key-time {
  color: #475569;
  font-size: 0.8125rem;
  white-space: nowrap;
}

.added-count,
.updated-count,
.failed-count {
  font-weight: 500;
  color: #64748b;
}

.added-count.has-records {
  color: #166534;
  font-weight: 600;
}

.updated-count.has-records {
  color: #1e3a8a;
  font-weight: 600;
}

.failed-count.has-failures {
  color: #991b1b;
  font-weight: 700;
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
  font-weight: 600;
  font-size: 13px;
  background-color: #f5f5f5;
  color: #666;
}

.count-badge.clickable {
  cursor: pointer;
  transition: all 0.2s ease;
}

.count-badge.clickable:hover {
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.count-badge.success {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.count-badge.success:hover {
  background-color: #c8e6c9;
}

.count-badge.info {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.count-badge.info:hover {
  background-color: #c8e6c9;
}

.count-badge.error {
  background-color: #ffebee;
  color: #c62828;
}

.count-badge.error:hover {
  background-color: #ffcdd2;
}

.count-badge.total {
  background-color: #f3f4f6;
  color: #4b5563;
}

.count-badge.total:hover {
  background-color: #e5e7eb;
}
</style>
