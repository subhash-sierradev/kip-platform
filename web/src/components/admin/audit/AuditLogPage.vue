<template>
  <div class="audit-log-page">
    <!-- Data Grid -->
    <div>
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>Loading audit logs...</span>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="error-state">
        <span class="error-message">{{ error }}</span>
        <button class="retry-btn" @click="() => fetchAuditLogs()">Retry</button>
      </div>

      <!-- Shared GenericDataGrid with pagination -->
      <GenericDataGrid
        v-else-if="!loading && !error"
        ref="auditGridRef"
        :data="filteredLogs"
        :columns="gridColumns"
        :page-size="10"
        :enable-export="true"
        :enable-clear-filters="true"
        :export-config="exportConfig"
      >
        <template #nameTemplate="{ data }">
          {{ data.entityName || 'N/A' }}
        </template>
        <template #typeTemplate="{ data }">
          <StatusChipForDataTable :status="data.entityType" :label="data.entityType" />
        </template>
        <template #tenantTemplate="{ data }">
          <span>{{ data.tenantId }}</span>
        </template>
        <template #activityTemplate="{ data }">
          <StatusChipForDataTable status="UNKNOWN" :label="data.action" />
        </template>
        <template #resultTemplate="{ data }">
          <StatusChipForDataTable v-if="data.result" :status="data.result" :label="data.result" />
          <StatusChipForDataTable v-else status="UNKNOWN" label="-" />
        </template>
        <template #timestampTemplate="{ data }">
          {{
            formatMetadataDate(
              typeof data.timestamp === 'number' ? new Date(data.timestamp) : data.timestamp
            )
          }}
        </template>
      </GenericDataGrid>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';
import { useAuditLogs } from '@/composables/useAuditLogs';
import { formatMetadataDate } from '../../../utils/dateUtils';
import './AuditLogPage.css';

const auditGridRef = ref();

// Export configuration for GenericDataGrid
const exportConfig = computed(() => ({
  headers: ['Name', 'Type', 'Tenant', 'User', 'Client IP', 'Activity', 'Result', 'Timestamp'],
  pickFields: (row: any) => [
    row.entityName || 'N/A',
    row.entityType || '',
    row.tenantId || '',
    row.performedBy || '',
    row.clientIpAddress || '',
    row.action || '',
    row.result || '',
    row.timestamp
      ? formatMetadataDate(
          typeof row.timestamp === 'number' ? new Date(row.timestamp) : row.timestamp
        )
      : '',
  ],
  filenamePrefix: 'audit-logs',
}));

const gridColumns = [
  { dataField: 'entityName', caption: 'Name', template: 'nameTemplate', width: undefined },
  { dataField: 'entityType', caption: 'Type', template: 'typeTemplate', width: 200 },
  { dataField: 'tenantId', caption: 'Tenant', template: 'tenantTemplate' },
  { dataField: 'performedBy', caption: 'User' },
  { dataField: 'clientIpAddress', caption: 'Client IP', width: 160 },
  { dataField: 'action', caption: 'Activity', template: 'activityTemplate', width: 180 },
  { dataField: 'result', caption: 'Result', template: 'resultTemplate', width: 120 },
  {
    dataField: 'timestamp',
    caption: 'Timestamp',
    template: 'timestampTemplate',
    width: 220,
    dataType: 'date' as const,
    filterEditorOptions: { type: 'date' },
  },
];

onMounted(() => {
  const style = document.createElement('style');
  style.innerHTML = `
  `;
  document.head.appendChild(style);
});

const { filteredLogs, loading, error, fetchAuditLogs } = useAuditLogs();
</script>

<script lang="ts">
export default {
  name: 'AuditLogPage',
};
</script>
