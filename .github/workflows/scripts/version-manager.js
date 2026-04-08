#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

class VersionManager {
  constructor() {
    // Root of the monorepo (two levels up from .github/workflows/scripts/)
    this.rootDir = path.resolve(__dirname, '..', '..', '..');
    this.versionFile = path.join(this.rootDir, 'version.json');
    this.frontendPackageJson = path.join(this.rootDir, 'web', 'package.json');
    // Single source of truth for all backend versions (Gradle-only build)
    this.gradleProperties = path.join(this.rootDir, 'api', 'gradle.properties');
  }

  readVersionFile() {
    if (!fs.existsSync(this.versionFile)) {
      throw new Error('version.json not found at project root');
    }
    return JSON.parse(fs.readFileSync(this.versionFile, 'utf8'));
  }

  writeVersionFile(versionData) {
    fs.writeFileSync(this.versionFile, JSON.stringify(versionData, null, 2) + '\n');
  }

  parseVersion(version) {
    const match = version.match(/^(\d+)\.(\d+)\.(\d+)(?:-(.+))?$/);
    if (!match) {
      throw new Error(`Invalid version format: ${version}`);
    }
    return {
      major: parseInt(match[1]),
      minor: parseInt(match[2]),
      patch: parseInt(match[3]),
      prerelease: match[4] || null
    };
  }

  formatVersion(versionObj) {
    let version = `${versionObj.major}.${versionObj.minor}.${versionObj.patch}`;
    if (versionObj.prerelease) {
      version += `-${versionObj.prerelease}`;
    }
    return version;
  }

  bumpVersion(currentVersion, bumpType) {
    const parsed = this.parseVersion(currentVersion);

    switch (bumpType) {
      case 'major':
        parsed.major++;
        parsed.minor = 0;
        parsed.patch = 0;
        parsed.prerelease = null;
        break;
      case 'minor':
        parsed.minor++;
        parsed.patch = 0;
        parsed.prerelease = null;
        break;
      case 'patch':
        parsed.patch++;
        parsed.prerelease = null;
        break;
      case 'prerelease':
        if (parsed.prerelease) {
          const prereleaseMatch = parsed.prerelease.match(/^(.+)\.(\d+)$/);
          if (prereleaseMatch) {
            parsed.prerelease = `${prereleaseMatch[1]}.${parseInt(prereleaseMatch[2]) + 1}`;
          } else {
            parsed.prerelease = `${parsed.prerelease}.1`;
          }
        } else {
          parsed.patch++;
          parsed.prerelease = 'alpha.0';
        }
        break;
      default:
        throw new Error(`Invalid bump type: ${bumpType}`);
    }

    return this.formatVersion(parsed);
  }

  updateFrontendVersion(newVersion) {
    if (fs.existsSync(this.frontendPackageJson)) {
      try {
        const packageJson = JSON.parse(fs.readFileSync(this.frontendPackageJson, 'utf8'));
        packageJson.version = newVersion;
        fs.writeFileSync(this.frontendPackageJson, JSON.stringify(packageJson, null, 2) + '\n');
        console.log(`✅ Updated Vue.js frontend version to ${newVersion}`);
      } catch (err) {
        console.error(`❌ Failed to update Vue.js frontend package.json: ${err.message}`);
      }
    } else {
      console.warn('Vue.js frontend package.json not found, skipping Vue.js frontend version update');
    }
  }

  /**
   * Update projectVersion and contractVersion in api/gradle.properties.
   * Both keys are bumped to the same version to keep all modules in sync.
   */
  updateGradleProperties(newVersion) {
    const propsFile = this.gradleProperties;
    const relativePath = path.relative(this.rootDir, propsFile).split(path.sep).join('/');

    if (!fs.existsSync(propsFile)) {
      console.warn(`⚠️  gradle.properties not found: ${relativePath}`);
      return;
    }

    let content = fs.readFileSync(propsFile, 'utf8');
    const original = content;

    content = content.replace(/^projectVersion=.+$/m, `projectVersion=${newVersion}`);
    content = content.replace(/^contractVersion=.+$/m, `contractVersion=${newVersion}`);

    if (content === original) {
      console.log(`ℹ️  No version change required in ${relativePath}`);
      return;
    }

    fs.writeFileSync(propsFile, content);
    console.log(`✅ Updated ${relativePath} -> projectVersion=${newVersion}, contractVersion=${newVersion}`);
  }

