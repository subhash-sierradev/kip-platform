import { ref } from 'vue';

import type { CreateRecipientPolicyRequest, RecipientPolicyResponse } from '@/api';
import { NotificationAdminService } from '@/api/services/NotificationAdminService';
import Alert from '@/utils/notificationUtils';

export function useNotificationPoliciesManager() {
  const policies = ref<RecipientPolicyResponse[]>([]);
  const loadingPolicies = ref(false);
  const saving = ref(false);

  async function fetchPolicies(): Promise<void> {
    loadingPolicies.value = true;
    try {
      policies.value = await NotificationAdminService.getPolicies();
    } catch {
      Alert.error('Failed to load recipient policies');
    } finally {
      loadingPolicies.value = false;
    }
  }

  async function createPolicy(payload: CreateRecipientPolicyRequest): Promise<boolean> {
    saving.value = true;
    try {
      const created = await NotificationAdminService.createPolicy({ requestBody: payload });
      policies.value = [created, ...policies.value];
      Alert.success('Recipient policy created');
      return true;
    } catch {
      Alert.error('Failed to create recipient policy');
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function deletePolicy(id: string): Promise<void> {
    try {
      await NotificationAdminService.deletePolicy({ id });
      policies.value = policies.value.filter(p => p.id !== id);
      Alert.success('Recipient policy deleted');
    } catch {
      Alert.error('Failed to delete recipient policy');
    }
  }

  async function upsertPolicy(
    payload: CreateRecipientPolicyRequest,
    existingId?: string
  ): Promise<boolean> {
    saving.value = true;
    try {
      if (existingId) {
        const updated = await NotificationAdminService.updatePolicy({
          id: existingId,
          requestBody: payload,
        });
        const idx = policies.value.findIndex(p => p.id === existingId);
        if (idx !== -1) {
          policies.value = [
            ...policies.value.slice(0, idx),
            updated,
            ...policies.value.slice(idx + 1),
          ];
        }
      } else {
        await NotificationAdminService.createPolicy({ requestBody: payload });
      }
      await fetchPolicies();
      Alert.success(existingId ? 'Recipient policy updated' : 'Recipient policy created');
      return true;
    } catch {
      Alert.error(
        existingId ? 'Failed to update recipient policy' : 'Failed to create recipient policy'
      );
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    policies,
    loadingPolicies,
    saving,
    fetchPolicies,
    createPolicy,
    deletePolicy,
    upsertPolicy,
  };
}
