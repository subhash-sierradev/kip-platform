import { describe, expect, it, vi } from 'vitest';

import { useSidebarSubmenus } from '@/components/layout/useSidebarSubmenus';

// Mock Pinia auth store usage inside composable
vi.mock('@/store/auth', () => ({
  useAuthStore: () => ({ hasRole: () => false }),
}));

describe('useSidebarSubmenus', () => {
  it('toggles inbound submenu based on collapsed state', () => {
    const api = useSidebarSubmenus(() => true); // collapsed
    api.toggleInboundSubmenu();
    expect(api.showInboundSubmenu.value).toBe(true);
    expect(api.showOutboundSubmenu.value).toBe(false);

    const api2 = useSidebarSubmenus(() => false); // expanded
    expect(api2.showInboundSubmenu.value).toBe(false);
    api2.toggleInboundSubmenu();
    expect(api2.showInboundSubmenu.value).toBe(true);
    api2.toggleInboundSubmenu();
    expect(api2.showInboundSubmenu.value).toBe(false);
  });

  it('updates submenu position from DOM', () => {
    document.body.innerHTML = `
      <aside class="kaseware-sidebar">
        <div class="nav-item" data-menu-key="inbound" style="position:absolute;left:0;top:0;width:100px;height:20px"></div>
      </aside>
    `;
    const api = useSidebarSubmenus(() => false);
    api.updateSubmenuPosition('inbound');
    expect(api.inboundSubmenuStyle.value.top).toMatch(/px$/);
    expect(api.inboundSubmenuStyle.value.left).toMatch(/px$/);
  });

  it('manages nested submenu state', () => {
    const api = useSidebarSubmenus(() => false);
    api.openNestedSubmenu('inbound-integrations');
    expect(api.activeNestedSubmenu.value).toBe('inbound-integrations');
    api.closeAllSubmenus();
    expect(api.activeNestedSubmenu.value).toBeNull();
    expect(api.showInboundSubmenu.value).toBe(false);
    expect(api.showOutboundSubmenu.value).toBe(false);
    expect(api.showAdminSubmenu.value).toBe(false);
  });
});
