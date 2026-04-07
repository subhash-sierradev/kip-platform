<template>
  <div class="step-toolbar" :class="{ compact }" role="group" aria-label="Step toolbar">
    <div class="step-info">
      <span class="step-number">Step {{ stepNumber }}</span>
      <span class="step-name">{{ stepName }}</span>
      <span v-if="subtitle" class="step-subtitle">{{ subtitle }}</span>
    </div>
    <div class="step-actions">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  stepName: string;
  stepNumber: number | string;
  subtitle?: string;
  compact?: boolean;
}

const props = defineProps<Props>();
const { stepName, stepNumber, subtitle, compact } = props;
</script>

<style scoped>
.step-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--dx-color-border, #e0e0e0);
  background: var(--dx-color-bg, #fff);
}
.step-toolbar.compact {
  padding: 6px 10px;
}
.step-info {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.step-number {
  font-weight: 600;
  color: var(--dx-color-primary, #0078d4);
}
.step-name {
  font-weight: 500;
}
.step-subtitle {
  color: #6b7280;
  font-size: 0.9em;
}
.step-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
