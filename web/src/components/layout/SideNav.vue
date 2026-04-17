<template>
  <aside
    class="kaseware-sidebar"
    :class="{ collapsed: isSidebarCollapsed }"
    @mouseenter="handleSidebarMouseEnter"
    @mouseleave="handleSidebarMouseLeave"
  >
    <!-- Navigation Menu -->

    <nav class="sidebar-nav">
      <div class="nav-group">
        <!-- Top divider for proper spacing -->
        <div
          v-if="canShowInbound || canShowOutbound || canShowAdmin"
          class="sidebar-menu-divider"
        ></div>

        <!-- Inbound -->
        <div
          v-if="canShowInbound"
          class="nav-item"
          :data-menu-key="'inbound'"
          :class="[
            { active: currentRoute.startsWith('/inbound') },
            showInboundSubmenu && !collapsed ? 'nav-item-flyout-open' : '',
          ]"
          @click="onParentClick('inbound')"
          @mouseenter="onMenuMouseEnter('inbound', $event)"
          @mouseleave="hideTooltip"
        >
          <div class="nav-icon">
            <i class="dx-icon dx-icon-import"></i>
          </div>
          <span v-if="!isSidebarCollapsed" class="nav-text">Inbound</span>
          <div class="nav-arrow">
            <svg
              :class="[
                { 'arrow-active': showInboundSubmenu || currentRoute.startsWith('/inbound') },
              ]"
              width="18"
              height="18"
              viewBox="0 0 18 18"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              :style="{ transform: showInboundSubmenu ? 'rotate(90deg)' : 'rotate(0deg)' }"
            >
              <polygon
                points="6,4 14,9 6,14"
                :fill="
                  showInboundSubmenu || currentRoute.startsWith('/inbound') ? '#fff' : '#bdbdbd'
                "
                :stroke="
                  showInboundSubmenu || currentRoute.startsWith('/inbound') ? '#f39c12' : 'none'
                "
                stroke-width="1.5"
              />
            </svg>
          </div>
        </div>
        <div v-if="canShowInbound" class="sidebar-menu-divider"></div>

        <!-- Outbound -->
        <div
          v-if="canShowOutbound"
          class="nav-item"
          :data-menu-key="'outbound'"
          :class="[
            { active: currentRoute.startsWith('/outbound') },
            showOutboundSubmenu && !collapsed ? 'nav-item-flyout-open' : '',
          ]"
          @click="onParentClick('outbound')"
          @mouseenter="onMenuMouseEnter('outbound', $event)"
          @mouseleave="hideTooltip"
        >
          <div class="nav-icon">
            <i class="dx-icon dx-icon-export"></i>
          </div>
          <span v-if="!isSidebarCollapsed" class="nav-text">Outbound</span>
          <div class="nav-arrow">
            <svg
              :class="[
                { 'arrow-active': showOutboundSubmenu || currentRoute.startsWith('/outbound') },
              ]"
              width="18"
              height="18"
              viewBox="0 0 18 18"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              :style="{ transform: showOutboundSubmenu ? 'rotate(90deg)' : 'rotate(0deg)' }"
            >
              <polygon
                points="6,4 14,9 6,14"
                :fill="
                  showOutboundSubmenu || currentRoute.startsWith('/outbound') ? '#fff' : '#bdbdbd'
                "
                :stroke="
                  showOutboundSubmenu || currentRoute.startsWith('/outbound') ? '#f39c12' : 'none'
                "
                stroke-width="1.5"
              />
            </svg>
          </div>
        </div>
        <div v-if="canShowOutbound" class="sidebar-menu-divider"></div>

        <!-- Admin -->
        <div
          v-if="canShowAdmin"
          class="nav-item"
          :data-menu-key="'admin'"
          :class="[
            { active: currentRoute.startsWith('/admin') },
            showAdminSubmenu && !collapsed ? 'nav-item-flyout-open' : '',
          ]"
          @click="onParentClick('admin')"
          @mouseenter="onMenuMouseEnter('admin', $event)"
          @mouseleave="hideTooltip"
        >
          <div class="nav-icon">
            <i class="dx-icon dx-icon-preferences"></i>
          </div>
          <span v-if="!isSidebarCollapsed" class="nav-text">Admin</span>
          <div class="nav-arrow">
            <svg
              :class="[{ 'arrow-active': showAdminSubmenu || currentRoute.startsWith('/admin') }]"
              width="18"
              height="18"
              viewBox="0 0 18 18"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
              :style="{ transform: showAdminSubmenu ? 'rotate(90deg)' : 'rotate(0deg)' }"
            >
              <polygon
                points="6,4 14,9 6,14"
                :fill="showAdminSubmenu || currentRoute.startsWith('/admin') ? '#fff' : '#bdbdbd'"
                :stroke="showAdminSubmenu || currentRoute.startsWith('/admin') ? '#f39c12' : 'none'"
                stroke-width="1.5"
              />
            </svg>
          </div>
        </div>
        <div v-if="canShowAdmin" class="sidebar-menu-divider"></div>
      </div>
      <!-- Tooltip for menu descriptions -->
      <CommonTooltip
        :title="tooltip.title"
        :description="tooltip.description"
        :visible="tooltip.visible"
        :x="tooltip.x"
        :y="tooltip.y"
        :menuStyle="true"
      />
    </nav>

    <!-- Flyout Submenus -->
    <div
      v-if="showInboundSubmenu && canShowInbound"
      class="submenu-flyout submenu-flyout-inbound"
      :style="inboundSubmenuStyle"
      @mouseleave="showInboundSubmenu = false"
    >
      <div
        v-if="hasFeature('feature_integration')"
        class="submenu-item"
        :class="{
          active: currentRoute === '/inbound/integrations',
          'has-nested': true,
          'open-nested': activeNestedSubmenu === 'inbound-integrations',
        }"
        @mouseenter="openNestedSubmenu('inbound-integrations')"
        @mouseleave="closeNestedSubmenu"
        @click="handleSubmenuClick('inbound', '/inbound/integrations')"
      >
        <div class="submenu-icon">
          <i class="dx-icon dx-icon-datatrending"></i>
        </div>
        <span class="submenu-text">Integrations</span>
      </div>
    </div>

    <div
      v-if="showOutboundSubmenu && canShowOutbound"
      class="submenu-flyout submenu-flyout-outbound"
      :style="outboundSubmenuStyle"
      @mouseleave="showOutboundSubmenu = false"
    >
      <div
        v-if="hasFeature('feature_arcgis_integration')"
        class="submenu-item"
        :class="{ active: currentRoute === '/outbound/integration/arcgis' }"
        @click="handleSubmenuClick('outbound', '/outbound/integration/arcgis')"
      >
        <div class="submenu-icon" style="color: var(--kw-secondary-dark)">
          <i class="dx-icon dx-icon-map"></i>
        </div>
        <span class="submenu-text">ArcGIS Integration</span>
      </div>
      <div
        v-if="hasFeature('feature_confluence_integration')"
        class="submenu-item"
        :class="{ active: currentRoute === '/outbound/integration/confluence' }"
        @click="handleSubmenuClick('outbound', '/outbound/integration/confluence')"
      >
        <div class="submenu-icon" style="color: var(--kw-secondary-dark)">
          <i class="dx-icon dx-icon-doc"></i>
        </div>
        <span class="submenu-text">Confluence Integration</span>
      </div>
      <div
        v-if="hasFeature('feature_jira_webhook')"
        class="submenu-item"
        :class="{ active: currentRoute === '/outbound/webhook/jira' }"
        @click="handleSubmenuClick('outbound', '/outbound/webhook/jira')"
      >
        <div class="submenu-icon">
          <WebhookIcon size="small" />
        </div>
        <span class="submenu-text">Jira Webhook</span>
      </div>
    </div>

    <div
      v-if="showAdminSubmenu && canShowAdmin"
      class="submenu-flyout submenu-flyout-admin"
      :style="adminSubmenuStyle"
      @mouseleave="showAdminSubmenu = false"
    >
      <div
        v-if="authStore.hasRole('tenant_admin') && hasFeature('feature_arcgis_integration')"
        class="submenu-item"
        :class="{ active: currentRoute === '/admin/connections/arcgis' }"
        @click="handleSubmenuClick('admin', '/admin/connections/arcgis')"
      >
        <div class="submenu-icon" style="color: var(--kw-secondary-dark)">
          <i class="dx-icon dx-icon-link"></i>
        </div>
        <span class="submenu-text">ArcGIS Connect</span>
      </div>
      <div
        v-if="authStore.hasRole('tenant_admin') && hasFeature('feature_confluence_integration')"
        class="submenu-item"
        :class="{ active: currentRoute === '/admin/connections/confluence' }"
        @click="handleSubmenuClick('admin', '/admin/connections/confluence')"
      >
        <div class="submenu-icon" style="color: var(--kw-secondary-dark)">
          <i class="dx-icon dx-icon-link"></i>
        </div>
        <span class="submenu-text">Confluence Connect</span>
      </div>
      <div
        v-if="authStore.hasRole('tenant_admin') && hasFeature('feature_jira_webhook')"
        class="submenu-item"
        :class="{ active: currentRoute === '/admin/connections/jira' }"
        @click="handleSubmenuClick('admin', '/admin/connections/jira')"
      >
        <div class="submenu-icon">
          <i class="dx-icon dx-icon-link"></i>
        </div>
        <span class="submenu-text">Jira Connect</span>
      </div>
      <div
        v-if="authStore.hasRole('tenant_admin')"
        class="submenu-item"
        :class="{ active: currentRoute === '/admin/audit-log' }"
        @click="handleSubmenuClick('admin', '/admin/audit-log')"
      >
        <div class="submenu-icon">
          <i class="dx-icon dx-icon-doc"></i>
        </div>
        <span class="submenu-text">Audit Logs</span>
      </div>
      <div
        v-if="isAppAdmin"
        class="submenu-item"
        :class="{ active: currentRoute === '/admin/clear-cache' }"
        @click="handleSubmenuClick('admin', '/admin/clear-cache')"
      >
        <div class="submenu-icon">
          <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
            <path
              d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 0 .5.5h3a.5.5 0 0 0 .5-.5V6a.5.5 0 0 1 1 0v6A1.5 1.5 0 0 1 9.5 13h-3A1.5 1.5 0 0 1 5 12V6a.5.5 0 0 1 .5-.5z"
            />
            <path
              d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1 0-2h3.086a1 1 0 0 1 .707.293l.707.707h2.586l.707-.707A1 1 0 0 1 10.414 2H13.5a1 1 0 0 1 1 1zM4.118 4 4 4.059V12a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3a.5.5 0 0 0 0 1H3v9a3 3 0 0 0 3 3h6a3 3 0 0 0 3-3V4h.5a.5.5 0 0 0 0-1h-11z"
            />
          </svg>
        </div>
        <span class="submenu-text">Clear Cache</span>
      </div>
      <div
        v-if="isAppAdmin"
        class="submenu-item"
        :class="{ active: currentRoute === '/admin/cache-statistics' }"
        @click="handleSubmenuClick('admin', '/admin/cache-statistics')"
      >
        <div class="submenu-icon">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
            <rect x="3" y="3" width="7" height="7" rx="1.5" />
            <rect x="14" y="3" width="7" height="7" rx="1.5" />
            <rect x="14" y="14" width="7" height="7" rx="1.5" />
            <rect x="3" y="14" width="7" height="7" rx="1.5" />
          </svg>
        </div>
        <span class="submenu-text">Cache Statistics</span>
      </div>
      <div
        v-if="authStore.hasRole('tenant_admin') || authStore.hasRole('app_admin')"
        class="submenu-item"
        :class="{ active: currentRoute === '/admin/notifications' }"
        @click="handleSubmenuClick('admin', '/admin/notifications')"
      >
        <div class="submenu-icon" style="color: var(--kw-secondary-dark)">
          <i class="dx-icon dx-icon-bell"></i>
        </div>
        <span class="submenu-text">Notifications</span>
      </div>
      <div
        v-if="authStore.hasRole('tenant_admin') || authStore.hasRole('app_admin')"
        class="submenu-item"
        :class="{ active: currentRoute === '/admin/site-config' }"
        @click="handleSubmenuClick('admin', '/admin/site-config')"
      >
        <div class="submenu-icon">
          <i class="dx-icon dx-icon-toolbox"></i>
        </div>
        <span class="submenu-text">Site Config</span>
      </div>
    </div>

    <!-- Footer: Branding + Version -->
    <div v-if="!isSidebarCollapsed" class="sidebar-footer">
      <div class="footer-brand">
        <span class="powered-text">POWERED BY</span>
        <div class="kaseware-brand">
          <img src="/kaseware.png" alt="Kaseware" class="brand-logo" />
          <span class="brand-text">KASEWARE</span>
        </div>
      </div>
      <AppVersion />
    </div>
  </aside>
