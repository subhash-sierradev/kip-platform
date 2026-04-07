<template>
  <div class="cf-panel-card">
    <!-- CARD HEADER -->
    <div class="cf-panel-header">
      <div class="cf-panel-header-left">
        <span class="cf-panel-title">Add Jira Custom Fields</span>
        <span v-if="rowsLocal.length > 0" class="cf-count-badge">{{ rowsLocal.length }}</span>
        <span class="cf-optional-label">Optional</span>
      </div>
      <button type="button" class="cf-panel-add-btn" :disabled="loading" @click="addRow">
        <svg width="11" height="11" viewBox="0 0 12 12" fill="currentColor" aria-hidden="true">
          <path
            d="M6.75 0.75a.75.75 0 0 0-1.5 0V5.25H0.75a.75.75 0 0 0 0 1.5H5.25v4.5a.75.75 0 0 0 1.5 0V6.75h4.5a.75.75 0 0 0 0-1.5H6.75V0.75Z"
          />
        </svg>
        Add Field
      </button>
    </div>

    <!-- STATUS BANNERS -->
    <div v-if="loading" class="cf-status-banner cf-status-loading">
      <svg
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2.5"
        class="cf-spin"
        aria-hidden="true"
      >
        <path d="M21 12a9 9 0 1 1-6.219-8.56" />
      </svg>
      Loading fields…
    </div>
    <div v-if="loadError" class="cf-status-banner cf-status-error">{{ loadError }}</div>

    <!-- ROWS + EMPTY STATE -->
    <TransitionGroup name="cf-row" tag="div" class="cf-rows-list">
      <!-- EMPTY STATE -->
      <div v-if="rowsLocal.length === 0 && !loading" key="__empty__" class="cf-empty-state">
        <svg width="44" height="44" viewBox="0 0 44 44" fill="none" aria-hidden="true">
          <rect x="7" y="11" width="30" height="5" rx="2.5" fill="#e5e7eb" />
          <rect x="7" y="20" width="22" height="5" rx="2.5" fill="#e5e7eb" />
          <rect x="7" y="29" width="26" height="5" rx="2.5" fill="#e5e7eb" />
          <circle cx="34" cy="34" r="8" fill="#f9fafb" stroke="#d1d5db" stroke-width="1.5" />
          <path
            d="M34 30v4l2.5 2.5"
            stroke="#9ca3af"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <p class="cf-empty-title">No custom fields added</p>
        <p class="cf-empty-sub">Enrich your Jira issues by mapping optional custom fields</p>
      </div>

      <!-- FIELD ROWS -->
      <div
        v-for="(row, idx) in rowsLocal"
        :key="row._id"
        class="cf-row-item"
        :ref="el => setRowRef(el, idx)"
      >
        <CustomFieldRow
          :connectionId="connectionId"
          :projectKey="props.projectKey"
          :row="row"
          :idx="idx"
          :fields="allFields"
          :projectUsers="projectUsers"
          :renderedPreview="renderedPreview[idx]"
          :initialLabel="labels[idx] || ''"
          :usedFieldKeys="usedFieldKeys"
          @row-change="onRowChange"
          @open-insert="onChildOpenInsert"
          @toggle-preview="onChildTogglePreview"
        />
        <button
          type="button"
          class="cf-trash-btn"
          aria-label="Remove custom field"
          @click="removeRow(idx)"
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <polyline points="3 6 5 6 21 6" />
            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
            <path d="M10 11v6M14 11v6" />
            <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
          </svg>
        </button>
      </div>
    </TransitionGroup>

    <!-- Confirmation Modal -->
    <div v-if="dialogOpen" class="cf-modal-backdrop" aria-modal="true" role="dialog">
      <div class="cf-modal">
        <h3 class="cf-modal-title">Remove Custom Field</h3>
        <p class="cf-modal-desc">
          This will remove the selected custom field mapping. This action cannot be undone.
        </p>
        <div class="cf-modal-actions">
          <button
            type="button"
            class="ms-btn-cancel"
            @click="closeDialog"
            :disabled="actionLoading"
          >
            Cancel
          </button>
          <button
            type="button"
            class="ms-btn-danger"
            @click="onConfirmDelete"
            :disabled="actionLoading"
          >
            Remove
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import type { ComponentPublicInstance } from 'vue';
import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';
import './CustomFieldsPanel.css';
import CustomFieldRow from './CustomFieldRow.vue';
import { useConfirmationDialog } from '@/composables/useConfirmationDialog';
import {
  generateUid,
  mapAndFilterFields,
  sortFields,
  isRowsValid,
  INCLUDE_KEYS,
} from '../utils/CustomFieldHelper';

