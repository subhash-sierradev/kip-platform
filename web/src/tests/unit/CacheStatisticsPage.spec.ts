/* eslint-disable simple-import-sort/imports */
import { mount, flushPromises } from '@vue/test-utils';
import { nextTick } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('devextreme-vue', () => ({
  DxButton: {
    name: 'DxButton',
    template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ text }}<slot /></button>',
    props: ['text', 'disabled', 'type', 'stylingMode'],
    emits: ['click'],
  },
}));

vi.mock('devextreme-vue/load-indicator', () => ({
  default: {
    name: 'DxLoadIndicator',
    template: '<div class="dx-load-indicator-stub"></div>',
  },
  DxButton: {
    name: 'DxButton',
    template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ text }}<slot /></button>',
    props: ['text', 'disabled', 'type', 'stylingMode'],
    emits: ['click'],
  },
}));

vi.mock('devextreme-vue/load-indicator', () => ({
  default: {
    name: 'DxLoadIndicator',
    template: '<div class="dx-load-indicator-stub"></div>',
  },
}));

vi.mock('@/api/services/SettingsService', () => ({
  SettingsService: {
    getAllCacheStats: vi.fn(),
  },
}));

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

const getAllCacheStatsMock = SettingsService.getAllCacheStats as ReturnType<typeof vi.fn>;

const stubs = {
  CommonTooltip: {
    template: '<div class="tooltip-stub"></div>',
  },
};

const mountPage = () => mount(CacheStatisticsPage, { global: { stubs } }) as any;

interface TextReadable {
  text: () => string;
}

async function settle() {
  await flushPromises();
  await nextTick();
}

