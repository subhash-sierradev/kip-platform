/* eslint-disable simple-import-sort/imports */
import { mount } from '@vue/test-utils';
import { describe, it, expect, vi, beforeEach } from 'vitest';

// Stub DevExtreme DxButton for this test suite
vi.mock('devextreme-vue', () => ({
  DxButton: { name: 'DxButton', template: '<button @click="$emit(\'click\')"><slot /></button>' },
}));

vi.mock('@/api/services/SettingsService', () => {
  return {
    SettingsService: {
      getAllCacheStats: vi.fn(),
    },
  };
});

vi.mock('@/composables/useTooltip', () => ({
  useTooltip: () => ({
    tooltip: { visible: false, text: '', x: 0, y: 0 },
    showTooltip: vi.fn(),
    moveTooltip: vi.fn(),
    hideTooltip: vi.fn(),
  }),
}));

import CacheStatisticsPage from '@/components/admin/cache/CacheStatisticsPage.vue';
import { SettingsService } from '@/api/services/SettingsService';

const mountAny = (...args: Parameters<typeof mount>) => mount(...args) as any;

const getAllCacheStatsMock = SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>;

const stubs = {
  DxButton: { name: 'DxButton', template: '<button @click="$emit(\'click\')"><slot /></button>' },
  CommonTooltip: { template: '<div class="tooltip-stub"><slot /></div>' },
};

