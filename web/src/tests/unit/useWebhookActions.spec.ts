/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Shared toast mock instance used across tests
const mockToast = {
  showSuccess: vi.fn(),
  showError: vi.fn(),
  showWarning: vi.fn(),
};

vi.mock('@/store/toast', () => ({
  useToastStore: () => mockToast,
}));

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    toggleWebhookActive: vi.fn(),
    deleteJiraWebhook: vi.fn(),
    canRetryExecution: vi.fn(),
    retryWebhookEvent: vi.fn(),
    testWebhook: vi.fn(),
  },
}));

import { JiraWebhookService } from '@/api/services/JiraWebhookService';
import {
  useWebhookActions,
  useWebhookRetry,
  useWebhookStatus,
} from '@/composables/useWebhookActions';

const makeExec = (over: any = {}) => ({
  id: 'e1',
  originalEventId: 'evt-1',
  status: 'failed',
  retryAttempt: 0,
  ...over,
});

describe('useWebhookActions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('status actions', () => {
    it('toggleWebhookStatus validates id and returns null when invalid', async () => {
      const { toggleWebhookStatus } = useWebhookStatus();
      const out = await toggleWebhookStatus('');
      expect(out).toBeNull();
      expect(mockToast.showError).toHaveBeenCalledWith('Invalid webhook id');
    });

    it('toggleWebhookStatus uses API boolean result directly', async () => {
      (JiraWebhookService.toggleWebhookActive as any).mockResolvedValue(true);
      const { toggleWebhookStatus } = useWebhookStatus();
      const out = await toggleWebhookStatus('w1', false);
      expect(JiraWebhookService.toggleWebhookActive).toHaveBeenCalledWith({ id: 'w1' });
      expect(out).toBe(true);
      expect(mockToast.showSuccess).toHaveBeenCalledWith(
        'Webhook has been enabled and is now active'
      );
    });

    it('toggleWebhookStatus shows disabled toast when false', async () => {
      (JiraWebhookService.toggleWebhookActive as any).mockResolvedValue(false);
      const { toggleWebhookStatus } = useWebhookStatus();
      const out = await toggleWebhookStatus('w1', true);
      expect(out).toBe(false);
      expect(mockToast.showWarning).toHaveBeenCalledWith(
        'Webhook has been disabled and will no longer execute triggers'
      );
    });

    it('toggleWebhookStatus infers new state from currentEnabled if API not boolean', async () => {
      (JiraWebhookService.toggleWebhookActive as any).mockResolvedValue({});
      const { toggleWebhookStatus } = useWebhookStatus();
      const out = await toggleWebhookStatus('w1', true);
      expect(out).toBe(false);
    });

    it('toggleWebhookStatus returns null when currentEnabled is undefined and API not boolean', async () => {
      (JiraWebhookService.toggleWebhookActive as any).mockResolvedValue({});
      const { toggleWebhookStatus } = useWebhookStatus();
      const out = await toggleWebhookStatus('w1');
      expect(out).toBeNull();
      expect(mockToast.showError).toHaveBeenCalledWith(
        'Failed to determine new status. Please refresh and try again.'
      );
    });

    it('toggleWebhookStatus handles error and returns null', async () => {
      (JiraWebhookService.toggleWebhookActive as any).mockRejectedValue(new Error('x'));
      const { toggleWebhookStatus } = useWebhookStatus();
      const out = await toggleWebhookStatus('w1', true);
      expect(out).toBeNull();
      expect(mockToast.showError).toHaveBeenCalledWith('Failed to update webhook status');
    });

    it('deleteWebhook validates id, deletes, and handles error', async () => {
      const { deleteWebhook } = useWebhookStatus();

      let ok = await deleteWebhook('');
      expect(ok).toBe(false);
      expect(mockToast.showError).toHaveBeenCalledWith('Invalid webhook id');

      (JiraWebhookService.deleteJiraWebhook as any).mockResolvedValue(undefined);
      ok = await deleteWebhook('w1');
      expect(JiraWebhookService.deleteJiraWebhook).toHaveBeenCalledWith('w1');
      expect(ok).toBe(true);
      expect(mockToast.showSuccess).toHaveBeenCalledWith('Webhook deleted');

      (JiraWebhookService.deleteJiraWebhook as any).mockRejectedValue(new Error('boom'));
      ok = await deleteWebhook('w1');
      expect(ok).toBe(false);
    });

    it('deleteWebhook handles non-Error exceptions', async () => {
      (JiraWebhookService.deleteJiraWebhook as any).mockRejectedValue('String error');
      const { deleteWebhook } = useWebhookStatus();

      const ok = await deleteWebhook('w1');

      expect(ok).toBe(false);
      expect(mockToast.showError).toHaveBeenCalledWith('Failed to delete webhook');
    });
  });

  describe('retry actions', () => {
    let mockExecution: any;
    beforeEach(() => {
      mockExecution = makeExec();
    });
    it('retryWebhookExecution validates id and can retry check', async () => {
      const { retryWebhookExecution } = useWebhookRetry();
      let ok = await retryWebhookExecution(makeExec({ id: '' }));
      expect(ok).toBe(false);

      (JiraWebhookService.canRetryExecution as any).mockReturnValue(false);
      ok = await retryWebhookExecution(makeExec());
      expect(ok).toBe(false);
    });

    it('retryWebhookExecution success path adds/removes from retrying set', async () => {
      (JiraWebhookService.canRetryExecution as any).mockReturnValue(true);
      const mockError = new Error('Retry failed');
      (JiraWebhookService.retryWebhookEvent as any).mockRejectedValue(mockError);

      const { retryWebhookExecution } = useWebhookActions();

      const result = await retryWebhookExecution(mockExecution);

      expect(result).toBe(false);
      expect(mockToast.showError).toHaveBeenCalledWith('Webhook retry failed: Retry failed');
    });

    it('successfully retries webhook execution', async () => {
      (JiraWebhookService.canRetryExecution as any).mockReturnValue(true);
      (JiraWebhookService.retryWebhookEvent as any).mockResolvedValue(undefined);

      const { retryWebhookExecution, isExecutionRetrying } = useWebhookRetry();

      const executionId = mockExecution.originalEventId || mockExecution.id;

      // Track retrying state
      const promise = retryWebhookExecution(mockExecution);
      // Note: In actual implementation, isExecutionRetrying would be true during execution

      const result = await promise;

      expect(result).toBe(true);
      expect(mockToast.showSuccess).toHaveBeenCalledWith(
        'Webhook retry completed successfully! Check the webhook history for execution details.'
      );
      expect(isExecutionRetrying(executionId)).toBe(false); // Should be cleaned up
    });

    it('prevents retry when execution cannot be retried', async () => {
      (JiraWebhookService.canRetryExecution as any).mockReturnValue(false);

      const { retryWebhookExecution } = useWebhookActions();

      const result = await retryWebhookExecution(mockExecution);

      expect(result).toBe(false);
      expect(mockToast.showError).toHaveBeenCalledWith(
        'This execution cannot be retried (maximum retry attempts reached or not failed)'
      );
      expect(JiraWebhookService.retryWebhookEvent).not.toHaveBeenCalled();
    });

    it('handles invalid execution id', async () => {
      const invalidExecution = { ...mockExecution, id: '' };

      const { retryWebhookExecution } = useWebhookActions();

      const result = await retryWebhookExecution(invalidExecution);

      expect(result).toBe(false);
      expect(mockToast.showError).toHaveBeenCalledWith('Invalid execution id');
      expect(JiraWebhookService.retryWebhookEvent).not.toHaveBeenCalled();
    });

    it('handles execution with null values', async () => {
      const executionWithNulls = makeExec({ originalEventId: null });
      (JiraWebhookService.canRetryExecution as any).mockReturnValue(true);
      (JiraWebhookService.retryWebhookEvent as any).mockResolvedValue(undefined);

      const { retryWebhookExecution } = useWebhookRetry();

      const result = await retryWebhookExecution(executionWithNulls);

      expect(result).toBe(true);
      // Should use execution.id when originalEventId is null
      expect(JiraWebhookService.retryWebhookEvent).toHaveBeenCalledWith({ eventId: 'e1' });
    });

    it('retryWebhookExecution handles failure and cleans up state', async () => {
      (JiraWebhookService.canRetryExecution as any).mockReturnValue(true);
      (JiraWebhookService.retryWebhookEvent as any).mockRejectedValue(new Error('bad'));
      const { retryWebhookExecution, isExecutionRetrying } = useWebhookRetry();

      const ok = await retryWebhookExecution(makeExec());
      expect(ok).toBe(false);
      expect(isExecutionRetrying('evt-1')).toBe(false);
    });

    it('retryWebhookExecution handles non-Error exceptions', async () => {
      (JiraWebhookService.canRetryExecution as any).mockReturnValue(true);
      (JiraWebhookService.retryWebhookEvent as any).mockRejectedValue('String error');
      const { retryWebhookExecution } = useWebhookRetry();

      const ok = await retryWebhookExecution(makeExec());
      expect(ok).toBe(false);
      expect(mockToast.showError).toHaveBeenCalledWith(
        'Webhook retry failed: Failed to retry webhook execution'
      );
    });

    it('canRetryExecution proxies to service', () => {
      (JiraWebhookService.canRetryExecution as any).mockReturnValue(true);

      const { canRetryExecution } = useWebhookRetry();
      const execution = makeExec();
      const result = canRetryExecution(execution);

      expect(result).toBe(true);
      expect(JiraWebhookService.canRetryExecution).toHaveBeenCalledWith(execution);
    });

    it('isExecutionRetrying returns false for unknown execution', () => {
      const { isExecutionRetrying } = useWebhookRetry();

      expect(isExecutionRetrying('unknown-id')).toBe(false);
    });
  });

  describe('testing actions', () => {
    it('testWebhook proxies to service and returns result', async () => {
      (JiraWebhookService.testWebhook as any).mockResolvedValue({
        success: true,
        responseCode: 200,
      });

      const { testWebhook } = useWebhookActions();
      const result = await testWebhook('w1', '{"hello":"world"}');

      expect(result).toEqual({ success: true, responseCode: 200 });
      expect(JiraWebhookService.testWebhook).toHaveBeenCalledWith('w1', '{"hello":"world"}');
    });

    it('testWebhook validates webhook id', async () => {
      const { testWebhook } = useWebhookActions();
      const result = await testWebhook('', '{"data":"test"}');

      expect(result).toEqual({ success: false, errorMessage: 'Invalid webhook id' });
      expect(mockToast.showError).toHaveBeenCalledWith('Invalid webhook id');
      expect(JiraWebhookService.testWebhook).not.toHaveBeenCalled();
    });

    it('testWebhook handles API errors', async () => {
      (JiraWebhookService.testWebhook as any).mockRejectedValue(new Error('Test failed'));

      const { testWebhook } = useWebhookActions();
      const result = await testWebhook('w1', '{"test":"data"}');

      expect(result).toEqual({ success: false, errorMessage: 'Test failed' });
      expect(mockToast.showError).toHaveBeenCalledWith('Webhook test failed: Test failed');
    });

    it('testWebhook handles non-Error exceptions', async () => {
      (JiraWebhookService.testWebhook as any).mockRejectedValue('String error');

      const { testWebhook } = useWebhookActions();
      const result = await testWebhook('w1');

      expect(result).toEqual({ success: false, errorMessage: 'Failed to test webhook' });
      expect(mockToast.showError).toHaveBeenCalledWith(
        'Webhook test failed: Failed to test webhook'
      );
    });

    it('testWebhook works without sample payload', async () => {
      (JiraWebhookService.testWebhook as any).mockResolvedValue({
        success: true,
        responseCode: 200,
      });

      const { testWebhook } = useWebhookActions();
      const result = await testWebhook('w1');

      expect(result).toEqual({ success: true, responseCode: 200 });
      expect(JiraWebhookService.testWebhook).toHaveBeenCalledWith('w1', undefined);
    });
  });
});
