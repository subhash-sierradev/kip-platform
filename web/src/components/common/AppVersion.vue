<template>
  <div class="app-version">
    <div class="version-stack">
      <div class="version-badge">
        <span class="vb-label">Web</span>
        <span class="vb-val">{{ version }}</span>
      </div>
      <div class="version-badge">
        <span class="vb-label">IMS</span>
        <span class="vb-val">{{ imsVersion }}</span>
      </div>
      <div class="version-badge">
        <span class="vb-label">IES</span>
        <span class="vb-val">{{ iesVersion }}</span>
      </div>
    </div>
    <span class="copyright-text">© {{ currentYear }} Kaseware Inc.</span>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { getApiBaseUrl } from '@/config/env';
import { getToken } from '@/config/keycloak';

const imsVersion = ref('...');
const iesVersion = ref('...');

const version = computed(() => {
  try {
    // @ts-ignore - Build-time constant injected by Vite
    return globalThis.__APP_VERSION__ || 'dev';
  } catch {
    return 'dev';
  }
});

const currentYear = computed(() => new Date().getFullYear());

onMounted(async () => {
  try {
    const token = getToken();
    const response = await fetch(`${getApiBaseUrl()}/management/version`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (response.ok) {
      const data = await response.json();
      imsVersion.value = data?.ims?.version ?? 'unknown';
      iesVersion.value = data?.ies?.version ?? 'unknown';
    }
  } catch {
    imsVersion.value = 'unavailable';
    iesVersion.value = 'unavailable';
  }
});
</script>

<script lang="ts">
export default {
  name: 'AppVersion',
};
</script>

<style scoped>
.app-version {
  width: 100%;
}

.version-stack {
  display: flex;
  flex-direction: column;
  gap: 3px;
  font-size: 10px;
  font-family: 'Segoe UI', system-ui, sans-serif;
  line-height: 1.4;
}

.version-badge {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.vb-label {
  color: rgba(255, 255, 255, 0.35);
  text-transform: uppercase;
  letter-spacing: 0.4px;
  flex-shrink: 0;
}

.vb-val {
  color: rgba(255, 255, 255, 0.65);
  font-variant-numeric: tabular-nums;
  font-family: 'SF Mono', 'Consolas', 'Courier New', monospace;
  font-size: 9.5px;
}

.copyright-text {
  display: block;
  margin-top: 6px;
  font-size: 9px;
  color: rgba(255, 255, 255, 0.25);
  letter-spacing: 0.2px;
  text-align: center;
}
</style>
