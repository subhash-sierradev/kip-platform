<template>
  <div class="app-version">
    <span class="version-text">KIP v{{ version }}</span>
    <span class="copyright-text">© {{ currentYear }} Kaseware</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

// Use the version from package.json (read at build time)
const version = computed(() => {
  // In development, get version from global or fallback
  try {
    // @ts-ignore - Build-time constant injected by Vite
    return globalThis.__APP_VERSION__ || 'dev';
  } catch {
    return 'dev';
  }
});

const currentYear = computed(() => new Date().getFullYear());
</script>

<script lang="ts">
export default {
  name: 'AppVersion',
};
</script>

<style scoped>
/* Styles are defined in SideNav.css to avoid duplication */
</style>
