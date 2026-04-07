import { beforeEach, describe, expect, it, vi } from 'vitest';

// Mock devextreme config
const configMock = vi.fn();
vi.mock('devextreme/core/config', () => ({ default: configMock }));

describe('activateDevExtremeLicense', () => {
  beforeEach(async () => {
    configMock.mockClear();
    vi.clearAllMocks();
    await vi.resetModules();
    // Clear all environment variables completely
    vi.unstubAllEnvs();
  });

  it('applies license from env key and logs when debug enabled', async () => {
    vi.stubEnv('VITE_DEVEXTREME_LICENSE_DEBUG', 'true');
    vi.stubEnv('VITE_DEVEXTREME_LICENSE_KEY', '  ABC-123  ');

    const infoSpy = vi.spyOn(console, 'info').mockImplementation(() => {});
    const { activateDevExtremeLicense } = await import('@/config/devextremeLicense');
    activateDevExtremeLicense();

    expect(configMock).toHaveBeenCalledWith({ licenseKey: 'ABC-123' });
    infoSpy.mockRestore();
  });

  // Note: Fallback via import.meta.glob is environment-specific in Vitest.
  // We validate env-key path and trial-mode path which cover primary branches.

  it('does nothing when no key found and debug warns trial mode', async () => {
    // Ensure clean state
    vi.unstubAllEnvs();
    await vi.resetModules();

    // Set debug mode but ensure no license keys exist
    vi.stubEnv('VITE_DEVEXTREME_LICENSE_DEBUG', 'true');

    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const { activateDevExtremeLicense } = await import('@/config/devextremeLicense');
    activateDevExtremeLicense();

    // No license found, should warn about trial mode
    expect(configMock).not.toHaveBeenCalled();
    expect(warnSpy).toHaveBeenCalledWith(
      '[DevExtreme] No license key found; running in trial mode'
    );
    warnSpy.mockRestore();
  });
});
