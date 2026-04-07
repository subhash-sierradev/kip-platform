import { ref } from 'vue';

import type { CreateNotificationRuleRequest, NotificationRuleResponse } from '@/api';
import { NotificationAdminService } from '@/api/services/NotificationAdminService';
import Alert from '@/utils/notificationUtils';

export function useNotificationRulesManager() {
  const rules = ref<NotificationRuleResponse[]>([]);
  const loadingRules = ref(false);
  const saving = ref(false);

  async function fetchRules(): Promise<void> {
    loadingRules.value = true;
    try {
      rules.value = await NotificationAdminService.getRules();
    } catch {
      Alert.error('Failed to load notification rules');
    } finally {
      loadingRules.value = false;
    }
  }

  async function createRule(payload: CreateNotificationRuleRequest): Promise<boolean> {
    saving.value = true;
    try {
      await NotificationAdminService.createRule({ requestBody: payload });
      await fetchRules();
      Alert.success('Notification rule created');
      return true;
    } catch {
      Alert.error('Failed to create notification rule');
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function deleteRule(id: string): Promise<void> {
    try {
      await NotificationAdminService.deleteRule({ id });
      rules.value = rules.value.filter(r => r.id !== id);
      Alert.success('Rule deleted');
    } catch {
      Alert.error('Failed to delete rule');
    }
  }

  async function updateRule(id: string, severity: string): Promise<boolean> {
    saving.value = true;
    try {
      await NotificationAdminService.updateRule({ id, requestBody: { severity } });
      await fetchRules();
      Alert.success('Notification rule updated');
      return true;
    } catch {
      Alert.error('Failed to update notification rule');
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function toggleRule(id: string): Promise<boolean> {
    try {
      const updated = await NotificationAdminService.toggleRule({ id });
      await fetchRules();
      Alert.success(updated.isEnabled ? 'Rule enabled' : 'Rule disabled');
      return true;
    } catch {
      Alert.error('Failed to toggle notification rule');
      return false;
    }
  }

  return {
    rules,
    loadingRules,
    saving,
    fetchRules,
    createRule,
    updateRule,
    deleteRule,
    toggleRule,
  };
}
