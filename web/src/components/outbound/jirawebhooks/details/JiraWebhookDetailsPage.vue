<template>
  <StandardDetailPageLayout
    :title="webhookTitle"
    :loading="loading"
    :error="error"
    :status="webhookData?.isEnabled ? 'enabled' : 'disabled'"
    :version="webhookData?.version?.toString()"
    :active-tab="tabs[activeTab].id"
    :tabs="formattedTabs"
    :show-back-button="true"
    :show-tooltips="true"
    :component-props="{
      webhookData: webhookData,
      webhookId: webhookId,
      loading: loading,
      ...mappingExtraProps,
    }"
    :component-events="{
      'status-updated': onStatusUpdated,
      refresh: fetchWebhookDetails,
    }"
    @back="handleBack"
    @retry="fetchWebhookDetails"
    @tab-change="handleTabChangeById"
    @status-updated="onStatusUpdated"
    @refresh="fetchWebhookDetails"
  >
    <!-- Custom webhook icon for header -->
    <template #custom-icon>
      <div class="webhook-icon-wrapper">
        <WebhookIcon size="medium" />
      </div>
    </template>
  </StandardDetailPageLayout>
</template>

<script setup lang="ts">
// Define component name
defineOptions({ name: 'WebhookDetailsPage' });
import { computed, ref, defineAsyncComponent, onMounted, inject, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { JiraWebhookService, type JiraWebhookDetail } from '@/api/services/JiraWebhookService';
import {
  JiraIntegrationService,
  type JiraProjectResponse,
  type JiraSprintResponse,
  type JiraUserResponse,
} from '@/api/services/JiraIntegrationService';

// Common components
import StandardDetailPageLayout from '@/components/common/StandardDetailPageLayout.vue';
import WebhookIcon from '@/components/common/WebhookIcon.vue';
import type { TabDefinition } from '@/types/tab';

// Inject breadcrumb function from AppShell
const setBreadcrumbTitle = inject<(title: string | null) => void>('setBreadcrumbTitle');

// Async component imports
const WebhookDetailsTab = defineAsyncComponent(() => import('./WebhookDetailsTab.vue'));
const SamplePayloadTab = defineAsyncComponent(() => import('./SamplePayloadTab.vue'));
const JiraFieldMappingTab = defineAsyncComponent(() => import('./JiraFieldMappingTab.vue'));
const WebhookHistoryTab = defineAsyncComponent(() => import('./WebhookHistoryTab.vue'));

// Vue Router
const route = useRoute();
const router = useRouter();

// Get webhook ID from route params
const webhookId = computed(() => route.params.id as string);

// State management
const webhookData = ref<JiraWebhookDetail | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);
const users = ref<{ accountId: string; displayName: string }[]>([]);
const usersLoading = ref(false);
const sprints = ref<{ value: string; label: string }[]>([]);
const sprintsLoading = ref(false);

// Fetch webhook details from API
const fetchWebhookDetails = async () => {
  if (!webhookId.value) {
    error.value = 'Webhook ID is required';
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    const data = await JiraWebhookService.getWebhookById(webhookId.value);
    webhookData.value = data;
    error.value = null;
  } catch (err: any) {
    console.error('Error fetching webhook details:', err);
    if (err.message.includes('403')) {
      error.value = 'Access denied. You may not have permission to view this webhook.';
    } else if (err.message.includes('404')) {
      error.value = 'Webhook not found. It may have been deleted or the ID is incorrect.';
    } else {
      error.value = err.message || 'Failed to load webhook details. Please try again.';
    }
  } finally {
    loading.value = false;
  }
};

// Set static breadcrumb title
onMounted(() => {
  fetchWebhookDetails();
  setBreadcrumbTitle?.('Jira Webhook Details');
});

// Dynamic title showing actual webhook name in the main page
const webhookTitle = computed(() => {
  if (loading.value) return 'Loading...';
  if (error.value) return 'Error loading webhook';
  return webhookData.value?.name || `Webhook ${webhookId.value}`;
});

// Tab configuration with DevExtreme format
const tabs = ref([
  {
    id: 'details',
    label: 'Webhook Details',
    iconClass: 'dx-icon dx-icon-preferences',
    component: WebhookDetailsTab,
  },
  {
    id: 'payload',
    label: 'Sample Payload',
    iconClass: 'dx-icon dx-icon-doc',
    component: SamplePayloadTab,
  },
  {
    id: 'mapping',
    label: 'Jira Field Mapping',
    iconClass: 'dx-icon dx-icon-link',
    component: JiraFieldMappingTab,
  },
  {
    id: 'history',
    label: 'Webhook History',
    iconClass: 'dx-icon dx-icon-event',
    component: WebhookHistoryTab,
  },
]);

