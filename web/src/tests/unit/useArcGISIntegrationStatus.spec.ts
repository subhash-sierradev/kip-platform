import { beforeEach, describe, expect, it, vi } from 'vitest';

const toastSpies = {
  showError: vi.fn(),
  showSuccess: vi.fn(),
  showWarning: vi.fn(),
};

vi.mock('@/store/toast', () => ({
  useToastStore: () => toastSpies,
}));

vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    toggleArcGISIntegrationActive: vi.fn(),
  },
}));

import { ArcGISIntegrationService } from '@/api/services/ArcGISIntegrationService';
import { useArcGISIntegrationStatus } from '@/composables/useArcGISIntegrationActions';

describe('useArcGISIntegrationStatus', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns null and shows error for invalid id', async () => {
    const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
    const res = await toggleIntegrationStatus('');
    expect(res).toBeNull();
    expect(toastSpies.showError).toHaveBeenCalledWith('Invalid integration id');
  });

  it('uses API boolean result and shows success toast when true', async () => {
    (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockResolvedValueOnce(true);
    const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
    const res = await toggleIntegrationStatus('abc');
    expect(res).toBe(true);
    expect(toastSpies.showSuccess).toHaveBeenCalled();
  });

  it('uses API boolean result and shows warning toast when false', async () => {
    (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockResolvedValueOnce(false);
    const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
    const res = await toggleIntegrationStatus('abc');
    expect(res).toBe(false);
    expect(toastSpies.showWarning).toHaveBeenCalled();
  });

  it('derives new status when API non-boolean and currentEnabled provided (true -> false)', async () => {
    (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockResolvedValueOnce({});
    const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
    const res = await toggleIntegrationStatus('abc', true);
    expect(res).toBe(false);
    expect(toastSpies.showWarning).toHaveBeenCalled();
  });

  it('errors when API non-boolean and currentEnabled undefined', async () => {
    (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockResolvedValueOnce({});
    const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
    const res = await toggleIntegrationStatus('abc');
    expect(res).toBeNull();
    expect(toastSpies.showError).toHaveBeenCalledWith(
      'Failed to determine new status. Please refresh and try again.'
    );
  });

  it('returns null and shows generic error when API throws', async () => {
    (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockRejectedValueOnce(
      new Error('boom')
    );
    const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
    const res = await toggleIntegrationStatus('abc', true);
    expect(res).toBeNull();
    expect(toastSpies.showError).toHaveBeenCalledWith('Failed to update ArcGIS integration status');
  });
});
