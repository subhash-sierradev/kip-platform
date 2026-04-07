<template>
  <div class="jira-field-mapping-tab">
    <div>
      <GenericDataGrid
        ref="dataGridRef"
        :data="fieldMappings"
        :columns="gridColumns"
        :page-size="10"
        :enable-export="false"
        :enable-clear-filters="true"
      >
        <template #fieldNameTemplate="{ data }">
          <div class="field-name-cell">
            <span class="field-name">{{ data.fieldName }}</span>
          </div>
        </template>
        <template #typeTemplate="{ data }">
          <span class="type-text">{{ capitalizeFirst(data.type) }}</span>
        </template>
        <template #mappedValueTemplate="{ data }">
          <div class="mapped-value-cell">
            <span
              class="mapped-value"
              :class="{
                'mapped-token':
                  typeof data.mappedValueDisplay === 'string' &&
                  data.mappedValueDisplay.trim().startsWith('{{') &&
                  data.mappedValueDisplay.trim().endsWith('}}'),
              }"
            >
              {{ data.mappedValueDisplay }}
            </span>
          </div>
        </template>
        <template #requiredTemplate="{ data }">
          <StatusChipForDataTable
            :status="data.required ? 'REQUIRED' : 'OPTIONAL'"
            :label="data.required ? 'Yes' : 'No'"
          />
        </template>
      </GenericDataGrid>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, type PropType } from 'vue';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';

import type { JiraWebhookDetail, JiraFieldMapping } from '@/api/services/JiraWebhookService';
import { capitalizeFirst } from '@/utils/stringFormatUtils';
import { formatDisplayDate, formatMetadataDate } from '@/utils/dateUtils';

interface FieldMappingDisplay {
  id: string;
  fieldName: string;
  type: string;
  mappedValue: string;
  isParent: boolean;
  isSprint: boolean;
  required: boolean;
  description?: string;
  createdDate: Date;
  lastModified: Date;
}

