<template>
  <div class="field-mapping-tab">
    <div v-if="loadingMappings" class="loading-state">
      <p>Loading field mappings...</p>
    </div>
    <div v-else-if="errorMappings" class="error-state">
      <p>{{ errorMappings }}</p>
    </div>
    <div v-else class="datagrid-container">
      <div class="audit-toolbar toolbar-right">
        <DxButton
          icon="refresh"
          styling-mode="text"
          type="normal"
          :hint="'Clear Filters'"
          :element-attr="{ 'aria-label': 'Clear Filters' }"
          @click="clearAllGridFilters"
        />
      </div>
      <GenericDataGrid ref="dataGridRef" :data="displayRows" :columns="gridColumns" :page-size="10">
        <template #indexTemplate="{ data }">
          <div class="index-cell">
            <span class="index-number">{{ data.index }}</span>
          </div>
        </template>
        <template #kasewareFieldTemplate="{ data }">
          <div class="simple-field-cell">
            <span class="field-name">{{ data.kasewareField }}</span>
          </div>
        </template>
        <template #arcgisFieldTemplate="{ data }">
          <div class="simple-field-cell">
            <span class="field-name">{{ data.arcgisField }}</span>
          </div>
        </template>
        <template #transformationTemplate="{ data }">
          <span class="transformation-text">{{ data.transformationType }}</span>
        </template>
        <template #mandatoryTemplate="{ data }">
          <div class="mandatory-cell">
            <StatusChipForDataTable v-if="data.isMandatory" status="REQUIRED" label="Required" />
            <StatusChipForDataTable v-else status="OPTIONAL" label="Optional" />
          </div>
        </template>
      </GenericDataGrid>
    </div>
  </div>
</template>

<script setup lang="ts">
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import { ref, computed, onMounted, type PropType } from 'vue';
import { DxButton } from 'devextreme-vue/button';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';
import type { ArcGISIntegrationResponse } from '@/api/models/ArcGISIntegrationResponse';
import type { IntegrationFieldMappingDto } from '@/api/models/IntegrationFieldMappingDto';
import type { GridColumn } from '@/components/common/GenericDataGrid.vue';
import { ArcGISIntegrationService } from '@/api/services/ArcGISIntegrationService';

const props = defineProps({
  integrationData: { type: Object as PropType<ArcGISIntegrationResponse | null>, default: null },
  integrationId: { type: String, required: true },
  loading: { type: Boolean, default: false },
});

const dataGridRef = ref();
const fieldMappings = ref<IntegrationFieldMappingDto[]>([]);
const loadingMappings = ref(false);
const errorMappings = ref<string | null>(null);

// Fetch field mappings when component mounts
async function fetchFieldMappings() {
  if (!props.integrationId) return;

  loadingMappings.value = true;
  errorMappings.value = null;

  try {
    fieldMappings.value = await ArcGISIntegrationService.getFieldMappings(props.integrationId);
  } catch (error) {
    console.error('Error fetching field mappings:', error);
    errorMappings.value = 'Failed to load field mappings';
    fieldMappings.value = [];
  } finally {
    loadingMappings.value = false;
  }
}

onMounted(() => {
  fetchFieldMappings();
});

const mappings = computed(() => fieldMappings.value);

const displayRows = computed(() => {
  // Sort by displayOrder first, then map to display format
  const sortedMappings = [...mappings.value].sort(
    (a, b) => (a.displayOrder || 0) - (b.displayOrder || 0)
  );

  return sortedMappings.map((m, i) => ({
    id: m.id || `mapping-${i}`,
    index: i + 1,
    kasewareField:
      m.sourceFieldPath || (m.sourceFieldPath ? m.sourceFieldPath.split('.').pop() : '') || '',
    arcgisField: m.targetFieldPath,
    transformationType: m.transformationType,
    isMandatory: m.isMandatory || false,
    displayOrder: m.displayOrder || 0,
  }));
});

const gridColumns: GridColumn[] = [
  {
    dataField: 'index',
    caption: '#',
    template: 'indexTemplate',
    width: 50,
    allowFiltering: false,
    allowSorting: false,
  },
  {
    dataField: 'kasewareField',
    caption: 'Kaseware Field',
    template: 'kasewareFieldTemplate',
    minWidth: 180,
    allowFiltering: true,
    allowSorting: true,
  },
  {
    dataField: 'arcgisField',
    caption: 'ArcGIS Field',
    template: 'arcgisFieldTemplate',
    minWidth: 180,
    allowFiltering: true,
    allowSorting: true,
  },
  {
    dataField: 'transformationType',
    caption: 'Transformation',
    template: 'transformationTemplate',
    width: 180,
    allowFiltering: true,
    allowSorting: true,
  },
  {
    dataField: 'isMandatory',
    caption: 'Required',
    template: 'mandatoryTemplate',
    width: 140,
    allowFiltering: true,
    allowSorting: true,
    alignment: 'left' as const,
    headerFilter: {
      dataSource: [
        { text: 'Yes', value: true },
        { text: 'No', value: false },
      ],
    },
  },
];

const clearAllGridFilters = (): void => {
  const gridComponent = dataGridRef.value;
  if (gridComponent?.instance) {
    try {
      gridComponent.instance.clearFilter();
      gridComponent.instance.clearSorting();
      if (typeof gridComponent.instance.searchByText === 'function') {
        gridComponent.instance.searchByText('');
      }
    } catch (error) {
      console.error('Error clearing field mapping grid filters:', error);
    }
  }
};
</script>

<style src="./ArcGISFieldMappingTab.css" scoped></style>
