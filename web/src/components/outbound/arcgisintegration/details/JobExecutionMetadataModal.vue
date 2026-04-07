<template>
  <AppModal v-model:open="isVisible" :title="modalTitleText" size="lg" fixed-height>
    <template #headerRight>
      <div class="job-id-section">
        <span class="job-id-label">Job ID:</span>
        <span class="job-id-value">{{ jobId }}</span>
        <button class="copy-job-id-btn" @click="copyJobId" title="Copy Job ID">
          <svg viewBox="0 0 24 24" class="copy-icon">
            <path
              fill="currentColor"
              d="M16 1H4c-1.1 0-2 .9-2 2v12h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
            />
          </svg>
        </button>
      </div>
    </template>

    <div class="metadata-content">
      <DxDataGrid
        :data-source="records"
        :show-borders="true"
        :row-alternation-enabled="true"
        :allow-column-resizing="true"
        :column-auto-width="true"
        :hover-state-enabled="true"
        :show-row-lines="true"
        :show-column-lines="true"
        :word-wrap-enabled="false"
        width="100%"
        height="100%"
        @exporting="onExporting"
      >
        <DxScrolling mode="virtual" />
        <DxPaging :enabled="false" />
        <DxSearchPanel
          :visible="true"
          :width="250"
          placeholder="Search records..."
          :highlight-case-sensitive="false"
        />
        <DxExport :enabled="true" :allow-export-selected-data="false" />

        <DxColumn
          caption="#"
          :width="60"
          :allow-sorting="false"
          :allow-resizing="false"
          alignment="center"
          cell-template="indexTemplate"
          css-class="index-cell"
        />

        <DxColumn
          data-field="documentId"
          caption="Document ID"
          :min-width="120"
          :allow-sorting="true"
          css-class="monospace-cell"
          cell-template="documentIdTemplate"
        />

        <DxColumn data-field="title" caption="Title" :min-width="120" :allow-sorting="true" />

        <DxColumn
          data-field="documentCreatedAt"
          caption="Document CreatedOn"
          :min-width="160"
          :allow-sorting="true"
          css-class="monospace-cell"
          cell-template="documentCreatedAtTemplate"
        />

        <DxColumn
          data-field="documentUpdatedAt"
          caption="Document UpdatedOn"
          :min-width="160"
          :allow-sorting="true"
          css-class="monospace-cell"
          cell-template="documentUpdatedAtTemplate"
        />

        <DxColumn
          data-field="locationId"
          caption="Location ID"
          :min-width="120"
          :allow-sorting="true"
          css-class="monospace-cell"
          cell-template="locationIdTemplate"
        />

        <DxColumn
          data-field="locationCreatedAt"
          caption="Location CreatedOn"
          :min-width="120"
          :allow-sorting="true"
          css-class="monospace-cell"
          cell-template="locationCreatedAtTemplate"
        />

        <DxColumn
          data-field="locationUpdatedAt"
          caption="Location UpdatedOn"
          :min-width="120"
          :allow-sorting="true"
          css-class="monospace-cell"
          cell-template="locationUpdatedAtTemplate"
        />

        <DxColumn
          v-if="recordType === 'failed'"
          data-field="errorMessage"
          caption="Error Message"
          :min-width="300"
          :allow-sorting="false"
          cell-template="errorTemplate"
        />

        <template #errorTemplate="{ data }">
          <div class="error-cell-wrapper">
            <span class="error-text" :title="data.value || 'Unknown error'">
              {{ data.value || 'Unknown error' }}
            </span>
          </div>
        </template>

        <template #indexTemplate="{ data }">
          <div class="index-cell-content">
            {{ getRowIndex(data.data) }}
          </div>
        </template>

        <template #documentIdTemplate="{ data }">
          <div class="id-cell-wrapper">
            <span class="id-text" :title="data.value">
              {{ truncateId(data.value) }}
            </span>
            <button
              class="copy-id-btn"
              :title="`Copy full ID: ${data.value}`"
              @click="copyToClipboard(data.value, 'Document ID')"
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

        <template #locationIdTemplate="{ data }">
          <div class="id-cell-wrapper">
            <span class="id-text" :title="data.value">
              {{ truncateId(data.value) }}
            </span>
            <button
              class="copy-id-btn"
              :title="`Copy full ID: ${data.value}`"
              @click="copyToClipboard(data.value, 'Location ID')"
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

        <template #documentCreatedAtTemplate="{ data }">
          <span :title="formatTimestamp(data.value)">
            {{ formatTimestamp(data.value) }}
          </span>
        </template>
        <template #documentUpdatedAtTemplate="{ data }">
          <span :title="formatTimestamp(data.value)">
            {{ formatTimestamp(data.value) }}
          </span>
        </template>
        <template #locationCreatedAtTemplate="{ data }">
          <span :title="formatTimestamp(data.value)">
            {{ formatTimestamp(data.value) }}
          </span>
        </template>
        <template #locationUpdatedAtTemplate="{ data }">
          <span :title="formatTimestamp(data.value)">
            {{ formatTimestamp(data.value) }}
          </span>
        </template>
      </DxDataGrid>
    </div>
  </AppModal>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import AppModal from '@/components/common/AppModal.vue';
