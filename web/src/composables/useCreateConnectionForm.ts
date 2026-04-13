import { computed, type Ref, ref } from 'vue';

import { ServiceType } from '@/api/models/enums';
import { useConnectionValidation } from '@/composables/useConnectionValidation';
import { useCredentialTypes } from '@/composables/useCredentialTypes';
import type {
  ConnectionStepConfig,
  ConnectionStepData,
  CredentialField,
} from '@/types/ConnectionStepData';
import { toPlainText } from '@/utils/stringFormatUtils';

const DEFAULT_ARCGIS_TOKEN_URL = 'https://www.arcgis.com/sharing/rest/oauth2/token';

interface CreateConnectionResponse {
  lastConnectionStatus?: string;
  lastConnectionMessage?: string;
}

interface UseCreateConnectionFormOptions {
  config: ConnectionStepConfig;
  submitConnection: (data: ConnectionStepData) => Promise<CreateConnectionResponse>;
}

interface FormStateRefs {
  connectionData: Ref<ConnectionStepData>;
  dialogLoading: Ref<boolean>;
  tested: Ref<boolean>;
  testSuccess: Ref<boolean>;
  testMessage: Ref<string>;
}

function getDefaultTokenUrl(serviceType: ConnectionStepConfig['serviceType']): string {
  return serviceType === ServiceType.ARCGIS ? DEFAULT_ARCGIS_TOKEN_URL : '';
}

function buildDefaultCreateConnectionData(config: ConnectionStepConfig): ConnectionStepData {
  return {
    connectionMethod: 'new',
    baseUrl: '',
    credentialType: config.defaultCredentialType || '',
    connectionName: '',
    username: '',
    password: '',
    clientId: '',
    clientSecret: '',
    tokenUrl: getDefaultTokenUrl(config.serviceType),
    scope: '',
    connected: false,
  };
}

function resetTestState(state: FormStateRefs) {
  state.dialogLoading.value = false;
  state.tested.value = false;
  state.testSuccess.value = false;
  state.testMessage.value = '';
}

function resetFormState(state: FormStateRefs, config: ConnectionStepConfig) {
  state.connectionData.value = buildDefaultCreateConnectionData(config);
  resetTestState(state);
}

function updateCredentialTypeData(state: FormStateRefs, config: ConnectionStepConfig) {
  state.connectionData.value = {
    ...state.connectionData.value,
    username: '',
    password: '',
    clientId: '',
    clientSecret: '',
    tokenUrl: getDefaultTokenUrl(config.serviceType),
    scope: '',
  };
  resetTestState(state);
}

async function submitConnectionForm(
  state: FormStateRefs,
  submitConnection: (data: ConnectionStepData) => Promise<CreateConnectionResponse>
) {
  state.dialogLoading.value = true;
  state.tested.value = false;
  state.testSuccess.value = false;
  state.testMessage.value = '';

  try {
    const response = await submitConnection(state.connectionData.value);
    const isSuccess = response.lastConnectionStatus === 'SUCCESS';
    const rawMessage =
      response.lastConnectionMessage ||
      (isSuccess ? 'Connection tested successfully' : 'Connection test failed');

    state.tested.value = true;
    state.testSuccess.value = isSuccess;
    state.testMessage.value = toPlainText(rawMessage, 'Connection test failed');

    return { success: isSuccess, response };
  } catch (error: unknown) {
    const apiError = error as { body?: { message?: string }; message?: string };
    const rawMessage = apiError.body?.message || apiError.message || 'Failed to test connection';

    state.tested.value = true;
    state.testSuccess.value = false;
    state.testMessage.value = toPlainText(rawMessage, 'Failed to test connection');

    return { success: false, response: null };
  } finally {
    state.dialogLoading.value = false;
  }
}

export function useCreateConnectionForm(options: UseCreateConnectionFormOptions) {
  const { config, submitConnection } = options;

  const connectionData = ref<ConnectionStepData>(buildDefaultCreateConnectionData(config));
  const dialogOpen = ref(false);
  const dialogLoading = ref(false);
  const tested = ref(false);
  const testSuccess = ref(false);
  const testMessage = ref('');
  const testButtonText = computed(() => (dialogLoading.value ? 'Testing...' : 'Test Connection'));
  const state: FormStateRefs = {
    connectionData,
    dialogLoading,
    tested,
    testSuccess,
    testMessage,
  };

  const { credentialTypes, fetchCredentialTypes } = useCredentialTypes(
    config.serviceType,
    config.supportsDynamicCredentials
  );

  const currentCredentialFields = computed((): CredentialField[] => {
    const credentialType = credentialTypes.value.find(
      entry => entry.value === connectionData.value.credentialType
    );
    return credentialType?.fields || [];
  });

  const useTwoColCredentialGrid = computed((): boolean => {
    const keys = new Set(currentCredentialFields.value.map(field => field.key));
    return keys.has('clientId') && keys.has('clientSecret');
  });

  const { canTestConnection } = useConnectionValidation({
    connectionData,
    credentialTypes,
    testSuccess,
    existingTestSuccess: ref(false),
    serviceType: config.serviceType,
  });

  async function openDialog() {
    resetFormState(state, config);
    await fetchCredentialTypes();
    dialogOpen.value = true;
  }

  function closeDialog() {
    dialogOpen.value = false;
    resetFormState(state, config);
  }

  function updateConnectionData(value: ConnectionStepData) {
    connectionData.value = value;
    resetTestState(state);
  }

  function onCredentialTypeChange() {
    updateCredentialTypeData(state, config);
  }

  async function handleSubmit() {
    return submitConnectionForm(state, submitConnection);
  }

  return {
    connectionData,
    dialogOpen,
    dialogLoading,
    tested,
    testSuccess,
    testMessage,
    testButtonText,
    credentialTypes,
    fetchCredentialTypes,
    currentCredentialFields,
    useTwoColCredentialGrid,
    canTestConnection,
    resetFormState: () => resetFormState(state, config),
    openDialog,
    closeDialog,
    updateConnectionData,
    onCredentialTypeChange,
    handleSubmit,
  };
}
