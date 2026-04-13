/**
 * Centralized Coverage Configuration
 *
 * Single source of truth for all coverage thresholds across the project.
 * Update these values in one place to apply across vitest, package.json scripts,
 * and CI/CD pipelines.
 */

/**
 * @typedef {Object} CoverageThreshold
 * @property {number} statements - Statement coverage percentage
 * @property {number} branches - Branch coverage percentage
 * @property {number} functions - Function coverage percentage
 * @property {number} lines - Line coverage percentage
 */

/**
 * @typedef {Object} CoverageConfig
 * @property {CoverageThreshold} threshold - Current coverage thresholds
 */

/** @type {CoverageConfig} */
const coverageConfig = {
  threshold: {
    statements: 89,
    branches: 83,
    functions: 89,
    lines: 90,
  },
};

export default coverageConfig;
