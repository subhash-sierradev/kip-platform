<template>
  <div class="ms-selector">
    <div
      class="ms-select-trigger"
      ref="triggerRef"
      :role="dropdownOpen ? undefined : 'button'"
      tabindex="0"
      aria-haspopup="true"
      :aria-expanded="dropdownOpen"
      @click.stop="toggleDropdown"
      @keydown="onTriggerKeydown"
    >
      <span v-if="!dropdownOpen">
        {{ query || 'Search custom fields...' }}
      </span>
      <input
        v-else
        ref="searchRef"
        class="ms-search-visible"
        :value="query"
        @input="onQueryInput"
        @keydown.stop
      />
      <span class="ms-chevron">▾</span>
    </div>

    <div v-if="dropdownOpen" class="ms-dropdown" :class="dropdownPlacement" ref="dropdownRef">
      <button
        v-for="f in filteredFields"
        :key="f.key"
        class="ms-dropdown-item"
        @mousedown.prevent="emit('apply-field', f)"
      >
        <span>{{ f.name }}</span>
        <span class="key">{{ f.key }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue';

import { FieldMeta } from '@/api/models/jirawebhook/FieldMeta';
import {
  decideVerticalPlacement,
  type DropdownPlacement,
} from '@/components/outbound/jirawebhooks/utils/dropdownPlacementUtils';

const props = defineProps<{
  query: string;
  dropdownOpen: boolean;
  dropdownPlacement: DropdownPlacement;
  filteredFields: FieldMeta[];
}>();

const emit = defineEmits<{
  (e: 'update:query', value: string): void;
  (e: 'update:dropdownOpen', value: boolean): void;
  (e: 'placement-change', value: DropdownPlacement): void;
  (e: 'apply-field', value: FieldMeta): void;
}>();

const searchRef = ref<HTMLInputElement | null>(null);
const triggerRef = ref<HTMLElement | null>(null);
const dropdownRef = ref<HTMLElement | null>(null);

function emitPlacement() {
  const placement = decideVerticalPlacement(triggerRef.value, dropdownRef.value);
  emit('placement-change', placement);
}

function toggleDropdown() {
  const nextOpen = !props.dropdownOpen;
  emit('update:dropdownOpen', nextOpen);

  if (nextOpen) {
    emit('update:query', '');
    nextTick(() => {
      searchRef.value?.focus();
      searchRef.value?.select();
      emitPlacement();
    });
  }
}

function onTriggerKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    toggleDropdown();
  }
}

function onQueryInput(event: Event) {
  const target = event.target as HTMLInputElement;
  emit('update:query', target.value);
}
</script>
