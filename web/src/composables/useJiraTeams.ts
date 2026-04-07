import { computed, ref } from 'vue';

import {
  JiraIntegrationService,
  type JiraTeamResponse,
} from '@/api/services/JiraIntegrationService';

export interface UseJiraTeamsOptions {
  connectionId: string;
}

export function useJiraTeams(opts: UseJiraTeamsOptions) {
  const loading = ref(false);
  const error = ref<string | null>(null);
  const items = ref<JiraTeamResponse[]>([]);

  async function search(query = ''): Promise<void> {
    if (!opts.connectionId) return;
    loading.value = true;
    error.value = null;
    try {
      const data = await JiraIntegrationService.getTeamsByConnectionId(opts.connectionId, {
        query,
        startAt: 0,
        maxResults: 50,
      });
      items.value = Array.isArray(data) ? data : [];
    } catch (e: unknown) {
      const msg = (e as { message?: string })?.message ?? 'Failed to load teams';
      error.value = msg;
      items.value = [];
    } finally {
      loading.value = false;
    }
  }

  const options = computed(() => items.value.map(t => ({ value: String(t.id), label: t.name })));

  return { loading, error, items, options, search };
}
