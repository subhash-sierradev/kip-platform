<template>
  <div class="arcgis-integration-dashboard dashboard-no-border">
    <DashboardToolbar
      :search="search"
      :sort-by="sortBy"
      :sort-options="arcgisSortOptions"
      :page-size="pageSize"
      :view-mode="viewMode"
      :current-page="currentPage"
      :total-count="totalCount"
      :page-size-options="pageSizeOptions"
      search-placeholder="Search integrations by name..."
      create-button-text="+ Add ArcGIS Integration"
      @update:search="search = $event"
      @update:sortBy="sortBy = $event"
      @update:pageSize="setPageSize($event)"
      @setViewMode="setViewMode"
      @prevPage="prevPage"
      @nextPage="nextPage"
      @create="goToCreate"
    />

    <div v-if="loading" class="integration-grid">
      <div v-for="n in 6" :key="'skeleton-' + n" class="integration-card">
        <div class="card-title skeleton skeleton-text" style="width: 60%"></div>
        <div class="card-desc skeleton skeleton-text" style="width: 90%"></div>
        <div class="card-meta">
          <span class="skeleton skeleton-text" style="width: 50px"></span>
          <span class="skeleton skeleton-text" style="width: 80px"></span>
          <span class="skeleton skeleton-text" style="width: 60px"></span>
        </div>
      </div>
    </div>

    <div
      v-else-if="filteredIntegrations.length > 0"
      :class="viewMode === 'grid' ? 'integration-grid' : 'integration-list'"
    >
      <ArcGISIntegrationCard
        v-for="integration in paginatedIntegrations"
        :key="integration.id"
        :integration="integration"
        :menu-items="getIntegrationMenuItems(integration)"
        @action="actionId => handleIntegrationAction(actionId, integration)"
        @open="openDetails"
      />
    </div>

    <!-- Empty State: search-aware messaging aligned with Webhook list -->
    <div v-else class="simple-empty-state">
      <h3 class="empty-title">
        {{ search ? 'No matching integrations found' : 'No ArcGIS Integration Found' }}
      </h3>
      <p class="empty-subtitle">
        {{
          search
            ? `No integrations match "${search}". Try adjusting your search terms.`
            : 'Create Your First ArcGIS Integration'
        }}
      </p>
    </div>

    <!-- Confirmation Dialog -->
    <ConfirmationDialog
      :open="dialogOpen"
      :type="pendingAction || 'custom'"
      :title="dialogTitle"
      :description="dialogDescription"
      :confirm-label="dialogConfirmLabel"
      :loading="actionLoading"
      @cancel="closeDialogLocal"
      @confirm="handleConfirm"
    />

    <!-- ArcGIS Integration Wizard Modal -->
    <ArcGISIntegrationWizard
      :open="wizardOpen"
      @close="wizardOpen = false"
      @integration-created="fetchIntegrations"
    />
    <!-- Edit ArcGIS Integration Wizard Modal -->
    <ArcGISIntegrationWizard
      :open="editWizardOpen"
      :mode="'edit'"
      :integration-id="editingIntegrationId || undefined"
      @close="editWizardOpen = false"
      @integration-updated="fetchIntegrations"
    />
    <!-- Clone ArcGIS Integration Wizard Modal -->
    <ArcGISIntegrationWizard
      :open="cloneWizardOpen"
      :mode="'clone'"
      :integration-id="cloningIntegrationId || undefined"
      @close="cloneWizardOpen = false"
      @integration-created="fetchIntegrations"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useListRouteSync } from '@/composables/useListRouteSync';
