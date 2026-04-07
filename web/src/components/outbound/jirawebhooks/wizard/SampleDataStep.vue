<template>
  <div class="sd-root">
    <h2 class="sd-title">Sample Webhook Payload for Jira Field Mapping</h2>

    <div class="sd-layout">
      <!-- LEFT PANEL -->
      <div class="sd-left">
        <!-- Error Banner -->
        <div v-if="errorMessage" class="sd-alert">
          {{ errorMessage }}
        </div>

        <!-- JSON Textarea Container (Matches screenshot EXACTLY) -->
        <div class="sd-textarea-wrapper">
          <textarea
            class="sd-textarea"
            :class="{ 'sd-textarea-error': !!errorMessage }"
            :value="jsonSampleLocal"
            placeholder='Paste your JSON payload here...&#10;&#10;Example:&#10;{&#10;  "form": {&#10;    "title": "Missing Files Case",&#10;    "assigneeEmail": "user@example.com",&#10;    "priority": "High"&#10;  }&#10;}'
            @input="onJsonInput"
          ></textarea>
        </div>
      </div>

      <!-- RIGHT PANEL -->
      <div class="sd-right">
        <!-- Upload JSON -->
        <button type="button" class="sd-btn sd-btn-orange" @click="handleUploadFile">
          <span class="sd-btn-icon">📁</span>
          Upload File
        </button>

        <!-- From Clipboard (NOT ORANGE NOW) -->
        <button type="button" class="sd-btn sd-btn-gray" @click="handlePasteFromClipboard">
          <span class="sd-btn-icon">📋</span>
          From Clipboard
        </button>

        <!-- Format JSON -->
        <button
          type="button"
          class="sd-btn sd-btn-format sd-btn-disabled"
          :disabled="!isJsonValidLocal"
          @click="formatJson"
        >
          <span class="sd-btn-icon">✨</span>
          Format JSON
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';

const props = defineProps<{ jsonSample: string }>();

const emit = defineEmits<{
  (e: 'update:jsonSample', value: string): void;
  (e: 'json-validity-change', valid: boolean): void;
}>();

const jsonSampleLocal = ref(props.jsonSample || '');
const errorMessage = ref('');
const isJsonValidLocal = ref(false);

function validateJson(value: string) {
  const trimmed = (value || '').trim();
  if (!trimmed) {
    errorMessage.value = '';
    isJsonValidLocal.value = false;
    emit('json-validity-change', false);
    return;
  }

  // Detect duplicate keys within the same JSON object block
  const dupInObject = findDuplicateKeyInSingleObject(trimmed);
  if (dupInObject.duplicate) {
    errorMessage.value = `Invalid JSON. Duplicate field found: "${dupInObject.key}"`;
    isJsonValidLocal.value = false;
    emit('json-validity-change', false);
    return;
  }

  try {
    JSON.parse(trimmed);
    errorMessage.value = '';
    isJsonValidLocal.value = true;
    emit('json-validity-change', true);
  } catch {
    errorMessage.value = 'Invalid JSON. Please fix the payload.';
    isJsonValidLocal.value = false;
    emit('json-validity-change', false);
  }
}

function onJsonInput(e: Event) {
  const val = (e.target as HTMLTextAreaElement).value;
  jsonSampleLocal.value = val;
  emit('update:jsonSample', val);
  validateJson(val);
}

onMounted(() => validateJson(jsonSampleLocal.value));

watch(
  () => props.jsonSample,
  val => {
    jsonSampleLocal.value = val || '';
    validateJson(jsonSampleLocal.value);
  }
);

function handleUploadFile() {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = '.json';
  input.onchange = async () => {
    const file = input.files?.[0];
    if (!file) return;
    const text = await file.text();
    // Try to auto-format on first load if JSON is valid
    let formatted = text;
    try {
      formatted = JSON.stringify(JSON.parse(text), null, 2);
    } catch {
      // keep original text if not valid JSON
    }
    jsonSampleLocal.value = formatted;
    emit('update:jsonSample', formatted);
    validateJson(formatted);
  };
  input.click();
}

async function handlePasteFromClipboard() {
  try {
    const text = await navigator.clipboard.readText();
    // Auto-format pasted JSON on first time if valid
    let formatted = text;
    try {
      formatted = JSON.stringify(JSON.parse(text), null, 2);
    } catch {
      // keep original text
    }
    jsonSampleLocal.value = formatted;
    emit('update:jsonSample', formatted);
    validateJson(formatted);
  } catch {
    errorMessage.value = 'Clipboard read failed. Paste manually.';
  }
}

function formatJson() {
  if (!isJsonValidLocal.value) return;
  try {
    const pretty = JSON.stringify(JSON.parse(jsonSampleLocal.value), null, 2);
    jsonSampleLocal.value = pretty;
    emit('update:jsonSample', pretty);
  } catch {
    void 0;
  }
}

