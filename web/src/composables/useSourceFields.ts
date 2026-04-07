import { computed, ref } from 'vue';

import type { KwDocField } from '@/api/models/KwDocField';
import { KwDocService } from '@/api/services/KwIntegrationService';

export function useSourceFields() {
  const fields = ref<KwDocField[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const load = async () => {
    if (loading.value) return;

    loading.value = true;
    error.value = null;

    try {
      const response = await KwDocService.getSourceFieldMappings();
      fields.value = response || [];
    } catch (err) {
      console.error('Failed to fetch source fields:', err);
      error.value = err instanceof Error ? err.message : 'Failed to load source fields';
      fields.value = [];
    } finally {
      loading.value = false;
    }
  };

  const isReady = computed(() => !loading.value && fields.value.length > 0);

  return {
    fields,
    loading,
    error,
    isReady,
    load,
  };
}
