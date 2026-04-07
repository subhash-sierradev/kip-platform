<template>
  <div class="modern-app-shell" :class="{ 'sidebar-collapsed': sidebarStore.isCollapsed }">
    <!-- Top Navigation Bar -->
    <TopBar :collapsed="sidebarStore.isCollapsed" @toggle="toggleNav" />

    <div class="app-body">
      <!-- Sidebar Navigation -->
      <SideNav
        :sections="navigationSections"
        :collapsed="sidebarStore.isCollapsed"
        class="app-sidebar"
      />

      <!-- Main Content -->
      <main class="main-content">
        <div class="content-container">
          <!-- Breadcrumb -->
          <nav class="breadcrumb-nav" aria-label="Breadcrumb">
            <div class="breadcrumb-container">
              <span
                v-for="(item, index) in breadcrumbItems"
                :key="item.text + index"
                class="breadcrumb-item"
                :class="{ active: index === breadcrumbItems.length - 1 }"
              >
                <span class="breadcrumb-text">{{ item.text }}</span>
                <span v-if="index < breadcrumbItems.length - 1" class="breadcrumb-separator">
                  ›
                </span>
              </span>
            </div>
          </nav>

          <!-- Page Content -->
          <div
            class="page-content"
            :class="{ 'no-page-padding': route.meta?.disableShellPadding === true }"
          >
            <slot />
          </div>
        </div>
      </main>
    </div>

    <!-- Mobile Overlay -->
    <div v-if="!sidebarStore.isCollapsed && isMobile" class="mobile-overlay" @click="toggleNav" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, provide } from 'vue';
import { useRoute } from 'vue-router';
import TopBar from './TopBar.vue';
import SideNav from './SideNav.vue';
import type { NavSection, NavLeaf } from '../../types/navigation';
import { useNotifications } from '@/composables/useNotifications';

useNotifications();

defineOptions({ name: 'AppShell' });

/* ------------------------------------------------------------------
 * Breadcrumb title injection (child → shell)
 * ------------------------------------------------------------------ */
const dynamicBreadcrumbTitle = ref<string | null>(null);

provide('setBreadcrumbTitle', (title: string | null) => {
  dynamicBreadcrumbTitle.value = title;
});

/* ------------------------------------------------------------------
 * Navigation configuration
 * ------------------------------------------------------------------ */
const navigationSections: NavSection[] = [
  { label: 'Home', route: '/', icon: 'home' },
  { label: 'Inbound', route: '/inbound/integrations', icon: 'import' },
  {
    label: 'Outbound',
    icon: 'export',
    route: '',
    children: [
      { label: 'ArcGIS Integration', route: '/outbound/integration/arcgis', icon: 'activefolder' },
      {
        label: 'Confluence Integration',
        route: '/outbound/integration/confluence',
        icon: 'activefolder',
      },
      { label: 'Jira Webhook', route: '/outbound/webhook/jira', icon: 'activefolder' },
    ],
  },
  {
    label: 'Admin',
    icon: 'user',
    route: '',
    children: [
      { label: 'ArcGIS Connect', route: '/admin/connections/arcgis', icon: 'link' },
      { label: 'Confluence Connect', route: '/admin/connections/confluence', icon: 'link' },
      { label: 'Jira Connect', route: '/admin/connections/jira', icon: 'link' },
      { label: 'Audit Log', route: '/admin/audit-log', icon: 'undo' },
      { label: 'Clear Cache', route: '/admin/clear-cache', icon: 'refresh' },
      { label: 'Cache Statistics', route: '/admin/cache-statistics', icon: 'statistics' },
      { label: 'Notifications', route: '/admin/notifications', icon: 'bell' },
      { label: 'Site Config', route: '/admin/site-config', icon: 'preferences' },
    ],
  },
];

/* ------------------------------------------------------------------
 * Layout state
 * ------------------------------------------------------------------ */
import { useSidebarStore } from '@/store/sidebar';
const sidebarStore = useSidebarStore();

const isMobile = ref(false);
const route = useRoute();

const toggleNav = () => {
  sidebarStore.toggleCollapse();
};

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768;
};

/* ------------------------------------------------------------------
 * Breadcrumb configuration (RULE BASED)
 * ------------------------------------------------------------------ */
type BreadcrumbItem = { text: string; url: string };

interface BreadcrumbRule {
  match: (path: string) => boolean;
  build: (path: string, title?: string | null) => BreadcrumbItem[];
}

