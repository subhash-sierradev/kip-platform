<style scoped>
.cache-chip .dx-button-content {
  padding-block: 4px;
}
</style>

<template>
  <div class="cache-statistics-page">
    <h2 class="title">Cache Statistics</h2>
    <div class="section cache-select-section">
      <div class="cache-label-row">
        <div class="section-label">Select Cache</div>
      </div>
      <div v-if="isStatsLoading" class="loading">Loading cache options...</div>
      <div v-else-if="isStatsError" class="error">Error loading cache options.</div>
      <div class="cache-options-scroll-wrapper cache-options-modern">
        <span
          v-for="option in cacheOptions"
          :key="option"
          class="cache-chip-wrapper"
          @mouseenter="e => showTooltip(e, option)"
          @mousemove="moveTooltip"
          @mouseleave="hideTooltip"
        >
          <DxButton
            :text="option.length > 25 ? option.slice(0, 25) + '…' : option"
            :type="selectedCache === option ? 'default' : 'normal'"
            :styling-mode="selectedCache === option ? 'contained' : 'outlined'"
            :class="['cache-chip-modern', { selected: selectedCache === option }]"
            :aria-label="option"
            :tabindex="0"
            @click="() => handleCacheSelection(option)"
          />
        </span>
      </div>
    </div>
    <div v-if="isStatsLoading" class="loading">Loading cache statistics...</div>
    <div v-else-if="isStatsError" class="error">Error loading cache statistics.</div>
    <div v-else class="stats-cards-modern">
      <div class="stats-card-modern">
        <div class="stats-title-modern">
          {{ selectedCache === 'All' ? 'Aggregated Cache Size' : 'Cache Size' }}
        </div>
        <div class="stats-value-modern">{{ statsValue('size') }}</div>
      </div>
      <div class="stats-card-modern">
        <div class="stats-title-modern">Request Count</div>
        <div class="stats-value-modern request-count">{{ statsValue('requestCount') }}</div>
      </div>
      <div class="stats-card-modern">
        <div class="stats-title-modern">Hit / Miss Ratio</div>
        <div class="stats-ratio-modern">
          <span class="hit-modern">Hit Rate: {{ statsValue('hitRate') }}</span>
          <span class="miss-modern">Miss Rate: {{ statsValue('missRate') }}</span>
        </div>
      </div>
    </div>
    <div class="note-modern">Statistics update based on selected cache or all caches.</div>
    <teleport to="body">
      <CommonTooltip v-bind="tooltip" />
    </teleport>
  </div>
</template>

<script setup lang="ts">
import './CacheStatisticsPage.css';
import { ref, computed, onMounted } from 'vue';
import { DxButton } from 'devextreme-vue';
import { SettingsService } from '@/api/services/SettingsService';
import { splitCamelCase } from '@/utils/stringFormatUtils';
import CommonTooltip from '@/components/common/Tooltip.vue';
import { useTooltip } from '@/composables/useTooltip';
import type { CacheStat, AggregatedStats } from '@/types/CacheTypes';

type Stats = CacheStat | AggregatedStats;

const statsData = ref<Record<string, CacheStat> | null>(null);
const isStatsLoading = ref(true);
const isStatsError = ref(false);
const selectedCache = ref('All');

const { tooltip, showTooltip, moveTooltip, hideTooltip } = useTooltip();

onMounted(async () => {
  await fetchStats();
});

async function fetchStats() {
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
}

const cacheOptions = computed(() => {
  if (statsData.value && typeof statsData.value === 'object') {
    const cacheKeys = Object.keys(statsData.value);
    const sortedCacheNames = cacheKeys
      .map(id => splitCamelCase(id))
      .sort((a, b) => a.localeCompare(b));
    return ['All', ...sortedCacheNames];
  }
  return ['All'];
});

function handleCacheSelection(cache: string) {
  selectedCache.value = cache;
}

function getCacheKey(label: string): string | null {
  if (!statsData.value) return null;
  if (!label || label === 'All') return null;
  const cacheKeys = Object.keys(statsData.value);
  const found = cacheKeys.find(id => splitCamelCase(id) === label);
  return found || null;
}

const stats = computed<Stats | null>(() => {
  if (statsData.value && typeof statsData.value === 'object') {
    if (selectedCache.value === 'All') {
      const cacheCount = Object.keys(statsData.value).length;
      let sumHitRate = 0;
      let sumMissRate = 0;
      let _totalSize = 0;
      let totalRequests = 0;
      let totalHit = 0;
      let totalMiss = 0;
      Object.values(statsData.value).forEach((stat: CacheStat) => {
        _totalSize += stat.size || 0;
        sumHitRate += stat.hitRate || 0;
        sumMissRate += stat.missRate || 0;
        totalRequests += stat.requestCount || 0;
        totalHit += (stat.requestCount || 0) * (stat.hitRate || 0);
        totalMiss += (stat.requestCount || 0) * (stat.missRate || 0);
      });
      // If there are no requests, hitRate and missRate are set to 0 by design to avoid division by zero.
      // This is the expected default when no requests exist.
      let hitRate = 0;
      let missRate = 0;
      if (totalRequests > 0) {
        hitRate = totalHit / totalRequests;
        missRate = totalMiss / totalRequests;
      }
      return {
        cacheCount,
        totalSize: _totalSize,
        requestCount: totalRequests,
        hitRate,
        missRate,
        avgHitRate: cacheCount ? sumHitRate / cacheCount : 0,
        avgMissRate: cacheCount ? sumMissRate / cacheCount : 0,
      };
    } else {
      const key = getCacheKey(selectedCache.value);
      return (
        (key && statsData.value[key]) || {
          size: 0,
          hitRate: 0,
          missRate: 0,
          requestCount: 0,
        }
      );
    }
  }
  return null;
});

function getSizeValue(statsObj: CacheStat | AggregatedStats): number {
  return (statsObj as AggregatedStats).totalSize ?? (statsObj as CacheStat).size ?? 0;
}

function getRequestCount(statsObj: CacheStat | AggregatedStats): number {
  return statsObj.requestCount ?? 0;
}

function getHitRateValue(statsObj: CacheStat | AggregatedStats): string {
  const value = (statsObj as AggregatedStats).avgHitRate ?? (statsObj as CacheStat).hitRate ?? 0;
  return `${(value * 100).toFixed(1)}%`;
}

function getMissRateValue(statsObj: CacheStat | AggregatedStats): string {
  const value = (statsObj as AggregatedStats).avgMissRate ?? (statsObj as CacheStat).missRate ?? 0;
  return `${(value * 100).toFixed(1)}%`;
}

function statsValue(field: string) {
  if (!stats.value) {
    return field === 'hitRate' || field === 'missRate' ? '0.0%' : 0;
  }

  switch (field) {
    case 'size':
      return getSizeValue(stats.value);
    case 'requestCount':
      return getRequestCount(stats.value);
    case 'hitRate':
      return getHitRateValue(stats.value);
    case 'missRate':
      return getMissRateValue(stats.value);
    default:
      return 0;
  }
}
</script>
