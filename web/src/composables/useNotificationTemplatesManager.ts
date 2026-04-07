import { ref } from 'vue';

import type { NotificationTemplateResponse } from '@/api';
import { NotificationAdminService } from '@/api/services/NotificationAdminService';
import Alert from '@/utils/notificationUtils';

export function useNotificationTemplatesManager() {
  const templates = ref<NotificationTemplateResponse[]>([]);
  const loadingTemplates = ref(false);
  const saving = ref(false);

  async function fetchTemplates(): Promise<void> {
    loadingTemplates.value = true;
    try {
      templates.value = await NotificationAdminService.getTemplates();
    } catch {
      Alert.error('Failed to load notification templates');
    } finally {
      loadingTemplates.value = false;
    }
  }

  return { templates, loadingTemplates, saving, fetchTemplates };
}
