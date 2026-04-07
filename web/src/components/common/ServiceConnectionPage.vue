<template>
  <div class="service-connection-page">
    <div class="table-container">
      <div v-if="loading" class="loading-wrap">
        <DxLoadIndicator />
      </div>
      <div v-else-if="error" class="error-wrap">{{ error }}</div>
      <div v-else>
        <!-- Global tooltip for service actions -->
        <Tooltip
          :visible="actionTipVisible"
          :x="actionTipX"
          :y="actionTipY"
          :id="actionTipId"
          :text="actionTipText"
        />
        <GenericDataGrid
          ref="gridRef"
          :data="allConnections"
          :columns="gridColumns"
          :page-index="page"
          :page-size="rowsPerPage"
          :enable-export="false"
          :enable-clear-filters="true"
          @option-changed="onGridOptionsChanged"
        >
          <template #statusCell="{ data }">
            <StatusChipForDataTable
              :status="data.lastConnectionStatus || 'UNKNOWN'"
              :label="data.lastConnectionStatus || 'UNKNOWN'"
            />
          </template>
          <template #dateCell="{ data }">
            {{ formatMetadataDate(data.lastConnectionTest) }}
          </template>

          <template #actionsCell="{ data }">
            <div class="actions-cell">
              <div
                class="action-wrapper"
                @mouseenter="e => onActionEnter('Test Connection', e)"
                @mousemove="onActionMove"
                @mouseleave="onActionLeave"
              >
                <DxButton
                  class="dx-button"
                  title="Test Connection"
                  :integration-options="{ disabled: false }"
                  @click="openDialog('test', data)"
                  :element-attr="{ 'aria-describedby': actionTipId }"
                >
                  <svg
                    class="wifi-icon"
                    width="18"
                    height="18"
                    viewBox="0 0 22 22"
                    fill="none"
                    stroke="#0891b2"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    :style="{ opacity: 1 }"
                  >
                    <path d="M2 8.82a15 15 0 0 1 20 0" />
                    <path d="M5 12.55a11 11 0 0 1 14 0" />
                    <path d="M8.5 16.05a6 6 0 0 1 7 0" />
                    <circle cx="12" cy="20" r="1" />
                  </svg>
                </DxButton>
              </div>

              <div
                class="action-wrapper"
                @mouseenter="e => onActionEnter('Update Secret', e, true)"
                @mousemove="onActionMove"
                @mouseleave="onActionLeave"
              >
                <DxButton
                  icon="key"
                  styling-mode="text"
                  type="normal"
                  :disabled="rowLoading[data.id]"
                  :element-attr="{ 'aria-label': 'Edit Secret', 'aria-describedby': actionTipId }"
                  @click="openSecretDialog(data)"
                />
              </div>

              <!-- Delete Button with tooltip -->
              <div
                class="action-wrapper"
                @mouseenter="e => onActionEnter('Delete Connection', e, true)"
                @mousemove="onActionMove"
                @mouseleave="onActionLeave"
              >
                <DxButton
                  icon="trash"
                  styling-mode="text"
                  type="normal"
                  :class="'btn-delete'"
                  :element-attr="{ 'aria-label': 'Delete', 'aria-describedby': actionTipId }"
                  @click="onDeleteClick(data)"
                />
              </div>
            </div>
          </template>
        </GenericDataGrid>
      </div>
    </div>
    <ConfirmationDialog
      :open="dialogOpen"
      :type="pendingAction || 'custom'"
      :description="confirmDialogDescription"
      :loading="dialogLoading"
      @cancel="closeDialog"
      @confirm="handleConfirm"
    />

    <AppModal
      :open="secretDialogOpen"
      title="Update Secret"
      size="sm"
      @update:open="value => (value ? (secretDialogOpen = true) : closeSecretDialog())"
    >
      <div class="secret-dialog">
        <label class="secret-label" for="secret-input">{{ secretDialogLabel }}</label>
        <div class="secret-input-with-icon">
          <input
            id="secret-input"
            v-model="secretValue"
            class="secret-input"
            :type="secretVisible ? 'text' : 'password'"
            autocomplete="new-password"
          />
          <button
            type="button"
            class="secret-icon-btn"
            :aria-label="secretVisible ? 'Hide secret' : 'Show secret'"
            @click="secretVisible = !secretVisible"
          >
            <Eye v-if="secretVisible" class="secret-visibility-icon" :size="16" />
            <EyeOff v-else class="secret-visibility-icon" :size="16" />
          </button>
        </div>
      </div>

      <template #footer>
        <div class="secret-actions">
          <DxButton
            text="Cancel"
            styling-mode="outlined"
            type="normal"
            @click="closeSecretDialog"
          />
          <DxButton
            text="Confirm"
            type="default"
            styling-mode="contained"
            :disabled="!secretValue || secretValue.length === 0 || secretDialogLoading"
            @click="confirmSecretUpdate"
          />
        </div>
      </template>
    </AppModal>
    <AppModal
      :open="dependentsDialogOpen"
      size="lg"
      fixed-height
      :style="{
        '--kw-modal-max-width': '1000px',
        '--kw-modal-max-height': '400px',
      }"
      @update:open="value => (value ? (dependentsDialogOpen = true) : closeDependentsDialog())"
    >
      <template #header>
        <div class="dependents-error-header">
          <svg class="dependents-error-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill="currentColor"
              d="M11 7h2v6h-2V7zm0 8h2v2h-2v-2zm1-14C6.48 1 2 5.48 2 11s4.48 10 10 10 10-4.48 10-10S17.52 1 12 1zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z"
            />
          </svg>
          <div class="dependents-error-text">
            <span class="dependents-error-title">Error</span>
            <p class="dependents-error-message">
              Unable to delete connection as it is currently in use by the following integrations
            </p>
          </div>
        </div>
      </template>

      <div class="dependents-dialog">
        <div class="dependents-grid">
          <div class="dependents-metadata-content">
            <DxDataGrid
              :data-source="dependentsGridItems"
              :show-borders="true"
              :row-alternation-enabled="true"
              :allow-column-resizing="true"
              :column-auto-width="true"
              :hover-state-enabled="true"
              :show-row-lines="true"
              :show-column-lines="true"
              :word-wrap-enabled="false"
              width="100%"
              height="100%"
            >
              <DxScrolling mode="virtual" />
              <DxPaging :enabled="false" />

              <DxColumn data-field="name" :caption="dependentsNameCaption" :min-width="200" />
              <DxColumn data-field="description" caption="Description" :min-width="300" />
              <DxColumn
                data-field="lastRunAt"
                caption="Last Run"
                :min-width="180"
                cell-template="lastRunCellTemplate"
              />
              <template #lastRunCellTemplate="{ data }">
                {{ data.value ? formatMetadataDate(data.value) : 'Never Run' }}
              </template>
            </DxDataGrid>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dependents-actions">
          <DxButton
            class="dependents-ok-btn"
            text="OK"
            type="danger"
            styling-mode="contained"
            :width="96"
            @click="closeDependentsDialog"
          />
        </div>
      </template>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
