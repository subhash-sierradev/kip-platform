import { defineStore } from 'pinia';
import { computed, ref } from 'vue';

export const useSidebarStore = defineStore('sidebar', () => {
  const isCollapsed = ref(false);
  const isHoverExpanded = ref(false);

  /**
   * Computed effective width of the sidebar
   * Returns 200px when expanded, 90px when collapsed (unless hover-expanded)
   */
  const effectiveWidth = computed(() => {
    return isCollapsed.value && !isHoverExpanded.value ? 90 : 200;
  });

  /**
   * Toggle sidebar collapsed state
   */
  function toggleCollapse() {
    isCollapsed.value = !isCollapsed.value;
  }

  /**
   * Set hover expansion state (temporary expansion on mouse hover)
   */
  function setHoverExpanded(value: boolean) {
    isHoverExpanded.value = value;
  }

  return {
    isCollapsed,
    isHoverExpanded,
    effectiveWidth,
    toggleCollapse,
    setHoverExpanded,
  };
});
