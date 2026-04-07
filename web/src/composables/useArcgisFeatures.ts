import { ref } from 'vue';

import type { ArcGISFeatureField } from '@/api/models/ArcGISFeatureField';
import { ArcGISIntegrationService } from '@/api/services/ArcGISIntegrationService';

export function useArcgisFeatures() {
  const fields = ref<ArcGISFeatureField[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const load = async (connectionId: string) => {
    loading.value = true;
    error.value = null;
    try {
      const resp = await ArcGISIntegrationService.listFeatureFields(connectionId);

      // Debug logging to understand the actual response structure
      if (process.env.NODE_ENV === 'development') {
        console.warn('ArcGIS Features API Response:', resp);
        console.warn('Response type:', typeof resp);
        console.warn('Is Array:', Array.isArray(resp));
        if (Array.isArray(resp) && resp.length > 0) {
          console.warn('First field structure:', resp[0]);
          console.warn('Available properties:', Object.keys(resp[0]));
        }
      }

      fields.value = Array.isArray(resp) ? resp : [];
    } catch (e) {
      error.value = (e as Error)?.message || 'Failed to load ArcGIS fields';
      fields.value = [];
      console.error('Failed to load ArcGIS fields:', e);
    } finally {
      loading.value = false;
    }
  };

  return { fields, loading, error, load };
}
