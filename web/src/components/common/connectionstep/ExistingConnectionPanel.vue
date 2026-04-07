<template>
  <div class="cs-section">
    <div class="cs-section-title">Saved Connections</div>

    <!-- Custom Dropdown -->
    <div class="cs-dropdown" :class="{ 'cs-dropdown-open': dropdownOpen }">
      <div class="cs-dropdown-selected" @click="toggleDropdown">
        <div v-if="selectedExistingConnection" class="cs-selected-content">
          <span class="cs-connection-name">{{ selectedExistingConnection.name }}</span>
          <span v-if="selectedExistingConnection.secretName" class="cs-selected-separator">•</span>
          <span v-if="selectedExistingConnection.secretName" class="cs-selected-id">{{
            selectedExistingConnection.secretName
          }}</span>
        </div>
        <span v-else class="cs-placeholder">Choose a saved connection</span>
        <span class="cs-arrow" :class="{ open: dropdownOpen }">▼</span>
      </div>

      <div v-if="dropdownOpen" class="cs-dropdown-list">
        <div v-if="loading" class="cs-loading-row">Loading connections...</div>
        <div v-else-if="existingConnections.length === 0" class="cs-empty">
          No saved connections found
        </div>
        <div
          v-else
          v-for="conn in existingConnections"
          :key="conn.id"
          class="cs-option"
          @click="handleSelect(conn)"
        >
          <div class="cs-connection-item">
            <div class="cs-connection-header">
              <span class="cs-connection-name">{{ conn.name }}</span>
              <span
                class="cs-badge"
                :class="{
                  'cs-badge-success': getConnectionStatus(conn).severity === 'success',
                  'cs-badge-error': getConnectionStatus(conn).severity === 'error',
                  'cs-badge-neutral': getConnectionStatus(conn).severity === 'neutral',
                }"
              >
                {{ getConnectionStatus(conn).label }}
              </span>
            </div>
            <div class="cs-connection-meta">
              <span v-if="conn.secretName" class="cs-connection-secret">{{ conn.secretName }}</span>
              <span v-if="conn.secretName" class="cs-meta-separator">•</span>
              <span class="cs-last-used"
                >Last tested: {{ formatLastTested(conn.lastConnectionTest) }}</span
              >
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Connection meta info -->
    <div v-if="existingConnections.length > 0" class="cs-connection-meta-line">
      {{ existingConnections.length }} connection{{ existingConnections.length !== 1 ? 's' : '' }} •
      {{ activeCount }} active • {{ failedCount }} failed
    </div>

    <!-- Verify Existing Connection Button -->
    <div class="cs-verification-row">
      <button
        class="cs-btn cs-btn-outlined-primary"
        :disabled="!existingConnectionId || isTestingExisting"
        @click="emit('verify-existing')"
      >
        {{ verifyButtonText }}
      </button>

      <div
        v-if="existingTested"
        class="cs-verification-message"
        :class="{
          'cs-verification-success': existingTestSuccess,
          'cs-verification-error': !existingTestSuccess,
        }"
      >
        <span class="cs-check-inline">{{ existingTestSuccess ? '✔' : '✖' }}</span>
        {{ existingTestMessage }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

import type { ConnectionStatusDetails, SavedConnection } from '@/types/ConnectionStepData';

const emit = defineEmits<{
  (e: 'select-existing', conn: SavedConnection): void;
  (e: 'verify-existing'): void;
}>();

const props = defineProps<{
  existingConnections: SavedConnection[];
  existingConnectionId?: string;
  loading: boolean;
  activeCount: number;
  failedCount: number;
  isTestingExisting: boolean;
  existingTested: boolean;
  existingTestSuccess: boolean;
  existingTestMessage: string;
  verifyButtonText: string;
  getConnectionStatus: (conn: SavedConnection) => ConnectionStatusDetails;
  formatLastTested: (value?: string) => string;
}>();

const dropdownOpen = ref(false);

const selectedExistingConnection = computed(
  () => props.existingConnections.find(conn => conn.id === props.existingConnectionId) ?? null
);

const toggleDropdown = () => {
  dropdownOpen.value = !dropdownOpen.value;
};

const handleSelect = (conn: SavedConnection) => {
  emit('select-existing', conn);
  dropdownOpen.value = false;
};
</script>
