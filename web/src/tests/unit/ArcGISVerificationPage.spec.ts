import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

// Mock the composable
const mockFetchRecords = vi.fn();
const mockReset = vi.fn();
const mockRecords = ref<Record<string, unknown>[]>([]);
const mockLoading = ref(false);
const mockError = ref<string | null>(null);
const mockHasMore = ref(false);
const mockCurrentOffset = ref(0);

vi.mock('@/composables/useArcGISVerification', () => ({
  useArcGISVerification: () => ({
    records: mockRecords,
    loading: mockLoading,
    error: mockError,
    hasMore: mockHasMore,
    currentOffset: mockCurrentOffset,
    fetchRecords: mockFetchRecords,
    reset: mockReset,
  }),
}));

// Stub DevExtreme and GenericDataGrid components
vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    name: 'DxButton',
    props: ['text', 'type', 'stylingMode', 'disabled', 'icon'],
    template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ text }}</button>',
    emits: ['click'],
  },
}));

vi.mock('devextreme-vue/text-box', () => ({
  DxTextBox: {
    name: 'DxTextBox',
    props: ['value', 'placeholder', 'showClearButton', 'stylingMode'],
    emits: ['update:value', 'enterKey'],
    template: '<input :value="value" @input="$emit(\'update:value\', $event.target.value)" />',
  },
}));

vi.mock('@/components/common/GenericDataGrid.vue', () => ({
  default: {
    name: 'GenericDataGrid',
    props: ['data', 'columns', 'pageSize', 'rowKey', 'enableClearFilters', 'enableExport'],
    template: '<div class="mock-grid" />',
  },
}));

import ArcGISVerificationPage from '@/components/admin/arcgisverification/ArcGISVerificationPage.vue';

beforeEach(() => {
  vi.clearAllMocks();
  mockRecords.value = [];
  mockLoading.value = false;
  mockError.value = null;
  mockHasMore.value = false;
  mockCurrentOffset.value = 0;
  mockFetchRecords.mockResolvedValue(undefined);
  mockReset.mockReset();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('ArcGISVerificationPage', () => {
  it('renders Search and Clear buttons', () => {
    const wrapper = mount(ArcGISVerificationPage);
    const buttons = wrapper.findAll('button');
    const labels = buttons.map(b => b.text());
    expect(labels).toContain('Search');
    expect(labels).toContain('Clear');
  });

  it('calls fetchRecords with offset 0 on Search click', async () => {
    const wrapper = mount(ArcGISVerificationPage);
    const searchButton = wrapper.findAll('button').find(b => b.text() === 'Search');
    await searchButton?.trigger('click');
    expect(mockFetchRecords).toHaveBeenCalledWith(0, '', '');
  });

  it('shows loading text when loading is true', async () => {
    mockLoading.value = true;
    const wrapper = mount(ArcGISVerificationPage);
    expect(wrapper.text()).toContain('Loading records');
  });

  it('shows error message when error is set', async () => {
    mockError.value = 'ArcGIS service error: 502';
    const wrapper = mount(ArcGISVerificationPage);
    expect(wrapper.text()).toContain('ArcGIS service error: 502');
  });

  it('shows "No records found" after search with empty result', async () => {
    const wrapper = mount(ArcGISVerificationPage);
    // Trigger search to set hasSearched=true
    const searchButton = wrapper.findAll('button').find(b => b.text() === 'Search');
    await searchButton?.trigger('click');
    await wrapper.vm.$nextTick();
    expect(wrapper.text()).toContain('No records found');
  });

  it('renders GenericDataGrid when records are present', async () => {
    mockRecords.value = [{ OBJECTID: 1, external_location_id: 'LOC-1', name: 'Test' }];
    const wrapper = mount(ArcGISVerificationPage);
    await wrapper.vm.$nextTick();
    expect(wrapper.find('.mock-grid').exists()).toBe(true);
  });

  it('shows Load Older Records button when hasMore is true', async () => {
    mockRecords.value = [{ OBJECTID: 1 }];
    mockHasMore.value = true;
    const wrapper = mount(ArcGISVerificationPage);
    await wrapper.vm.$nextTick();
    const buttons = wrapper.findAll('button');
    const loadMoreBtn = buttons.find(b => b.text().includes('Load Older Records'));
    expect(loadMoreBtn?.exists()).toBe(true);
  });

  it('calls fetchRecords with offset+1000 on Load Older Records click', async () => {
    mockRecords.value = [{ OBJECTID: 1 }];
    mockHasMore.value = true;
    mockCurrentOffset.value = 0;
    const wrapper = mount(ArcGISVerificationPage);
    await wrapper.vm.$nextTick();
    const buttons = wrapper.findAll('button');
    const loadMoreBtn = buttons.find(b => b.text().includes('Load Older Records'));
    await loadMoreBtn?.trigger('click');
    expect(mockFetchRecords).toHaveBeenCalledWith(1000, '', '');
  });

  it('shows OBJECTID range in summary when records include OBJECTID', async () => {
    mockRecords.value = [
      { OBJECTID: 8756, external_location_id: 'LOC-8756' },
      { OBJECTID: 8000, external_location_id: 'LOC-8000' },
      { OBJECTID: 7757, external_location_id: 'LOC-7757' },
    ];
    const wrapper = mount(ArcGISVerificationPage);
    await wrapper.vm.$nextTick();
    expect(wrapper.text()).toContain('8756');
    expect(wrapper.text()).toContain('7757');
  });

  it('clears inputs and hides empty state on Clear click', async () => {
    const wrapper = mount(ArcGISVerificationPage);
    // Trigger search first
    const searchBtn = wrapper.findAll('button').find(b => b.text() === 'Search');
    await searchBtn?.trigger('click');
    await wrapper.vm.$nextTick();

    const clearBtn = wrapper.findAll('button').find(b => b.text() === 'Clear');
    await clearBtn?.trigger('click');
    await wrapper.vm.$nextTick();

    expect(wrapper.text()).not.toContain('No records found');
  });

  it('displays record count summary when records are loaded', async () => {
    mockRecords.value = [
      { OBJECTID: 1, external_location_id: 'LOC-1' },
      { OBJECTID: 2, external_location_id: 'LOC-2' },
    ];
    const wrapper = mount(ArcGISVerificationPage);
    await wrapper.vm.$nextTick();
    expect(wrapper.text()).toContain('2');
  });
});
