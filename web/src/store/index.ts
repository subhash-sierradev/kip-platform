import { createPinia } from 'pinia';

export function configureStore() {
  const pinia = createPinia();
  return pinia;
}

export { useAuthStore } from './auth';
export { useGlobalLoadingStore } from './globalLoading';
export { useNotificationStore } from './notification';
export { useToastStore } from './toast';
