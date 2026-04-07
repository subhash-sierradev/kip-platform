<template>
  <section class="ms-left">
    <div class="ms-left-card">
      <div class="ms-left-header">
        <h2 class="ms-left-title">Incoming JSON</h2>
        <p class="ms-left-subtitle">Sample webhook payload for field mapping reference</p>
      </div>

      <div class="ms-json-container">
        <div v-if="jsonSampleTrimmed" class="ms-json">
          <div v-for="(line, idx) in jsonLines" :key="idx" class="ms-json-line">
            <template v-if="line.type === 'prop'">
              <span class="ms-json-indent">{{ line.indent }}</span>
              <strong class="ms-json-key">"{{ line.key }}"</strong>
              <span>{{ line.colon }}{{ line.value }}</span>
            </template>
            <template v-else>
              <span>{{ line.text }}</span>
            </template>
          </div>
        </div>
        <div v-else class="ms-json-empty">
          No sample JSON provided. Paste a sample payload in the previous step.
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ jsonSample: string }>();

const jsonSampleTrimmed = computed(() => (props.jsonSample || '').trim());

type JsonLineProp = { type: 'prop'; indent: string; key: string; colon: string; value: string };
type JsonLineText = { type: 'text'; text: string };

const jsonLines = computed<(JsonLineProp | JsonLineText)[]>(() => {
  const raw = jsonSampleTrimmed.value;
  if (!raw) return [];
  let pretty = raw;
  try {
    pretty = JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    // keep as-is if not valid JSON
  }
  return pretty.split('\n').map(line => {
    const m = line.match(/^(\s*)"([^"]+)"(\s*:\s*)(.*)$/);
    if (m) {
      return { type: 'prop', indent: m[1], key: m[2], colon: m[3], value: m[4] } as JsonLineProp;
    }
    return { type: 'text', text: line } as JsonLineText;
  });
});
</script>
<style src="./MappingStep.css" scoped></style>
