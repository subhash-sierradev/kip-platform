<template>
  <div class="confluence-details-root">
    <StandardDetailPageLayout
      :title="integrationTitle"
      :loading="loading"
      :error="error"
      :status="confluenceIntegrationResponse?.isEnabled ? 'enabled' : 'disabled'"
      :version="confluenceIntegrationResponse?.version?.toString()"
      :active-tab="tabs[activeTab].id"
      :tabs="formattedTabs"
      :show-back-button="true"
      icon="dx-icon-doc"
      :component-props="{
        integrationData: confluenceIntegrationResponse,
        integrationId: integrationId,
        loading: loading,
      }"
      :component-events="{
        'status-updated': onStatusUpdated,
        refresh: fetchIntegrationDetails,
        'edit-requested': openEditWizard,
        'clone-requested': openCloneWizard,
      }"
      @back="handleBack"
      @retry="fetchIntegrationDetails"
      @tab-change="handleTabChangeById"
      @status-updated="onStatusUpdated"
      @refresh="fetchIntegrationDetails"
    />

    <ConfluenceIntegrationWizard
      :open="editWizardOpen"
      :mode="'edit'"
      :integration-id="integrationId"
      @close="editWizardOpen = false"
      @integration-updated="fetchIntegrationDetails"
    />
    <ConfluenceIntegrationWizard
      :open="cloneWizardOpen"
      :mode="'clone'"
      :integration-id="cloningIntegrationId || undefined"
      @close="cloneWizardOpen = false"
      @integration-created="fetchIntegrationDetails"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ConfluenceIntegrationDetailsPage' });
import { computed, ref, defineAsyncComponent, onMounted, inject, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import StandardDetailPageLayout from '@/components/common/StandardDetailPageLayout.vue';
import { ConfluenceIntegrationService } from '@/api/services/ConfluenceIntegrationService';
import type { ConfluenceIntegrationResponse } from '@/api/models/ConfluenceIntegrationResponse';
import { ROUTES } from '@/router/routes';
import type { TabDefinition } from '@/types/tab';
import ConfluenceIntegrationWizard from '../wizard/ConfluenceIntegrationWizard.vue';

const route = useRoute();
const router = useRouter();
const setBreadcrumbTitle = inject<(title: string | null) => void>('setBreadcrumbTitle');

const integrationId = computed(() => route.params.id as string);
const confluenceIntegrationResponse = ref<ConfluenceIntegrationResponse | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);
const editWizardOpen = ref(false);
const cloneWizardOpen = ref(false);
const cloningIntegrationId = ref<string | null>(null);

const ConfluenceOverviewTab = defineAsyncComponent(() => import('./ConfluenceOverviewTab.vue'));
const ConfluenceJobHistoryTab = defineAsyncComponent(() => import('./ConfluenceJobHistoryTab.vue'));
const ConfluenceSettingsTab = defineAsyncComponent(() => import('./ConfluenceSettingsTab.vue'));
const ScheduleInfoTab = defineAsyncComponent(
  () => import('@/components/outbound/arcgisintegration/details/ScheduleInfoTab.vue')
);

const tabs = ref([
  {
    id: 'overview',
    label: 'Overview',
    iconClass: 'dx-icon dx-icon-info',
    component: ConfluenceOverviewTab,
  },
  {
    id: 'settings',
    label: 'Confluence Settings',
    iconClass: 'dx-icon dx-icon-globe',
    component: ConfluenceSettingsTab,
  },
  {
    id: 'schedule',
    label: 'Schedule Info',
    iconClass: 'dx-icon dx-icon-event',
    component: ScheduleInfoTab,
  },
  {
    id: 'job-history',
    label: 'Job History',
    iconClass: 'dx-icon dx-icon-clock',
    component: ConfluenceJobHistoryTab,
  },
]);

const activeTab = ref(0);

const formattedTabs = computed<TabDefinition[]>(() =>
  tabs.value.map(tab => ({
    id: tab.id,
    label: tab.label,
    component: tab.component,
    iconClass: tab.iconClass,
  }))
);

const integrationTitle = computed(() => {
  if (loading.value) return 'Loading...';
  if (error.value) return 'Error loading integration';
  return confluenceIntegrationResponse.value?.name || `Integration ${integrationId.value}`;
});

function handleTabChangeById(tabId: string): void {
  const tabIndex = tabs.value.findIndex(tab => tab.id === tabId);
  if (tabIndex !== -1) activeTab.value = tabIndex;
}

function onStatusUpdated(enabled: boolean): void {
  if (confluenceIntegrationResponse.value) {
    confluenceIntegrationResponse.value = {
      ...confluenceIntegrationResponse.value,
      isEnabled: enabled,
    };
  }
  fetchIntegrationDetails();
}

function handleBack(): void {
  router.replace({ path: ROUTES.confluenceIntegration, query: route.query });
}

function openEditWizard(): void {
  editWizardOpen.value = true;
}

function openCloneWizard(): void {
  cloningIntegrationId.value = integrationId.value;
  cloneWizardOpen.value = true;
}

async function fetchIntegrationDetails(): Promise<void> {
  if (!integrationId.value) {
    error.value = 'Integration ID is required';
    return;
  }
  loading.value = true;
  error.value = null;
  try {
    confluenceIntegrationResponse.value =
      await ConfluenceIntegrationService.getConfluenceIntegrationById(integrationId.value);
  } catch (err: unknown) {
    const message = (err as { message?: string })?.message;
    error.value = message || 'Failed to load integration details. Please try again.';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchIntegrationDetails();
  setBreadcrumbTitle?.('Confluence Integration Details');
});

onUnmounted(() => {
  setBreadcrumbTitle?.(null);
});
</script>
