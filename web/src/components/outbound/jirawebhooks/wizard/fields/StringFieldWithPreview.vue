<template>
  <div class="ms-string-with-insert">
    <div class="ms-text-input-stack">
      <input
        v-show="!previewOpen"
        class="ms-input ms-value"
        :value="value"
        @input="onInput"
        placeholder="Enter value"
      />
      <div v-show="previewOpen" class="ms-inline-preview" aria-live="polite">
        {{ previewText }}
      </div>
    </div>

    <div class="ms-icon-col">
      <Tooltip
        :visible="actionTipVisible"
        :x="actionTipX"
        :y="actionTipY"
        :id="actionTipId"
        :text="actionTipText"
      />

      <button
        type="button"
        class="ps-add-icon"
        aria-label="Add JSON key"
        :aria-describedby="actionTipId"
        @mouseenter="e => emit('action-enter', { text: 'Insert fields', event: e })"
        @mousemove="emit('action-move', $event)"
        @mouseleave="emit('action-leave')"
        @click="emit('open-insert')"
      >
        <i class="dx-icon dx-icon-add"></i>
      </button>

      <button
        type="button"
        class="ps-eye-icon"
        :disabled="!value.trim()"
        aria-label="Toggle preview"
        :aria-describedby="actionTipId"
        @mouseenter="
          e =>
            emit('action-enter', { text: previewOpen ? 'Hide preview' : 'Show preview', event: e })
        "
        @mousemove="emit('action-move', $event)"
        @mouseleave="emit('action-leave')"
        @click="emit('toggle-preview')"
      >
        <svg
          v-if="!previewOpen"
          class="ps-eye-svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
          <circle cx="12" cy="12" r="3"></circle>
        </svg>
        <svg
          v-else
          class="ps-eye-svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
          ></path>
          <line x1="1" y1="1" x2="23" y2="23"></line>
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import Tooltip from '@/components/common/Tooltip.vue';

defineProps<{
  value: string;
  previewText: string;
  previewOpen: boolean;
  actionTipVisible: boolean;
  actionTipX: number;
  actionTipY: number;
  actionTipId: string;
  actionTipText: string;
}>();

const emit = defineEmits<{
  (e: 'update:value', value: string): void;
  (e: 'string-input'): void;
  (e: 'open-insert'): void;
  (e: 'toggle-preview'): void;
  (e: 'action-enter', payload: { text: string; event: MouseEvent }): void;
  (e: 'action-move', event: MouseEvent): void;
  (e: 'action-leave'): void;
}>();

function onInput(event: Event) {
  const target = event.target as HTMLInputElement;
  emit('update:value', target.value);
  emit('string-input');
}
</script>