// Format tabs for StandardDetailPageLayout component
const formattedTabs = computed<TabDefinition[]>(() =>
  tabs.value.map(tab => ({
    id: tab.id,
    label: tab.label,
    component: tab.component, // Use actual component reference
    iconClass: tab.iconClass,
  }))
);

// Active tab state
const activeTab = ref(0);

// Provide users only to the mapping tab to avoid extraneous props on others
const mappingExtraProps = computed<Record<string, unknown>>(() => {
  const current = tabs.value[activeTab.value];
  if (current?.id === 'mapping') {
    return {
      users: users.value,
      usersLoading: usersLoading.value,
      sprints: sprints.value,
      sprintsLoading: sprintsLoading.value,
    };
  }
  return {};
});

// Methods
function handleTabChangeById(tabId: string): void {
  const tabIndex = tabs.value.findIndex(tab => tab.id === tabId);
  if (tabIndex !== -1) {
    activeTab.value = tabIndex;
  }
}

function handleBack(): void {
  router.back();
}

// Child events handlers
function onStatusUpdated(enabled: boolean): void {
  if (webhookData.value) {
    webhookData.value = { ...webhookData.value, isEnabled: enabled } as JiraWebhookDetail;
  }
}

// Cleanup breadcrumb title when component unmounts
onUnmounted(() => {
  setBreadcrumbTitle?.(null);
});

// Helpers to reduce complexity
function extractProjectLabelOrValue(): string | null {
  const mappings = webhookData.value?.jiraFieldMappings || [];
  const projectMapping = mappings.find(
    m =>
      (m.jiraFieldId || '').toLowerCase() === 'project' ||
      (m.jiraFieldName || '').toLowerCase() === 'project'
  );
  if (!projectMapping) return null;
  const labelOrValue = (projectMapping.displayLabel || projectMapping.defaultValue || '').trim();
  return labelOrValue || null;
}

async function resolveProjectKey(
  connectionId: string,
  labelOrValue: string
): Promise<string | null> {
  try {
    const projects: JiraProjectResponse[] =
      await JiraIntegrationService.getProjectsByConnectionId(connectionId);
    const byKey = projects.find(p => p.key === labelOrValue)?.key;
    if (byKey) return byKey;
    const byName = projects.find(p => p.name === labelOrValue)?.key;
    return byName ?? null;
  } catch {
    return null;
  }
}

async function loadUsersForMapping(): Promise<void> {
  usersLoading.value = true;
  users.value = [];
  try {
    const connectionId = webhookData.value?.connectionId;
    if (!connectionId) return;
    const labelOrValue = extractProjectLabelOrValue();
    if (!labelOrValue) return;
    const projectKey = await resolveProjectKey(connectionId, labelOrValue);
    if (!projectKey) return;
    try {
      const userList: JiraUserResponse[] =
        await JiraIntegrationService.getProjectUsersByConnectionId(connectionId, projectKey);
      users.value = userList.map(u => ({ accountId: u.accountId, displayName: u.displayName }));
    } catch {
      users.value = [];
    }
  } finally {
    usersLoading.value = false;
  }
}

async function loadSprintsForMapping(): Promise<void> {
  sprintsLoading.value = true;
  sprints.value = [];
  try {
    const connectionId = webhookData.value?.connectionId;
    if (!connectionId) return;
    const labelOrValue = extractProjectLabelOrValue();
    if (!labelOrValue) return;
    const projectKey = await resolveProjectKey(connectionId, labelOrValue);
    if (!projectKey) return;
    try {
      const sprintList: JiraSprintResponse[] =
        await JiraIntegrationService.getSprintsByConnectionId(connectionId, {
          projectKey,
          state: 'active,future',
          startAt: 0,
          maxResults: 50,
        });
      sprints.value = sprintList.map(s => ({
        value: String(s.id),
        label: `${s.name} (${s.state})`,
      }));
    } catch {
      sprints.value = [];
    }
  } finally {
    sprintsLoading.value = false;
  }
}

// Derive and load users for the JiraFieldMappingTab
watch(
  () => ({
    connectionId: webhookData.value?.connectionId,
    jiraFieldMappings: webhookData.value?.jiraFieldMappings,
  }),
  () => {
    void loadUsersForMapping();
    void loadSprintsForMapping();
  },
  { immediate: true, deep: true }
);
</script>

<style scoped>
/* Wrapper to ensure proper centering of webhook icon in the header */
.webhook-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

/* Additional adjustment for the SVG icon itself */
.webhook-icon-wrapper :deep(.webhook-icon-svg) {
  display: block;
  margin: auto;
}
</style>
