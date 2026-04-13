// Environment configuration
// This file handles environment-specific settings
//
// Resolution order (first non-empty wins):
//   1. window.__RUNTIME_CONFIG__  — injected at container startup by docker-entrypoint.sh
//   2. Vite build-time placeholders (__VITE_*__) — replaced during `vite build`
//   3. import.meta.env.VITE_*     — Vite dev-server / .env files
//   4. Hard-coded defaults

// Runtime config injected by Docker entrypoint (see docker-entrypoint.sh)
interface RuntimeConfig {
  VITE_API_BASE_URL?: string;
  VITE_KEYCLOAK_URL?: string;
  VITE_KEYCLOAK_REALM?: string;
  VITE_KEYCLOAK_CLIENT_ID?: string;
}

declare global {
  interface Window {
    __RUNTIME_CONFIG__?: RuntimeConfig;
  }
}

export interface EnvironmentConfig {
  API_BASE_URL: string;
  KEYCLOAK_URL: string;
  KEYCLOAK_REALM: string;
  KEYCLOAK_CLIENT_ID: string;
}

// Resolves a single config value using the 4-level priority chain:
// runtime → build-time placeholder → Vite env → hard-coded default
const resolveValue = (runtime: string | undefined, placeholder: string, fallback: string): string =>
  runtime || (!placeholder.startsWith('__VITE_') ? placeholder : fallback);

// Get environment configuration
export const getEnvironmentConfig = (): EnvironmentConfig => {
  const runtime = window.__RUNTIME_CONFIG__;

  return {
    API_BASE_URL: resolveValue(
      runtime?.VITE_API_BASE_URL,
      '__VITE_API_BASE_URL__',
      (import.meta.env.VITE_API_BASE_URL as string) || 'http://localhost:8085'
    ),
    KEYCLOAK_URL: resolveValue(
      runtime?.VITE_KEYCLOAK_URL,
      '__VITE_KEYCLOAK_URL__',
      (import.meta.env.VITE_KEYCLOAK_URL as string) || 'https://kaseware.sierradev.com/auth'
    ),
    KEYCLOAK_REALM: resolveValue(
      runtime?.VITE_KEYCLOAK_REALM,
      '__VITE_KEYCLOAK_REALM__',
      (import.meta.env.VITE_KEYCLOAK_REALM as string) || 'integration-platform-realm-dev'
    ),
    KEYCLOAK_CLIENT_ID: resolveValue(
      runtime?.VITE_KEYCLOAK_CLIENT_ID,
      '__VITE_KEYCLOAK_CLIENT_ID__',
      (import.meta.env.VITE_KEYCLOAK_CLIENT_ID as string) || 'integration-ui-vue'
    ),
  };
};

// Environment-specific API base URL
export const getApiBaseUrl = (): string => {
  const config = getEnvironmentConfig();
  return `${config.API_BASE_URL}/api`;
};

// Legacy export for backward compatibility
export const env = {
  get apiBaseUrl() {
    return getApiBaseUrl();
  },
  get keycloak() {
    const config = getEnvironmentConfig();
    return {
      url: config.KEYCLOAK_URL,
      realm: config.KEYCLOAK_REALM,
      clientId: config.KEYCLOAK_CLIENT_ID,
    };
  },
};
