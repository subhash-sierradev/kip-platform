<template>
  <div class="cs-root">
    <div class="cs-container">
      <!-- Connection Method Selection -->
      <div class="cs-section">
        <div class="cs-section-title">Connection Method</div>
        <div class="cs-simple-radio-group">
          <label v-if="canCreateNewConnection" class="cs-simple-radio-item">
            <input
              type="radio"
              class="cs-radio"
              :value="'new'"
              v-model="localData.connectionMethod"
              @change="onConnectionMethodChange"
            />
            <p class="cs-simple-radio-text">
              <strong>Create a New Connection</strong> – Set up a new connection configuration
            </p>
          </label>
          <label class="cs-simple-radio-item">
            <input
              type="radio"
              class="cs-radio"
              :value="'existing'"
              v-model="localData.connectionMethod"
              @change="onConnectionMethodChange"
            />
            <p class="cs-simple-radio-text">
              <strong>Use an Existing Connection</strong> – Reuse a previously configured connection
            </p>
          </label>
        </div>
      </div>

      <!-- Existing Connection Selection -->
      <ExistingConnectionPanel
        v-if="localData.connectionMethod === 'existing'"
        :existing-connections="existingConnections"
        :existing-connection-id="localData.existingConnectionId"
        :loading="existingLoading"
        :active-count="activeCount"
        :failed-count="failedCount"
        :is-testing-existing="isTestingExisting"
        :existing-tested="existingTested"
        :existing-test-success="existingTestSuccess"
        :existing-test-message="existingTestMessage"
        :verify-button-text="verifyButtonText"
        :get-connection-status="getConnectionStatus"
        :format-last-tested="formatLastTested"
        @select-existing="selectExistingConnection"
        @verify-existing="handleVerifyExisting"
      />

      <!-- New Connection Form -->
      <NewConnectionForm
        v-if="localData.connectionMethod === 'new'"
        :model-value="localData"
        :config="config"
        :credential-types="credentialTypes"
        :current-credential-fields="currentCredentialFields"
        :use-two-col-credential-grid="useTwoColCredentialGrid"
        :can-test-connection="canTestConnection"
        :is-testing="isTesting"
        :tested="tested"
        :test-success="testSuccess"
        :test-message="testMessage"
        :test-button-text="testButtonText"
        @update:modelValue="Object.assign(localData, $event)"
        @credential-type-change="onCredentialTypeChange"
        @test-connection="handleTestConnection"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, watch, onMounted } from 'vue';
import { useCredentialTypes } from '@/composables/useCredentialTypes';
import { useAuth } from '@/composables/useAuth';
import { useExistingConnections } from '@/composables/useExistingConnections';
import { useConnectionTest } from '@/composables/useConnectionTest';
import { useConnectionValidation } from '@/composables/useConnectionValidation';
import type {
  ConnectionStepData,
  ConnectionStepConfig,
  SavedConnection,
  CredentialField,
} from '@/types/ConnectionStepData';
import ExistingConnectionPanel from './ExistingConnectionPanel.vue';
import NewConnectionForm from './NewConnectionForm.vue';
import './ConnectionStep.css';

/**
 * Props
 */
interface Props {
  modelValue: ConnectionStepData;
  config: ConnectionStepConfig;
}

const props = defineProps<Props>();
const { userInfo } = useAuth();

/**
 * Emits
 */
const emit = defineEmits<{
  'update:modelValue': [value: ConnectionStepData];
  'validation-change': [isValid: boolean];
  'connection-success': [connectionId: string];
}>();

/**
 * Local reactive state
 */
const localData = reactive<ConnectionStepData>({ ...props.modelValue });
const canCreateNewConnection = computed(() => {
  const roles = userInfo.value.roles ?? [];
  return roles.includes('tenant_admin') || roles.includes('app_admin');
});

/**
 * Composables
 */
const { credentialTypes, fetchCredentialTypes } = useCredentialTypes(
  props.config.serviceType,
  props.config.supportsDynamicCredentials
);