</template>

<script setup lang="ts">
/* eslint-disable max-lines */
import { useRouter } from 'vue-router';
import { computed, ref, watch, onMounted, onUnmounted } from 'vue';
import { useSidebarSubmenus } from './useSidebarSubmenus';
import AppVersion from '../common/AppVersion.vue';
import CommonTooltip from '../common/Tooltip.vue';
import WebhookIcon from '../common/WebhookIcon.vue';
import { sidebarMenuDescriptions } from './sidebarMenuDescriptions';
import './layout.css';

import { useAuthStore } from '@/store/auth';
const tooltip = ref({ visible: false, title: '', description: '', x: 0, y: 0 });
const authStore = useAuthStore();
let tooltipTimeout: ReturnType<typeof setTimeout> | null = null;

const isAppAdmin = computed(() => authStore.hasRole('app_admin'));
function hasFeature(role: string): boolean {
  if (!role) return true;
  return authStore.hasRole(role);
}
const canShowInbound = computed(() => hasFeature('feature_integration'));
const canShowOutbound = computed(
  () =>
    hasFeature('feature_jira_webhook') ||
    hasFeature('feature_arcgis_integration') ||
    hasFeature('feature_confluence_integration')
);
const canShowAdmin = computed(
  () => authStore.hasRole('tenant_admin') || authStore.hasRole('app_admin')
);