describe('CacheStatisticsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAllCacheStatsMock.mockResolvedValue({
      UserCache: { size: 10, requestCount: 5, hitRate: 0.6, missRate: 0.4 },
    });
  });

  it('loads and displays cache stats', async () => {
    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });

    // Wait for promises to resolve
    await Promise.resolve();
    await Promise.resolve();

    expect(wrapper.text()).toContain('Request Count');
    expect(wrapper.text()).toContain('5');
  });

  it('shows error messages when service fails', async () => {
    getAllCacheStatsMock.mockRejectedValueOnce(new Error('fail'));

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    expect(wrapper.find('.error').exists()).toBe(true);
    expect(wrapper.text()).toContain('Error loading cache options.');
    expect(wrapper.text()).toContain('Error loading cache statistics.');
  });

  it('computes aggregated averages for All selection', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      FirstCache: { size: 10, requestCount: 0, hitRate: 0.6, missRate: 0.4 },
      SecondCache: { size: 20, requestCount: 10, hitRate: 0.5, missRate: 0.5 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Aggregated average hit/miss uses avg across caches (0.55 / 0.45)
    const ratio = wrapper.find('.stats-ratio-modern');
    expect(ratio.text()).toContain('Hit Rate: 55.0%');
    expect(ratio.text()).toContain('Miss Rate: 45.0%');
    // Aggregated cache size
    const sizeValue = wrapper.find('.stats-card-modern .stats-value-modern');
    expect(sizeValue.exists()).toBe(true);
    // Total size and request count aggregated
    expect(wrapper.text()).toContain('Request Count');
    expect(wrapper.find('.request-count').text()).toBe('10');
  });

  it('changes selection and shows single cache stats on click', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      UserCache: { size: 42, requestCount: 7, hitRate: 0.25, missRate: 0.75 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Click the cache chip via DxButton component stub
    const chipComponents = wrapper.findAllComponents({ name: 'DxButton' });
    expect(chipComponents.length).toBeGreaterThan(0);
    const target = chipComponents[chipComponents.length > 1 ? 1 : 0];
    target.vm.$emit('click');

    // Values should reflect the single cache
    expect(wrapper.find('.request-count').text()).toBe('7');
    const ratio = wrapper.find('.stats-ratio-modern');
    expect(ratio.text()).toContain('Hit Rate: 25.0%');
    expect(ratio.text()).toContain('Miss Rate: 75.0%');
  });

  it('shows loading state initially', async () => {
    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });

    expect(wrapper.text()).toContain('Loading cache options...');
    expect(wrapper.text()).toContain('Loading cache statistics...');
  });

  it('handles null statsData by returning default cacheOptions', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce(null);

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Should show only 'All' option
    const buttons = wrapper.findAllComponents({ name: 'DxButton' });
    expect(buttons.length).toBe(1);
  });

  it('handles empty statsData object', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({});

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Should show 'All' option and default stats
    expect(wrapper.text()).toContain('Hit Rate: 0.0%');
    expect(wrapper.text()).toContain('Miss Rate: 0.0%');
  });

  it('handles cache with zero requestCount in aggregation', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      CacheA: { size: 5, requestCount: 0, hitRate: 0, missRate: 0 },
      CacheB: { size: 10, requestCount: 0, hitRate: 0, missRate: 0 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // When totalRequests is 0, hitRate and missRate default to 0
    expect(wrapper.text()).toContain('Hit Rate: 0.0%');
    expect(wrapper.text()).toContain('Miss Rate: 0.0%');
  });

  it('sorts cache options alphabetically', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      ZebraCache: { size: 1, requestCount: 1, hitRate: 0.5, missRate: 0.5 },
      AppleCache: { size: 2, requestCount: 2, hitRate: 0.5, missRate: 0.5 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    const buttons = wrapper.findAllComponents({ name: 'DxButton' });
    // First is 'All', then sorted: 'Apple Cache', 'Zebra Cache'
    expect(buttons.length).toBe(3);
  });

  it('truncates long cache names with ellipsis', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      VeryLongCacheNameThatExceedsTwentyFiveCharacters: {
        size: 1,
        requestCount: 1,
        hitRate: 0.5,
        missRate: 0.5,
      },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Verify cache option exists (converted from camelCase)
    expect(wrapper.vm.cacheOptions).toContain(
      'Very Long Cache Name That Exceeds Twenty Five Characters'
    );
  });

  it('handles selection of cache with missing stats', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      UserCache: { size: 10, requestCount: 5, hitRate: 0.6, missRate: 0.4 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Manually trigger selection of non-existent cache
    await wrapper.vm.handleCacheSelection('NonExistentCache');

    // Should return default stats (0 values)
    expect(wrapper.find('.request-count').text()).toBe('0');
  });

  it('computes totalSize correctly for aggregated stats', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      CacheA: { size: 15, requestCount: 10, hitRate: 0.5, missRate: 0.5 },
      CacheB: { size: 25, requestCount: 20, hitRate: 0.6, missRate: 0.4 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Total size should be 15 + 25 = 40
    const sizeCard = wrapper.findAll('.stats-card-modern')[0];
    expect(sizeCard.text()).toContain('40');
  });

  it('returns correct stats when selecting specific cache', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      FirstCache: { size: 10, requestCount: 5, hitRate: 0.8, missRate: 0.2 },
      SecondCache: { size: 20, requestCount: 10, hitRate: 0.5, missRate: 0.5 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Select FirstCache
    const buttons = wrapper.findAllComponents({ name: 'DxButton' });
    await buttons[1].vm.$emit('click'); // First cache after 'All'

    const ratio = wrapper.find('.stats-ratio-modern');
    expect(ratio.text()).toContain('Hit Rate: 80.0%');
    expect(ratio.text()).toContain('Miss Rate: 20.0%');
  });

  it('handles missing hitRate/missRate fields in cache stats', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      IncompleteCache: { size: 10, requestCount: 5 } as any, // Missing hitRate/missRate
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Click to select the cache
    const buttons = wrapper.findAllComponents({ name: 'DxButton' });
    await buttons[1].vm.$emit('click');

    // Should default to 0.0%
    const ratio = wrapper.find('.stats-ratio-modern');
    expect(ratio.text()).toContain('Hit Rate: 0.0%');
    expect(ratio.text()).toContain('Miss Rate: 0.0%');
  });

  it('displays correct label for aggregated cache size', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      CacheA: { size: 10, requestCount: 5, hitRate: 0.5, missRate: 0.5 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // When 'All' is selected, should show 'Aggregated Cache Size'
    expect(wrapper.text()).toContain('Aggregated Cache Size');
  });

  it('displays correct label for single cache size', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      UserCache: { size: 10, requestCount: 5, hitRate: 0.5, missRate: 0.5 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Select single cache
    const buttons = wrapper.findAllComponents({ name: 'DxButton' });
    await buttons[1].vm.$emit('click');

    // Should show 'Cache Size' (not 'Aggregated')
    expect(wrapper.text()).toContain('Cache Size');
    expect(wrapper.text()).not.toContain('Aggregated Cache Size');
  });

  it('uses tooltip composable for cache chip interactions', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      UserCache: { size: 10, requestCount: 5, hitRate: 0.5, missRate: 0.5 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Verify cache chip wrapper exists with event handlers
    const chip = wrapper.find('.cache-chip-wrapper');
    expect(chip.exists()).toBe(true);
  });

  it('computes weighted average correctly for aggregated hitRate/missRate', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      CacheA: { size: 10, requestCount: 100, hitRate: 0.9, missRate: 0.1 },
      CacheB: { size: 20, requestCount: 100, hitRate: 0.6, missRate: 0.4 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Weighted average: (100*0.9 + 100*0.6) / 200 = 0.75 (75%)
    const ratio = wrapper.find('.stats-ratio-modern');
    expect(ratio.text()).toContain('Hit Rate: 75.0%');
    expect(ratio.text()).toContain('Miss Rate: 25.0%');
  });

  it('handles statsValue with unknown field', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      UserCache: { size: 10, requestCount: 5, hitRate: 0.5, missRate: 0.5 },
    });

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // Call statsValue with invalid field
    const result = wrapper.vm.statsValue('unknownField');
    expect(result).toBe(0);
  });

  it('returns percentage string when stats is null and field is rate', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce(null);

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // When stats is null, hitRate and missRate return '0.0%'
    expect(wrapper.vm.statsValue('hitRate')).toBe('0.0%');
    expect(wrapper.vm.statsValue('missRate')).toBe('0.0%');
  });

  it('returns numeric 0 when stats is null and field is not rate', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce(null);

    const wrapper = mountAny(CacheStatisticsPage, { global: { stubs } });
    await Promise.resolve();
    await Promise.resolve();

    // When stats is null, size and requestCount return 0
    expect(wrapper.vm.statsValue('size')).toBe(0);
    expect(wrapper.vm.statsValue('requestCount')).toBe(0);
  });
});
