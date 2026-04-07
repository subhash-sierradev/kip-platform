<template>
  <div v-if="open" class="ms-modal-backdrop">
    <div class="ms-modal ms-field-modal">
      <h3 class="ms-modal-title">{{ title }}</h3>
      <input
        type="text"
        class="ms-search-field"
        placeholder="Search available fields..."
        v-model="fieldSearch"
        @input="onSearchChange"
      />
      <div class="ms-field-list" role="listbox">
        <div
          v-for="item in filteredFields"
          :key="`${item.placeholder}:${item.depth}`"
          class="ms-field-item"
          role="option"
          tabindex="0"
          @click="emit('select', item.placeholder)"
          @keydown.enter.prevent="emit('select', item.placeholder)"
        >
          <div class="ms-field-item-name" :style="{ paddingLeft: `${item.depth * 14}px` }">
            {{ item.name }}
          </div>
          <div class="ms-field-item-placeholder">{{ item.placeholder }}</div>
        </div>
        <div v-if="filteredFields.length === 0" class="ms-empty-row">No matching fields</div>
      </div>
      <div class="ms-modal-actions">
        <button type="button" class="ms-btn-cancel" @click="emit('close')">Close</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

const props = defineProps<{
  open: boolean;
  jsonSample: string;
  title?: string;
}>();

const emit = defineEmits<{
  (e: 'select', placeholder: string): void;
  (e: 'close'): void;
}>();

const fieldSearch = ref('');

watch(
  () => props.open,
  isOpen => {
    if (!isOpen) fieldSearch.value = '';
  }
);

const availableFields = computed(() => {
  try {
    const parsed = JSON.parse(props.jsonSample || '{}');
    return extractPaths(parsed);
  } catch {
    return [];
  }
});

interface FieldDisplayItem {
  name: string;
  placeholder: string;
}

interface FieldTreeItem extends FieldDisplayItem {
  depth: number;
}

const transformedFields = computed<FieldTreeItem[]>(() => availableFields.value);

const filteredFields = computed<FieldTreeItem[]>(() => {
  const q = fieldSearch.value.trim().toLowerCase();
  if (!q) return transformedFields.value;
  const terms = q.split(/\s+/).filter(Boolean);
  return transformedFields.value.filter(f => {
    const hay = `${f.name} ${f.placeholder}`.toLowerCase();
    return terms.every(t => hay.includes(t));
  });
});

function onSearchChange() {
  // reactive via v-model
}

function extractPaths(obj: unknown): FieldTreeItem[] {
  if (!obj || typeof obj !== 'object') {
    return [];
  }

  const items: FieldTreeItem[] = [];
  const seen = new Set<string>();

  const pushItem = (path: string, depth: number) => {
    if (!path || seen.has(path)) return;
    seen.add(path);
    items.push({
      name: path,
      placeholder: `{{${path}}}`,
      depth,
    });
  };

  const walkArray = (arr: unknown[], path: string, depth: number) => {
    const arrayPath = `${path}[]`;
    pushItem(arrayPath, depth);

    const sample = arr.find(entry => entry !== null && entry !== undefined);
    if (sample === undefined) return;

    if (Array.isArray(sample)) {
      walkArray(sample, arrayPath, depth + 1);
      return;
    }

    if (typeof sample === 'object') {
      walkObject(sample as Record<string, unknown>, arrayPath, depth + 1);
    }
  };

  const walkValue = (value: unknown, path: string, depth: number) => {
    if (Array.isArray(value)) {
      walkArray(value, path, depth);
      return;
    }

    pushItem(path, depth);
    if (value && typeof value === 'object') {
      walkObject(value as Record<string, unknown>, path, depth + 1);
    }
  };

  const walkObject = (objValue: Record<string, unknown>, prefix: string, depth: number) => {
    for (const key of Object.keys(objValue)) {
      const childPath = prefix ? `${prefix}.${key}` : key;
      walkValue(objValue[key], childPath, depth);
    }
  };

  walkObject(obj as Record<string, unknown>, '', 0);
  return items;
}

const title = computed(() => props.title || 'Select Field to Insert');
</script>
<style src="./InsertFieldModal.css" scoped></style>
