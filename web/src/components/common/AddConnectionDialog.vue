<template>
  <AppModal
    :open="dialogOpen"
    class="create-connection-modal"
    size="lg"
    :title="`Add ${serviceLabel} Connection`"
    :style="{
      '--kw-modal-max-width': '880px',
      '--kw-modal-max-height': '80vh',
    }"
    @update:open="handleModalOpenChange"
  >
    <div class="create-connection-dialog cs-root">
      <div class="cs-container">
        <NewConnectionForm
          :model-value="connectionData"
          :config="connectionConfig"
          :show-title="false"
          :credential-types="credentialTypes"
          :current-credential-fields="currentCredentialFields"
          :use-two-col-credential-grid="useTwoColCredentialGrid"
          :can-test-connection="canTestConnection"
          :is-testing="dialogLoading"
          :tested="tested"
          :test-success="testSuccess"
          :test-message="testMessage"
          :test-button-text="testButtonText"
          @update:modelValue="updateConnectionData"
          @credential-type-change="onCredentialTypeChange"
          @test-connection="handleCreateConnection"
        />
      </div>
    </div>
  </AppModal>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue';

import type { ConnectionStepData } from '@/types/ConnectionStepData';
import { ServiceType, ServiceTypeMetadata } from '@/api/models/enums';
import AppModal from '@/components/common/AppModal.vue';
import NewConnectionForm from '@/components/common/connectionstep/NewConnectionForm.vue';
import { useCreateConnectionForm } from '@/composables/useCreateConnectionForm';
import { getConnectionStepConfig } from '@/utils/connectionStepConfig';

interface CreateConnectionResponse {
  lastConnectionStatus?: string;
  lastConnectionMessage?: string;
}

interface Props {
  open: boolean;
  serviceType: ServiceType;
  submitConnection: (data: ConnectionStepData) => Promise<CreateConnectionResponse>;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  'update:open': [value: boolean];
  created: [];
}>();

const connectionConfig = getConnectionStepConfig(props.serviceType);
const serviceLabel = computed(() => {
  return ServiceTypeMetadata[props.serviceType]?.displayName ?? props.serviceType;
});

const {
  connectionData,
  dialogOpen,
  dialogLoading,
  tested,
  testSuccess,
  testMessage,
  testButtonText,
  credentialTypes,
  currentCredentialFields,
  useTwoColCredentialGrid,
  canTestConnection,
  openDialog,
  closeDialog,
  updateConnectionData,
  onCredentialTypeChange,
  handleSubmit,
} = useCreateConnectionForm({
  config: connectionConfig,
  submitConnection: props.submitConnection,
});

watch(
  () => props.open,
  isOpen => {
    if (isOpen) {
      void openDialog();
      return;
    }

    closeDialog();
  },
  { immediate: true }
);

function emitOpen(value: boolean) {
  emit('update:open', value);
}

function handleModalOpenChange(value: boolean) {
  if (value) {
    emitOpen(true);
    return;
  }

  closeDialog();
  emitOpen(false);
}

async function handleCreateConnection() {
  const { success } = await handleSubmit();

  if (!success) {
    return;
  }

  emit('created');
  closeDialog();
  emitOpen(false);
}
</script>
