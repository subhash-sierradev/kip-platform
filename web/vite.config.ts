import { defineConfig, loadEnv } from 'vite';
// Declare Node globals for ESLint without enabling browser rule conflicts
 
declare const process: NodeJS.Process;
 
import { readFileSync } from 'node:fs';
import path from 'node:path';

import vue from '@vitejs/plugin-vue';

// Helper to get package version
const getPackageVersion = (cwd: string): string => {
  try {
    const pkgRaw = readFileSync(path.resolve(cwd, 'package.json'), 'utf8');
    const pkg = JSON.parse(pkgRaw);
    return pkg.version || 'development';
  } catch {
    return 'development';
  }
};

// Helper to create global definitions
const createGlobalDefs = (env: Record<string, string>, version: string) => ({
  'globalThis.__APP_VERSION__': JSON.stringify(version),
  'globalThis.__APP_BUILD_NUMBER__': JSON.stringify(env.VITE_APP_BUILD_NUMBER || '0'),
  'globalThis.__APP_BUILD_DATE__': JSON.stringify(env.VITE_APP_BUILD_DATE || new Date().toISOString()),
});

// Helper to create server config
const createServerConfig = (env: Record<string, string>) => ({
  allowedHosts: ['kaseware.sierradev.com'],
  port: 8084,
  strictPort: true,
  hmr: env.VITE_REMOTE_HMR === 'true' ? {
    protocol: 'wss',
    host: 'kaseware.sierradev.com'
  } : undefined
});

// Build the config dynamically
export default defineConfig(({ mode }) => {
  const cwd = process.cwd();
  const env = loadEnv(mode, cwd, '');
  const pkgVersion = getPackageVersion(cwd);

  return {
    plugins: [vue()],
    base: env.VITE_BASE_URL || '/',
    define: createGlobalDefs(env, pkgVersion),
    server: createServerConfig(env),
    resolve: {
      alias: {
        '@': path.resolve(cwd, 'src')
      }
    },
    optimizeDeps: {
      include: [
        'devextreme-vue'
      ]
    },
    ssr: {
      noExternal: ['devextreme', 'devextreme-vue']
    }
  };
});