function showTooltip(menuKey: string, event: MouseEvent) {
  const entry = sidebarMenuDescriptions[menuKey];
  if (!entry) return;
  const target = event.currentTarget as HTMLElement;
  if (tooltipTimeout) {
    clearTimeout(tooltipTimeout);
    tooltipTimeout = null;
  }
  tooltipTimeout = setTimeout(() => {
    if (!target || !document.body.contains(target)) return;
    const rect = target.getBoundingClientRect();
    let x = rect.right + 12;
    let y = rect.top + rect.height / 2 - 28;
    const tooltipWidth = 340; // max-width from CSS
    const tooltipHeight = 80; // approximate height
    if (x + tooltipWidth > window.innerWidth) {
      x = rect.left - tooltipWidth - 12;
    }
    if (x < 0) x = 12;
    if (y + tooltipHeight > window.innerHeight) {
      y = window.innerHeight - tooltipHeight - 12;
    }
    if (y < 0) y = 12;
    tooltip.value = {
      visible: true,
      title: entry.title,
      description: entry.description,
      x,
      y,
    };
  }, 1000);
}

function hideTooltip() {
  if (tooltipTimeout) {
    clearTimeout(tooltipTimeout);
    tooltipTimeout = null;
  }
  tooltip.value.visible = false;
}