const props = defineProps({
  webhookData: {
    type: Object as PropType<JiraWebhookDetail | null>,
    default: null,
  },
  webhookId: {
    type: String,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  // Optional users list to map accountId -> displayName (ref: PreviewStep.vue)
  users: {
    type: Array as PropType<Array<{ accountId: string; displayName: string }>>,
    default: () => [],
  },
  usersLoading: {
    type: Boolean,
    default: false,
  },
  sprints: {
    type: Array as PropType<Array<{ value: string; label: string }>>,
    default: () => [],
  },
  sprintsLoading: {
    type: Boolean,
    default: false,
  },
});

// Build a lookup map for user IDs to display names, if provided
const userMap = computed(() => {
  const map = new Map<string, string>();
  (props.users ?? []).forEach(u => {
    map.set(u.accountId, u.displayName);
  });
  return map;
});

const sprintMap = computed(() => {
  const map = new Map<string, string>();
  (props.sprints ?? []).forEach(s => {
    map.set(String(s.value), s.label);
  });
  return map;
});

function isTemplatePreferredType(typeUpper: string): boolean {
  return (
    typeUpper === 'DATE' ||
    typeUpper === 'DATETIME' ||
    typeUpper === 'USER' ||
    typeUpper === 'MULTIUSER'
  );
}

type MappingKey = keyof Pick<JiraFieldMapping, 'template' | 'displayLabel' | 'defaultValue'>;
function pickFirst(mapping: JiraFieldMapping, keys: MappingKey[], fallback: string): string {
  for (const key of keys) {
    const val = mapping[key];
    if (typeof val === 'string' && val.length > 0) {
      return val;
    }
  }
  return fallback;
}

const TEMPLATE_ONLY_TYPES = new Set<string>(['STRING', 'NUMBER', 'FLOAT', 'INTEGER']);
function isTemplateOnlyType(typeUpper: string): boolean {
  return TEMPLATE_ONLY_TYPES.has(typeUpper);
}

function chooseMappedValue(mapping: JiraFieldMapping, typeUpper: string): string {
  const noMapping = 'No mapping';
  const jiraFieldId = String(mapping.jiraFieldId ?? '')
    .trim()
    .toLowerCase();

  if (jiraFieldId === 'parent') {
    return pickFirst(mapping, ['template', 'displayLabel', 'defaultValue'], noMapping);
  }

  if (isTemplateOnlyType(typeUpper)) {
    return pickFirst(mapping, ['template', 'displayLabel', 'defaultValue'], noMapping);
  }
  const keys: MappingKey[] = isTemplatePreferredType(typeUpper)
    ? ['template', 'displayLabel', 'defaultValue']
    : ['displayLabel', 'template', 'defaultValue'];
  return pickFirst(mapping, keys, noMapping);
}

function isSprintField(mapping: JiraFieldMapping, typeUpper: string): boolean {
  const metadataType = String(
    mapping.metadata?.fieldType ?? mapping.metadata?.type ?? ''
  ).toLowerCase();
  if (metadataType === 'sprint') {
    return true;
  }

  if (typeUpper !== 'NUMBER' && typeUpper !== 'STRING') {
    return false;
  }

  const fieldName = String(mapping.jiraFieldName ?? '').toLowerCase();
  return fieldName === 'sprint';
}

const fieldMappings = computed(() => {
  const list = props.webhookData?.jiraFieldMappings || [];

  return list.map((mapping, index) => {
    const typeUpper = (mapping.dataType || 'STRING').toUpperCase();
    const chosenValue = chooseMappedValue(mapping, typeUpper);
    const isParent =
      String(mapping.jiraFieldId ?? '')
        .trim()
        .toLowerCase() === 'parent';

    return {
      id: mapping.id || `mapping-${index}`,
      fieldName: mapping.jiraFieldName || mapping.jiraFieldId || 'Unknown Field',
      type: typeUpper,
      mappedValue: chosenValue,
      isParent,
      isSprint: isSprintField(mapping, typeUpper),
      mappedValueDisplay: getDisplayMappedValue({
        mappedValue: chosenValue,
        type: typeUpper,
        isParent,
        isSprint: isSprintField(mapping, typeUpper),
      } as any),
      required: mapping.required || false,
      description: `Field mapping for ${mapping.jiraFieldName || mapping.jiraFieldId}`,
      createdDate: new Date(),
      lastModified: new Date(),
    };
  });
});

const gridColumns = [
  {
    dataField: 'fieldName',
    caption: 'Field Name',
    template: 'fieldNameTemplate',
    width: 200,
    allowFiltering: true,
    allowSorting: true,
  },
  {
    dataField: 'type',
    caption: 'Type',
    template: 'typeTemplate',
    width: 120,
    allowFiltering: true,
    allowSorting: true,
  },
  {
    dataField: 'mappedValueDisplay',
    caption: 'Mapped Value',
    template: 'mappedValueTemplate',
    minWidth: 250,
    allowFiltering: true,
    allowSorting: true,
  },
  {
    dataField: 'required',
    caption: 'Required',
    template: 'requiredTemplate',
    width: 150,
    allowFiltering: true,
    allowSorting: true,
  },
];

function formatDateSmart(raw: string): string {
  try {
    const date = parseDate(raw);
    if (!date) return raw;

    return hasTime(raw) ? formatMetadataDate(date) : formatDisplayDate(date);
  } catch {
    return raw;
  }
}

function parseDate(raw: string): Date | null {
  const d = new Date(raw);
  return Number.isFinite(d.getTime()) ? d : null;
}

function hasTime(raw: string): boolean {
  return raw.includes('T') || /\d{2}:\d{2}/.test(raw);
}

function formatUsers(raw: string): string {
  const ids = raw
    .split(',')
    .map(v => v.trim())
    .filter(Boolean);

  if (ids.length === 0) return '';
  if (props.usersLoading && userMap.value.size === 0) {
    return 'Loading users...';
  }

  return ids.map(id => userMap.value.get(id) ?? id).join(', ');
}

function formatSprints(raw: string): string {
  const ids = raw
    .split(',')
    .map(v => v.trim())
    .filter(Boolean);

  if (ids.length === 0) return '';
  if (props.sprintsLoading && sprintMap.value.size === 0) {
    return 'Loading sprints...';
  }

  return ids.map(id => sprintMap.value.get(id) ?? id).join(', ');
}

function formatParentKey(raw: string): string {
  const value = raw.trim();
  if (!value) return value;

  if (value.startsWith('{{') && value.endsWith('}}')) {
    return value;
  }

  try {
    const parsed = JSON.parse(value) as { key?: unknown };
    const key = parsed?.key;
    if (typeof key === 'string' && key.trim().length > 0) {
      return key.trim();
    }
  } catch {
    // Fall through to raw value when not valid JSON.
  }

  return value;
}

function getDisplayMappedValue(row: FieldMappingDisplay): string {
  const raw = String(row.mappedValue ?? '');
  if (!raw) return raw;

  if (row.isParent) {
    return formatParentKey(raw);
  }

  // Keep JSON templates untouched
  if (raw.includes('{{')) return raw;

  if (row.isSprint) {
    return formatSprints(raw);
  }

  const type = (row.type || '').toUpperCase();

  if (type === 'DATE' || type === 'DATETIME') {
    return formatDateSmart(raw);
  }

  if (type === 'USER' || type === 'MULTIUSER') {
    return formatUsers(raw);
  }

  return raw;
}
</script>

<style src="./JiraFieldMappingTab.css" scoped></style>
