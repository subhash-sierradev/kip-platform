<template>
  <div class="confluence-integration-dashboard dashboard-no-border">
    <DashboardToolbar
      :search="search"
      :sort-by="sortBy"
      :sort-options="confluenceSortOptions"
      :page-size="pageSize"
      :view-mode="viewMode"
      :current-page="currentPage"
      :total-count="totalCount"
      :page-size-options="pageSizeOptions"
      search-placeholder="Search integrations by name..."
      create-button-text="+ Add Confluence Integration"
      @update:search="search = $event"
      @update:sortBy="sortBy = $event"
      @update:pageSize="pageSize = $event"
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
      <ConfluenceIntegrationCard
        v-for="integration in paginatedIntegrations"
        :key="integration.id"
        :integration="integration"
        :menu-items="getIntegrationMenuItems(integration)"
        @action="actionId => handleIntegrationAction(actionId, integration)"
        @open="openDetails"
      />
    </div>

    <div v-else class="simple-empty-state">
      <h3 class="empty-title">
        {{ search ? 'No matching integrations found' : 'No Confluence Integration Found' }}
      </h3>
      <p class="empty-subtitle">
        {{
          search
            ? `No integrations match "${search}". Try adjusting your search terms.`
            : 'Create Your First Confluence Integration'
        }}
      </p>
    </div>

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

    <!-- Create Wizard -->
    <ConfluenceIntegrationWizard
      :open="wizardOpen"
      @close="wizardOpen = false"
      @integration-created="fetchIntegrations"
    />
    <!-- Edit Wizard -->
    <ConfluenceIntegrationWizard
      :open="editWizardOpen"
      :mode="'edit'"
      :integration-id="editingIntegrationId || undefined"
      @close="editWizardOpen = false"
      @integration-updated="fetchIntegrations"
    />
    <!-- Clone Wizard -->
    <ConfluenceIntegrationWizard
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
import ConfluenceIntegrationWizard from './wizard/ConfluenceIntegrationWizard.vue';
import ConfluenceIntegrationCard from './ConfluenceIntegrationCard.vue';
import DashboardToolbar from '@/components/common/DashboardToolbar.vue';
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import {
  useConfluenceIntegrationActions,
  useConfluenceIntegrationStatus,
} from '@/composables/useConfluenceIntegrationActions';
import { useConfirmationDialog } from '@/composables/useConfirmationDialog';
import {
  getIntegrationMenuItems,
  createConfluenceDialogConfig,
} from './utils/ConfluenceDashboardHelper';
import type { ConfluenceIntegrationSummaryResponse } from '@/api/models/ConfluenceIntegrationSummaryResponse';
import type { DashboardSortOption, DashboardViewMode } from '@/types/dashboard';

const router = useRouter();
const route = useRoute();

const { loading, getAllIntegrations, deleteIntegration } = useConfluenceIntegrationActions();
const { toggleIntegrationStatus } = useConfluenceIntegrationStatus();

const search = ref('');
const integrations = ref<ConfluenceIntegrationSummaryResponse[]>([]);
const sortBy = ref('createdDate');
const viewMode = ref<DashboardViewMode>('grid');
const wizardOpen = ref(false);
const editWizardOpen = ref(false);
const editingIntegrationId = ref<string | null>(null);
const cloneWizardOpen = ref(false);
const cloningIntegrationId = ref<string | null>(null);

const pageSize = ref(6);
const currentPage = ref(1);
const pageSizeOptions = [6, 12, 24, 48];

const confluenceSortOptions: DashboardSortOption[] = [
  { value: 'name', label: 'Name' },
  { value: 'createdDate', label: 'Created Date' },
  { value: 'isEnabled', label: 'Status' },
];

const confluenceDialogConfig = createConfluenceDialogConfig();

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
} = useConfirmationDialog(confluenceDialogConfig);

const pendingIntegration = ref<ConfluenceIntegrationSummaryResponse | null>(null);

const fetchIntegrations = async (): Promise<void> => {
  integrations.value = await getAllIntegrations();
};

const filteredIntegrations = computed(() => {
  if (!search.value) return integrations.value;
  const q = search.value.toLowerCase();
  return integrations.value.filter(
    i => (i.name || '').toLowerCase().includes(q) || i.createdBy.toLowerCase().includes(q)
  );
});