/* eslint max-lines: ["error", { "max": 550 }] */

import { ref, onMounted, computed, watch } from 'vue';
import DxButton from 'devextreme-vue/button';
import DxLoadIndicator from 'devextreme-vue/load-indicator';
import { Eye, EyeOff } from 'lucide-vue-next';
import Tooltip from '@/components/common/Tooltip.vue';
import { useServiceConnectionsAdmin } from '@/composables/useServiceConnectionsAdmin';
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';
import { formatMetadataDate } from '@/utils/dateUtils';
import GenericDataGrid from '@/components/common/GenericDataGrid.vue';
import { ServiceType, ServiceTypeMetadata } from '@/api/models/enums';
import AppModal from '@/components/common/AppModal.vue';
import { DxDataGrid, DxColumn, DxPaging, DxScrolling } from 'devextreme-vue/data-grid';
import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import type { ConnectionDependentItemResponse, ConnectionDependentsResponse } from '@/api/models';
import { useToastStore } from '@/store/toast';
import { ApiError } from '@/api/core/ApiError';

const gridRef = ref();
const rowLoading = ref<{ [key: string]: boolean }>({});

const toast = useToastStore();

// Props
interface Props {
  serviceType: ServiceType;
}
const props = defineProps<Props>();

const {
  allConnections,
  loading,
  error,
  page,
  setPage,
  rowsPerPage,
  setRowsPerPage,
  testConnection,
  deleteConnection,
  rotateSecret,
  fetchConnections,
} = useServiceConnectionsAdmin(props.serviceType);

