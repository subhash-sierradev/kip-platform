<template>
  <div class="jira-webhook-dashboard dashboard-no-border">
    <DashboardToolbar
      :search="search"
      :sort-by="sortBy"
      :sort-options="jiraSortOptions"
      :page-size="pageSize"
      :view-mode="viewMode"
      :current-page="currentPage"
      :total-count="totalCount"
      :page-size-options="pageSizeOptions"
      search-placeholder="Search webhooks by name..."
      create-button-text="+ Add Jira Webhook"
      @update:search="search = $event"
      @update:sortBy="sortBy = $event"
      @update:pageSize="setPageSize($event)"
      @setViewMode="setViewMode"
      @prevPage="prevPage"
      @nextPage="nextPage"
      @create="goToCreate"
    />
    <JiraWebhookCard
      :loading="loading"
      :search="search"
      :webhooks="paginatedWebhooks"
      :total-count="totalCount"
      :view-mode="viewMode"
      :get-project-label="getProjectLabel"
      :get-issue-type-label="getIssueTypeLabel"
      :get-assignee="getAssignee"
      :format-metadata-date="formatMetadataDate"
      @open="openDetails"
      @action="handleWebhookAction"
      @copy="copyToClipboard"
    />
    <!-- Pagination moved to toolbar; bottom bar removed -->
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
    <!-- Jira Webhook Wizard Modal -->
    <JiraWebhookWizard
      v-if="wizardOpen"
      :open="wizardOpen"
      :editMode="!!editingWebhookData && !cloneMode"
      :editingWebhookData="editingWebhookData"
      :cloneMode="cloneMode"
      :cloningWebhookData="cloningWebhookData"
      @close="handleWizardClose"
      @webhook-created="fetchWebhooks"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, type Ref } from 'vue';
const editingWebhookData: Ref<JiraWebhook | undefined> = ref(undefined);
const editingWebhookId = ref<string | undefined>(undefined);
const cloneMode = ref<boolean>(false);
const cloningWebhookData: Ref<JiraWebhook | undefined> = ref(undefined);
import { useRouter, useRoute } from 'vue-router';
import { JiraWebhookService } from '@/api/services/JiraWebhookService';
import { ROUTES } from '@/router/routes';
import type { JiraWebhook, JiraFieldMapping } from '@/types/JiraWebhook';
import { formatMetadataDate } from '@/utils/dateUtils';
import {
  getProjectLabel,
  getIssueTypeLabel,
  getAssignee,
  getSortFunction,
  debounce,
  copyToClipboard as copyClipboardHelper,
  buildStateQuery as buildQueryHelper,
} from './utils/JiraWebhookDashboardHelper';

const router = useRouter();
const route = useRoute();
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import JiraWebhookWizard from './wizard/JiraWebhookWizard.vue';
import DashboardToolbar from '@/components/common/DashboardToolbar.vue';
import JiraWebhookCard from './JiraWebhookCard.vue';
import {
  useConfirmationDialog,
  createDefaultDialogConfig,
} from '@/composables/useConfirmationDialog';
import { useResponsivePageSize } from '@/composables/useResponsivePageSize';
import { useWebhookActions } from '@/composables/useWebhookActions';
import { useToastStore } from '@/store/toast';
import type { DashboardSortOption, DashboardViewMode } from '@/types/dashboard';

const search = ref('');
const error = ref('');
const loading = ref(true);
const webhooks = ref<JiraWebhook[]>([]);
const sortBy = ref('createdDate');
const viewMode = ref<DashboardViewMode>('grid');
const wizardOpen = ref(false);
// Pagination state
const { manualPageSize, pageSize, resetPageSize, setPageSize } = useResponsivePageSize();
const currentPage = ref(1);
const pageSizeOptions = [6, 9, 12, 24, 48];

// Sort options for Jira webhooks
const jiraSortOptions: DashboardSortOption[] = [
  { value: 'name', label: 'Name' },
  { value: 'createdDate', label: 'Created Date' },
  { value: 'lastModifiedDate', label: 'Updated Date' },
  { value: 'isEnabled', label: 'Status' },
  { value: 'lastTrigger', label: 'Last Triggered' },
];

