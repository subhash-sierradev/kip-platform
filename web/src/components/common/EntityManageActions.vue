<template>
  <div class="details-card actions-card">
    <div class="card-header">
      <h3 class="card-title">
        <i class="dx-icon dx-icon-toolbox title-icon" aria-hidden="true"></i>
        {{ title }}
      </h3>
    </div>
    <div class="card-body">
      <div class="actions-grid" role="group" :aria-label="`${title} actions`">
        <DxButton
          v-for="action in actions"
          :key="action.id"
          :text="action.label"
          :icon="action.iconType === 'devextreme' ? action.icon : undefined"
          type="normal"
          styling-mode="outlined"
          :class="['action-button', `${action.id}-btn`, action.variant]"
          :data-testid="`${action.id}-button`"
          :disabled="action.disabled || globalLoading"
          @click="$emit('action', action.id)"
        >
          <template v-if="action.iconType === 'lucide'" #default>
            <div class="dx-button-content">
              <component :is="action.lucideIcon" :size="16" class="lucide-icon action-icon" />
              <span class="dx-button-text">{{ action.label }}</span>
            </div>
          </template>
        </DxButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'EntityManageActions' });

import { DxButton } from 'devextreme-vue';

export interface ActionDefinition {
  /** Unique action identifier */
  id: string;
  /** Display label for the action */
  label: string;
  /** DevExtreme icon class (optional) */
  icon?: string;
  /** Icon type - determines how the icon is rendered */
  iconType?: 'devextreme' | 'lucide';
  /** Lucide icon component (when iconType is 'lucide') */
  lucideIcon?: any;
  /** Visual variant for styling */
  variant?: 'primary' | 'secondary' | 'danger';
  /** Whether the action is disabled */
  disabled?: boolean;
}

interface EntityManageActionsProps {
  /** List of actions to display */
  actions: ActionDefinition[];
  /** Title for the actions panel */
  title?: string;
  /** Global loading state that disables all actions */
  globalLoading?: boolean;
}

withDefaults(defineProps<EntityManageActionsProps>(), {
  title: 'Manage',
  globalLoading: false,
});

interface EntityManageActionsEmits {
  /** Emitted when an action is clicked */
  (e: 'action', actionId: string): void;
}

defineEmits<EntityManageActionsEmits>();
</script>

<style scoped>
.details-card {
  background: white;
  border-radius: 12px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.12),
    0 1px 2px rgba(0, 0, 0, 0.24);
  border: 1px solid #e5e7eb;
  overflow: hidden;
  height: fit-content;
}

.card-header {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e5e7eb;
  padding: 1rem 1.25rem;
}

.card-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.title-icon {
  font-size: 1rem;
}

.card-body {
  padding: 1.25rem;
}

.actions-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-button {
  text-align: center !important;
  justify-content: center !important;
  min-height: 44px !important;
  font-weight: 500 !important;
  border-radius: 8px !important;
  transition: all 0.2s ease !important;
  padding: 0 16px !important;
  display: flex !important;
  align-items: center !important;
}

/* Override DevExtreme's button wrapper completely */
.action-button.dx-button {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  height: 44px !important;
  line-height: 1 !important;
}

.action-button:hover:not(.dx-state-disabled) {
  transform: translateY(-1px) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
}

/* Force all DevExtreme button content to center */
.action-button .dx-button-content,
.action-button.dx-button .dx-button-content {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 8px !important;
  height: 44px !important;
  line-height: 1.2 !important;
  min-height: 44px !important;
  width: 100% !important;
  flex-direction: row !important;
}

/* Force DevExtreme button wrapper alignment */
.action-button .dx-button,
.action-button.dx-widget {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  height: 44px !important;
  line-height: 1 !important;
}

/* Force DevExtreme icon alignment */
.action-button .dx-icon,
.action-button.dx-button .dx-icon {
  margin: 0 !important;
  font-size: 16px !important;
  line-height: 1 !important;
  vertical-align: middle !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  height: 16px !important;
  width: 16px !important;
}

/* Force DevExtreme button text alignment */
.action-button .dx-button-text,
.action-button.dx-button .dx-button-text {
  font-size: 14px !important;
  line-height: 1.2 !important;
  margin: 0 !important;
  vertical-align: middle !important;
  display: inline-block !important;
  height: auto !important;
  align-self: center !important;
}

/* Lucide icon specific styling */
.lucide-icon.action-icon {
  margin: 0;
  flex-shrink: 0;
  width: 16px;
  height: 16px;
  vertical-align: middle;
}

.action-button:focus {
  outline: 2px solid #3b82f6 !important;
  outline-offset: 2px !important;
}

/* Action variants */
.action-button.primary {
  border-color: #f97316 !important;
  color: #f97316 !important;
}

.action-button.primary:hover:not(.dx-state-disabled) {
  background: #fff7ed !important;
  border-color: #ea580c !important;
  color: #ea580c !important;
}

.action-button.secondary {
  border-color: #6b7280 !important;
  color: #6b7280 !important;
}

.action-button.secondary:hover:not(.dx-state-disabled) {
  background: #f9fafb !important;
  border-color: #4b5563 !important;
  color: #4b5563 !important;
}

.action-button.danger {
  border-color: #dc2626 !important;
  color: #dc2626 !important;
}

.action-button.danger:hover:not(.dx-state-disabled) {
  background: #fef2f2 !important;
  border-color: #b91c1c !important;
  color: #b91c1c !important;
}

/* Disabled state */
.action-button.dx-state-disabled {
  opacity: 0.5 !important;
  transform: none !important;
}
</style>