onMounted(() => {
  fetchConnections();
});

const dialogOpen = ref(false);
const pendingAction = ref<'test' | 'delete' | null>(null);
const pendingConnection = ref<any>(null);

const serviceLabel = computed(() => {
  return ServiceTypeMetadata[props.serviceType]?.displayName ?? props.serviceType;
});
const confirmDialogDescription = computed(() => {
  if (pendingAction.value === 'delete')
    return `Are you sure you want to delete this connection? This action is permanent and cannot be undone.`;
  if (pendingAction.value === 'test')
    return `This will test the ${serviceLabel.value} connection using the selected credentials to verify connectivity. Do you want to continue?`;
  return '';
});

// Loading state for the dialog derived from rowLoading of the pending connection
const dialogLoading = computed<boolean>(() => {
  const id = pendingConnection.value?.id;
  return id ? !!rowLoading.value[id] : false;
});

const secretDialogOpen = ref(false);
const secretValue = ref('');
const secretVisible = ref(false);
const secretConnection = ref<any>(null);
const secretDialogLabel = computed(() => {
  return props.serviceType === ServiceType.ARCGIS
    ? 'New OAuth client secret'
    : 'New password / client secret';
});
const secretDialogLoading = computed<boolean>(() => {
  const id = secretConnection.value?.id;
  return id ? !!rowLoading.value[id] : false;
});

const dependentsDialogOpen = ref(false);
const dependentsResponse = ref<ConnectionDependentsResponse | null>(null);

const dependentsGridItems = computed<ConnectionDependentItemResponse[]>(() => {
  return dependentsResponse.value?.integrations ?? [];
});

// Name column caption varies by service type
const dependentsNameCaption = computed(() => {
  if (!dependentsResponse.value?.serviceType) return 'Integration Name';

  if (dependentsResponse.value.serviceType === ServiceType.ARCGIS) {
    return 'ArcGIS Integration Name';
  }
  if (dependentsResponse.value.serviceType === ServiceType.CONFLUENCE) {
    return 'Confluence Integration Name';
  }
  return 'Jira Webhook Name';
});

// Tooltip state for actions
const actionTipVisible = ref(false);
const actionTipX = ref(0);
const actionTipY = ref(0);
const actionTipId = 'service-actions-tooltip';
const actionTipText = ref('');
const actionTipLeft = ref(false);
const LEFT_OFFSET = 220; // px, approximate tooltip width for left-side placement
const CURSOR_OFFSET = 12; // px gap from cursor
function onActionEnter(text: string, e: MouseEvent, placeLeft = false) {
  actionTipText.value = text;
  actionTipVisible.value = true;
  actionTipLeft.value = placeLeft;
  const x = placeLeft ? e.clientX - CURSOR_OFFSET - LEFT_OFFSET : e.clientX + CURSOR_OFFSET;
  actionTipX.value = Math.max(8, x);
  actionTipY.value = e.clientY + CURSOR_OFFSET;
}
function onActionMove(e: MouseEvent) {
  const x = actionTipLeft.value
    ? e.clientX - CURSOR_OFFSET - LEFT_OFFSET
    : e.clientX + CURSOR_OFFSET;
  actionTipX.value = Math.max(8, x);
  actionTipY.value = e.clientY + CURSOR_OFFSET;
}
function onActionLeave() {
  actionTipVisible.value = false;
}

