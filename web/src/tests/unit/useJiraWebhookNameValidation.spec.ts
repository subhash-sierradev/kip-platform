import { describe, expect, it, vi } from 'vitest';
import { computed, nextTick, ref } from 'vue';

import { useJiraWebhookNameValidation } from '@/composables/useJiraWebhookNameValidation';

const serviceHoisted = vi.hoisted(() => ({
  getAllJiraNormalizedNames: vi.fn(),
}));

vi.mock('@/api/services/JiraWebhookService', () => ({
  JiraWebhookService: {
    getAllJiraNormalizedNames: serviceHoisted.getAllJiraNormalizedNames,
  },
}));

describe('useJiraWebhookNameValidation', () => {
  it('flags duplicate names after loading normalized names', async () => {
    serviceHoisted.getAllJiraNormalizedNames.mockResolvedValueOnce(['Example Webhook']);
    const integrationName = ref('');

    const { isDuplicateName, loadNormalizedNames } = useJiraWebhookNameValidation({
      integrationName,
      editMode: false,
      originalName: computed(() => undefined),
    });

    await loadNormalizedNames();
    integrationName.value = 'Example Webhook';
    await nextTick();

    expect(isDuplicateName.value).toBe(true);
  });

  it('allows original name in edit mode', async () => {
    serviceHoisted.getAllJiraNormalizedNames.mockResolvedValueOnce(['Example Webhook']);
    const integrationName = ref('');

    const { isDuplicateName, loadNormalizedNames } = useJiraWebhookNameValidation({
      integrationName,
      editMode: true,
      originalName: computed(() => 'Example Webhook'),
    });

    await loadNormalizedNames();
    integrationName.value = 'Example Webhook';
    await nextTick();

    expect(isDuplicateName.value).toBe(false);
  });

  it('does not flag empty names', async () => {
    const integrationName = ref('');
    const { isDuplicateName } = useJiraWebhookNameValidation({
      integrationName,
      editMode: false,
      originalName: computed(() => undefined),
    });

    integrationName.value = '';
    await nextTick();
    expect(isDuplicateName.value).toBe(false);
  });
});
