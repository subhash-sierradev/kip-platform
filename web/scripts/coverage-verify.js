#!/usr/bin/env node

/**
 * Coverage Verification Script
 * 
 * Reads coverage results and verifies they meet the configured thresholds.
 * Uses centralized coverage configuration from coverage.config.js
 */

import { readFileSync } from 'fs';
import coverageConfig from '../coverage.config.js';

try {
  // Read coverage summary
  const coverage = JSON.parse(readFileSync('./coverage/coverage-summary.json', 'utf8'));
  const thresholds = coverageConfig.threshold;
  const total = coverage.total;

  // Check each threshold
  const failedChecks = [];
  Object.keys(thresholds).forEach(key => {
    if (total[key].pct < thresholds[key]) {
      failedChecks.push(`❌ ${key.toUpperCase()} coverage ${total[key].pct}% is below ${thresholds[key]}%`);
    }
  });

  // Report results
  if (failedChecks.length > 0) {
    failedChecks.forEach(check => console.error(check));
    process.exit(1);
  } else {
    const fmt = (n) => typeof n === 'number' ? Number(n.toFixed(2)) : n;
    const summary = {
      statements: fmt(total.statements.pct),
      branches: fmt(total.branches.pct),
      functions: fmt(total.functions.pct),
      lines: fmt(total.lines.pct)
    };
    console.log('✅ All coverage thresholds met!');
    console.log(`📊 Coverage summary: statements ${summary.statements}% | branches ${summary.branches}% | functions ${summary.functions}% | lines ${summary.lines}%`);
    process.exit(0);
  }
} catch (error) {
  console.error('❌ Error verifying coverage:', error.message);
  process.exit(1);
}