/// <reference types="vitest" />
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';

// Import centralized coverage configuration
import coverageConfig from './coverage.config.js';

// Cross-platform path resolution
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const testSuite = process.env.VITEST_SUITE ?? 'all';

const integrationIncludePatterns = ['src/tests/integration/**/*.{test,spec}.{ts,vue}'];
const baseIncludePatterns = [
  'src/**/*.{test,spec}.{ts,vue}',
  'src/test/**/*.{test,spec}.{ts,vue}',
  'src/tests/**/*.{test,spec}.{ts,vue}'
];

const includePatterns =
  testSuite === 'integration'
    ? integrationIncludePatterns
    : testSuite === 'unit'
      ? baseIncludePatterns
      : [...baseIncludePatterns, ...integrationIncludePatterns];

const excludePatterns = [
  'node_modules/**',
  'dist/**',
  'coverage/**',
  'src/test/setup.ts',
  '*.e2e.{test,spec}.{ts}',
  '**/*.d.ts',
  'src/tests/unit/ArcGISWizard.FieldMappingStep.spec.ts',
  ...(testSuite === 'unit' ? ['src/tests/integration/**'] : [])
];

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      // block trial panel at import time (CJS-safe)
      'devextreme/cjs/__internal/core/license/trial_panel.client':
        path.resolve(__dirname, 'src/tests/mocks/devextreme-trial-panel.ts'),

      // block theme engine entirely
      'devextreme/cjs/__internal/ui/themes':
        path.resolve(__dirname, 'src/tests/mocks/devextreme-themes.ts'),
    }
  },
  test: {
    globals: true,
    isolate: true,
    environment: 'jsdom',
    setupFiles: ['./src/tests/setup/setup.ts'],
    include: includePatterns,
    exclude: excludePatterns,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'json-summary', 'html', 'lcov'],
      reportsDirectory: './coverage',
      include: [
        'src/**/*.ts',
        'src/**/*.vue'
      ],
      exclude: [
        'node_modules/**',
        'dist/**',
        'build/**',
        'coverage/**',
        'src/test/**',
        'src/tests/**',
        '**/*.test.*',
        '**/*.spec.*',
        'src/main.ts',
        'src/env.d.ts',
        'src/shims-vue.d.ts',
        '**/*.d.ts',
        '**/*.js.map',
        '**/*.css.map',
        '**/generated/**',
        '**/.temp/**',
        '**/temp/**',
        '**/.cache/**',
        'src/**/*.js',

        // Category 1: Auto-generated OpenAPI client code
        // Emitted by openapi-typescript-codegen with "/* istanbul ignore file */"
        // headers. No team-written logic; must not be edited.
        'src/api/core/**',

        // Category 2: DTO / model files (no executable business logic)
        // Plain TypeScript interfaces, enums, and generated response shapes.
        // Nothing meaningful to assert; integration tests validate wire format.
        'src/api/models/**',

        // Category 3: Barrel / re-export index files (no logic)
        'src/api/index.ts',
        'src/api/services/index.ts',
        'src/composables/index.ts',

        // Category 4: TypeScript type-only definitions
        'src/types/**',

        // Category 5: Application bootstrap and router config
        // Entry points with no independently testable logic.
        'src/App.vue',
        'src/router/index.ts'

      ],
      thresholds: {
        global: {
          statements: coverageConfig.threshold.statements,
          branches: coverageConfig.threshold.branches,
          functions: coverageConfig.threshold.functions,
          lines: coverageConfig.threshold.lines
        }
      }
    },
    testTimeout: 10000,
    hookTimeout: 10000,
    teardownTimeout: 1000,
  }
});
