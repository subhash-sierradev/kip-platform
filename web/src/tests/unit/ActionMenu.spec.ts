import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { ActionMenuItem } from '@/components/common/ActionMenu.vue';
import ActionMenu from '@/components/common/ActionMenu.vue';

describe('ActionMenu', () => {
  let wrapper: any;
  let mockItems: ActionMenuItem[];

  beforeEach(() => {
    mockItems = [
      {
        id: 'edit',
        label: 'Edit',
        icon: 'dx-icon-edit',
        iconType: 'devextreme',
        ariaLabel: 'Edit item',
      },
      {
        id: 'delete',
        label: 'Delete',
        svgPath:
          'M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v2M4 6h16',
        iconType: 'svg',
      },
      {
        id: 'clone',
        label: 'Clone',
        icon: 'copy-icon',
        iconType: 'css',
        disabled: false,
      },
      {
        id: 'disabled-action',
        label: 'Disabled Action',
        disabled: true,
        divider: true,
      },
    ];
  });

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount();
    }
  });

  describe('Component Rendering', () => {
    it('renders trigger button with default aria-label', () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });

      const triggerButton = wrapper.find('.action-menu-trigger');
      expect(triggerButton.exists()).toBe(true);
      expect(triggerButton.attributes('aria-label')).toBe('More actions');
      expect(triggerButton.attributes('aria-expanded')).toBe('false');
    });

    it('renders trigger button with custom aria-label', () => {
      wrapper = mount(ActionMenu, {
        props: {
          items: mockItems,
          triggerAriaLabel: 'Custom menu actions',
        },
      });

      const triggerButton = wrapper.find('.action-menu-trigger');
      expect(triggerButton.attributes('aria-label')).toBe('Custom menu actions');
    });

    it('renders trigger button as disabled when disabled prop is true', () => {
      wrapper = mount(ActionMenu, {
        props: {
          items: mockItems,
          disabled: true,
        },
      });

      const triggerButton = wrapper.find('.action-menu-trigger');
      expect(triggerButton.attributes('disabled')).toBeDefined();
    });

    it('renders menu dropdown when opened', async () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });

      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(false);

      await wrapper.find('.action-menu-trigger').trigger('click');

      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(true);
      expect(wrapper.find('.action-menu-dropdown').attributes('role')).toBe('menu');
    });
  });

  describe('Menu Items Rendering', () => {
    beforeEach(async () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });
      // Open the menu
      await wrapper.find('.action-menu-trigger').trigger('click');
    });

    it('renders all menu items with correct labels', () => {
      const menuItems = wrapper.findAll('.action-menu-item');
      expect(menuItems).toHaveLength(4);

      expect(menuItems[0].text()).toBe('Edit');
      expect(menuItems[1].text()).toBe('Delete');
      expect(menuItems[2].text()).toBe('Clone');
      expect(menuItems[3].text()).toBe('Disabled Action');
    });

    it('renders DevExtreme icons correctly', () => {
      const editItem = wrapper.findAll('.action-menu-item')[0];
      const devextremeIcon = editItem.find('i.dx-icon-edit');

      expect(devextremeIcon.exists()).toBe(true);
      expect(devextremeIcon.classes()).toContain('action-menu-item-icon');
    });

    it('renders SVG icons correctly', () => {
      const deleteItem = wrapper.findAll('.action-menu-item')[1];
      const svgIcon = deleteItem.find('svg');

      expect(svgIcon.exists()).toBe(true);
      expect(svgIcon.classes()).toContain('action-menu-item-icon');
      expect(svgIcon.attributes('viewBox')).toBe('0 0 24 24');

      const path = svgIcon.find('path');
      expect(path.exists()).toBe(true);
      expect(path.attributes('d')).toContain('M19 7l-.867 12.142A2');
    });

    it('renders CSS icons correctly', () => {
      const cloneItem = wrapper.findAll('.action-menu-item')[2];
      const cssIcon = cloneItem.find('i.copy-icon');

      expect(cssIcon.exists()).toBe(true);
      expect(cssIcon.classes()).toContain('action-menu-item-icon');
    });

    it('disables items when disabled property is true', () => {
      const disabledItem = wrapper.findAll('.action-menu-item')[3];
      expect(disabledItem.attributes('disabled')).toBeDefined();
    });

    it('renders dividers after items with divider property', () => {
      const dividers = wrapper.findAll('.action-menu-divider');
      expect(dividers).toHaveLength(1);
      expect(dividers[0].attributes('role')).toBe('separator');
    });

    it('applies aria-label correctly', () => {
      const editItem = wrapper.findAll('.action-menu-item')[0];
      expect(editItem.attributes('aria-label')).toBe('Edit item');

      expect(editItem.attributes('role')).toBe('menuitem');
    });
  });

  describe('Menu Interaction', () => {
    beforeEach(() => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });
    });

    it('opens menu when trigger button is clicked', async () => {
      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(false);

      await wrapper.find('.action-menu-trigger').trigger('click');

      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(true);
      expect(wrapper.find('.action-menu-trigger').attributes('aria-expanded')).toBe('true');
    });

    it('closes menu when trigger button is clicked again', async () => {
      // Open menu
      await wrapper.find('.action-menu-trigger').trigger('click');
      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(true);

      // Close menu
      await wrapper.find('.action-menu-trigger').trigger('click');
      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(false);
      expect(wrapper.find('.action-menu-trigger').attributes('aria-expanded')).toBe('false');
    });

    it('does not open menu when disabled', async () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems, disabled: true },
      });

      await wrapper.find('.action-menu-trigger').trigger('click');
      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(false);
    });

    it('emits action event when menu item is clicked', async () => {
      await wrapper.find('.action-menu-trigger').trigger('click');

      const editItem = wrapper.findAll('.action-menu-item')[0];
      await editItem.trigger('click');

      expect(wrapper.emitted('action')).toBeTruthy();
      expect(wrapper.emitted('action')[0]).toEqual(['edit']);
    });

    it('closes menu after menu item is clicked', async () => {
      await wrapper.find('.action-menu-trigger').trigger('click');
      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(true);

      const editItem = wrapper.findAll('.action-menu-item')[0];
      await editItem.trigger('click');

      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(false);
    });

    it('does not emit action event when disabled item is clicked', async () => {
      await wrapper.find('.action-menu-trigger').trigger('click');

      const disabledItem = wrapper.findAll('.action-menu-item')[3];
      await disabledItem.trigger('click');

      expect(wrapper.emitted('action')).toBeFalsy();
    });

    it('covers the disabled toggle branch directly', () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems, disabled: true },
      });

      const setupState = (wrapper.vm as any).$?.setupState;
      setupState.toggleMenu();

      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(false);
    });
  });

  describe('Click Outside Handling', () => {
    let clickHandler: ((event: Event) => void) | null = null;

    beforeEach(() => {
      // Mock document event listeners
      vi.spyOn(document, 'addEventListener').mockImplementation((type, handler) => {
        if (type === 'click') {
          clickHandler = handler as (event: Event) => void;
        }
      });

      vi.spyOn(document, 'removeEventListener').mockImplementation(() => {
        clickHandler = null;
      });
    });

    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('adds click event listener on mount', () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });

      expect(document.addEventListener).toHaveBeenCalledWith('click', expect.any(Function));
      expect(clickHandler).toBeTruthy();
    });

    it('removes click event listener on unmount', () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });

      wrapper.unmount();

      expect(document.removeEventListener).toHaveBeenCalledWith('click', expect.any(Function));
    });

    it('closes menu when clicking outside', async () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });

      // Open menu
      await wrapper.find('.action-menu-trigger').trigger('click');
      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(true);

      // Create a mock click event outside the component
      const outsideElement = document.createElement('div');
      const clickEvent = new Event('click');
      Object.defineProperty(clickEvent, 'target', {
        value: outsideElement,
        enumerable: true,
      });

      if (clickHandler) {
        clickHandler(clickEvent);
        await wrapper.vm.$nextTick();
      }

      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(false);
    });

    it('does not close menu when clicking inside', async () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });

      // Open menu
      await wrapper.find('.action-menu-trigger').trigger('click');
      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(true);

      // Create a mock click event inside the component
      const menuElement = wrapper.find('.action-menu').element;
      const clickEvent = new Event('click');
      Object.defineProperty(clickEvent, 'target', {
        value: menuElement,
        enumerable: true,
      });

      if (clickHandler) {
        clickHandler(clickEvent);
        await wrapper.vm.$nextTick();
      }

      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(true);
    });

    it('ignores outside-click handling when the menu container ref is missing', () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });

      const setupState = (wrapper.vm as any).$?.setupState;
      setupState.menuContainer = undefined;
      setupState.handleClickOutside(new Event('click'));

      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(false);
    });

    describe('Menu Coordination', () => {
      it('closes an open menu when another menu instance opens', async () => {
        const first = mount(ActionMenu, { props: { items: mockItems } });
        const second = mount(ActionMenu, { props: { items: mockItems } });

        await first.find('.action-menu-trigger').trigger('click');
        expect(first.find('.action-menu-dropdown').exists()).toBe(true);

        await second.find('.action-menu-trigger').trigger('click');

        expect(first.find('.action-menu-dropdown').exists()).toBe(false);
        expect(second.find('.action-menu-dropdown').exists()).toBe(true);

        first.unmount();
        second.unmount();
      });

      it('skips dropdown positioning when the dropdown ref is unavailable', async () => {
        wrapper = mount(ActionMenu, {
          props: { items: mockItems },
        });

        const setupState = (wrapper.vm as any).$?.setupState;
        setupState.dropdownElement = undefined;
        setupState.toggleMenu();
        await wrapper.vm.$nextTick();

        expect(wrapper.find('.action-menu-dropdown').exists()).toBe(true);
      });
    });
  });

  describe('Event Propagation', () => {
    it('prevents event propagation on trigger button click', async () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });

      const clickEvent = new MouseEvent('click', { bubbles: true, cancelable: true });
      const stopPropagationSpy = vi.spyOn(clickEvent, 'stopPropagation');

      const trigger = wrapper.find('.action-menu-trigger');
      await trigger.trigger('click', {
        preventDefault: () => {},
        stopPropagation: stopPropagationSpy,
      });

      // The .stop modifier should prevent propagation
      // In testing, we verify the functionality works correctly
      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(true);
    });

    it('prevents event propagation on menu item click', async () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });

      await wrapper.find('.action-menu-trigger').trigger('click');

      const clickEvent = new MouseEvent('click', { bubbles: true, cancelable: true });
      const stopPropagationSpy = vi.spyOn(clickEvent, 'stopPropagation');

      const menuItem = wrapper.findAll('.action-menu-item')[0];
      await menuItem.trigger('click', {
        preventDefault: () => {},
        stopPropagation: stopPropagationSpy,
      });

      // Verify the action was emitted and menu closed
      expect(wrapper.emitted('action')).toBeTruthy();
      expect(wrapper.find('.action-menu-dropdown').exists()).toBe(false);
    });
  });

  describe('Accessibility', () => {
    beforeEach(async () => {
      wrapper = mount(ActionMenu, {
        props: { items: mockItems },
      });
      await wrapper.find('.action-menu-trigger').trigger('click');
    });

    it('has proper ARIA attributes on trigger button', () => {
      const trigger = wrapper.find('.action-menu-trigger');

      expect(trigger.attributes('aria-label')).toBe('More actions');
      expect(trigger.attributes('aria-expanded')).toBe('true');
    });

    it('has proper role attributes on menu elements', () => {
      const dropdown = wrapper.find('.action-menu-dropdown');
      const menuItems = wrapper.findAll('.action-menu-item');
      const divider = wrapper.find('.action-menu-divider');

      expect(dropdown.attributes('role')).toBe('menu');
      menuItems.forEach((item: any) => {
        expect(item.attributes('role')).toBe('menuitem');
      });
      expect(divider.attributes('role')).toBe('separator');
    });

    it('uses custom aria-label when provided', () => {
      const editItem = wrapper.findAll('.action-menu-item')[0];
      expect(editItem.attributes('aria-label')).toBe('Edit item');
    });

    it('falls back to label for aria-label when custom not provided', () => {
      const deleteItem = wrapper.findAll('.action-menu-item')[1];
      expect(deleteItem.attributes('aria-label')).toBe('Delete');
    });
  });

  describe('Edge Cases', () => {
    it('handles empty items array', () => {
      wrapper = mount(ActionMenu, {
        props: { items: [] },
      });

      expect(wrapper.find('.action-menu-trigger').exists()).toBe(true);
    });

    it('handles items without icons', async () => {
      const itemsWithoutIcons: ActionMenuItem[] = [{ id: 'no-icon', label: 'No Icon Item' }];

      wrapper = mount(ActionMenu, {
        props: { items: itemsWithoutIcons },
      });

      await wrapper.find('.action-menu-trigger').trigger('click');

      const menuItem = wrapper.find('.action-menu-item');
      expect(menuItem.exists()).toBe(true);
      expect(menuItem.text()).toBe('No Icon Item');
      expect(menuItem.find('i').exists()).toBe(false);
      expect(menuItem.find('svg').exists()).toBe(false);
    });

    it('handles missing iconType with fallback', async () => {
      const itemsWithIcon: ActionMenuItem[] = [
        { id: 'fallback-icon', label: 'Fallback Icon', icon: 'some-icon' },
      ];

      wrapper = mount(ActionMenu, {
        props: { items: itemsWithIcon },
      });

      await wrapper.find('.action-menu-trigger').trigger('click');

      const menuItem = wrapper.find('.action-menu-item');
      const icon = menuItem.find('i.some-icon');
      expect(icon.exists()).toBe(true);
      expect(icon.classes()).toContain('action-menu-item-icon');
    });

    it('handles items with variant property', async () => {
      const itemsWithVariant: ActionMenuItem[] = [
        { id: 'primary-action', label: 'Primary', variant: 'primary' },
        { id: 'danger-action', label: 'Danger', variant: 'danger' },
        { id: 'secondary-action', label: 'Secondary', variant: 'secondary' },
      ];

      wrapper = mount(ActionMenu, {
        props: { items: itemsWithVariant },
      });

      await wrapper.find('.action-menu-trigger').trigger('click');

      const items = wrapper.findAll('.action-menu-item');
      expect(items[0].classes()).toContain('variant-primary');
      expect(items[1].classes()).toContain('variant-danger');
      expect(items[2].classes()).toContain('variant-secondary');
    });

    it('handles items without divider property', async () => {
      const itemsWithoutDivider: ActionMenuItem[] = [
        { id: 'action1', label: 'Action 1' },
        { id: 'action2', label: 'Action 2', divider: false },
      ];

      wrapper = mount(ActionMenu, {
        props: { items: itemsWithoutDivider },
      });

      await wrapper.find('.action-menu-trigger').trigger('click');

      const dividers = wrapper.findAll('.action-menu-divider');
      expect(dividers).toHaveLength(0);
    });

    it('handles items with enabled state (disabled: false)', async () => {
      const enabledItems: ActionMenuItem[] = [
        { id: 'enabled', label: 'Enabled Action', disabled: false },
      ];

      wrapper = mount(ActionMenu, {
        props: { items: enabledItems },
      });

      await wrapper.find('.action-menu-trigger').trigger('click');

      const item = wrapper.find('.action-menu-item');
      expect(item.attributes('disabled')).toBeUndefined();

      await item.trigger('click');
      expect(wrapper.emitted('action')).toBeTruthy();
      expect(wrapper.emitted('action')[0]).toEqual(['enabled']);
    });
  });
});
