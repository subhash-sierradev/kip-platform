import { ref, watch } from 'vue';

import { normalizeIntegrationNameForCompare } from '@/utils/globalNormalizedUtils';

export interface IntegrationNameValidationOptions {
  mode?: 'create' | 'edit' | 'clone';
  integrationNameGetter: () => string;
  getAllNamesFunction: () => Promise<string[]>;
}

export function useIntegrationNameValidation(options: IntegrationNameValidationOptions) {
  const { mode, integrationNameGetter, getAllNamesFunction } = options;

  const allNormalizedNames = ref<string[]>([]);
  const isDuplicateName = ref(false);
  const originalName = ref<string>('');

  /**
   * Load all existing integration names from the backend
   */
  async function loadAllNames(): Promise<void> {
    try {
      allNormalizedNames.value = await getAllNamesFunction();
    } catch (error) {
      console.error('Failed to load integration names:', error);
      throw error;
    }
  }

  /**
   * Check if the current name is a duplicate against all normalized names
   */
  function checkDuplicateInList(currentName: string): boolean {
    if (!currentName || !currentName.trim()) {
      return false;
    }

    const normalizedCurrent = normalizeIntegrationNameForCompare(currentName);
    return allNormalizedNames.value.some(
      name => normalizeIntegrationNameForCompare(name) === normalizedCurrent
    );
  }

  /**
   * Validate the current integration name against the database
   * Used before final submission to ensure no race conditions
   */
  async function validateBeforeSubmit(currentName: string): Promise<boolean> {
    const currentDbNames = await getAllNamesFunction();
    const normalizedCurrentName = normalizeIntegrationNameForCompare(currentName);
    const normalizedOriginalName = normalizeIntegrationNameForCompare(originalName.value || '');

    const hasDuplicate = currentDbNames.some((dbName: string) => {
      const normalizedDbName = normalizeIntegrationNameForCompare(dbName);
      // In edit mode, allow the original name
      if (mode === 'edit' && normalizedDbName === normalizedOriginalName) {
        return false;
      }
      return normalizedDbName === normalizedCurrentName;
    });

    return hasDuplicate;
  }

  /**
   * Setup watcher for name changes to check for duplicates in real-time
   */
  function setupNameWatcher(): void {
    watch(
      integrationNameGetter,
      newName => {
        isDuplicateName.value = checkDuplicateInList(newName);
      },
      { deep: true }
    );
  }

  return {
    allNormalizedNames,
    isDuplicateName,
    originalName,
    loadAllNames,
    checkDuplicateInList,
    validateBeforeSubmit,
    setupNameWatcher,
  };
}
