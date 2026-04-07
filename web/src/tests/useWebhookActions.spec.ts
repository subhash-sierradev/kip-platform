import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

// Mocks
vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    toggleWebhookActive: vi.fn(),
    deleteJiraWebhook: vi.fn(),
    canRetryExecution: vi.fn(),
    retryWebhookEvent: vi.fn(),
    testWebhook: vi.fn(),
  },
}));

// Create a controllable mock toast store
const showSuccess = vi.fn();
const showWarning = vi.fn();
const showError = vi.fn();

vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showSuccess,
    showWarning,
    showError,
  }),
}));

// Import mocked modules after vi.mock
import { JiraWebhookService } from '@/api/services/JiraWebhookService';
import { useWebhookActions } from '@/composables/useWebhookActions';

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('toggleWebhookStatus', () => {
  it('returns null and shows error when id is invalid', async () => {
    const { toggleWebhookStatus } = useWebhookActions();
    const result = await toggleWebhookStatus('');
    expect(result).toBeNull();
    expect(showError).toHaveBeenCalledWith('Invalid webhook id');
    expect(JiraWebhookService.toggleWebhookActive).not.toHaveBeenCalled();
  });

  it('returns API boolean and shows success when enabled', async () => {
    (
      JiraWebhookService.toggleWebhookActive as unknown as ReturnType<typeof vi.fn>
    ).mockResolvedValue(true);
    const { toggleWebhookStatus } = useWebhookActions();
    const result = await toggleWebhookStatus('abc');
    expect(result).toBe(true);
    expect(JiraWebhookService.toggleWebhookActive).toHaveBeenCalledWith({ id: 'abc' });
    expect(showSuccess).toHaveBeenCalledWith('Webhook has been enabled and is now active');
    expect(showWarning).not.toHaveBeenCalled();
    expect(showError).not.toHaveBeenCalled();
  });

  it('returns API boolean and shows warning when disabled', async () => {
    (
      JiraWebhookService.toggleWebhookActive as unknown as ReturnType<typeof vi.fn>
    ).mockResolvedValue(false);
    const { toggleWebhookStatus } = useWebhookActions();
    const result = await toggleWebhookStatus('abc');
    expect(result).toBe(false);
    expect(showWarning).toHaveBeenCalledWith(
      'Webhook has been disabled and will no longer execute triggers'
    );
    expect(showSuccess).not.toHaveBeenCalled();
  });

  it('when API does not return boolean, invert currentEnabled and show appropriate toast', async () => {
    (
      JiraWebhookService.toggleWebhookActive as unknown as ReturnType<typeof vi.fn>
    ).mockResolvedValue(undefined);
    const { toggleWebhookStatus } = useWebhookActions();
    const result1 = await toggleWebhookStatus('abc', true);
    expect(result1).toBe(false);
    expect(showWarning).toHaveBeenCalledWith(
      'Webhook has been disabled and will no longer execute triggers'
    );

    vi.clearAllMocks();
    const result2 = await toggleWebhookStatus('abc', false);
    expect(result2).toBe(true);
    expect(showSuccess).toHaveBeenCalledWith('Webhook has been enabled and is now active');
  });

  it('returns null and shows error when API is non-boolean and currentEnabled is undefined', async () => {
    (
      JiraWebhookService.toggleWebhookActive as unknown as ReturnType<typeof vi.fn>
    ).mockResolvedValue(undefined);
    const { toggleWebhookStatus } = useWebhookActions();
    const result = await toggleWebhookStatus('abc');
    expect(result).toBeNull();
    expect(showError).toHaveBeenCalledWith(
      'Failed to determine new status. Please refresh and try again.'
    );
    expect(showSuccess).not.toHaveBeenCalled();
    expect(showWarning).not.toHaveBeenCalled();
  });

  it('returns null and shows error on API failure', async () => {
    (
      JiraWebhookService.toggleWebhookActive as unknown as ReturnType<typeof vi.fn>
    ).mockRejectedValue(new Error('network error'));
    const { toggleWebhookStatus } = useWebhookActions();
    const result = await toggleWebhookStatus('abc', true);
    expect(result).toBeNull();
    expect(showError).toHaveBeenCalledWith('Failed to update webhook status');
  });
});

describe('deleteWebhook', () => {
  it('returns false and shows error when id is invalid', async () => {
    const { deleteWebhook } = useWebhookActions();
    const result = await deleteWebhook('');
    expect(result).toBe(false);
    expect(showError).toHaveBeenCalledWith('Invalid webhook id');
    expect(JiraWebhookService.deleteJiraWebhook).not.toHaveBeenCalled();
  });

  it('returns true and shows success on API success', async () => {
    (JiraWebhookService.deleteJiraWebhook as unknown as ReturnType<typeof vi.fn>).mockResolvedValue(
      undefined
    );
    const { deleteWebhook } = useWebhookActions();
    const result = await deleteWebhook('abc');
    expect(result).toBe(true);
    expect(JiraWebhookService.deleteJiraWebhook).toHaveBeenCalledWith('abc');
    expect(showSuccess).toHaveBeenCalledWith('Webhook deleted');
    expect(showError).not.toHaveBeenCalled();
  });

  it('returns false and shows specific error message on API failure', async () => {
    (JiraWebhookService.deleteJiraWebhook as unknown as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error('delete failed')
    );
    const { deleteWebhook } = useWebhookActions();
    const result = await deleteWebhook('abc');
    expect(result).toBe(false);
    expect(showError).toHaveBeenCalledWith('delete failed');
  });

  it('returns false and shows generic error message when thrown value is not Error', async () => {
    (JiraWebhookService.deleteJiraWebhook as unknown as ReturnType<typeof vi.fn>).mockRejectedValue(
      'bad'
    );
    const { deleteWebhook } = useWebhookActions();
    const result = await deleteWebhook('abc');
    expect(result).toBe(false);
    expect(showError).toHaveBeenCalledWith('Failed to delete webhook');
  });
});
