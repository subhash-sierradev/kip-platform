<template>
  <div v-if="open" class="confirm">
    <div class="box">
      <h4>{{ title }}</h4>
      <p class="msg" v-for="line in message.split('\n')" :key="line">{{ line }}</p>
      <div class="row">
        <button class="btn" @click="$emit('confirm')">{{ confirmText }}</button>
        <button class="btn warn" @click="$emit('cancel')">{{ cancelText }}</button>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
defineProps<{
  open: boolean;
  title: string;
  message: string;
  confirmText: string;
  cancelText: string;
}>();
</script>
<style scoped>
.confirm {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.2);
  display: grid;
  place-items: center;
}
.box {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  min-width: 320px;
}
.row {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 12px;
}
.btn {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
}
.btn.warn {
  background: #f59e0b;
  color: #fff;
  border-color: #f59e0b;
}
.msg {
  color: #4b5563;
}
</style>
