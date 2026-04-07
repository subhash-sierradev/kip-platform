/// <reference types="vitest" />
import { render, screen } from '@testing-library/vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ref } from 'vue';

// Mock all DevExtreme components before any imports
vi.mock('devextreme-vue', () => ({
  default: {},
  DxDataGrid: { name: 'DxDataGrid' },
  DxColumn: { name: 'DxColumn' },
}));

vi.mock('@/composables/useJiraConnections', () => ({
  useJiraConnections: () => {
    const data = ref([
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
    const loading = ref(false);
    const error = ref(null);
    return {
      data,
      loading,
      error,
      fetch: vi.fn(async () => Promise.resolve()),
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
});
