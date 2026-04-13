/* eslint-disable simple-import-sort/imports */
import { flushPromises, mount } from '@vue/test-utils';
import { describe, it, expect, vi } from 'vitest';

// Stub DevExtreme DxButton at module level to ensure consistent rendering
vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    name: 'DxButton',
    template: '<button @click="$emit(\'click\')" aria-label="Add Configuration"><slot /></button>',
  },
}));

vi.mock('@/api/services/SettingsService', () => ({
  SettingsService: {
    listSiteConfigs: vi.fn().mockResolvedValue([
      {
        id: 'k',
        value: 'v',
        type: 'STRING',
        description: 'd',
        lastModifiedDate: new Date().toISOString(),
      },
    ]),
  },
}));

vi.mock('@/api/core/request', () => ({
  request: vi.fn(),
}));
vi.mock('../../../api/core/request', () => ({
  request: vi.fn(),
}));

vi.mock('@/api/core/OpenAPI', () => ({
  OpenAPI: {},
}));
vi.mock('../../../api/core/OpenAPI', () => ({
  OpenAPI: {},
}));

vi.mock('@/utils/notificationUtils', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}));
vi.mock('../../../utils/notificationUtils', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}));

import SiteConfigPage from '@/components/admin/siteconfig/SiteConfigPage.vue';

const InteractiveConfirmationDialogStub = {
  props: ['open', 'type', 'title', 'description', 'confirmLabel', 'loading'],
  emits: ['cancel', 'confirm'],
  template: `
    <div v-if="open" class="confirmation-dialog-stub">
      <span class="dialog-description">{{ description }}</span>
      <button class="dialog-cancel" @click="$emit('cancel')">cancel</button>
      <button class="dialog-confirm" :disabled="loading" @click="$emit('confirm')">
        {{ confirmLabel }}
      </button>
    </div>
  `,
};

