import { mount, VueWrapper } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import DetailsPageHeader from '@/components/common/DetailsPageHeader.vue';

// Mock DevExtreme DxButton
vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    name: 'DxButton',
    template: '<button class="dx-button" @click="$emit(\'click\')"><slot /></button>',
    props: ['type', 'icon', 'text', 'stylingMode', 'disabled'],
  },
}));

// Mock CommonTooltip
vi.mock('@/components/common/CommonTooltip.vue', () => ({
  default: {
    name: 'CommonTooltip',
    template: '<div v-if="visible" class="common-tooltip">{{ text }}</div>',
    props: ['visible', 'text', 'x', 'y'],
  },
}));

// Mock TabNavigation
vi.mock('@/components/common/TabNavigation.vue', () => ({
  default: {
    name: 'TabNavigation',
    template: '<div class="tab-navigation"><slot /></div>',
    props: ['tabs', 'activeTab'],
  },
}));

// Mock useTooltip composable
const mockShowTooltip = vi.fn();
const mockHideTooltip = vi.fn();

vi.mock('@/composables/useTooltip', () => ({
  useTooltip: () => ({
    tooltip: {
      visible: false,
      text: '',
      x: 0,
      y: 0,
    },
    showTooltip: mockShowTooltip,
    hideTooltip: mockHideTooltip,
  }),
}));

