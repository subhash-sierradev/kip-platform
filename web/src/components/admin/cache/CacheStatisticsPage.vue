<style scoped>
.cache-chip .dx-button-content {
  padding-block: 4px;
}
</style>

<template>
  <div class="cache-statistics-page">
    <div v-if="isStatsError" class="state-card error-state">
      <div>
        <h3>Unable to load cache statistics</h3>
        <p>Try refreshing the page data and then select a cache region again.</p>
      </div>
      <DxButton text="Retry" type="normal" styling-mode="outlined" @click="fetchStats" />
    </div>
    <template v-else>
      <div class="cache-layout-grid">
        <aside class="cache-selection-panel">
          <div class="section cache-select-section">
            <div class="section-header">
              <div class="section-heading-row">
                <div class="section-label">Select Cache</div>
                <p class="section-helper">
                  Choose a specific cache region or stay on All to review aggregate platform
                  metrics.
                </p>
              </div>
            </div>

            <div v-if="isStatsLoading" class="state-inline loading-inline">
              <DxLoadIndicator :width="20" :height="20" />
              <span>Loading cache options...</span>
            </div>
            <div v-else-if="hasCacheData" class="cache-options-scroll-wrapper cache-options-modern">
              <span v-for="option in cacheOptions" :key="option" class="cache-chip-wrapper">
                <DxButton
                  :text="option"
                  :type="selectedCache === option ? 'default' : 'normal'"
                  :styling-mode="selectedCache === option ? 'contained' : 'outlined'"
                  :class="['cache-chip-modern', { selected: selectedCache === option }]"
                  :aria-label="option"
                  :tabindex="0"
                  @click="() => handleCacheSelection(option)"
                />
              </span>
            </div>
            <div v-else class="state-inline empty-inline">
              <span>No cache filters are available yet.</span>
            </div>
          </div>
        </aside>

        <section class="cache-metrics-panel">
          <div v-if="isStatsLoading" class="state-card loading-state">
            <DxLoadIndicator :width="24" :height="24" />
            <div>
              <h3>Loading cache statistics</h3>
              <p>Preparing current cache metrics for the selected region.</p>
            </div>
          </div>
          <div v-else-if="hasCacheData" class="stats-cards-modern">
            <div class="stats-card-modern">
              <div class="stats-card-header">
                <span class="stats-card-kicker">Capacity</span>
                <span class="stats-context">{{ capacityContextLabel }}</span>
              </div>
              <div class="stats-title-modern">{{ capacityTitle }}</div>
              <div class="stats-value-modern">{{ statsValue('size') }}</div>
            </div>
            <div class="stats-card-modern">
              <div class="stats-card-header">
                <span class="stats-card-kicker">Traffic</span>
                <span class="stats-context">Requests</span>
              </div>
              <div class="stats-title-modern">Request Count</div>
              <div class="stats-value-modern">{{ statsValue('requestCount') }}</div>
            </div>
            <div class="stats-card-modern ratio-card">
              <div class="stats-card-header">
                <span class="stats-card-kicker">Performance</span>
                <span class="stats-context">Hit vs Miss</span>
              </div>
              <div class="stats-title-modern">Hit / Miss Ratio</div>
              <div class="stats-ratio-modern">
                <span class="ratio-pill ratio-pill-hit">Hit Rate: {{ statsValue('hitRate') }}</span>
                <span class="ratio-pill ratio-pill-miss"
                  >Miss Rate: {{ statsValue('missRate') }}</span
                >
              </div>
            </div>
          </div>
          <div v-else class="state-card empty-state">
            <i class="dx-icon dx-icon-box empty-icon" aria-hidden="true"></i>
            <h3>No cache statistics found</h3>
            <p>Cache metrics will appear here once the application reports cache activity.</p>
          </div>

          <div class="note-modern">
            Statistics update based on the selected cache or the aggregated All selection.
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import './CacheStatisticsPage.css';
import { ref, computed, onMounted } from 'vue';
import { DxButton } from 'devextreme-vue';
import DxLoadIndicator from 'devextreme-vue/load-indicator';
import { SettingsService } from '@/api/services/SettingsService';
import { splitCamelCase } from '@/utils/stringFormatUtils';
import type { CacheStat, AggregatedStats } from '@/types/CacheTypes';

type Stats = CacheStat | AggregatedStats;

const statsData = ref<Record<string, CacheStat> | null>(null);
const isStatsLoading = ref(true);
const isStatsError = ref(false);
const selectedCache = ref('All');

const cacheCount = computed(() => Object.keys(statsData.value ?? {}).length);
const hasCacheData = computed(() => cacheCount.value > 0);

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

const capacityContextLabel = computed(() =>
  selectedCache.value === 'All' ? 'All Caches' : selectedCache.value
);

const capacityTitle = computed(() =>
  selectedCache.value === 'All' ? 'Aggregated Cache Size' : 'Cache Size'
);

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
