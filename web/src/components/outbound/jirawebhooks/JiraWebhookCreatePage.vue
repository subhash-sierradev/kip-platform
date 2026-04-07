<template>
  <div class="wizard-overlay">
    <div class="wizard-container">
      <JiraWebhookWizard :open="true" @close="handleClose" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import JiraWebhookWizard from './wizard/JiraWebhookWizard.vue';

const router = useRouter();
function handleClose() {
  router.push('/outbound/jira-webhook');
}
</script>

<style scoped>
.wizard-overlay {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.35);
  display: grid;
  place-items: center;
  z-index: 1000;
}
</style>
