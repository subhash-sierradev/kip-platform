<template>
  <div class="cc-root">
    <div class="cc-container">
      <!-- ── Content Configuration ─────────────────────────────── -->

      <!-- REPORT NAME TEMPLATE -->
      <div class="cc-field">
        <label class="cc-label"> Report Name Template <span class="cc-required">*</span> </label>
        <input
          class="cc-input"
          :class="{ 'cc-input-error': !localData.reportNameTemplate.trim() && hasInteracted }"
          v-model="localData.reportNameTemplate"
          type="text"
          placeholder="e.g. Aggregated Daily Report - {date}"
          @input="hasInteracted = true"
        />
        <div class="cc-helper">
          Use <code class="cc-code">{date}</code> as a placeholder — it will be replaced with the
          execution date (e.g., <em>{{ previewDate }}</em
          >).
        </div>
        <div v-if="reportNamePreview" class="cc-preview">
          Preview: <strong>{{ reportNamePreview }}</strong>
        </div>
      </div>

      <!-- LANGUAGES -->
      <div class="cc-field">
        <label class="cc-label"> Languages <span class="cc-required">*</span> </label>
        <DxTagBox
          v-model:value="localData.languageCodes"
          :data-source="languageItems"
          display-expr="label"
          value-expr="code"
          :search-enabled="true"
          :show-selection-controls="true"
          :max-display-tags="6"
          styling-mode="outlined"
          placeholder="Select languages..."
          :disabled="langsApi.loading.value"
        />
        <div v-if="langsApi.loading.value" class="cc-helper">Loading languages…</div>
        <div v-if="langsApi.hasError.value" class="cc-error-row">
          <span class="cc-error-text">{{ langsApi.error.value }}</span>
          <button type="button" class="cc-retry-btn" @click="loadLanguages">Retry</button>
        </div>
        <div v-else class="cc-helper">
          Select one or more output languages for the Confluence page.
        </div>
      </div>

      <!-- TABLE OF CONTENTS -->
      <div class="cc-field cc-toc-row">
        <label class="cc-label">Include Table of Contents</label>
        <div class="cc-toc-toggle">
          <DxSwitch
            :value="localData.includeTableOfContents"
            @value-changed="localData.includeTableOfContents = $event.value"
          />
          <span class="cc-toc-label">
            {{ localData.includeTableOfContents ? 'Enabled' : 'Disabled' }}
          </span>
        </div>
        <div class="cc-helper">
          Automatically prepend a table of contents to the generated page.
        </div>
      </div>

      <!-- ── Publishing Destination ────────────────────────────── -->
      <div class="cc-section-divider">Publishing Destination</div>

      <!-- SPACE KEY -->
      <div class="cc-field">
        <label class="cc-label"> Confluence Space <span class="cc-required">*</span> </label>

        <DxSelectBox
          v-model:value="localData.confluenceSpaceKey"
          :data-source="spaceOptions"
          display-expr="displayText"
          value-expr="key"
          :search-enabled="true"
          :show-clear-button="true"
          styling-mode="outlined"
          placeholder="Select a space..."
          :disabled="!connectionId || spacesApi.loading.value"
          :class="{ 'cc-select-error': spaceKeyError }"
          @value-changed="validateSpaceKey"
        />
        <div v-if="!connectionId" class="cc-helper cc-helper-muted">
          Complete the Confluence connection step to load spaces.
        </div>
        <div v-else-if="spacesApi.loading.value" class="cc-helper">Loading spaces…</div>
        <div v-else-if="spacesApi.hasError.value" class="cc-error-row">
          <span class="cc-error-text">{{ spacesApi.error.value }}</span>
          <button type="button" class="cc-retry-btn" @click="loadSpaces">Retry</button>
        </div>
        <div v-else-if="spaceKeyError" class="cc-error-text">{{ spaceKeyError }}</div>
        <div v-else class="cc-helper">
          Select the Confluence space where pages will be published.
        </div>
      </div>

      <!-- TARGET FOLDER (optional) -->
      <div class="cc-field">
        <label class="cc-label">
          Space Folder
          <span class="cc-optional">(Optional)</span>
        </label>

        <DxSelectBox
          :value="localData.confluenceSpaceKeyFolderKey || ''"
          :data-source="pageOptions"
          display-expr="displayText"
          value-expr="id"
          :search-enabled="true"
          :show-clear-button="true"
          styling-mode="outlined"
          placeholder="Select a folder..."
          :disabled="!connectionId || !localData.confluenceSpaceKey || pagesApi.loading.value"
          @value-changed="onFolderChanged"
        />
        <div v-if="!connectionId" class="cc-helper cc-helper-muted">
          Complete the Confluence connection step to load folders.
        </div>
        <div v-else-if="!localData.confluenceSpaceKey" class="cc-helper cc-helper-muted">
          Select a space above to choose a target folder.
        </div>
        <div v-else-if="pagesApi.loading.value" class="cc-helper">Loading folders…</div>
        <div v-else-if="pagesApi.hasError.value" class="cc-error-row">
          <span class="cc-error-text">{{ pagesApi.error.value }}</span>
          <button
            type="button"
            class="cc-retry-btn"
            @click="loadPages(localData.confluenceSpaceKey)"
          >
            Retry
          </button>
        </div>
        <div v-else class="cc-helper">
          Select the folder where reports will be created. Leave empty to publish directly in the
          space.
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue';
import { DxTagBox } from 'devextreme-vue/tag-box';
import { DxSwitch } from 'devextreme-vue/switch';
import { DxSelectBox } from 'devextreme-vue/select-box';
import { MasterDataService } from '@/api/services/MasterDataService';
import {
  ConfluenceIntegrationService,
  type ConfluenceSpaceDto,
  type ConfluencePageDto,
} from '@/api/services/ConfluenceIntegrationService';
import { useApiData } from '@/composables/useApiData';
import type { LanguageDto } from '@/api/models/LanguageDto';
import type { ConfluencePageConfigData } from '@/types/ConfluenceFormData';

