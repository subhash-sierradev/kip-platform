<template>
  <div class="arcgis-details-root">
    <StandardDetailPageLayout
      :title="integrationTitle"
      :loading="loading"
      :error="error"
      :status="arcGISIntegrationResponse?.isEnabled ? 'enabled' : 'disabled'"
      :version="arcGISIntegrationResponse?.version?.toString()"
      :active-tab="tabs[activeTab].id"
      :tabs="formattedTabs"
      :show-back-button="true"
      icon="dx-icon-map"
      :component-props="{
        integrationData: arcGISIntegrationResponse,
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
    <!-- Edit ArcGIS Integration Wizard Modal -->
    <ArcGISIntegrationWizard
      :open="editWizardOpen"
      :mode="'edit'"
      :integration-id="integrationId"
      @close="editWizardOpen = false"
      @integration-updated="fetchIntegrationDetails"
    />
    <!-- Clone ArcGIS Integration Wizard Modal -->
    <ArcGISIntegrationWizard
      :open="cloneWizardOpen"
      :mode="'clone'"
      :integration-id="cloningIntegrationId || undefined"
      @close="cloneWizardOpen = false"
      @integration-created="fetchIntegrationDetails"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ArcGISIntegrationDetailsPage' });
import { computed, ref, defineAsyncComponent, onMounted, inject, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import StandardDetailPageLayout from '@/components/common/StandardDetailPageLayout.vue';
import { ArcGISIntegrationService } from '@/api/services/ArcGISIntegrationService';
import type { ArcGISIntegrationResponse } from '@/api/models/ArcGISIntegrationResponse';
import type { TabDefinition } from '@/types/tab';
import ArcGISIntegrationWizard from '../wizard/ArcGISIntegrationWizard.vue';

const route = useRoute();
const router = useRouter();
// Allow setting a friendly breadcrumb label instead of showing the ID
const setBreadcrumbTitle = inject<(title: string | null) => void>('setBreadcrumbTitle');

const integrationId = computed(() => route.params.id as string);
const arcGISIntegrationResponse = ref<ArcGISIntegrationResponse | null>(null);
const loading = ref(false);
const error = ref<string | null>(null);
const editWizardOpen = ref(false);
const cloneWizardOpen = ref(false);
const cloningIntegrationId = ref<string | null>(null);

const BasicDetailsTab = defineAsyncComponent(() => import('./BasicDetailsTab.vue'));
const ScheduleInfoTab = defineAsyncComponent(() => import('./ScheduleInfoTab.vue'));
const FieldMappingTab = defineAsyncComponent(() => import('./FieldMappingTab.vue'));
const JobHistoryTab = defineAsyncComponent(() => import('./JobHistoryTab.vue'));

const tabs = ref([
  {
    id: 'details',
    label: 'Basic Details',
    iconClass: 'dx-icon dx-icon-info',
    component: BasicDetailsTab,
  },
  {
    id: 'schedule',
    label: 'Schedule Info',
    iconClass: 'dx-icon dx-icon-event',
    component: ScheduleInfoTab,
  },
  {
    id: 'mapping',
    label: 'Field Mapping',
    iconClass: 'dx-icon dx-icon-link',
    component: FieldMappingTab,
  },
  {
    id: 'job-history',
    label: 'Job History',
    iconClass: 'dx-icon dx-icon-clock',
    component: JobHistoryTab,
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
  return arcGISIntegrationResponse.value?.name || `Integration ${integrationId.value}`;
});

function handleTabChangeById(tabId: string): void {
  const tabIndex = tabs.value.findIndex(tab => tab.id === tabId);
  if (tabIndex !== -1) {
    activeTab.value = tabIndex;
  }
}

function onStatusUpdated(enabled: boolean): void {
  if (arcGISIntegrationResponse.value) {
    // Update the enabled status immediately for better UX
    arcGISIntegrationResponse.value = {
      ...arcGISIntegrationResponse.value,
      isEnabled: enabled,
    };
  }
  // Refresh the integration details to get updated version and other fields
  fetchIntegrationDetails();
}

function handleBack(): void {
  // Prefer history back to preserve list query + scroll
  if (window.history.length > 1) {
    router.back();
    return;
  }
  // Fallback: direct navigation (no history) – go to list
  router.push({ path: '/outbound/integration/arcgis', query: route.query });
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
    arcGISIntegrationResponse.value = await ArcGISIntegrationService.getArcGISIntegrationById(
      integrationId.value
    );
  } catch (err: unknown) {
    const message = (err as { message?: string })?.message;
    error.value = message || 'Failed to load integration details. Please try again.';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  fetchIntegrationDetails();
  setBreadcrumbTitle?.('ArcGIS Integration Details');
});

onUnmounted(() => {
  setBreadcrumbTitle?.(null);
});
</script>
