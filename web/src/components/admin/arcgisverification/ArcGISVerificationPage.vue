<template>
  <!-- TODO KIP-547 REMOVE — Temporary ArcGIS feature service verification page -->
  <div class="arcgis-verification-page">
    <!-- Search controls -->
    <div class="verification-filters">
      <div class="filter-field">
        <label class="filter-label">Object ID</label>
        <DxTextBox
          v-model:value="objectIdInput"
          placeholder="Enter numeric OBJECTID"
          :show-clear-button="true"
          styling-mode="outlined"
          @enter-key="handleSearch"
        />
      </div>
      <div class="filter-field">
        <label class="filter-label">Location ID</label>
        <DxTextBox
          v-model:value="locationIdInput"
          placeholder="Enter external_location_id"
          :show-clear-button="true"
          styling-mode="outlined"
          @enter-key="handleSearch"
        />
      </div>
      <div class="filter-actions">
        <DxButton
          text="Search"
          type="default"
          styling-mode="contained"
          icon="search"
          class="search-btn"
          :disabled="loading"
          @click="handleSearch"
        />
        <DxButton
          text="Clear"
          type="normal"
          styling-mode="outlined"
          :disabled="loading"
          @click="handleClear"
        />
      </div>
      <!-- Inline summary — lives in the filter row to save vertical space -->
      <div v-if="!loading && records.length > 0" class="filter-summary">
        <span
          ><strong>{{ records.length }}</strong> record(s)</span
        >
        <span v-if="objectIdRange">
          · OBJECTID <strong>{{ objectIdRange.max }}</strong> →
          <strong>{{ objectIdRange.min }}</strong>
        </span>
        <span v-if="hasMore" class="filter-summary-more">· more available</span>
      </div>
    </div>

    <!-- Loading indicator -->
    <div v-if="loading" class="verification-loading">
      <span>Loading records…</span>
    </div>

    <!-- Error alert -->
    <div v-if="error" class="verification-error">
      {{ error }}
    </div>

    <!-- Data grid -->
    <div v-if="records.length > 0" class="verification-grid">
      <GenericDataGrid
        :data="records"
        :columns="derivedColumns"
        :page-size="100"
        :allowed-page-sizes="[100, 500, 1000]"
        row-key="OBJECTID"
        :enable-clear-filters="true"
        :enable-export="true"
        :export-config="exportConfig"
      />
    </div>

    <!-- Empty state -->
    <div
      v-if="!loading && !error && records.length === 0 && hasSearched"
      class="verification-empty"
    >
      No records found.
    </div>

    <!-- Load next page -->
    <div v-if="hasMore && !loading" class="verification-pagination">
      <DxButton
        text="Load Older Records (1,000)"
        type="normal"
        styling-mode="outlined"
        icon="chevronright"
        @click="handleLoadMore"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { DxButton } from 'devextreme-vue/button';
import { DxTextBox } from 'devextreme-vue/text-box';

import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import { useArcGISVerification } from '@/composables/useArcGISVerification';
import type { GridColumn } from '@/components/common/GenericDataGrid.vue';

defineOptions({ name: 'ArcGISVerificationPage' });

const { records, loading, error, hasMore, currentOffset, fetchRecords, reset } =
  useArcGISVerification();

const objectIdInput = ref('');
const locationIdInput = ref('');
const hasSearched = ref(false);

/** Pinned columns shown first in this exact order; remaining keys sorted A→Z. */
const PINNED_COLUMNS = ['OBJECTID', 'external_document_id', 'external_location_id'];

/** OBJECTID range across all currently loaded records (highest → lowest, descending order). */
const objectIdRange = computed<{ max: number; min: number } | null>(() => {
  if (records.value.length === 0) return null;
  const ids = records.value
    .map(r => r['OBJECTID'])
    .filter((v): v is number => typeof v === 'number');
  if (ids.length === 0) return null;
  return { max: Math.max(...ids), min: Math.min(...ids) };
});

const derivedColumns = computed<GridColumn[]>(() => {
  if (records.value.length === 0) return [];

  // Union of all keys across every record so columns never disappear when a field is blank
  const allKeys = [...new Set(records.value.flatMap(r => Object.keys(r)))];
  const remaining = allKeys
    .filter(k => !PINNED_COLUMNS.includes(k))
    .sort((a, b) => a.localeCompare(b));
  const presentPinned = PINNED_COLUMNS.filter(k => allKeys.includes(k));
  const ordered = [...presentPinned, ...remaining];

  return ordered.map(key => ({
    dataField: key,
    caption: key,
    dataType: 'string',
    allowSorting: true,
    allowFiltering: true,
  }));
});

const exportConfig = computed(() => ({
  headers: derivedColumns.value.map(c => c.caption ?? c.dataField ?? ''),
  pickFields: (row: Record<string, unknown>) =>
    derivedColumns.value.map(c => {
      const val = c.dataField ? row[c.dataField] : undefined;
      return val !== undefined && val !== null ? String(val) : '';
    }),
  filenamePrefix: 'arcgis-verification',
}));

async function handleSearch(): Promise<void> {
  hasSearched.value = true;
  await fetchRecords(0, objectIdInput.value, locationIdInput.value);
}

async function handleLoadMore(): Promise<void> {
  await fetchRecords(currentOffset.value + 1000, objectIdInput.value, locationIdInput.value);
}

function handleClear(): void {
  objectIdInput.value = '';
  locationIdInput.value = '';
  hasSearched.value = false;
  reset();
}
</script>

<style scoped>
.arcgis-verification-page {
  padding: 1rem 1.25rem;
  background: var(--kw-content-bg, #fafbfc);
  min-height: calc(100vh - 180px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.verification-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.verification-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #111827;
  margin: 0;
  letter-spacing: 0.01em;
}

.verification-subtitle {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
  line-height: 1.5;
}

.verification-filters {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.filter-field {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #374151;
  letter-spacing: 0.01em;
  white-space: nowrap;
  width: 80px;
  flex-shrink: 0;
}

.filter-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-bottom: 1px;
}

.filter-summary {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8125rem;
  color: #374151;
  white-space: nowrap;
}

.filter-summary-more {
  color: #6b7280;
}

.verification-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  color: #6b7280;
  font-size: 0.875rem;
}

.verification-error {
  padding: 12px 16px;
  background: #fff5f5;
  border: 1px solid #fecaca;
  border-left: 4px solid #dc2626;
  border-radius: 10px;
  color: #b91c1c;
  font-size: 0.875rem;
  font-weight: 500;
}

.verification-summary {
  font-size: 0.8125rem;
  font-weight: 500;
  color: #6b7280;
  padding: 6px 2px;
}

.verification-empty {
  padding: 48px 24px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  text-align: center;
  font-size: 0.875rem;
  color: #9ca3af;
}

.verification-grid {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.verification-pagination {
  display: flex;
  justify-content: center;
  padding: 8px 0 4px;
}

.search-btn.dx-button-mode-contained.dx-button-default {
  background: #f59e0b;
  border-color: #f59e0b;
}

.search-btn.dx-button-mode-contained.dx-button-default .dx-button-text {
  color: #ffffff;
}

.search-btn.dx-button-mode-contained.dx-button-default:hover:not(.dx-state-disabled),
.search-btn.dx-button-mode-contained.dx-button-default:focus:not(.dx-state-disabled) {
  background: #ee9024;
  border-color: #ee9024;
}
</style>