  determineBumpType() {
    try {
      const currentBranch = execSync('git rev-parse --abbrev-ref HEAD', { encoding: 'utf8' }).trim();

      if (currentBranch.includes('feature/') || currentBranch.includes('feat/')) {
        return 'minor';
      } else if (currentBranch.includes('bugfix/') || currentBranch.includes('fix/') || currentBranch.includes('defect/') || currentBranch.includes('hotfix/')) {
        return 'patch';
      } else if (currentBranch.includes('major/') || currentBranch.includes('breaking/')) {
        return 'major';
      }

      const recentCommits = execSync('git log --pretty=format:"%s" -5', { encoding: 'utf8' });

      if (recentCommits.includes('BREAKING CHANGE') || recentCommits.includes('!:')) {
        return 'major';
      } else if (recentCommits.includes('feat:') || recentCommits.includes('feature:')) {
        return 'minor';
      } else if (recentCommits.includes('fix:') || recentCommits.includes('bugfix:') || recentCommits.includes('defect:')) {
        return 'patch';
      }
      return 'patch';
    } catch (error) {
      console.warn('Could not determine bump type automatically, defaulting to patch');
      return 'patch';
    }
  }



  createLocalTag(version) {
    try {
      const tagName = `v${version}`;

      try {
        execSync(`git rev-parse --verify ${tagName}`, { stdio: 'ignore' });
        console.log(`⚠️ Tag ${tagName} already exists, skipping tag creation`);
        return false;
      } catch {
      }

      execSync(`git tag -a "${tagName}" -m "Release version ${version}"`, { stdio: 'inherit' });
      console.log(`🏷️ Created local tag: ${tagName}`);
      return true;
    } catch (error) {
      console.error(`❌ Failed to create tag: ${error.message}`);
      return false;
    }
  }

  bump(bumpType = null) {
    const versionData = this.readVersionFile();
    const currentVersion = versionData.version;

    const actualBumpType = bumpType || this.determineBumpType();
    const newVersion = this.bumpVersion(currentVersion, actualBumpType);

    console.log(`🔄 Bumping version from ${currentVersion} to ${newVersion} (${actualBumpType})`);

    versionData.version = newVersion;
    versionData.lastUpdated = new Date().toISOString();
    versionData.buildNumber = (versionData.buildNumber || 0) + 1;
    this.writeVersionFile(versionData);

    this.updateFrontendVersion(newVersion);
    this.updateGradleProperties(newVersion);
    this.createLocalTag(newVersion);

    console.log(`✅ Version bumped to ${newVersion} successfully!`);
    console.log(`📝 Note: Changes are local only. Tag created locally: v${newVersion}`);

    return {
      oldVersion: currentVersion,
      newVersion: newVersion,
      buildNumber: versionData.buildNumber,
      bumpType: actualBumpType
    };
  }

  getCurrentVersion() {
    const versionData = this.readVersionFile();
    return versionData.version;
  }

  sync() {
    const versionData = this.readVersionFile();
    const version = versionData.version;

    console.log(`🔄 Syncing version ${version} across all components...`);

    this.updateFrontendVersion(version);
    this.updateGradleProperties(version);

    console.log(`✅ Version ${version} synced successfully!`);
  }

  listTags() {
    try {
      const tags = execSync('git tag -l "v*" --sort=-version:refname', { encoding: 'utf8' });
      if (tags.trim()) {
        console.log('📋 Local version tags:');
        tags.trim().split('\n').slice(0, 10).forEach(tag => {
          console.log(`  ${tag}`);
        });
      } else {
        console.log('📋 No version tags found');
      }
    } catch (error) {
      console.error('❌ Failed to list tags:', error.message);
    }
  }
}

// CLI Interface
if (require.main === module) {
  const versionManager = new VersionManager();
  const command = process.argv[2];
  const arg = process.argv[3];

  try {
    switch (command) {
      case 'bump': {
        const result = versionManager.bump(arg);
        console.log(JSON.stringify(result, null, 2));
        break;
      }
      case 'current':
        console.log(versionManager.getCurrentVersion());
        break;
      case 'sync':
        versionManager.sync();
        break;
      case 'tags':
        versionManager.listTags();
        break;
      default:
        console.log(`
Usage: node scripts/version-manager.js <command> [argument]

Commands:
  bump [major|minor|patch|prerelease]  - Bump version (auto-detects if no argument)
  current                              - Show current version
  sync                                 - Sync current version to all components
  tags                                 - List local version tags

Examples:
  node scripts/version-manager.js bump minor
  node scripts/version-manager.js bump
  node scripts/version-manager.js current
  node scripts/version-manager.js sync
  node scripts/version-manager.js tags

Note: This script only creates local tags and does not push to remote repositories.
        `);
        process.exit(1);
    }
  } catch (error) {
    console.error('❌ Error:', error.message);
    process.exit(1);
  }
}

module.exports = VersionManager;