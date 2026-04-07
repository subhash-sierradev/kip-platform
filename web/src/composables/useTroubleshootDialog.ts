import { computed, ref } from 'vue';

import { type WebhookExecution } from '@/api/services/JiraWebhookService';

/**
 * Composable for managing troubleshoot dialog state and interactions
 * @returns Object containing dialog visibility, selected execution, and dialog control functions
 */
export function useTroubleshootDialog() {
  const troubleshootDialogVisible = ref(false);
  const selectedExecution = ref<WebhookExecution | null>(null);

  const currentExecution = computed(() => selectedExecution.value);

  const showTroubleshootDialog = (execution: WebhookExecution) => {
    selectedExecution.value = execution;
    troubleshootDialogVisible.value = true;
  };

  const closeTroubleshootDialog = () => {
    troubleshootDialogVisible.value = false;
    selectedExecution.value = null;
  };

  return {
    troubleshootDialogVisible,
    selectedExecution,
    currentExecution,
    showTroubleshootDialog,
    closeTroubleshootDialog,
  };
}
