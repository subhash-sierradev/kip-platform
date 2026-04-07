import { ref } from 'vue';

import { NotificationAdminService } from '@/api/services/NotificationAdminService';
import { useToastStore } from '@/store/toast';

/**
 * Composable for managing notification rules reset functionality.
 * Handles resetting notification rules and recipients to default values for the tenant's assigned features.
 */
export function useNotificationReset() {
  const toast = useToastStore();
  const isResetting = ref(false);
  const showConfirmDialog = ref(false);

  /**
   * Resets notification rules to defaults for the current tenant.
   * Shows a confirmation dialog before proceeding.
   * Restores rules and recipient policies based on the tenant's assigned features,
   * while preserving custom notification templates.
   */
  async function resetToDefaults(): Promise<void> {
    showConfirmDialog.value = true;
  }

  /**
   * Confirms the reset operation and calls the backend API.
   */
  async function confirmReset(): Promise<void> {
    showConfirmDialog.value = false;
    isResetting.value = true;

    try {
      const response = await NotificationAdminService.resetRulesToDefaults();

      toast.showSuccess(
        `Notification rules reset to defaults successfully. ${response.rulesRestored} rules restored.`
      );
      return Promise.resolve();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Failed to reset notification rules';
      toast.showError(`Error: ${message}`);
      throw error;
    } finally {
      isResetting.value = false;
    }
  }

  /**
   * Cancels the reset operation.
   */
  function cancelReset(): void {
    showConfirmDialog.value = false;
  }

  return {
    isResetting,
    showConfirmDialog,
    resetToDefaults,
    confirmReset,
    cancelReset,
  };
}
