<template>
  <div class="basic-details-tab">
    <div class="details-grid three-column">
      <!-- Info Card -->
      <div class="details-card info-card">
        <div class="card-header">
          <h3 class="card-title"><i class="dx-icon dx-icon-info title-icon"></i> Info</h3>
        </div>
        <div class="card-body">
          <div class="metadata-info">
            <div class="info-item">
              <span class="info-label">Created By</span
              ><span class="info-value">{{ integrationData?.createdBy || 'N/A' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Created</span
              ><span class="info-value">{{
                formatMetadataDate(integrationData?.createdDate)
              }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Last Modified By</span
              ><span class="info-value">{{ integrationData?.lastModifiedBy || 'N/A' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Last Modified</span
              ><span class="info-value">{{
                formatMetadataDate(integrationData?.lastModifiedDate)
              }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Configuration Card -->
      <div class="details-card configuration-card">
        <div class="card-header">
          <h3 class="card-title">
            <i class="dx-icon dx-icon-preferences title-icon"></i> Configuration
          </h3>
        </div>
        <div class="card-body">
          <div class="configuration-info">
            <div class="config-item">
              <span class="config-label">Integration Name</span>
              <div class="config-value name-value">{{ integrationData?.name || 'N/A' }}</div>
            </div>
            <div class="config-item">
              <span class="config-label">Description</span>
              <pre
                class="config-value description-value"
                :class="{ 'placeholder-text': !integrationData?.description }"
                >{{ integrationData?.description || 'No description provided' }}</pre
              >
            </div>
            <div class="config-row two-column">
              <div class="config-col">
                <div class="config-item">
                  <span class="config-label">Item Type</span>
                  <div class="config-value">
                    {{ capitalizeFirst(integrationData?.itemType || '') }}
                  </div>
                </div>
              </div>
              <div class="config-col">
                <div class="config-item">
                  <span class="config-label">Item Subtype</span>
                  <div class="config-value">
                    {{ integrationData?.itemSubtypeLabel || integrationData?.itemSubtype || 'N/A' }}
                  </div>
                </div>
              </div>
            </div>
            <div v-if="integrationData?.dynamicDocumentType" class="config-item">
              <span class="config-label">Dynamic Document Type</span>
              <div class="config-value">
                {{
                  integrationData.dynamicDocumentTypeLabel || integrationData.dynamicDocumentType
                }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Manage Actions Card -->
      <EntityManageActions :actions="manageActions" title="Manage" @action="handleActionClick" />
    </div>

    <div v-if="!loading && !integrationData" class="empty-state">
      <i class="dx-icon dx-icon-info empty-icon"></i>
      <h3>No Integration Data Found</h3>
      <p>Integration details are not available.</p>
    </div>

    <ConfirmationDialog
      :open="dialogOpen"
      :type="pendingAction || 'custom'"
      :title="dialogTitle"
      :description="dialogDescription"
      :confirm-label="dialogConfirmLabel"
      :loading="actionLoading"
      @cancel="closeDialog"
      @confirm="handleConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import type { PropType } from 'vue';
import { ref, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import EntityManageActions from '@/components/common/EntityManageActions.vue';
import { useConfirmationDialog, type DialogConfigMap } from '@/composables/useConfirmationDialog';
import {
  useConfluenceIntegrationActions,
  useConfluenceIntegrationStatus,
} from '@/composables/useConfluenceIntegrationActions';
import { useToastStore } from '@/store/toast';
import type { ConfluenceIntegrationResponse } from '@/api/models/ConfluenceIntegrationResponse';
import type { ActionDefinition } from '@/components/common/EntityManageActions.vue';
import { formatMetadataDate } from '@/utils/dateUtils';
import { capitalizeFirst } from '@/utils/stringFormatUtils';

const props = defineProps({
  integrationData: {
    type: Object as PropType<ConfluenceIntegrationResponse | null>,
    default: null,
  },
  integrationId: { type: String, required: true },
  loading: { type: Boolean, default: false },
});

const emit = defineEmits<{
  (e: 'refresh'): void;
  (e: 'status-updated', enabled: boolean): void;
  (e: 'edit-requested'): void;
  (e: 'clone-requested'): void;
}>();

const router = useRouter();
const toast = useToastStore();
const { deleteIntegration } = useConfluenceIntegrationActions();
const { toggleIntegrationStatus } = useConfluenceIntegrationStatus();

const localIntegration = ref<ConfluenceIntegrationResponse | null>(props.integrationData ?? null);
watch(
  () => props.integrationData,
  v => {
    localIntegration.value = v ?? null;
  }
);

const confluenceDialogConfig: DialogConfigMap = {
  enable: {
    title: 'Enable Confluence Integration',
    desc: 'Enabling this integration will allow scheduled or manual runs.',
    label: 'Enable',
  },
  disable: {
    title: 'Disable Confluence Integration',
    desc: 'Disabling this integration will prevent runs until re-enabled.',
    label: 'Disable',
  },
  delete: {
    title: 'Delete Confluence Integration',
    desc: 'Deleting this integration will remove it permanently. This action cannot be undone.',
    label: 'Delete',
  },
};

const {
  dialogOpen,
  actionLoading,
  pendingAction,
  dialogTitle,
  dialogDescription,
  dialogConfirmLabel,
  openDialog,
  closeDialog,
  confirmWithHandlers,
} = useConfirmationDialog(confluenceDialogConfig);

async function handleConfirm(): Promise<void> {
  await confirmWithHandlers({
    enable: async () => {
      await handleToggleIntegration();
    },
    disable: async () => {
      await handleToggleIntegration();
    },
    delete: async () => handleDeleteIntegration(),
  });
}

async function handleToggleIntegration(): Promise<boolean> {
  if (!props.integrationId) {
    toast.showError('Integration id missing');
    return false;
  }
  const newStatus = await toggleIntegrationStatus(
    props.integrationId,
    localIntegration.value?.isEnabled
  );
  if (newStatus !== null) {
    if (localIntegration.value) localIntegration.value.isEnabled = newStatus;
    emit('status-updated', newStatus);
    return true;
  }
  return false;
}

async function handleDeleteIntegration(): Promise<boolean> {
  if (!props.integrationId) {
    toast.showError('Integration id missing');
    return false;
  }
  const ok = await deleteIntegration(props.integrationId);
  if (ok) router.back();
  return ok;
}

function handleActionClick(actionId: string): void {
  switch (actionId) {
    case 'edit':
      emit('edit-requested');
      break;
    case 'clone':
      emit('clone-requested');
      break;
    case 'toggle':
      openDialog(localIntegration.value?.isEnabled ? 'disable' : 'enable');
      break;
    case 'delete':
      openDialog('delete');
      break;
    default:
      break;
  }
}

const manageActions = computed<ActionDefinition[]>(() => [
  {
    id: 'edit',
    label: 'Edit Integration',
    icon: 'edit',
    iconType: 'devextreme',
    variant: 'secondary',
  },
  {
    id: 'clone',
    label: 'Clone Integration',
    icon: 'copy',
    iconType: 'devextreme',
    variant: 'secondary',
  },
  {
    id: 'toggle',
    label: localIntegration.value?.isEnabled ? 'Disable Integration' : 'Enable Integration',
    icon: localIntegration.value?.isEnabled ? 'cursorprohibition' : 'video',
    iconType: 'devextreme',
    variant: 'secondary',
  },
  {
    id: 'delete',
    label: 'Delete Integration',
    icon: 'trash',
    iconType: 'devextreme',
    variant: 'danger',
  },
]);
</script>

<style src="@/components/outbound/arcgisintegration/details/ArcGISDetailsTab.css" scoped></style>
