import { computed, ref } from 'vue';

import {
  JiraIntegrationService,
  type JiraSprintResponse,
} from '@/api/services/JiraIntegrationService';

export interface UseJiraSprintsOptions {
  connectionId: string;
  boardId?: number; // optional hint; backend may infer from projectKey
  projectKey?: string; // optional
  state?: string; // e.g., 'active,future'
}

export function useJiraSprints(opts: UseJiraSprintsOptions) {
  const loading = ref(false);
  const error = ref<string | null>(null);
  const items = ref<JiraSprintResponse[]>([]);

  // Reset cached results so the empty-query guard below allows a fresh fetch.
  // Call this before reloading after a projectKey change.
  function clear() {
    items.value = [];
    error.value = null;
  }

  async function search(query = ''): Promise<void> {
    if (!opts.connectionId) return;
    // Skip redundant full-list fetches: if items are already loaded and the
    // caller is not filtering, serve the cache. This stops the sprint API being
    // called on every value change when watch(fieldType) fires concurrently or
    // while allFields is still loading (fieldType temporarily 'string'→'sprint').
    if (query === '' && items.value.length > 0) return;
    loading.value = true;
    error.value = null;
    try {
      const state = opts.state ?? 'active,future';
      const data = await JiraIntegrationService.getSprintsByConnectionId(opts.connectionId, {
        boardId: opts.boardId,
        projectKey: opts.projectKey,
        state,
        query,
        startAt: 0,
        maxResults: 50,
      });
      items.value = (Array.isArray(data) ? data : []).filter((s: JiraSprintResponse) =>
        'archived' in s ? !s.archived : true
      );
    } catch (e: unknown) {
      const msg = (e as { message?: string })?.message ?? 'Failed to load sprints';
      error.value = msg;
      items.value = [];
    } finally {
      loading.value = false;
    }
  }

  const options = computed(() =>
    items.value.map(s => ({ value: String(s.id), label: `${s.name} (${s.state})` }))
  );

  return { loading, error, items, options, search, clear };
}