import { useResponsivePageSize } from '@/composables/useResponsivePageSize';
import { ROUTES } from '@/router/routes';
// formatMetadataDate no longer used in page; handled in card component
import ArcGISIntegrationWizard from './wizard/ArcGISIntegrationWizard.vue';
import ArcGISIntegrationCard from './ArcGISIntegrationCard.vue';
import DashboardToolbar from '@/components/common/DashboardToolbar.vue';
import type { ActionMenuItem } from '@/components/common/ActionMenu.vue';
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import { useToastStore } from '../../../store/toast';
import {
  useArcGISIntegrationActions,
  useArcGISIntegrationStatus,
  useArcGISIntegrationTrigger,
} from '@/composables/useArcGISIntegrationActions';
import {
  useConfirmationDialog,
  createDefaultDialogConfig,
} from '@/composables/useConfirmationDialog';
import type { ArcGISIntegrationSummaryResponse } from '@/api/models/ArcGISIntegrationSummaryResponse';
import type { DashboardSortOption, DashboardViewMode } from '@/types/dashboard';
// schedule formatting handled in ArcGISIntegrationCard

const router = useRouter();
const route = useRoute();

const { loading, getAllIntegrations, deleteIntegration } = useArcGISIntegrationActions();

const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
const { triggerJobExecution } = useArcGISIntegrationTrigger();

const search = ref('');
const integrations = ref<ArcGISIntegrationSummaryResponse[]>([]);
const sortBy = ref('createdDate');
const viewMode = ref<DashboardViewMode>('grid');
const wizardOpen = ref(false);
const editWizardOpen = ref(false);
const editingIntegrationId = ref<string | null>(null);
const cloneWizardOpen = ref(false);
const cloningIntegrationId = ref<string | null>(null);

// Pagination state
const { manualPageSize, pageSize, resetPageSize, setPageSize } = useResponsivePageSize();
const currentPage = ref(1);
const pageSizeOptions = [6, 9, 12, 24, 48];

// Sort options for ArcGIS integrations
const arcgisSortOptions: DashboardSortOption[] = [
  { value: 'name', label: 'Name' },
  { value: 'createdDate', label: 'Created Date' },
  { value: 'lastModifiedDate', label: 'Updated Date' },
  { value: 'isEnabled', label: 'Status' },
  { value: 'lastTrigger', label: 'Last Triggered' },
];

const toast = useToastStore();

// Confirmation dialog setup
const arcgisDialogConfig = {
  ...createDefaultDialogConfig(),
  enable: {
    title: 'Enable ArcGIS Integration',
    desc: 'Enabling this integration will allow scheduled or manual runs.',
    label: 'Enable',
  },
  disable: {
    title: 'Disable ArcGIS Integration',
    desc: 'Disabling this integration will prevent runs until re-enabled.',
    label: 'Disable',
  },
  delete: {
    title: 'Delete ArcGIS Integration',
    desc: 'Deleting this integration will remove it permanently. This action cannot be undone.',
    label: 'Delete',
  },
  runNow: {
    title: 'Run ArcGIS Integration',
    desc: 'This will trigger an immediate execution of this integration job.',
    label: 'Run Now',
  },
};

const {
  dialogOpen,
  actionLoading,
  pendingAction,
  dialogTitle,
  dialogDescription,
  dialogConfirmLabel,
  openDialog: openDialogBase,
  closeDialog,
  confirmWithHandlers,
} = useConfirmationDialog(arcgisDialogConfig);

const pendingIntegration = ref<ArcGISIntegrationSummaryResponse | null>(null);

// Local wrapper functions to adapt to component interface
// schedule formatting moved to ArcGISIntegrationCard component

// Real API data fetching function with proper field mapping
const fetchIntegrations = async (): Promise<void> => {
  integrations.value = await getAllIntegrations();
};

// Helper functions for enable/disable labels
function enableDisableLabel(integration: ArcGISIntegrationSummaryResponse): string {
  return integration.isEnabled ? 'Disable' : 'Enable';
}

function enableDisableIcon(integration: ArcGISIntegrationSummaryResponse): string {
  return integration.isEnabled ? 'dx-icon-cursorprohibition' : 'dx-icon-video';
}

// Computed properties for filtering and sorting
const filteredIntegrations = computed(() => {
  if (!search.value) return integrations.value;
  const searchLower = search.value.toLowerCase();
  return integrations.value.filter(
    integration =>
      (integration.name || '').toLowerCase().includes(searchLower) ||
      integration.createdBy.toLowerCase().includes(searchLower)
  );
});

