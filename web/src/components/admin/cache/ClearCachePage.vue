<template>
  <div class="clear-cache-page">
    <div class="header-row">
      <h2 class="title">Clear Cache</h2>
      <DxButton
        class="clear-all-btn"
        text="Clear All"
        :type="'danger'"
        :disabled="isClearingAll"
        @click="openClearAllDialog"
      />
    </div>
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
      :loading="dialogState.type === 'clearAll' ? isClearingAll : false"
      :max-width="400"
      @cancel="closeDialog"
      @confirm="handleDialogConfirm"
    />

    <div class="cache-card">
      <div v-if="isStatsError" class="error">Error loading cache items.</div>
      <div v-else-if="!cacheItems.length && !isStatsLoading" class="empty">
        No cache items found.
      </div>
      <div v-else class="cache-grid">
        <div v-for="item in cacheItems" :key="item.id" class="cache-row">
          <div class="cache-main">
            <span class="cache-avatar">
              <svg width="20" height="20" viewBox="0 0 24 24">
                <path fill="#888" d="M10 4H2v16h20V6H12l-2-2zm0 2l2 2h8v12H4V6h6zm2 4v6h4v-6h-4z" />
              </svg>
            </span>
            <span class="cache-label" :title="item.label">{{ item.label }}</span>
          </div>
          <PolishedPillStack
            :count="item.size !== null ? item.size : 0"
            :aria-label="`Cache item count for ${item.label}`"
          />
          <DxButton
            class="delete-btn"
            icon="trash"
            :type="'danger'"
            styling-mode="text"
            :disabled="false"
            @click="openDeleteDialog(item)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { DxButton, DxLoadPanel } from 'devextreme-vue';
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import PolishedPillStack from '@/components/common/PolishedPillStack.vue';
import { SettingsService } from '@/api/services/SettingsService';
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
const cacheToDelete = ref<CacheItem | null>(null);

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

const fetchStats = async () => {
  isStatsLoading.value = true;
  isStatsError.value = false;
  try {
    const data = await SettingsService.getAllCacheStats();
    statsData.value = data;
  } catch {
    isStatsError.value = true;
  } finally {
    isStatsLoading.value = false;
  }
};

onMounted(fetchStats);

const cacheItems = computed<CacheItem[]>(() => {
  if (!statsData.value) return [];
  return Object.keys(statsData.value).map(id => ({
    id,
    label: splitCamelCase(id),
    size: statsData.value![id]?.size ?? null,
  }));
});

function openDeleteDialog(item: CacheItem) {
  cacheToDelete.value = item;
  dialogState.value = { open: true, type: 'delete' };
}

function openClearAllDialog() {
  dialogState.value = { open: true, type: 'clearAll' };
}

function closeDialog() {
  dialogState.value.open = false;
  cacheToDelete.value = null;
}

async function handleDialogConfirm() {
  if (dialogState.value.type === 'delete' && cacheToDelete.value) {
    try {
      await SettingsService.clearCacheByName({ cacheName: cacheToDelete.value.id });
      Alert.success(`Cache '${cacheToDelete.value.label}' cleared`);
      await fetchStats();
    } catch (e: unknown) {
      const err = e instanceof Error ? e.message : 'Unknown error';
      Alert.error(`Failed to clear cache '${cacheToDelete.value.label}': ${err}`);
    }
    closeDialog();
  } else if (dialogState.value.type === 'clearAll') {
    isClearingAll.value = true;
    try {
      await SettingsService.clearAllCaches();
      Alert.success('All caches cleared');
      await fetchStats();
    } catch (e: unknown) {
      const err = e instanceof Error ? e.message : 'Unknown error';
      Alert.error(`Failed to clear all caches: ${err}`);
    }
    isClearingAll.value = false;
    closeDialog();
  }
}
</script>

<style src="./ClearCachePage.css"></style>
