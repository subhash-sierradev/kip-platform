<!-- PreviewStep.vue -->
<template>
  <div class="ps-root">
    <p class="ps-subtitle">
      Review your webhook configuration and see how the Jira ticket will be created.
    </p>

    <div class="ps-grid">
      <!-- WEBHOOK CONFIGURATION -->
      <div class="ps-card">
        <h3 class="ps-card-title">Webhook Configuration</h3>

        <div class="ps-row">
          <div class="ps-label">Integration Name:</div>
          <div class="ps-value">{{ integrationName }}</div>
          <div class="ps-label-error-message" v-if="props.isDuplicateName">
            Jira webhook name already exists
          </div>
        </div>

        <div class="ps-row ps-description-row">
          <div class="ps-label">Description:</div>
          <div class="ps-desc-box">
            <pre :class="{ 'ps-placeholder-text': !description }">{{
              description || 'No description provided'
            }}</pre>
          </div>
        </div>
      </div>

      <!-- JIRA FIELD MAPPING -->
      <div class="ps-card">
        <h3 class="ps-card-title">Jira Field Mapping</h3>

        <div class="ps-review-grid">
          <div class="ps-review-item">
            <span class="ps-review-label">Project:</span>
            <span class="ps-review-value">{{ projectName }}</span>
          </div>

          <div class="ps-review-item">
            <span class="ps-review-label">Issue Type:</span>
            <span class="ps-review-value">{{ issueTypeName }}</span>
          </div>

          <div class="ps-review-item" v-if="isSubtaskIssueType">
            <span class="ps-review-label">Subtask's Parent:</span>
            <span class="ps-review-value">{{ parentName }}</span>
          </div>

          <div v-if="hasAssignee" class="ps-review-item">
            <span class="ps-review-label">Assignee:</span>
            <span class="ps-review-value">{{ assigneeName }}</span>
          </div>

          <div class="ps-review-item ps-preview-item">
            <div class="ps-preview-header">
              <span class="ps-review-label">Summary:</span>
              <button
                class="ps-eye-icon"
                @click="toggleSummary"
                aria-label="Toggle summary preview"
              >
                <svg
                  v-if="!showResolvedSummary"
                  class="ps-eye-svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                <svg
                  v-else
                  class="ps-eye-svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
                  ></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </button>
            </div>
            <div class="ps-preview-box">
              <pre>{{ previewSummary }}</pre>
            </div>
          </div>

          <div
            v-if="mappingData.descriptionFieldMapping && mappingData.descriptionFieldMapping.trim()"
            class="ps-review-item ps-preview-item"
          >
            <div class="ps-preview-header">
              <span class="ps-review-label">Description:</span>
              <button
                class="ps-eye-icon"
                @click="toggleDescription"
                aria-label="Toggle description preview"
              >
                <svg
                  v-if="!showResolvedDescription"
                  class="ps-eye-svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                <svg
                  v-else
                  class="ps-eye-svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
                  ></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </button>
            </div>
            <div class="ps-preview-box">
              <pre>{{ previewDescription }}</pre>
            </div>
          </div>
        </div>
      </div>

      <!-- CUSTOM FIELDS -->
      <div class="ps-card" v-if="nonParentCustomFields.length > 0">
        <h3 class="ps-card-title">Custom Fields</h3>

        <div class="ps-review-grid">
          <div v-for="cf in nonParentCustomFields" :key="cf.jiraFieldKey" class="ps-review-item">
            <span class="ps-review-label">{{ getFieldLabel(cf) }}</span>
            <span class="ps-review-value">
              <template v-if="Array.isArray(formatCustomValue(cf))">
                <span
                  v-for="v in formatCustomValue(cf)"
                  :key="cf.jiraFieldKey + ':' + v"
                  class="ps-custom-value"
                  >{{ v }}</span
                >
              </template>
              <template v-else>
                <span class="ps-custom-value">{{ formatCustomValue(cf) }}</span>
              </template>
            </span>
          </div>
        </div>
      </div>

      <!-- SAMPLE PAYLOAD -->
      <!-- SAMPLE WEBHOOK PAYLOAD (Accordion) -->
      <div class="ps-card">
        <div class="ps-accordion-header" :class="{ expanded: showJson }" @click="toggleJson">
          <h3 class="ps-card-title">Sample Webhook Payload</h3>

          <svg class="ps-accordion-arrow" :class="{ open: showJson }" viewBox="0 0 24 24">
            <path d="M7 10l5 5 5-5z"></path>
          </svg>
        </div>

        <transition name="fade">
          <div v-if="showJson" class="ps-json-box">
            <pre>{{ formattedJson }}</pre>
          </div>
        </transition>
      </div>

      <!-- ALWAYS VISIBLE INFO BOX (SEPARATE CARD) -->
      <div class="ps-card">
        <h3 class="ps-steps-title">Integration Workflow</h3>

        <ol class="ps-steps-list">
          <li>Receive webhook payload from Kaseware application.</li>
          <li>Transform data using configured field mappings.</li>
          <li>
            Create new Jira ticket in <b>{{ projectName }}</b> project.
          </li>
          <li v-if="hasAssignee">
            Assign ticket to <b>{{ assigneeName }}</b
            >.
          </li>
          <li>Log execution results and provide status confirmation.</li>
        </ol>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { CustomFieldMapping } from '@/api/services/JiraWebhookService';