function openDialog(type: 'test' | 'delete', connection: any) {
  pendingAction.value = type;
  pendingConnection.value = connection;
  dialogOpen.value = true;
}

function closeDependentsDialog() {
  dependentsDialogOpen.value = false;
}

function resetDependentsDialogState() {
  dependentsResponse.value = null;
}

watch(dependentsDialogOpen, (isOpen, wasOpen) => {
  if (!isOpen && wasOpen) resetDependentsDialogState();
});

function hasAnyDependents(resp: ConnectionDependentsResponse | null): boolean {
  if (!resp) return false;
  return (resp.integrations?.length ?? 0) > 0;
}

function openDependentsDialog(details: ConnectionDependentsResponse) {
  dependentsResponse.value = details;
  dependentsDialogOpen.value = true;
}

async function onDeleteClick(connection: any) {
  const id = connection?.id;
  if (!id) return;

  rowLoading.value[id] = true;
  try {
    const resp = await IntegrationConnectionService.getConnectionDependents({ connectionId: id });
    if (hasAnyDependents(resp)) {
      openDependentsDialog(resp);
      return;
    }

    openDialog('delete', connection);
  } catch {
    toast.showError('Failed to check connection dependents');
  } finally {
    rowLoading.value[id] = false;
  }
}

function closeDialog() {
  dialogOpen.value = false;
  pendingAction.value = null;
  pendingConnection.value = null;
}

function openSecretDialog(connection: any) {
  secretConnection.value = connection;
  secretValue.value = '';
  secretVisible.value = false;
  secretDialogOpen.value = true;
}

function closeSecretDialog() {
  secretDialogOpen.value = false;
  secretConnection.value = null;
  secretValue.value = '';
  secretVisible.value = false;
}

async function confirmSecretUpdate() {
  const id = secretConnection.value?.id;
  if (!id || !secretValue.value) return;

  rowLoading.value[id] = true;
  try {
    const success = await rotateSecret(id, secretValue.value);
    if (success) {
      closeSecretDialog();
    }
  } finally {
    rowLoading.value[id] = false;
  }
}

function refreshGrid() {
  gridRef.value?.instance?.refresh(true);
}

async function handleTestAction(id: string) {
  await testConnection(id);
  refreshGrid();
}

async function handleDeleteAction(id: string) {
  try {
    await deleteConnection(id);
    refreshGrid();
  } catch (e) {
    if (e instanceof ApiError && e.status === 409) {
      const details = e.body?.details as ConnectionDependentsResponse | undefined;
      if (details && hasAnyDependents(details)) {
        openDependentsDialog(details);
      } else {
        toast.showError('Unable to delete connection');
      }
      return;
    }

    toast.showError('Failed to delete connection');
  }
}

async function handleConfirm() {
  if (!pendingConnection.value) return;

  const id = pendingConnection.value.id;
  rowLoading.value[id] = true;

  try {
    switch (pendingAction.value) {
      case 'test':
        await handleTestAction(id);
        closeDialog();
        return;

      case 'delete':
        await handleDeleteAction(id);
        closeDialog();
        return;

      default:
        closeDialog();
    }
  } finally {
    rowLoading.value[id] = false;
  }
}

const gridColumns = [
  { dataField: 'name', caption: 'Connection Name' },
  { dataField: 'secretName', caption: 'Connection Key' },
  { dataField: 'lastConnectionStatus', caption: 'Last Connection Status', template: 'statusCell' },
  {
    dataField: 'lastConnectionTest',
    caption: 'Last Accessed',
    template: 'dateCell',
    dataType: 'date' as const,
    filterEditorOptions: { type: 'date' },
  },
  { caption: 'Actions', template: 'actionsCell', width: 200, allowFiltering: false },
];

function onGridOptionsChanged(e: any) {
  if (e.fullName === 'paging.pageIndex') {
    setPage(e.value);
  }
  if (e.fullName === 'paging.pageSize') {
    setRowsPerPage(e.value);
    setPage(0);
  }
}
</script>

<style scoped src="./ServiceConnectionPage.css"></style>
