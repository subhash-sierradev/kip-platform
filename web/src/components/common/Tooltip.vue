<template>
  <div
    v-if="visible"
    :id="id"
    :class="['custom-tooltip', { 'menu-tooltip': menuStyle }]"
    :style="{ left: x + 'px', top: y + 'px' }"
    role="tooltip"
  >
    <template v-if="menuStyle">
      <div class="tooltip-arrow"></div>
      <div class="tooltip-title">{{ title }}</div>
      <div class="tooltip-description">{{ description }}</div>
    </template>
    <template v-else>
      <slot>{{ text }}</slot>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'CommonTooltip' });
/**
 * Tooltip accessibility:
 * To ensure screen reader support, pass a unique `id` prop to this component and set `aria-describedby` on the trigger element to match.
 * Example:
 *   <CommonTooltip :id="'my-tooltip'" ... />
 *   <button aria-describedby="my-tooltip">Hover me</button>
 */

const props = defineProps<{
  text?: string;
  title?: string;
  description?: string;
  visible: boolean;
  x: number;
  y: number;
  id?: string;
  menuStyle?: boolean;
}>();

if (props.menuStyle && (!props.title || !props.description)) {
  console.warn('Tooltip with menuStyle requires both title and description');
}
</script>

<style scoped>
.custom-tooltip {
  position: fixed;
  z-index: 9999;
  background: #222;
  color: #fff;
  padding: 8px 14px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
  pointer-events: none;
  max-width: 320px;
  word-break: break-word;
  transition: opacity 0.15s;
}

.menu-tooltip {
  background: #888;
  padding: 12px 18px 10px 18px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 400;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.18);
  min-width: 220px;
  max-width: 340px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.menu-tooltip .tooltip-arrow {
  position: absolute;
  left: -12px;
  top: 18px;
  width: 0;
  height: 0;
  border-top: 12px solid transparent;
  border-bottom: 12px solid transparent;
  border-right: 12px solid #888;
  filter: drop-shadow(0 2px 2px rgba(0, 0, 0, 0.1));
}
.menu-tooltip .tooltip-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 2px;
  color: #fff;
}
.menu-tooltip .tooltip-description {
  font-size: 13px;
  font-weight: 400;
  color: #f5f5f5;
  margin-left: 1px;
}
</style>