import {
  formatCustomValue as formatCustomValueWithMaps,
  formatJsonSample,
  formatParentName,
  getFieldLabel as getCustomFieldLabel,
  toSprintMap,
  toUserMap,
} from '../utils/previewStepFormatters';
import './PreviewStep.css';
import { parseJsonSafe, resolveTemplateWithObject } from '../utils/templatePathResolver';

// -------- PROPS --------
const props = defineProps<{
  integrationName: string;
  description?: string;
  isDuplicateName: boolean;
  jsonSample: string;

  mappingData: {
    selectedProject: string;
    selectedIssueType: string;
    selectedAssignee: string;
    summary: string;
    descriptionFieldMapping: string;
    customFields?: CustomFieldMapping[];
  };

  // In your actual code these are optional, so we handle that below
  projects?: Array<{ key: string; name: string }>;
  issueTypes?: Array<{ id: string; name: string; subtask?: boolean }>;
  users?: Array<{ accountId: string; displayName: string }>;
  sprints?: Array<{ value: string; label: string }>;
  selectedParentLabel?: string;
}>();

// ---------- TEMPLATE RESOLVER ----------
function resolveTemplate(template: string): string {
  const json = parseJsonSafe(props.jsonSample);
  return resolveTemplateWithObject(template, json, { preserveUnresolved: true });
}

// ---------- SUMMARY / DESCRIPTION PREVIEW ----------
const showResolvedSummary = ref(false);
const showResolvedDescription = ref(false);

const previewSummary = computed(() =>
  showResolvedSummary.value ? resolveTemplate(props.mappingData.summary) : props.mappingData.summary
);

const previewDescription = computed(() =>
  showResolvedDescription.value
    ? resolveTemplate(props.mappingData.descriptionFieldMapping)
    : props.mappingData.descriptionFieldMapping
);

function toggleSummary() {
  showResolvedSummary.value = !showResolvedSummary.value;
}
function toggleDescription() {
  showResolvedDescription.value = !showResolvedDescription.value;
}

// ---------- JSON SAMPLE COLLAPSE ----------
const showJson = ref(false);
function toggleJson() {
  showJson.value = !showJson.value;
}

const formattedJson = computed(() => {
  return formatJsonSample(props.jsonSample);
});

const userMap = computed(() => {
  return toUserMap(props.users ?? []);
});

const sprintMap = computed(() => {
  return toSprintMap(props.sprints ?? []);
});

function formatCustomValue(cf: CustomFieldMapping): string | string[] {
  return formatCustomValueWithMaps(cf, props.jsonSample, userMap.value, sprintMap.value);
}

const nonParentCustomFields = computed(() => {
  return (props.mappingData.customFields ?? []).filter(cf => {
    return (
      String(cf.jiraFieldKey || '')
        .trim()
        .toLowerCase() !== 'parent'
    );
  });
});

// ---------- PROJECT / ISSUE / USER LABELS ----------
const projectName = computed(() => {
  // props.projects might be undefined -> fallback to []
  const list = props.projects ?? [];
  const p = list.find(p => p.key === props.mappingData.selectedProject);
  return p?.name || 'Not Selected';
});

const selectedIssueType = computed(() => {
  const list = props.issueTypes ?? [];
  return list.find(t => t.id === props.mappingData.selectedIssueType);
});

const issueTypeName = computed(() => {
  return selectedIssueType.value?.name || 'Not Selected';
});

const isSubtaskIssueType = computed(() => {
  if (typeof selectedIssueType.value?.subtask === 'boolean') {
    return selectedIssueType.value.subtask;
  }

  const normalized = issueTypeName.value
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, '');
  return normalized === 'subtask';
});

const parentName = computed(() => {
  return formatParentName(
    props.mappingData.customFields ?? [],
    props.jsonSample,
    props.selectedParentLabel
  );
});

const assigneeName = computed(() => {
  const list = props.users ?? [];
  const u = list.find(u => u.accountId === props.mappingData.selectedAssignee);
  return u?.displayName || 'Unassigned';
});

const hasAssignee = computed(() => {
  return props.mappingData.selectedAssignee && props.mappingData.selectedAssignee.trim() !== '';
});

function getFieldLabel(cf: CustomFieldMapping): string {
  return getCustomFieldLabel(cf);
}
</script>
