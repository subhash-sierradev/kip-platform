<template>
  <div v-if="loading" class="webhook-grid">
    <div class="webhook-card" v-for="n in 6" :key="'skeleton-' + n">
      <div class="card-title skeleton skeleton-text" style="width: 60%"></div>
      <div class="card-desc skeleton skeleton-text" style="width: 90%"></div>
      <div class="card-meta">
        <span class="skeleton skeleton-text" style="width: 50px"></span>
        <span class="skeleton skeleton-text" style="width: 80px"></span>
        <span class="skeleton skeleton-text" style="width: 60px"></span>
        <span class="skeleton skeleton-text" style="width: 40px"></span>
      </div>
    </div>
  </div>
  <div v-else-if="totalCount > 0" :class="viewMode === 'grid' ? 'webhook-grid' : 'webhook-list'">
    <div
      v-for="webhook in webhooks"
      :key="webhook.id"
      class="webhook-card"
      @click="emit('open', webhook.id)"
    >
      <div class="card-top-row">
        <div class="icon-box">
          <WebhookIcon size="medium" />
        </div>
        <div class="title-box" style="flex: 1">
          <div v-if="viewMode === 'list'" class="title-row-list" style="width: 100%">
            <div style="display: flex; align-items: center; gap: 8px; width: 100%">
              <span class="title-text">{{ webhook.name }}</span>
              <span
                class="status-chip"
                :class="webhook.isEnabled ? 'status-active' : 'status-disabled'"
              >
                {{ webhook.isEnabled ? 'Enabled' : 'Disabled' }}
              </span>
              <span v-if="getProjectLabel(webhook)" class="project-chip">{{
                getProjectLabel(webhook)
              }}</span>
              <span style="flex: 1 1 0"></span>
              <span class="trigger-label">Last Trigger:</span>
              <span
                v-if="webhook.lastEventHistory?.status"
                class="trigger-chip"
                :class="'trigger-' + webhook.lastEventHistory.status.toLowerCase()"
              >
                {{ webhook.lastEventHistory.status }}
              </span>
              <span v-if="webhook.lastEventHistory?.triggeredAt" class="trigger-date">
                {{ formatMetadataDate(webhook.lastEventHistory.triggeredAt) }}
              </span>
              <span v-if="!webhook.lastEventHistory" class="trigger-date">No triggers yet</span>
            </div>
            <div style="display: flex; align-items: center; margin-top: 5px">
              <span class="info-cell" style="display: inline; padding: 0; margin: 0">
                <span class="label">Issue Type:</span>
                <span class="value">{{ getIssueTypeLabel(webhook) }}</span>
                <span class="label" style="margin-left: 25px">Assignee:</span>
                <span class="value">{{ getAssignee(webhook) }}</span>
              </span>
              <span style="flex: 1 1 0"></span>
              <span class="created-text"
                ><span class="label">Created:</span>
                {{ formatMetadataDate(webhook.createdDate) }}</span
              >
            </div>
          </div>
          <div v-else>
            <div class="title-text">{{ webhook.name }}</div>
            <div class="created-text">
              <span class="label">Created:</span> {{ formatMetadataDate(webhook.createdDate) }}
            </div>
          </div>
        </div>

        <ActionMenu
          :items="getWebhookMenuItems(webhook)"
          @action="emit('action', $event, webhook)"
          trigger-aria-label="Webhook options"
        />
      </div>

      <div v-if="viewMode === 'grid'" style="display: flex; flex-direction: column; gap: 0">
        <div class="chips-row" style="margin-bottom: 12px">
          <span
            class="status-chip"
            :class="webhook.isEnabled ? 'status-active' : 'status-disabled'"
          >
            {{ webhook.isEnabled ? 'Enabled' : 'Disabled' }}
          </span>
          <span v-if="getProjectLabel(webhook)" class="project-chip" style="margin-right: 55px">
            {{ getProjectLabel(webhook) }}
          </span>
        </div>
        <div class="info-row" style="margin-bottom: 0; margin-top: -6px">
          <div class="info-cell">
            <span class="label">Issue Type:</span>
            <span class="value">{{ getIssueTypeLabel(webhook) }}</span>
          </div>
          <div
            class="info-cell align-right"
            style="display: flex; flex-direction: column; align-items: flex-end; gap: 0"
          >
            <div style="margin-top: -30px">
              <span class="trigger-label">Last Trigger:</span>
              <span
                v-if="webhook.lastEventHistory?.status"
                class="trigger-chip"
                :class="'trigger-' + webhook.lastEventHistory.status.toLowerCase()"
              >
                {{ webhook.lastEventHistory.status }}
              </span>
              <span v-if="webhook.lastEventHistory?.triggeredAt" class="trigger-date">
                {{ formatMetadataDate(webhook.lastEventHistory.triggeredAt) }}
              </span>
              <span v-if="!webhook.lastEventHistory" class="trigger-date"> No triggers yet</span>
            </div>
            <div style="margin-top: 5px">
              <span class="label">Assignee:</span>
              <span class="value">{{ getAssignee(webhook) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="url-row">
        <span class="url-label">Webhook URL:</span>
        <span class="url-value">{{ webhook.webhookUrl }}</span>
        <button
          class="copy-icon-btn"
          @click.stop="emit('copy', webhook.webhookUrl)"
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
  <div v-else class="simple-empty-state">
    <h3 class="empty-title">{{ search ? 'No matching webhooks found' : 'No webhooks found' }}</h3>
    <p class="empty-subtitle">
      {{
        search
          ? `No webhooks match "${search}". Try adjusting your search terms.`
          : 'Create your first webhook to get started'
      }}
    </p>
  </div>
</template>

<script setup lang="ts">
import type { DashboardViewMode } from '@/types/dashboard';
import type { JiraWebhook } from '@/types/JiraWebhook';
import WebhookIcon from '@/components/common/WebhookIcon.vue';
import ActionMenu, { type ActionMenuItem } from '@/components/common/ActionMenu.vue';
import {
  enableDisableAria,
  enableDisableIcon,
  enableDisableLabel,
} from './utils/JiraWebhookDashboardHelper';

const emit = defineEmits<{
  (e: 'open', webhookId: string): void;
  (e: 'action', actionId: string, webhook: JiraWebhook): void;
  (e: 'copy', value: string): void;
}>();

defineProps<{
  loading: boolean;
  search: string;
  webhooks: JiraWebhook[];
  totalCount: number;
  viewMode: DashboardViewMode;
  getProjectLabel: (webhook: JiraWebhook) => string | undefined;
  getIssueTypeLabel: (webhook: JiraWebhook) => string;
  getAssignee: (webhook: JiraWebhook) => string;
  formatMetadataDate: (value: string) => string;
}>();

function getWebhookMenuItems(webhook: JiraWebhook): ActionMenuItem[] {
  return [
    {
      id: 'edit',
      label: 'Edit',
      iconType: 'svg',
      svgPath:
        'M3 17.25V21h3.75l11-11-3.75-3.75-11 11zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z',
    },
    {
      id: 'clone',
      label: 'Clone',
      iconType: 'svg',
      svgPath:
        'M16 1H4c-1.1 0-2 .9-2 2v12h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z',
    },
    {
      id: webhook.isEnabled ? 'disable' : 'enable',
      label: enableDisableLabel(webhook),
      iconType: 'devextreme',
      icon: enableDisableIcon(webhook),
      ariaLabel: enableDisableAria(webhook),
    },
    {
      id: 'delete',
      label: 'Delete',
      iconType: 'svg',
      svgPath: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
    },
  ];
}
</script>
