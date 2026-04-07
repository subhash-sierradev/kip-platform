import type Keycloak from 'keycloak-js';
import { computed, ref } from 'vue';

import {
  getToken,
  getTokenParsed,
  getUserInfo,
  isAuthenticated,
  loginIfNeeded,
  logout as keycloakLogout,
} from '@/config/keycloak';

interface KeycloakTokenParsedBase {
  name?: string;
  email?: string;
  preferred_username?: string;
  tenant_id?: string | string[];
  roles?: string[];
  realm_access?: { roles?: string[] };
  [claim: string]: unknown;
}

export interface AuthInfo {
  isAuth: boolean;
  token: string | undefined;
  userName: string;
  userMail: string;
  tenantId: string;
  userId: string;
  roles: string[];
  tokenParsed: KeycloakTokenParsedBase;
  keycloak: Keycloak | undefined;
}

const keycloakInstance = ref<Keycloak | null>(null);
const initialized = ref(false);
const loading = ref(false);

interface UserInfoFields {
  userName?: string;
  userMail?: string;
  tenantId?: string;
  userId?: string;
  roles?: string[];
}

function getUserName(userInfo: UserInfoFields): string {
  return userInfo?.userName || 'Unknown User';
}
function getUserMail(userInfo: UserInfoFields): string {
  return userInfo?.userMail || '';
}
function getTenantId(userInfo: UserInfoFields): string {
  return userInfo?.tenantId || '';
}
function getUserId(userInfo: UserInfoFields): string {
  return userInfo?.userId || '';
}
function getRoles(userInfo: UserInfoFields): string[] {
  return userInfo?.roles || [];
}

function mapUserInfo(
  userInfo: UserInfoFields,
  tokenParsed: KeycloakTokenParsedBase,
  token: string | undefined,
  isAuth: boolean
): AuthInfo {
  return {
    isAuth,
    token,
    userName: getUserName(userInfo),
    userMail: getUserMail(userInfo),
    tenantId: getTenantId(userInfo),
    userId: getUserId(userInfo),
    roles: getRoles(userInfo),
    tokenParsed,
    keycloak: keycloakInstance.value || undefined,
  };
}

export function useAuthInfo() {
  const authInfo = computed((): AuthInfo => {
    const token = getToken();
    const isAuth = initialized.value && isAuthenticated() && !!token;
    const tokenParsed = getTokenParsed() || {};
    const userInfo = getUserInfo() || {};
    return mapUserInfo(userInfo, tokenParsed, token, isAuth);
  });

  const init = async () => {
    if (initialized.value) return keycloakInstance.value;

    loading.value = true;
    const kc = await loginIfNeeded();
    keycloakInstance.value = kc;
    initialized.value = true;
    loading.value = false;
    return kc;
  };

  const logout = async () => {
    await keycloakLogout();
    keycloakInstance.value = null;
    initialized.value = false;
  };

  const hasRole = (role: string) => {
    const info = authInfo.value;
    return info.roles.includes(role);
  };

  const hasAnyRole = (roles: string[]) => {
    const info = authInfo.value;
    return roles.some(role => info.roles.includes(role));
  };

  const hasAllRoles = (roles: string[]) => {
    const info = authInfo.value;
    return roles.every(role => info.roles.includes(role));
  };

  return {
    authInfo: computed(() => authInfo.value),
    loading: computed(() => loading.value),
    initialized: computed(() => initialized.value),
    init,
    logout,
    hasRole,
    hasAnyRole,
    hasAllRoles,
  };
}
