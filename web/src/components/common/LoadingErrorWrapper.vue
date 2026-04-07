<template>
  <div class="loading-error-wrapper">
    <!-- Loading State with DevExtreme LoadPanel -->
    <DxLoadPanel
      :visible="loading && !hasData"
      :message="loadingMessage"
      :show-indicator="true"
      :show-pane="true"
      :shading="true"
      :container="container"
    />

    <!-- Error State -->
    <div v-if="error" class="error-container" role="alert" aria-live="assertive">
      <div class="error-card">
        <i class="dx-icon dx-icon-warning error-icon" aria-hidden="true"></i>
        <h3 class="error-title">{{ errorTitle }}</h3>
        <p class="error-message">{{ error }}</p>
        <DxButton
          v-if="showRetry"
          :text="retryLabel"
          icon="refresh"
          type="default"
          styling-mode="contained"
          :disabled="loading"
          @click="$emit('retry')"
        />
      </div>
    </div>

    <!-- Main Content Slot -->
    <div v-else class="content-wrapper">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'LoadingErrorWrapper' });

import { DxButton, DxLoadPanel } from 'devextreme-vue';

interface LoadingErrorWrapperProps {
  /** Loading state indicator */
  loading: boolean;
  /** Error message to display */
  error: string | null;
  /** Whether data has been loaded (prevents loader showing when data exists) */
  hasData?: boolean;
  /** Custom loading message */
  loadingMessage?: string;
  /** Custom error title */
  errorTitle?: string;
  /** Custom retry button label */
  retryLabel?: string;
  /** Container selector for LoadPanel positioning */
  container?: string;
  /** Whether to show retry button */
  showRetry?: boolean;
}

withDefaults(defineProps<LoadingErrorWrapperProps>(), {
  hasData: false,
  loadingMessage: 'Loading...',
  errorTitle: 'Unable to load data',
  retryLabel: 'Try Again',
  container: undefined,
  showRetry: true,
});

interface LoadingErrorWrapperEmits {
  /** Emitted when retry button is clicked */
  (e: 'retry'): void;
}

defineEmits<LoadingErrorWrapperEmits>();
</script>

<style scoped>
.loading-error-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.content-wrapper {
  width: 100%;
  height: 100%;
}

.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  padding: 2rem 0;
}

.error-card {
  background: white;
  border-radius: 12px;
  padding: 3rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  text-align: center;
  max-width: 400px;
  width: 100%;
}

.error-icon {
  font-size: 3rem;
  color: #ef4444;
  margin-bottom: 1.5rem;
}

.error-title {
  color: #111827;
  font-size: 1.25rem;
  font-weight: 600;
  margin: 0 0 0.5rem 0;
}

.error-message {
  color: #6b7280;
  margin-bottom: 2rem;
  line-height: 1.5;
}

/* DevExtreme LoadPanel Customization */
:global(.dx-loadpanel-content) {
  background: white !important;
  border-radius: 12px !important;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15) !important;
  border: none !important;
}

:global(.dx-loadpanel-message) {
  color: #374151 !important;
  font-weight: 500 !important;
}

:global(.dx-loadindicator-icon) {
  border-color: #f97316 transparent transparent transparent !important;
}
</style>