const breadcrumbRules: BreadcrumbRule[] = [
  {
    match: p => p === '/inbound/integrations',
    build: () => [
      { text: 'Home', url: '/' },
      { text: 'Inbound', url: '#' },
      { text: 'Integrations', url: '/inbound/integrations' },
    ],
  },
  {
    match: p => p.startsWith('/outbound/webhook/jira'),
    build: (path, title) => [
      { text: 'Home', url: '/' },
      { text: 'Outbound', url: '#' },
      { text: 'Jira Webhook', url: '/outbound/webhook/jira' },
      ...(path !== '/outbound/webhook/jira' ? [{ text: title || 'Jira Details', url: path }] : []),
    ],
  },
  {
    match: p => p.startsWith('/outbound/integration/arcgis'),
    build: (path, title) => [
      { text: 'Home', url: '/' },
      { text: 'Outbound', url: '#' },
      { text: 'ArcGIS Integration', url: '/outbound/integration/arcgis' },
      ...(path !== '/outbound/integration/arcgis'
        ? [{ text: title || 'ArcGIS Details', url: path }]
        : []),
    ],
  },
  {
    match: p => p.startsWith('/outbound/integration/confluence'),
    build: (path, title) => [
      { text: 'Home', url: '/' },
      { text: 'Outbound', url: '#' },
      { text: 'Confluence Integration', url: '/outbound/integration/confluence' },
      ...(path !== '/outbound/integration/confluence'
        ? [{ text: title || 'Confluence Details', url: path }]
        : []),
    ],
  },
  {
    match: p => p === '/admin/notifications',
    build: () => [
      { text: 'Home', url: '/' },
      { text: 'Admin', url: '#' },
      { text: 'Notifications', url: '/admin/notifications' },
    ],
  },
  {
    match: p => p === '/notifications',
    build: () => [
      { text: 'Home', url: '/' },
      { text: 'Notifications', url: '/notifications' },
    ],
  },
  {
    match: p => p === '/admin/connections/jira',
    build: () => [
      { text: 'Home', url: '/' },
      { text: 'Admin', url: '#' },
      { text: 'Jira Connect', url: '/admin/connections/jira' },
    ],
  },
  {
    match: p => p === '/admin/connections/confluence',
    build: () => [
      { text: 'Home', url: '/' },
      { text: 'Admin', url: '#' },
      { text: 'Confluence Connect', url: '/admin/connections/confluence' },
    ],
  },
  {
    match: p => p === '/admin/connections/arcgis',
    build: () => [
      { text: 'Home', url: '/' },
      { text: 'Admin', url: '#' },
      { text: 'ArcGIS Connect', url: '/admin/connections/arcgis' },
    ],
  },
];

/* ------------------------------------------------------------------
 * Breadcrumb computation
 * ------------------------------------------------------------------ */
function computeBreadcrumbItems(path: string, dynamicTitle?: string | null): BreadcrumbItem[] {
  const rule = breadcrumbRules.find(r => r.match(path));
  if (rule) {
    return rule.build(path, dynamicTitle);
  }
  return buildFromNavigation(path);
}

function buildFromNavigation(path: string): BreadcrumbItem[] {
  const items: BreadcrumbItem[] = [{ text: 'Home', url: '/' }];
  const segments = path.split('/').filter(Boolean);

  let currentPath = '';
  for (const segment of segments) {
    currentPath += `/${segment}`;
    const section = findSectionByRoute(currentPath);
    if (!section) continue;

    const parent = findParentByChildRoute(currentPath);
    if (parent && !items.some(i => i.text === parent.label)) {
      items.push({ text: parent.label, url: parent.route || '#' });
    }

    items.push({ text: section.label, url: currentPath });
  }

  return items;
}

const breadcrumbItems = computed(() =>
  computeBreadcrumbItems(route.path, dynamicBreadcrumbTitle.value)
);

/* ------------------------------------------------------------------
 * Navigation helpers
 * ------------------------------------------------------------------ */
function findSectionByRoute(routePath: string): NavSection | NavLeaf | null {
  for (const section of navigationSections) {
    if (section.route === routePath) return section;
    if (section.children) {
      const child = section.children.find(c => c.route === routePath);
      if (child) return child;
    }
  }
  return null;
}

function findParentByChildRoute(childRoute: string): NavSection | null {
  return navigationSections.find(s => s.children?.some(c => c.route === childRoute)) || null;
}

/* ------------------------------------------------------------------
 * Lifecycle
 * ------------------------------------------------------------------ */
onMounted(() => {
  checkMobile();
  window.addEventListener('resize', checkMobile);
});

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile);
});
</script>

<style scoped>
@import './layout.css';

.modern-app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #181818;
  overflow: hidden;
}

.app-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.app-sidebar {
  flex-shrink: 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(180deg, #e5e5e5 0%, #f5f5f5 100%);
  border-radius: 12px 0 0 0;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  margin: 0 0 0.25rem 0;
}

.content-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Breadcrumb styles are now handled in layout.css for consistency */

.page-content {
  flex: 1;
  overflow: auto;
  padding: 0.75rem;
  background: transparent;
}

.page-content.no-page-padding {
  padding: 0;
}

/* Mobile overlay */
.mobile-overlay {
  position: fixed;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 90;
  transition: opacity 0.3s ease;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .main-content {
    margin: 0;
    border-radius: 0;
  }

  .page-content {
    padding: 0.5rem;
  }

  .breadcrumb-nav {
    padding: 0.75rem 1rem 0.5rem;
  }
}

/* Sidebar collapsed state adjustments */
.modern-app-shell.sidebar-collapsed .main-content {
  margin-left: 0;
}

/* Custom scrollbar for content */
.page-content::-webkit-scrollbar {
  width: 6px;
}

.page-content::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}

.page-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.page-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
