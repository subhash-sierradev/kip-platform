<template>
  <div class="pill-stack" :aria-label="ariaLabel || `Count: ${count}`">
    <span class="pill-badge">{{ count }}</span>
  </div>
</template>

<script setup lang="ts">
const { count, ariaLabel } = defineProps<{ count: number; ariaLabel?: string }>();
</script>

<style scoped>
.pill-stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 0;
  background: none;
  min-height: 0;
  margin: 0 28px;
  height: 100%;
}
.pill-badge {
  width: 28px;
  height: 22px;
  border-radius: 11px;
  border: 2px solid #1976d2;
  background: #fff;
  color: #1976d2;
  font-size: 0.95rem;
  font-weight: 600;
  font-family: 'Inter', 'Segoe UI', Arial, sans-serif;
  box-shadow: 0 2px 8px rgba(25, 118, 210, 0.08);
  outline: none;
  letter-spacing: 0.5px;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  transition:
    border-color 0.18s,
    color 0.18s,
    background 0.18s;
}
@media (max-width: 900px) {
  .pill-stack {
    margin: 0 8px;
  }
  .pill-badge {
    width: 22px;
    height: 18px;
    font-size: 0.8rem;
  }
}
</style>