const fetchWebhooks = async () => {
  loading.value = true;
  error.value = '';
  try {
    const data = await JiraWebhookService.listJiraWebhooks();
    webhooks.value = Array.isArray(data) ? data : [];
  } catch (e: any) {
    error.value = e?.message || 'Failed to load Jira webhooks.';
    toast.showError(error.value);
  } finally {
    loading.value = false;
  }
};
const { toggleWebhookStatus, deleteWebhook: deleteWebhookAction } = useWebhookActions();

function applyStateFromRoute() {
  const q = route.query;
  if (typeof q.search === 'string') search.value = q.search;
  // Validate sortBy against allowed values
  const validSortOptions = ['name', 'createdDate', 'lastModifiedDate', 'isEnabled', 'lastTrigger'];
  if (typeof q.sort === 'string' && validSortOptions.includes(q.sort)) {
    sortBy.value = q.sort;
  }
  // Validate viewMode against allowed values
  const validViewModes = ['grid', 'list'];
  if (typeof q.view === 'string' && validViewModes.includes(q.view)) {
    viewMode.value = q.view as DashboardViewMode;
  }
  if (typeof q.page === 'string') {
    const p = Number(q.page);
    if (!Number.isNaN(p) && p >= 1) currentPage.value = p;
  }

  if (typeof q.size === 'string') {
    const size = Number(q.size);
    if (pageSizeOptions.includes(size)) {
      setPageSize(size);
      return;
    }
  }

  resetPageSize();
}

onMounted(async () => {
  // Restore state before fetching data so UI initializes correctly
  applyStateFromRoute();
  const scrollQ = route.query.scroll as string | undefined;
  const y = scrollQ ? Number(scrollQ) : 0;
  // Fetch data after state is applied
  await fetchWebhooks();
  // Mark initial restoration complete so pagination/watchers can act normally
  initialRestorationDone.value = true;
  // Restore scroll after data is available
  if (!Number.isNaN(y) && y > 0) {
    // Ensure DOM is rendered before scrolling
    await nextTick();
    window.requestAnimationFrame(() => {
      window.scrollTo({ top: y, behavior: 'auto' });
    });
  }
});

const filteredWebhooks = computed(() => {
  if (!search.value) return webhooks.value;
  const s = search.value.toLowerCase();
  return webhooks.value.filter(
    (w: JiraWebhook) =>
      w.name?.toLowerCase().includes(s) ||
      w.webhookUrl?.toLowerCase().includes(s) ||
      w.createdBy?.toLowerCase().includes(s)
  );
});

const sortedWebhooks = computed(() => {
  const arr = [...filteredWebhooks.value];
  const sortFunction = getSortFunction(sortBy.value);
  arr.sort(sortFunction);
  return arr;
});

const paginatedWebhooks = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return sortedWebhooks.value.slice(start, end);
});

const totalPages = computed(() => {
  const total = sortedWebhooks.value.length;
  return Math.max(1, Math.ceil(total / pageSize.value));
});

// Range display for current page
const totalCount = computed(() => sortedWebhooks.value.length);

function nextPage() {
  if (currentPage.value < totalPages.value) currentPage.value += 1;
}
function prevPage() {
  if (currentPage.value > 1) currentPage.value -= 1;
}

const suppressRouteUpdate = ref(false);
const updateRouteQuery = debounce(() => {
  router.replace({ query: buildStateQuery(false) });
}, 300);
// Flag to prevent clobbering restored state before initial data load completes
const initialRestorationDone = ref(false);

// Reset / clamp pagination only after initial restoration.
watch(
  [search, sortBy, viewMode, pageSize],
  ([_s, _sort, _view, _size], [prevS, _prevSort, _prevView, prevSize]) => {
    if (!initialRestorationDone.value) return;
    const max = totalPages.value;
    if (currentPage.value > max) {
      suppressRouteUpdate.value = true;
      currentPage.value = max;
      return;
    }
    // Page reset triggers: search changed or page size changed. Sort changes preserve current page.
    const pageSizeChanged = _size !== prevSize;
    const searchChanged = _s !== prevS;
    if ((pageSizeChanged || searchChanged) && currentPage.value !== 1) {
      suppressRouteUpdate.value = true;
      currentPage.value = 1;
    }
  }
);

