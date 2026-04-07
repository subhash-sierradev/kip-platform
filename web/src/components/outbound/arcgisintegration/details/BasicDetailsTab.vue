<template>
  <div class="basic-details-tab">
    <div class="details-grid three-column">
      <!-- Integration Info Card -->
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
                  <div class="config-value">{{ integrationData?.itemSubtypeLabel }}</div>
                </div>
              </div>
            </div>
            <div v-if="integrationData?.dynamicDocumentType" class="config-item">
              <span class="config-label">Dynamic Document Type</span>
              <div class="config-value">{{ dynamicDocumentTypeDisplay }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Manage ArcGIS Integration Card -->
      <EntityManageActions :actions="manageActions" title="Manage" @action="handleActionClick" />
    </div>

    <div v-if="!loading && !integrationData" class="empty-state">
      <i class="dx-icon dx-icon-info empty-icon"></i>
      <h3>No Integration Data Found</h3>
      <p>Integration details are not available.</p>
    </div>

    <!-- Confirmation Dialog -->
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
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import EntityManageActions from '@/components/common/EntityManageActions.vue';
import { useConfirmationDialog, type DialogConfigMap } from '@/composables/useConfirmationDialog';
import {
  useArcGISIntegrationActions,
  useArcGISIntegrationStatus,
  useArcGISIntegrationTrigger,
} from '@/composables/useArcGISIntegrationActions';
import { useToastStore } from '@/store/toast';
import { useRouter } from 'vue-router';
import type { ArcGISIntegrationResponse } from '@/api/models/ArcGISIntegrationResponse';
import type { ActionDefinition } from '@/components/common/EntityManageActions.vue';
import { formatMetadataDate } from '@/utils/dateUtils';
import { capitalizeFirst } from '@/utils/stringFormatUtils';
import { Zap } from 'lucide-vue-next';
import { KwDocService } from '@/api/services/KwIntegrationService';

const props = defineProps({
  integrationData: { type: Object as PropType<ArcGISIntegrationResponse | null>, default: null },
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
const { triggerJobExecution } = useArcGISIntegrationTrigger();

const localIntegration = ref<ArcGISIntegrationResponse | null>(props.integrationData ?? null);
watch(
  () => props.integrationData,
  v => {
    localIntegration.value = v ?? null;
  }
);

const DYNAMIC_SUBTYPES = new Set(['DOCUMENT_DRAFT_DYNAMIC', 'DOCUMENT_FINAL_DYNAMIC']);
const resolvedDynamicDocumentLabel = ref<string>('');

const dynamicDocumentTypeDisplay = computed(() => {
  const data = props.integrationData;
  if (!data?.dynamicDocumentType) return '';

  // Prefer backend-provided label.
  if (data.dynamicDocumentTypeLabel) return data.dynamicDocumentTypeLabel;

  // Fall back to resolved label (looked up by id) or the raw value.
  return resolvedDynamicDocumentLabel.value || data.dynamicDocumentType;
});

async function resolveDynamicDocumentLabelIfNeeded(data: ArcGISIntegrationResponse | null) {
  resolvedDynamicDocumentLabel.value = '';
  if (!data?.dynamicDocumentType) return;
  if (data.dynamicDocumentTypeLabel) return;
  if (!data.itemSubtype || !DYNAMIC_SUBTYPES.has(data.itemSubtype)) return;

  try {
    const docs = await KwDocService.getDynamicDocuments('DOCUMENT', data.itemSubtype);
    const match = Array.isArray(docs)
      ? docs.find(d => d.id === data.dynamicDocumentType)
      : undefined;
    resolvedDynamicDocumentLabel.value = match?.title || '';
  } catch {
    // Swallow: UI will fall back to showing raw id.
    resolvedDynamicDocumentLabel.value = '';
  }
}

watch(
  () => props.integrationData,
  data => {
    resolveDynamicDocumentLabelIfNeeded(data ?? null);
  },
  { immediate: true }
);

const { deleteIntegration } = useArcGISIntegrationActions();
const { toggleIntegrationStatus } = useArcGISIntegrationStatus();

const arcgisDialogConfig: DialogConfigMap = {
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
  openDialog,
  closeDialog,
  confirmWithHandlers,
} = useConfirmationDialog(arcgisDialogConfig);

async function handleConfirm() {
  await confirmWithHandlers({
    enable: async () => {
      await handleToggleIntegration();
    },
    disable: async () => {
      await handleToggleIntegration();
    },
    delete: async () => {
      return await handleDeleteIntegration();
    },
    runNow: async () => {
      await handleTriggerIntegration();
    },
  });
}

async function handleToggleIntegration(): Promise<boolean> {
  if (!props.integrationId) {
    toast.showError('Integration id missing');
    return false;
  }
  const currentEnabled = localIntegration.value?.isEnabled;
  const newStatus = await toggleIntegrationStatus(props.integrationId, currentEnabled);

  if (newStatus !== null) {
    // Update local integration data
    if (localIntegration.value) {
      localIntegration.value.isEnabled = newStatus;
    }
    // Emit event for parent to update its data
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
  if (ok) {
    router.back();
  }
  return ok;
}

function handleEditIntegration(): void {
  emit('edit-requested');
}

function handleCloneIntegration(): void {
  emit('clone-requested');
}

async function handleTriggerIntegration(): Promise<void> {
  await triggerJobExecution(props.integrationId, props.integrationData?.name);
}

// Computed actions for EntityManageActions
const manageActions = computed<ActionDefinition[]>(() => [
  {
    id: 'run',
    label: 'Run Now',
    iconType: 'lucide',
    lucideIcon: Zap,
    variant: 'secondary',
    disabled: !localIntegration.value?.isEnabled,
  },
  {
    id: 'edit',
    label: 'Edit Integration',
    icon: 'edit',
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
    id: 'clone',
    label: 'Clone Integration',
    icon: 'copy',
    iconType: 'devextreme',
    variant: 'secondary',
  },
  {
    id: 'delete',
    label: 'Delete Integration',
    icon: 'trash',
    iconType: 'devextreme',
    variant: 'secondary',
  },
]);

function handleActionClick(actionId: string): void {
  switch (actionId) {
    case 'run':
      openDialog('runNow');
      break;
    case 'edit':
      handleEditIntegration();
      break;
    case 'toggle':
      openDialog(localIntegration.value?.isEnabled ? 'disable' : 'enable');
      break;
    case 'clone':
      handleCloneIntegration();
      break;
    case 'delete':
      openDialog('delete');
      break;
  }
}
</script>

<style src="./ArcGISDetailsTab.css" scoped></style>
