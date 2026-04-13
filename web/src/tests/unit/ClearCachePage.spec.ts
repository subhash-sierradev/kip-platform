/* eslint-disable simple-import-sort/imports */
import { mount, flushPromises } from '@vue/test-utils';
import { nextTick } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

interface TextReadable {
  text: () => string;
}

const hoisted = vi.hoisted(() => ({
  showTooltipMock: vi.fn(),
  moveTooltipMock: vi.fn(),
  hideTooltipMock: vi.fn(),
}));

vi.mock('@/api/services/SettingsService');
vi.mock('@/utils/notificationUtils', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));
vi.mock('@/composables/useTooltip', () => ({
  useTooltip: () => ({
    tooltip: { visible: false, text: '', x: 0, y: 0 },
    showTooltip: hoisted.showTooltipMock,
    moveTooltip: hoisted.moveTooltipMock,
    hideTooltip: hoisted.hideTooltipMock,
  }),
}));

import ClearCachePage from '@/components/admin/cache/ClearCachePage.vue';
import { SettingsService } from '@/api/services/SettingsService';
import Alert from '@/utils/notificationUtils';

const GenericDataGridStub = {
  name: 'GenericDataGrid',
  props: ['data'],
  template: `
    <div class="generic-grid-stub">
      <div v-for="row in data" :key="row.id" class="generic-grid-row">
        <div class="generic-grid-name"><slot name="nameTemplate" :data="row" /></div>
        <div class="generic-grid-key"><slot name="cacheKeyTemplate" :data="row" /></div>
        <div class="generic-grid-count"><slot name="countTemplate" :data="row" /></div>
        <div class="generic-grid-actions"><slot name="actionsTemplate" :data="row" /></div>
      </div>
    </div>
  `,
};

const stubs = {
  ConfirmationDialog: {
    template: `
      <div v-if="open" class="confirmation-dialog" :data-type="type">
        <div class="dialog-title">{{ title }}</div>
        <div class="dialog-description">{{ description }}</div>
        <button class="cancel-btn" @click="$emit('cancel')">Cancel</button>
        <button class="confirm-btn" :disabled="loading" @click="$emit('confirm')">{{ confirmLabel }}</button>
      </div>
    `,
    props: ['open', 'type', 'title', 'description', 'confirmLabel', 'loading', 'maxWidth'],
    emits: ['cancel', 'confirm'],
  },
  DxButton: {
    name: 'DxButton',
    template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ text }}<slot /></button>',
    props: ['text', 'type', 'disabled', 'icon', 'stylingMode', 'elementAttr'],
    emits: ['click'],
  },
  DxLoadPanel: {
    template: '<div v-if="visible" class="dx-loadpanel">{{ message }}</div>',
    props: ['visible', 'showIndicator', 'showPane', 'shading', 'message'],
  },
  DxLoadIndicator: {
    name: 'DxLoadIndicator',
    template: '<div class="dx-load-indicator-stub"></div>',
  },
  DxTextBox: {
    name: 'DxTextBox',
    template: '<input :value="value" @input="$emit(\'update:value\', $event.target.value)" />',
    props: ['value', 'mode', 'placeholder', 'showClearButton', 'valueChangeEvent'],
    emits: ['update:value'],
  },
  GenericDataGrid: GenericDataGridStub,
  CommonTooltip: { template: '<div class="tooltip-stub"></div>' },
};

const mockCacheStats = {
  UserCache: { size: 10, requestCount: 5, hitRate: 0.6, missRate: 0.4 },
  JobCache: { size: 2, requestCount: 1, hitRate: 1, missRate: 0 },
  SessionCache: { size: 5, requestCount: 10, hitRate: 0.5, missRate: 0.5 },
};

const mountPage = () => mount(ClearCachePage, { global: { stubs } }) as any;

async function settle() {
  await flushPromises();
  await nextTick();
}

