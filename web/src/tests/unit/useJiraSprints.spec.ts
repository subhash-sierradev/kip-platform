import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useJiraSprints } from '@/composables/useJiraSprints';

vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getSprintsByConnectionId: vi.fn(),
  },
}));
import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';
const getSprintsByConnectionId = vi.mocked(JiraIntegrationService.getSprintsByConnectionId);

describe('useJiraSprints', () => {
  beforeEach(() => {
    getSprintsByConnectionId.mockReset();
  });

  it('has initial empty state', () => {
    const { loading, error, items, options } = useJiraSprints({ connectionId: 'c-1' });
    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
    expect(items.value).toEqual([]);
    expect(options.value).toEqual([]);
  });

  it('returns early when no connectionId', async () => {
    const { search, items, loading, error } = useJiraSprints({ connectionId: '' });
    await search('sprint');
    expect(getSprintsByConnectionId).not.toHaveBeenCalled();
    expect(items.value).toEqual([]);
    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
  });

  it('uses default state and filters archived sprints', async () => {
    getSprintsByConnectionId.mockResolvedValueOnce([
      { id: 11, name: 'Sprint 1', state: 'active' },
      { id: 12, name: 'Old Sprint', state: 'closed', archived: true },
    ]);

    const { search, items, options, error, loading } = useJiraSprints({ connectionId: 'conn-77' });
    await search('');

    expect(getSprintsByConnectionId).toHaveBeenCalledTimes(1);
    expect(getSprintsByConnectionId).toHaveBeenCalledWith('conn-77', {
      boardId: undefined,
      projectKey: undefined,
      state: 'active,future',
      query: '',
      startAt: 0,
      maxResults: 50,
    });

    expect(error.value).toBeNull();
    expect(loading.value).toBe(false);
    // archived sprint is filtered out
    expect(items.value).toEqual([{ id: 11, name: 'Sprint 1', state: 'active' }]);
    expect(options.value).toEqual([{ value: '11', label: 'Sprint 1 (active)' }]);
  });

  it('honors provided state, boardId, and projectKey', async () => {
    getSprintsByConnectionId.mockResolvedValueOnce([{ id: 21, name: 'Sprint X', state: 'future' }]);

    const { search } = useJiraSprints({
      connectionId: 'conn-88',
      boardId: 5,
      projectKey: 'PRJ',
      state: 'closed',
    });
    await search('needle');

    expect(getSprintsByConnectionId).toHaveBeenCalledWith('conn-88', {
      boardId: 5,
      projectKey: 'PRJ',
      state: 'closed',
      query: 'needle',
      startAt: 0,
      maxResults: 50,
    });
  });

  it('handles errors and sets error message', async () => {
    getSprintsByConnectionId.mockRejectedValueOnce(new Error('oops'));

    const { search, items, options, error, loading } = useJiraSprints({ connectionId: 'conn-99' });
    await search();

    expect(getSprintsByConnectionId).toHaveBeenCalledTimes(1);
    expect(loading.value).toBe(false);
    expect(error.value).toBe('oops');
    expect(items.value).toEqual([]);
    expect(options.value).toEqual([]);
  });
});
