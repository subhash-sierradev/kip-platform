<template>
  <div class="app-version">
    <div class="version-row">
      <span class="version-label">Web</span>
      <span class="version-value">{{ version }}</span>
      <span class="version-sep">·</span>
      <span class="version-label">IMS</span>
      <span class="version-value">{{ imsVersion }}</span>
      <span class="version-sep">·</span>
      <span class="version-label">IES</span>
      <span class="version-value">{{ iesVersion }}</span>
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
.version-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  font-size: 10px;
  font-family: 'Segoe UI', system-ui, sans-serif;
  line-height: 1.4;
}

.version-label {
  color: rgba(255, 255, 255, 0.4);
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.version-value {
  color: rgba(255, 255, 255, 0.6);
  font-variant-numeric: tabular-nums;
}

.version-sep {
  color: rgba(255, 255, 255, 0.2);
}

.copyright-text {
  display: block;
  margin-top: 0.25rem;
  font-size: 9px;
  color: rgba(255, 255, 255, 0.3);
  letter-spacing: 0.2px;
  text-align: center;
}
</style>
