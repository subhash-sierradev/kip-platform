import { ref } from 'vue';

import type { IntegrationJobExecutionDto } from '@/api/models/IntegrationJobExecutionDto';
import { ArcGISIntegrationService } from '@/api/services/ArcGISIntegrationService';

export function useArcGISJobHistory() {
  const history = ref<IntegrationJobExecutionDto[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchJobHistory(integrationId: string): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      history.value = await ArcGISIntegrationService.getJobHistory(integrationId);
    } catch (e: unknown) {
      const message = (e as { message?: string })?.message;
      error.value = message || 'Failed to load job history.';
      history.value = [];
    } finally {
      loading.value = false;
    }
  }

  return { history, loading, error, fetchJobHistory };
}
