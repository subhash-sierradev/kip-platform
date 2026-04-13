<template>
  <div class="cs-section">
    <div v-if="showTitle" class="cs-section-title">New Connection</div>

    <!-- Credential Type Selector (for services with dynamic types) -->
    <div v-if="config.showCredentialTypeSelector" class="cs-field">
      <label class="cs-label"> Credential Type<span class="cs-required">*</span> </label>
      <select
        v-model="localData.credentialType"
        class="cs-input"
        @change="emit('credential-type-change')"
      >
        <option value="" disabled>Select credential type</option>
        <option v-for="credType in credentialTypes" :key="credType.value" :value="credType.value">
          {{ credType.label }}
        </option>
      </select>
    </div>

    <!-- Base URL and Connection Name -->
    <div class="cs-field-row">
      <div class="cs-field">
        <label class="cs-label">
          {{ config.baseUrlLabel }}<span class="cs-required">*</span>
        </label>
        <input
          type="url"
          class="cs-input"
          v-model="localData.baseUrl"
          :placeholder="config.baseUrlPlaceholder"
        />
      </div>
      <div class="cs-field">
        <label class="cs-label"> Connection Name<span class="cs-required">*</span> </label>
        <input
          type="text"
          class="cs-input"
          v-model="localData.connectionName"
          placeholder="Enter connection name"
        />
      </div>
    </div>

    <!-- Dynamic Credential Fields -->
    <div class="cs-credential-section" :class="{ 'cs-credential-grid': useTwoColCredentialGrid }">
      <div v-for="field in currentCredentialFields" :key="field.key" class="cs-field">
        <label class="cs-label">
          {{ field.label }}<span v-if="field.required" class="cs-required">*</span>
        </label>

        <!-- Password/Secret fields with visibility toggle -->
        <div v-if="field.type === 'password'" class="cs-input-with-icon">
          <input
            :type="passwordVisibility[field.key] ? 'text' : 'password'"
            class="cs-input"
            v-model="(localData as any)[field.key]"
            :placeholder="field.placeholder || ''"
          />
          <button
            type="button"
            class="cs-icon-btn"
            @click="togglePasswordVisibility(field.key)"
            :aria-label="passwordVisibility[field.key] ? 'Hide password' : 'Show password'"
          >
            <Eye v-if="passwordVisibility[field.key]" class="cs-visibility-icon" :size="16" />
            <EyeOff v-else class="cs-visibility-icon" :size="16" />
          </button>
        </div>

        <!-- Other field types -->
        <input
          v-else
          :type="field.type"
          class="cs-input"
          v-model="(localData as any)[field.key]"
          :placeholder="field.placeholder || ''"
        />

        <!-- Help text -->
        <div v-if="field.helpText" class="cs-helper">{{ field.helpText }}</div>
      </div>
    </div>

    <!-- Test Connection Button and Message -->
    <div class="cs-test-section">
      <button
        class="cs-test-btn"
        :class="{
          'cs-test-btn-success': tested && testSuccess,
          'cs-test-btn-failed': tested && !testSuccess,
        }"
        :disabled="!canTestConnection || isTesting"
        @click="emit('test-connection')"
      >
        <span v-if="isTesting" class="cs-loading-spinner"></span>
        {{ testButtonText }}
      </button>

      <div
        v-if="tested"
        class="cs-test-message"
        :class="{
          'cs-test-message-success': testSuccess,
          'cs-test-message-error': !testSuccess,
        }"
      >
        {{ testMessage }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue';
import { Eye, EyeOff } from 'lucide-vue-next';

import type {
  ConnectionStepConfig,
  ConnectionStepData,
  CredentialField,
  CredentialTypeOption,
} from '@/types/ConnectionStepData';

const emit = defineEmits<{
  (e: 'update:modelValue', value: ConnectionStepData): void;
  (e: 'credential-type-change'): void;
  (e: 'test-connection'): void;
}>();

const props = defineProps<{
  modelValue: ConnectionStepData;
  config: ConnectionStepConfig;
  credentialTypes: CredentialTypeOption[];
  currentCredentialFields: CredentialField[];
  useTwoColCredentialGrid: boolean;
  canTestConnection: boolean;
  isTesting: boolean;
  tested: boolean;
  testSuccess: boolean;
  testMessage: string;
  testButtonText: string;
  showTitle?: boolean;
}>();

const showTitle = props.showTitle ?? true;

const localData = reactive<ConnectionStepData>({ ...props.modelValue });
const passwordVisibility = reactive<Record<string, boolean>>({});
let syncingFromParent = false;

watch(
  () => props.modelValue,
  newValue => {
    syncingFromParent = true;
    Object.assign(localData, newValue);
    Promise.resolve().then(() => {
      syncingFromParent = false;
    });
  },
  { deep: true }
);

watch(
  localData,
  newValue => {
    if (syncingFromParent) return;
    emit('update:modelValue', { ...newValue });
  },
  { deep: true }
);

const togglePasswordVisibility = (fieldKey: string) => {
  passwordVisibility[fieldKey] = !passwordVisibility[fieldKey];
};
</script>
