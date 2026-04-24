import { type ComputedRef, type Ref, ref, watch } from 'vue';

import { JiraWebhookService } from '@/api/services/JiraWebhookService';
import { normalizeIntegrationNameForCompare } from '@/utils/globalNormalizedUtils';

interface UseJiraWebhookNameValidationOptions {
  integrationName: Ref<string>;
  editMode?: boolean;
  originalName: ComputedRef<string | undefined>;
}

export function useJiraWebhookNameValidation(options: UseJiraWebhookNameValidationOptions) {
  const allWebhookNormalizedNames = ref<string[]>([]);
  const isDuplicateName = ref(false);

  const computeDuplicate = (name: string): boolean => {
    if (!name.trim()) return false;
    const normalizedCurrentName = normalizeIntegrationNameForCompare(name.trim());
    const normalizedOriginalName = normalizeIntegrationNameForCompare(
      options.originalName.value || ''
    );

    return allWebhookNormalizedNames.value.some(dbName => {
      const normalizedDbName = normalizeIntegrationNameForCompare(dbName);
      if (options.editMode && normalizedDbName === normalizedOriginalName) {
        return false;
      }
      return normalizedDbName === normalizedCurrentName;
    });
  };

  const loadNormalizedNames = async () => {
    try {
      allWebhookNormalizedNames.value = await JiraWebhookService.getAllJiraNormalizedNames();
    } catch (error) {
      console.error('Failed to load webhook names:', error);
    }
  };

  watch(
    [
      () => options.integrationName.value,
      allWebhookNormalizedNames,
      () => options.originalName.value,
      () => options.editMode,
    ],
    ([newName]) => {
      isDuplicateName.value = computeDuplicate(newName || '');
    },
    { immediate: true }
  );

  return {
    allWebhookNormalizedNames,
    isDuplicateName,
    loadNormalizedNames,
  };
}
