<template>
  <div class="filters-section" role="search" aria-label="Audit log filters">
    <div class="filter-group">
      <div class="filter-item">
        <label for="entity-type-filter" class="filter-label">Entity Type</label>
        <select
          id="entity-type-filter"
          class="filter-select"
          aria-label="Filter by entity type"
          :value="selectedEntityType || ''"
          @change="handleEntityTypeChange"
        >
          <option value="">All Entity Types</option>
          <option v-for="type in entityTypes" :key="type.value" :value="type.value">
            {{ type.label }}
          </option>
        </select>
      </div>
      <div class="filter-item">
        <label for="user-filter" class="filter-label">User</label>
        <select
          id="user-filter"
          class="filter-select"
          aria-label="Filter by user"
          :value="selectedUserId || ''"
          @change="handleUserIdChange"
        >
          <option value="">All Users</option>
          <option v-for="user in userIds" :key="user.value" :value="user.value">
            {{ user.label }}
          </option>
        </select>
      </div>
      <div class="filter-item">
        <label for="activity-filter" class="filter-label">Activity</label>
        <select
          id="activity-filter"
          class="filter-select"
          aria-label="Filter by activity type"
          :value="selectedActivity || ''"
          @change="handleActivityChange"
        >
          <option value="">All Activities</option>
          <option v-for="activity in activities" :key="activity.value" :value="activity.value">
            {{ activity.label }}
          </option>
        </select>
      </div>
      <div class="filter-item">
        <button
          type="button"
          class="clear-filters-btn"
          aria-label="Clear all filters"
          :disabled="!hasActiveFilters"
          @click="handleClearFilters"
        >
          Clear Filters
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { EntityType } from '../../../api';

interface FilterOption {
  value: string;
  label: string;
}

interface Props {
  selectedEntityType: EntityType | null;
  selectedUserId: string | null;
  selectedActivity: string | null;
  entityTypes: FilterOption[];
  userIds: FilterOption[];
  activities: FilterOption[];
}

interface Emits {
  'update:selectedEntityType': [value: string | null];
  'update:selectedUserId': [value: string | null];
  'update:selectedActivity': [value: string | null];
  'clear-filters': [];
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

// Computed property to check if there are active filters
const hasActiveFilters = computed((): boolean => {
  return !!(props.selectedEntityType || props.selectedUserId || props.selectedActivity);
});

// Event handlers with proper typing
const handleEntityTypeChange = (event: Event): void => {
  const target = event.target as unknown as { value: string };
  const value = target.value;
  emit('update:selectedEntityType', value === '' ? null : value);
};

const handleUserIdChange = (event: Event): void => {
  const target = event.target as unknown as { value: string };
  const value = target.value;
  emit('update:selectedUserId', value === '' ? null : value);
};

const handleActivityChange = (event: Event): void => {
  const target = event.target as unknown as { value: string };
  const value = target.value;
  emit('update:selectedActivity', value === '' ? null : value);
};

const handleClearFilters = (): void => {
  emit('clear-filters');
};
</script>

<script lang="ts">
export default {
  name: 'AuditLogFilters',
};
</script>

<style scoped>
.filters-section {
  background: #ffffff;
  padding: 1rem;
  border-radius: 4px;
  border: 1px solid #ddd;
  margin-bottom: 1rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.filter-group {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.filter-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #4a4a4a;
  margin: 0;
}

.filter-select {
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: white;
  color: #333;
  min-width: 160px;
  font-size: 0.875rem;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #f59e0b;
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.2);
}

.clear-filters-btn {
  padding: 0.5rem 1rem;
  background: #6b7280;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  margin-top: 1.25rem;
  transition: background-color 0.2s ease;
}

.clear-filters-btn:hover:not(:disabled) {
  background: #4b5563;
}

.clear-filters-btn:disabled {
  background: #d1d5db;
  color: #9ca3af;
  cursor: not-allowed;
}

.clear-filters-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(107, 114, 128, 0.3);
}
</style>