// Clamp currentPage when totalPages changes (e.g., search reduces results)
watch(totalPages, newTotal => {
  if (!initialRestorationDone.value) return;
  if (currentPage.value > newTotal) {
    suppressRouteUpdate.value = true;
    currentPage.value = newTotal;
  }
});

// Debounced route sync; skipped if we just internally adjusted currentPage.
watch([search, sortBy, viewMode, currentPage, pageSize], () => {
  if (suppressRouteUpdate.value) {
    suppressRouteUpdate.value = false;
    return;
  }
  updateRouteQuery();
});

// Utility functions

const copyToClipboard = (text: string) => {
  copyClipboardHelper(text, msg => toast.showSuccess(msg));
};

function buildStateQuery(includeScroll = false) {
  return buildQueryHelper({
    search: search.value,
    sortBy: sortBy.value,
    viewMode: viewMode.value,
    currentPage: currentPage.value,
    pageSize: pageSize.value,
    persistPageSize: manualPageSize.value !== null,
    includeScroll,
  });
}

const openDetails = (webhookId: string) => {
  router.push({ path: ROUTES.jiraWebhookDetails(webhookId), query: buildStateQuery(true) });
};

const setViewMode = (mode: DashboardViewMode) => {
  viewMode.value = mode;
};

function goToCreate() {
  // Clear any existing edit/clone state before opening wizard for create mode
  editingWebhookId.value = undefined;
  editingWebhookData.value = undefined;
  cloneMode.value = false;
  cloningWebhookData.value = undefined;
  wizardOpen.value = true;
}

// eslint-disable-next-line complexity
async function editWebhook(webhookId: string) {
  try {
    const data = await JiraWebhookService.getWebhookById(webhookId);
    const normalizedMappings = Array.isArray(data.jiraFieldMappings)
      ? normalizeJiraFieldMappings(data.jiraFieldMappings)
      : [];
    editingWebhookId.value = webhookId;
    // Ensure clone mode is reset when entering edit mode
    cloneMode.value = false;
    editingWebhookData.value = {
      id: typeof data.id === 'string' ? data.id : '',
      name: typeof data.name === 'string' ? data.name : '',
      webhookUrl: typeof data.webhookUrl === 'string' ? data.webhookUrl : '',
      jiraFieldMappings: normalizedMappings,
      isEnabled: typeof data.isEnabled === 'boolean' ? data.isEnabled : false,
      isDeleted: typeof data.isDeleted === 'boolean' ? data.isDeleted : false,
      createdBy: typeof data.createdBy === 'string' ? data.createdBy : '',
      createdDate: typeof data.createdDate === 'string' ? data.createdDate : '',
      lastEventHistory: {
        eventId: '',
        eventType: '',
        timestamp: '',
        status: '',
      },
      description: typeof data.description === 'string' ? data.description : '',
      samplePayload: typeof data.samplePayload === 'string' ? data.samplePayload : '',
      connectionId: typeof data.connectionId === 'string' ? data.connectionId : '',
      fieldsMapping: Array.isArray((data as any).fieldsMapping)
        ? (data as any).fieldsMapping
        : undefined,
    };
    wizardOpen.value = true;
  } catch (e) {
    toast.showError('Failed to load webhook for editing.');
    console.error('Failed to load webhook for editing:', e);
  }
}
async function cloneWebhook(webhookId: string) {
  try {
    const data = await JiraWebhookService.getWebhookById(webhookId);
    const normalizedMappings = Array.isArray(data.jiraFieldMappings)
      ? normalizeJiraFieldMappings(data.jiraFieldMappings)
      : [];

    // Prepare cloning data: clear identifiers, adjust name, preserve mappings and sample payload
    cloningWebhookData.value = {
      id: '',
      name: typeof data.name === 'string' ? `${data.name}` : 'Webhook',
      webhookUrl: '',
      jiraFieldMappings: normalizedMappings,
      isEnabled: false,
      isDeleted: false,
      createdBy: '',
      createdDate: '',
      lastEventHistory: { eventId: '', eventType: '', timestamp: '', status: '' },
      description: typeof data.description === 'string' ? data.description : '',
      samplePayload: typeof data.samplePayload === 'string' ? data.samplePayload : '',
      connectionId: typeof data.connectionId === 'string' ? data.connectionId : '',
      fieldsMapping: Array.isArray((data as any).fieldsMapping)
        ? (data as any).fieldsMapping
        : undefined,
    };

    // Ensure edit state is cleared and set clone mode
    editingWebhookId.value = undefined;
    editingWebhookData.value = undefined;
    cloneMode.value = true;
    wizardOpen.value = true;
  } catch (e) {
    toast.showError('Failed to load webhook for cloning.');
    console.error('Failed to load webhook for cloning:', e);
  }
}

