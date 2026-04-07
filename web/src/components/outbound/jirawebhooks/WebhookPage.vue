<template>
  <div class="webhook-page">
    <h2>Jira Webhooks</h2>
    <div class="toolbar-row">
      <button class="btn primary" style="pointer-events: auto" @click.prevent.stop="openWizard()">
        Create Webhook
      </button>
    </div>
    <div class="webhook-list">
      <div v-for="webhook in webhooks" :key="webhook.id" class="webhook-card">
        <!-- Header row with icon, title, created date, status chip, and menu -->
        <div class="card-top-row">
          <div class="icon-box">
            <WebhookIcon size="medium" />
          </div>
          <div class="title-box">
            <div class="title-text">{{ webhook.name }}</div>
            <div class="created-text">Created: {{ formatMetadataDate(webhook.createdDate) }}</div>
          </div>
          <div class="status-box">
            <span
              class="status-chip"
              :class="webhook.isEnabled ? 'status-active' : 'status-disabled'"
              >{{ webhook.isEnabled ? 'Enabled' : 'Disabled' }}</span
            >
          </div>
          <!-- Three-dot menu (future: implement actions) -->
          <div class="options-menu">
            <button class="options-button" aria-label="Options">
              <svg viewBox="0 0 24 24" class="options-icon">
                <circle cx="12" cy="5" r="2" fill="currentColor" />
                <circle cx="12" cy="12" r="2" fill="currentColor" />
                <circle cx="12" cy="19" r="2" fill="currentColor" />
              </svg>
            </button>
          </div>
        </div>
        <!-- Chips & last trigger info row -->
        <div class="chips-row">
          <span v-if="getProjectLabel(webhook)" class="project-chip">{{
            getProjectLabel(webhook)
          }}</span>
          <span class="trigger-label">Last Trigger:</span>
          <span
            class="trigger-chip"
            :class="'trigger-' + (webhook.lastEventHistory?.status?.toLowerCase() || 'pending')"
            >{{ webhook.lastEventHistory?.status || 'PENDING' }}</span
          >
          <span v-if="webhook.lastEventHistory?.triggeredAt" class="trigger-date">{{
            formatMetadataDate(webhook.lastEventHistory.triggeredAt)
          }}</span>
          <span v-else class="trigger-date">No triggers yet</span>
        </div>
        <!-- Issue Type / Assignee Row -->
        <div class="info-row">
          <div class="info-cell">
            <span class="label">Issue Type:</span>
            <span class="value">{{ getIssueTypeLabel(webhook) }}</span>
          </div>
          <div class="info-cell align-right">
            <span class="label">Assignee:</span>
            <span class="value">{{ getAssignee(webhook) }}</span>
          </div>
        </div>
        <!-- URL Row -->
        <div class="url-row">
          <span class="url-label">Webhook URL:</span>
          <span class="url-value" :title="webhook.webhookUrl">{{ webhook.webhookUrl }}</span>
          <button
            class="copy-icon-btn"
            @click.stop="copyToClipboard(webhook.webhookUrl)"
            aria-label="Copy Webhook URL"
          >
            <svg viewBox="0 0 24 24" class="copy-icon">
              <path
                fill="currentColor"
                d="M16 1H4c-1.1 0-2 .9-2 2v12h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
              />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <JiraWebhookWizard v-if="wizardOpen" :open="wizardOpen" @close="wizardOpen = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { JiraWebhookService } from '@/api/services/JiraWebhookService';
import type { JiraWebhook } from '@/types/JiraWebhook';
import WebhookIcon from '@/components/common/WebhookIcon.vue';
import JiraWebhookWizard from './wizard/JiraWebhookWizard.vue';
import { formatMetadataDate } from '@/utils/dateUtils';

const webhooks = ref<JiraWebhook[]>([]);
const wizardOpen = ref(false);

const fetchWebhooks = async () => {
  try {
    const data = await JiraWebhookService.listJiraWebhooks();
    webhooks.value = Array.isArray(data) ? data : [];
  } catch (e) {
    console.error('Failed to load Jira webhooks:', e);
  }
};

onMounted(fetchWebhooks);

function openWizard() {
  wizardOpen.value = true;
}

function getProjectLabel(webhook: JiraWebhook): string {
  const mapping = webhook.jiraFieldMappings?.find(
    (f: any) =>
      (typeof f.jiraFieldId === 'string' && f.jiraFieldId === 'project') ||
      (typeof f.displayLabel === 'string' && f.displayLabel.toLowerCase().includes('project'))
  );
  return typeof mapping?.displayLabel === 'string' ? mapping.displayLabel : '';
}

function getIssueTypeLabel(webhook: JiraWebhook): string {
  const mapping = webhook.jiraFieldMappings?.find(
    (f: any) =>
      (typeof f.jiraFieldId === 'string' && f.jiraFieldId === 'issuetype') ||
      (typeof f.displayLabel === 'string' && f.displayLabel.toLowerCase().includes('issue type'))
  );
  return typeof mapping?.displayLabel === 'string' ? mapping.displayLabel : '';
}

function getAssignee(webhook: JiraWebhook): string {
  const assigneeMapping = webhook.jiraFieldMappings?.find(
    (f: any) =>
      (typeof f.jiraFieldId === 'string' && f.jiraFieldId === 'assignee') ||
      (typeof f.jiraFieldName === 'string' && f.jiraFieldName.toLowerCase() === 'assignee')
  );
  return typeof assigneeMapping?.displayLabel === 'string'
    ? assigneeMapping.displayLabel
    : typeof assigneeMapping?.defaultValue === 'string'
      ? assigneeMapping.defaultValue
      : 'Unassigned';
}

function copyToClipboard(text: string): void {
  navigator.clipboard.writeText(text).catch(err => console.error('Failed to copy text:', err));
}
</script>

<style>
@import './WebhookPage.css';
.toolbar-row {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.btn {
  padding: 8px 16px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}
.btn.primary {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}
.webhook-card {
  background: #fff;
  border: 1px solid #d9dce0;
  border-radius: 4px;
  padding: 0.75rem 0.9rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.07);
}
.status-chip {
  font-weight: bold;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}
.status-active {
  background-color: #4caf50;
  color: #fff;
}
.status-disabled {
  background-color: #f44336;
  color: #fff;
}
.trigger-chip {
  font-weight: bold;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}
.trigger-success {
  background-color: #4caf50;
  color: #fff;
}
.trigger-pending {
  background-color: #ff9800;
  color: #fff;
}
.trigger-failure {
  background-color: #f44336;
  color: #fff;
}
</style>
