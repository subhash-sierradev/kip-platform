<template>
  <div class="siteconfig-page">
    <div class="site-config-container">
      <div class="table-wrapper">
        <GenericDataGrid
          ref="siteConfigGridRef"
          :data="siteConfigsForGrid"
          :columns="gridColumns"
          :page-size="10"
          :enable-export="false"
          :enable-clear-filters="true"
        >
          <template #valueTemplate="{ data }">
            <span v-if="data.type === 'TIMESTAMP'" class="timestamp-value">
              {{ formatDate(data.configValue, { includeTime: true, format: 'short' }) }}
            </span>
            <span v-else>{{ data.configValue }}</span>
          </template>
          <template #typeTemplate="{ data }">
            <StatusChipForDataTable :status="data.type" :label="data.type" />
          </template>
          <template #configTypeTemplate="{ data }">
            {{ data.configType }}
          </template>
          <template #dateTemplate="{ data }">
            {{ formatDate(data.lastModifiedDate, { includeTime: true, format: 'short' }) }}
          </template>
          <template #actionTemplate="{ data }">
            <DxButton
              icon="edit"
              styling-mode="text"
              type="normal"
              :hint="'Edit ' + data.configKey"
              :element-attr="{ 'aria-label': 'Edit' }"
              @click="handleEditConfiguration(data)"
            />
            <DxButton
              v-if="!TenantTypeUtil.isGlobal(data.tenantId)"
              icon="trash"
              styling-mode="text"
              type="normal"
              :hint="'Delete Configuration'"
              :element-attr="{ 'aria-label': 'Delete' }"
              @click="handleDeleteConfiguration(data)"
            />
          </template>
        </GenericDataGrid>
      </div>
    </div>
    <SiteConfigModal
      :show="openModal"
      :is-saving="isSaving"
      :save-error="null"
      :form-data="formData"
      @close="handleModalClose"
      @save="handleSaveChanges"
      @update:id="formData.id = $event"
      @update:type="formData.type = $event"
      @update:value="formData.value = $event"
      @update:description="formData.description = $event"
    />
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
import { computed, ref, onMounted, reactive } from 'vue';
import { SettingsService } from '../../../api/services/SettingsService';
import { OpenAPI } from '../../../api/core/OpenAPI';
import { request as __request } from '../../../api/core/request';
import Alert from '../../../utils/notificationUtils';
import SiteConfigModal from './SiteConfigModal.vue';
import ConfirmationDialog from '../../common/ConfirmationDialog.vue';
import StatusChipForDataTable from '@/components/common/StatusChipForDataTable.vue';
import { useConfirmationDialog } from '../../../composables/useConfirmationDialog';
import './SiteConfigPage.css';
import { DxButton } from 'devextreme-vue/button';
import GenericDataGrid from '../../common/GenericDataGrid.vue';
import { formatDate } from '../../../utils/dateUtils';
import type { ConfigType, SiteConfig } from '../../../api/models/SiteConfig';
import { TenantTypeUtil } from '../../../types/TenantType';

const siteConfigs = ref<SiteConfig[]>([]);
const loading = ref(false);
const error = ref<string | null>(null);

const editingConfig = ref<SiteConfig | null>(null);
const isSaving = ref(false);
const openModal = ref(false);

