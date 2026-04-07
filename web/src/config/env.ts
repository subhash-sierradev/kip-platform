// Environment configuration
// This file handles environment-specific settings

export interface EnvironmentConfig {
  API_BASE_URL: string;
  KEYCLOAK_URL: string;
  KEYCLOAK_REALM: string;
  KEYCLOAK_CLIENT_ID: string;
}

// Get environment configuration
export const getEnvironmentConfig = (): EnvironmentConfig => {
  // Try to get from build-time environment variables
  // These are replaced by Vite during build
  const apiBaseUrl = '__VITE_API_BASE_URL__';
  const keycloakUrl = '__VITE_KEYCLOAK_URL__';
  const keycloakRealm = '__VITE_KEYCLOAK_REALM__';
  const keycloakClientId = '__VITE_KEYCLOAK_CLIENT_ID__';

  // Check if variables were replaced (they won't start with '__' if replaced)
  const isReplaced = (value: string) => !value.startsWith('__VITE_');

  // Fall back to import.meta.env if build-time replacement didn't occur
  const fallbackApiBaseUrl =
    (import.meta.env.VITE_API_BASE_URL as string) || 'http://localhost:8085';
  const fallbackKeycloakUrl =
    (import.meta.env.VITE_KEYCLOAK_URL as string) || 'https://kaseware.sierradev.com/auth';
  const fallbackKeycloakRealm =
    (import.meta.env.VITE_KEYCLOAK_REALM as string) || 'integration-platform-realm-dev';
  const fallbackKeycloakClientId =
    (import.meta.env.VITE_KEYCLOAK_CLIENT_ID as string) || 'integration-ui';

  return {
    API_BASE_URL: isReplaced(apiBaseUrl) ? apiBaseUrl : fallbackApiBaseUrl,
    KEYCLOAK_URL: isReplaced(keycloakUrl) ? keycloakUrl : fallbackKeycloakUrl,
    KEYCLOAK_REALM: isReplaced(keycloakRealm) ? keycloakRealm : fallbackKeycloakRealm,
    KEYCLOAK_CLIENT_ID: isReplaced(keycloakClientId) ? keycloakClientId : fallbackKeycloakClientId,
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