describe('CacheStatisticsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAllCacheStatsMock.mockResolvedValue({
      UserCache: { size: 10, requestCount: 5, hitRate: 0.6, missRate: 0.4 },
    });
  });

  it('loads and displays cache statistics cards', async () => {
    const wrapper = mountPage();
    await settle();

    expect(wrapper.text()).toContain('Aggregated Cache Size');
    expect(wrapper.text()).toContain('Request Count');
    expect(wrapper.text()).toContain('5');
    expect(wrapper.findAll('.stats-card-modern')).toHaveLength(3);
    expect(wrapper.findAll('.stats-card-modern')).toHaveLength(3);
  });

  it('shows the error state when loading fails', async () => {
    getAllCacheStatsMock.mockRejectedValueOnce(new Error('fail'));

    const wrapper = mountPage();
    await settle();

    expect(wrapper.find('.state-card.error-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('Unable to load cache statistics');
    expect(wrapper.text()).toContain('Try refreshing the page data');
    expect(wrapper.text()).toContain('Retry');
  });

  it('retries loading statistics from the error state', async () => {
    getAllCacheStatsMock.mockRejectedValueOnce(new Error('fail')).mockResolvedValueOnce({
      UserCache: { size: 10, requestCount: 5, hitRate: 0.6, missRate: 0.4 },
    });

    const wrapper = mountPage();
    await settle();

    await wrapper.find('.state-card.error-state button').trigger('click');
    await settle();

    expect(getAllCacheStatsMock).toHaveBeenCalledTimes(2);
    expect(wrapper.find('.state-card.error-state').exists()).toBe(false);
    expect(wrapper.findAll('.stats-card-modern')).toHaveLength(3);
  });

  it('computes aggregated totals and weighted rates for All selection', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      FirstCache: { size: 10, requestCount: 0, hitRate: 0.6, missRate: 0.4 },
      SecondCache: { size: 20, requestCount: 10, hitRate: 0.5, missRate: 0.5 },
    });

    const wrapper = mountPage();
    await settle();

    const cards = wrapper.findAll('.stats-card-modern');
    expect(cards[0].text()).toContain('30');
    expect(cards[1].text()).toContain('10');
    expect(cards[2].text()).toContain('Hit Rate: 55.0%');
    expect(cards[2].text()).toContain('Miss Rate: 45.0%');
  });

  it('changes selection and shows single cache stats on click', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      UserCache: { size: 42, requestCount: 7, hitRate: 0.25, missRate: 0.75 },
    });

    const wrapper = mountPage();
    await settle();

    const buttons = wrapper.findAllComponents({ name: 'DxButton' });
    await buttons[1].trigger('click');
    await nextTick();

    const cards = wrapper.findAll('.stats-card-modern');
    expect(cards[0].text()).toContain('Cache Size');
    expect(cards[0].text()).toContain('42');
    expect(cards[1].text()).toContain('7');
    expect(cards[2].text()).toContain('Hit Rate: 25.0%');
    expect(cards[2].text()).toContain('Miss Rate: 75.0%');
  });

  it('shows loading content before stats resolve', () => {
    getAllCacheStatsMock.mockReturnValue(new Promise(() => {}));

    const wrapper = mountPage();

    expect(wrapper.text()).toContain('Loading cache options...');
    expect(wrapper.text()).toContain('Loading cache statistics');
    expect(wrapper.find('.loading-state').exists()).toBe(true);
  });

  it('shows the empty state when stats are null', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce(null);

    const wrapper = mountPage();
    await settle();

    expect(wrapper.text()).toContain('No cache filters are available yet.');
    expect(wrapper.find('.state-card.empty-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('No cache statistics found');
    expect(wrapper.vm.cacheOptions).toEqual(['All']);
  });

  it('shows NA rates when all caches have zero requests', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      CacheA: { size: 5, requestCount: 0, hitRate: 0, missRate: 0 },
      CacheB: { size: 10, requestCount: 0, hitRate: 0, missRate: 0 },
    });

    const wrapper = mountPage();
    await settle();

    const cards = wrapper.findAll('.stats-card-modern');
    expect(cards[2].text()).toContain('Hit Rate: N/A');
    expect(cards[2].text()).toContain('Miss Rate: N/A');
  });

  it('shows the empty state when stats are empty', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({});

    const wrapper = mountPage();
    await settle();

    expect(wrapper.text()).toContain('No cache filters are available yet.');
    expect(wrapper.find('.state-card.empty-state').exists()).toBe(true);
    expect(wrapper.text()).toContain('No cache statistics found');
  });

  it('sorts cache options alphabetically after All', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      ZebraCache: { size: 1, requestCount: 1, hitRate: 0.5, missRate: 0.5 },
      AppleCache: { size: 2, requestCount: 2, hitRate: 0.5, missRate: 0.5 },
    });

    const wrapper = mountPage();
    await settle();

    const buttons = wrapper.findAllComponents({ name: 'DxButton' });
    const labels = buttons.map((button: TextReadable) => button.text());
    expect(labels).toEqual(['All', 'Apple Cache', 'Zebra Cache']);
  });

  it('returns null cache keys for blank labels and the All filter', async () => {
    const wrapper = mountPage();
    await settle();

    expect(wrapper.vm.getCacheKey('')).toBeNull();
    expect(wrapper.vm.getCacheKey('All')).toBeNull();
  });

  it('returns null cache keys when stats data is unavailable', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce(null);

    const wrapper = mountPage();
    await settle();

    expect(wrapper.vm.getCacheKey('User Cache')).toBeNull();
  });

  it('shows NA rates when the selected cache has zero requests', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      EmptyCache: { size: 5, requestCount: 0, hitRate: 1, missRate: 0 },
    });

    const wrapper = mountPage();
    await settle();

    await wrapper.vm.handleCacheSelection('Empty Cache');
    await nextTick();

    const cards = wrapper.findAll('.stats-card-modern');
    expect(cards[2].text()).toContain('Hit Rate: N/A');
    expect(cards[2].text()).toContain('Miss Rate: N/A');
  });

  it('shows zero rates for a selected cache with missing hit and miss fields', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce({
      IncompleteCache: { size: 10, requestCount: 5 },
    });

    const wrapper = mountPage();
    await settle();

    await wrapper.vm.handleCacheSelection('Incomplete Cache');
    await nextTick();

    const cards = wrapper.findAll('.stats-card-modern');
    expect(cards[2].text()).toContain('Hit Rate: 0.0%');
    expect(cards[2].text()).toContain('Miss Rate: 0.0%');
  });

  it('shows NA rates when selecting a missing cache', async () => {
    const wrapper = mountPage();
    await settle();

    await wrapper.vm.handleCacheSelection('Non Existent Cache');
    await nextTick();

    const cards = wrapper.findAll('.stats-card-modern');
    expect(cards[0].text()).toContain('0');
    expect(cards[1].text()).toContain('0');
    expect(cards[2].text()).toContain('Hit Rate: N/A');
    expect(cards[2].text()).toContain('Miss Rate: N/A');
  });

  it('returns defaults from statsValue when stats are unavailable', async () => {
    getAllCacheStatsMock.mockResolvedValueOnce(null);

    const wrapper = mountPage();
    await settle();

    expect(wrapper.vm.statsValue('hitRate')).toBe('0.0%');
    expect(wrapper.vm.statsValue('missRate')).toBe('0.0%');
    expect(wrapper.vm.statsValue('size')).toBe(0);
    expect(wrapper.vm.statsValue('requestCount')).toBe(0);
    expect(wrapper.vm.statsValue('unknownField')).toBe(0);
  });
});
