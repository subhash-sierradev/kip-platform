import type { Ref } from 'vue';
import { computed, ref } from 'vue';

/**
 * Simple composable for API state management
 */
export function useApiData<T>(initialValue: T[] = []) {
  const data = ref(initialValue) as Ref<T[]>;
  const loading = ref(false);
  const error = ref<string | null>(null);

  const hasData = computed(() => data.value.length > 0);
  const hasError = computed(() => error.value !== null);

  const setData = (newData: T[]) => {
    data.value = newData;
    error.value = null;
  };

  const setError = (errorMessage: string) => {
    error.value = errorMessage;
    data.value = [];
  };

  const setLoading = (isLoading: boolean) => {
    loading.value = isLoading;
  };

  const reset = () => {
    data.value = [];
    loading.value = false;
    error.value = null;
  };

  return {
    data,
    loading,
    error,
    hasData,
    hasError,
    setData,
    setError,
    setLoading,
    reset,
  };
}
