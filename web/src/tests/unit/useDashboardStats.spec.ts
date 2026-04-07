/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { api } from '@/api/OpenAPI';
import { useDashboardStats } from '@/composables/useDashboardStats';

vi.mock('@/api/OpenAPI', () => {
  const get = vi.fn();
  return {
    api: { get },
  };
});

describe('useDashboardStats', () => {
  beforeEach(() => {
    (api.get as unknown as ReturnType<typeof vi.fn>).mockReset?.();
  });

  it('initializes with defaults', () => {
    const { stats, loading, error } = useDashboardStats();
    expect(stats.value).toEqual([]);
    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();
  });

  it('loads stats using totalElements when present', async () => {
    (api.get as any).mockResolvedValue({ data: { totalElements: 7 } });

    const { stats, loading, error, load } = useDashboardStats();
    const p = load();
    expect(loading.value).toBe(true);
    await p;

    expect(loading.value).toBe(false);
    expect(error.value).toBeNull();

    expect(stats.value).toEqual([
      { title: 'Jira Connections', value: 7 },
      { title: 'Active Webhooks', value: 0 },
      { title: 'Pending Tasks', value: 0 },
    ]);

    expect((api.get as any).mock.calls[0][0]).toBe('/integrations/jira/connections/global');
    expect((api.get as any).mock.calls[0][1]).toEqual({ params: { page: 0, size: 1 } });
  });

  it('falls back to content length when totalElements is missing', async () => {
    (api.get as any).mockResolvedValue({ data: { content: [1, 2, 3] } });

    const { stats, load } = useDashboardStats();
    await load();

    expect(stats.value[0]).toEqual({ title: 'Jira Connections', value: 3 });
  });

  it('sets error message and stops loading on failure', async () => {
    (api.get as any).mockRejectedValue(new Error('boom'));

    const { stats, error, loading, load } = useDashboardStats();
    await load();

    expect(loading.value).toBe(false);
    expect(error.value).toBe('boom');
    expect(stats.value).toEqual([]);
  });

  it('handles non-Error exceptions', async () => {
    (api.get as any).mockRejectedValue('String error');

    const { error, load } = useDashboardStats();
    await load();

    expect(error.value).toBe('Failed to load dashboard');
  });

  it('handles null exception', async () => {
    (api.get as any).mockRejectedValue(null);

    const { error, load } = useDashboardStats();
    await load();

    expect(error.value).toBe('Failed to load dashboard');
  });

  it('handles response with missing totalElements and content', async () => {
    (api.get as any).mockResolvedValue({ data: {} });

    const { stats, load } = useDashboardStats();
    await load();

    expect(stats.value[0]).toEqual({ title: 'Jira Connections', value: 0 });
  });

  it('handles response with null content', async () => {
    (api.get as any).mockResolvedValue({ data: { content: null } });

    const { stats, load } = useDashboardStats();
    await load();

    expect(stats.value[0]).toEqual({ title: 'Jira Connections', value: 0 });
  });

  it('clears previous error on successful reload', async () => {
    (api.get as any).mockRejectedValueOnce(new Error('First error'));

    const { error, load } = useDashboardStats();
    await load();
    expect(error.value).toBe('First error');

    // Second load succeeds
    (api.get as any).mockResolvedValue({ data: { totalElements: 5 } });
    await load();

    expect(error.value).toBeNull();
  });
});
