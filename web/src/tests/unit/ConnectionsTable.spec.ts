/// <reference types="vitest" />
import { render, screen } from '@testing-library/vue';
import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

const mockData = ref([
  {
    id: '1',
    name: 'Primary',
    serviceType: 'JIRA',
    baseUrl: 'https://jira.example',
    jiraBaseUrl: 'https://jira.example',
    lastConnectionStatus: 'SUCCESS',
    authType: 'BASIC_AUTH',
  },
]);
const mockLoading = ref(false);
const mockError = ref<string | null>(null);
const mockFetch = vi.fn(async () => Promise.resolve());

// Mock all DevExtreme components before any imports
vi.mock('devextreme-vue', () => ({
  default: {},
  DxDataGrid: { name: 'DxDataGrid' },
  DxColumn: { name: 'DxColumn' },
}));

vi.mock('@/composables/useJiraConnections', () => ({
  useJiraConnections: () => {
    return {
      data: mockData,
      loading: mockLoading,
      error: mockError,
      fetch: mockFetch,
      create: vi.fn(),
    };
  },
}));

// Mock common components that might use DevExtreme
vi.mock('@/components/common/LoadingSpinner.vue', () => ({
  default: { template: '<div>Loading...</div>' },
}));

vi.mock('@/components/common/ErrorPanel.vue', () => ({
  default: { template: '<div>Error: {{ message }}</div>', props: ['message'] },
}));

vi.mock('@/components/common/StatusBadge.vue', () => ({
  default: { template: '<span>{{ status }}</span>', props: ['status'] },
}));

import ConnectionsTable from '@/components/admin/jiraconnections/ConnectionsTable.vue';

describe('ConnectionsTable', () => {
  beforeEach(() => {
    mockData.value = [
      {
        id: '1',
        name: 'Primary',
        serviceType: 'JIRA',
        baseUrl: 'https://jira.example',
        jiraBaseUrl: 'https://jira.example',
        lastConnectionStatus: 'SUCCESS',
        authType: 'BASIC_AUTH',
      },
    ];
    mockLoading.value = false;
    mockError.value = null;
    mockFetch.mockClear();

    // Ensure crypto is available in each test
    if (!globalThis.crypto) {
      Object.defineProperty(globalThis, 'crypto', {
        value: {
          getRandomValues: vi.fn(),
          randomUUID: vi.fn().mockReturnValue('test-uuid'),
        },
        configurable: true,
      });
    }
  });

  it('renders connection row', async () => {
    render(ConnectionsTable, { props: { autofetch: false } });
    // Data is injected via mock composable; should render immediately
    expect(screen.getByText(/Primary/)).toBeTruthy();
    expect(screen.getByText(/https:\/\/jira.example/)).toBeTruthy();
    expect(screen.getByText(/SUCCESS/)).toBeTruthy();
  });

  it('auto-fetches connections when autofetch is not disabled', async () => {
    mount(ConnectionsTable, { props: { autofetch: true } });
    await Promise.resolve();

    expect(mockFetch).toHaveBeenCalledWith({});
  });

  it('falls back to an empty list when connection data is null', async () => {
    mockData.value = null as unknown as typeof mockData.value;

    render(ConnectionsTable, { props: { autofetch: false } });

    expect(screen.getByText(/Connection Key/)).toBeTruthy();
    expect(screen.queryByText(/Primary/)).toBeNull();
  });
});
