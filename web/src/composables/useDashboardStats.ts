import { ref } from 'vue';

import { api } from '@/api/OpenAPI';

interface StatCard {
  title: string;
  value: number;
}

export function useDashboardStats() {
  const stats = ref<StatCard[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function load() {
    loading.value = true;
    error.value = null;
    try {
      // Placeholder values until real endpoint is added
      const connectionsResp = await api.get('/integrations/jira/connections/global', {
        params: { page: 0, size: 1 },
      });
      const count =
        connectionsResp.data.totalElements ?? (connectionsResp.data.content?.length || 0);
      stats.value = [
        { title: 'Jira Connections', value: count },
        { title: 'Active Webhooks', value: 0 },
        { title: 'Pending Tasks', value: 0 },
      ];
    } catch (e: unknown) {
      const errorMessage = e instanceof Error ? e.message : 'Failed to load dashboard';
      error.value = errorMessage;
    } finally {
      loading.value = false;
    }
  }

  return { stats, loading, error, load };
}
