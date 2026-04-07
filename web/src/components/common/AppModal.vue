<template>
  <teleport to="body">
    <div
      v-if="openModel"
      class="kw-modal-backdrop"
      data-testid="kw-modal-backdrop"
      @click="onBackdropClick"
    >
      <div
        ref="dialogRef"
        class="kw-modal"
        :class="[sizeClass, fixedHeightClass]"
        data-testid="kw-modal-card"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="title ? titleId : undefined"
        tabindex="-1"
        v-bind="attrs"
        @click.stop
      >
        <header class="kw-modal__header">
          <div class="kw-modal__header-inner">
            <div>
              <slot name="header">
                <h2 v-if="title" class="kw-modal__title" :id="titleId">{{ title }}</h2>
              </slot>
            </div>

            <div class="kw-modal__header-right">
              <slot name="headerRight" />
              <button
                type="button"
                class="kw-modal__close"
                aria-label="Close dialog"
                @click="requestClose"
              >
                ✕
              </button>
            </div>
          </div>
        </header>

        <main class="kw-modal__content">
          <slot />
        </main>

        <footer v-if="$slots.footer" class="kw-modal__footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, useAttrs, watch } from 'vue';

defineOptions({ name: 'AppModal', inheritAttrs: false });

type ModalSize = 'sm' | 'md' | 'lg';

interface Props {
  open: boolean;
  title?: string;
  size?: ModalSize;
  fixedHeight?: boolean;
  closeOnOverlayClick?: boolean;
  closeOnEsc?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  title: undefined,
  size: 'md',
  fixedHeight: false,
  closeOnOverlayClick: true,
  closeOnEsc: true,
});

const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const attrs = useAttrs();
const dialogRef = ref<HTMLElement | null>(null);

const openModel = computed({
  get: () => props.open,
  set: (value: boolean) => emit('update:open', value),
});

const sizeClass = computed(() => {
  if (props.size === 'sm') return 'kw-modal--sm';
  if (props.size === 'lg') return 'kw-modal--lg';
  return 'kw-modal--md';
});

const fixedHeightClass = computed(() => (props.fixedHeight ? 'kw-modal--fixed-height' : ''));

const titleId = `kw-modal-title-${Math.random().toString(36).slice(2)}`;

let previousBodyOverflow = '';
let previousActiveElement: HTMLElement | null = null;

const requestClose = () => {
  openModel.value = false;
};

const onBackdropClick = () => {
  if (props.closeOnOverlayClick) {
    requestClose();
  }
};

const onKeyDown = (event: KeyboardEvent) => {
  if (!props.closeOnEsc) return;
  if (event.key === 'Escape' && props.open) {
    requestClose();
  }
};

watch(
  () => props.open,
  async isOpen => {
    if (isOpen) {
      previousActiveElement = document.activeElement as HTMLElement | null;
      previousBodyOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      document.addEventListener('keydown', onKeyDown);

      await nextTick();
      dialogRef.value?.focus();
      return;
    }

    document.removeEventListener('keydown', onKeyDown);
    document.body.style.overflow = previousBodyOverflow || '';
    previousActiveElement?.focus?.();
    previousActiveElement = null;
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeyDown);
  document.body.style.overflow = previousBodyOverflow || '';
});
</script>

<style>
@import '@/styles/kw-modal.css';
</style>