function onParentClick(menuKey: string) {
  hideTooltip();
  if (menuKey === 'inbound') toggleInboundSubmenu();
  if (menuKey === 'outbound') toggleOutboundSubmenu();
  if (menuKey === 'admin') toggleAdminSubmenu();
}

const props = defineProps<{
  collapsed?: boolean;
}>();

const router = useRouter();
const collapsed = computed(() => !!props.collapsed);
const currentRoute = computed(() => router.currentRoute.value.path);

// Import sidebar store for reactive width management
import { useSidebarStore } from '@/store/sidebar';
const sidebarStore = useSidebarStore();

// Sidebar hover-to-expand state
const isSidebarCollapsed = ref(collapsed.value);
let wasInitiallyCollapsed = false;

// Mobile detection: disable hover expansion on touch devices
const isMobileDevice = ref(false);

// Listen for viewport changes
const handleResize = () => {
  isMobileDevice.value = window.matchMedia('(pointer: coarse)').matches || window.innerWidth < 768;
};

onMounted(() => {
  // Check if device uses coarse pointer (touch) or viewport is mobile
  isMobileDevice.value = window.matchMedia('(pointer: coarse)').matches || window.innerWidth < 768;

  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});

watch(collapsed, (val: boolean) => {
  isSidebarCollapsed.value = val;
});

