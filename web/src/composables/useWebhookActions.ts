import { ref } from 'vue';

import { JiraWebhookService, type WebhookExecution } from '@/api/services/JiraWebhookService';
import { useToastStore } from '@/store/toast';

// Helper functions
function determineNewStatus(apiResult: unknown, currentEnabled?: boolean): boolean | null {
  if (typeof apiResult === 'boolean') {
    return apiResult;
  }
  if (typeof currentEnabled === 'boolean') {
    return !currentEnabled;
  }
  return null;
}

function showStatusToast(toast: ReturnType<typeof useToastStore>, enabled: boolean): void {
  if (enabled) {
    toast.showSuccess('Webhook has been enabled and is now active');
  } else {
    toast.showWarning('Webhook has been disabled and will no longer execute triggers');
  }
}

function handleRetryError(error: unknown, toast: ReturnType<typeof useToastStore>): void {
  console.error('Failed to retry webhook execution:', error);
  const errorMessage = error instanceof Error ? error.message : 'Failed to retry webhook execution';
  toast.showError(`Webhook retry failed: ${errorMessage}`);
}

function handleTestError(error: unknown, toast: ReturnType<typeof useToastStore>): string {
  console.error('Failed to test webhook:', error);
  const errorMessage = error instanceof Error ? error.message : 'Failed to test webhook';
  toast.showError(`Webhook test failed: ${errorMessage}`);
  return errorMessage;
}

// Webhook status management composable
export function useWebhookStatus() {
  const toast = useToastStore();

  async function toggleWebhookStatus(
    webhookId: string,
    currentEnabled?: boolean
  ): Promise<boolean | null> {
    if (!webhookId) {
      toast.showError('Invalid webhook id');
      return null;
    }
    try {
      const apiResult = await JiraWebhookService.toggleWebhookActive({ id: webhookId });
      const newEnabled = determineNewStatus(apiResult, currentEnabled);

      if (newEnabled === null) {
        console.error(
          'toggleWebhookStatus: currentEnabled is undefined and API did not return a boolean'
        );
        toast.showError('Failed to determine new status. Please refresh and try again.');
        return null;
      }

      showStatusToast(toast, newEnabled);
      return newEnabled;
    } catch (error) {
      console.error('Failed to toggle webhook status:', error);
      toast.showError('Failed to update webhook status');
      return null;
    }
  }

  async function deleteWebhook(webhookId: string): Promise<boolean> {
    if (!webhookId) {
      toast.showError('Invalid webhook id');
      return false;
    }
    try {
      await JiraWebhookService.deleteJiraWebhook(webhookId);
      toast.showSuccess('Webhook deleted');
      return true;
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Failed to delete webhook';
      console.error('Failed to delete webhook', e);
      toast.showError(message);
      return false;
    }
  }

  return {
    toggleWebhookStatus,
    deleteWebhook,
  };
}

// Webhook execution retry composable
export function useWebhookRetry() {
  const toast = useToastStore();
  const retryingExecutions = ref(new Set<string>());

  async function retryWebhookExecution(execution: WebhookExecution): Promise<boolean> {
    if (!execution.id) {
      toast.showError('Invalid execution id');
      return false;
    }

    if (!JiraWebhookService.canRetryExecution(execution)) {
      toast.showError(
        'This execution cannot be retried (maximum retry attempts reached or not failed)'
      );
      return false;
    }

    const executionId = execution.originalEventId || execution.id;
    retryingExecutions.value.add(executionId);

    try {
      await JiraWebhookService.retryWebhookEvent({ eventId: executionId });
      toast.showSuccess(
        'Webhook retry completed successfully! Check the webhook history for execution details.'
      );
      return true;
    } catch (error) {
      handleRetryError(error, toast);
      return false;
    } finally {
      retryingExecutions.value.delete(executionId);
    }
  }

  function isExecutionRetrying(executionId: string): boolean {
    return retryingExecutions.value.has(executionId);
  }

  function canRetryExecution(execution: WebhookExecution): boolean {
    return JiraWebhookService.canRetryExecution(execution);
  }

  return {
    retryWebhookExecution,
    isExecutionRetrying,
    canRetryExecution,
  };
}

// Webhook testing composable
export function useWebhookTesting() {
  const toast = useToastStore();

  async function testWebhook(
    webhookId: string,
    samplePayload?: string
  ): Promise<{
    success: boolean;
    responseCode?: number;
    responseBody?: string;
    errorMessage?: string;
  }> {
    if (!webhookId) {
      toast.showError('Invalid webhook id');
      return { success: false, errorMessage: 'Invalid webhook id' };
    }

    try {
      const result = await JiraWebhookService.testWebhook(webhookId, samplePayload);

      // Note: Toast messages are handled by the calling component for better context
      return result;
    } catch (error) {
      const errorMessage = handleTestError(error, toast);
      return { success: false, errorMessage };
    }
  }

  return {
    testWebhook,
  };
}

// Main composable that combines all webhook actions (maintains backward compatibility)
export function useWebhookActions() {
  const statusActions = useWebhookStatus();
  const retryActions = useWebhookRetry();
  const testingActions = useWebhookTesting();

  return {
    ...statusActions,
    ...retryActions,
    ...testingActions,
  };
}
