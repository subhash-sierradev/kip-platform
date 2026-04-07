<template>
  <div class="webhook-details-tab">
    <!-- Three-Column Details Grid -->
    <div class="details-grid three-column">
      <!-- Webhook Info Card (25%) -->
      <div class="details-card webhook-info-card">
        <div class="card-header">
          <h3 class="card-title">
            <i class="dx-icon dx-icon-info title-icon"></i>
            Info
          </h3>
        </div>
        <div class="card-body">
          <div class="metadata-info">
            <div class="info-item">
              <span class="info-label">Created By</span>
              <span class="info-value">{{ metadataFormData.createdBy }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Created</span>
              <span class="info-value">{{ metadataFormData.createdDate }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Last Modified By</span>
              <span class="info-value">{{ metadataFormData.lastModifiedBy }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">Last Modified</span>
              <span class="info-value">{{ metadataFormData.lastModifiedDate }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Configuration Card (50%) -->
      <div class="details-card configuration-card">
        <div class="card-header">
          <h3 class="card-title">
            <i class="dx-icon dx-icon-preferences title-icon"></i>
            Configuration
          </h3>
        </div>
        <div class="card-body">
          <div class="configuration-info">
            <div class="config-item">
              <span class="config-label">Webhook Name</span>
              <div class="config-value name-value">{{ configurationFormData.name }}</div>
            </div>
            <div class="config-item">
              <span class="config-label">Description</span>
              <pre
                class="config-value description-value"
                :class="{ 'placeholder-text': !localWebhook?.description }"
                >{{ configurationFormData.description }}</pre
              >
            </div>
            <div class="config-item">
              <span class="config-label">Webhook URL</span>
              <div class="url-container">
                <div class="config-value url-value">{{ configurationFormData.webhookUrl }}</div>
                <button class="copy-url-btn" @click="copyToClipboard" title="Copy URL to clipboard">
                  <i class="dx-icon dx-icon-copy"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Manage Webhook Card (25%) -->
      <div class="details-card actions-card">
        <EntityManageActions :actions="manageActions" @action="handleActionClick" />
      </div>
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

    <!-- Edit/Clone Wizard -->
    <JiraWebhookWizard
      v-if="wizardOpen"
      :open="wizardOpen"
      :editMode="editMode"
      :editingWebhookData="editingWebhookData || undefined"
      :cloneMode="cloneMode"
      :cloningWebhookData="cloningWebhookData || undefined"
      @close="wizardOpen = false"
      @webhook-created="
        () => {
          wizardOpen = false;
          emit('refresh');
        }
      "
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { formatMetadataDate } from '@/utils/dateUtils';
import { type JiraWebhookDetail } from '@/api/services/JiraWebhookService';
import { useToastStore } from '@/store/toast';
import { useRouter } from 'vue-router';
import './WebhookDetailsTab.css';
import JiraWebhookWizard from '@/components/outbound/jirawebhooks/wizard/JiraWebhookWizard.vue';
import { Zap } from 'lucide-vue-next';

// DevExtreme imports
import ConfirmationDialog from '@/components/common/ConfirmationDialog.vue';
import EntityManageActions from '@/components/common/EntityManageActions.vue';
import type { ActionDefinition } from '@/components/common/EntityManageActions.vue';
import {
  useConfirmationDialog,
  createDefaultDialogConfig,
} from '@/composables/useConfirmationDialog';
import { useWebhookActions } from '@/composables/useWebhookActions';

interface Props {
  webhookId: string;
  webhookData?: JiraWebhookDetail | null;
  loading?: boolean;
}

const props = defineProps<Props>();
// Emits to notify parent of status changes or need to refresh data
const emit = defineEmits<{ (e: 'status-updated', enabled: boolean): void; (e: 'refresh'): void }>();
const router = useRouter();

// Local reactive copy to reflect status changes immediately
const localWebhook = ref<JiraWebhookDetail | null>(props.webhookData ?? null);
watch(
  () => props.webhookData,
  v => {
    localWebhook.value = v ?? null;
  }
);

// Test webhook loading state
const testLoading = ref(false);

// Initialize toast store
const toastStore = useToastStore();
// Shared webhook actions composable (instantiate at setup level)
const { toggleWebhookStatus, deleteWebhook, testWebhook } = useWebhookActions();

// Configure manage actions for EntityManageActions component
const manageActions = computed<ActionDefinition[]>(() => [
  {
    id: 'test',
    label: 'Test Webhook',
    iconType: 'lucide',
    lucideIcon: Zap,
    variant: 'secondary',
    disabled: !localWebhook.value?.isEnabled || testLoading.value,
    description: 'Test webhook functionality',
  },
  {
    id: 'edit',
    label: 'Edit Webhook',
    icon: 'edit',
    iconType: 'devextreme',
    variant: 'secondary',
    disabled: false,
    description: 'Edit webhook configuration',
  },
  {
    id: 'toggle',
    label: localWebhook.value?.isEnabled ? 'Disable Webhook' : 'Enable Webhook',
    icon: localWebhook.value?.isEnabled ? 'cursorprohibition' : 'video',
    iconType: 'devextreme',
    variant: 'secondary',
    disabled: false,
    description: localWebhook.value?.isEnabled ? 'Disable webhook' : 'Enable webhook',
  },
  {
    id: 'clone',
    label: 'Clone Webhook',
    icon: 'copy',
    iconType: 'devextreme',
    variant: 'secondary',
    disabled: false,
    description: 'Create a copy of this webhook',
  },
  {
    id: 'delete',
    label: 'Delete Webhook',
    icon: 'trash',
    iconType: 'devextreme',
    variant: 'secondary',
    disabled: false,
    description: 'Permanently delete this webhook',
  },
]);

// Handle action clicks from EntityManageActions
const handleActionClick = (actionId: string) => {
  switch (actionId) {
    case 'test':
      handleTestWebhook();
      break;
    case 'edit':
      handleEditWebhook();
      break;
    case 'toggle':
      openDialog(localWebhook.value?.isEnabled ? 'disable' : 'enable');
      break;
    case 'clone':
      handleCloneWebhook();
      break;
    case 'delete':
      openDialog('delete');
      break;
    default:
      console.warn(`Unknown action: ${actionId}`);
  }
};

// Computed form data for metadata section
const metadataFormData = computed(() => ({
  createdBy: localWebhook.value?.createdBy || 'N/A',
  createdDate: formatMetadataDate(localWebhook.value?.createdDate),
  lastModifiedBy: localWebhook.value?.lastModifiedBy || 'N/A',
  lastModifiedDate: formatMetadataDate(localWebhook.value?.lastModifiedDate),
}));

// Computed form data for configuration section
const configurationFormData = computed(() => ({
  name: localWebhook.value?.name || 'N/A',
  description: localWebhook.value?.description || 'No description provided',
  webhookUrl: localWebhook.value?.webhookUrl || 'N/A',
}));

async function copyToClipboard(): Promise<void> {
  const url = localWebhook.value?.webhookUrl || '';
  if (!url || url === 'N/A') {
    toastStore.showWarning('No webhook URL available to copy');
    return;
  }

  try {
    await navigator.clipboard.writeText(url);
    toastStore.showSuccess('Webhook URL successfully copied to clipboard');
  } catch (err) {
    console.error('Failed to copy URL:', err);
    toastStore.showError('Failed to copy webhook URL to clipboard');
  }
}

async function handleToggleStatus(): Promise<void> {
  if (!props.webhookId) {
    toastStore.showError('Invalid webhook id');
    return;
  }
  try {
    const newEnabled = await toggleWebhookStatus(props.webhookId, localWebhook.value?.isEnabled);
    if (newEnabled === null) {
      return;
    }
    if (localWebhook.value) {
      localWebhook.value = { ...localWebhook.value, isEnabled: newEnabled };
    }
    // Notify parent so it can refetch or update its own cached webhookData
    emit('status-updated', newEnabled);
    emit('refresh');
  } catch (error) {
    toastStore.showError('Failed to update webhook status');
    console.error('Failed to toggle webhook status:', error);
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
  openDialog,
  closeDialog,
  confirmWithHandlers,
} = useConfirmationDialog(createDefaultDialogConfig());

async function handleConfirm() {
  await confirmWithHandlers({
    delete: async () => handleDeleteWebhook(),
    enable: async () => handleToggleStatus(),
    disable: async () => handleToggleStatus(),
  });
}

// Wizard state for edit/clone
const wizardOpen = ref(false);
const editMode = ref(false);
const cloneMode = ref(false);
const editingWebhookData = ref<JiraWebhookDetail | null>(null);
const cloningWebhookData = ref<JiraWebhookDetail | null>(null);

function handleCloneWebhook(): void {
  if (!localWebhook.value) {
    toastStore.showError('Webhook details not loaded');
    return;
  }
  editMode.value = false;
  cloneMode.value = true;
  editingWebhookData.value = null;
  cloningWebhookData.value = { ...localWebhook.value } as JiraWebhookDetail;
  wizardOpen.value = true;
}

async function handleDeleteWebhook(): Promise<boolean> {
  const ok = await deleteWebhook(props.webhookId);
  if (ok) {
    router.back();
  }
  return ok;
}

async function handleTestWebhook(): Promise<void> {
  if (!localWebhook.value) {
    toastStore.showError('Webhook details not loaded');
    return;
  }

  if (!localWebhook.value.isEnabled) {
    toastStore.showWarning('Cannot test a disabled webhook. Please enable it first.');
    return;
  }

  testLoading.value = true;
  try {
    // Use the webhook's sample payload or provide a default Jira webhook payload
    const samplePayload = localWebhook.value.samplePayload || getDefaultJiraWebhookPayload();

    // Call with both parameters
    const result = await testWebhook(props.webhookId, samplePayload);

    if (result.success) {
      toastStore.showSuccess(
        'Webhook test completed successfully! Check the webhook history for execution details.'
      );
    } else {
      toastStore.showError(`Webhook test failed: ${result.errorMessage || 'Unknown error'}`);
    }
  } catch (error) {
    console.error('Webhook test error:', error);
    toastStore.showError('Failed to test webhook. Please try again.');
  } finally {
    testLoading.value = false;
  }
}

function getDefaultJiraWebhookPayload(): string {
  // Default Jira webhook payload structure for testing
  return JSON.stringify(
    {
      timestamp: Date.now(),
      webhookEvent: 'jira:issue_updated',
      issue_event_type_name: 'issue_updated',
      user: {
        self: 'https://test-jira.atlassian.net/rest/api/2/user?username=test-user',
        name: 'test-user',
        key: 'test-user',
        emailAddress: 'test@example.com',
        displayName: 'Test User',
        active: true,
      },
      issue: {
        id: '12345',
        self: 'https://test-jira.atlassian.net/rest/api/2/issue/12345',
        key: 'TEST-123',
        fields: {
          summary: 'Sample test issue for webhook validation',
          description: 'This is a test issue created for webhook testing purposes',
          issuetype: {
            self: 'https://test-jira.atlassian.net/rest/api/2/issuetype/1',
            id: '1',
            name: 'Bug',
            iconUrl: 'https://test-jira.atlassian.net/images/icons/issuetypes/bug.png',
          },
          priority: {
            self: 'https://test-jira.atlassian.net/rest/api/2/priority/3',
            iconUrl: 'https://test-jira.atlassian.net/images/icons/priorities/medium.svg',
            name: 'Medium',
            id: '3',
          },
          status: {
            self: 'https://test-jira.atlassian.net/rest/api/2/status/1',
            iconUrl: 'https://test-jira.atlassian.net/images/icons/statuses/open.png',
            name: 'Open',
            id: '1',
          },
        },
      },
      changelog: {
        id: '67890',
        items: [
          {
            field: 'summary',
            fieldtype: 'jira',
            from: null,
            fromString: 'Old summary',
            to: null,
            toString: 'Sample test issue for webhook validation',
          },
        ],
      },
    },
    null,
    2
  );
}

function handleEditWebhook(): void {
  if (!localWebhook.value) {
    toastStore.showError('Webhook details not loaded');
    return;
  }
  editMode.value = true;
  cloneMode.value = false;
  editingWebhookData.value = { ...localWebhook.value } as JiraWebhookDetail;
  cloningWebhookData.value = null;
  wizardOpen.value = true;
}
</script>

<script lang="ts">
export default {
  name: 'WebhookDetailsTab',
};
</script>