defineOptions({ name: 'ConfluenceConfigStep' });

interface Props {
  modelValue: ConfluencePageConfigData;
  connectionId?: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'update:modelValue', value: ConfluencePageConfigData): void;
  (e: 'validation-change', isValid: boolean): void;
}>();

const localData = ref<ConfluencePageConfigData>({ ...props.modelValue });
const hasInteracted = ref(false);

const spaceKeyError = ref('');

const langsApi = useApiData<LanguageDto>();
const spacesApi = useApiData<ConfluenceSpaceDto>();
const pagesApi = useApiData<ConfluencePageDto>();

interface SpaceOption {
  key: string;
  displayText: string;
}

interface PageOption {
  id: string;
  displayText: string;
}

const spaceOptions = computed<SpaceOption[]>(() =>
  spacesApi.data.value.map(s => ({
    key: s.key,
    displayText: `${s.name} (${s.key})`,
  }))
);

const pageOptions = computed<PageOption[]>(() => [
  { id: '', displayText: '— Publish at space root —' },
  ...pagesApi.data.value.map(p => ({
    id: p.id,
    displayText: '📂 ' + (p.parentTitle ? `${p.parentTitle} > ${p.title}` : p.title),
  })),
]);

const languageItems = computed(() =>
  langsApi.data.value.map(l => ({
    code: l.code,
    label: `${l.name} (${l.nativeName})`,
  }))
);

const previewDate = computed(() => {
  const now = new Date();
  return `${now.getFullYear()}/${String(now.getMonth() + 1).padStart(2, '0')}/${String(now.getDate()).padStart(2, '0')}`;
});

const reportNamePreview = computed(() => {
  const template = (localData.value.reportNameTemplate || '').trim();
  if (!template) return '';
  return template.replace(/\{date\}/g, previewDate.value);
});

const loadLanguages = async () => {
  try {
    langsApi.setLoading(true);
    const result = await MasterDataService.getAllActiveLanguages();
    langsApi.setData(result);
  } catch (err: any) {
    langsApi.setError(err?.message || err?.body?.message || 'Failed to load languages');
  } finally {
    langsApi.setLoading(false);
  }
};

const loadSpaces = async () => {
  if (!props.connectionId) return;
  try {
    spacesApi.setLoading(true);
    const result = await ConfluenceIntegrationService.getSpacesByConnectionId(props.connectionId);
    spacesApi.setData(result);
    // Resolve display label for any pre-existing space key (e.g. edit/clone mode)
    // Treat label === key as "unresolved" and always update if a matching space is found
    const existingKey = localData.value.confluenceSpaceKey;
    if (existingKey) {
      const found = spaceOptions.value.find(o => o.key === existingKey);
      if (found) localData.value.confluenceSpaceLabel = found.displayText;
    }
  } catch (err: any) {
    spacesApi.setError(err?.message || err?.body?.message || 'Failed to load spaces');
  } finally {
    spacesApi.setLoading(false);
  }
};

const loadPages = async (spaceKey: string) => {
  if (!props.connectionId || !spaceKey) {
    pagesApi.setData([]);
    return;
  }
  try {
    pagesApi.setLoading(true);
    const result = await ConfluenceIntegrationService.getPagesByConnectionIdAndSpace(
      props.connectionId,
      spaceKey
    );
    pagesApi.setData(result);
    // Resolve display label for any pre-existing folder key (e.g. edit/clone mode)
    // Always update if a matching page is found, treating title === label as "unresolved"
    const existingFolder = localData.value.confluenceSpaceKeyFolderKey;
    if (existingFolder) {
      resolvePrefixFolderLabel(existingFolder, spaceKey);
    }
  } catch (err: any) {
    pagesApi.setError(err?.message || err?.body?.message || 'Failed to load folders');
  } finally {
    pagesApi.setLoading(false);
  }
};

function validateSpaceKey() {
  const val = (localData.value.confluenceSpaceKey || '').trim();
  if (!val) {
    spaceKeyError.value = 'Space Key is required.';
    return;
  }
  spaceKeyError.value = '';
}