const getSortValue = (i: ConfluenceIntegrationSummaryResponse, field: string): unknown => {
  switch (field) {
    case 'name':
      return i.name || '';
    case 'createdDate':
      return i.createdDate;
    case 'isEnabled':
      return i.isEnabled;
    default:
      return i[field as keyof ConfluenceIntegrationSummaryResponse];
  }
};

const sortedIntegrations = computed(() => {
  const sorted = [...filteredIntegrations.value];
  if (sortBy.value && sorted.length > 0) {
    sorted.sort((a, b) => {
      const aVal = getSortValue(a, sortBy.value);
      const bVal = getSortValue(b, sortBy.value);
      if (sortBy.value === 'createdDate') {
        return new Date(bVal as string).getTime() - new Date(aVal as string).getTime();
      }
      if (sortBy.value === 'isEnabled') {
        if (aVal === bVal) return 0;
        return aVal ? -1 : 1;
      }
      return String(aVal).localeCompare(String(bVal));
    });
  }
  return sorted;
});

const totalCount = computed(() => sortedIntegrations.value.length);
const totalPages = computed(() => Math.ceil(totalCount.value / pageSize.value));

const paginatedIntegrations = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return sortedIntegrations.value.slice(start, start + pageSize.value);
});

const setViewMode = (mode: DashboardViewMode): void => {
  viewMode.value = mode;
};
const prevPage = (): void => {
  if (currentPage.value > 1) currentPage.value--;
};
const nextPage = (): void => {
  if (currentPage.value < totalPages.value) currentPage.value++;
};
const goToCreate = (): void => {
  wizardOpen.value = true;
};

function handleIntegrationAction(
  actionId: string,
  integration: ConfluenceIntegrationSummaryResponse
): void {
  switch (actionId) {
    case 'edit':
      editingIntegrationId.value = integration.id;
      editWizardOpen.value = true;
      break;
    case 'clone':
      cloningIntegrationId.value = integration.id;
      cloneWizardOpen.value = true;
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

function openDialog(
  type: 'enable' | 'disable' | 'delete',
  integration: ConfluenceIntegrationSummaryResponse
): void {
  pendingIntegration.value = integration;
  openDialogBase(type as Parameters<typeof openDialogBase>[0]);
}

async function handleConfirm(): Promise<void> {
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
  });
}

function closeDialogLocal(): void {
  closeDialog();
  pendingIntegration.value = null;
}

async function deleteIntegrationAction(integrationId: string): Promise<void> {
  const ok = await deleteIntegration(integrationId);
  if (ok) {
    integrations.value = integrations.value.filter(i => i.id !== integrationId);
  }
}

async function enableDisableIntegration(integrationId: string, isEnabled?: boolean): Promise<void> {
  const newEnabled = await toggleIntegrationStatus(integrationId, isEnabled);
  if (typeof newEnabled === 'boolean') {
    const idx = integrations.value.findIndex(i => i.id === integrationId);
    if (idx !== -1) {
      integrations.value[idx] = { ...integrations.value[idx], isEnabled: newEnabled };
    }
  } else {
    await fetchIntegrations();
  }
}

const openDetails = (integrationId: string): void => {
  router.push({
    path: `/outbound/integration/confluence/${integrationId}`,
    query: buildStateQuery(true),
  });
};

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
    validSortOptions: ['name', 'createdDate', 'isEnabled'],
    validViewModes: ['grid', 'list'],
    totalPages,
  },
  { search, sortBy, viewMode, currentPage, pageSize, pageSizeOptions }
);

onMounted(async () => {
  ensureDetailsRoute(
    '/outbound/integration/confluence/:id',
    'confluence-integration-details',
    () => import('./details/ConfluenceIntegrationDetailsPage.vue')
  );
  applyStateFromRoute();
  await fetchIntegrations();
  markRestored();
  await restoreScrollFromQuery();
});
</script>

<script lang="ts">
export default {
  name: 'ConfluenceIntegrationPage',
};
</script>

<style>
@import '@/styles/dashboard-common.css';
@import '@/components/outbound/arcgisintegration/ArcGISIntegrationPage.css';
</style>
