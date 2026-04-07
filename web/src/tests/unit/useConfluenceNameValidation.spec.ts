import { beforeEach, describe, expect, it, vi } from 'vitest';
import { computed, nextTick, ref } from 'vue';

import { ConfluenceIntegrationService } from '@/api/services/ConfluenceIntegrationService';
import { useConfluenceNameValidation } from '@/composables/useConfluenceNameValidation';

vi.mock('@/api/services/ConfluenceIntegrationService', () => ({
  ConfluenceIntegrationService: {
    getAllNormalizedNames: vi.fn(),
  },
}));

vi.mock('@/utils/globalNormalizedUtils', () => ({
  normalizeIntegrationNameForCompare: vi.fn((name: string) => name.trim().toLowerCase()),
}));

describe('useConfluenceNameValidation', () => {
  const integrationName = ref('');
  const originalName = ref('');

  beforeEach(() => {
    vi.clearAllMocks();
    integrationName.value = '';
    originalName.value = '';
  });

  it('loads normalized names from service', async () => {
    (ConfluenceIntegrationService.getAllNormalizedNames as any).mockResolvedValue([
      'alpha',
      'beta',
    ]);

    const model = useConfluenceNameValidation({
      integrationName,
      originalName: computed(() => originalName.value),
    });

    await model.loadNormalizedNames();

    expect(ConfluenceIntegrationService.getAllNormalizedNames).toHaveBeenCalledOnce();
    expect(model.allNormalizedNames.value).toEqual(['alpha', 'beta']);
  });

  it('logs and tolerates service errors while loading names', async () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    (ConfluenceIntegrationService.getAllNormalizedNames as any).mockRejectedValue(
      new Error('network')
    );

    const model = useConfluenceNameValidation({
      integrationName,
      originalName: computed(() => originalName.value),
    });

    await model.loadNormalizedNames();

    expect(errorSpy).toHaveBeenCalledWith(
      'Failed to load Confluence integration names:',
      expect.any(Error)
    );
    expect(model.allNormalizedNames.value).toEqual([]);
    errorSpy.mockRestore();
  });

  it('marks duplicate names in create mode', async () => {
    (ConfluenceIntegrationService.getAllNormalizedNames as any).mockResolvedValue([
      'Daily Export',
      'Nightly Push',
    ]);

    const model = useConfluenceNameValidation({
      integrationName,
      originalName: computed(() => originalName.value),
      editMode: false,
    });

    await model.loadNormalizedNames();
    integrationName.value = '  daily export  ';
    await nextTick();

    expect(model.isDuplicateName.value).toBe(true);
  });

  it('allows unchanged original name in edit mode', async () => {
    (ConfluenceIntegrationService.getAllNormalizedNames as any).mockResolvedValue([
      'Daily Export',
      'Nightly Push',
    ]);
    originalName.value = 'Daily Export';

    const model = useConfluenceNameValidation({
      integrationName,
      originalName: computed(() => originalName.value),
      editMode: true,
    });

    await model.loadNormalizedNames();
    integrationName.value = 'daily export';
    await nextTick();

    expect(model.isDuplicateName.value).toBe(false);
  });

  it('flags duplicates in edit mode when renaming to another existing name', async () => {
    (ConfluenceIntegrationService.getAllNormalizedNames as any).mockResolvedValue([
      'Daily Export',
      'Nightly Push',
    ]);
    originalName.value = 'Daily Export';

    const model = useConfluenceNameValidation({
      integrationName,
      originalName: computed(() => originalName.value),
      editMode: true,
    });

    await model.loadNormalizedNames();
    integrationName.value = 'nightly push';
    await nextTick();

    expect(model.isDuplicateName.value).toBe(true);
  });

  it('treats empty or whitespace names as non-duplicates', async () => {
    (ConfluenceIntegrationService.getAllNormalizedNames as any).mockResolvedValue(['one']);

    const model = useConfluenceNameValidation({
      integrationName,
      originalName: computed(() => originalName.value),
    });

    await model.loadNormalizedNames();
    integrationName.value = '   ';
    await nextTick();

    expect(model.isDuplicateName.value).toBe(false);
  });
});
