import { computed, reactive, ref } from 'vue';

import { createDefaultFormData } from '@/components/outbound/confluenceintegration/wizard/confluenceWizardConfig';
import type { ScheduleConfigurationData } from '@/types/ArcGISFormData';
import type { ConfluencePageConfigData } from '@/types/ConfluenceFormData';
import type { ConnectionStepData } from '@/types/ConnectionStepData';
import type { UnifiedIntegrationStepData } from '@/types/UnifiedIntegrationStep';

/* eslint max-lines-per-function: ["error", { "max": 160 }] */
export function useConfluenceWizardState() {
  const formData = reactive(createDefaultFormData());

  /**
   * Temporarily holds the Confluence Site URL entered in Step 3 (Connection).
   * The base URL is stored in the connection's Key Vault secret.
   */
  const pendingConnectionBaseUrl = ref('');

  const stepValidation = reactive<Record<number, boolean>>({
    1: false,
    2: false,
    3: false,
    4: false,
    5: true,
  });

  // ── Computed step data ────────────────────────────────────────────────
  const unifiedIntegrationStepData = computed<UnifiedIntegrationStepData>(() => ({
    name: formData.name,
    description: formData.description,
    itemType: formData.itemType,
    subType: formData.subType,
    subTypeLabel: formData.subTypeLabel,
    dynamicDocument: formData.dynamicDocument,
    dynamicDocumentLabel: formData.dynamicDocumentLabel,
  }));

  const scheduleData = computed<ScheduleConfigurationData>(() => ({
    executionDate: formData.executionDate,
    executionTime: formData.executionTime,
    frequencyPattern: formData.frequencyPattern,
    dailyFrequency: formData.dailyFrequency,
    selectedDays: formData.selectedDays,
    selectedMonths: formData.selectedMonths,
    isExecuteOnMonthEnd: formData.isExecuteOnMonthEnd,
    cronExpression: formData.cronExpression,
    businessTimeZone: formData.businessTimeZone,
    timeCalculationMode: formData.timeCalculationMode,
  }));

  const pageConfigData = computed<ConfluencePageConfigData>(() => ({
    confluenceSpaceKey: formData.confluenceSpaceKey,
    confluenceSpaceLabel: formData.confluenceSpaceLabel,
    confluenceSpaceKeyFolderKey: formData.confluenceSpaceKeyFolderKey,
    confluenceSpaceFolderLabel: formData.confluenceSpaceFolderLabel,
    languageCodes: formData.languageCodes,
    reportNameTemplate: formData.reportNameTemplate,
    includeTableOfContents: formData.includeTableOfContents,
  }));

  const genericConnectionData = computed<ConnectionStepData>(() => ({
    connectionMethod: formData.connectionMethod || 'existing',
    baseUrl: pendingConnectionBaseUrl.value || '',
    credentialType: 'BASIC_AUTH',
    username: formData.username,
    password: formData.password,
    connectionName: formData.connectionName,
    connected: !!(formData.existingConnectionId || formData.createdConnectionId),
    existingConnectionId: formData.existingConnectionId,
    createdConnectionId: formData.createdConnectionId,
  }));

  const activeConnectionId = computed(() => {
    if (formData.connectionMethod === 'existing') return formData.existingConnectionId || null;
    return formData.createdConnectionId || null;
  });

  // ── Updaters ─────────────────────────────────────────────────────────
  function updateUnifiedIntegrationStepData(data: UnifiedIntegrationStepData) {
    const isDynamic = ['DOCUMENT_DRAFT_DYNAMIC', 'DOCUMENT_FINAL_DYNAMIC'].includes(data.subType);
    Object.assign(formData, {
      name: data.name,
      description: data.description,
      itemType: data.itemType,
      subType: data.subType,
      subTypeLabel: data.subTypeLabel || '',
      dynamicDocument: isDynamic ? data.dynamicDocument || '' : '',
      dynamicDocumentLabel: isDynamic ? data.dynamicDocumentLabel || '' : '',
    });
  }

  function updateScheduleData(data: ScheduleConfigurationData) {
    Object.assign(formData, {
      ...data,
      isExecuteOnMonthEnd: data.isExecuteOnMonthEnd ?? false,
    });
  }

  function updatePageConfigData(data: ConfluencePageConfigData) {
    Object.assign(formData, data);
  }

  function handleConnectionDataUpdate(data: ConnectionStepData) {
    // Keep the in-progress base URL locally so ConnectionStep does not get reset by parent re-renders.
    pendingConnectionBaseUrl.value = data.baseUrl || '';
    Object.assign(formData, {
      connectionMethod: data.connectionMethod,
      existingConnectionId: data.existingConnectionId,
      createdConnectionId: data.createdConnectionId,
      connectionName: data.connectionName,
      username: data.username || '',
      password: data.password || '',
    });
  }

  function setStepValidation(step: number, valid: boolean) {
    stepValidation[step] = valid;
  }

  function resetFormData() {
    Object.assign(formData, createDefaultFormData());
    pendingConnectionBaseUrl.value = '';
    Object.assign(stepValidation, { 1: false, 2: false, 3: false, 4: false, 5: true });
  }

  return {
    formData,
    stepValidation,
    unifiedIntegrationStepData,
    scheduleData,
    pageConfigData,
    genericConnectionData,
    activeConnectionId,
    updateUnifiedIntegrationStepData,
    updateScheduleData,
    updatePageConfigData,
    handleConnectionDataUpdate,
    setStepValidation,
    resetFormData,
  };
}
