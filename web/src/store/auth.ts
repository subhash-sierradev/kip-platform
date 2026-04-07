import type Keycloak from 'keycloak-js';
import { defineStore } from 'pinia';

import {
  getToken,
  getUserInfo,
  isAuthenticated,
  loginIfNeeded,
  logout as keycloakLogout,
} from '@/config/keycloak';

interface UserInfo {
  userName: string;
  userMail: string;
  tenantId: string;
  userId: string;
  roles: string[];
}

interface AuthState {
  token: string | null;
  loading: boolean;
  initialized: boolean;
  userInfo: UserInfo | null;
  keycloakInstance: Keycloak | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: null,
    loading: false,
    initialized: false,
    userInfo: null,
    keycloakInstance: null,
  }),

  getters: {
    isAuthenticated: state => state.initialized && !!state.token && isAuthenticated(),
    currentUser: state => state.userInfo,
    userRoles: state => state.userInfo?.roles ?? [],
  },

  actions: {
    async init() {
      if (this.initialized) return this.keycloakInstance;

      this.loading = true;
      try {
        const kc = await loginIfNeeded();
        this.keycloakInstance = kc;
        this.token = getToken() || null;
        this.userInfo = getUserInfo();
        this.initialized = true;
        return kc;
      } catch {
        // Handle error appropriately or log it if needed
      } finally {
        this.loading = false;
      }
    },

    async logout() {
      try {
        await keycloakLogout();
        this.$reset();
      } catch {
        console.error('Logout failed');
      }
    },

    updateToken(token: string) {
      this.token = token;
      this.userInfo = getUserInfo();
    },

    hasRole(role: string): boolean {
      return this.userInfo?.roles.includes(role) ?? false;
    },

    hasAnyRole(roles: string[]): boolean {
      return roles.some(role => this.userInfo?.roles.includes(role) ?? false);
    },

    hasAllRoles(roles: string[]): boolean {
      return roles.every(role => this.userInfo?.roles.includes(role) ?? false);
    },

    $reset() {
      this.token = null;
      this.loading = false;
      this.initialized = false;
      this.userInfo = null;
      this.keycloakInstance = null;
    },
  },
});
