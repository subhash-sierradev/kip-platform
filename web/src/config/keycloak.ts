import Keycloak from 'keycloak-js';

import { getEnvironmentConfig } from './env';

interface KeycloakTokenParsedBase {
  name?: string;
  email?: string;
  preferred_username?: string;
  tenant_id?: string | string[];
  roles?: string[];
  realm_access?: { roles?: string[] };
  [claim: string]: unknown;
}

let keycloak: Keycloak | null = null;

export function initKeycloak(): Keycloak {
  if (keycloak) return keycloak;

  const config = getEnvironmentConfig();
  keycloak = new Keycloak({
    url: config.KEYCLOAK_URL,
    realm: config.KEYCLOAK_REALM,
    clientId: config.KEYCLOAK_CLIENT_ID,
  });

  return keycloak;
}

export async function loginIfNeeded(): Promise<Keycloak> {
  const kc = initKeycloak();
  const authenticated = await kc.init({
    onLoad: 'login-required',
    checkLoginIframe: false,
    enableLogging: import.meta.env.MODE === 'development',
  });

  if (!authenticated) {
    await kc.login();
  }
  return kc;
}

export function getToken(): string | undefined {
  return keycloak?.token;
}

export function getTokenParsed(): KeycloakTokenParsedBase | undefined {
  return keycloak?.tokenParsed as KeycloakTokenParsedBase | undefined;
}

export function isAuthenticated(): boolean {
  return keycloak?.authenticated ?? false;
}

export async function logout(): Promise<void> {
  if (!keycloak) return;

  // Normalizes baseUrl: '/' maps to empty string; otherwise ensures a leading slash is present.
  // Trailing slashes are preserved as-is.
  const baseUrl = import.meta.env.VITE_BASE_URL || '/';
  const normalizedBaseUrl =
    baseUrl === '/' ? '' : baseUrl.startsWith('/') ? baseUrl : `/${baseUrl}`;

  const redirectUri = window.location.origin + normalizedBaseUrl;

  await keycloak.logout({
    redirectUri,
  });
}

export async function refreshToken(minValidity = 30): Promise<boolean> {
  if (!keycloak) return false;

  try {
    const refreshed = await keycloak.updateToken(minValidity);
    return refreshed;
  } catch {
    await keycloak.login();
    return false;
  }
}

// Get user information from token
export function getUserInfo() {
  const tokenParsed = getTokenParsed();
  if (!tokenParsed) return null;
  const userName = tokenParsed.name || 'Unknown User';
  const userMail = tokenParsed.email || '';
  const tenantId = extractTenantId(tokenParsed.tenant_id);
  const userId = tokenParsed.preferred_username || '';
  const roles = extractRoles(tokenParsed);

  return {
    userName,
    userMail,
    tenantId,
    userId,
    roles,
    tokenParsed,
  };
}

// Helper function to extract tenant ID
function extractTenantId(tenantIdData: unknown): string {
  if (Array.isArray(tenantIdData)) {
    return typeof tenantIdData[0] === 'string' ? tenantIdData[0].replace(/\//g, '') : '';
  }
  if (typeof tenantIdData === 'string') {
    return tenantIdData.replace(/\//g, '');
  }
  return '';
}

// Helper function to extract roles
function extractRoles(tokenParsed: {
  roles?: string[];
  realm_access?: { roles?: string[] };
}): string[] {
  return Array.isArray(tokenParsed.roles)
    ? tokenParsed.roles
    : tokenParsed.realm_access?.roles || [];
}

export default keycloak;
