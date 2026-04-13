<template>
  <div class="ms-selects-rows">
    <!-- Row 1: Jira Project | Assignee (always) -->
    <div class="ms-grid-two">
      <div class="ms-field">
        <label class="ms-label">
          Jira Project
          <span class="ms-required">*</span>
        </label>
        <select class="ms-input ms-select" v-model="projectValue" @change="emit('project-change')">
          <option value="">Select a Project</option>
          <option v-for="p in projects" :key="p.key" :value="p.key">
            {{ p.name }}
          </option>
        </select>
      </div>

      <div class="ms-field ms-field--assignee">
        <label class="ms-label">Assignee</label>
        <select
          class="ms-input ms-select"
          v-model="assigneeValue"
          :disabled="!projectValue"
          @change="emit('assignee-change')"
        >
          <option value="">Select an Assignee</option>
          <option v-for="u in users" :key="u.accountId" :value="u.accountId">
            {{ u.displayName }}
          </option>
        </select>
      </div>
    </div>

    <!-- Row 2: Issue Type (left half always) | Subtask's Parent (right half, appears/disappears) -->
    <div class="ms-grid-two">
      <div class="ms-field">
        <label class="ms-label">
          Issue Type
          <span class="ms-required">*</span>
        </label>
        <select
          class="ms-input ms-select"
          v-model="issueTypeValue"
          :disabled="!projectValue"
          @change="emit('issue-type-change')"
        >
          <option value="">Select an Issue Type</option>
          <option v-for="t in issueTypes" :key="t.id" :value="t.id">
            {{ t.name }}
          </option>
        </select>
      </div>

      <div v-if="showParentField" class="ms-field ms-field--parent">
        <label class="ms-label">
          Subtask's Parent
          <span class="ms-required">*</span>
        </label>
        <div ref="parentDropdownRef" class="ms-search-select">
          <div class="ms-search-select__control">
            <input
              class="ms-input ms-search-select__input"
              v-model="parentSearchTerm"
              :disabled="isParentSelectDisabled"
              placeholder="Search parent issue"
              type="text"
              role="combobox"
              :id="parentComboboxId"
              :aria-controls="parentListboxId"
              aria-haspopup="listbox"
              aria-autocomplete="list"
              :aria-expanded="isParentDropdownOpen"
              @focus="openParentDropdown"
              @input="onParentSearchInput"
              @keydown.enter.prevent="onParentSearchEnter"
              @keydown.down.prevent="openParentDropdown"
            />
            <button
              type="button"
              class="ms-search-select__toggle"
              :disabled="isParentSelectDisabled"
              @click="toggleParentDropdown"
              aria-label="Toggle parent issue options"
            >
              <span class="ms-search-select__caret" aria-hidden="true"></span>
            </button>
          </div>

          <div
            v-if="isParentDropdownOpen"
            class="ms-search-select__menu"
            role="listbox"
            :id="parentListboxId"
            :aria-labelledby="parentComboboxId"
          >
            <button
              v-for="option in filteredParentFieldOptions"
              :key="option.value"
              type="button"
              class="ms-search-select__option"
              role="option"
              :id="getParentOptionId(option.value)"
              :aria-selected="parentValue === option.value"
              :class="{ 'is-selected': parentValue === option.value }"
              @click="selectParentOption(option)"
              @keydown="onParentOptionKeydown($event, option)"
            >
              {{ option.label }}
            </button>

            <div v-if="filteredParentFieldOptions.length === 0" class="ms-search-select__empty">
              No matching parent issues
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import type { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import type { JiraUser } from '@/api/models/jirawebhook/JiraUser';

const props = defineProps<{
  projects: JiraProject[];
  issueTypes: JiraIssueType[];
  users: JiraUser[];
  selectedProject: string;
  selectedIssueType: string;
  selectedParent: string;
  selectedAssignee: string;
  showParentField?: boolean;
  parentFieldOptions?: Array<{ value: string; label: string }>;
}>();

const emit = defineEmits<{
  (e: 'update:selectedProject', value: string): void;
  (e: 'update:selectedIssueType', value: string): void;
  (e: 'update:selectedParent', value: string): void;
  (e: 'parent-label-change', payload: { value: string; label: string }): void;
  (e: 'update:selectedAssignee', value: string): void;
  (e: 'project-change'): void;
  (e: 'issue-type-change'): void;
  (e: 'parent-change'): void;
  (e: 'parent-search', query: string): void;
  (e: 'assignee-change'): void;
}>();

const MIN_PARENT_SEARCH_LENGTH = 5;

const projectValue = computed({
  get: () => props.selectedProject,
  set: (value: string) => emit('update:selectedProject', value),
});

const issueTypeValue = computed({
  get: () => props.selectedIssueType,
  set: (value: string) => emit('update:selectedIssueType', value),
});

const parentValue = computed({
  get: () => props.selectedParent,
  set: (value: string) => emit('update:selectedParent', value),
});

const assigneeValue = computed({
  get: () => props.selectedAssignee,
  set: (value: string) => emit('update:selectedAssignee', value),
});

const parentSearchTerm = ref('');
const isParentDropdownOpen = ref(false);
const parentDropdownRef = ref<HTMLElement | null>(null);
const lastParentSearchQuery = ref('');
const selectedParentKeyCache = ref('');
const selectedParentLabelCache = ref('');
const instanceUid = getCurrentInstance()?.uid ?? '0';
const parentComboboxId = `parent-issue-combobox-${instanceUid}`;
const parentListboxId = `parent-issue-listbox-${instanceUid}`;

const isParentSelectDisabled = computed(() => !projectValue.value || !issueTypeValue.value);

const filteredParentFieldOptions = computed(() => {
  const allOptions = props.parentFieldOptions || [];
  const query = parentSearchTerm.value.trim().toLowerCase();
  if (!query) {
    return allOptions;
  }

  return allOptions.filter(option => {
    const label = String(option.label || '').toLowerCase();
    const value = String(option.value || '').toLowerCase();
    return label.includes(query) || value.includes(query);
  });
});

function syncParentSearchTermWithSelection() {
  if (isParentDropdownOpen.value) {
    return;
  }

  if (!props.selectedParent) {
    selectedParentKeyCache.value = '';
    selectedParentLabelCache.value = '';
    parentSearchTerm.value = '';
    return;
  }

  const selected = (props.parentFieldOptions || []).find(
    option => option.value === props.selectedParent
  );
  if (selected) {
    selectedParentKeyCache.value = selected.value;
    selectedParentLabelCache.value = selected.label;
    parentSearchTerm.value = selected.label;
    emit('parent-label-change', { value: selected.value, label: selected.label });
    return;
  }

  if (
    selectedParentKeyCache.value === props.selectedParent &&
    selectedParentLabelCache.value.trim().length > 0
  ) {
    parentSearchTerm.value = selectedParentLabelCache.value;
    return;
  }

  parentSearchTerm.value = props.selectedParent;
}

function openParentDropdown() {
  if (isParentSelectDisabled.value) {
    return;
  }
  isParentDropdownOpen.value = true;
}

function toggleParentDropdown() {
  if (isParentSelectDisabled.value) {
    return;
  }
  isParentDropdownOpen.value = !isParentDropdownOpen.value;
}

function onParentSearchInput() {
  openParentDropdown();
  maybeTriggerParentSearch(false);
}

function onParentSearchEnter() {
  openParentDropdown();

  const exactMatch = findExactParentOption(parentSearchTerm.value);
  if (exactMatch) {
    selectParentOption(exactMatch);
    return;
  }

  maybeTriggerParentSearch(true);
}

function onParentOptionKeydown(event: KeyboardEvent, option: { value: string; label: string }) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    selectParentOption(option);
  }
}

