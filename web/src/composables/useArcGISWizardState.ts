import { computed, reactive } from 'vue';

import { createDefaultFormData } from '@/components/outbound/arcgisintegration/wizard/arcgisWizardConfig';
import type { ConnectionStepData } from '@/types/ConnectionStepData';
import type { UnifiedIntegrationStepData } from '@/types/UnifiedIntegrationStep';

import type {
  BasicDetailsData,
  ConnectionData,
  DocumentSelectionData,
  FieldMappingData,
  ScheduleConfigurationData,
} from '../types/ArcGISFormData';

const DYNAMIC_SUBTYPES = 'DOCUMENT_FINAL_DYNAMIC';

function createDataUpdaters(formData: ReturnType<typeof createDefaultFormData>) {
  return {
    updateUnifiedIntegrationStepData(data: UnifiedIntegrationStepData) {
      const isDynamic = DYNAMIC_SUBTYPES === data.subType;
      Object.assign(formData, {
        name: data.name,
        description: data.description,
        itemType: data.itemType,
        subType: data.subType,
        subTypeLabel: data.subTypeLabel || '',
        dynamicDocument: isDynamic ? data.dynamicDocument || '' : '',
        dynamicDocumentLabel: isDynamic ? data.dynamicDocumentLabel || '' : '',
      });
    },
    updateScheduleConfigurationData(data: ScheduleConfigurationData) {
      Object.assign(formData, {
        executionDate: data.executionDate,
        executionTime: data.executionTime,
        frequencyPattern: data.frequencyPattern,
        dailyFrequency: data.dailyFrequency,
        selectedDays: data.selectedDays,
        selectedMonths: data.selectedMonths,
        monthlyPattern: data.monthlyPattern,
        monthlyWeek: data.monthlyWeek,
        monthlyWeekday: data.monthlyWeekday,
        monthlyDay: data.monthlyDay,
        isExecuteOnMonthEnd: data.isExecuteOnMonthEnd,
        cronExpression: data.cronExpression,
        businessTimeZone: data.businessTimeZone,
        timeCalculationMode: data.timeCalculationMode,
      });
    },
    updateConnectionData(data: ConnectionData) {
      Object.assign(formData, data);
    },
    updateFieldMappingData(data: FieldMappingData) {
      formData.fieldMappings = data.fieldMappings;
    },
  };
}

function createComputedProperties(formData: ReturnType<typeof createDefaultFormData>) {
  const unifiedIntegrationStepData = computed<UnifiedIntegrationStepData>(() => ({
    name: formData.name,
    description: formData.description,
    itemType: formData.itemType,
    subType: formData.subType,
    subTypeLabel: formData.subTypeLabel,
    dynamicDocument: formData.dynamicDocument || undefined,
    dynamicDocumentLabel: formData.dynamicDocumentLabel,
  }));

  const scheduleConfigurationData = computed<ScheduleConfigurationData>(() => ({
    executionDate: formData.executionDate,
    executionTime: formData.executionTime,
    frequencyPattern: formData.frequencyPattern,
    dailyFrequency: formData.dailyFrequency,
    dailyFrequencyRule:
      'dailyFrequencyRule' in formData
        ? ((formData as Record<string, unknown>).dailyFrequencyRule as string)
        : undefined,
    selectedDays: formData.selectedDays,
    selectedMonths: formData.selectedMonths,
    monthlyPattern: formData.monthlyPattern,
    monthlyWeek: formData.monthlyWeek,
    monthlyWeekday: formData.monthlyWeekday,
    monthlyDay: formData.monthlyDay,
    isExecuteOnMonthEnd: formData.isExecuteOnMonthEnd,
    cronExpression: formData.cronExpression,
    businessTimeZone: formData.businessTimeZone,
    timeCalculationMode: formData.timeCalculationMode,
  }));

  const genericConnectionData = computed<ConnectionStepData>(() => ({
    connectionMethod: formData.connectionMethod || 'existing',
    baseUrl: formData.arcgisEndpoint,
    credentialType: formData.credentialType,
    username: formData.username,
    password: formData.password,
    clientId: formData.clientId,
    clientSecret: formData.clientSecret,
    tokenUrl: formData.tokenUrl || '',
    scope: formData.scope || '',
    connectionName: formData.connectionName,
    connected: !!formData.existingConnectionId || !!formData.createdConnectionId,
    existingConnectionId: formData.existingConnectionId,
    createdConnectionId: formData.createdConnectionId,
  }));

  const fieldMappingData = computed<FieldMappingData>(() => ({
    fieldMappings: formData.fieldMappings,
  }));

  const activeConnectionId = computed(() => {
    if (formData.connectionMethod === 'existing') return formData.existingConnectionId || null;
    return formData.createdConnectionId || null;
  });

  return {
    unifiedIntegrationStepData,
    scheduleConfigurationData,
    genericConnectionData,
    fieldMappingData,
    activeConnectionId,
  };
}

export function useArcGISWizardState() {
  const formData = reactive(createDefaultFormData());

  const stepValidation = reactive<Record<number, boolean>>({
    1: false,
    2: false,
    3: false,
    4: false,
    5: true,
  });

  const computedProps = createComputedProperties(formData);
  const updaters = createDataUpdaters(formData);

  const basicDetailsData = computed<BasicDetailsData>(() => ({
    name: computedProps.unifiedIntegrationStepData.value.name,
    description: computedProps.unifiedIntegrationStepData.value.description,
  }));

  const documentSelectionData = computed<DocumentSelectionData>(() => ({
    itemType: computedProps.unifiedIntegrationStepData.value.itemType,
    subType: computedProps.unifiedIntegrationStepData.value.subType,
    subTypeLabel: computedProps.unifiedIntegrationStepData.value.subTypeLabel,
    dynamicDocument: computedProps.unifiedIntegrationStepData.value.dynamicDocument,
    dynamicDocumentLabel: computedProps.unifiedIntegrationStepData.value.dynamicDocumentLabel,
  }));

  function updateBasicDetailsData(data: BasicDetailsData) {
    updaters.updateUnifiedIntegrationStepData({
      ...computedProps.unifiedIntegrationStepData.value,
      ...data,
    });
  }

  function updateDocumentSelectionData(data: DocumentSelectionData) {
    updaters.updateUnifiedIntegrationStepData({
      ...computedProps.unifiedIntegrationStepData.value,
      ...data,
    });
  }

  function handleConnectionDataUpdate(data: ConnectionStepData) {
    updaters.updateConnectionData({
      arcgisEndpoint: data.baseUrl,
      fetchMode: formData.fetchMode,
      credentialType: data.credentialType,
      username: data.username || '',
      password: data.password || '',
      clientId: data.clientId || '',
      clientSecret: data.clientSecret || '',
      tokenUrl: data.tokenUrl || '',
      scope: data.scope || '',
      connectionName: data.connectionName,
      connectionMethod: data.connectionMethod,
      existingConnectionId: data.existingConnectionId,
      createdConnectionId: data.createdConnectionId,
    });
  }

  function setStepValidation(step: number, valid: boolean) {
    stepValidation[step] = valid;
  }

  function resetFormData() {
    Object.assign(formData, createDefaultFormData());
    Object.assign(stepValidation, { 1: false, 2: false, 3: false, 4: false, 5: true });
  }

  return {
    formData,
    stepValidation,
    ...computedProps,
    basicDetailsData,
    documentSelectionData,
    ...updaters,
    updateBasicDetailsData,
    updateDocumentSelectionData,
    handleConnectionDataUpdate,
    setStepValidation,
    resetFormData,
  };
}
