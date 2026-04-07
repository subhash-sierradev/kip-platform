<template>
  <div class="ms-root">
    <div class="ms-panels">
      <IncomingJsonPanel :json-sample="props.jsonSample" />
      <section class="ms-right">
        <div class="ms-card">
          <div class="ms-card-header">
            <h2 class="ms-card-title">Map to Jira Fields</h2>
            <p class="ms-card-subtitle">Define how webhook data maps to Jira.</p>
          </div>
          <div class="ms-form">
            <CommonTooltip
              :visible="actionTipVisible"
              :x="actionTipX"
              :y="actionTipY"
              :id="actionTipId"
              :text="actionTipText"
            />
            <ProjectIssueAssigneeSelects
              :projects="projectsLocal"
              :issue-types="issueTypesLocal"
              :users="usersLocal"
              :show-parent-field="isSubtaskIssueType"
              :parent-field-options="parentFieldOptions"
              v-model:selectedProject="mappingDataLocal.selectedProject"
              v-model:selectedIssueType="mappingDataLocal.selectedIssueType"
              v-model:selectedParent="selectedParentField"
              v-model:selectedAssignee="mappingDataLocal.selectedAssignee"
              @project-change="onProjectChange"
              @issue-type-change="onIssueTypeChange"
              @parent-change="onParentChange"
              @parent-search="onParentSearch"
              @assignee-change="emitMapping"
            />
            <TemplateFieldEditor
              ref="summaryEditorRef"
              label="Summary"
              required
              target="summary"
              :model-value="mappingDataLocal.summary"
              :placeholder="'Enter issue summary template using {{field}} placeholders'"
              :show-preview="showSummaryPreview"
              :preview-text="renderedSummary"
              :action-tip-id="actionTipId"
              :on-action-enter="onActionEnter"
              :on-action-move="onActionMove"
              :on-action-leave="onActionLeave"
              textarea-class="ms-input ms-textarea ms-textarea--summary"
              preview-class="ms-inline-preview ms-inline-preview--summary"
              @update:modelValue="
                value => {
                  mappingDataLocal.summary = value;
                  emitMapping();
                }
              "
              @toggle-preview="toggleInlinePreview('summary')"
              @open-insert="openFieldModal"
            />
            <TemplateFieldEditor
              ref="descriptionEditorRef"
              label="Description"
              target="description"
              :model-value="mappingDataLocal.descriptionFieldMapping"
              :placeholder="'Enter issue description template using {{field}} placeholders'"
              :show-preview="showDescriptionPreview"
              :preview-text="renderedDescription"
              :action-tip-id="actionTipId"
              :on-action-enter="onActionEnter"
              :on-action-move="onActionMove"
              :on-action-leave="onActionLeave"
              preview-class="ms-inline-preview ms-inline-preview--description"
              @update:modelValue="
                value => {
                  mappingDataLocal.descriptionFieldMapping = value;
                  emitMapping();
                }
              "
              @toggle-preview="toggleInlinePreview('description')"
              @open-insert="openFieldModal"
            />
            <CustomFieldsPanel
              ref="customFieldsRef"
              :connectionId="props.connectionId ?? ''"
              :projectKey="mappingDataLocal.selectedProject"
              :issueTypeId="mappingDataLocal.selectedIssueType"
              :isSubtaskIssueType="isSubtaskIssueType"
              :rows="rowsLocal"
              :projectUsers="usersLocal"
              :renderedPreviews="customFieldPreviews"
              @update:rows="onRowsUpdate"
              @valid-change="onCustomFieldsValidChange"
              @open-insert="onCustomFieldInsert"
              @toggle-preview="onTogglePreview"
            />
          </div>
        </div>
      </section>
    </div>
    <InsertFieldModal
      :open="showFieldModal"
      :json-sample="props.jsonSample"
      @select="insertField"
      @close="closeFieldModal"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, toRef } from 'vue';