// Note: Do not pre-check for duplicate keys using regex.
// JSON validity is determined solely by JSON.parse here.
// However, we do check for duplicate keys within the same object block.
function findDuplicateKeyInSingleObject(jsonStr: string): { duplicate: boolean; key?: string } {
  // Lightweight scanner that tracks object nesting and string states
  let inString = false;
  let escape = false;
  let objectDepth = 0;
  const keySets: Array<Set<string>> = [];
  let i = 0;

  const readString = (): string => {
    // assumes the current char is the opening quote
    let result = '';
    i++; // skip opening quote
    inString = true;
    escape = false;
    for (; i < jsonStr.length; i++) {
      const ch = jsonStr[i];
      if (escape) {
        result += ch;
        escape = false;
        continue;
      }
      if (ch === '\\') {
        // backslash
        escape = true;
        result += ch;
        continue;
      }
      if (ch === '"') {
        inString = false;
        i++; // move past closing quote
        break;
      }
      result += ch;
    }
    return result;
  };

  const skipWhitespace = () => {
    while (i < jsonStr.length && /\s/.test(jsonStr[i])) i++;
  };

  while (i < jsonStr.length) {
    const ch = jsonStr[i];
    if (!inString) {
      if (ch === '{') {
        objectDepth++;
        keySets.push(new Set<string>());
        i++;
        continue;
      }
      if (ch === '}') {
        objectDepth = Math.max(0, objectDepth - 1);
        keySets.pop();
        i++;
        continue;
      }
      if (ch === '"') {
        // potential key only when inside an object and followed by ':' at the same level
        const key = readString();
        skipWhitespace();
        if (jsonStr[i] === ':') {
          // It's a key-value pair
          if (objectDepth > 0) {
            const set = keySets[keySets.length - 1];
            if (set.has(key)) {
              return { duplicate: true, key };
            }
            set.add(key);
          }
          i++; // skip ':'
        }
        continue;
      }
      // other structural characters: [, ], , etc.
      i++;
      continue;
    } else {
      // inside a string, handled by readString; shouldn't get here often
      i++;
    }
  }

  return { duplicate: false };
}
</script>

<style scoped>
/* ROOT LAYOUT */
.sd-root {
  display: flex;
  flex-direction: column;
  margin-left: 50px;
  margin-right: 50px;
  gap: 18px;
}

/* TITLE */
.sd-title {
  text-align: left;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

/* GRID */
.sd-layout {
  display: grid;
  grid-template-columns: 2fr 200px;
  gap: 20px;
}

/* LEFT PANEL */
.sd-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* TEXTAREA WRAPPER */
.sd-textarea-wrapper {
  background: #f8f9fa;
  border: 2px solid #d1d5db;
  border-radius: 6px;
  padding: 10px;
  min-height: 300px;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.04);
}

/* TEXTAREA */
.sd-textarea {
  width: 100%;
  height: 100%;
  border: none;
  resize: none;
  outline: none;
  font-size: 13px;
  color: #374151;
  background: transparent;
  font-family: monospace;
}

/* PLACEHOLDER STYLING */
.sd-textarea::placeholder {
  color: var(--kw-placeholder-color) !important;
  font-style: italic;
  opacity: 0.7;
  font-size: 12px;
  line-height: 1.4;
}

.sd-textarea::-webkit-input-placeholder {
  color: var(--kw-placeholder-color) !important;
  font-style: italic;
  opacity: 0.7;
  font-size: 12px;
  line-height: 1.4;
}

.sd-textarea::-moz-placeholder {
  color: var(--kw-placeholder-color) !important;
  font-style: italic;
  opacity: 0.7;
  font-size: 12px;
  line-height: 1.4;
}

.sd-textarea:-ms-input-placeholder {
  color: var(--kw-placeholder-color) !important;
  font-style: italic;
  opacity: 0.7;
  font-size: 12px;
  line-height: 1.4;
}

/* INVALID */
.sd-textarea-error {
  border-color: #dc2626 !important;
}

/* RIGHT PANEL */
.sd-right {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* BUTTON BASE */
.sd-btn {
  width: 100%;
  height: 42px;
  color: white;
  border-radius: 6px;
  border: 1px solid #f59e0b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 12px;
}

.sd-btn-icon {
  font-size: 14px;
  margin-right: 2px;
}

/* ORANGE BUTTON */
.sd-btn-orange {
  background: #f59e0b;
  color: white;
}
.sd-btn-orange:hover {
  background: #d97706;
}

/* NEW — distinct style for Format JSON button only */
.sd-btn-format {
  background: #ffffff; /* emerald */
  color: rgb(0, 0, 0);
}
.sd-btn-format:hover {
  background: #f9f8f2; /* teal-ish hover */
}

.sd-alert {
  color: #dc2626;
  font-size: 13px;
  margin-top: 4px;
}

/* NEW — GRAY BUTTON (for Clipboard) */
.sd-btn-gray {
  background: #ffffff;
  color: #f59e0b;
  font-weight: 600;
  border: 1px solid #f59e0b;
}
.sd-btn-gray:hover {
  background: #f9f8f2;
  color: #f59e0b;
}

/* DISABLED */
.sd-btn-disabled:disabled {
  background: #f3f4f6;
  color: var(--kw-placeholder-color);
  border-color: #e5e7eb;
  cursor: default;
}
.sd-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}
.sd-modal {
  background: #fff;
  border-radius: 8px;
  padding: 16px 18px;
  width: 520px;
  box-shadow: 0 10px 25px rgba(15, 23, 42, 0.25);
}
.sd-modal-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #111827;
}
.sd-modal-textarea {
  width: 100%;
  min-height: 200px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  padding: 10px;
  font-family: monospace;
  font-size: 13px;
}
.sd-modal-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
