/**
 * Auto Logout Composable for Vue.js
 */

import { onMounted, onUnmounted, readonly, type Ref, ref, watch } from 'vue';

import { SettingsService } from '@/api/services/SettingsService';
import { logout as keycloakLogout } from '@/config/keycloak';
import { useAuthStore } from '@/store/auth';

// Constants
const DEFAULT_INACTIVITY_LIMIT = 15 * 60 * 1000; // 15 minutes
const AUTO_LOGOUT_KEY = 'AUTO_LOGOUT_TIME';
const ACTIVITY_EVENTS = ['mousemove', 'keydown', 'mousedown', 'touchstart', 'scroll', 'click'];

interface UseAutoLogoutReturn {
  inactivityLimit: Readonly<Ref<number>>;
  isEnabled: Readonly<Ref<boolean>>;
  timeRemaining: Readonly<Ref<number>>;
}

// Helper to process config value
const processConfigValue = (
  entry: { configKey: string; configValue: string } | undefined,
  inactivityLimit: Ref<number>,
  isEnabled: Ref<boolean>
) => {
  if (!entry?.configValue) return;

  const mins = parseFloat(entry.configValue);
  if (Number.isNaN(mins)) return;

  if (mins > 0) {
    inactivityLimit.value = mins * 60 * 1000;
    isEnabled.value = true;
  } else if (mins === 0) {
    isEnabled.value = false;
  }
};

// Helper functions
const createConfigFetcher = (authStore: ReturnType<typeof useAuthStore>) => {
  return async (inactivityLimit: Ref<number>, isEnabled: Ref<boolean>) => {
    if (!authStore.isAuthenticated || !authStore.token || !authStore.initialized) {
      return;
    }

    try {
      const configs = await SettingsService.listSiteConfigs();

      if (!Array.isArray(configs)) {
        return;
      }

      const entry = configs.find(c => (c.configKey || '').toUpperCase() === AUTO_LOGOUT_KEY);
      processConfigValue(entry, inactivityLimit, isEnabled);
    } catch (error) {
      console.error('[AutoLogout] Error fetching site configs:', error);
      if (error instanceof Error) {
        console.error('[AutoLogout] Error details:', error.message);
      }
    }
  };
};

const createTimerManager = (inactivityLimit: Ref<number>, timeRemaining: Ref<number>) => {
  const timers = {
    timer: undefined as number | undefined,
    countdownTimer: undefined as number | undefined,
  };

  const clearTimers = () => {
    if (timers.timer) {
      clearTimeout(timers.timer);
      timers.timer = undefined;
    }
    if (timers.countdownTimer) {
      clearTimeout(timers.countdownTimer);
      timers.countdownTimer = undefined;
    }
  };

  const updateCountdown = () => {
    timeRemaining.value = Math.max(0, timeRemaining.value - 1000);
    if (timeRemaining.value > 0) {
      timers.countdownTimer = window.setTimeout(updateCountdown, 1000);
    }
  };

  return { timers, clearTimers, updateCountdown };
};

// Helper to setup activity listeners
const createActivityManager = (resetTimer: () => void, isEnabled: Ref<boolean>) => {
  const setupActivityListeners = () => {
    if (!isEnabled.value) return;

    ACTIVITY_EVENTS.forEach(event => {
      window.addEventListener(event, resetTimer, { passive: true });
    });
  };

  const cleanupActivityListeners = () => {
    ACTIVITY_EVENTS.forEach(event => {
      window.removeEventListener(event, resetTimer);
    });
  };

  return { setupActivityListeners, cleanupActivityListeners };
};

// Helper to setup authentication watcher
const setupAuthWatcher = (
  authStore: ReturnType<typeof useAuthStore>,
  fetchConfig: (inactivityLimit: Ref<number>, isEnabled: Ref<boolean>) => Promise<void>,
  activityManager: ReturnType<typeof createActivityManager>,
  resetTimer: () => void,
  timerManager: ReturnType<typeof createTimerManager>,
  _inactivityLimit: Ref<number>,
  _isEnabled: Ref<boolean>,
  initTimer: Ref<number | undefined>
) => {
  watch(
    () => ({
      token: authStore.token,
      isAuthenticated: authStore.isAuthenticated,
      initialized: authStore.initialized,
    }),
    async newAuth => {
      if (initTimer.value) {
        clearTimeout(initTimer.value);
        initTimer.value = undefined;
      }

      if (newAuth.token && newAuth.isAuthenticated && newAuth.initialized) {
        initTimer.value = window.setTimeout(async () => {
          await fetchConfig(_inactivityLimit, _isEnabled);
          if (_isEnabled.value) {
            activityManager.setupActivityListeners();
            resetTimer();
          }
          initTimer.value = undefined;
        }, 0);
      } else {
        activityManager.cleanupActivityListeners();
        timerManager.clearTimers();
      }
    },
    { immediate: true }
  );
};

export function useAutoLogout(): UseAutoLogoutReturn {
  const authStore = useAuthStore();
  const initTimer = ref<number | undefined>(undefined);
  const inactivityLimit = ref<number>(DEFAULT_INACTIVITY_LIMIT);
  const isEnabled = ref<boolean>(true);
  const timeRemaining = ref<number>(0);

  const fetchConfig = createConfigFetcher(authStore);
  const timerManager = createTimerManager(inactivityLimit, timeRemaining);

  const performLogout = async () => {
    try {
      await keycloakLogout();
    } catch {
      window.location.href = window.location.origin;
    }
  };

  const resetTimer = () => {
    if (!isEnabled.value || !authStore.isAuthenticated) return;

    timerManager.clearTimers();
    timerManager.timers.timer = window.setTimeout(() => {
      performLogout();
    }, inactivityLimit.value);
    timeRemaining.value = inactivityLimit.value;
    timerManager.timers.countdownTimer = window.setTimeout(timerManager.updateCountdown, 1000);
  };

  const activityManager = createActivityManager(resetTimer, isEnabled);

  // Setup watchers
  setupAuthWatcher(
    authStore,
    fetchConfig,
    activityManager,
    resetTimer,
    timerManager,
    inactivityLimit,
    isEnabled,
    initTimer
  );

  // Watch for enabled state changes
  watch(isEnabled, enabled => {
    if (enabled && authStore.isAuthenticated) {
      activityManager.setupActivityListeners();
      resetTimer();
    } else {
      activityManager.cleanupActivityListeners();
      timerManager.clearTimers();
    }
  });

  onMounted(() => {
    // Component mounted
  });

  onUnmounted(() => {
    activityManager.cleanupActivityListeners();
    timerManager.clearTimers();
    if (initTimer.value) {
      clearTimeout(initTimer.value);
      initTimer.value = undefined;
    }
  });

  return {
    inactivityLimit: readonly(inactivityLimit),
    isEnabled: readonly(isEnabled),
    timeRemaining: readonly(timeRemaining),
  };
}

export default useAutoLogout;
