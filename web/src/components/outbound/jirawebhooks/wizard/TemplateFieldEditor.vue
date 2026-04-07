<template>
  <div class="ms-field">
    <div class="ms-field-header">
      <label class="ms-label">
        {{ label }}
        <span v-if="required" class="ms-required">*</span>
      </label>
      <div class="ms-actions">
        <button
          type="button"
          class="ps-add-icon"
          title="Insert fields"
          :aria-label="`Add JSON key to ${label}`"
          :aria-describedby="actionTipId"
          @mouseenter="e => onActionEnter('Insert fields', e)"
          @mousemove="onActionMove"
          @mouseleave="onActionLeave"
          @click="onOpenInsert"
        >
          <i class="dx-icon dx-icon-add"></i>
        </button>
        <button
          type="button"
          class="ps-eye-icon"
          :disabled="!hasValue"
          :title="showPreview ? 'Hide preview' : 'Show preview'"
          aria-label="Toggle preview"
          :aria-describedby="actionTipId"
          @mouseenter="e => onActionEnter(showPreview ? 'Hide preview' : 'Show preview', e)"
          @mousemove="onActionMove"
          @mouseleave="onActionLeave"
          @click="emit('toggle-preview')"
        >
          <svg
            v-if="!showPreview"
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
              d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
            ></path>
            <line x1="1" y1="1" x2="23" y2="23"></line>
          </svg>
        </button>
      </div>
    </div>
    <div class="ms-textarea-row">
      <div class="ms-textarea-stack">
        <textarea
          ref="inputRef"
          v-show="!showPreview"
          :class="textareaClass"
          :rows="rows"
          :placeholder="placeholder"
          v-model="value"
          @click="captureSelection"
          @keyup="captureSelection"
        ></textarea>
        <div v-show="showPreview" :class="previewClass" aria-live="polite">
          {{ previewText || 'Preview is empty' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';

type Target = 'summary' | 'description';

type ActionEnterHandler = (text: string, e: MouseEvent) => void;
type ActionMoveHandler = (e: MouseEvent) => void;
type ActionLeaveHandler = () => void;

const props = defineProps<{
  label: string;
  required?: boolean;
  modelValue: string;
  placeholder: string;
  showPreview: boolean;
  previewText: string;
  target: Target;
  actionTipId: string;
  onActionEnter: ActionEnterHandler;
  onActionMove: ActionMoveHandler;
  onActionLeave: ActionLeaveHandler;
  rows?: number;
  textareaClass?: string;
  previewClass?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void;
  (e: 'toggle-preview'): void;
  (e: 'open-insert', target: Target): void;
}>();

const inputRef = ref<HTMLTextAreaElement | null>(null);
const selection = ref<{ start: number; end: number } | null>(null);

const value = computed({
  get: () => props.modelValue,
  set: (nextValue: string) => emit('update:modelValue', nextValue),
});

const hasValue = computed(() => (props.modelValue || '').trim().length > 0);

const rows = computed(() => props.rows ?? 4);
const textareaClass = computed(() => props.textareaClass ?? 'ms-input ms-textarea');
const previewClass = computed(() => props.previewClass ?? 'ms-inline-preview');

function captureSelection() {
  const el = inputRef.value;
  if (!el) return;
  selection.value = { start: el.selectionStart ?? 0, end: el.selectionEnd ?? 0 };
}

function onOpenInsert() {
  captureSelection();
  emit('open-insert', props.target);
}

async function insertPlaceholder(placeholder: string) {
  const text = props.modelValue || '';
  const sel = selection.value;
  let start = sel?.start ?? text.length;
  let end = sel?.end ?? text.length;
  if (start < 0) start = 0;
  if (end < start) end = start;

  const needsSpace = start > 0 && !/\s/.test(text[start - 1]);
  const prefix = text.slice(0, start);
  const suffix = text.slice(end);
  const toInsert = `${needsSpace ? ' ' : ''}${placeholder}`;
  const newValue = prefix + toInsert + suffix;
  emit('update:modelValue', newValue);

  await nextTick();
  const el = inputRef.value;
  if (el) {
    const caret = (prefix + toInsert).length;
    el.focus();
    try {
      el.setSelectionRange(caret, caret);
    } catch {
      /* ignore */
    }
  }
}

defineExpose({ insertPlaceholder });
</script>
<style src="./MappingStep.css" scoped></style>
