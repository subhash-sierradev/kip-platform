import axios from 'axios';
import { type Ref, ref, watch } from 'vue';

import { OpenAPI } from '@/api/core/OpenAPI';
import { getApiBaseUrl } from '@/config/env';
import { getToken, refreshToken } from '@/config/keycloak';
import { useAuthStore } from '@/store';

// Configure OpenAPI base URL from environment variables
const configureBaseUrl = () => {
  const apiBaseUrl = getApiBaseUrl();

  if (apiBaseUrl) {
    OpenAPI.BASE = apiBaseUrl;
  } else {
    const isDevelopment =
      window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';

    if (isDevelopment) {
      OpenAPI.BASE = 'http://localhost:8085/api';
    } else {
      OpenAPI.BASE = 'https://kaseware.sierradev.com/backend/api';
    }
  }
};

/**
 * Configure OpenAPI client with reactive token management and dynamic headers
 * This matches the React frontend useConfigureOpenAPI pattern
 * @param token - Reactive reference to the current JWT token
 */
export function useConfigureOpenAPI(token: Ref<string | undefined>) {
  const latestTokenRef = ref<string | undefined>(token.value);
  const authStore = useAuthStore();

  // Configure base URL once
  configureBaseUrl();

  // Set up reactive token function
  OpenAPI.TOKEN = async () => {
    // Always prefer live Keycloak token so requests don't use stale store values.
    const currentToken = getToken();
    if (currentToken) {
      try {
        await refreshToken(30);
      } catch {
        // refreshToken handles login fallback internally; continue with best available token.
      }
      return getToken() || currentToken;
    }

    return latestTokenRef.value || '';
  };

  // Provide dynamic headers (tenant/user) via resolver
  OpenAPI.HEADERS = async () => {
    const user = authStore.currentUser;
    const headers: Record<string, string> = {};

    if (user?.tenantId) {
      headers['X-TENANT-ID'] = user.tenantId;
    }
    if (user?.userId) {
      headers['X-USER-ID'] = user.userId;
    }

    return headers;
  };

  // Watch for token changes and keep the latest value in sync for OpenAPI
  watch(
    token,
    newToken => {
      latestTokenRef.value = newToken;
    },
    { immediate: true }
  );

  // Return the configured API client for manual use
  return {
    apiClient: axios,
    OpenAPI,
  };
}