function isSpaceRootFolder(folderKey: string, spaceKey: string) {
  return !folderKey || folderKey === 'ROOT' || folderKey === spaceKey;
}

function resolvePrefixFolderLabel(existingFolder: string, spaceKey: string) {
  if (isSpaceRootFolder(existingFolder, spaceKey)) {
    localData.value.confluenceSpaceKeyFolderKey = 'ROOT';
    localData.value.confluenceSpaceFolderLabel = '';
  } else {
    const found = pageOptions.value.find(o => o.id === existingFolder);
    if (found) {
      localData.value.confluenceSpaceFolderLabel = found.displayText.replace('\uD83D\uDCC2 ', '');
    }
  }
}

function onFolderChanged(e: any) {
  const newValue = e.value || 'ROOT';
  localData.value.confluenceSpaceKeyFolderKey = newValue;

  if (isSpaceRootFolder(newValue, localData.value.confluenceSpaceKey || '')) {
    localData.value.confluenceSpaceFolderLabel = '';
    return;
  }

  const found = pageOptions.value.find((o: PageOption) => o.id === newValue);
  localData.value.confluenceSpaceFolderLabel = found
    ? found.displayText.replace('\uD83D\uDCC2 ', '')
    : newValue;
}

const isValid = computed(() => {
  const key = (localData.value.confluenceSpaceKey || '').trim();
  const hasLangs = (localData.value.languageCodes || []).length > 0;
  const hasTemplate = (localData.value.reportNameTemplate || '').trim().length > 0;
  return !!key && hasLangs && hasTemplate;
});

// Must be declared BEFORE the deep localData watcher so it runs first in Vue's flush queue.
// If declared after, the deep watcher emits stale label data (label not yet set) and the
// props.modelValue watcher resets localData, losing the label update entirely.
watch(
  () => localData.value.confluenceSpaceKey,
  (newKey, oldKey) => {
    if (newKey !== oldKey) {
      localData.value.confluenceSpaceKeyFolderKey = 'ROOT';
      localData.value.confluenceSpaceFolderLabel = '';
      if (newKey) {
        const found = spaceOptions.value.find(o => o.key === newKey);
        localData.value.confluenceSpaceLabel = found?.displayText || newKey;
        loadPages(newKey);
      } else {
        localData.value.confluenceSpaceLabel = '';
        pagesApi.setData([]);
      }
    }
  }
);

watch(localData, v => emit('update:modelValue', { ...v }), { deep: true });
watch(isValid, v => emit('validation-change', v), { immediate: true });

watch(
  () => props.modelValue,
  newVal => {
    if (JSON.stringify(newVal) !== JSON.stringify(localData.value)) {
      localData.value = { ...newVal };
    }
  }
);

watch(
  () => props.connectionId,
  newId => {
    if (newId) loadSpaces();
  }
);

onMounted(() => {
  loadLanguages();
  if (props.connectionId) {
    loadSpaces();
    if (localData.value.confluenceSpaceKey) loadPages(localData.value.confluenceSpaceKey);
  }
});
</script>

<style scoped>
.cc-root {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 10px;
}

.cc-container {
  width: 100%;
  max-width: 600px;
}

.cc-section-divider {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 8px;
  margin-bottom: 20px;
}

.cc-field {
  margin-bottom: 24px;
}

.cc-label {
  font-size: 15px;
  color: #000;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.cc-required {
  color: #dc2626;
}

.cc-optional {
  font-size: 12px;
  color: #6b7280;
  font-weight: 400;
}

.cc-input {
  width: 100%;
  height: 38px;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background-color: #fff;
  box-sizing: border-box;
}

.cc-input:focus {
  border-color: #2563eb;
  outline: none;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.2);
}

.cc-input-error {
  border-color: #dc2626;
}

.cc-select-error :deep(.dx-texteditor) {
  border-color: #dc2626;
}

.cc-error-text {
  margin-top: 4px;
  font-size: 12px;
  color: #dc2626;
}

.cc-helper {
  margin-top: 4px;
  font-size: 11px;
  color: #6b7280;
}

.cc-helper-muted {
  font-style: italic;
  color: #9ca3af;
}

.cc-code {
  background: #f3f4f6;
  border-radius: 3px;
  padding: 1px 4px;
  font-family: monospace;
}

.cc-error-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.cc-retry-btn {
  font-size: 12px;
  color: #2563eb;
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
}

.cc-preview {
  margin-top: 6px;
  font-size: 12px;
  color: #374151;
}

.cc-toc-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cc-toc-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cc-toc-label {
  font-size: 14px;
  color: #374151;
}

/* Orange switch — ON */
.cc-toc-toggle :deep(.dx-switch.dx-switch-on-value .dx-switch-handle)::before {
  background-color: #f59e0b !important;
}

/* Dark switch — OFF */
.cc-toc-toggle :deep(.dx-switch:not(.dx-switch-on-value) .dx-switch-handle)::before {
  background-color: #2c3e50 !important;
}

@media (max-width: 600px) {
  .cc-container {
    padding: 0 8px;
  }
}
</style>
