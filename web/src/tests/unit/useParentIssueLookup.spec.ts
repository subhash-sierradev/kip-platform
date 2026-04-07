import { beforeEach, describe, expect, it, vi } from 'vitest';

import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';
import { useParentIssueLookup } from '@/components/outbound/jirawebhooks/utils/useParentIssueLookup';

vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getProjectParentIssuesByConnectionId: vi.fn(),
  },
}));

describe('useParentIssueLookup', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches parent issues and stores results', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy.mockResolvedValueOnce([
      { key: 'SCRUM-1', summary: 'One' },
      { key: 'SCRUM-2', summary: 'Two' },
    ]);

    const lookup = useParentIssueLookup();
    await lookup.fetchParentIssuesForProject('c1', 'SCRUM', true);

    expect(parentIssuesSpy).toHaveBeenCalledTimes(1);
    expect(parentIssuesSpy).toHaveBeenCalledWith('c1', 'SCRUM', { maxResults: 100 });
    expect(lookup.parentIssues.value).toEqual([
      { key: 'SCRUM-1', summary: 'One' },
      { key: 'SCRUM-2', summary: 'Two' },
    ]);
  });

  it('skips duplicate initial fetch for same connection/project/subtask key', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy.mockResolvedValue([{ key: 'SCRUM-1', summary: 'One' }]);

    const lookup = useParentIssueLookup();
    await lookup.fetchParentIssuesForProject('c1', 'SCRUM', true);
    await lookup.fetchParentIssuesForProject('c1', 'SCRUM', true);

    expect(parentIssuesSpy).toHaveBeenCalledTimes(1);
  });

  it('resets issues when fetch preconditions are invalid', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy.mockResolvedValueOnce([{ key: 'SCRUM-1', summary: 'One' }]);

    const lookup = useParentIssueLookup();
    await lookup.fetchParentIssuesForProject('c1', 'SCRUM', true);
    expect(lookup.parentIssues.value.length).toBe(1);

    await lookup.fetchParentIssuesForProject(undefined, 'SCRUM', true);

    expect(parentIssuesSpy).toHaveBeenCalledTimes(1);
    expect(lookup.parentIssues.value).toEqual([]);
  });

  it('searches remotely when query length is 5+ and updates results', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy
      .mockResolvedValueOnce([{ key: 'SCRUM-1', summary: 'One' }])
      .mockResolvedValueOnce([{ key: 'SCRUM-75', summary: 'Historical issue' }]);

    const lookup = useParentIssueLookup();
    await lookup.fetchParentIssuesForProject('c1', 'SCRUM', true);

    await lookup.searchParentIssuesByQuery('scrum-75', 'c1', 'SCRUM', true);

    expect(parentIssuesSpy).toHaveBeenCalledTimes(2);
    expect(parentIssuesSpy).toHaveBeenLastCalledWith('c1', 'SCRUM', {
      maxResults: 100,
      query: 'scrum-75',
    });
    expect(lookup.parentIssues.value).toEqual([{ key: 'SCRUM-75', summary: 'Historical issue' }]);
  });

  it('does not call remote search for queries shorter than 5 and restores default results', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy
      .mockResolvedValueOnce([{ key: 'SCRUM-1', summary: 'One' }])
      .mockResolvedValueOnce([{ key: 'SCRUM-75', summary: 'Historical issue' }]);

    const lookup = useParentIssueLookup();
    await lookup.fetchParentIssuesForProject('c1', 'SCRUM', true);
    await lookup.searchParentIssuesByQuery('scrum-75', 'c1', 'SCRUM', true);

    await lookup.searchParentIssuesByQuery('scr', 'c1', 'SCRUM', true);

    expect(parentIssuesSpy).toHaveBeenCalledTimes(2);
    expect(lookup.parentIssues.value).toEqual([{ key: 'SCRUM-1', summary: 'One' }]);
  });

  it('dedupes same search query for same connection/project/subtask context', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy
      .mockResolvedValueOnce([{ key: 'SCRUM-1', summary: 'One' }])
      .mockResolvedValueOnce([{ key: 'SCRUM-75', summary: 'Historical issue' }]);

    const lookup = useParentIssueLookup();
    await lookup.fetchParentIssuesForProject('c1', 'SCRUM', true);

    await lookup.searchParentIssuesByQuery('scrum-75', 'c1', 'SCRUM', true);
    await lookup.searchParentIssuesByQuery('scrum-75', 'c1', 'SCRUM', true);

    expect(parentIssuesSpy).toHaveBeenCalledTimes(2);
  });

  it('resetParentIssues clears state', async () => {
    const parentIssuesSpy = vi.mocked(JiraIntegrationService.getProjectParentIssuesByConnectionId);
    parentIssuesSpy.mockResolvedValueOnce([{ key: 'SCRUM-1', summary: 'One' }]);

    const lookup = useParentIssueLookup();
    await lookup.fetchParentIssuesForProject('c1', 'SCRUM', true);

    lookup.resetParentIssues();

    expect(lookup.parentIssues.value).toEqual([]);
  });
});
