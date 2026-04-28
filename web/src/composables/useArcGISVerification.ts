// TODO KIP-547 REMOVE — Temporary composable for ArcGIS feature service verification
import { ref } from 'vue';

import { OpenAPI } from '@/api/core/OpenAPI';
import { request as __request } from '@/api/core/request';

export interface ArcGISVerificationRecord {
  [key: string]: unknown;
}

export interface ArcGISVerificationPageResponse {
  features: ArcGISVerificationRecord[];
  fetchedCount: number;
  offset: number;
  exceededTransferLimit: boolean;
}

export function useArcGISVerification() {
  const records = ref<ArcGISVerificationRecord[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const hasMore = ref(false);
  const currentOffset = ref(0);

  async function fetchRecords(
    offset: number,
    objectId?: string,
    locationId?: string
  ): Promise<void> {
    loading.value = true;
    error.value = null;

    try {
      const query: Record<string, string | number> = { offset };
      if (objectId?.trim()) query.objectId = objectId.trim();
      if (locationId?.trim()) query.locationId = locationId.trim();

      const response = await __request<ArcGISVerificationPageResponse>(OpenAPI, {
        method: 'GET',
        url: '/management/arcgis/verification/features',
        query,
      });

      if (offset === 0) {
        records.value = response.features ?? [];
      } else {
        records.value = [...records.value, ...(response.features ?? [])];
      }

      currentOffset.value = offset;
      hasMore.value = response.exceededTransferLimit ?? false;
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : 'Failed to load ArcGIS records. Please try again.';
      error.value = message;
    } finally {
      loading.value = false;
    }
  }

  function reset(): void {
    records.value = [];
    error.value = null;
    hasMore.value = false;
    currentOffset.value = 0;
  }

  return {
    records,
    loading,
    error,
    hasMore,
    currentOffset,
    fetchRecords,
    reset,
  };
}
