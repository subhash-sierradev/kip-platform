// Extracted submenu logic from SideNav.vue
import { computed, Ref, ref } from 'vue';

import { useAuthStore } from '@/store/auth';

function useSubmenuToggles(
  collapsed: () => boolean,
  showInboundSubmenu: Ref<boolean>,
  showOutboundSubmenu: Ref<boolean>,
  showAdminSubmenu: Ref<boolean>,
  closeAllSubmenus: () => void
) {
  function toggleInboundSubmenu() {
    if (collapsed()) {
      closeAllSubmenus();
      showInboundSubmenu.value = true;
    } else {
      showInboundSubmenu.value = !showInboundSubmenu.value;
      if (showInboundSubmenu.value) {
        closeAllSubmenus();
        showInboundSubmenu.value = true;
      }
    }
  }
  function toggleOutboundSubmenu() {
    if (collapsed()) {
      closeAllSubmenus();
      showOutboundSubmenu.value = true;
    } else {
      showOutboundSubmenu.value = !showOutboundSubmenu.value;
      if (showOutboundSubmenu.value) {
        closeAllSubmenus();
        showOutboundSubmenu.value = true;
      }
    }
  }
  function toggleAdminSubmenu() {
    if (collapsed()) {
      closeAllSubmenus();
      showAdminSubmenu.value = true;
    } else {
      if (!showAdminSubmenu.value) {
        closeAllSubmenus();
        showAdminSubmenu.value = true;
      } else {
        showAdminSubmenu.value = false;
      }
    }
  }
  return { toggleInboundSubmenu, toggleOutboundSubmenu, toggleAdminSubmenu };
}

function useSubmenuPositioning(
  inboundSubmenuStyle: Ref<Record<string, string>>,
  outboundSubmenuStyle: Ref<Record<string, string>>,
  adminSubmenuStyle: Ref<Record<string, string>>
) {
  function updateSubmenuPosition(menuKey: 'inbound' | 'outbound' | 'admin') {
    const sidebar = document.querySelector('.kaseware-sidebar');
    if (!sidebar) return;
    const navItem = sidebar.querySelector(`.nav-item[data-menu-key="${menuKey}"]`);
    if (!navItem) return;
    const rect = navItem.getBoundingClientRect();
    const scrollY = window.scrollY || window.pageYOffset;
    const style = {
      top: `${rect.top + scrollY}px`,
      left: `${rect.right}px`,
    };
    if (menuKey === 'inbound') inboundSubmenuStyle.value = style;
    if (menuKey === 'outbound') outboundSubmenuStyle.value = style;
    if (menuKey === 'admin') adminSubmenuStyle.value = style;
  }
  return { updateSubmenuPosition };
}

export function useSidebarSubmenus(collapsed: () => boolean) {
  const showInboundSubmenu = ref(false);
  const showOutboundSubmenu = ref(false);
  const showAdminSubmenu = ref(false);
  const activeNestedSubmenu = ref<string | null>(null);
  const inboundSubmenuStyle = ref<Record<string, string>>({});
  const outboundSubmenuStyle = ref<Record<string, string>>({});
  const adminSubmenuStyle = ref<Record<string, string>>({});
  const authStore = useAuthStore();
  const isAppAdmin = computed(() => authStore.hasRole('app_admin'));

  function openNestedSubmenu(key: string) {
    activeNestedSubmenu.value = key;
  }
  function closeNestedSubmenu() {
    activeNestedSubmenu.value = null;
  }
  function closeAllSubmenus() {
    showInboundSubmenu.value = false;
    showOutboundSubmenu.value = false;
    showAdminSubmenu.value = false;
    activeNestedSubmenu.value = null;
  }

  const { toggleInboundSubmenu, toggleOutboundSubmenu, toggleAdminSubmenu } = useSubmenuToggles(
    collapsed,
    showInboundSubmenu,
    showOutboundSubmenu,
    showAdminSubmenu,
    closeAllSubmenus
  );
  const { updateSubmenuPosition } = useSubmenuPositioning(
    inboundSubmenuStyle,
    outboundSubmenuStyle,
    adminSubmenuStyle
  );

  return {
    showInboundSubmenu,
    showOutboundSubmenu,
    showAdminSubmenu,
    activeNestedSubmenu,
    inboundSubmenuStyle,
    outboundSubmenuStyle,
    adminSubmenuStyle,
    isAppAdmin,
    openNestedSubmenu,
    closeNestedSubmenu,
    closeAllSubmenus,
    toggleInboundSubmenu,
    toggleOutboundSubmenu,
    toggleAdminSubmenu,
    updateSubmenuPosition,
  };
}