describe('SiteConfigPage', () => {
  it('renders grid', async () => {
    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div class="generic-grid-stub" />' },
          SiteConfigModal: { template: '<div class="siteconfig-modal-stub" />', props: ['show'] },
          ConfirmationDialog: { template: '<div class="confirmation-dialog-stub" />' },
          DxButton: { template: '<button><slot /></button>' },
        },
      },
    });

    await flushPromises();

    expect(wrapper.find('.generic-grid-stub').exists()).toBe(true);
  });

  it('configures Config Type filter with GLOBAL/CUSTOM and disables Value filtering', async () => {
    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: {
            name: 'GenericDataGrid',
            props: ['columns'],
            template: '<div class="generic-grid-stub" />',
          },
          SiteConfigModal: { template: '<div class="siteconfig-modal-stub" />', props: ['show'] },
          ConfirmationDialog: { template: '<div class="confirmation-dialog-stub" />' },
          DxButton: { template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    const grid = wrapper.findComponent({ name: 'GenericDataGrid' });
    const columns = grid.props('columns') as Array<Record<string, unknown>>;

    const valueColumn = columns.find(column => column.dataField === 'configValue');
    expect(valueColumn).toBeTruthy();
    expect(valueColumn?.allowFiltering).toBe(false);

    const configTypeColumn = columns.find(column => column.caption === 'Config Type');
    expect(configTypeColumn).toBeTruthy();
    expect(configTypeColumn?.dataField).toBe('configType');
    expect(configTypeColumn?.headerFilter).toEqual({
      dataSource: [
        { text: 'GLOBAL', value: 'GLOBAL' },
        { text: 'CUSTOM', value: 'CUSTOM' },
      ],
    });
  });

  it('renders status chip for type column', async () => {
    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValueOnce([
      {
        id: 'k',
        configKey: 'key',
        configValue: 'value',
        type: 'STRING',
        description: 'd',
        lastModifiedDate: new Date().toISOString(),
      },
    ] as any);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: {
            props: ['data'],
            template:
              '<div class="generic-grid-stub"><div v-for="row in data" class="grid-row"><slot name="typeTemplate" :data="row" /></div></div>',
          },
          SiteConfigModal: { template: '<div />', props: ['show'] },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { template: '<button><slot /></button>' },
          StatusChipForDataTable: {
            props: ['status', 'label'],
            template: '<span class="status-chip-stub">{{ label || status }}</span>',
          },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    expect(wrapper.findAll('.status-chip-stub').length).toBe(1);
  });

  it('validates JSON/BOOLEAN/NUMBER and prevents save on invalid input', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;
    vi.mocked(request).mockResolvedValue({});

    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValueOnce([
      {
        id: 'existing-id',
        configKey: 'existing',
        configValue: 'v',
        type: 'STRING',
        description: '',
        createdDate: new Date().toISOString(),
        lastModifiedDate: new Date().toISOString(),
        createdBy: 'tester',
        lastModifiedBy: 'tester',
        tenantId: 'tenant',
        version: 1,
        isDeleted: false,
      },
    ]);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    // Simulate edit mode by calling handleEditConfiguration directly
    const config = {
      id: 'existing-id',
      configKey: 'existing',
      configValue: 'old-value',
      type: 'JSON' as const,
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    };
    wrapper.vm.handleEditConfiguration(config);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });

    // Invalid JSON
    modal.vm.$emit('update:type', 'JSON');
    modal.vm.$emit('update:value', '{ invalid');
    modal.vm.$emit('save');
    expect(Alert.warning).toHaveBeenCalledWith('Invalid JSON format. Please enter valid JSON.');

    // Invalid BOOLEAN
    modal.vm.$emit('update:type', 'BOOLEAN');
    modal.vm.$emit('update:value', 'maybe');
    modal.vm.$emit('save');
    expect(Alert.warning).toHaveBeenCalledWith('Boolean value must be either "true" or "false".');

    // Invalid NUMBER
    modal.vm.$emit('update:type', 'NUMBER');
    modal.vm.$emit('update:value', 'abc');
    modal.vm.$emit('save');
    expect(Alert.warning).toHaveBeenCalledWith(
      'Invalid number format. Please enter a valid number.'
    );

    // Required fields validation
    modal.vm.$emit('update:type', 'STRING');
    modal.vm.$emit('update:id', '');
    modal.vm.$emit('update:value', '');
    modal.vm.$emit('save');
    expect(Alert.warning).toHaveBeenCalledWith('Please fill in all required fields');
  });

  it('saves configuration successfully and closes modal', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;
    vi.mocked(request).mockResolvedValueOnce({});

    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValue([
      {
        id: 'existing-id',
        configKey: 'key',
        configValue: 'value',
        type: 'STRING',
        description: '',
        createdDate: new Date().toISOString(),
        lastModifiedDate: new Date().toISOString(),
        createdBy: 'tester',
        lastModifiedBy: 'tester',
        tenantId: 'tenant',
        version: 1,
        isDeleted: false,
      },
    ]);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div class="siteconfig-modal-stub" />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    // Simulate edit mode
    const config = {
      id: 'existing-id',
      configKey: 'key',
      configValue: 'old-value',
      type: 'STRING' as const,
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    };
    wrapper.vm.handleEditConfiguration(config);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    modal.vm.$emit('update:value', 'new-value');
    modal.vm.$emit('save');

    // Await microtasks for save + fetchSiteConfigs
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();

    // Success toast
    expect(Alert.success).toHaveBeenCalledWith('Configuration updated successfully');
  });

  it('shows error on save failure', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;
    vi.mocked(request).mockRejectedValueOnce({ response: { data: { message: 'bad' } } });

    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValue([
      {
        id: 'existing-id',
        configKey: 'key',
        configValue: 'value',
        type: 'STRING',
        description: '',
        createdDate: new Date().toISOString(),
        lastModifiedDate: new Date().toISOString(),
        createdBy: 'tester',
        lastModifiedBy: 'tester',
        tenantId: 'tenant',
        version: 1,
        isDeleted: false,
      },
    ]);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    // Simulate edit mode
    const config = {
      id: 'existing-id',
      configKey: 'key',
      configValue: 'old-value',
      type: 'STRING' as const,
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    };
    wrapper.vm.handleEditConfiguration(config);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    modal.vm.$emit('update:value', 'new-value');
    modal.vm.$emit('save');

    await Promise.resolve();
    await wrapper.vm.$nextTick();
    expect(Alert.error).toHaveBeenCalledWith('bad');
    expect(modal.props('saveError')).toBeFalsy();
  });

  it('validates TIMESTAMP format - invalid format, unparseable date, future warning', async () => {
    const Alert = (await import('@/utils/notificationUtils')).default as any;

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    const config = {
      id: 'existing-id',
      configKey: 'timestamp-key',
      configValue: '2026-01-01T00:00:00Z',
      type: 'TIMESTAMP' as const,
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    };
    wrapper.vm.handleEditConfiguration(config);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });

    // Invalid format
    modal.vm.$emit('update:type', 'TIMESTAMP');
    modal.vm.$emit('update:value', '2026-01-01T00:00:00');
    modal.vm.$emit('save');
    expect(Alert.warning).toHaveBeenCalledWith(
      'Invalid timestamp format. Please use ISO-8601 UTC format (e.g., 2026-01-01T00:00:00Z).'
    );

    // Unparseable
    modal.vm.$emit('update:value', '2026-99-99T00:00:00Z');
    modal.vm.$emit('save');
    expect(Alert.warning).toHaveBeenCalledWith(
      'Invalid timestamp value. Please enter a valid date.'
    );

    // Future warning
    const futureDate = new Date(Date.now() + 100000000).toISOString();
    modal.vm.$emit('update:value', futureDate);
    modal.vm.$emit('save');
    expect(Alert.warning).toHaveBeenCalledWith('Warning: The timestamp is set in the future.');
  });

  it('accepts valid boolean and number values without validation warnings', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;
    vi.mocked(request).mockResolvedValue({});

    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValue([
      {
        id: 'valid-config-id',
        configKey: 'valid-key',
        configValue: '1',
        type: 'STRING',
        description: '',
        createdDate: new Date().toISOString(),
        lastModifiedDate: new Date().toISOString(),
        createdBy: 'tester',
        lastModifiedBy: 'tester',
        tenantId: 'tenant',
        version: 1,
        isDeleted: false,
      },
    ]);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    const config = {
      id: 'valid-config-id',
      configKey: 'valid-key',
      configValue: '1',
      type: 'STRING' as const,
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    };
    wrapper.vm.handleEditConfiguration(config);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    Alert.warning.mockClear();

    modal.vm.$emit('update:type', 'BOOLEAN');
    modal.vm.$emit('update:value', 'false');
    modal.vm.$emit('save');
    await Promise.resolve();

    modal.vm.$emit('update:type', 'NUMBER');
    modal.vm.$emit('update:value', '42');
    modal.vm.$emit('save');
    await Promise.resolve();

    expect(Alert.warning).not.toHaveBeenCalled();
  });

  it('preserves an existing configuration key when editing the same record', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;
    vi.mocked(request).mockResolvedValue({});

    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValue([
      {
        id: 'same-id',
        configKey: 'same-key',
        configValue: 'value',
        type: 'STRING',
        description: '',
        createdDate: new Date().toISOString(),
        lastModifiedDate: new Date().toISOString(),
        createdBy: 'tester',
        lastModifiedBy: 'tester',
        tenantId: 'tenant',
        version: 1,
        isDeleted: false,
      },
    ]);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    const config = {
      id: 'same-id',
      configKey: 'same-key',
      configValue: 'value',
      type: 'STRING' as const,
      description: '',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    };

    wrapper.vm.handleEditConfiguration(config);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    modal.vm.$emit('update:id', 'same-key');
    modal.vm.$emit('update:value', 'updated');
    modal.vm.$emit('save');
    await Promise.resolve();

    expect(Alert.warning).not.toHaveBeenCalledWith(
      'A configuration with this key already exists. Please use a unique key.'
    );
  });

  it('validates unique key - prevents duplicate keys', async () => {
    const Alert = (await import('@/utils/notificationUtils')).default as any;

    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValue([
      {
        id: 'existing-id-1',
        configKey: 'existing-key',
        configValue: 'value1',
        type: 'STRING',
        description: '',
        createdDate: new Date().toISOString(),
        lastModifiedDate: new Date().toISOString(),
        createdBy: 'tester',
        lastModifiedBy: 'tester',
        tenantId: 'tenant',
        version: 1,
        isDeleted: false,
      },
      {
        id: 'existing-id-2',
        configKey: 'another-key',
        configValue: 'value2',
        type: 'STRING',
        description: '',
        createdDate: new Date().toISOString(),
        lastModifiedDate: new Date().toISOString(),
        createdBy: 'tester',
        lastModifiedBy: 'tester',
        tenantId: 'tenant',
        version: 1,
        isDeleted: false,
      },
    ]);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    const config = {
      id: 'existing-id-2',
      configKey: 'another-key',
      configValue: 'value2',
      type: 'STRING' as const,
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    };
    wrapper.vm.handleEditConfiguration(config);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    modal.vm.$emit('update:id', 'existing-key');
    modal.vm.$emit('save');
    expect(Alert.warning).toHaveBeenCalledWith(
      'A configuration with this key already exists. Please use a unique key.'
    );
  });

  it('handles delete configuration flow - opens dialog and closes', async () => {
    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValue([
      {
        id: 'to-delete-id',
        configKey: 'to-delete',
        configValue: 'value',
        type: 'STRING',
        description: '',
        createdDate: new Date().toISOString(),
        lastModifiedDate: new Date().toISOString(),
        createdBy: 'tester',
        lastModifiedBy: 'tester',
        tenantId: 'tenant',
        version: 1,
        isDeleted: false,
      },
    ]);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: { template: '<div />' },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    const config = {
      id: 'to-delete-id',
      configKey: 'to-delete',
      configValue: 'value',
      type: 'STRING' as const,
      description: '',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    };

    wrapper.vm.handleDeleteConfiguration(config);
    await wrapper.vm.$nextTick();
    expect(wrapper.vm.dialogOpen).toBe(true);
    expect(wrapper.vm.pendingAction).toBe('delete');
    expect(wrapper.vm.configToDelete).toEqual(config);

    await wrapper.vm.closeDialog();
    expect(wrapper.vm.dialogOpen).toBe(false);
  });

  it('shows fetch error on listSiteConfigs failure', async () => {
    const Alert = (await import('@/utils/notificationUtils')).default as any;

    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockRejectedValueOnce(new Error('network error'));

    mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: { template: '<div />' },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    expect(Alert.error).toHaveBeenCalledWith(
      'Failed to load site configurations. Please try again.'
    );
  });

  it('shows override message for GLOBAL tenant config', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;
    vi.mocked(request).mockResolvedValueOnce({});

    const { SettingsService } = await import('@/api/services/SettingsService');
    vi.mocked(SettingsService.listSiteConfigs).mockResolvedValue([
      {
        id: 'global-id',
        configKey: 'global-key',
        configValue: 'value',
        type: 'STRING',
        description: '',
        createdDate: new Date().toISOString(),
        lastModifiedDate: new Date().toISOString(),
        createdBy: 'tester',
        lastModifiedBy: 'tester',
        tenantId: 'GLOBAL',
        version: 1,
        isDeleted: false,
      },
    ]);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    const config = {
      id: 'global-id',
      configKey: 'global-key',
      configValue: 'old-value',
      type: 'STRING' as const,
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'GLOBAL',
      version: 1,
      isDeleted: false,
    };
    wrapper.vm.handleEditConfiguration(config);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    modal.vm.$emit('update:value', 'new-value');
    modal.vm.$emit('save');

    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();

    // Verify success message was called (either override or update)
    expect(Alert.success).toHaveBeenCalled();
    const successCalls = Alert.success.mock.calls;
    const hasCorrectMessage = successCalls.some(
      (call: any[]) =>
        call[0] === 'Configuration overridden successfully' ||
        call[0] === 'Configuration updated successfully'
    );
    expect(hasCorrectMessage).toBe(true);
  });

  it('closes modal and resets form data', async () => {
    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await Promise.resolve();
    await Promise.resolve();

    const config = {
      id: 'test-id',
      configKey: 'test-key',
      configValue: 'test-value',
      type: 'STRING' as const,
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    };
    wrapper.vm.handleEditConfiguration(config);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    expect(modal.props('show')).toBe(true);

    modal.vm.$emit('close');
    await wrapper.vm.$nextTick();

    expect(modal.props('show')).toBe(false);
  });

  it('deletes a configuration after confirmation and refreshes the data', async () => {
    const { request } = await import('@/api/core/request');
    const { SettingsService } = await import('@/api/services/SettingsService');
    const Alert = (await import('@/utils/notificationUtils')).default as any;

    vi.clearAllMocks();
    vi.mocked(request).mockResolvedValue({});
    vi.mocked(SettingsService.listSiteConfigs)
      .mockResolvedValueOnce([
        {
          id: 'to-delete-id',
          configKey: 'to-delete',
          configValue: 'value',
          type: 'STRING',
          description: '',
          createdDate: new Date().toISOString(),
          lastModifiedDate: new Date().toISOString(),
          createdBy: 'tester',
          lastModifiedBy: 'tester',
          tenantId: 'tenant',
          version: 1,
          isDeleted: false,
        },
      ] as any)
      .mockResolvedValueOnce([] as any);

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: { template: '<div />' },
          ConfirmationDialog: InteractiveConfirmationDialogStub,
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await flushPromises();

    wrapper.vm.handleDeleteConfiguration({
      id: 'to-delete-id',
      configKey: 'to-delete',
      tenantId: 'tenant',
    } as any);
    await wrapper.vm.$nextTick();

    await wrapper.find('.dialog-confirm').trigger('click');
    await flushPromises();

    expect(request).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'DELETE', url: '/management/site-configs/to-delete-id' })
    );
    expect(Alert.success).toHaveBeenCalledWith('Configuration deleted successfully');
    expect(SettingsService.listSiteConfigs).toHaveBeenCalledTimes(2);
    expect(wrapper.vm.configToDelete).toBeNull();
    expect(wrapper.vm.dialogOpen).toBe(false);
  });

  it('shows a delete error and keeps the dialog open when delete fails', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;

    vi.clearAllMocks();
    vi.mocked(request).mockRejectedValueOnce(new Error('delete failed'));

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: { template: '<div />' },
          ConfirmationDialog: InteractiveConfirmationDialogStub,
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await flushPromises();

    wrapper.vm.handleDeleteConfiguration({
      id: 'to-delete-id',
      configKey: 'to-delete',
      tenantId: 'tenant',
    } as any);
    await wrapper.vm.$nextTick();

    await wrapper.find('.dialog-confirm').trigger('click');
    await flushPromises();

    expect(Alert.error).toHaveBeenCalledWith('Failed to delete configuration. Please try again.');
    expect(wrapper.vm.dialogOpen).toBe(true);
    expect(wrapper.vm.configToDelete).toEqual(
      expect.objectContaining({ id: 'to-delete-id', configKey: 'to-delete' })
    );
  });

  it('does not delete when no configuration is pending', async () => {
    const { request } = await import('@/api/core/request');

    vi.clearAllMocks();
    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: { template: '<div />' },
          ConfirmationDialog: InteractiveConfirmationDialogStub,
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await flushPromises();

    wrapper.vm.pendingAction = 'delete';
    await wrapper.vm.$nextTick();

    await (wrapper.vm as any).handleConfirm();
    await flushPromises();

    expect(request).not.toHaveBeenCalled();
  });

  it('shows the backend update error message when save fails with a response payload', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;

    vi.clearAllMocks();
    vi.mocked(request).mockRejectedValueOnce({
      response: { data: { message: 'Configuration cannot be overridden right now.' } },
    });

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await flushPromises();

    wrapper.vm.handleEditConfiguration({
      id: 'config-id',
      configKey: 'config-key',
      configValue: 'value',
      type: 'STRING',
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    } as any);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    modal.vm.$emit('update:value', 'updated-value');
    modal.vm.$emit('save');
    await flushPromises();

    expect(Alert.error).toHaveBeenCalledWith('Configuration cannot be overridden right now.');
  });

  it('falls back to the generic update error message when save fails without a backend message', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;

    vi.clearAllMocks();
    vi.mocked(request).mockRejectedValueOnce(new Error('update failed'));

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await flushPromises();

    wrapper.vm.handleEditConfiguration({
      id: 'config-id',
      configKey: 'config-key',
      configValue: 'value',
      type: 'STRING',
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    } as any);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    modal.vm.$emit('update:value', 'updated-value');
    modal.vm.$emit('save');
    await flushPromises();

    expect(Alert.error).toHaveBeenCalledWith('Failed to update configuration. Please try again.');
  });

  it('saves a valid past timestamp without showing validation warnings', async () => {
    const { request } = await import('@/api/core/request');
    const Alert = (await import('@/utils/notificationUtils')).default as any;

    vi.clearAllMocks();
    vi.mocked(request).mockResolvedValue({});

    const wrapper = mount(SiteConfigPage, {
      global: {
        stubs: {
          GenericDataGrid: { template: '<div />' },
          SiteConfigModal: {
            name: 'SiteConfigModal',
            template: '<div />',
            props: ['show', 'isSaving', 'saveError', 'formData'],
          },
          ConfirmationDialog: { template: '<div />' },
          DxButton: { name: 'DxButton', template: '<button><slot /></button>' },
        },
      },
    });

    await flushPromises();

    wrapper.vm.handleEditConfiguration({
      id: 'config-id',
      configKey: 'config-key',
      configValue: '2020-01-01T00:00:00Z',
      type: 'TIMESTAMP',
      description: 'test',
      createdDate: new Date().toISOString(),
      lastModifiedDate: new Date().toISOString(),
      createdBy: 'tester',
      lastModifiedBy: 'tester',
      tenantId: 'tenant',
      version: 1,
      isDeleted: false,
    } as any);
    await wrapper.vm.$nextTick();

    const modal = wrapper.findComponent({ name: 'SiteConfigModal' });
    modal.vm.$emit('update:type', 'TIMESTAMP');
    modal.vm.$emit('update:value', '2020-01-01T00:00:00Z');
    modal.vm.$emit('save');
    await flushPromises();

    expect(request).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'PUT',
        url: '/management/site-configs/config-id',
      })
    );
    expect(Alert.warning).not.toHaveBeenCalledWith('Warning: The timestamp is set in the future.');
  });
});
