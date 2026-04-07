<template>
  <div class="page-header">
    <div class="header-content">
      <div class="header-left">
        <DxButton
          v-if="showBackButton"
          icon="chevronleft"
          type="normal"
          styling-mode="text"
          class="back-button"
          @click="$emit('back')"
          :aria-label="backButtonLabel"
        />
        <div class="page-title-section">
          <div v-if="icon || $slots['custom-icon']" class="icon-container">
            <slot v-if="$slots['custom-icon']" name="custom-icon"></slot>
            <i v-else :class="iconClass" aria-hidden="true"></i>
          </div>
          <div class="title-content">
            <div class="title-row">
              <h1
                ref="titleRef"
                class="page-title"
                @mouseenter="handleTitleMouseEnter"
                @mouseleave="handleTitleMouseLeave"
              >
                {{ displayTitle }}
              </h1>
            </div>
            <div v-if="version || status" class="badges-row">
              <span v-if="version" class="version-badge">v{{ version }}</span>
              <span v-if="status" class="status-indicator" :class="statusClass">
                {{ statusLabel }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="tabs && tabs.length > 0" class="header-tabs">
        <TabNavigation
          :tabs="tabs"
          :active-tab="activeTab"
          @tab-change="$emit('tab-change', $event)"
        />
      </div>
    </div>

    <!-- Tooltip for truncated title -->
    <CommonTooltip
      :text="tooltip.text"
      :visible="tooltip.visible"
      :x="tooltip.x"
      :y="tooltip.y"
      id="page-title-tooltip"
    />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'DetailsPageHeader' });

import { computed, ref } from 'vue';
import { DxButton } from 'devextreme-vue';
import CommonTooltip from '@/components/common/Tooltip.vue';
import TabNavigation from '@/components/common/TabNavigation.vue';
import { useTooltip } from '@/composables/useTooltip';
import type { TabDefinition } from '@/types/tab';

export type { TabDefinition };

interface DetailsPageHeaderProps {
  /** Page title */
  title: string;
  /** Loading state for title */
  loading?: boolean;
  /** Error state for title */
  error?: string | null;
  /** Icon name or class */
  icon?: string;
  /** Version number or string */
  version?: number | string;
  /** Status of the entity */
  status?: 'enabled' | 'disabled' | null;
  /** Whether to show back button */
  showBackButton?: boolean;
  /** Back button aria label */
  backButtonLabel?: string;
  /** Tab definitions */
  tabs?: TabDefinition[];
  /** Active tab ID */
  activeTab?: string;
}

const props = withDefaults(defineProps<DetailsPageHeaderProps>(), {
  loading: false,
  error: null,
  icon: undefined,
  version: undefined,
  status: null,
  showBackButton: true,
  backButtonLabel: 'Go back',
  tabs: undefined,
  activeTab: 'details',
});

interface DetailsPageHeaderEmits {
  /** Emitted when back button is clicked */
  (e: 'back'): void;
  /** Emitted when tab is changed */
  (e: 'tab-change', tabId: string): void;
}

defineEmits<DetailsPageHeaderEmits>();

// Tooltip management
const { tooltip, showTooltip, hideTooltip } = useTooltip();
const titleRef = ref<HTMLElement | null>(null);

// Computed properties
const displayTitle = computed(() => {
  if (props.loading) return 'Loading...';
  if (props.error) return 'Error loading data';
  return props.title;
});

const iconClass = computed(() => {
  if (!props.icon) return '';
  return props.icon.startsWith('dx-icon') ? props.icon : `dx-icon dx-icon-${props.icon}`;
});

const statusClass = computed(() => ({
  'status-active': props.status === 'enabled',
  'status-disabled': props.status === 'disabled',
}));

const statusLabel = computed(() => {
  if (props.status === 'enabled') return 'Enabled';
  if (props.status === 'disabled') return 'Disabled';
  return '';
});

// Helper function to check if text is truncated
function isTextTruncated(element: HTMLElement | null): boolean {
  if (!element) return false;
  return element.scrollWidth > element.clientWidth;
}

// Tooltip event handlers
function handleTitleMouseEnter(event: MouseEvent): void {
  if (isTextTruncated(titleRef.value)) {
    showTooltip(event, displayTitle.value);
  }
}

function handleTitleMouseLeave(): void {
  hideTooltip();
}
</script>

<style scoped>
/* Header styles matching existing patterns */
.page-header {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(12px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  min-height: 3.5rem;
  gap: 1rem;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex: 1;
  min-width: 0;
}

.back-button {
  color: #6b7280 !important;
  background: transparent !important;
  border: none !important;
  padding: 0.5rem !important;
  min-width: auto !important;
  border-radius: 8px !important;
  transition: all 0.15s ease !important;
}

.back-button:hover {
  color: #374151 !important;
  background: rgba(243, 244, 246, 0.5) !important;
  transform: translateX(-1px) !important;
}

.page-title-section {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  max-width: 75%;
}

.icon-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: #ffffff;
  border-radius: 8px;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
  color: var(--kw-secondary-dark);
  padding: 0;
}

.icon-container i {
  font-size: 20px;
}

.title-content {
  flex: 1;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.25rem;
  flex-wrap: nowrap;
  min-width: 0;
  overflow: hidden;
}

.badges-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.page-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 500;
  color: #111827;
  line-height: 1.3;
  letter-spacing: -0.015em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 600px;
  flex-shrink: 1;
  min-width: 0;
  cursor: default;
}

.page-title:hover {
  cursor: help;
}

.version-badge {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
  color: white;
  padding: 0.125rem 0.5rem;
  border-radius: 12px;
  font-size: 0.625rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  box-shadow: 0 1px 3px rgba(100, 116, 139, 0.2);
}

.status-indicator {
  padding: 0.125rem 0.5rem;
  border-radius: 12px;
  font-size: 0.625rem;
  font-weight: 500;
  letter-spacing: 0.03em;
  border: 1px solid transparent;
}

.status-indicator.status-active {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
  border-color: #22c55e;
  box-shadow: 0 1px 3px rgba(34, 197, 94, 0.15);
}

.status-indicator.status-disabled {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #dc2626;
  border-color: #ef4444;
  box-shadow: 0 1px 3px rgba(239, 68, 68, 0.15);
}

.header-tabs {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
