#!/usr/bin/env node

/**
 * Test Coverage Enforcement Script
 * 
 * Runs coverage tests and enforces thresholds with proper exit code handling
 */

import { exec } from 'child_process';
import { readFileSync } from 'fs';
import coverageConfig from '../coverage.config.js';

console.log('🧪 Running tests with coverage...');

// Run vitest with coverage using exec (shell but with escaped command)
exec('npm run test:coverage', { maxBuffer: 1024 * 1024 * 10 }, (error, stdout, stderr) => {
  if (stdout) console.log(stdout);
  if (stderr) console.error(stderr);
  
  if (error) {
    console.error('❌ Tests failed!');
    process.exit(error.code || 1);
  }
  
  console.log('✅ Tests passed! Checking coverage thresholds...');
  
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
      console.error('\n💥 Coverage thresholds not met:');
      failedChecks.forEach(check => console.error(check));
      console.error('\n🎯 Add more tests to reach 80% coverage on all metrics!');
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
});