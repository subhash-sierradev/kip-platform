<template>
  <div class="clear-cache-page">
    <DxLoadPanel
      :visible="isStatsLoading"
      :show-indicator="true"
      :show-pane="true"
      :shading="true"
      message="Loading cache items..."
    />
    <ConfirmationDialog
      :open="dialogState.open"
      :type="dialogState.type === 'delete' ? 'delete' : 'custom'"
      :title="dialogTitle"
      :description="dialogDescription"
      :confirm-label="dialogConfirmLabel"
      :confirm-color="dialogConfirmColor"
      :loading="dialogLoading"
      :max-width="400"
      @cancel="closeDialog"
      @confirm="handleDialogConfirm"
    />
    <teleport to="body">
      <CommonTooltip v-bind="tooltip" />
    </teleport>

    <div v-if="isStatsError" class="state-card error-state">
      <div>
        <h3>Unable to load cache items</h3>
        <p>Try refreshing the page data and attempt the cache action again.</p>
      </div>
      <DxButton text="Retry" type="normal" styling-mode="outlined" @click="retryFetchStats" />
    </div>
    <div v-else-if="!cacheItems.length && !isStatsLoading" class="state-card empty-state">
      <i class="dx-icon dx-icon-box empty-icon" aria-hidden="true"></i>
      <h3>No cache items found</h3>
      <p>Cache regions will appear here once the application reports cache statistics.</p>
    </div>
    <div v-else class="data-grid-container">
      <div class="cache-table-toolbar">
        <div class="toolbar-left">
          <div class="search-field">
            <label class="search-label" for="cache-search">Search Cache</label>
            <DxTextBox
              id="cache-search"
              v-model:value="searchText"
              class="cache-search-box"
              mode="search"
              placeholder="Search by cache name or key"
              :show-clear-button="true"
              value-change-event="input"
            />
          </div>
          <span
            class="clear-filters-trigger"
            @mouseenter="showTooltip($event, 'Reset Filters')"
            @mousemove="moveTooltip"
            @mouseleave="hideTooltip"
          >
            <DxButton
              icon="refresh"
              class="clear-filters-btn"
              type="normal"
              styling-mode="text"
              @click="handleClearFilters"
            />
          </span>
        </div>

        <div class="toolbar-right">
          <DxButton
            class="clear-all-btn"
            text="Clear All"
            type="default"
            styling-mode="contained"
            :disabled="isClearingAll || !cacheItems.length"
            :element-attr="{ 'aria-label': 'Clear all caches' }"
            @click="openClearAllDialog"
          />
        </div>
      </div>

      <div v-if="!filteredCacheItems.length" class="table-empty-state">
        <h3>No matching cache items</h3>
        <p>Try a different cache name or key.</p>
      </div>

      <GenericDataGrid
        ref="cacheGridRef"
        v-else
        :data="filteredCacheItems"
        :columns="gridColumns"
        :page-size="10"
        :enable-export="false"
        :enable-clear-filters="false"
        pager-info-text="{0} - {1} of {2} cache items"
        row-key="id"
      >
        <template #nameTemplate="{ data: row }">
          <div class="cache-name-cell">
            <span class="cache-avatar" aria-hidden="true">
              <svg width="18" height="18" viewBox="0 0 24 24">
                <path
                  fill="currentColor"
                  d="M10 4H2v16h20V6H12l-2-2zm0 2l2 2h8v12H4V6h6zm2 4v6h4v-6h-4z"
                />
              </svg>
            </span>
            <span class="cache-name-text">
              {{ row.label }}
            </span>
          </div>
        </template>
        <template #cacheKeyTemplate="{ data: row }">
          <span class="cache-key-text">
            {{ row.id }}
          </span>
        </template>
        <template #countTemplate="{ data: row }">
          <span class="count-badge" :aria-label="`Cache item count for ${row.label}`">
            {{ row.size !== null ? row.size : 0 }}
          </span>
        </template>
        <template #actionsTemplate="{ data: row }">
          <div class="actions-cell">
            <div v-if="isCacheClearing(row.id)" class="row-loading-indicator" aria-live="polite">
              <DxLoadIndicator :width="18" :height="18" />
              <span>Clearing...</span>
            </div>
            <span
              v-else
              class="cache-action-trigger"
              @mouseenter="showTooltip($event, 'Clear')"
              @mousemove="moveTooltip"
              @mouseleave="hideTooltip"
            >
              <DxButton
                class="cache-action-btn"
                icon="trash"
                type="normal"
                styling-mode="text"
                :disabled="isClearingAll"
                :element-attr="{ 'aria-label': `Clear cache ${row.label}` }"
                @click="openDeleteDialog(row)"
              />
            </span>
          </div>
        </template>
      </GenericDataGrid>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { DxButton, DxLoadPanel } from 'devextreme-vue';
import DxLoadIndicator from 'devextreme-vue/load-indicator';
import DxTextBox from 'devextreme-vue/text-box';
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import CommonTooltip from '@/components/common/Tooltip.vue';
import { SettingsService } from '@/api/services/SettingsService';
import { useTooltip } from '@/composables/useTooltip';
import { splitCamelCase } from '@/utils/stringFormatUtils';
import Alert from '@/utils/notificationUtils';
import type { CacheStat } from '@/types/CacheTypes';

interface CacheItem {
  id: string;
  label: string;
  size: number | null;
}

const statsData = ref<Record<string, CacheStat> | null>(null);
const isStatsLoading = ref(true);
const isStatsError = ref(false);
const isClearingAll = ref(false);
const clearingCacheIds = ref(new Set<string>());
const cacheToDelete = ref<CacheItem | null>(null);
const cacheGridRef = ref<InstanceType<typeof GenericDataGrid> | null>(null);
const searchText = ref('');
const { tooltip, showTooltip, moveTooltip, hideTooltip } = useTooltip();

