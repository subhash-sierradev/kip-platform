import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mock before imports so service uses the mocked api
vi.mock('@/api/OpenAPI', () => ({
  api: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() },
}));

import { api } from '@/api/OpenAPI';
import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';

beforeEach(() => {
  (api.get as any).mockReset?.();
  (api.post as any).mockReset?.();
});

describe('JiraIntegrationService', () => {
  it('testAndCreateConnection posts to test-connection', async () => {
    (api.post as any).mockResolvedValueOnce({ data: { id: '1' } });
    const res = await JiraIntegrationService.testAndCreateConnection({} as any);
    expect(res).toEqual({ id: '1' });
    expect(api.post).toHaveBeenCalledWith('/integrations/connections/test-connection', {});
  });

  it('listConnections GETs integrations/connections with serviceType param', async () => {
    (api.get as any).mockResolvedValueOnce({ data: [{ id: 'a' }] });
    const res = await JiraIntegrationService.listConnections();
    expect(res).toEqual([{ id: 'a' }]);
    expect(api.get).toHaveBeenCalledWith('/integrations/connections', {
      params: { serviceType: 'JIRA' },
    });
  });

  it('testExistingConnection posts to /{id}/test', async () => {
    (api.post as any).mockResolvedValueOnce({ data: { success: true } });
    const res = await JiraIntegrationService.testExistingConnection('id1');
    expect(res.success).toBe(true);
    expect(api.post).toHaveBeenCalledWith(
      '/integrations/connections/id1/test',
      {},
      {
        loadingMessage: 'Verifying Jira connection...',
      }
    );
  });

  it('fetches projects, issue types, users, and fields by connection id', async () => {
    (api.post as any).mockResolvedValue({ data: [] });
    await JiraIntegrationService.getProjectsByConnectionId('c1');
    expect(api.post).toHaveBeenCalledWith(
      '/integrations/jira/connections/c1/projects',
      {},
      {
        loadingMessage: 'Fetching Jira projects...',
      }
    );

    await JiraIntegrationService.getProjectIssueTypesByConnectionId('c1', 'P');
    expect(api.post).toHaveBeenCalledWith(
      '/integrations/jira/connections/c1/projects/P/issue-types'
    );

    await JiraIntegrationService.getProjectUsersByConnectionId('c1', 'P');
    expect(api.post).toHaveBeenCalledWith('/integrations/jira/connections/c1/projects/P/users');

    await JiraIntegrationService.getFieldsByConnectionId('c1');
    expect(api.post).toHaveBeenCalledWith('/integrations/jira/connections/c1/fields');
  });

  it('fetches teams with optional parameters', async () => {
    (api.post as any).mockResolvedValue({ data: [{ id: '1', name: 'Team A' }] });

    // Test without options
    await JiraIntegrationService.getTeamsByConnectionId('c1');
    expect(api.post).toHaveBeenCalledWith('/integrations/jira/connections/c1/teams', null, {
      params: {},
    });

    // Test with options
    const opts = { query: 'search', startAt: 0, maxResults: 50 };
    await JiraIntegrationService.getTeamsByConnectionId('c1', opts);
    expect(api.post).toHaveBeenCalledWith('/integrations/jira/connections/c1/teams', null, {
      params: opts,
    });
  });

  it('fetches parent issues with default and explicit search options', async () => {
    (api.post as any).mockResolvedValue({ data: [{ key: 'PRJ-1' }] });

    await JiraIntegrationService.getProjectParentIssuesByConnectionId('c1', 'PRJ');
    expect(api.post).toHaveBeenCalledWith(
      '/integrations/jira/connections/c1/projects/PRJ/parent-issues',
      null,
      { params: {} }
    );

    const opts = { startAt: 10, maxResults: 25, query: 'Parent' };
    await JiraIntegrationService.getProjectParentIssuesByConnectionId('c1', 'PRJ', opts);
    expect(api.post).toHaveBeenCalledWith(
      '/integrations/jira/connections/c1/projects/PRJ/parent-issues',
      null,
      { params: opts }
    );
  });

  it('fetches meta fields with and without an issue type id', async () => {
    (api.post as any).mockResolvedValue({ data: [{ id: 'summary', name: 'Summary' }] });

    await JiraIntegrationService.getProjectMetaFieldsByConnectionId('c1', 'PRJ');
    expect(api.post).toHaveBeenCalledWith(
      '/integrations/jira/connections/c1/projects/PRJ/meta-fields',
      null,
      { params: { issueTypeId: undefined } }
    );

    await JiraIntegrationService.getProjectMetaFieldsByConnectionId('c1', 'PRJ', '10001');
    expect(api.post).toHaveBeenCalledWith(
      '/integrations/jira/connections/c1/projects/PRJ/meta-fields',
      null,
      { params: { issueTypeId: '10001' } }
    );
  });

  it('fetches sprints with optional parameters', async () => {
    (api.post as any).mockResolvedValue({ data: [{ id: 1, name: 'Sprint 1', state: 'active' }] });

    // Test without options
    await JiraIntegrationService.getSprintsByConnectionId('c1');
    expect(api.post).toHaveBeenCalledWith('/integrations/jira/connections/c1/sprints', null, {
      params: {},
    });

    // Test with options
    const opts = {
      boardId: 1,
      state: 'active',
      query: 'search',
      startAt: 0,
      maxResults: 50,
      projectKey: 'TEST',
    };
    await JiraIntegrationService.getSprintsByConnectionId('c1', opts);
    expect(api.post).toHaveBeenCalledWith('/integrations/jira/connections/c1/sprints', null, {
      params: opts,
    });
  });
});
