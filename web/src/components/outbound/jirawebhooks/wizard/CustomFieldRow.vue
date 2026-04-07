<template>
  <FieldSelectorDropdown
    :query="query"
    :dropdown-open="dropdownOpen"
    :dropdown-placement="dropdownPlacement"
    :filtered-fields="filteredFields"
    @update:query="query = $event"
    @update:dropdown-open="dropdownOpen = $event"
    @placement-change="dropdownPlacement = $event"
    @apply-field="applyField"
  />

  <!-- VALUE INPUT -->
  <div class="ms-value-field">
    <!-- NUMBER -->
    <input
      v-if="fieldType === 'number'"
      type="number"
      class="ms-input ms-value"
      v-model="localRow.value"
      @input="syncRow"
      placeholder="Enter number"
    />

    <!-- BOOLEAN -->
    <select
      v-else-if="fieldType === 'boolean'"
      class="ms-input ms-value"
      v-model="localRow.value"
      @change="syncRow"
    >
      <option value="">Select...</option>
      <option value="true">True</option>
      <option value="false">False</option>
    </select>

    <!-- DATE -->
    <input
      v-else-if="fieldType === 'date'"
      type="date"
      class="ms-input ms-value"
      v-model="localRow.value"
      @input="syncRow"
    />

    <!-- DATETIME -->
    <input
      v-else-if="fieldType === 'datetime'"
      type="datetime-local"
      class="ms-input ms-value"
      v-model="localRow.value"
      @input="syncRow"
    />

    <!-- SINGLE USER -->
    <select
      v-else-if="fieldType === 'user'"
      class="ms-input ms-value"
      v-model="localRow.value"
      @change="syncRow"
    >
      <option value="">Select user…</option>
      <option v-for="u in projectUsers" :key="u.accountId" :value="u.accountId">
        {{ u.displayName }}
      </option>
    </select>

    <MultiUserPickerDropdown
      v-else-if="fieldType === 'multiuser' && projectUsers.length"
      :open="userDropdownOpen"
      :placement="userDropdownPlacement"
      :selected-value="localRow.value"
      :users="projectUsers"
      @update:open="userDropdownOpen = $event"
      @update:placement="userDropdownPlacement = $event"
      @toggle-user="onToggleMultiUser"
    />

    <!-- TEAM (atlassian-team) -->
    <select
      v-else-if="fieldType === 'team'"
      class="ms-input ms-value"
      v-model="localRow.value"
      @change="syncRow"
    >
      <option value="">Select team…</option>
      <option v-for="opt in teamOptions" :key="opt.value" :value="opt.value">
        {{ opt.label }}
      </option>
    </select>

    <!-- SPRINT (gh-sprint) -->
    <select
      v-else-if="fieldType === 'sprint'"
      class="ms-input ms-value"
      v-model="localRow.value"
      @change="syncRow"
    >
      <option value="">Select sprint…</option>
      <option v-for="opt in sprintOptions" :key="opt.value" :value="opt.value">
        {{ opt.label }}
      </option>
    </select>

    <!-- URL -->
    <input
      v-else-if="fieldType === 'url'"
      type="url"
      class="ms-input ms-value"
      v-model="localRow.value"
      placeholder="https://example.com"
      @input="syncRow"
    />

    <!-- OPTION -->
    <select
      v-else-if="fieldType === 'option'"
      class="ms-input ms-value"
      v-model="localRow.value"
      @change="syncRow"
    >
      <option value="">Select option…</option>
      <option v-for="v in allowedValues" :key="v" :value="v">{{ v }}</option>
    </select>

    <!-- LABELS -->
    <div v-else-if="fieldType === 'labels'" class="ms-multi-select">
      <div
        v-for="opt in allowedValues"
        :key="opt"
        :class="[
          'ms-chip',
          (localRow.value || '')
            .split(',')
            .map(s => s.trim())
            .includes(opt)
            ? 'selected'
            : '',
        ]"
        @click="toggleArrayValue(opt)"
      >
        {{ opt }}
      </div>
    </div>

    <!-- GENERIC ARRAY -->
    <div v-else-if="fieldType === 'array'" class="ms-multi-select">
      <div
        v-for="opt in allowedValues"
        :key="opt"
        :class="[
          'ms-chip',
          (localRow.value || '')
            .split(',')
            .map(s => s.trim())
            .includes(opt)
            ? 'selected'
            : '',
        ]"
        @click="toggleArrayValue(opt)"
      >
        {{ opt }}
      </div>
    </div>

    <StringFieldWithPreview
      v-else-if="fieldType === 'string'"
      :value="localRow.value"
      :preview-text="previewText"
      :preview-open="stringPreviewOpen"
      :action-tip-visible="actionTipVisible"
      :action-tip-x="actionTipX"
      :action-tip-y="actionTipY"
      :action-tip-id="actionTipId"
      :action-tip-text="actionTipText"
      @update:value="localRow.value = $event"
      @string-input="onStringInput"
      @open-insert="onOpenInsert"
      @toggle-preview="toggleStringPreview"
      @action-enter="onActionEnter"
      @action-move="onActionMove"
      @action-leave="onActionLeave"
    />

    <!-- DEFAULT TEXT -->
    <input
      v-else
      class="ms-input ms-value"
      v-model="localRow.value"
      @input="syncRow"
      placeholder="Enter value"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { FieldMeta } from '@/api/models/jirawebhook/FieldMeta';
import { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import {
  previewText as computePreviewText,
  toggleArrayValueValue,
} from '../utils/CustomFieldHelper';
import { useActionTooltip } from './composables/useActionTooltip';
import FieldSelectorDropdown from './fields/FieldSelectorDropdown.vue';
import MultiUserPickerDropdown from './fields/MultiUserPickerDropdown.vue';
import StringFieldWithPreview from './fields/StringFieldWithPreview.vue';
import './CustomFieldsPanel.css';
import { useJiraTeams } from '@/composables/useJiraTeams';
import { useJiraSprints } from '@/composables/useJiraSprints';

const props = defineProps<{
  connectionId: string;
  projectKey?: string;
  row: { _id: string; field: string; value: string };
  idx: number;
  fields: FieldMeta[];
  projectUsers: JiraUser[];
  renderedPreview?: string;
  initialLabel?: string;
  usedFieldKeys?: string[]; // keys already selected in other rows
}>();

const emit = defineEmits<{
  (
    e: 'row-change',
    payload: { idx: number; field: string; value: string; label: string; type: string }
  ): void;
  (e: 'open-insert', payload: { rowIndex: number }): void;
  (e: 'toggle-preview', payload: { rowIndex: number; value: string }): void;
}>();

const localRow = ref({ ...props.row });
// Watch _id and field changes only — replacing localRow on value-only changes
// invalidates fieldType (even if the type hasn't changed), which triggers
// watch(fieldType) → ensureDynamicOptionsLoaded → sprint/team API call on every
// keystroke. Split into two watchers to avoid this.
watch(
  () => [props.row._id, props.row.field] as const,
  ([newId, newField]) => {
    const idChanged = newId !== localRow.value._id;
    const fieldChanged = newField !== localRow.value.field;
    if (idChanged || fieldChanged) {
      localRow.value = { ...props.row };
      if (fieldChanged && !idChanged) {
        stringPreviewOpen.value = false;
        userDropdownOpen.value = false;
      }
    }
  }
);
// Update value in-place (e.g. JSON placeholder insert) without touching the
// field key, so fieldType stays stable and no API calls are triggered.
watch(
  () => props.row.value,
  newValue => {
    if (newValue !== localRow.value.value) {
      localRow.value.value = newValue;
    }
  }
);

const query = ref<string>(props.initialLabel || '');
const dropdownOpen = ref<boolean>(false);
const userDropdownOpen = ref<boolean>(false);
const stringPreviewOpen = ref<boolean>(false);
const dropdownPlacement = ref<'up' | 'down'>('down');
const userDropdownPlacement = ref<'up' | 'down'>('down');

const fieldMap = computed<Record<string, FieldMeta>>(() =>
  Object.fromEntries((props.fields || []).map(f => [f.key, f]))
);
const fieldType = computed(() => fieldMap.value[localRow.value.field]?.type || 'string');
const allowedValues = computed<string[]>(
  () => fieldMap.value[localRow.value.field]?.allowedValues || []
);
const projectUsers = computed<JiraUser[]>(() => props.projectUsers || []);

const usedKeys = computed<string[]>(() => props.usedFieldKeys || []);
const filteredFields = computed(() => {
  const q = (query.value || '').toLowerCase();
  const currentKey = localRow.value.field;
  return (
    (props.fields || [])
      .filter(f => !q || f.name.toLowerCase().includes(q) || f.key.toLowerCase().includes(q))
      // Exclude fields already used in other rows, but keep current row's field visible
      .filter(f => f.key === currentKey || !usedKeys.value.includes(f.key))
  );
});

const previewText = computed(() => computePreviewText(props.renderedPreview, localRow.value.value));
const {
  actionTipVisible,
  actionTipX,
  actionTipY,
  actionTipText,
  actionTipId,
  onActionEnter,
  onActionMove,
  onActionLeave,
} = useActionTooltip(props.row._id);

function syncRow() {
  const label = query.value || fieldMap.value[localRow.value.field]?.name || '';
  const type = fieldType.value;
  emit('row-change', {
    idx: props.idx,
    field: localRow.value.field,
    value: localRow.value.value,
    label,
    type,
  });
}

function applyField(f: FieldMeta) {
  const changed = localRow.value.field !== f.key;
  localRow.value.field = f.key;
  query.value = f.name;
  dropdownOpen.value = false;
  if (changed) {
    localRow.value.value = '';
    stringPreviewOpen.value = false;
    userDropdownOpen.value = false;
  }
  syncRow();
}

function onToggleMultiUser(accountId: string) {
  localRow.value.value = ((): string => {
    const selected = (localRow.value.value || '').split(',').filter(Boolean);
    return selected.includes(accountId)
      ? selected.filter(v => v !== accountId).join(',')
      : [...selected, accountId].join(',');
  })();
  syncRow();
}

function toggleArrayValue(opt: string) {
  localRow.value.value = toggleArrayValueValue(localRow.value.value, opt);
  syncRow();
}

function onOpenInsert() {
  emit('open-insert', { rowIndex: props.idx });
}
function toggleStringPreview() {
  if (!localRow.value.value.trim()) return;
  stringPreviewOpen.value = !stringPreviewOpen.value;
  emit('toggle-preview', { rowIndex: props.idx, value: localRow.value.value });
}
function onStringInput() {
  stringPreviewOpen.value = false;
  syncRow();
}

// Dynamic options for Team and Sprint fields
const jiraTeams = useJiraTeams({ connectionId: props.connectionId });
const jiraSprints = useJiraSprints({
  connectionId: props.connectionId,
  projectKey: props.projectKey,
  state: 'active,future',
});
const teamOptions = computed(
  () => jiraTeams.options.value as Array<{ value: string; label: string }>
);
const sprintOptions = computed(
  () => jiraSprints.options.value as Array<{ value: string; label: string }>
);

// Track which types have been fetched — prevents re-fetching every time fieldType
// is invalidated by unrelated reactive updates (e.g. mappingDataRef being replaced
// upstream causes props.fields to be a new array reference, invalidating fieldMap
// → fieldType → this watch fires repeatedly).
const loadedTypes = new Set<string>();
// Blocks concurrent calls to the same type while the first await is still in
// flight (race window between watch firing and loadedTypes.add() completing).
const inFlight = new Set<string>();

async function ensureDynamicOptionsLoaded() {
  const t = fieldType.value;
  if (loadedTypes.has(t) || inFlight.has(t)) return;
  inFlight.add(t);
  try {
    if (t === 'team') {
      await jiraTeams.search('');
      loadedTypes.add('team');
    } else if (t === 'sprint') {
      await jiraSprints.search('');
      loadedTypes.add('sprint');
    }
  } finally {
    inFlight.delete(t);
  }
}

// Load when field type switches into team/sprint and on mount
watch(
  fieldType,
  () => {
    ensureDynamicOptionsLoaded();
  },
  { immediate: true }
);

// Reload sprint options if projectKey changes while sprint is selected type
watch(
  () => props.projectKey,
  async () => {
    if (fieldType.value === 'sprint') {
      loadedTypes.delete('sprint');
      inFlight.delete('sprint');
      jiraSprints.clear(); // reset items so the useJiraSprints empty-query guard lets the refetch through
      await jiraSprints.search('');
      loadedTypes.add('sprint');
    }
  }
);
</script>

<style src="./CustomFieldsPanel.css"></style>
