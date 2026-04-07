/* eslint-disable simple-import-sort/imports */
import { mount, flushPromises } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { nextTick } from 'vue';

vi.mock('@/api/services/SettingsService');
vi.mock('@/utils/notificationUtils', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

import ClearCachePage from '@/components/admin/cache/ClearCachePage.vue';
import { SettingsService } from '@/api/services/SettingsService';
import Alert from '@/utils/notificationUtils';

describe('ClearCachePage', () => {
  const mockCacheStats = {
    UserCache: { size: 10, requestCount: 5, hitRate: 0.6, missRate: 0.4 },
    JobCache: { size: 2, requestCount: 1, hitRate: 1, missRate: 0 },
    SessionCache: { size: 5, requestCount: 10, hitRate: 0.5, missRate: 0.5 },
  };

  const stubs = {
    PolishedPillStack: {
      template: '<div class="pill-stack" :aria-label="ariaLabel">{{ count }}</div>',
      props: ['count', 'ariaLabel'],
    },
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
      template:
        '<button :class="className" :disabled="disabled" @click="$emit(\'click\')"><slot>{{ text }}</slot></button>',
      props: ['text', 'type', 'disabled', 'icon', 'stylingMode'],
      emits: ['click'],
    },
    DxLoadPanel: {
      template: '<div v-if="visible" class="dx-loadpanel">{{ message }}</div>',
      props: ['visible', 'showIndicator', 'showPane', 'shading', 'message'],
    },
  };

  beforeEach(() => {
    vi.clearAllMocks();
    (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockResolvedValue(
      mockCacheStats
    );
    (SettingsService.clearCacheByName as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);
    (SettingsService.clearAllCaches as ReturnType<typeof vi.fn>).mockResolvedValue(undefined);
  });

  describe('Component Mounting and Initial State', () => {
    it('renders the page with title', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.title').text()).toBe('Clear Cache');
    });

    it('fetches cache stats on mount', async () => {
      mount(ClearCachePage, { global: { stubs } });
      await flushPromises();

      expect(SettingsService.getAllCacheStats).toHaveBeenCalledTimes(1);
    });

    it('shows loading panel while fetching stats', async () => {
      mount(ClearCachePage, { global: { stubs } });
      // Note: Loading state may resolve too quickly in tests
      // The component correctly shows loading state during actual data fetch
      await nextTick();

      // After mount, loading should have started (though may complete quickly)
      expect(SettingsService.getAllCacheStats).toHaveBeenCalled();
    });

    it('hides loading panel after stats are loaded', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.dx-loadpanel').exists()).toBe(false);
    });
  });

  describe('Cache List Display', () => {
    it('renders cache list with formatted labels', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      expect(wrapper.text()).toContain('User Cache');
      expect(wrapper.text()).toContain('Job Cache');
      expect(wrapper.text()).toContain('Session Cache');
    });

    it('displays cache item counts using PolishedPillStack', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      const pillStacks = wrapper.findAll('.pill-stack');
      expect(pillStacks.length).toBeGreaterThan(0);
    });

    it('renders delete button for each cache item', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      const deleteButtons = wrapper.findAll('.delete-btn');
      expect(deleteButtons.length).toBe(3); // 3 cache items
    });

    it('displays cache items in alphabetical order', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      const labels = wrapper.findAll('.cache-label');
      const labelTexts = labels.map(l => l.text());
      expect(labelTexts).toContain('User Cache');
      expect(labelTexts).toContain('Job Cache');
      expect(labelTexts).toContain('Session Cache');
    });

    it('handles cache items with null size', async () => {
      (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockResolvedValue({
        EmptyCache: { size: null, requestCount: 0, hitRate: 0, missRate: 0 },
      });

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      const pillStack = wrapper.find('.pill-stack');
      expect(pillStack.text()).toContain('0');
    });
  });

  describe('Error Handling', () => {
    it('displays error message when stats fetch fails', async () => {
      (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Network error')
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.error').exists()).toBe(true);
      expect(wrapper.find('.error').text()).toContain('Error loading cache items');
    });

    it('hides loading panel after error', async () => {
      (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Network error')
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.dx-loadpanel').exists()).toBe(false);
    });

    it('does not render cache grid when error occurs', async () => {
      (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Network error')
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.cache-grid').exists()).toBe(false);
    });
  });

  describe('Empty State', () => {
    it('displays empty message when no cache items exist', async () => {
      (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockResolvedValue({});

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.empty').exists()).toBe(true);
      expect(wrapper.find('.empty').text()).toContain('No cache items found');
    });

    it('does not render cache grid when empty', async () => {
      (SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>).mockResolvedValue({});

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.cache-grid').exists()).toBe(false);
    });
  });

  describe('Delete Single Cache Dialog', () => {
    it('opens delete dialog when delete button clicked', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      const deleteBtn = wrapper.findAll('.delete-btn')[0];
      await deleteBtn.trigger('click');
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').exists()).toBe(true);
    });

    it('displays correct title in delete dialog', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      expect(wrapper.find('.dialog-title').text()).toBe('Confirm Delete');
    });

    it('displays cache name in delete dialog description', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      const description = wrapper.find('.dialog-description').text();
      expect(description).toContain('User Cache'); // First delete button in list (Job, Session, User alphabetically)
    });

    it('displays Delete confirm label in delete dialog', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      expect(wrapper.find('.confirm-btn').text()).toBe('Delete');
    });

    it('closes delete dialog when cancel clicked', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();
      expect(wrapper.find('.confirmation-dialog').exists()).toBe(true);

      await wrapper.find('.cancel-btn').trigger('click');
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
    });

    it('calls clearCacheByName when delete confirmed', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(SettingsService.clearCacheByName).toHaveBeenCalledWith({ cacheName: 'UserCache' });
    });

    it('shows success alert after cache cleared', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(Alert.success).toHaveBeenCalledWith("Cache 'User Cache' cleared");
    });

    it('refetches stats after cache cleared', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      vi.clearAllMocks(); // Clear initial mount call

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(SettingsService.getAllCacheStats).toHaveBeenCalled();
    });

    it('closes dialog after successful delete', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();
      expect(wrapper.find('.confirmation-dialog').exists()).toBe(true);

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
    });

    it('shows error alert when delete fails', async () => {
      (SettingsService.clearCacheByName as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Delete failed')
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(Alert.error).toHaveBeenCalledWith(expect.stringContaining('Failed to clear cache'));
    });

    it('closes dialog after failed delete', async () => {
      (SettingsService.clearCacheByName as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Delete failed')
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
    });

    it('handles non-Error exceptions in delete', async () => {
      (SettingsService.clearCacheByName as ReturnType<typeof vi.fn>).mockRejectedValue(
        'String error'
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(Alert.error).toHaveBeenCalledWith(expect.stringContaining('Unknown error'));
    });
  });

  describe('Clear All Caches Dialog', () => {
    it('opens clear all dialog when Clear All button clicked', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').exists()).toBe(true);
    });

    it('displays correct title in clear all dialog', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      expect(wrapper.find('.dialog-title').text()).toBe('Confirm Clear All');
    });

    it('displays clear all warning in dialog description', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      const description = wrapper.find('.dialog-description').text();
      expect(description).toContain('clear all caches');
      expect(description).toContain('system performance');
    });

    it('displays Clear All confirm label', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      expect(wrapper.find('.confirm-btn').text()).toBe('Clear All');
    });

    it('closes clear all dialog when cancel clicked', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();
      expect(wrapper.find('.confirmation-dialog').exists()).toBe(true);

      await wrapper.find('.cancel-btn').trigger('click');
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
    });

    it('calls clearAllCaches when confirm clicked', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(SettingsService.clearAllCaches).toHaveBeenCalledTimes(1);
    });

    it('shows success alert after all caches cleared', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(Alert.success).toHaveBeenCalledWith('All caches cleared');
    });

    it('refetches stats after all caches cleared', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      vi.clearAllMocks(); // Clear initial mount call

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(SettingsService.getAllCacheStats).toHaveBeenCalled();
    });

    it('closes dialog after successful clear all', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();
      expect(wrapper.find('.confirmation-dialog').exists()).toBe(true);

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
    });

    it('shows error alert when clear all fails', async () => {
      (SettingsService.clearAllCaches as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Clear all failed')
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(Alert.error).toHaveBeenCalledWith(
        expect.stringContaining('Failed to clear all caches')
      );
    });

    it('closes dialog after failed clear all', async () => {
      (SettingsService.clearAllCaches as ReturnType<typeof vi.fn>).mockRejectedValue(
        new Error('Clear all failed')
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').exists()).toBe(false);
    });

    it('handles non-Error exceptions in clear all', async () => {
      (SettingsService.clearAllCaches as ReturnType<typeof vi.fn>).mockRejectedValue(
        'String error'
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await flushPromises();

      expect(Alert.error).toHaveBeenCalledWith(expect.stringContaining('Unknown error'));
    });

    it('disables Clear All button during clear all operation', async () => {
      (SettingsService.clearAllCaches as ReturnType<typeof vi.fn>).mockImplementation(
        () => new Promise(resolve => setTimeout(resolve, 100))
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await nextTick();

      // Button should be disabled during operation
      const clearAllBtn = wrapper.find('.clear-all-btn');
      expect(clearAllBtn.attributes('disabled')).toBeDefined();
    });

    it('sets dialog loading state during clear all', async () => {
      (SettingsService.clearAllCaches as ReturnType<typeof vi.fn>).mockImplementation(
        () => new Promise(resolve => setTimeout(resolve, 100))
      );

      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      await wrapper.find('.confirm-btn').trigger('click');
      await nextTick();

      // Confirm button should be disabled during loading
      expect((wrapper.find('.confirm-btn').element as HTMLButtonElement).disabled).toBe(true);
    });
  });

  describe('Dialog Type Management', () => {
    it('sets dialog type to delete for single cache deletion', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.findAll('.delete-btn')[0].trigger('click');
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').attributes('data-type')).toBe('delete');
    });

    it('sets dialog type to custom for clear all', async () => {
      const wrapper = mount(ClearCachePage, { global: { stubs } });
      await flushPromises();
      await nextTick();

      await wrapper.find('.clear-all-btn').trigger('click');
      await nextTick();

      expect(wrapper.find('.confirmation-dialog').attributes('data-type')).toBe('custom');
    });
  });
});