describe('ClearCachePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockResolvedValue(
      mockCacheStats
    );
    (SettingsService.clearCacheByName as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);
    (SettingsService.clearAllCaches as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);
  });

  it('shows the loading panel while cache stats are still loading', () => {
    (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockReturnValue(
      new Promise(() => {})
    );

    const wrapper = mountPage();

    expect(wrapper.vm.isStatsLoading).toBe(true);
  });

  it('renders grid rows with formatted labels in alphabetical order', async () => {
    const wrapper = mountPage();
    await settle();

    const labels = wrapper.findAll('.cache-name-text').map((node: TextReadable) => node.text());
    expect(labels).toEqual(['Job Cache', 'Session Cache', 'User Cache']);
    expect(wrapper.findAll('.count-badge').map((node: TextReadable) => node.text())).toEqual([
      '2',
      '5',
      '10',
    ]);
    expect(wrapper.findAll('.cache-action-btn')).toHaveLength(3);
  });

  it('shows zero when a cache size is null', async () => {
    (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      EmptyCache: { size: null, requestCount: 0, hitRate: 0, missRate: 0 },
    });

    const wrapper = mountPage();
    await settle();

    expect(wrapper.find('.count-badge').text()).toBe('0');
  });

  it('filters the visible rows from the search box and clears the filter', async () => {
    const wrapper = mountPage();
    await settle();

    await wrapper.find('input').setValue('session');
    await nextTick();

    expect(wrapper.findAll('.generic-grid-row')).toHaveLength(1);

    await wrapper.find('.clear-filters-btn').trigger('click');
    await nextTick();

    expect(wrapper.findAll('.generic-grid-row')).toHaveLength(3);
  });

  it('shows the no matching state when search text filters out every cache', async () => {
    const wrapper = mountPage();
    await settle();

    await wrapper.find('input').setValue('missing-cache');
    await nextTick();

    expect(wrapper.find('.table-empty-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('No matching cache items');
  });

  it('filters rows by cache key as well as label', async () => {
    const wrapper = mountPage();
    await settle();

    await wrapper.find('input').setValue('usercache');
    await nextTick();

    const labels = wrapper.findAll('.cache-name-text').map((node: TextReadable) => node.text());
    expect(labels).toEqual(['User Cache']);
  });

  it('wires tooltip handlers for filter and action affordances', async () => {
    const wrapper = mountPage();
    await settle();

    await wrapper
      .find('.clear-filters-trigger')
      .trigger('mouseenter', { clientX: 20, clientY: 30 });
    await wrapper.find('.clear-filters-trigger').trigger('mousemove', { clientX: 25, clientY: 35 });
    await wrapper.find('.clear-filters-trigger').trigger('mouseleave');

    await wrapper.find('.cache-action-trigger').trigger('mouseenter', { clientX: 80, clientY: 90 });
    await wrapper.find('.cache-action-trigger').trigger('mousemove', { clientX: 85, clientY: 95 });
    await wrapper.find('.cache-action-trigger').trigger('mouseleave');

    expect(hoisted.showTooltipMock).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({ clientX: 20, clientY: 30 }),
      'Reset Filters'
    );
    expect(hoisted.showTooltipMock).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({ clientX: 80, clientY: 90 }),
      'Clear'
    );
    expect(hoisted.moveTooltipMock).toHaveBeenCalledTimes(2);
    expect(hoisted.hideTooltipMock).toHaveBeenCalledTimes(2);
  });

  it('clears local search text and grid filters together', async () => {
    const clearFilter = vi.fn();
    const clearSorting = vi.fn();
    const wrapper = mountPage();
    await settle();

    wrapper.vm.searchText = 'user';
    wrapper.vm.cacheGridRef = {
      instance: {
        clearFilter,
        clearSorting,
      },
    };

    wrapper.vm.handleClearFilters();
    await nextTick();

    expect(wrapper.vm.searchText).toBe('');
    expect(clearFilter).toHaveBeenCalledTimes(1);
    expect(clearSorting).toHaveBeenCalledTimes(1);
  });

  it('shows the error state when stats loading fails', async () => {
    (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      new Error('Network error')
    );

    const wrapper = mountPage();
    await settle();

    expect(wrapper.find('.state-card.error-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('Unable to load cache items');
    expect(wrapper.text()).toContain('Try refreshing the page data');
  });

  it('retries fetching cache stats from the error state', async () => {
    (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>)
      .mockRejectedValueOnce(new Error('Network error'))
      .mockResolvedValueOnce(mockCacheStats);

    const wrapper = mountPage();
    await settle();

    await wrapper.vm.retryFetchStats();
    await settle();

    expect(SettingsService.getAllCacheStats).toHaveBeenCalledTimes(2);
    expect(wrapper.vm.isStatsError).toBe(false);
    expect(wrapper.vm.cacheItems).toHaveLength(3);
  });

  it('shows the empty state when no cache items exist', async () => {
    (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({});

    const wrapper = mountPage();
    await settle();

    expect(wrapper.vm.cacheItems).toEqual([]);
    expect(wrapper.text()).toContain('No cache items found');
  });

  it('disables the clear all button when no cache items exist', async () => {
    (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockResolvedValueOnce({});

    const wrapper = mountPage();
    await settle();

    expect(wrapper.vm.cacheItems).toHaveLength(0);
    expect(wrapper.vm.dialogDescription).toBe('');
  });

  it('opens the delete dialog for a selected cache row', async () => {
    const wrapper = mountPage();
    await settle();

    wrapper.vm.openDeleteDialog({ id: 'JobCache', label: 'Job Cache', size: 2 });
    await nextTick();

    expect(wrapper.find('.confirmation-dialog').exists()).toBe(true);
    expect(wrapper.find('.confirmation-dialog').attributes('data-type')).toBe('delete');
    expect(wrapper.find('.dialog-title').text()).toBe('Confirm Delete');
    expect(wrapper.find('.dialog-description').text()).toContain('Job Cache');
    expect(wrapper.find('.confirm-btn').text()).toBe('Delete');
  });

  it('computes dialog loading for a cache delete in progress', async () => {
    const wrapper = mountPage();
    await settle();

    wrapper.vm.openDeleteDialog({ id: 'JobCache', label: 'Job Cache', size: 2 });
    wrapper.vm.clearingCacheIds.add('JobCache');
    await nextTick();

    expect(wrapper.vm.dialogLoading).toBe(true);
  });

  it('computes dialog loading for a clear-all operation in progress', async () => {
    const wrapper = mountPage();
    await settle();

    wrapper.vm.openClearAllDialog();
    wrapper.vm.isClearingAll = true;
    await nextTick();

    expect(wrapper.vm.dialogLoading).toBe(true);
  });

  it('clears the selected cache reference when the dialog closes', async () => {
    const wrapper = mountPage();
    await settle();

    wrapper.vm.openDeleteDialog({ id: 'JobCache', label: 'Job Cache', size: 2 });
    await nextTick();
    wrapper.vm.closeDialog();
    await nextTick();

    expect(wrapper.vm.cacheToDelete).toBeNull();
    expect(wrapper.vm.dialogState.open).toBe(false);
    expect(wrapper.vm.dialogState.type).toBeNull();
  });

  it('does nothing when dialog confirm runs without an active dialog type', async () => {
    const wrapper = mountPage();
    await settle();

    await wrapper.vm.handleDialogConfirm();
    await settle();

    expect(SettingsService.clearCacheByName).not.toHaveBeenCalled();
    expect(SettingsService.clearAllCaches).not.toHaveBeenCalled();
  });

  it('shows the row clearing state while a delete request is in progress', async () => {
    let resolveClear: (() => void) | undefined;
    (SettingsService.clearCacheByName as ReturnType<typeof vi.fn>).mockImplementationOnce(
      () =>
        new Promise<void>(resolve => {
          resolveClear = resolve;
        })
    );

    const wrapper = mountPage();
    await settle();

    await wrapper.findAll('.cache-action-btn')[0].trigger('click');
    await nextTick();
    await wrapper.find('.confirm-btn').trigger('click');
    await nextTick();

    expect(wrapper.find('.row-loading-indicator').exists()).toBe(true);

    resolveClear?.();
    await settle();
  });

  it('closes the delete dialog when cancel is clicked', async () => {
    const wrapper = mountPage();
    await settle();

    await wrapper.findAll('.cache-action-btn')[0].trigger('click');
    await nextTick();
    await wrapper.find('.cancel-btn').trigger('click');
    await nextTick();

    expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
  });

  it('clears a selected cache and reloads the stats', async () => {
    const wrapper = mountPage();
    await settle();
    vi.clearAllMocks();

    await wrapper.findAll('.cache-action-btn')[0].trigger('click');
    await nextTick();
    await wrapper.find('.confirm-btn').trigger('click');
    await settle();

    expect(SettingsService.clearCacheByName).toHaveBeenCalledWith({ cacheName: 'JobCache' });
    expect(Alert.success).toHaveBeenCalledWith("Cache 'Job Cache' cleared");
    expect(SettingsService.getAllCacheStats).toHaveBeenCalled();
    expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
  });

  it('shows an unknown error notification when clearing a cache fails with a non-error value', async () => {
    (SettingsService.clearCacheByName as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      'String error'
    );

    const wrapper = mountPage();
    await settle();

    await wrapper.findAll('.cache-action-btn')[0].trigger('click');
    await nextTick();
    await wrapper.find('.confirm-btn').trigger('click');
    await settle();

    expect(Alert.error).toHaveBeenCalledWith(expect.stringContaining('Unknown error'));
  });

  it('shows an error notification when clearing a cache fails', async () => {
    (SettingsService.clearCacheByName as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      new Error('Delete failed')
    );

    const wrapper = mountPage();
    await settle();

    await wrapper.findAll('.cache-action-btn')[0].trigger('click');
    await nextTick();
    await wrapper.find('.confirm-btn').trigger('click');
    await settle();

    expect(Alert.error).toHaveBeenCalledWith(expect.stringContaining('Failed to clear cache'));
    expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
  });

  it('opens the clear all dialog and clears all caches', async () => {
    const wrapper = mountPage();
    await settle();
    vi.clearAllMocks();

    await wrapper.find('.clear-all-btn').trigger('click');
    await nextTick();

    expect(wrapper.find('.confirmation-dialog').attributes('data-type')).toBe('custom');
    expect(wrapper.find('.dialog-title').text()).toBe('Confirm Clear All');

    await wrapper.find('.confirm-btn').trigger('click');
    await settle();

    expect(SettingsService.clearAllCaches).toHaveBeenCalledTimes(1);
    expect(Alert.success).toHaveBeenCalledWith('All caches cleared');
    expect(SettingsService.getAllCacheStats).toHaveBeenCalled();
  });

  it('closes the clear all dialog when cancel is clicked', async () => {
    const wrapper = mountPage();
    await settle();

    await wrapper.find('.clear-all-btn').trigger('click');
    await nextTick();
    await wrapper.find('.cancel-btn').trigger('click');
    await nextTick();

    expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
  });

  it('shows an error notification when clear all fails with an Error object', async () => {
    (SettingsService.clearAllCaches as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      new Error('Clear all failed')
    );

    const wrapper = mountPage();
    await settle();

    await wrapper.find('.clear-all-btn').trigger('click');
    await nextTick();
    await wrapper.find('.confirm-btn').trigger('click');
    await settle();

    expect(Alert.error).toHaveBeenCalledWith(
      expect.stringContaining('Failed to clear all caches: Clear all failed')
    );
  });

  it('shows an error notification when clear all fails', async () => {
    (SettingsService.clearAllCaches as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
      'String error'
    );

    const wrapper = mountPage();
    await settle();

    await wrapper.find('.clear-all-btn').trigger('click');
    await nextTick();
    await wrapper.find('.confirm-btn').trigger('click');
    await settle();

    expect(Alert.error).toHaveBeenCalledWith(expect.stringContaining('Unknown error'));
    expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
  });
});
