import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, useAttrs } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';

import TopBar from '@/components/layout/TopBar.vue';
import { useNotificationStore } from '@/store/notification';

// Create mock logout function that can be spied on
const mockLogout = vi.fn().mockResolvedValue(undefined);

// Create a configurable mock
let mockAuthStore: any = {
  hasRole: (role: string) => role === 'app_admin',
  currentUser: {
    userName: 'john smith',
    tenantId: 'AcmeCorp',
    userMail: 'john.smith@acmecorp.com',
    userId: 'jsmith123',
  },
  logout: mockLogout,
};

// Mock auth store with configurable return
vi.mock('@/store/auth', () => ({
  useAuthStore: () => mockAuthStore,
}));

// Mock utility
vi.mock('@/utils/stringFormatUtils', () => ({
  capitalizeName: (s: string) =>
    s
      .split(' ')
      .map(w => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' '),
}));

// Functional stub that forwards class and click
const DxButtonStub = defineComponent({
  name: 'DxButton',
  emits: ['click'],
  setup(_, { emit, slots }) {
    const attrs = useAttrs();
    return () =>
      h(
        'button',
        { class: ['dx-button', attrs.class as any], onClick: () => emit('click') },
        slots.default?.()
      );
  },
});

const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', component: { template: '<div />' } }],
});

