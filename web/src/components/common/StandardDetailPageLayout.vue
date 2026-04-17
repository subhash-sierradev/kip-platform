<template>
  <div :class="['detail-page-layout', containerClass]">
    <!-- Page Header -->
    <DetailsPageHeader
      :title="title"
      :loading="loading"
      :error="error"
      :icon="icon"
      :version="version"
      :status="status"
      :show-back-button="showBackButton"
      :back-button-label="backButtonLabel"
      :tabs="tabs"
      :active-tab="activeTab"
      @back="$emit('back')"
      @tab-change="(tabId: string) => emit('tab-change', tabId)"
    >
      <!-- Pass through custom icon slot -->
      <template v-if="$slots['custom-icon']" #custom-icon>
        <slot name="custom-icon"></slot>
      </template>
    </DetailsPageHeader>

    <!-- Main Content Area -->
    <div class="page-content">
      <!-- Loading and Error Wrapper -->
      <LoadingErrorWrapper
        :loading="loading"
        :error="error"
        :has-data="hasData"
        :loading-message="loadingMessage"
        :error-title="errorTitle"
        :retry-label="retryLabel"
        :container="`.${containerClass}`"
        :show-retry="showRetry"
        @retry="$emit('retry')"
      >
        <!-- Dynamic Tab Content or Slot -->
        <div v-if="tabs && tabs.length > 0" class="tab-content">
          <component
            :is="activeTabComponent"
            v-bind="componentProps"
            @status-updated="$emit('status-updated', $event)"
            @refresh="$emit('refresh')"
            v-on="componentEvents"
          />
        </div>

        <!-- Default slot for custom content -->
        <slot v-else />
      </LoadingErrorWrapper>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StandardDetailPageLayout' });

import { computed } from 'vue';
import DetailsPageHeader from '@/components/common/DetailsPageHeader.vue';
import LoadingErrorWrapper from '@/components/common/LoadingErrorWrapper.vue';
import type { TabDefinition } from '@/types/tab';

interface StandardDetailPageLayoutProps {
  /** Page title */
  title: string;
  /** Loading state */
  loading: boolean;
  /** Error message */
  error: string | null;
  /** Whether data has been loaded */
  hasData?: boolean;
  /** Page icon */
  icon?: string;
  /** Entity version */
  version?: number | string;
  /** Entity status */
  status?: 'enabled' | 'disabled' | null;
  /** Whether to show back button */
  showBackButton?: boolean;
  /** Back button aria label */
  backButtonLabel?: string;
  /** Tab definitions */
  tabs?: TabDefinition[];
  /** Active tab ID */
  activeTab?: string;
  /** Props to pass to tab components */
  componentProps?: Record<string, any>;
  /** Events to bind to tab components */
  componentEvents?: Record<string, any>;
  /** Custom container CSS class */
  containerClass?: string;
  /** Loading message */
  loadingMessage?: string;
  /** Error title */
  errorTitle?: string;
  /** Retry button label */
  retryLabel?: string;
  /** Whether to show retry button */
  showRetry?: boolean;
}

const props = withDefaults(defineProps<StandardDetailPageLayoutProps>(), {
  hasData: false,
  icon: undefined,
  version: undefined,
  status: null,
  showBackButton: true,
  backButtonLabel: 'Go back',
  tabs: undefined,
  activeTab: 'details',
  componentProps: () => ({}),
  componentEvents: () => ({}),
  containerClass: 'detail-page',
  loadingMessage: 'Loading...',
  errorTitle: 'Unable to load data',
  retryLabel: 'Try Again',
  showRetry: true,
});

interface StandardDetailPageLayoutEmits {
  /** Emitted when back button is clicked */
  (e: 'back'): void;
  /** Emitted when tab is changed */
  (e: 'tab-change', tabId: string): void;
  /** Emitted when retry button is clicked */
  (e: 'retry'): void;
  /** Emitted when entity status is updated */
  (e: 'status-updated', enabled: boolean): void;
  /** Emitted when refresh is requested */
  (e: 'refresh'): void;
}

const emit = defineEmits<StandardDetailPageLayoutEmits>();

// Computed property to find active tab component
const activeTabComponent = computed(() => {
  if (!props.tabs || !props.activeTab) return null;
  const activeTabDef = props.tabs.find(tab => tab.id === props.activeTab);
  return activeTabDef?.component || null;
});
</script>

<style scoped>
.detail-page-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #f8fafc;
}

.page-content {
  flex: 1;
  padding: 1rem 1.5rem;
  width: 100%;
  position: relative;
  box-sizing: border-box;
  overflow: auto;
  min-height: 0;
}

.tab-content {
  animation: fadeIn 0.3s ease-in-out;
  width: 100%;
  height: 100%;
  min-height: 0;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