// Confirmation dialog state
const siteConfigDialogConfig = {
  delete: {
    title: 'Delete Configuration',
    desc: 'Deleting this configuration will remove it permanently. This action cannot be undone.',
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
} = useConfirmationDialog(siteConfigDialogConfig);
const configToDelete = ref<SiteConfig | null>(null);

const formData = reactive({
  id: '',
  type: 'STRING' as ConfigType,
  value: '',
  description: '',
});

const siteConfigsForGrid = computed(() => {
  return siteConfigs.value.map(config => ({
    ...config,
    configType: TenantTypeUtil.isGlobal(config.tenantId) ? 'GLOBAL' : 'CUSTOM',
  }));
});

const resetFormData = (): void => {
  formData.id = '';
  formData.type = 'STRING';
  formData.value = '';
  formData.description = '';
};

const fetchSiteConfigs = async (): Promise<void> => {
  loading.value = true;
  error.value = null;

  try {
    const configs = await SettingsService.listSiteConfigs();
    siteConfigs.value = configs;
  } catch {
    const errorMessage = 'Failed to load site configurations';
    error.value = errorMessage;
    Alert.error('Failed to load site configurations. Please try again.');
  } finally {
    loading.value = false;
  }
};

const handleEditConfiguration = (config: SiteConfig): void => {
  editingConfig.value = config;
  formData.id = config.configKey;
  formData.type = config.type;
  formData.value = config.configValue;
  formData.description = config.description || '';
  openModal.value = true;
};

const handleModalClose = (): void => {
  openModal.value = false;
  editingConfig.value = null;
  isSaving.value = false;
  resetFormData();
};

const handleDeleteConfiguration = (config: SiteConfig): void => {
  configToDelete.value = config;
  openDialog('delete');
};

const handleConfirm = async (): Promise<void> => {
  await confirmWithHandlers({
    delete: async () => {
      if (!configToDelete.value) {
        return false;
      }
      try {
        await __request(OpenAPI, {
          method: 'DELETE',
          url: `/management/site-configs/${configToDelete.value.id}`,
        });
        await fetchSiteConfigs();
        Alert.success('Configuration deleted successfully');
        configToDelete.value = null;
        return true;
      } catch {
        Alert.error('Failed to delete configuration. Please try again.');
        return false;
      }
    },
  });
};

const validateRequiredFields = (): boolean => {
  if (!formData.id?.trim() || !formData.value?.trim() || !formData.type) {
    const message = 'Please fill in all required fields';
    Alert.warning(message);
    return false;
  }
  return true;
};

const validateJsonFormat = (): boolean => {
  if (formData.type !== 'JSON') return true;

  try {
    JSON.parse(formData.value);
    return true;
  } catch {
    const message = 'Invalid JSON format. Please enter valid JSON.';
    Alert.warning(message);
    return false;
  }
};

const siteConfigGridRef = ref();

const gridColumns = [
  { dataField: 'configKey', caption: 'Key', minWidth: 180 },
  {
    dataField: 'configValue',
    caption: 'Value',
    template: 'valueTemplate',
    minWidth: 150,
    allowFiltering: false,
  },
  { dataField: 'description', caption: 'Description', minWidth: 200 },
  { dataField: 'type', caption: 'Type', template: 'typeTemplate', minWidth: 100 },
  {
    dataField: 'configType',
    caption: 'Config Type',
    template: 'configTypeTemplate',
    minWidth: 120,
    allowSorting: true,
    allowFiltering: true,
    headerFilter: {
      dataSource: [
        { text: 'GLOBAL', value: 'GLOBAL' },
        { text: 'CUSTOM', value: 'CUSTOM' },
      ],
    },
  },
  {
    dataField: 'lastModifiedDate',
    caption: 'Last Modified',
    template: 'dateTemplate',
    minWidth: 180,
    dataType: 'date' as const,
    filterEditorOptions: { type: 'date' },
  },
  { caption: 'Actions', template: 'actionTemplate', minWidth: 120, allowFiltering: false },
];

const validateBooleanFormat = (): boolean => {
  if (formData.type !== 'BOOLEAN') return true;

  const lowerValue = String(formData.value).toLowerCase();
  if (lowerValue !== 'true' && lowerValue !== 'false') {
    const message = 'Boolean value must be either "true" or "false".';
    Alert.warning(message);
    return false;
  }
  return true;
};

const validateNumberFormat = (): boolean => {
  if (formData.type !== 'NUMBER') return true;

  if (isNaN(Number(formData.value))) {
    const message = 'Invalid number format. Please enter a valid number.';
    Alert.warning(message);
    return false;
  }
  return true;
};

const validateTimestampFormat = (): boolean => {
  if (formData.type !== 'TIMESTAMP') return true;

  // ISO-8601 UTC format validation
  const iso8601Regex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?Z$/;
  if (!iso8601Regex.test(formData.value)) {
    const message =
      'Invalid timestamp format. Please use ISO-8601 UTC format (e.g., 2026-01-01T00:00:00Z).';
    Alert.warning(message);
    return false;
  }

  // Validate it's a parseable date
  try {
    const parsed = new Date(formData.value);
    if (isNaN(parsed.getTime())) {
      throw new Error('Invalid date');
    }

    // Optional: warn if timestamp is in the future
    if (parsed.getTime() > Date.now()) {
      Alert.warning('Warning: The timestamp is set in the future.');
    }
  } catch {
    const message = 'Invalid timestamp value. Please enter a valid date.';
    Alert.warning(message);
    return false;
  }
  return true;
};

const validateUniqueKey = (): boolean => {
  // Ensure that the configuration key is unique among all configurations
  const duplicateExists = siteConfigs.value.some(config => {
    // Skip the currently edited configuration when checking for duplicates
    if (editingConfig.value && config.id === editingConfig.value.id) {
      return false;
    }
    // Check for duplicate key within the same tenant context
    return config.configKey === formData.id;
  });

  if (duplicateExists) {
    const message = 'A configuration with this key already exists. Please use a unique key.';
    Alert.warning(message);
    return false;
  }
  return true;
};

const validateForm = (): boolean => {
  return (
    validateRequiredFields() &&
    validateUniqueKey() &&
    validateJsonFormat() &&
    validateBooleanFormat() &&
    validateNumberFormat() &&
    validateTimestampFormat()
  );
};

const handleSaveChanges = async (): Promise<void> => {
  if (!validateForm()) {
    return;
  }

  isSaving.value = true;

  try {
    const payload = {
      configKey: formData.id,
      configValue: formData.value,
      type: formData.type,
    };

    // Update existing configuration
    await __request(OpenAPI, {
      method: 'PUT',
      url: `/management/site-configs/${editingConfig.value?.id}`,
      body: payload,
    });

    await fetchSiteConfigs();
    handleModalClose();
    const isOverride = editingConfig.value?.tenantId === 'GLOBAL';
    Alert.success(
      isOverride ? 'Configuration overridden successfully' : 'Configuration updated successfully'
    );
  } catch (err: unknown) {
    const errorMessage =
      (err as { response?: { data?: { message?: string } } }).response?.data?.message ||
      'Failed to update configuration. Please try again.';
    Alert.error(errorMessage);
  } finally {
    isSaving.value = false;
  }
};

// Lifecycle hooks
onMounted(() => {
  fetchSiteConfigs();
});

// Expose methods for testing
defineExpose({
  handleEditConfiguration,
  handleDeleteConfiguration,
  dialogOpen,
  pendingAction,
  configToDelete,
  closeDialog,
  dialogTitle,
  dialogDescription,
  dialogConfirmLabel,
});
</script>
<style src="./SiteConfigPage.css"></style>