describe('TopBar.vue', () => {
  beforeEach(() => {
    mockLogout.mockClear();
    // Reset mock to default values
    mockAuthStore = {
      hasRole: (role: string) => role === 'app_admin',
      currentUser: {
        userName: 'john smith',
        tenantId: 'AcmeCorp',
        userMail: 'john.smith@acmecorp.com',
        userId: 'jsmith123',
      },
      logout: mockLogout,
    };
  });

  it('emits toggle when menu button clicked', async () => {
    const wrapper = mount(TopBar, {
      props: { collapsed: false },
      global: {
        plugins: [router],
        components: { DxButton: DxButtonStub },
      },
    });

    const btn = wrapper.find('.dx-button.menu-toggle');
    expect(btn.exists()).toBe(true);
    await btn.trigger('click');
    expect(wrapper.emitted('toggle')).toBeTruthy();
  });

  it('applies collapsed class to menu toggle when collapsed prop is true', async () => {
    const wrapper = mount(TopBar, {
      props: { collapsed: true },
      global: {
        plugins: [router],
        components: { DxButton: DxButtonStub },
      },
    });

    const btn = wrapper.find('.menu-toggle');
    expect(btn.classes()).toContain('collapsed');
  });

  it('shows formatted tenant id and user name', async () => {
    const wrapper = mount(TopBar, {
      props: { collapsed: true },
      global: {
        plugins: [router],
        components: { DxButton: DxButtonStub },
      },
    });

    expect(wrapper.find('.org-label').text()).toBe('AcmeCorp');
    expect(wrapper.find('.user-name').text()).toBe('john smith');
  });

  it('navigates to home when logo is clicked', async () => {
    const push = vi.spyOn(router, 'push');
    const wrapper = mount(TopBar, {
      global: {
        plugins: [router],
        components: { DxButton: DxButtonStub },
      },
    });

    const logo = wrapper.find('.logo-section');
    await logo.trigger('click');
    expect(push).toHaveBeenCalledWith('/');
  });

  it('toggles user dropdown and closes on outside click', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    expect(wrapper.find('.user-dropdown').exists()).toBe(true);

    document.body.click();
    await Promise.resolve();
    expect(wrapper.find('.user-dropdown').exists()).toBe(false);
  });

  it('displays user email and user ID in dropdown', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    const dropdownUserNames = wrapper.findAll('.dropdown-user-name');
    expect(dropdownUserNames).toHaveLength(2);
    expect(dropdownUserNames[0].text()).toBe('jsmith123');
    expect(dropdownUserNames[1].text()).toBe('john.smith@acmecorp.com');
  });

  it('rotates arrow icon when dropdown is open', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const arrow = wrapper.find('.arrow-icon');
    expect(arrow.classes()).not.toContain('rotated');

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    expect(arrow.classes()).toContain('rotated');
  });

  it('calls logout when logout button is clicked', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    const logoutBtn = wrapper.find('.user-dropdown-item');
    await logoutBtn.trigger('click');

    expect(mockLogout).toHaveBeenCalledTimes(1);
  });

  it('closes dropdown after logout is clicked', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    expect(wrapper.find('.user-dropdown').exists()).toBe(true);

    const logoutBtn = wrapper.find('.user-dropdown-item');
    await logoutBtn.trigger('click');
    await wrapper.vm.$nextTick();

    expect(wrapper.find('.user-dropdown').exists()).toBe(false);
  });

  it('shows notification badge with has-notifications class', async () => {
    const notificationStore = useNotificationStore();
    notificationStore.unreadCount = 3;

    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const notificationItem = wrapper.find('.notification-item');
    expect(notificationItem.exists()).toBe(true);
    expect(notificationItem.classes()).toContain('has-notifications');
  });

  it('triggers notification toggle when notification icon is clicked', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const notificationItem = wrapper.find('.notification-item');
    await notificationItem.trigger('click');

    // Since toggleNotifications is empty, we just verify the element is clickable
    expect(notificationItem.exists()).toBe(true);
  });

  it('triggers help guide toggle when help icon is clicked', async () => {
    const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const helpItem = wrapper.find('.help-item');
    await helpItem.trigger('click');

    expect(consoleWarnSpy).toHaveBeenCalledWith(
      'Help guide clicked - functionality to be implemented'
    );
    consoleWarnSpy.mockRestore();
  });

  it('displays organization icon in badge', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const orgIcon = wrapper.find('.org-icon i.dx-icon-group');
    expect(orgIcon.exists()).toBe(true);
  });

  it('displays notification bell icon', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const bellIcon = wrapper.find('.notification-bell');
    expect(bellIcon.exists()).toBe(true);
  });

  it('displays help icon', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const helpIcon = wrapper.find('.help-icon.dx-icon-help');
    expect(helpIcon.exists()).toBe(true);
  });

  it('displays user icon in profile and dropdown header', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profileUserIcon = wrapper.find('.user-profile .user-icon i.dx-icon-user');
    expect(profileUserIcon.exists()).toBe(true);

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    const dropdownUserIcon = wrapper.find('.user-dropdown-header .user-icon i.dx-icon-mention');
    expect(dropdownUserIcon.exists()).toBe(true);
  });

  it('displays email icon in dropdown header', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    const emailIcon = wrapper.find('.user-dropdown-header i.dx-icon-email');
    expect(emailIcon.exists()).toBe(true);
  });

  it('displays logout icon in logout button', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    const logoutIcon = wrapper.find('.logout-icon i.dx-icon-export');
    expect(logoutIcon.exists()).toBe(true);
  });

  it('does not close dropdown when clicking inside dropdown', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    expect(wrapper.find('.user-dropdown').exists()).toBe(true);

    const dropdown = wrapper.find('.user-dropdown');
    await dropdown.trigger('click');
    await wrapper.vm.$nextTick();

    expect(wrapper.find('.user-dropdown').exists()).toBe(true);
  });

  it('handles auth store with missing user data gracefully', async () => {
    // Set mock to return null currentUser
    mockAuthStore.currentUser = null;

    const wrapper = mount(TopBar, {
      global: {
        plugins: [router],
        components: { DxButton: DxButtonStub },
      },
    });

    // Should display fallback values
    expect(wrapper.find('.org-label').text()).toBe('No Tenant');
    expect(wrapper.find('.user-name').text()).toBe('Unknown User');

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    const dropdownUserNames = wrapper.findAll('.dropdown-user-name');
    expect(dropdownUserNames[0].text()).toBe('No User ID');
    expect(dropdownUserNames[1].text()).toBe('No Email');
  });

  it('handles collapsed prop being undefined', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const btn = wrapper.find('.menu-toggle');
    // When collapsed is undefined, it should not have collapsed class (computed returns false)
    expect(btn.classes()).not.toContain('collapsed');
  });

  it('handles logout error silently', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const failingLogout = vi.fn().mockRejectedValue(new Error('Logout failed'));

    // Mock the auth store at the component instance level
    const wrapper = mount(TopBar, {
      global: {
        plugins: [router],
        components: { DxButton: DxButtonStub },
      },
    });

    // Replace the logout function with a failing one
    const authStore = (wrapper.vm as any).authStore;
    authStore.logout = failingLogout;

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    const logoutBtn = wrapper.find('.user-dropdown-item');
    await logoutBtn.trigger('click');

    // Wait for the promise to settle
    await new Promise(resolve => setTimeout(resolve, 10));

    // Error should be caught and handled silently (no console.error)
    expect(consoleErrorSpy).not.toHaveBeenCalled();
    expect(failingLogout).toHaveBeenCalled();
    consoleErrorSpy.mockRestore();
  });

  it('does not close dropdown when userMenuRef is null during click outside', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    expect(wrapper.find('.user-dropdown').exists()).toBe(true);

    // Simulate ref being null
    (wrapper.vm as any).userMenuRef = null;

    document.body.click();
    await Promise.resolve();

    // Dropdown should still exist because ref was null
    expect(wrapper.find('.user-dropdown').exists()).toBe(true);
  });

  it('shows notification badge without has-notifications when hasUnreadNotifications is false', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    // Change hasUnreadNotifications to false
    (wrapper.vm as any).hasUnreadNotifications = false;
    await wrapper.vm.$nextTick();

    const notificationItem = wrapper.find('.notification-item');
    expect(notificationItem.exists()).toBe(true);
    expect(notificationItem.classes()).not.toContain('has-notifications');
  });

  it('toggles user menu on and off multiple times', async () => {
    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');

    // Open
    await profile.trigger('click');
    expect(wrapper.find('.user-dropdown').exists()).toBe(true);

    // Close
    await profile.trigger('click');
    expect(wrapper.find('.user-dropdown').exists()).toBe(false);

    // Open again
    await profile.trigger('click');
    expect(wrapper.find('.user-dropdown').exists()).toBe(true);
  });

  it('handles currentUser with missing userName property', async () => {
    // Set mock with partial user data
    mockAuthStore.currentUser = {
      tenantId: 'AcmeCorp',
      userMail: 'john.smith@acmecorp.com',
      userId: 'jsmith123',
    };

    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    expect(wrapper.find('.user-name').text()).toBe('Unknown User');
  });

  it('handles currentUser with missing tenantId property', async () => {
    mockAuthStore.currentUser = {
      userName: 'john smith',
      userMail: 'john.smith@acmecorp.com',
      userId: 'jsmith123',
    };

    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    expect(wrapper.find('.org-label').text()).toBe('No Tenant');
  });

  it('handles currentUser with missing userMail property', async () => {
    mockAuthStore.currentUser = {
      userName: 'john smith',
      tenantId: 'AcmeCorp',
      userId: 'jsmith123',
    };

    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    const dropdownUserNames = wrapper.findAll('.dropdown-user-name');
    expect(dropdownUserNames[1].text()).toBe('No Email');
  });

  it('handles currentUser with missing userId property', async () => {
    mockAuthStore.currentUser = {
      userName: 'john smith',
      tenantId: 'AcmeCorp',
      userMail: 'john.smith@acmecorp.com',
    };

    const wrapper = mount(TopBar, {
      global: { plugins: [router], components: { DxButton: DxButtonStub } },
    });

    const profile = wrapper.find('.user-profile');
    await profile.trigger('click');
    await wrapper.vm.$nextTick();

    const dropdownUserNames = wrapper.findAll('.dropdown-user-name');
    expect(dropdownUserNames[0].text()).toBe('No User ID');
  });
});