function handleSidebarMouseEnter() {
  // Skip hover expansion on mobile/touch devices
  if (isMobileDevice.value) return;

  if (collapsed.value) {
    wasInitiallyCollapsed = true;
    isSidebarCollapsed.value = false;
    sidebarStore.setHoverExpanded(true);
  }
}

function handleSidebarMouseLeave() {
  // Skip if not mobile (cleanup)
  if (isMobileDevice.value) return;

  if (wasInitiallyCollapsed) {
    isSidebarCollapsed.value = true;
    wasInitiallyCollapsed = false;
    sidebarStore.setHoverExpanded(false);
    // Close all flyout submenus when sidebar collapses
    showInboundSubmenu.value = false;
    showOutboundSubmenu.value = false;
    showAdminSubmenu.value = false;
    activeNestedSubmenu.value = null;
  }
}

// Submenu state and logic extracted to composable
const {
  showInboundSubmenu,
  showOutboundSubmenu,
  showAdminSubmenu,
  activeNestedSubmenu,
  inboundSubmenuStyle,
  outboundSubmenuStyle,
  adminSubmenuStyle,
  // isAppAdmin is now only in this file
  openNestedSubmenu,
  closeNestedSubmenu,
  closeAllSubmenus,
  toggleInboundSubmenu,
  toggleOutboundSubmenu,
  toggleAdminSubmenu,
  updateSubmenuPosition,
} = useSidebarSubmenus(() => collapsed.value);

// Update submenu position on open
watch(showInboundSubmenu, val => {
  if (val) updateSubmenuPosition('inbound');
});
watch(showOutboundSubmenu, val => {
  if (val) updateSubmenuPosition('outbound');
});
watch(showAdminSubmenu, val => {
  if (val) updateSubmenuPosition('admin');
});

const resizeListener = () => {
  if (showInboundSubmenu.value) updateSubmenuPosition('inbound');
  if (showOutboundSubmenu.value) updateSubmenuPosition('outbound');
  if (showAdminSubmenu.value) updateSubmenuPosition('admin');
};
const scrollListener = () => {
  if (showInboundSubmenu.value) updateSubmenuPosition('inbound');
  if (showOutboundSubmenu.value) updateSubmenuPosition('outbound');
  if (showAdminSubmenu.value) updateSubmenuPosition('admin');
};

onMounted(() => {
  window.addEventListener('resize', resizeListener);
  window.addEventListener('scroll', scrollListener, { passive: true });
});

onUnmounted(() => {
  window.removeEventListener('resize', resizeListener);
  window.removeEventListener('scroll', scrollListener);
});

function handleSubmenuClick(type: 'inbound' | 'outbound' | 'admin', route: string) {
  if (type === 'inbound') showInboundSubmenu.value = false;
  if (type === 'outbound') showOutboundSubmenu.value = false;
  if (type === 'admin') showAdminSubmenu.value = false;
  navigateToRoute(route);
  // Collapse sidebar if it was temporarily expanded (hovered)
  if (wasInitiallyCollapsed) {
    isSidebarCollapsed.value = true;
    wasInitiallyCollapsed = false;
    closeAllSubmenus();
  }
}

function navigateToRoute(route: string) {
  if (route && route !== currentRoute.value && route.trim()) {
    router.push(route).catch((err: Error) => {
      console.error('Navigation error:', err);
    });
    closeNestedSubmenu(); // Ensure nested submenu state is cleared after navigation
  }
}

function onMenuMouseEnter(menuKey: string, event: MouseEvent) {
  closeAllSubmenus();
  showTooltip(menuKey, event);
}
</script>
