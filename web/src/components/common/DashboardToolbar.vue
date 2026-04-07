<template>
  <div class="dashboard-header">
    <div class="dashboard-toolbar">
      <div class="search-bar">
        <span class="search-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="11" cy="11" r="7" stroke="#bdbdbd" stroke-width="2" />
            <path d="M20 20L16.65 16.65" stroke="#bdbdbd" stroke-width="2" stroke-linecap="round" />
          </svg>
        </span>
        <input type="text" :placeholder="searchPlaceholder" v-model="searchModel" />
      </div>

      <div class="sort-and-view">
        <label for="sortBySelect" class="sort-label">Sort by:</label>
        <select id="sortBySelect" v-model="sortByModel" aria-label="Sort items by field">
          <option v-for="option in sortOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>

        <button
          @click="$emit('setViewMode', 'grid')"
          :class="viewMode === 'grid' ? 'active' : ''"
          title="Grid View"
          aria-label="Grid View"
        >
          <svg viewBox="0 0 24 24" class="view-icon">
            <path
              fill="currentColor"
              d="M3 3h7v7H3V3zm9 0h7v7h-7V3zM3 12h7v7H3v-7zm9 0h7v7h-7v-7z"
            />
          </svg>
        </button>

        <button
          @click="$emit('setViewMode', 'list')"
          :class="viewMode === 'list' ? 'active' : ''"
          title="List View"
          aria-label="List View"
        >
          <svg viewBox="0 0 24 24" class="view-icon">
            <path
              fill="currentColor"
              d="M3 5h2v2H3V5zm4 0h12v2H7V5zm0 4h12v2H7V9zM3 9h2v2H3V9zm0 4h2v2H3v-2zm4 0h12v2H7v-2zm-4 4h2v2H3v-2zm4 0h12v2H7v-2z"
            />
          </svg>
        </button>
      </div>

      <div class="toolbar-right">
        <div class="toolbar-pagination">
          <label for="pageSizeSelect" class="page-size-label">Per Page:</label>
          <select
            id="pageSizeSelect"
            :value="pageSize"
            @change="onPageSizeChange"
            aria-label="Select page size"
          >
            <option v-for="size in pageSizeOptions" :key="'ps-' + size" :value="size">
              {{ size }}
            </option>
          </select>

          <span
            class="page-arrow"
            :class="{ disabled: totalCount === 0 || currentPage === 1 }"
            @click="handlePrevPage"
            aria-label="Previous page"
          >
            ‹
          </span>

          <span class="current-page">{{ displayCurrentPage }}</span>

          <span
            class="page-arrow"
            :class="{ disabled: totalCount === 0 || currentPage === totalPages }"
            @click="handleNextPage"
            aria-label="Next page"
          >
            ›
          </span>

          <span class="page-info">{{ pageStart }}-{{ pageEnd }} of {{ totalCount }}</span>
        </div>

        <button :class="createButtonClass" @click="$emit('create')" :aria-label="createButtonText">
          <i v-if="createButtonIcon" :class="createButtonIcon" style="margin-right: 8px"></i>
          {{ createButtonText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { DashboardToolbarProps, DashboardToolbarEmits } from '@/types/dashboard';

// Component name for debugging
defineOptions({ name: 'DashboardToolbar' });

// Props with defaults
const props = withDefaults(defineProps<DashboardToolbarProps>(), {
  searchPlaceholder: 'Search items...',
  createButtonText: '+ Add Item',
  createButtonIcon: undefined,
});

// Emits
const emit = defineEmits<DashboardToolbarEmits>();

// Computed two-way binding for search
const searchModel = computed({
  get: () => props.search,
  set: (value: string) => emit('update:search', value),
});

// Computed two-way binding for sort
const sortByModel = computed({
  get: () => props.sortBy,
  set: (value: string) => emit('update:sortBy', value),
});

// Pagination computed properties
const totalPages = computed(() => Math.ceil(props.totalCount / props.pageSize));

const displayCurrentPage = computed(() => {
  return props.totalCount === 0 ? 0 : props.currentPage;
});

const pageStart = computed(() => {
  if (props.totalCount === 0) return 0;
  return (props.currentPage - 1) * props.pageSize + 1;
});

const pageEnd = computed(() => {
  if (props.totalCount === 0) return 0;
  return Math.min(props.currentPage * props.pageSize, props.totalCount);
});

// Create button CSS class
const createButtonClass = computed(() => {
  // Default to webhook style, but allow customization
  return 'create-webhook-btn';
});

const onPageSizeChange = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  emit('update:pageSize', Number(target.value));
};

const handlePrevPage = () => {
  if (props.totalCount > 0 && props.currentPage > 1) {
    emit('prevPage');
  }
};

const handleNextPage = () => {
  if (props.totalCount > 0 && props.currentPage < totalPages.value) {
    emit('nextPage');
  }
};
</script>

<style>
/* Import common dashboard styles */
@import '@/styles/dashboard-common.css';
</style>
