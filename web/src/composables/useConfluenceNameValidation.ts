import { type ComputedRef, type Ref, ref, watch } from 'vue';

import { ConfluenceIntegrationService } from '@/api/services/ConfluenceIntegrationService';
import { normalizeIntegrationNameForCompare } from '@/utils/globalNormalizedUtils';

interface UseConfluenceNameValidationOptions {
  integrationName: Ref<string>;
  editMode?: boolean;
  originalName: ComputedRef<string | undefined>;
}

export function useConfluenceNameValidation(options: UseConfluenceNameValidationOptions) {
  const allNormalizedNames = ref<string[]>([]);
  const isDuplicateName = ref(false);

  const computeDuplicate = (name: string): boolean => {
    if (!name.trim()) return false;
    const normalizedCurrentName = normalizeIntegrationNameForCompare(name.trim());
    const normalizedOriginalName = normalizeIntegrationNameForCompare(
      options.originalName.value || ''
    );

    return allNormalizedNames.value.some(dbName => {
      const normalizedDbName = normalizeIntegrationNameForCompare(dbName);
      if (options.editMode && normalizedDbName === normalizedOriginalName) {
        return false;
      }
      return normalizedDbName === normalizedCurrentName;
    });
  };

  const loadNormalizedNames = async () => {
    try {
      allNormalizedNames.value = await ConfluenceIntegrationService.getAllNormalizedNames();
    } catch (error) {
      console.error('Failed to load Confluence integration names:', error);
    }
  };

  watch(
    () => options.integrationName.value,
    newName => {
      isDuplicateName.value = computeDuplicate(newName || '');
    }
  );

  return {
    allNormalizedNames,
    isDuplicateName,
    loadNormalizedNames,
  };
}
