// DevExtreme license activation: Environment variable only
// - VITE_DEVEXTREME_LICENSE_KEY: the DevExtreme license string from properties file
// Simplified to use only environment configuration

import config from 'devextreme/core/config';

function isNonEmpty(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0;
}

export function activateDevExtremeLicense(): void {
  const debug = Boolean(import.meta.env.VITE_DEVEXTREME_LICENSE_DEBUG);
  const envKey = import.meta.env.VITE_DEVEXTREME_LICENSE_KEY as string | undefined;

  if (isNonEmpty(envKey)) {
    config({ licenseKey: envKey.trim() });
    return;
  }

  // No license key found - continue in trial mode
  if (debug) console.warn('[DevExtreme] No license key found; running in trial mode');
}