const {
  existingConnections,
  loading: existingLoading,
  activeCount,
  failedCount,
  isTestingExisting,
  existingTested,
  existingTestSuccess,
  existingTestMessage,
  verifyButtonText,
  fetchExistingConnections,
  verifyConnection,
  resetVerification,
  getConnectionStatus,
  formatLastTested,
} = useExistingConnections(props.config.serviceType);

const {
  isTesting,
  tested,
  testSuccess,
  testMessage,
  testButtonText,
  createdConnectionId,
  testConnection,
  resetTest,
} = useConnectionTest(props.config.serviceType);

const { canTestConnection, isStepValid } = useConnectionValidation({
  connectionData: computed(() => localData),
  credentialTypes,
  testSuccess,
  existingTestSuccess,
  serviceType: props.config.serviceType,
});

/**
 * Computed: Current credential fields based on selected type
 */
const currentCredentialFields = computed((): CredentialField[] => {
  const credType = credentialTypes.value.find(ct => ct.value === localData.credentialType);
  return credType?.fields || [];
});

const useTwoColCredentialGrid = computed((): boolean => {
  const keys = new Set(currentCredentialFields.value.map(f => f.key));
  return keys.has('clientId') && keys.has('clientSecret');
});

/**
 * Watchers
 */

// Watch local data changes and emit to parent
watch(
  () => localData,
  newData => {
    emit('update:modelValue', { ...newData });
  },
  { deep: true }
);

// Watch step validity and emit validation-change
watch(
  isStepValid,
  valid => {
    localData.connected = valid;
    emit('validation-change', valid);
  },
  { immediate: true }
);

// When verifying an existing connection succeeds, emit its id so the parent wizard
// can store the connectionId (some wizards gate Next on connectionId being set).
watch(
  () =>
    [
      localData.connectionMethod,
      localData.existingConnectionId,
      existingTested.value,
      existingTestSuccess.value,
    ] as const,
  ([method, existingId, testedVal, successVal]) => {
    if (method !== 'existing') return;
    if (!existingId) return;
    if (testedVal && successVal) {
      emit('connection-success', existingId);
    }
  }
);

// Watch created connection ID
watch(createdConnectionId, async id => {
  if (id) {
    localData.createdConnectionId = id;
    emit('connection-success', id);

    // Refresh existing connections list to include newly created connection
    await fetchExistingConnections();
  }
});

// Watch for external model value changes
watch(
  () => props.modelValue,
  newValue => {
    Object.assign(localData, newValue);
  },
  { deep: true }
);

watch(
  canCreateNewConnection,
  allowed => {
    if (allowed || localData.connectionMethod !== 'new') return;

    localData.connectionMethod = 'existing';
    localData.createdConnectionId = undefined;
    resetTest();
  },
  { immediate: true }
);

/**
 * Handlers
 */

const onConnectionMethodChange = async () => {
  // Reset test states when switching connection method
  resetTest();
  resetVerification();
  localData.existingConnectionId = undefined;
  localData.createdConnectionId = undefined;

  // Refresh connections list when switching to existing method
  // This ensures any newly created connections appear in the dropdown
  if (localData.connectionMethod === 'existing') {
    await fetchExistingConnections();
  }
};

const onCredentialTypeChange = () => {
  // Reset credential fields when type changes
  localData.username = '';
  localData.password = '';
  localData.clientId = '';
  localData.clientSecret = '';
  localData.tokenUrl = '';
  localData.scope = '';
  resetTest();
};

const selectExistingConnection = (conn: SavedConnection) => {
  localData.existingConnectionId = conn.id;
  localData.baseUrl = conn.baseUrl;
  resetVerification();
};

const handleVerifyExisting = async () => {
  if (!localData.existingConnectionId) return;
  await verifyConnection(localData.existingConnectionId);
};

const handleTestConnection = async () => {
  await testConnection(localData);
};

/**
 * Lifecycle
 */
onMounted(async () => {
  // Initialize default credential type if not set
  if (!localData.credentialType && props.config.defaultCredentialType) {
    localData.credentialType = props.config.defaultCredentialType;
  }

  // Fetch credential types and existing connections
  await Promise.all([fetchCredentialTypes(), fetchExistingConnections()]);
});
</script>