// Sorting helper function to reduce complexity
const getSortValue = (integration: ArcGISIntegrationSummaryResponse, sortField: string): any => {
  switch (sortField) {
    case 'name':
      return integration.name || '';
    case 'createdDate':
      return integration.createdDate;
    case 'lastModifiedDate':
      return integration.lastModifiedDate;
    case 'isEnabled':
      return integration.isEnabled;
    case 'lastTrigger':
      return null; // ArcGIS integrations don't have trigger history
    default:
      return integration[sortField as keyof ArcGISIntegrationSummaryResponse];
  }
};

// Date sorting comparison
const compareDates = (aValue: string, bValue: string): number => {
  const dateA = new Date(aValue);
  const dateB = new Date(bValue);
  return dateB.getTime() - dateA.getTime(); // Newest first
};

// Status sorting comparison (enabled first)
const compareStatus = (aValue: boolean, bValue: boolean): number => {
  if (aValue === bValue) return 0;
  return aValue ? -1 : 1;
};

const sortedIntegrations = computed(() => {
  const sorted = [...filteredIntegrations.value];

  if (sortBy.value && sorted.length > 0) {
    sorted.sort((a, b) => {
      const aValue = getSortValue(a, sortBy.value);
      const bValue = getSortValue(b, sortBy.value);

      // Handle date fields
      if (sortBy.value === 'createdDate' || sortBy.value === 'lastModifiedDate') {
        return compareDates(aValue as string, bValue as string);
      }

      // Handle status field
      if (sortBy.value === 'isEnabled') {
        return compareStatus(aValue as boolean, bValue as boolean);
      }

      // Handle last trigger (always null for ArcGIS, so no sorting)
      if (sortBy.value === 'lastTrigger') {
        return 0; // No sorting for ArcGIS integrations
      }

      // Handle string fields (ascending order)
      return String(aValue).localeCompare(String(bValue));
    });
  }

  return sorted;
});

// Pagination computed properties
const totalCount = computed(() => sortedIntegrations.value.length);
const totalPages = computed(() => Math.ceil(totalCount.value / pageSize.value));

const paginatedIntegrations = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return sortedIntegrations.value.slice(start, end);
});

// UI interaction functions
const setViewMode = (mode: DashboardViewMode): void => {
  viewMode.value = mode;
};

const prevPage = (): void => {
  if (currentPage.value > 1) {
    currentPage.value--;
  }
};

const nextPage = (): void => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
  }
};
const goToCreate = (): void => {
  wizardOpen.value = true;
};

// ActionMenu helper functions
function getIntegrationMenuItems(integration: ArcGISIntegrationSummaryResponse): ActionMenuItem[] {
  const items: ActionMenuItem[] = [
    { id: 'trigger', label: 'Run Now', iconType: 'svg' as const, svgPath: 'M8 5v14l11-7z' },
    {
      id: 'edit',
      label: 'Edit',
      iconType: 'svg' as const,
      svgPath:
        'M3 17.25V21h3.75l11-11-3.75-3.75-11 11zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z',
    },
    {
      id: 'clone',
      label: 'Clone',
      iconType: 'svg' as const,
      svgPath:
        'M16 1H4c-1.1 0-2 .9-2 2v12h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z',
    },
    {
      id: integration.isEnabled ? 'disable' : 'enable',
      label: enableDisableLabel(integration),
      iconType: 'devextreme' as const,
      icon: enableDisableIcon(integration),
    },
    {
      id: 'delete',
      label: 'Delete',
      iconType: 'svg' as const,
      svgPath: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
    },
  ];

  // Only show trigger option if integration is enabled
  if (!integration.isEnabled) {
    return items.filter(item => item.id !== 'trigger');
  }

  return items;
}

function handleIntegrationAction(
  actionId: string,
  integration: ArcGISIntegrationSummaryResponse
): void {
  switch (actionId) {
    case 'trigger':
      openDialog('runNow' as any, integration);
      break;
    case 'edit':
      editIntegration(integration.id);
      break;
    case 'clone':
      cloneIntegration(integration.id);
      break;
    case 'enable':
    case 'disable':
      openDialog(actionId, integration);
      break;
    case 'delete':
      openDialog('delete', integration);
      break;
    default:
      console.warn('Unknown action:', actionId);
  }
}