describe('DetailsPageHeader', () => {
  let wrapper: VueWrapper;

  const defaultProps = {
    title: 'Test Page Title',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Component Mounting & Props', () => {
    it('renders component with default props', () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
      });

      expect(wrapper.exists()).toBe(true);
      expect(wrapper.find('.page-header').exists()).toBe(true);
    });

    it('displays title correctly', () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
      });

      const title = wrapper.find('.page-title');
      expect(title.exists()).toBe(true);
      expect(title.text()).toBe('Test Page Title');
    });

    it('passes custom title prop', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          title: 'Custom Title Text',
        },
      });

      expect(wrapper.find('.page-title').text()).toBe('Custom Title Text');
    });
  });

  describe('Loading State', () => {
    it('displays "Loading..." when loading prop is true', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          loading: true,
        },
      });

      expect(wrapper.find('.page-title').text()).toBe('Loading...');
    });

    it('displays normal title when loading is false', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          loading: false,
        },
      });

      expect(wrapper.find('.page-title').text()).toBe('Test Page Title');
    });
  });

  describe('Error State', () => {
    it('displays "Error loading data" when error prop is provided', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          error: 'Some error message',
        },
      });

      expect(wrapper.find('.page-title').text()).toBe('Error loading data');
    });

    it('displays normal title when error is null', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          error: null,
        },
      });

      expect(wrapper.find('.page-title').text()).toBe('Test Page Title');
    });

    it('prioritizes loading state over error state', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          loading: true,
          error: 'Error message',
        },
      });

      expect(wrapper.find('.page-title').text()).toBe('Loading...');
    });
  });

  describe('Back Button', () => {
    it('shows back button by default (showBackButton defaults to true)', () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
      });

      expect(wrapper.find('.back-button').exists()).toBe(true);
    });

    it('hides back button when showBackButton is false', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          showBackButton: false,
        },
      });

      expect(wrapper.find('.back-button').exists()).toBe(false);
    });

    it('emits back event when back button is clicked', async () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
      });

      await wrapper.find('.back-button').trigger('click');

      expect(wrapper.emitted('back')).toBeTruthy();
      expect(wrapper.emitted('back')).toHaveLength(1);
    });

    it('uses custom backButtonLabel prop', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          backButtonLabel: 'Return to List',
        },
      });

      const backButton = wrapper.find('.back-button');
      expect(backButton.exists()).toBe(true);
    });

    it('uses default backButtonLabel when not provided', () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
      });

      const backButton = wrapper.find('.back-button');
      expect(backButton.exists()).toBe(true);
    });
  });

  describe('Icon Display', () => {
    it('displays icon container when icon prop is provided', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          icon: 'folder',
        },
      });

      const iconContainer = wrapper.find('.icon-container');
      expect(iconContainer.exists()).toBe(true);
    });

    it('does not display icon container when icon prop is undefined', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          icon: undefined,
        },
      });

      const iconContainer = wrapper.find('.icon-container');
      expect(iconContainer.exists()).toBe(false);
    });

    it('adds dx-icon prefix to icon class when not already present', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          icon: 'folder',
        },
      });

      const icon = wrapper.find('.icon-container i');
      expect(icon.classes()).toContain('dx-icon');
      expect(icon.classes()).toContain('dx-icon-folder');
    });

    it('uses icon class as-is when it starts with dx-icon', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          icon: 'dx-icon-custom',
        },
      });

      const icon = wrapper.find('.icon-container i');
      expect(icon.classes()).toContain('dx-icon-custom');
    });

    it('renders custom icon slot when provided', () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
        slots: {
          'custom-icon': '<div class="custom-icon-content">Custom Icon</div>',
        },
      });

      expect(wrapper.find('.custom-icon-content').exists()).toBe(true);
      expect(wrapper.find('.custom-icon-content').text()).toBe('Custom Icon');
    });

    it('prefers custom icon slot over icon prop', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          icon: 'folder',
        },
        slots: {
          'custom-icon': '<div class="custom-icon-content">Custom</div>',
        },
      });

      expect(wrapper.find('.custom-icon-content').exists()).toBe(true);
      expect(wrapper.find('.dx-icon-folder').exists()).toBe(false);
    });
  });

  describe('Version Badge', () => {
    it('displays version badge when version prop is provided', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          version: '1.0.0',
        },
      });

      const versionBadge = wrapper.find('.version-badge');
      expect(versionBadge.exists()).toBe(true);
      expect(versionBadge.text()).toBe('v1.0.0');
    });

    it('does not display version badge when version is undefined', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          version: undefined,
        },
      });

      expect(wrapper.find('.version-badge').exists()).toBe(false);
    });

    it('displays different version values correctly', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          version: '2.5.3',
        },
      });

      expect(wrapper.find('.version-badge').text()).toBe('v2.5.3');
    });
  });

  describe('Status Indicator', () => {
    it('displays enabled status when status is "enabled"', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          status: 'enabled',
        },
      });

      const statusIndicator = wrapper.find('.status-indicator');
      expect(statusIndicator.exists()).toBe(true);
      expect(statusIndicator.text()).toBe('Enabled');
      expect(statusIndicator.classes()).toContain('status-active');
    });

    it('displays disabled status when status is "disabled"', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          status: 'disabled',
        },
      });

      const statusIndicator = wrapper.find('.status-indicator');
      expect(statusIndicator.exists()).toBe(true);
      expect(statusIndicator.text()).toBe('Disabled');
      expect(statusIndicator.classes()).toContain('status-disabled');
    });

    it('does not display status indicator when status is null', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          status: null,
        },
      });

      expect(wrapper.find('.status-indicator').exists()).toBe(false);
    });

    it('does not display status indicator when status is undefined', () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
        // status defaults to null
      });

      expect(wrapper.find('.status-indicator').exists()).toBe(false);
    });
  });

  describe('Tab Navigation', () => {
    const mockTabs = [
      { id: 'details', label: 'Details' },
      { id: 'history', label: 'History' },
    ];

    it('displays tab navigation when tabs prop is provided', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          tabs: mockTabs,
        },
      });

      expect(wrapper.findComponent({ name: 'TabNavigation' }).exists()).toBe(true);
    });

    it('does not display tab navigation when tabs is undefined', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          tabs: undefined,
        },
      });

      expect(wrapper.findComponent({ name: 'TabNavigation' }).exists()).toBe(false);
    });

    it('passes tabs prop to TabNavigation component', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          tabs: mockTabs,
        },
      });

      const tabNav = wrapper.findComponent({ name: 'TabNavigation' });
      expect(tabNav.props('tabs')).toEqual(mockTabs);
    });

    it('passes activeTab prop to TabNavigation component', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          tabs: mockTabs,
          activeTab: 'history',
        },
      });

      const tabNav = wrapper.findComponent({ name: 'TabNavigation' });
      expect(tabNav.props('activeTab')).toBe('history');
    });

    it('uses default activeTab value of "details"', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          tabs: mockTabs,
        },
      });

      const tabNav = wrapper.findComponent({ name: 'TabNavigation' });
      expect(tabNav.props('activeTab')).toBe('details');
    });

    it('emits tab-change event when tab is changed', async () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          tabs: mockTabs,
        },
      });

      const tabNav = wrapper.findComponent({ name: 'TabNavigation' });
      await tabNav.vm.$emit('tab-change', 'history');

      expect(wrapper.emitted('tab-change')).toBeTruthy();
      expect(wrapper.emitted('tab-change')?.[0]).toEqual(['history']);
    });
  });

  describe('Tooltip Functionality', () => {
    it('calls showTooltip when mouse enters truncated title', async () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
      });

      const titleElement = wrapper.find('.page-title');
      const element = titleElement.element as HTMLElement;

      // Mock the element dimensions to simulate truncation
      Object.defineProperty(element, 'scrollWidth', { value: 800, writable: true });
      Object.defineProperty(element, 'clientWidth', { value: 600, writable: true });

      await titleElement.trigger('mouseenter');
      await nextTick();

      expect(mockShowTooltip).toHaveBeenCalledTimes(1);
    });

    it('does not call showTooltip when mouse enters non-truncated title', async () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
      });

      const titleElement = wrapper.find('.page-title');
      const element = titleElement.element as HTMLElement;

      // Mock the element dimensions to simulate no truncation
      Object.defineProperty(element, 'scrollWidth', { value: 400, writable: true });
      Object.defineProperty(element, 'clientWidth', { value: 600, writable: true });

      await titleElement.trigger('mouseenter');
      await nextTick();

      expect(mockShowTooltip).not.toHaveBeenCalled();
    });

    it('calls hideTooltip when mouse leaves title', async () => {
      wrapper = mount(DetailsPageHeader, {
        props: defaultProps,
      });

      const titleElement = wrapper.find('.page-title');

      await titleElement.trigger('mouseleave');
      await nextTick();

      expect(mockHideTooltip).toHaveBeenCalledTimes(1);
    });

    it('shows loading text in tooltip when title is truncated during loading', async () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          loading: true,
        },
      });

      const titleElement = wrapper.find('.page-title');
      const element = titleElement.element as HTMLElement;

      // Mock truncation
      Object.defineProperty(element, 'scrollWidth', { value: 800, writable: true });
      Object.defineProperty(element, 'clientWidth', { value: 600, writable: true });

      await titleElement.trigger('mouseenter');
      await nextTick();

      expect(mockShowTooltip).toHaveBeenCalledWith(expect.any(MouseEvent), 'Loading...');
    });

    it('shows error text in tooltip when title is truncated during error', async () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          ...defaultProps,
          error: 'Error message',
        },
      });

      const titleElement = wrapper.find('.page-title');
      const element = titleElement.element as HTMLElement;

      // Mock truncation
      Object.defineProperty(element, 'scrollWidth', { value: 800, writable: true });
      Object.defineProperty(element, 'clientWidth', { value: 600, writable: true });

      await titleElement.trigger('mouseenter');
      await nextTick();

      expect(mockShowTooltip).toHaveBeenCalledWith(expect.any(MouseEvent), 'Error loading data');
    });
  });

  describe('Complete Header Rendering', () => {
    it('renders all elements when all props are provided', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          title: 'Complete Header',
          loading: false,
          error: null,
          icon: 'folder',
          version: '1.2.3',
          status: 'enabled',
          showBackButton: true,
          backButtonLabel: 'Back',
          tabs: [
            { id: 'details', label: 'Details' },
            { id: 'settings', label: 'Settings' },
          ],
          activeTab: 'details',
        },
      });

      expect(wrapper.find('.page-header').exists()).toBe(true);
      expect(wrapper.find('.back-button').exists()).toBe(true);
      expect(wrapper.find('.page-title').text()).toBe('Complete Header');
      expect(wrapper.find('.icon-container').exists()).toBe(true);
      expect(wrapper.find('.version-badge').text()).toBe('v1.2.3');
      expect(wrapper.find('.status-indicator').text()).toBe('Enabled');
      expect(wrapper.findComponent({ name: 'TabNavigation' }).exists()).toBe(true);
    });

    it('renders minimal header with only required props', () => {
      wrapper = mount(DetailsPageHeader, {
        props: {
          title: 'Minimal Header',
          showBackButton: false,
        },
      });

      expect(wrapper.find('.page-header').exists()).toBe(true);
      expect(wrapper.find('.page-title').text()).toBe('Minimal Header');
      expect(wrapper.find('.back-button').exists()).toBe(false);
      expect(wrapper.find('.icon-container').exists()).toBe(false);
      expect(wrapper.find('.version-badge').exists()).toBe(false);
      expect(wrapper.find('.status-indicator').exists()).toBe(false);
      expect(wrapper.findComponent({ name: 'TabNavigation' }).exists()).toBe(false);
    });
  });
});
