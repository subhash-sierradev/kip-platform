<template>
  <div class="confluence-settings-tab">
    <div class="settings-container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <i class="dx-icon dx-icon-clock loading-icon"></i>
        <p>Loading confluence settings...</p>
      </div>

      <!-- No Data State -->
      <div v-else-if="!integrationData" class="empty-state">
        <i class="dx-icon dx-icon-info empty-icon"></i>
        <h3>No Integration Data Found</h3>
        <p>Integration details are not available.</p>
      </div>

      <!-- Confluence Settings Display -->
      <div v-else class="settings-content-minimal">
        <!-- Confluence Configuration Card -->
        <div class="info-card">
          <div class="card-header">
            <h4 class="card-title">
              <i class="dx-icon dx-icon-globe title-icon"></i>
              Confluence Configuration
            </h4>
          </div>
          <div class="card-body">
            <div class="info-grid">
              <!-- Confluence Space -->
              <div class="info-row">
                <i class="dx-icon dx-icon-box field-icon" aria-hidden="true"></i>
                <span class="label">Space</span>
                <span class="value">{{ spaceDisplay }}</span>
              </div>

              <!-- Space Folder -->
              <div class="info-row">
                <i class="dx-icon dx-icon-activefolder field-icon" aria-hidden="true"></i>
                <span class="label">Folder</span>
                <span class="value">{{ folderDisplay }}</span>
              </div>

              <!-- Report Name Template -->
              <div class="info-row">
                <i class="dx-icon dx-icon-description field-icon" aria-hidden="true"></i>
                <span class="label">Report Name</span>
                <span class="value">{{ integrationData.reportNameTemplate || 'N/A' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Report Settings Card -->
        <div class="info-card">
          <div class="card-header">
            <h4 class="card-title">
              <i class="dx-icon dx-icon-preferences title-icon"></i>
              Report Settings
            </h4>
          </div>
          <div class="card-body">
            <div class="info-grid">
              <!-- Table of Contents -->
              <div class="info-row">
                <span class="label">Table of Contents:</span>
                <span class="value">
                  <span class="status-badge" :class="tocBadgeClass">
                    {{ integrationData.includeTableOfContents ? 'Enabled' : 'Disabled' }}
                  </span>
                </span>
              </div>

              <!-- Languages -->
              <div class="info-row">
                <span class="label">Languages:</span>
                <div class="value">
                  <div v-if="languagesList.length > 0" class="tag-list">
                    <span v-for="lang in languagesList" :key="lang.code" class="tag language-tag">
                      {{ lang.display }}
                    </span>
                  </div>
                  <span v-else class="no-data">No languages configured</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ConfluenceSettingsTab' });
import type { PropType } from 'vue';
import { computed } from 'vue';
import type { ConfluenceIntegrationResponse } from '@/api/models/ConfluenceIntegrationResponse';

const props = defineProps({
  integrationData: {
    type: Object as PropType<ConfluenceIntegrationResponse | null>,
    default: null,
  },
  loading: { type: Boolean, default: false },
});

const spaceDisplay = computed(() => {
  const label = props.integrationData?.confluenceSpaceLabel?.trim();
  if (label) return label;

  const key = props.integrationData?.confluenceSpaceKey;
  if (!key) return 'N/A';
  return key;
});

function isSentinelFolderKey(key: string, spaceKey?: string): boolean {
  return key === 'ROOT' || key === spaceKey;
}

const folderDisplay = computed(() => {
  const label = props.integrationData?.confluenceSpaceFolderLabel?.trim();
  if (label) return label;

  const folderKey = props.integrationData?.confluenceSpaceKeyFolderKey;
  if (!folderKey) return 'Root Folder';
  if (isSentinelFolderKey(folderKey, props.integrationData?.confluenceSpaceKey))
    return 'Root Folder';
  return folderKey;
});

// Languages list with proper display format
const languagesList = computed(() => {
  const langs = props.integrationData?.languages;
  if (langs && langs.length > 0) {
    return langs.map(l => ({
      code: l.code || '',
      display: `${l.name} (${l.nativeName})`,
    }));
  }

  // Fallback to language codes if full language objects not available
  const codes = props.integrationData?.languageCodes;
  if (codes && codes.length > 0) {
    return codes.map(c => ({
      code: c,
      display: c.toUpperCase(),
    }));
  }

  return [];
});

// Badge class for Table of Contents
const tocBadgeClass = computed(() => {
  return props.integrationData?.includeTableOfContents ? 'status-enabled' : 'status-disabled';
});
</script>

<style src="./ConfluenceSettingsTab.css" scoped></style>