// Props & emits
const props = defineProps<{
  connectionId: string;
  projectKey?: string;
  issueTypeId?: string;
  isSubtaskIssueType?: boolean;
  rows: Array<{ field: string; value: string; label?: string; type?: string }>;
  projectUsers: Array<{ accountId: string; displayName: string }>;
  renderedPreviews?: string[];
}>();

const emit = defineEmits<{
  (e: 'update:rows', value: any[]): void;
  (e: 'valid-change', value: boolean): void;
  (e: 'open-insert', payload: { rowIndex: number }): void;
  (e: 'toggle-preview', payload: { rowIndex: number; value: string }): void;
}>();

defineExpose({ insertJsonPlaceholder });

// State
const rowsLocal = ref<{ _id: string; field: string; value: string }[]>([]);
const labels = ref<string[]>([]);
const rawFields = ref<any[]>([]);
const allFields = computed(() =>
  props.isSubtaskIssueType
    ? rawFields.value.filter(field => field.type !== 'sprint')
    : rawFields.value
);
const loading = ref(false);
const loadError = ref('');
const projectUsers = computed(() => props.projectUsers || []);
const activeStringRowIndex = ref<number | null>(null);
const renderedPreview = computed(() => props.renderedPreviews || []);
const rowRefs = ref<Array<HTMLElement | null>>([]);
// Guard: prevents the props.rows watcher from calling initFromParent when the
// update:rows emission originated from within this component. Without this,
// every value edit regenerates _ids → destroys/remounts all CustomFieldRow
// children → their sprint/team watchers (immediate:true) re-fire → API spam.
const isSyncing = ref(false);
function setRowRef(el: Element | ComponentPublicInstance | null, idx: number) {
  const raw = (el as any)?.$el ? (el as any).$el : el;
  rowRefs.value[idx] = (raw as HTMLElement) || null;
}

// Confirmation dialog
const { dialogOpen, actionLoading, openDialog, closeDialog, confirmWithHandlers } =
  useConfirmationDialog();
const pendingDeleteIndex = ref<number | null>(null);

// Helpers
const fieldMap = computed(() => Object.fromEntries(allFields.value.map((f: any) => [f.key, f])));
function fieldType(key: string) {
  return fieldMap.value[key]?.type || 'string';
}
const usedFieldKeys = computed<string[]>(() => rowsLocal.value.map(r => r.field).filter(Boolean));

// API
async function loadMetadata() {
  const projectKey = String(props.projectKey || '').trim();
  if (!props.connectionId || !projectKey) {
    rawFields.value = [];
    loadError.value = '';
    return;
  }

  loading.value = true;
  loadError.value = '';
  try {
    const resp = await JiraIntegrationService.getProjectMetaFieldsByConnectionId(
      props.connectionId,
      projectKey,
      props.issueTypeId
    );
    rawFields.value = mapAndFilterFields(resp, INCLUDE_KEYS).sort(sortFields);
  } catch {
    loadError.value = 'Failed to load Jira fields.';
    rawFields.value = [];
  } finally {
    loading.value = false;
  }
}

function resetRowsForSelectionChange() {
  rowsLocal.value = [];
  labels.value = [];
  emit('valid-change', true);
  syncRows();
}

