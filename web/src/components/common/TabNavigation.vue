<template>
  <div class="custom-tabs" role="tablist" :aria-label="ariaLabel">
    <button
      v-for="tab in tabs"
      :key="tab.id"
      @click="handleTabClick(tab.id)"
      @keydown="handleKeyNavigation"
      @mouseenter="$emit('tab-mouseenter', tab.id, $event)"
      @mouseleave="$emit('tab-mouseleave')"
      @mousemove="$emit('tab-mousemove', $event)"
      :class="['tab-button', { active: activeTab === tab.id }]"
      :aria-selected="activeTab === tab.id"
      :aria-controls="`panel-${tab.id}`"
      :id="`tab-${tab.id}`"
      role="tab"
      :tabindex="activeTab === tab.id ? 0 : -1"
      type="button"
    >
      <i v-if="tab.iconClass" :class="tab.iconClass" class="tab-icon" aria-hidden="true"></i>
      <span class="tab-text">{{ tab.label }}</span>
    </button>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'TabNavigation' });

import { nextTick } from 'vue';
import type { TabDefinition } from '@/types/tab';

interface TabNavigationProps {
  /** Tab definitions */
  tabs: TabDefinition[];
  /** Active tab ID */
  activeTab: string;
  /** Aria label for tab list */
  ariaLabel?: string;
}

const props = withDefaults(defineProps<TabNavigationProps>(), {
  ariaLabel: 'Navigation tabs',
});

interface TabNavigationEmits {
  /** Emitted when tab is clicked */
  (e: 'tab-change', tabId: string): void;
  /** Emitted when mouse enters tab */
  (e: 'tab-mouseenter', tabId: string, event: MouseEvent): void;
  /** Emitted when mouse leaves tab */
  (e: 'tab-mouseleave'): void;
  /** Emitted when mouse moves over tab */
  (e: 'tab-mousemove', event: MouseEvent): void;
}

const emit = defineEmits<TabNavigationEmits>();

// Tab interaction handlers
function handleTabClick(tabId: string): void {
  if (tabId !== props.activeTab) {
    emit('tab-change', tabId);

    // Focus management for accessibility
    nextTick(() => {
      const newTab = document.getElementById(`tab-${tabId}`);
      newTab?.focus();
    });
  }
}

function handleKeyNavigation(event: KeyboardEvent): void {
  const currentIndex = props.tabs.findIndex(tab => tab.id === props.activeTab);
  let newIndex = currentIndex;

  switch (event.key) {
    case 'ArrowLeft':
    case 'ArrowUp':
      event.preventDefault();
      newIndex = currentIndex > 0 ? currentIndex - 1 : props.tabs.length - 1;
      break;
    case 'ArrowRight':
    case 'ArrowDown':
      event.preventDefault();
      newIndex = currentIndex < props.tabs.length - 1 ? currentIndex + 1 : 0;
      break;
    case 'Home':
      event.preventDefault();
      newIndex = 0;
      break;
    case 'End':
      event.preventDefault();
      newIndex = props.tabs.length - 1;
      break;
    default:
      return;
  }

  if (newIndex !== currentIndex && newIndex >= 0 && newIndex < props.tabs.length) {
    handleTabClick(props.tabs[newIndex].id);
  }
}
</script>

<style scoped>
.custom-tabs {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background: transparent;
  width: auto;
  padding: 0.5rem 1rem;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.8);
  backdrop-filter: blur(8px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(229, 231, 235, 0.4);
}

.tab-button {
  background: transparent;
  border: none;
  border-radius: 10px;
  color: #64748b;
  font-weight: 500;
  padding: 0.625rem 1.25rem;
  margin: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  font-size: 0.8125rem;
  white-space: nowrap;
  position: relative;
  min-width: fit-content;
  letter-spacing: 0.025em;
  text-transform: capitalize;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tab-button:hover {
  color: #475569;
  background: rgba(248, 250, 252, 0.8);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.tab-button.active {
  color: #f97316;
  background: #ffffff;
  font-weight: 600;
  box-shadow:
    0 4px 16px rgba(249, 115, 22, 0.2),
    0 2px 8px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.tab-button:focus {
  outline: 2px solid #f97316;
  outline-offset: 2px;
}

.tab-icon {
  font-size: 1rem;
  color: inherit;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-button.active .tab-icon {
  color: #f97316;
  transform: scale(1.1);
}

.tab-text {
  font-size: 0.8125rem;
  font-weight: inherit;
  line-height: 1.2;
  display: block;
}
</style>
