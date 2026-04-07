<template>
  <div v-if="open" class="jw-modal-backdrop">
    <div class="jw-cancel-dialog">
      <div class="jw-cancel-header">
        <div class="jw-cancel-icon">!</div>
        <div class="jw-cancel-title">Warning</div>
      </div>
      <div class="jw-cancel-body">
        <p>Are you sure you want to cancel?</p>
        <p class="jw-cancel-subtitle">All unsaved data will be lost.</p>
      </div>
      <div class="jw-cancel-actions">
        <button
          type="button"
          class="jw-btn jw-btn-outlined jw-btn-neutral"
          @click="emit('confirm')"
        >
          Yes, Cancel
        </button>
        <button type="button" class="jw-btn jw-btn-warning" @click="emit('close')">
          No, Go Back
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ open: boolean }>();

const emit = defineEmits<{
  (e: 'confirm'): void;
  (e: 'close'): void;
}>();
</script>
