import { computed, onMounted } from 'vue';

import { useAuthInfo } from './useAuthInfo';

/**
 * Simple auth composable for basic authentication needs
 * For more detailed user info, use useAuthInfo instead
 */
export function useAuth() {
  const { authInfo, loading, initialized, init } = useAuthInfo();

  onMounted(async () => {
    if (!initialized.value) {
      try {
        await init();
      } catch {
        // Handle authentication error silently
      }
    }
  });

  return {
    token: computed(() => authInfo.value.token),
    isAuthenticated: computed(() => authInfo.value.isAuth),
    loading,
    initialized,
    userInfo: computed(() => ({
      userName: authInfo.value.userName,
      userMail: authInfo.value.userMail,
      tenantId: authInfo.value.tenantId,
      userId: authInfo.value.userId,
      roles: authInfo.value.roles,
    })),
  };
}
