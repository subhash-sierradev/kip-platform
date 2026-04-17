#!/usr/bin/env node

/**
 * version-manager.js — Per-component version management for kip-platform monorepo.
 *
 * Each component (web, ims, ies, contract) has its own independent SemVer version.
 * Source files are the single source of truth — no shared version.json.
 *   web      -> web/package.json
 *   ims      -> api/gradle.properties (imsVersion)
 *   ies      -> api/gradle.properties (iesVersion)
 *   contract -> api/gradle.properties (contractVersion)
 *
 * Usage:
 *   node scripts/version-manager.js current [component]
 *   node scripts/version-manager.js bump <component> [major|minor|patch]
 *   node scripts/version-manager.js tags [component]
 */

'use strict';

const fs   = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const ROOT_DIR       = path.resolve(__dirname, '..');
const WEB_PKG        = path.join(ROOT_DIR, 'web', 'package.json');
const IMS_BUILD      = path.join(ROOT_DIR, 'api', 'integration-management-service', 'build.gradle.kts');
const IES_BUILD      = path.join(ROOT_DIR, 'api', 'integration-execution-service', 'build.gradle.kts');
const CONTRACT_BUILD = path.join(ROOT_DIR, 'api', 'integration-execution-contract', 'build.gradle.kts');

const COMPONENTS = {
  web:      { label: 'Web (Vue)',                  type: 'npm',    buildFile: null },
  ims:      { label: 'Integration Mgmt Service',   type: 'gradle', buildFile: IMS_BUILD },
  ies:      { label: 'Integration Exec Service',   type: 'gradle', buildFile: IES_BUILD },
  contract: { label: 'Integration Exec Contract',  type: 'gradle', buildFile: CONTRACT_BUILD },
};

// ---------------------------------------------------------------------------
// SemVer utilities
// ---------------------------------------------------------------------------

function parseVersion(version) {
  const match = version.match(/^(\d+)\.(\d+)\.(\d+)(?:-(.+))?$/);
  if (!match) throw new Error(`Invalid version format: ${version}`);
  return {
    major:      parseInt(match[1], 10),
    minor:      parseInt(match[2], 10),
    patch:      parseInt(match[3], 10),
    prerelease: match[4] || null,
  };
}

function formatVersion(v) {
  const base = `${v.major}.${v.minor}.${v.patch}`;
  return v.prerelease ? `${base}-${v.prerelease}` : base;
}

function bumpVersion(current, bumpType) {
  const v = parseVersion(current);
  // Strip prerelease before bumping
  v.prerelease = null;

  switch (bumpType) {
    case 'major': v.major++; v.minor = 0; v.patch = 0; break;
    case 'minor': v.minor++; v.patch = 0; break;
    case 'patch': v.patch++; break;
    default: throw new Error(`Invalid bump type "${bumpType}". Use major | minor | patch.`);
  }
  return formatVersion(v);
}

// ---------------------------------------------------------------------------
// Source-file readers (read current version from each component's own file)
// ---------------------------------------------------------------------------

function readNpmVersion() {
  const pkg = JSON.parse(fs.readFileSync(WEB_PKG, 'utf8'));
  if (!pkg.version) throw new Error('No "version" field in web/package.json');
  return pkg.version;
}

function readBuildGradleVersion(buildFile) {
  const content = fs.readFileSync(buildFile, 'utf8');
  const match = content.match(/^version\s*=\s*"(.+)"$/m);
  if (!match) throw new Error(`Cannot find version = "..." in ${path.relative(ROOT_DIR, buildFile)}`);
  return match[1].trim();
}

function readComponentVersion(name) {
  const comp = COMPONENTS[name];
  if (!comp) throw new Error(`Unknown component "${name}". Valid: ${Object.keys(COMPONENTS).join(', ')}`);
  return comp.type === 'npm' ? readNpmVersion() : readBuildGradleVersion(comp.buildFile);
}

// ---------------------------------------------------------------------------
// Source-file writers
// ---------------------------------------------------------------------------

function writeNpmVersion(newVersion) {
  const pkg = JSON.parse(fs.readFileSync(WEB_PKG, 'utf8'));
  const old = pkg.version;
  pkg.version = newVersion;
  fs.writeFileSync(WEB_PKG, JSON.stringify(pkg, null, 2) + '\n');
  console.log(`  ✅ web/package.json: ${old} → ${newVersion}`);
}

