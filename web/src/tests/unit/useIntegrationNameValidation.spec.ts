import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

import { useIntegrationNameValidation } from '@/composables/useIntegrationNameValidation';

vi.mock('@/utils/globalNormalizedUtils', () => ({
  normalizeIntegrationNameForCompare: vi.fn((name: string) => name.toLowerCase().trim()),
}));

describe('useIntegrationNameValidation', () => {
  const mockGetAllNames = vi.fn();
  let integrationNameRef = ref('');

  beforeEach(() => {
    vi.clearAllMocks();
    integrationNameRef = ref('');
  });

  describe('loadAllNames', () => {
    it('loads and stores all normalized names successfully', async () => {
      mockGetAllNames.mockResolvedValue([
        'Integration One',
        'Integration Two',
        'Integration Three',
      ]);

      const { loadAllNames, allNormalizedNames } = useIntegrationNameValidation({
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      await loadAllNames();

      expect(mockGetAllNames).toHaveBeenCalledOnce();
      expect(allNormalizedNames.value).toEqual([
        'Integration One',
        'Integration Two',
        'Integration Three',
      ]);
    });

    it('handles error when loading names fails', async () => {
      const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
      mockGetAllNames.mockRejectedValue(new Error('Network error'));

      const { loadAllNames } = useIntegrationNameValidation({
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      await expect(loadAllNames()).rejects.toThrow('Network error');
      expect(consoleError).toHaveBeenCalledWith(
        'Failed to load integration names:',
        expect.any(Error)
      );

      consoleError.mockRestore();
    });
  });

  describe('checkDuplicateInList', () => {
    beforeEach(async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration', 'Another One', 'Third Name']);
    });

    it('returns false for empty or whitespace-only names', async () => {
      const { loadAllNames, checkDuplicateInList } = useIntegrationNameValidation({
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      await loadAllNames();

      expect(checkDuplicateInList('')).toBe(false);
      expect(checkDuplicateInList('   ')).toBe(false);
    });

    it('returns true for exact duplicate (case-insensitive)', async () => {
      const { loadAllNames, checkDuplicateInList } = useIntegrationNameValidation({
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      await loadAllNames();

      expect(checkDuplicateInList('Existing Integration')).toBe(true);
      expect(checkDuplicateInList('existing integration')).toBe(true);
      expect(checkDuplicateInList('EXISTING INTEGRATION')).toBe(true);
    });

    it('returns false for unique names', async () => {
      const { loadAllNames, checkDuplicateInList } = useIntegrationNameValidation({
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      await loadAllNames();

      expect(checkDuplicateInList('Unique Name')).toBe(false);
      expect(checkDuplicateInList('Brand New Integration')).toBe(false);
    });

    it('handles names with extra whitespace', async () => {
      const { loadAllNames, checkDuplicateInList } = useIntegrationNameValidation({
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      await loadAllNames();

      expect(checkDuplicateInList('  Existing Integration  ')).toBe(true);
    });
  });

  describe('validateBeforeSubmit', () => {
    it('detects duplicate in create mode', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration', 'Another One']);

      const { validateBeforeSubmit } = useIntegrationNameValidation({
        mode: 'create',
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      const isDuplicate = await validateBeforeSubmit('existing integration');

      expect(isDuplicate).toBe(true);
      expect(mockGetAllNames).toHaveBeenCalledOnce();
    });

    it('allows unique name in create mode', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration', 'Another One']);

      const { validateBeforeSubmit } = useIntegrationNameValidation({
        mode: 'create',
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      const isDuplicate = await validateBeforeSubmit('Unique Name');

      expect(isDuplicate).toBe(false);
    });

    it('allows original name in edit mode', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration', 'Another One']);

      const { validateBeforeSubmit, originalName } = useIntegrationNameValidation({
        mode: 'edit',
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      originalName.value = 'Existing Integration';
      const isDuplicate = await validateBeforeSubmit('Existing Integration');

      expect(isDuplicate).toBe(false);
    });

    it('detects duplicate when editing to different existing name', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration', 'Another One']);

      const { validateBeforeSubmit, originalName } = useIntegrationNameValidation({
        mode: 'edit',
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      originalName.value = 'Existing Integration';
      const isDuplicate = await validateBeforeSubmit('Another One');

      expect(isDuplicate).toBe(true);
    });

    it('allows unique name in edit mode', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration', 'Another One']);

      const { validateBeforeSubmit, originalName } = useIntegrationNameValidation({
        mode: 'edit',
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      originalName.value = 'Existing Integration';
      const isDuplicate = await validateBeforeSubmit('Unique New Name');

      expect(isDuplicate).toBe(false);
    });

    it('handles case-insensitive matching in edit mode', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration', 'Another One']);

      const { validateBeforeSubmit, originalName } = useIntegrationNameValidation({
        mode: 'edit',
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      originalName.value = 'existing integration';
      const isDuplicate = await validateBeforeSubmit('EXISTING INTEGRATION');

      expect(isDuplicate).toBe(false);
    });

    it('detects duplicate in clone mode', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration', 'Another One']);

      const { validateBeforeSubmit } = useIntegrationNameValidation({
        mode: 'clone',
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      const isDuplicate = await validateBeforeSubmit('existing integration');

      expect(isDuplicate).toBe(true);
    });

    it('handles empty originalName in edit mode', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration']);

      const { validateBeforeSubmit, originalName } = useIntegrationNameValidation({
        mode: 'edit',
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      originalName.value = '';
      const isDuplicate = await validateBeforeSubmit('Existing Integration');

      expect(isDuplicate).toBe(true);
    });
  });

  describe('setupNameWatcher', () => {
    it('updates isDuplicateName when integration name changes', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration', 'Another One']);

      const { loadAllNames, setupNameWatcher, isDuplicateName } = useIntegrationNameValidation({
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      await loadAllNames();
      setupNameWatcher();

      expect(isDuplicateName.value).toBe(false);

      integrationNameRef.value = 'Existing Integration';
      await vi.waitFor(() => expect(isDuplicateName.value).toBe(true));

      integrationNameRef.value = 'Unique Name';
      await vi.waitFor(() => expect(isDuplicateName.value).toBe(false));
    });

    it('handles name changes with whitespace', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration']);

      const { loadAllNames, setupNameWatcher, isDuplicateName } = useIntegrationNameValidation({
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      await loadAllNames();
      setupNameWatcher();

      integrationNameRef.value = '  Existing Integration  ';
      await vi.waitFor(() => expect(isDuplicateName.value).toBe(true));
    });

    it('handles empty name changes', async () => {
      mockGetAllNames.mockResolvedValue(['Existing Integration']);

      const { loadAllNames, setupNameWatcher, isDuplicateName } = useIntegrationNameValidation({
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      await loadAllNames();
      setupNameWatcher();

      integrationNameRef.value = '';
      await vi.waitFor(() => expect(isDuplicateName.value).toBe(false));
    });
  });

  describe('integration tests', () => {
    it('full workflow: load names, watch for changes, validate before submit', async () => {
      mockGetAllNames.mockResolvedValue(['Integration A', 'Integration B']);

      const {
        loadAllNames,
        setupNameWatcher,
        isDuplicateName,
        validateBeforeSubmit,
        originalName,
      } = useIntegrationNameValidation({
        mode: 'edit',
        integrationNameGetter: () => integrationNameRef.value,
        getAllNamesFunction: mockGetAllNames,
      });

      // 1. Load all existing names
      await loadAllNames();

      // 2. Set original name (edit mode)
      originalName.value = 'Integration A';

      // 3. Setup watcher
      setupNameWatcher();

      // 4. Change name to duplicate (should detect in watcher)
      integrationNameRef.value = 'Integration B';
      await vi.waitFor(() => expect(isDuplicateName.value).toBe(true));

      // 5. Validate before submit should detect duplicate
      const isDuplicateB = await validateBeforeSubmit('Integration B');
      expect(isDuplicateB).toBe(true);

      // 6. Change name to original (watcher still marks as duplicate in list, but validateBeforeSubmit allows it)
      integrationNameRef.value = 'Integration A';
      // Note: checkDuplicateInList doesn't know about originalName, so watcher marks it as duplicate
      await vi.waitFor(() => expect(isDuplicateName.value).toBe(true));

      // 7. Validate before submit should allow original (edit mode logic)
      const isDuplicateA = await validateBeforeSubmit('Integration A');
      expect(isDuplicateA).toBe(false);

      // 8. Change name to unique (should allow)
      integrationNameRef.value = 'Unique Integration';
      await vi.waitFor(() => expect(isDuplicateName.value).toBe(false));

      // 9. Validate before submit should allow unique
      const isDuplicateUnique = await validateBeforeSubmit('Unique Integration');
      expect(isDuplicateUnique).toBe(false);
    });
  });
});