// Init & sync
function initFromParent() {
  const incoming = Array.isArray(props.rows) ? props.rows : [];
  // Preserve existing _ids at matching indices so CustomFieldRow components
  // are NOT destroyed/recreated when only values change. Recreating them
  // triggers their sprint/team watchers (immediate:true) causing API calls
  // on every keystroke.
  rowsLocal.value = incoming.map((r, i) => ({
    _id: rowsLocal.value[i]?._id ?? generateUid(),
    field: r.field,
    value: r.value,
  }));
  labels.value = incoming.map(r => r.label ?? '');
}

function syncRows() {
  isSyncing.value = true;
  const payload = rowsLocal.value.map((r, i) => ({
    field: r.field,
    value: r.value,
    label: labels.value[i] || fieldMap.value[r.field]?.name || '',
    type: fieldType(r.field),
  }));
  emit('update:rows', payload);
  emit('valid-change', isRowsValid(rowsLocal.value));
  nextTick(() => {
    isSyncing.value = false;
  });
}

// Insert placeholder
function openInsert(idx: number) {
  activeStringRowIndex.value = idx;
  emit('open-insert', { rowIndex: idx });
}

function insertJsonPlaceholder(placeholder: string) {
  if (activeStringRowIndex.value === null) return;
  const idx = activeStringRowIndex.value;
  const row = rowsLocal.value[idx];
  row.value = `${row.value || ''} ${placeholder}`.trim();
  syncRows();
}

// Row actions
function addRow() {
  rowsLocal.value.push({ _id: generateUid(), field: '', value: '' });
  labels.value.push('');
  syncRows();
  nextTick(() => {
    const lastIdx = rowsLocal.value.length - 1;
    const rowEl = rowRefs.value[lastIdx];
    if (rowEl && rowEl.scrollIntoView) {
      rowEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  });
}
function executeRemoveRow(idx: number) {
  rowsLocal.value.splice(idx, 1);
  labels.value.splice(idx, 1);
  syncRows();
}
function removeRow(idx: number) {
  pendingDeleteIndex.value = idx;
  openDialog('delete');
}

// Lifecycle
onMounted(async () => {
  initFromParent();
});
watch(
  () => props.rows,
  () => {
    // Skip when the change was emitted by syncRows — reacting to our own
    // emission regenerates _ids, remounts all CustomFieldRow children, and
    // re-triggers their sprint/team API fetches on every value change.
    if (isSyncing.value) return;
    initFromParent();
  },
  { deep: true }
);
watch(
  () => [props.projectKey, props.issueTypeId],
  async (nextValues, prevValues) => {
    if (!prevValues) {
      await loadMetadata();
      return;
    }

    const [nextProject, nextIssueType] = nextValues;
    const [prevProject, prevIssueType] = prevValues;
    if (nextProject === prevProject && nextIssueType === prevIssueType) {
      return;
    }
    resetRowsForSelectionChange();
    await loadMetadata();
  },
  { immediate: true }
);

// Confirm modal handler
async function onConfirmDelete() {
  await confirmWithHandlers({
    delete: () => {
      const idx = pendingDeleteIndex.value;
      if (idx == null) return false;
      executeRemoveRow(idx);
      return true;
    },
  });
}

// Child events
function onRowChange(payload: {
  idx: number;
  field: string;
  value: string;
  label: string;
  type: string;
}) {
  const { idx, field, value, label } = payload;
  rowsLocal.value[idx].field = field;
  rowsLocal.value[idx].value = value;
  labels.value[idx] = label;
  syncRows();
}
function onChildOpenInsert(payload: { rowIndex: number }) {
  openInsert(payload.rowIndex);
}
function onChildTogglePreview(payload: { rowIndex: number; value: string }) {
  emit('toggle-preview', payload);
}
</script>

<style src="./CustomFieldsPanel.css"></style>
