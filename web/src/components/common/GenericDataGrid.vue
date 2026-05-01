<template>
  <!-- Generic Toolbar -->
  <div v-if="showToolbar" class="generic-grid-toolbar">
    <DxButton
      v-if="enableExport"
      icon="download"
      styling-mode="text"
      type="default"
      :hint="'Export CSV'"
      :element-attr="{ 'aria-label': 'Export CSV' }"
      @click="handleExport"
    />
    <slot name="toolbarActions" />
    <DxButton
      v-if="enableClearFilters"
      icon="refresh"
      styling-mode="text"
      type="normal"
      :hint="'Clear Filters'"
      :element-attr="{ 'aria-label': 'Clear Filters' }"
      @click="handleClearFilters"
    />
  </div>

  <DxDataGrid
    ref="gridRef"
    :data-source="data"
    :show-borders="true"
    :row-alternation-enabled="true"
    :allow-column-reordering="true"
    :allow-column-resizing="true"
    :column-auto-width="true"
    :hover-state-enabled="true"
    :show-row-lines="true"
    :show-column-lines="true"
    :word-wrap-enabled="true"
    width="100%"
    :key-expr="rowKey"
    :repaint-changes-only="true"
    :onOptionChanged="emitOptionChanged"
  >
    <DxPaging
      :display-mode="'adaptive'"
      :previous-next-button-visible="true"
      :navigation-buttons-visible="true"
      :page-size="resolvedPageSize"
      :page-index="resolvedPageIndex"
      :enabled="true"
    />
    <DxPager
      :show-navigation-buttons="true"
      :display-mode="'adaptive'"
      :show-page-size-selector="true"
      :allowed-page-sizes="props.allowedPageSizes"
      :show-info="true"
      :visible="true"
      :info-text="pagerInfoText"
    />
    <DxHeaderFilter :visible="true" />

    <!-- Dynamic Columns -->
    <template v-for="col in columns" :key="col.dataField || col.caption">
      <DxColumn
        :data-field="col.dataField"
        :caption="col.caption"
        :data-type="col.dataType"
        :width="col.width"
        :min-width="col.minWidth"
        :allow-sorting="col.allowSorting ?? true"
        :allow-filtering="col.allowFiltering ?? true"
        :visible="col.visible ?? true"
        :alignment="col.alignment"
        :cell-template="col.template ? getTemplate(col.template) : undefined"
      >
        <DxHeaderFilter
          v-if="col.headerFilter?.dataSource"
          :data-source="col.headerFilter.dataSource"
        />
      </DxColumn>
    </template>
  </DxDataGrid>
</template>

<script lang="ts" setup>
import { ref, computed, h, useSlots, render } from 'vue';
import { DxDataGrid, DxColumn, DxPaging, DxPager, DxHeaderFilter } from 'devextreme-vue/data-grid';
import { DxButton } from 'devextreme-vue/button';
import query from 'devextreme/data/query';
import type DataGrid from 'devextreme/ui/data_grid';
import { exportGridData } from './ExportGridData';

const slots = useSlots();

// ---------------------
// Types and Interfaces
// ---------------------
export type GridColumn = {
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
  headerFilter?: {
    dataSource?: { text: string; value: string | number | boolean | Date | null }[];
  };
};

type ExportConfig = {
  headers: string[];
  pickFields: (row: any) => (string | number | Date | undefined)[];
  filenamePrefix?: string;
};

const props = withDefaults(
  defineProps<{
    data: any[];
    columns: GridColumn[];
    pageSize?: number;
    pageIndex?: number;
    allowedPageSizes?: number[];
    rowKey?: string;
    enableExport?: boolean;
    enableClearFilters?: boolean;
    exportConfig?: ExportConfig;
    pagerInfoText?: string;
  }>(),
  {
    data: () => [],
    columns: () => [],
    pageSize: 10,
    pageIndex: 0,
    allowedPageSizes: () => [10, 20, 50, 100],
    rowKey: 'id',
    enableExport: false,
    enableClearFilters: false,
    pagerInfoText: '{0} - {1} of {2} items',
  }
);

const resolvedPageSize = computed(() => props.pageSize);
const resolvedPageIndex = computed(() => props.pageIndex);

const showToolbar = computed(
  () => props.enableExport || props.enableClearFilters || !!slots.toolbarActions
);

const gridRef = ref<InstanceType<typeof DxDataGrid> | null>(null);

// Emit option changes to parent so pagination state can be tracked
const emit = defineEmits<{
  (e: 'option-changed', payload: any): void;
}>();

const emitOptionChanged = (e: any) => {
  emit('option-changed', e);
};

// Generic action handlers
const handleClearFilters = () => {
  const gridComponent = gridRef.value;
  if (gridComponent && gridComponent.instance) {
    gridComponent.instance.clearFilter();
    gridComponent.instance.clearSorting();
  }
};

const handleExport = async () => {
  try {
    const gridInstance = gridRef.value?.instance;
    let exportData = props.data;

    if (gridInstance && typeof gridInstance.getCombinedFilter === 'function') {
      const combinedFilter = gridInstance.getCombinedFilter();
      if (Array.isArray(combinedFilter) && combinedFilter.length > 0) {
        exportData = query(props.data).filter(combinedFilter).toArray();
      }
    }

    await exportGridData({
      gridRef: undefined,
      fallbackData: exportData,
      headers: props.exportConfig!.headers,
      pickFields: props.exportConfig!.pickFields,
      filenamePrefix: props.exportConfig!.filenamePrefix || 'grid-export',
    });
  } catch (error) {
    console.error('CSV export failed:', error);
  }
};

// Expose the underlying DevExtreme instance to parent components
defineExpose({
  get instance() {
    return gridRef.value?.instance as DataGrid | undefined;
  },
});

// -------------------------------------------------------------
// ⭐ Converts Vue slot into DevExtreme cell template
// -------------------------------------------------------------
const getTemplate = (slotName: string) => {
  // return (cellData: any) => {
  return (container: any, cellData: any) => {
    const safePayload = {
      data: cellData && cellData.data ? cellData.data : (cellData ?? {}),
    };
    const vnode = h('div', {}, slots[slotName]?.(safePayload) ?? '');
    render(vnode, container);
    return container;
  };
};
</script>

<style scoped>
.generic-grid-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 0.25rem;
}

.generic-grid-toolbar .dx-button {
  min-width: 30px;
  height: 32px;
}

/* GLOBAL DEVEXTREME ROW HEIGHT REDUCTION - 20% smaller rows */
:deep(.dx-datagrid-rowsview .dx-row),
:deep(.dx-datagrid-content .dx-datagrid-table .dx-row),
:deep(.dx-datagrid-rowsview tbody > tr),
:deep(.dx-row) {
  min-height: 24px !important;
  line-height: 24px !important;
}

:deep(.dx-datagrid-rowsview .dx-row > td) {
  min-height: 24px !important;
  padding: 1px 6px !important;
  font-size: 0.75rem !important;
  line-height: 22px !important;
  vertical-align: middle !important;
}

/* Grid rows styling */
:deep(.dx-row) {
  background: #fff;
  border-bottom: 1px solid #f1f5f9;
}

:deep(.dx-row:nth-child(even)) {
  background: #f8fafc;
}

:deep(.dx-row:hover) {
  background: #e2e8f0;
  cursor: pointer;
}

/* Hover state for all badges */
:deep(.type-badge:hover),
:deep(.activity-badge:hover),
:deep(.status-badge:hover),
:deep(span[class*='badge']:hover) {
  background-color: #e2e8f0 !important;
  color: #334155 !important;
}
</style>