// Dialog actions
function openDialog(
  type: 'enable' | 'disable' | 'delete' | 'runNow',
  integration: ArcGISIntegrationSummaryResponse,
  event?: Event
) {
  event?.stopPropagation();
  pendingIntegration.value = integration;
  openDialogBase(type as any);
}

async function handleConfirm() {
  if (!pendingIntegration.value) return;
  await confirmWithHandlers({
    delete: async () => {
      try {
        await deleteIntegrationAction(pendingIntegration.value!.id);
        return true;
      } catch {
        return false;
      }
    },
    enable: async () =>
      enableDisableIntegration(pendingIntegration.value!.id, pendingIntegration.value!.isEnabled),
    disable: async () =>
      enableDisableIntegration(pendingIntegration.value!.id, pendingIntegration.value!.isEnabled),
    runNow: async () => triggerIntegration(pendingIntegration.value!),
  });
}

function closeDialogLocal() {
  closeDialog();
  pendingIntegration.value = null;
}

// Integration actions
async function deleteIntegrationAction(integrationId: string) {
  const ok = await deleteIntegration(integrationId);
  if (ok) {
    integrations.value = integrations.value.filter(
      (integration: ArcGISIntegrationSummaryResponse) => integration.id !== integrationId
    );
  }
}

async function enableDisableIntegration(integrationId: string, isEnabled?: boolean) {
  const newEnabled = await toggleIntegrationStatus(integrationId, isEnabled);
  if (typeof newEnabled === 'boolean') {
    const idx = integrations.value.findIndex(
      (integration: ArcGISIntegrationSummaryResponse) => integration.id === integrationId
    );
    if (idx !== -1) {
      integrations.value[idx] = { ...integrations.value[idx], isEnabled: newEnabled };
    }
  } else {
    await fetchIntegrations();
  }
}

const editIntegration = (integrationId: string): void => {
  editingIntegrationId.value = integrationId;
  editWizardOpen.value = true;
};

const cloneIntegration = (integrationId: string): void => {
  cloningIntegrationId.value = integrationId;
  cloneWizardOpen.value = true;
};

// Integration trigger action
async function triggerIntegration(integration: ArcGISIntegrationSummaryResponse): Promise<void> {
  await triggerJobExecution(integration.id, integration.name);
}

const openDetails = (integrationId: string): void => {
  const targetPath = ROUTES.arcgisIntegrationDetails(integrationId);
  router.push({ path: targetPath, query: buildStateQuery(true) }).catch(error => {
    console.error('Navigation to ArcGIS details failed:', error);
    toast.showError('Failed to open integration details');
  });
};

// --- Route-query sync via shared composable ---
const {
  applyStateFromRoute,
  buildStateQuery,
  restoreScrollFromQuery,
  markRestored,
  ensureDetailsRoute,
} = useListRouteSync(
  {
    router,
    route,
    validSortOptions: ['name', 'createdDate', 'lastModifiedDate', 'isEnabled', 'lastTrigger'],
    validViewModes: ['grid', 'list'],
    totalPages,
  },
  {
    search,
    sortBy,
    viewMode,
    currentPage,
    manualPageSize,
    pageSize,
    pageSizeOptions,
    resetPageSize,
    setPageSize,
  }
);

onMounted(async () => {
  ensureDetailsRoute(
    '/outbound/integration/arcgis/:id',
    'arcgis-integration-details',
    () => import('./details/ArcGISIntegrationDetailsPage.vue')
  );
  applyStateFromRoute();
  await fetchIntegrations();
  markRestored();
  await restoreScrollFromQuery();
});
</script>

<script lang="ts">
export default {
  name: 'ArcGISIntegrationPage',
};
</script>

<style>
@import '@/styles/dashboard-common.css';
@import './ArcGISIntegrationPage.css';
</style>

<style scoped>
.arcgis-integration-dashboard {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}
</style>
