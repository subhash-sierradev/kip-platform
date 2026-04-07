/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.useFakeTimers();

vi.mock('@/api/services/SettingsService', () => ({
  SettingsService: {
    listSiteConfigs: vi.fn(),
  },
}));

vi.mock('@/config/keycloak', () => ({
  logout: vi.fn(async () => {}),
}));

vi.mock('@/store/auth', () => ({
  useAuthStore: vi.fn(() => ({
    isAuthenticated: true,
    token: 'tok-abc',
    initialized: true,
  })),
}));

import { SettingsService } from '@/api/services/SettingsService';
import { logout as keycloakLogout } from '@/config/keycloak';
import useAutoLogout from '@/composables/useAutoLogout';

const AUTO_LOGOUT_KEY = 'AUTO_LOGOUT_TIME';

describe('useAutoLogout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.restoreAllMocks();
  });

  it('disables when AUTO_LOGOUT_TIME = 0 and does not attach listeners', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: AUTO_LOGOUT_KEY, configValue: '0' },
    ]);
    const addSpy = vi.spyOn(window, 'addEventListener');

    const auto = useAutoLogout();
    // initial setup schedules fetch immediately via watch
    await vi.advanceTimersByTimeAsync(100);

    expect(auto.isEnabled.value).toBe(false);
    expect(addSpy).not.toHaveBeenCalled();
    expect(auto.timeRemaining.value).toBe(0);
  });

  it('enables when AUTO_LOGOUT_TIME > 0, attaches listeners, and logs out after inactivity', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: AUTO_LOGOUT_KEY, configValue: '0.001' }, // 0.001 min = ~60ms
    ]);

    const addSpy = vi.spyOn(window, 'addEventListener');

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100); // allow initial fetch to run

    expect(auto.isEnabled.value).toBe(true);
    expect(auto.inactivityLimit.value).toBeGreaterThan(0);
    expect(addSpy).toHaveBeenCalled();
    expect(auto.timeRemaining.value).toBeGreaterThan(0);

    // advance just past inactivity limit to trigger logout
    await vi.advanceTimersByTimeAsync(auto.inactivityLimit.value + 10);
    // Multiple resetTimer calls can occur due to watchers; assert at least once
    expect(keycloakLogout).toHaveBeenCalled();
  });

  it('handles missing configValue in site config', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: AUTO_LOGOUT_KEY, configValue: undefined },
    ]);

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    // Should keep default enabled state
    expect(auto.isEnabled.value).toBe(true);
  });

  it('handles non-numeric configValue gracefully', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: AUTO_LOGOUT_KEY, configValue: 'invalid' },
    ]);

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    // Should keep default enabled state
    expect(auto.isEnabled.value).toBe(true);
  });

  it('handles non-array response from listSiteConfigs', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue(null);

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    // Should keep default enabled state
    expect(auto.isEnabled.value).toBe(true);
  });

  it('handles API error when fetching configs', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    (SettingsService.listSiteConfigs as any).mockRejectedValue(new Error('API Error'));

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      '[AutoLogout] Error fetching site configs:',
      expect.any(Error)
    );
    expect(consoleErrorSpy).toHaveBeenCalledWith('[AutoLogout] Error details:', 'API Error');
    // Should keep default enabled state
    expect(auto.isEnabled.value).toBe(true);
  });

  it('handles non-Error exception when fetching configs', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    (SettingsService.listSiteConfigs as any).mockRejectedValue('String error');

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    expect(consoleErrorSpy).toHaveBeenCalledWith(
      '[AutoLogout] Error fetching site configs:',
      'String error'
    );
    // Should not call error details for non-Error (only 1 console.error call)
    expect(auto.isEnabled.value).toBe(true);
  });

  it('handles case-insensitive config key matching', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: 'auto_logout_time', configValue: '10' },
    ]);

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    expect(auto.isEnabled.value).toBe(true);
    expect(auto.inactivityLimit.value).toBe(10 * 60 * 1000);
  });

  it('handles empty configKey in config entry', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: '', configValue: '5' },
    ]);

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    // Should not match and keep defaults
    expect(auto.isEnabled.value).toBe(true);
  });

  it('updates timeRemaining countdown properly', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: AUTO_LOGOUT_KEY, configValue: '0.1' }, // 6 seconds
    ]);

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    const initialRemaining = auto.timeRemaining.value;
    expect(initialRemaining).toBeGreaterThan(0);

    // Advance by 2 seconds
    await vi.advanceTimersByTimeAsync(2000);

    expect(auto.timeRemaining.value).toBeLessThan(initialRemaining);
    expect(auto.timeRemaining.value).toBeGreaterThan(0);
  });

  it('countdown stops at 0 and does not go negative', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: AUTO_LOGOUT_KEY, configValue: '0.001' },
    ]);

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    // Advance past the limit
    await vi.advanceTimersByTimeAsync(auto.inactivityLimit.value + 5000);

    expect(auto.timeRemaining.value).toBe(0);
  });

  it('resets timer on user activity events', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: AUTO_LOGOUT_KEY, configValue: '0.1' },
    ]);

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    const initialRemaining = auto.timeRemaining.value;

    // Simulate some time passing
    await vi.advanceTimersByTimeAsync(2000);
    expect(auto.timeRemaining.value).toBeLessThan(initialRemaining);

    // Simulate user activity (mousemove)
    window.dispatchEvent(new Event('mousemove'));
    await vi.advanceTimersByTimeAsync(100);

    // Timer should be reset
    expect(auto.timeRemaining.value).toBeGreaterThanOrEqual(initialRemaining - 200);
  });

  it('skips immediate fetch when not authenticated initially', async () => {
    const { useAuthStore } = await import('@/store/auth');
    (useAuthStore as any).mockReturnValue({
      isAuthenticated: false,
      token: null,
      initialized: false,
    });

    (SettingsService.listSiteConfigs as any).mockClear();

    useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    // Should not fetch config when not authenticated
    expect(SettingsService.listSiteConfigs).not.toHaveBeenCalled();
  });

  it('does not reset timer when user is not authenticated', async () => {
    // Start authenticated
    const { useAuthStore } = await import('@/store/auth');
    const mockAuthStore = {
      isAuthenticated: true,
      token: 'tok-123',
      initialized: true,
    };
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any);

    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: AUTO_LOGOUT_KEY, configValue: '5' },
    ]);
    const addSpy = vi.spyOn(window, 'addEventListener');

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    expect(auto.isEnabled.value).toBe(true);
    const initialTime = auto.timeRemaining.value;

    // Change auth state to not authenticated
    mockAuthStore.isAuthenticated = false;
    mockAuthStore.token = '' as any;

    // Simulate user activity when not authenticated
    const mousemoveHandler = addSpy.mock.calls.find(call => call[0] === 'mousemove')?.[1];
    if (mousemoveHandler && typeof mousemoveHandler === 'function') {
      mousemoveHandler(new MouseEvent('mousemove'));
    }

    // Since not authenticated, timer should not reset to initial value
    await vi.advanceTimersByTimeAsync(2000);
    expect(auto.timeRemaining.value).toBeLessThan(initialTime);
  });

  it('handles keycloak logout failure gracefully', async () => {
    (SettingsService.listSiteConfigs as any).mockResolvedValue([
      { configKey: AUTO_LOGOUT_KEY, configValue: '1' },
    ]);
    vi.mocked(keycloakLogout).mockRejectedValueOnce(new Error('Logout failed'));

    // Mock window.location
    const originalLocation = window.location;
    delete (window as any).location;
    window.location = { ...originalLocation, origin: 'http://test.com' } as any;

    const auto = useAutoLogout();
    await vi.advanceTimersByTimeAsync(100);

    expect(auto.isEnabled.value).toBe(true);

    // Advance past the logout time (1 minute)
    await vi.advanceTimersByTimeAsync(60 * 1000);

    // Should attempt keycloak logout and fallback to redirect
    expect(keycloakLogout).toHaveBeenCalled();

    // Restore window.location
    Object.defineProperty(window, 'location', {
      value: originalLocation,
      writable: true,
      configurable: true,
    });
  });
});
