<template>
  <div class="error-panel" role="alert">{{ message }}</div>
</template>
<script setup lang="ts">
defineProps<{ message: string }>();
</script>
<style scoped>
/* TODO: Move alert styles to global theme; leaving temporary */
.error-panel {
  background: #fdecea;
  color: #86181d;
  border: 1px solid #f5c2c7;
  padding: 0.75rem 0.9rem;
  border-radius: 4px;
  font-size: 0.85rem;
}
</style>
