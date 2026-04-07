<template>
  <footer class="jw-footer">
    <div class="jw-footer-left">
      <button
        v-if="activeStep > 0"
        type="button"
        class="jw-btn jw-btn-outlined"
        @click="emit('previous')"
      >
        ← Previous
      </button>
    </div>

    <div class="jw-footer-right">
      <button type="button" class="jw-btn jw-btn-outlined jw-btn-neutral" @click="emit('cancel')">
        Cancel
      </button>

      <button
        v-if="activeStep < totalSteps - 1"
        type="button"
        class="jw-btn jw-btn-primary"
        :disabled="nextDisabled"
        @click="emit('next')"
      >
        Next →
      </button>

      <button
        v-else
        type="button"
        class="jw-btn jw-btn-primary"
        :disabled="submitDisabled"
        @click="emit('next')"
      >
        {{ submitLabel }}
      </button>
    </div>
  </footer>
</template>

<script setup lang="ts">
defineProps<{
  activeStep: number;
  totalSteps: number;
  nextDisabled: boolean;
  submitDisabled: boolean;
  submitLabel: string;
}>();

const emit = defineEmits<{
  (e: 'previous'): void;
  (e: 'next'): void;
  (e: 'cancel'): void;
}>();
</script>