function writeBuildGradleVersion(buildFile, newVersion) {
  let content = fs.readFileSync(buildFile, 'utf8');
  const match = content.match(/^version\s*=\s*"(.+)"$/m);
  const old = match ? match[1].trim() : '(not set)';
  content = content.replace(/^version\s*=\s*".+"$/m, `version = "${newVersion}"`);
  fs.writeFileSync(buildFile, content);
  console.log(`  ✅ ${path.relative(ROOT_DIR, buildFile)}: ${old} → ${newVersion}`);
}

function writeComponentVersion(name, newVersion) {
  const comp = COMPONENTS[name];
  if (comp.type === 'npm') {
    writeNpmVersion(newVersion);
  } else {
    writeBuildGradleVersion(comp.buildFile, newVersion);
  }
}

// ---------------------------------------------------------------------------
// Git tag helper
// ---------------------------------------------------------------------------

function createLocalTag(tagName, message) {
  try {
    execSync(`git rev-parse --verify "${tagName}"`, { stdio: 'ignore' });
    console.log(`  ⚠️  Tag ${tagName} already exists, skipping.`);
    return false;
  } catch {
    // tag doesn't exist — safe to create
  }
  try {
    execSync(`git tag -a "${tagName}" -m "${message}"`, { stdio: 'inherit' });
    console.log(`  🏷️  Created local tag: ${tagName}`);
    return true;
  } catch (err) {
    console.error(`  ❌ Failed to create tag ${tagName}: ${err.message}`);
    return false;
  }
}

// ---------------------------------------------------------------------------
// Commands
// ---------------------------------------------------------------------------

/**
 * current [component]
 * Print current version(s) directly from source files.
 */
function cmdCurrent(args) {
  const name = args[0];

  if (name) {
    if (!COMPONENTS[name]) throw new Error(`Unknown component "${name}".`);
    console.log(readComponentVersion(name));
  } else {
    console.log('\n📦 Component Versions (from source files):');
    for (const [key, meta] of Object.entries(COMPONENTS)) {
      try {
        const ver = readComponentVersion(key);
        console.log(`  ${key.padEnd(10)}  ${ver.padEnd(15)}  ${meta.label}`);
      } catch (err) {
        console.log(`  ${key.padEnd(10)}  (not set)       ${meta.label}`);
      }
    }
    console.log('');
  }
}

/**
 * bump <component> [major|minor|patch]
 * Bump a single component's version directly in its source file.
 * If bump type is omitted, defaults to 'patch'.
 */
function cmdBump(args) {
  const name     = args[0];
  const bumpType = args[1] || 'patch';

  if (!name) throw new Error('Usage: bump <component> [major|minor|patch]');
  if (!COMPONENTS[name]) throw new Error(`Unknown component "${name}". Valid: ${Object.keys(COMPONENTS).join(', ')}`);

  const current = readComponentVersion(name);
  const newVersion = bumpVersion(current, bumpType);
  console.log(`\n🔢 Bumping ${name}: ${current} → ${newVersion} (${bumpType})\n`);

  writeComponentVersion(name, newVersion);

  console.log(`\n✅ ${name} bumped to ${newVersion}`);
  console.log('  📝 Changes are local only. Push commits + git push --tags when ready.\n');

  return { component: name, oldVersion: current, newVersion, bumpType };
}

/**
 * tags [component]
 * List local git tags for one or all components.
 */
function cmdTags(args) {
  const name = args[0];
  const pattern = name ? `${name}-*` : '*-*.*.**';

  try {
    const tags = execSync(`git tag -l "${pattern}" --sort=-version:refname`, { encoding: 'utf8' });
    if (tags.trim()) {
      console.log(`\n🏷️  Tags matching "${pattern}":`);
      tags.trim().split('\n').slice(0, 20).forEach(t => console.log(`  ${t}`));
      console.log('');
    } else {
      console.log(`\n  (no tags matching "${pattern}")\n`);
    }
  } catch (err) {
    console.error('❌ Failed to list tags:', err.message);
  }
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

const [,, command, ...rest] = process.argv;

try {
  switch (command) {
    case 'current': cmdCurrent(rest); break;
    case 'bump':    cmdBump(rest);    break;
    case 'tags':    cmdTags(rest);    break;
    default:
      console.error(`
Usage: node scripts/version-manager.js <command> [args]

Commands:
  current [component]            Print component version(s) from source files
  bump <component> [type]        Bump one component (major|minor|patch, default: patch)
  tags [component]               List local git tags for a component or all

Components: ${Object.keys(COMPONENTS).join(', ')}
`);
      process.exit(1);
  }
} catch (err) {
  console.error(`\n❌ Error: ${err.message}\n`);
  process.exit(1);
}
