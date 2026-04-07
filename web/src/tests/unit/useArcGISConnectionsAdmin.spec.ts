import { describe, expect, it, vi } from 'vitest';

import { ServiceType } from '@/api/models/enums';
import { useServiceConnectionsAdmin } from '@/composables/useServiceConnectionsAdmin';
import { useToastStore } from '@/store/toast';

vi.mock('@/composables/useServiceConnectionsAdmin');
vi.mock('@/store/toast');

// Mock toast store before importing
vi.mocked(useToastStore).mockReturnValue({
  $id: 'toast',
  $state: {} as any,
  $patch: vi.fn(),
  $reset: vi.fn(),
  $subscribe: vi.fn(),
  $dispose: vi.fn(),
  $onAction: vi.fn(),
  toasts: { value: [] } as any,
  toastCount: { value: 0 } as any,
  showSuccess: vi.fn(),
  showError: vi.fn(),
  showWarning: vi.fn(),
  showInfo: vi.fn(),
  clearToast: vi.fn(),
  clearAllToasts: vi.fn(),
  updateToast: vi.fn(),
} as any);

// Import after mocking
const { useArcGISConnectionsAdmin } = await import('@/composables/useArcGISConnectionsAdmin');

describe('useArcGISConnectionsAdmin', () => {
  it('should call useServiceConnectionsAdmin with ARCGIS service type', () => {
    const mockResult = {
      loading: { value: false },
      error: { value: null },
      connections: { value: [] },
      fetchConnections: vi.fn(),
      testConnection: vi.fn(),
      deleteConnection: vi.fn(),
      rotateSecret: vi.fn(),
    };

    vi.mocked(useServiceConnectionsAdmin).mockReturnValue(mockResult as any);

    const result = useArcGISConnectionsAdmin();

    expect(useServiceConnectionsAdmin).toHaveBeenCalledWith(ServiceType.ARCGIS);
    expect(result).toBe(mockResult);
  });
});