import CustomFieldsPanel from './CustomFieldsPanel.vue';
import IncomingJsonPanel from './IncomingJsonPanel.vue';
import ProjectIssueAssigneeSelects from './ProjectIssueAssigneeSelects.vue';
import TemplateFieldEditor from './TemplateFieldEditor.vue';
import InsertFieldModal from './InsertFieldModal.vue';
import CommonTooltip from '@/components/common/Tooltip.vue';
import type { CustomFieldMapping } from '@/api/services/JiraWebhookService';
import { useJiraProjectMetadata } from '../utils/useJiraProjectMetadata';
import { useParentIssueLookup } from '../utils/useParentIssueLookup';
import { useSubtaskParentMapping } from '../utils/useSubtaskParentMapping';
import {
  mergeRowsIntoCustomFields,
  toNonParentRows,
  type MappingRow,
} from '../utils/mappingStepFieldUtils';
import { JiraConnectionRequest } from '@/api/models/jirawebhook/JiraConnectionRequest';
import { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import { MappingData } from '@/api/models/jirawebhook/MappingData';
import { resolveTemplateWithJson } from '../utils/templatePathResolver';

const props = defineProps<{
  jsonSample: string;
  jiraConnectionRequest?: JiraConnectionRequest;
  connectionId?: string;
  mappingData: MappingData;
  projects?: JiraProject[];
  issueTypes?: JiraIssueType[];
  users?: JiraUser[];
  editMode?: boolean;
  cloneMode?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:mappingData', value: MappingData): void;
  (e: 'validation-change', valid: boolean): void;
  (e: 'projects-change', list: JiraProject[]): void;
  (e: 'issueTypes-change', list: JiraIssueType[]): void;
  (e: 'users-change', list: JiraUser[]): void;
}>();

const activeCustomRowIndex = ref<number | null>(null);
const customFieldPreviews = ref<string[]>([]);
const customFieldsRef = ref<any>(null);

type TemplateFieldEditorExpose = { insertPlaceholder: (placeholder: string) => void };
const summaryEditorRef = ref<TemplateFieldEditorExpose | null>(null);
const descriptionEditorRef = ref<TemplateFieldEditorExpose | null>(null);
const actionTipVisible = ref(false);
const actionTipX = ref(0);
const actionTipY = ref(0);
const actionTipText = ref('');
const actionTipId = 'mapping-actions-tooltip';
const CURSOR_GAP = 12;

function onActionEnter(text: string, e: MouseEvent) {
  actionTipText.value = text;
  actionTipX.value = e.clientX + CURSOR_GAP;
  actionTipY.value = e.clientY + CURSOR_GAP;
  actionTipVisible.value = true;
}
function onActionMove(e: MouseEvent) {
  actionTipX.value = e.clientX + CURSOR_GAP;
  actionTipY.value = e.clientY + CURSOR_GAP;
}
function onActionLeave() {
  actionTipVisible.value = false;
}

function onTogglePreview({ rowIndex, value }: { rowIndex: number; value: string }) {
  customFieldPreviews.value[rowIndex] = resolveTemplateWithJson(value || '', props.jsonSample);
}

function onCustomFieldInsert(payload: { rowIndex: number }) {
  activeCustomRowIndex.value = payload.rowIndex;
  modalTarget.value = 'custom';
  showFieldModal.value = true;
}
const mappingDataLocal = ref<MappingData>({ ...props.mappingData });
const { projectsLocal, issueTypesLocal, usersLocal } = useJiraProjectMetadata({
  mappingData: mappingDataLocal,
  projects: toRef(props, 'projects'),
  issueTypes: toRef(props, 'issueTypes'),
  users: toRef(props, 'users'),
  connectionId: toRef(props, 'connectionId'),
  jiraConnectionRequest: toRef(props, 'jiraConnectionRequest'),
  emitProjectsChange: (list: JiraProject[]) => emit('projects-change', list),
  emitIssueTypesChange: (list: JiraIssueType[]) => emit('issueTypes-change', list),
  emitUsersChange: (list: JiraUser[]) => emit('users-change', list),
});
const customFieldsLocal = ref<CustomFieldMapping[]>(props.mappingData.customFields || []);
const customFieldsValid = ref(true);
const {
  parentIssues: parentIssuesLocal,
  fetchParentIssuesForProject,
  searchParentIssuesByQuery,
  resetParentIssues,
} = useParentIssueLookup();
const {
  selectedParentField,
  isSubtaskIssueType,
  parentFieldOptions,
  upsertParentCustomField,
  removeParentCustomField,
  syncSelectedParentFromCustomFields,
} = useSubtaskParentMapping({
  issueTypes: issueTypesLocal,
  selectedIssueType: computed(() => mappingDataLocal.value.selectedIssueType),
  customFields: customFieldsLocal,
  parentIssues: parentIssuesLocal,
});
const rowsLocal = ref<MappingRow[]>(toNonParentRows(customFieldsLocal.value));

function hasKnownSelectedIssueType(): boolean {
  const selectedIssueType = mappingDataLocal.value.selectedIssueType;
  if (!selectedIssueType) {
    return false;
  }

  return issueTypesLocal.value.some(issueType => issueType.id === selectedIssueType);
}
function onRowsUpdate(nextRows: MappingRow[]) {
  const baselineFields = [
    ...(props.mappingData.customFields || []),
    ...(customFieldsLocal.value || []),
  ];
  customFieldsLocal.value = mergeRowsIntoCustomFields(nextRows, baselineFields);
  syncSelectedParentFromCustomFields();
  emitMapping();
}
watch(
  () => props.mappingData.customFields,
  val => {
    customFieldsLocal.value = Array.isArray(val) ? [...val] : [];
    rowsLocal.value = toNonParentRows(customFieldsLocal.value);
    syncSelectedParentFromCustomFields();
  },
  { deep: true }
);

watch(
  () => props.mappingData,
  val => {
    mappingDataLocal.value = { ...val };
  }
);

watch(
  () => [mappingDataLocal.value.selectedIssueType, issueTypesLocal.value],
  () => {
    if (!isSubtaskIssueType.value && hasKnownSelectedIssueType()) {
      selectedParentField.value = '';
      if (removeParentCustomField()) {
        emitMapping();
      }
    } else {
      syncSelectedParentFromCustomFields();
    }
    validate();
  },
  { deep: true }
);

watch(
  () => [props.connectionId, mappingDataLocal.value.selectedProject, isSubtaskIssueType.value],
  async () => {
    await fetchParentIssuesForProject(
      props.connectionId,
      mappingDataLocal.value.selectedProject,
      isSubtaskIssueType.value
    );
  },
  { immediate: true }
);
function emitMapping() {
  emit('update:mappingData', { ...mappingDataLocal.value, customFields: customFieldsLocal.value });
  validate();
}

function validate() {
  const v =
    !!mappingDataLocal.value.selectedProject &&
    !!mappingDataLocal.value.selectedIssueType &&
    !!mappingDataLocal.value.summary.trim() &&
    (!isSubtaskIssueType.value || !!selectedParentField.value.trim()) &&
    customFieldsValid.value;

  emit('validation-change', v);
}
function onCustomFieldsValidChange(valid: boolean) {
  customFieldsValid.value = valid;
  validate();
}
const showFieldModal = ref(false);
const modalTarget = ref<'summary' | 'description' | 'custom'>('summary');

function openFieldModal(
  target: 'summary' | 'description' | 'custom',
  payload?: { rowIndex?: number; value?: string }
) {
  modalTarget.value = target;

  if (target === 'custom') {
    activeCustomRowIndex.value = payload?.rowIndex ?? null;
  }

  showFieldModal.value = true;
}

function closeFieldModal() {
  showFieldModal.value = false;
}

function insertField(field: string) {
  const placeholder = field;

  if (modalTarget.value === 'summary') {
    summaryEditorRef.value?.insertPlaceholder(placeholder);
  } else if (modalTarget.value === 'description') {
    descriptionEditorRef.value?.insertPlaceholder(placeholder);
  } else if (modalTarget.value === 'custom' && activeCustomRowIndex.value !== null) {
    customFieldsRef.value?.insertJsonPlaceholder(placeholder);
  }

  emitMapping();
  closeFieldModal();
}
function onProjectChange() {
  mappingDataLocal.value.selectedIssueType = '';
  mappingDataLocal.value.selectedAssignee = '';
  resetParentIssues();
  selectedParentField.value = '';
  removeParentCustomField();
  emitMapping();
}

function onIssueTypeChange() {
  if (!isSubtaskIssueType.value && hasKnownSelectedIssueType()) {
    resetParentIssues();
    selectedParentField.value = '';
    removeParentCustomField();
  }
  emitMapping();
}

async function onParentSearch(query: string) {
  await searchParentIssuesByQuery(
    query,
    props.connectionId,
    mappingDataLocal.value.selectedProject,
    isSubtaskIssueType.value
  );
}

function onParentChange() {
  upsertParentCustomField(selectedParentField.value);
  emitMapping();
}
validate();
const showSummaryPreview = ref(false);
const showDescriptionPreview = ref(false);

function toggleInlinePreview(target: 'summary' | 'description') {
  target === 'summary'
    ? (showSummaryPreview.value = !showSummaryPreview.value)
    : (showDescriptionPreview.value = !showDescriptionPreview.value);
}

const renderedSummary = computed(() =>
  resolveTemplateWithJson(mappingDataLocal.value.summary, props.jsonSample)
);
const renderedDescription = computed(() =>
  resolveTemplateWithJson(mappingDataLocal.value.descriptionFieldMapping, props.jsonSample)
);
</script>
<style src="./MappingStep.css" scoped></style>