function normalizeJiraFieldMappings(mappings: any[]): JiraFieldMapping[] {
  return mappings.map(
    m =>
      ({
        sourceField: typeof m.jiraFieldId === 'string' ? m.jiraFieldId : '',
        targetField: '',
        dataType: typeof m.dataType === 'string' ? m.dataType : '',
        required: typeof m.required === 'boolean' ? m.required : false,
        jiraFieldId: typeof m.jiraFieldId === 'string' ? m.jiraFieldId : '',
        displayLabel: typeof m.displayLabel === 'string' ? m.displayLabel : '',
        jiraFieldName: typeof m.jiraFieldName === 'string' ? m.jiraFieldName : '',
        template: typeof m.template === 'string' ? m.template : '',
        defaultValue: typeof m.defaultValue === 'string' ? m.defaultValue : '',
        metadata: typeof m.metadata === 'object' && m.metadata !== null ? m.metadata : {},
      }) as JiraFieldMapping
  );
}

function handleWizardClose() {
  wizardOpen.value = false;
  editingWebhookId.value = undefined;
  editingWebhookData.value = undefined;
  cloneMode.value = false;
  cloningWebhookData.value = undefined;
}

async function enableDisableWebhook(webhookId: string, isEnabled: boolean) {
  const newEnabled = await toggleWebhookStatus(webhookId, isEnabled);
  if (typeof newEnabled === 'boolean') {
    const idx = webhooks.value.findIndex((w: JiraWebhook) => w.id === webhookId);
    if (idx !== -1) {
      webhooks.value[idx] = { ...webhooks.value[idx], isEnabled: newEnabled };
    }
  } else {
    await fetchWebhooks();
  }
}

function handleWebhookAction(actionId: string, webhook: JiraWebhook): void {
  switch (actionId) {
    case 'edit':
      editWebhook(webhook.id);
      break;
    case 'clone':
      cloneWebhook(webhook.id);
      break;
    case 'enable':
    case 'disable':
      openDialog(actionId, webhook);
      break;
    case 'delete':
      openDialog('delete', webhook);
      break;
    default:
      console.warn('Unknown action:', actionId);
  }
}

async function deleteWebhook(webhookId: string) {
  const ok = await deleteWebhookAction(webhookId);
  if (ok) {
    webhooks.value = webhooks.value.filter((w: JiraWebhook) => w.id !== webhookId);
  }
}

// Confirmation dialog via shared composable
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
} = useConfirmationDialog(createDefaultDialogConfig());
const pendingWebhook = ref<JiraWebhook | null>(null);

function openDialog(type: 'enable' | 'disable' | 'delete', webhook: JiraWebhook, event?: Event) {
  event?.stopPropagation();
  pendingWebhook.value = webhook;
  openDialogBase(type);
}

async function handleConfirm() {
  if (!pendingWebhook.value) return;
  await confirmWithHandlers({
    delete: async () => {
      try {
        await deleteWebhook(pendingWebhook.value!.id);
        return true;
      } catch {
        // Error is already handled (toast and log) inside deleteWebhook; intentionally ignored here.
        return false;
      }
    },
    enable: async () =>
      enableDisableWebhook(pendingWebhook.value!.id, pendingWebhook.value!.isEnabled),
    disable: async () =>
      enableDisableWebhook(pendingWebhook.value!.id, pendingWebhook.value!.isEnabled),
  });
}

function closeDialogLocal() {
  closeDialog();
  pendingWebhook.value = null;
}

// Initialize toast store
const toast = useToastStore();
</script>

<style>
/* Import common dashboard styles and page-specific styles */
@import '@/styles/dashboard-common.css';
@import './JiraWebhookDashboard.css';
</style>