function refreshGrid() {
  void nextTick(() => {
    const gridInstance = cacheGridRef.value?.instance;

    if (gridInstance && typeof gridInstance.repaint === 'function') {
      gridInstance.repaint();
      return;
    }

    if (gridInstance && typeof gridInstance.refresh === 'function') {
      void gridInstance.refresh();
    }
  });
}

// Dialog state
const dialogState = ref<{ open: boolean; type: 'delete' | 'clearAll' | null }>({
  open: false,
  type: null,
});
const dialogTitle = computed(() =>
  dialogState.value.type === 'delete' ? 'Confirm Delete' : 'Confirm Clear All'
);
const dialogDescription = computed(() => {
  if (dialogState.value.type === 'delete' && cacheToDelete.value) {
    return `Do you really want to delete the ${cacheToDelete.value.label}? Clearing this cache may temporarily affect system performance until it is rebuilt.`;
  }
  if (dialogState.value.type === 'clearAll') {
    return 'Do you really want to clear all caches? Clearing all caches may temporarily affect system performance until they are rebuilt.';
  }
  return '';
});
const dialogConfirmLabel = computed(() =>
  dialogState.value.type === 'delete' ? 'Delete' : 'Clear All'
);
const dialogConfirmColor = computed(() =>
  dialogState.value.type === 'clearAll' ? 'error' : undefined
);
const dialogLoading = computed(() => {
  if (dialogState.value.type === 'clearAll') {
    return isClearingAll.value;
  }

  return !!cacheToDelete.value && clearingCacheIds.value.has(cacheToDelete.value.id);
});

const gridColumns = [
  {
    dataField: 'label',
    caption: 'Cache Name',
    template: 'nameTemplate',
    minWidth: 220,
    allowFiltering: false,
  },
  {
    dataField: 'id',
    caption: 'Cache Key',
    template: 'cacheKeyTemplate',
    minWidth: 240,
    allowFiltering: false,
  },
  {
    dataField: 'size',
    caption: 'Count',
    template: 'countTemplate',
    width: 120,
    alignment: 'center' as const,
  },
  {
    caption: 'Actions',
    template: 'actionsTemplate',
    allowSorting: false,
    allowFiltering: false,
    width: 140,
    alignment: 'center' as const,
  },
];

const fetchStats = async (showLoading = true, shouldRefreshGrid = true) => {
  if (showLoading) {
    isStatsLoading.value = true;
  }
  isStatsError.value = false;
  try {
    const data = await SettingsService.getAllCacheStats();
    statsData.value = data ? { ...data } : {};
    if (shouldRefreshGrid) {
      refreshGrid();
    }
  } catch {
    isStatsError.value = true;
  } finally {
    if (showLoading) {
      isStatsLoading.value = false;
    }
  }
};

onMounted(() => {
  void fetchStats();
});

const cacheItems = computed<CacheItem[]>(() => {
  if (!statsData.value) return [];
  return Object.keys(statsData.value)
    .map(id => ({
      id,
      label: splitCamelCase(id),
      size: statsData.value![id]?.size ?? null,
    }))
    .sort((first, second) => first.label.localeCompare(second.label));
});

const filteredCacheItems = computed<CacheItem[]>(() => {
  const searchValue = searchText.value.trim().toLowerCase();
  if (!searchValue) {
    return cacheItems.value;
  }

  return cacheItems.value.filter(item => {
    return (
      item.label.toLowerCase().includes(searchValue) || item.id.toLowerCase().includes(searchValue)
    );
  });
});

function isCacheClearing(cacheId: string) {
  return clearingCacheIds.value.has(cacheId);
}

function retryFetchStats() {
  void fetchStats();
}

function handleClearFilters() {
  searchText.value = '';
  cacheGridRef.value?.instance?.clearFilter();
  cacheGridRef.value?.instance?.clearSorting();
}

function openDeleteDialog(item: CacheItem) {
  cacheToDelete.value = item;
  dialogState.value = { open: true, type: 'delete' };
}

function openClearAllDialog() {
  dialogState.value = { open: true, type: 'clearAll' };
}

function closeDialog() {
  dialogState.value = { open: false, type: null };
  cacheToDelete.value = null;
}

async function handleDialogConfirm() {
  if (dialogState.value.type === 'delete' && cacheToDelete.value) {
    const cacheId = cacheToDelete.value.id;
    try {
      clearingCacheIds.value.add(cacheId);
      refreshGrid();
      await SettingsService.clearCacheByName({ cacheName: cacheId });
      Alert.success(`Cache '${cacheToDelete.value.label}' cleared`);
      await fetchStats(false, false);
    } catch (e: unknown) {
      const err = e instanceof Error ? e.message : 'Unknown error';
      Alert.error(`Failed to clear cache '${cacheToDelete.value.label}': ${err}`);
    } finally {
      clearingCacheIds.value.delete(cacheId);
      refreshGrid();
    }
    closeDialog();
  } else if (dialogState.value.type === 'clearAll') {
    isClearingAll.value = true;
    refreshGrid();
    try {
      await SettingsService.clearAllCaches();
      Alert.success('All caches cleared');
      await fetchStats(false, false);
    } catch (e: unknown) {
      const err = e instanceof Error ? e.message : 'Unknown error';
      Alert.error(`Failed to clear all caches: ${err}`);
    }
    isClearingAll.value = false;
    refreshGrid();
    closeDialog();
  }
}
</script>

<style src="./ClearCachePage.css"></style>
