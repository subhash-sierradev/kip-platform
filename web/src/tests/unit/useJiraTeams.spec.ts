import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useJiraTeams } from '@/composables/useJiraTeams';

vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getTeamsByConnectionId: vi.fn(),
  },
}));
import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';
const getTeamsByConnectionId = vi.mocked(JiraIntegrationService.getTeamsByConnectionId);

describe('useJiraTeams', () => {
  beforeEach(() => {
    getTeamsByConnectionId.mockReset();
  });

  it('has initial empty state', () => {
    const { loading, error, items, options } = useJiraTeams({ connectionId: 'conn-1' });
    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
    expect(items.value).toEqual([]);
    expect(options.value).toEqual([]);
  });

  it('returns early when no connectionId', async () => {
    const { search, items, loading, error } = useJiraTeams({ connectionId: '' });
    await search('team');
    expect(getTeamsByConnectionId).not.toHaveBeenCalled();
    expect(items.value).toEqual([]);
    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
  });

  it('loads and maps teams successfully', async () => {
    getTeamsByConnectionId.mockResolvedValueOnce([
      { id: '1', name: 'Team Alpha' },
      { id: '2', name: 'Team Beta' },
    ]);

    const { search, items, options, error, loading } = useJiraTeams({ connectionId: 'conn-42' });
    await search('tea');

    expect(getTeamsByConnectionId).toHaveBeenCalledTimes(1);
    expect(getTeamsByConnectionId).toHaveBeenCalledWith('conn-42', {
      query: 'tea',
      startAt: 0,
      maxResults: 50,
    });

    expect(error.value).toBeNull();
    expect(loading.value).toBe(false);
    expect(items.value).toEqual([
      { id: '1', name: 'Team Alpha' },
      { id: '2', name: 'Team Beta' },
    ]);
    expect(options.value).toEqual([
      { value: '1', label: 'Team Alpha' },
      { value: '2', label: 'Team Beta' },
    ]);
  });

  it('handles errors and sets error message', async () => {
    getTeamsByConnectionId.mockRejectedValueOnce(new Error('boom'));

    const { search, items, options, error, loading } = useJiraTeams({ connectionId: 'conn-9' });
    await search();

    expect(getTeamsByConnectionId).toHaveBeenCalledTimes(1);
    expect(loading.value).toBe(false);
    expect(error.value).toBe('boom');
    expect(items.value).toEqual([]);
    expect(options.value).toEqual([]);
  });

  it('normalizes non-array responses to an empty list', async () => {
    getTeamsByConnectionId.mockResolvedValueOnce({ items: [] } as any);

    const { search, items, options, error } = useJiraTeams({ connectionId: 'conn-4' });
    await search('ops');

    expect(error.value).toBeNull();
    expect(items.value).toEqual([]);
    expect(options.value).toEqual([]);
  });

  it('uses a fallback error message for non-Error rejections', async () => {
    getTeamsByConnectionId.mockRejectedValueOnce({});

    const { search, error, loading, items } = useJiraTeams({ connectionId: 'conn-10' });
    await search('ops');

    expect(loading.value).toBe(false);
    expect(error.value).toBe('Failed to load teams');
    expect(items.value).toEqual([]);
  });
});
