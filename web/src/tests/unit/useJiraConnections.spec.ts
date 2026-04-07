import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/api/OpenAPI', () => {
  return {
    api: {
      get: vi.fn(),
      post: vi.fn(),
    },
  };
});

import { api } from '@/api/OpenAPI';
import { useJiraConnections } from '@/composables/useJiraConnections';

describe('useJiraConnections', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('fetch() loads connections with default params and maps content', async () => {
    (api.get as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: { content: [{ id: '1', name: 'Conn A' }] },
    });

    const { data, loading, error, fetch } = useJiraConnections();

    const p = fetch();
    expect(loading.value).toBe(true);
    await p;

    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
    expect(data.value).toEqual([{ id: '1', name: 'Conn A' }]);

    expect(api.get).toHaveBeenCalledWith('/integrations/jira/connections/global', {
      params: { page: 0, size: 10, status: 'ALL' },
    });
  });

  it('fetch() passes provided page/size/status', async () => {
    (api.get as any).mockResolvedValue({ data: { content: [] } });

    const { fetch } = useJiraConnections();
    await fetch({ page: 2, size: 50, status: 'ACTIVE' });

    expect(api.get).toHaveBeenCalledWith('/integrations/jira/connections/global', {
      params: { page: 2, size: 50, status: 'ACTIVE' },
    });
  });

  it('fetch() sets error on failure and clears loading', async () => {
    (api.get as any).mockRejectedValue(new Error('API Error'));

    const { data, loading, error, fetch } = useJiraConnections();
    await fetch();

    expect(loading.value).toBe(false);
    expect(error.value).toBe('API Error');
    expect(data.value).toBeNull();
  });

  it('fetch() handles non-Error exceptions with fallback message', async () => {
    (api.get as any).mockRejectedValue('String error');

    const { error, fetch } = useJiraConnections();
    await fetch();

    expect(error.value).toBe('Request failed');
  });

  it('fetch() handles null content from API', async () => {
    (api.get as any).mockResolvedValue({ data: {} });

    const { data, fetch } = useJiraConnections();
    await fetch();

    expect(data.value).toEqual([]);
  });

  it('fetch() handles missing data object from API', async () => {
    (api.get as any).mockResolvedValue({});

    const { error, fetch } = useJiraConnections();

    // This would likely cause an error accessing .data.content
    await fetch();

    // Since resp.data is undefined, resp.data.content will throw
    // The error handler catches it and sets error
    expect(error.value).not.toBeNull();
  });

  it('fetch() clears previous error on successful request', async () => {
    (api.get as any).mockRejectedValueOnce(new Error('First error'));

    const { error, fetch } = useJiraConnections();
    await fetch();
    expect(error.value).toBe('First error');

    (api.get as any).mockResolvedValue({ data: { content: [{ id: '1' }] } });
    await fetch();

    expect(error.value).toBeNull();
  });

  it('fetch() maintains loading state correctly during concurrent calls', async () => {
    let resolveFirst: (() => void) | undefined;
    const firstPromise = new Promise(resolve => {
      resolveFirst = () => resolve({ data: { content: [] } });
    });
    (api.get as any).mockReturnValueOnce(firstPromise);

    const { loading, fetch } = useJiraConnections();
    const p1 = fetch();

    expect(loading.value).toBe(true);

    // Second fetch overrides
    (api.get as any).mockResolvedValueOnce({ data: { content: [] } });
    const p2 = fetch();

    resolveFirst!();
    await p1;
    await p2;

    expect(loading.value).toBe(false);
  });

  it('create() posts payload and returns API response', async () => {
    const payload = { jiraBaseUrl: 'https://example.atlassian.net', credentialTypeCode: 'BASIC' };
    (api.post as any).mockResolvedValue({ data: { id: 'c-1' } });

    const { create } = useJiraConnections();
    const resp = await create(payload);

    expect(api.post).toHaveBeenCalledWith('/integrations/jira/connections', payload);
    expect(resp).toEqual({ data: { id: 'c-1' } });
  });

  it('create() handles API errors', async () => {
    const payload = { jiraBaseUrl: 'https://example.atlassian.net', credentialTypeCode: 'BASIC' };
    (api.post as any).mockRejectedValue(new Error('Create failed'));

    const { create } = useJiraConnections();

    await expect(create(payload)).rejects.toThrow('Create failed');
  });

  it('data, loading, and error have correct initial states', () => {
    const { data, loading, error } = useJiraConnections();

    expect(data.value).toBeNull();
    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
  });

  it('fetch() handles all IntegrationConnectionResponse fields', async () => {
    const connection = {
      id: '123',
      name: 'Test Connection',
      serviceType: 'JIRA',
      baseUrl: 'https://test.atlassian.net',
      jiraBaseUrl: 'https://legacy.atlassian.net',
      lastConnectionStatus: 'ACTIVE',
      authType: 'OAUTH2',
    };

    (api.get as any).mockResolvedValue({ data: { content: [connection] } });

    const { data, fetch } = useJiraConnections();
    await fetch();

    expect(data.value).toEqual([connection]);
    expect(data.value![0].jiraBaseUrl).toBe('https://legacy.atlassian.net');
  });
});
