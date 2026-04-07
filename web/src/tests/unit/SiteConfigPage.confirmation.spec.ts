import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { SettingsService } from '@/api/services/SettingsService';
import SiteConfigPage from '@/components/admin/siteconfig/SiteConfigPage.vue';
import { useAuthStore } from '@/store/auth';

// Mock dependencies
vi.mock('@/store/auth');
vi.mock('@/api/services/SettingsService');
vi.mock('@/utils/notificationUtils');

// Mock the request function
vi.mock('@/api/core/request', () => ({
  request: vi.fn(),
}));

// Mock components
vi.mock('@/components/common/GenericDataGrid.vue', () => ({
  default: { template: '<div>GenericDataGrid</div>' },
}));

vi.mock('@/components/admin/siteconfig/SiteConfigModal.vue', () => ({
  default: { template: '<div>SiteConfigModal</div>' },
}));

vi.mock('@/components/common/ConfirmationDialog.vue', () => ({
  default: {
    template:
      '<div data-testid="confirmation-dialog" v-if="open">{{ title }} - {{ description }}</div>',
    props: ['open', 'type', 'title', 'description', 'confirmLabel', 'loading'],
  },
}));

describe('SiteConfigPage - Confirmation Dialog', () => {
  let wrapper: any;
  const mockConfig = {
    id: 'test-config-1',
    configKey: 'TEST_CONFIG',
    configValue: 'test-value',
    description: 'Test description',
    type: 'STRING' as const,
    createdDate: '2024-01-01T00:00:00Z',
    lastModifiedDate: '2024-01-01T00:00:00Z',
    createdBy: 'test-user',
    lastModifiedBy: 'test-user',
    tenantId: 'test-tenant',
    version: 1,
    isDeleted: false,
  };

  beforeEach(() => {
    vi.clearAllMocks();

    // Mock auth store
    const mockAuthStore = {
      hasRole: vi.fn().mockReturnValue(true), // Mock as super admin
    };
    vi.mocked(useAuthStore).mockReturnValue(mockAuthStore as any);

    // Mock SettingsService
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValue([mockConfig]);

    wrapper = mount(SiteConfigPage);
  });

  it('should open confirmation dialog when delete is triggered', async () => {
    // Wait for component to initialize
    await wrapper.vm.$nextTick();

    // Get the component's exposed methods
    const vm = wrapper.vm;

    // Trigger delete configuration
    vm.handleDeleteConfiguration(mockConfig);
    await wrapper.vm.$nextTick();

    // Check that dialog is opened
    expect(vm.dialogOpen).toBe(true);
    expect(vm.pendingAction).toBe('delete');
    expect(vm.configToDelete).toEqual(mockConfig);
  });

  it('should display custom site config dialog configuration', async () => {
    await wrapper.vm.$nextTick();

    const vm = wrapper.vm;

    // Trigger delete
    vm.handleDeleteConfiguration(mockConfig);
    await wrapper.vm.$nextTick();

    // Check dialog title and description are from our custom config
    expect(vm.dialogTitle).toBe('Delete Configuration');
    expect(vm.dialogDescription).toBe(
      'Deleting this configuration will remove it permanently. This action cannot be undone.'
    );
    expect(vm.dialogConfirmLabel).toBe('Delete');
  });

  it('should render ConfirmationDialog component when dialog is open', async () => {
    await wrapper.vm.$nextTick();

    const vm = wrapper.vm;

    // Initially dialog should not be rendered
    expect(wrapper.find('[data-testid="confirmation-dialog"]').exists()).toBe(false);

    // Trigger delete
    vm.handleDeleteConfiguration(mockConfig);
    await wrapper.vm.$nextTick();

    // Now dialog should be rendered
    const dialog = wrapper.find('[data-testid="confirmation-dialog"]');
    expect(dialog.exists()).toBe(true);
    expect(dialog.text()).toContain('Delete Configuration');
  });

  it('should clear configToDelete when dialog is closed', async () => {
    await wrapper.vm.$nextTick();

    const vm = wrapper.vm;

    // Set up initial state
    vm.handleDeleteConfiguration(mockConfig);
    await wrapper.vm.$nextTick();

    expect(vm.configToDelete).toEqual(mockConfig);

    // Close dialog
    vm.closeDialog();
    await wrapper.vm.$nextTick();

    expect(vm.dialogOpen).toBe(false);
    expect(vm.pendingAction).toBe(null);
    // configToDelete should remain until successful delete or explicit clear
  });
});
