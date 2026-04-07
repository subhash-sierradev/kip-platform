import { ref } from 'vue';

import { JiraWebhookService, type WebhookExecution } from '@/api/services/JiraWebhookService';

/**
 * Composable for managing webhook execution history data
 * @param webhookId - The webhook ID to fetch history for
 * @returns Object containing loading state, execution history data, and fetch function
 */
export function useWebhookHistoryData(webhookId: string) {
  const loading = ref(false);
  const executionHistory = ref<WebhookExecution[]>([]);

  const fetchWebhookHistory = async () => {
    if (!webhookId) {
      console.error('Webhook ID is required to fetch execution history');
      return;
    }

    loading.value = true;
    try {
      const data = await JiraWebhookService.getWebhookExecutions(webhookId);
      executionHistory.value = data;
    } catch (error) {
      console.error('Failed to fetch webhook history:', error);
    } finally {
      loading.value = false;
    }
  };

  return {
    loading,
    executionHistory,
    fetchWebhookHistory,
  };
}
