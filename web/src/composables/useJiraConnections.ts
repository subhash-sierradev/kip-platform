import { ref } from 'vue';

import { api } from '@/api/OpenAPI';

export interface IntegrationConnectionResponse {
  id: string;
  name?: string;
  serviceType?: string;
  baseUrl?: string;
  jiraBaseUrl?: string; // For backward compatibility
  lastConnectionStatus?: string;
  authType?: string;
}

interface FetchParams {
  page?: number;
  size?: number;
  status?: string;
}

export function useJiraConnections() {
  const data = ref<IntegrationConnectionResponse[] | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetch(params: FetchParams = {}) {
    loading.value = true;
    error.value = null;
    try {
      const resp = await api.get('/integrations/jira/connections/global', {
        params: { page: params.page ?? 0, size: params.size ?? 10, status: params.status ?? 'ALL' },
      });
      data.value = resp.data.content ?? [];
    } catch (e: unknown) {
      const errorMessage = e instanceof Error ? e.message : 'Request failed';
      error.value = errorMessage;
    } finally {
      loading.value = false;
    }
  }

  async function create(payload: { jiraBaseUrl: string; credentialTypeCode: string }) {
    return api.post('/integrations/jira/connections', payload);
  }

  return { data, loading, error, fetch, create };
}