import {
  DxDataGrid,
  DxColumn,
  DxPaging,
  DxSearchPanel,
  DxExport,
  DxScrolling,
} from 'devextreme-vue/data-grid';
import { exportDataGrid } from 'devextreme/excel_exporter';
import type { ExportingEvent } from 'devextreme/ui/data_grid';
import { Workbook } from 'devextreme-exceljs-fork';
import { saveAs } from 'file-saver';
import { useToastStore } from '@/store/toast';
import { formatMetadataDate } from '@/utils/dateUtils';
import type { RecordMetadata, FailedRecordMetadata } from '@/api/models/RecordMetadata';

interface Props {
  visible: boolean;
  recordType: 'added' | 'updated' | 'failed' | 'all';
  records: RecordMetadata[] | FailedRecordMetadata[];
  jobId: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:visible': [value: boolean];
}>();
const toast = useToastStore();

const isVisible = computed({
  get: () => props.visible,
  set: value => emit('update:visible', value),
});

const modalTitleText = computed(() => {
  if (props.recordType === 'all') {
    return `All Records - ${props.records.length} total`;
  }
  const typeLabel = props.recordType.charAt(0).toUpperCase() + props.recordType.slice(1);
  return `${typeLabel} Records - ${props.records.length} total`;
});

const formatTimestamp = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return 'N/A';
  if (value === 0) return '';
  return formatMetadataDate(new Date(value));
};

// Pre-computed index map for O(1) lookup instead of O(n) linear search on every render
const recordIndexMap = computed(() => {
  const map = new Map<string, number>();
  props.records.forEach((record: any, index: number) => {
    const key = `${record.documentId}|${record.locationId}`;
    if (!map.has(key)) {
      map.set(key, index + 1);
    }
  });
  return map;
});

const getRowIndex = (rowData: any) => {
  const key = `${rowData.documentId}|${rowData.locationId}`;
  return recordIndexMap.value.get(key) ?? 0;
};

const copyJobId = () => {
  navigator.clipboard
    .writeText(props.jobId)
    .then(() => {
      toast.showSuccess('Job ID copied to clipboard');
    })
    .catch(() => {
      toast.showError('Failed to copy Job ID');
    });
};

const truncateId = (id: string | null | undefined): string => {
  if (!id) return 'N/A';
  return id.substring(0, 7) + '...';
};

const copyToClipboard = (text: string, label: string) => {
  navigator.clipboard
    .writeText(text)
    .then(() => {
      toast.showSuccess(`${label} copied to clipboard`);
    })
    .catch(() => {
      toast.showError(`Failed to copy ${label}`);
    });
};

const timestampFields = new Set([
  'documentCreatedAt',
  'documentUpdatedAt',
  'locationCreatedAt',
  'locationUpdatedAt',
]);

const getExportCellValue = (gridCell: any): unknown => {
  if (!gridCell || gridCell.rowType !== 'data') {
    return undefined;
  }

  if (gridCell.column?.caption === '#') {
    return gridCell.data ? getRowIndex(gridCell.data as any) : '';
  }

  const dataField = gridCell.column?.dataField;
  if (typeof gridCell.value === 'number' && timestampFields.has(dataField)) {
    return formatTimestamp(gridCell.value);
  }

  return undefined;
};

const onExporting = async (e: ExportingEvent): Promise<void> => {
  e.cancel = true;
  try {
    const workbook = new Workbook();
    const worksheet = workbook.addWorksheet('Records');

    await exportDataGrid({
      component: e.component,
      worksheet,
      autoFilterEnabled: true,
      customizeCell: ({ gridCell, excelCell }) => {
        const exportValue = getExportCellValue(gridCell);
        if (exportValue !== undefined) {
          excelCell.value = exportValue;
        }
      },
    });

    const buffer = await workbook.xlsx.writeBuffer();
    const fileName = `job_${props.jobId}_${props.recordType}.xlsx`;
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    });
    saveAs(blob, fileName);
    toast.showSuccess(`Exported ${props.records.length} records successfully`);
  } catch (error) {
    toast.showError('Failed to generate export file');
    console.error('Export generation error:', error);
  }
};
</script>

<style scoped src="./JobExecutionMetadataModal.css"></style>