function getParentOptionId(value: string): string {
  return `${parentListboxId}-option-${String(value || '').replace(/[^a-zA-Z0-9_-]/g, '_') || 'none'}`;
}

function selectParentOption(option: { value: string; label: string }) {
  parentValue.value = option.value;
  selectedParentKeyCache.value = option.value;
  selectedParentLabelCache.value = option.label;
  parentSearchTerm.value = option.label;
  isParentDropdownOpen.value = false;
  lastParentSearchQuery.value = '';
  emit('parent-label-change', { value: option.value, label: option.label });
  emit('parent-change');
}

function normalizeParentSearchQuery(value: string): string {
  return (value || '').trim();
}

function findExactParentOption(query: string): { value: string; label: string } | undefined {
  const normalized = normalizeParentSearchQuery(query).toLowerCase();
  if (!normalized) {
    return undefined;
  }

  return (props.parentFieldOptions || []).find(option => {
    const label = String(option.label || '')
      .trim()
      .toLowerCase();
    const value = String(option.value || '')
      .trim()
      .toLowerCase();
    return label === normalized || value === normalized;
  });
}

function hasExactParentMatch(query: string): boolean {
  const normalized = normalizeParentSearchQuery(query).toLowerCase();
  if (!normalized) {
    return false;
  }

  return (props.parentFieldOptions || []).some(option => {
    const label = String(option.label || '')
      .trim()
      .toLowerCase();
    const value = String(option.value || '')
      .trim()
      .toLowerCase();
    return label === normalized || value === normalized;
  });
}

function maybeTriggerParentSearch(forceRemoteFallback: boolean) {
  const normalized = normalizeParentSearchQuery(parentSearchTerm.value);
  const shouldResetRemoteResults = normalized.length < MIN_PARENT_SEARCH_LENGTH;
  if (shouldResetRemoteResults) {
    if (lastParentSearchQuery.value) {
      emit('parent-search', '');
      lastParentSearchQuery.value = '';
    }
    return;
  }

  if (isParentSelectDisabled.value) {
    return;
  }

  if (!forceRemoteFallback && hasExactParentMatch(normalized)) {
    return;
  }

  if (!forceRemoteFallback && lastParentSearchQuery.value === normalized) {
    return;
  }

  emit('parent-search', normalized);
  lastParentSearchQuery.value = normalized;
}

function onDocumentMouseDown(event: MouseEvent) {
  if (!isParentDropdownOpen.value) {
    return;
  }

  const target = event.target as Node | null;
  if (!target) {
    return;
  }

  const rootElement = parentDropdownRef.value;
  if (rootElement?.contains(target)) {
    return;
  }

  isParentDropdownOpen.value = false;
  syncParentSearchTermWithSelection();
}

watch(
  () => [props.selectedParent, props.parentFieldOptions],
  () => {
    syncParentSearchTermWithSelection();
  },
  { deep: true, immediate: true }
);

watch(
  () => props.showParentField,
  show => {
    if (!show) {
      isParentDropdownOpen.value = false;
      syncParentSearchTermWithSelection();
    }
  }
);

onMounted(() => {
  document.addEventListener('mousedown', onDocumentMouseDown);
});

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDocumentMouseDown);
});
</script>
<style src="./MappingStep.css" scoped></style>
