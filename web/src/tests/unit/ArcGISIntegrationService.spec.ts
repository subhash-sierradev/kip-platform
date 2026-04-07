import { beforeEach, describe, expect, it, vi } from 'vitest';

import { request as coreRequest } from '@/api/core/request';
import { ArcGISIntegrationService } from '@/api/services/ArcGISIntegrationService';

// Mock core request safely without referencing top-level variables
vi.mock('@/api/core/request', () => ({ request: vi.fn() }));

beforeEach(() => {
  (coreRequest as any).mockReset?.();
});

describe('ArcGISIntegrationService', () => {
  it('lists integrations via GET /integrations/arcgis', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    const res = await ArcGISIntegrationService.listArcGISIntegrations();
    expect(res).toEqual([]);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/arcgis',
      })
    );
  });

  it('getIntegrationById returns detail and maps errors with status', async () => {
    (coreRequest as any).mockResolvedValueOnce({
      id: 'x',
      name: 'A',
      connectionId: 'c',
      itemType: 't',
    });
    const ok = await ArcGISIntegrationService.getArcGISIntegrationById('x');
    expect(ok.id).toBe('x');

    const err404 = Object.assign(new Error('not found'), { status: 404 });
    (coreRequest as any).mockRejectedValueOnce(err404);
    await expect(ArcGISIntegrationService.getArcGISIntegrationById('missing')).rejects.toThrow(
      /Integration not found/
    );

    // Test 403 error
    const err403 = Object.assign(new Error('forbidden'), { status: 403 });
    (coreRequest as any).mockRejectedValueOnce(err403);
    await expect(ArcGISIntegrationService.getArcGISIntegrationById('forbidden')).rejects.toThrow(
      /Access denied/
    );

    // Test generic error with message
    const errWithMessage = Object.assign(new Error('Custom error'), {
      status: 500,
      message: 'Server error',
    });
    (coreRequest as any).mockRejectedValueOnce(errWithMessage);
    await expect(ArcGISIntegrationService.getArcGISIntegrationById('server-error')).rejects.toThrow(
      /Server error/
    );

    // Test error without status but with message
    const errNoStatus = { message: 'Network error' };
    (coreRequest as any).mockRejectedValueOnce(errNoStatus);
    await expect(
      ArcGISIntegrationService.getArcGISIntegrationById('network-error')
    ).rejects.toThrow(/Network error/);

    // Test default error case
    const errGeneric = Object.assign(new Error('generic'), { status: 500 });
    (coreRequest as any).mockRejectedValueOnce(errGeneric);
    await expect(
      ArcGISIntegrationService.getArcGISIntegrationById('generic-error')
    ).rejects.toThrow(/generic/);
  });

  it('getJobHistory hits /executions', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    await ArcGISIntegrationService.getJobHistory('idJ');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/arcgis/idJ/executions',
      })
    );
  });

  it('triggerJobExecution posts to /trigger', async () => {
    (coreRequest as any).mockResolvedValueOnce(undefined);
    await ArcGISIntegrationService.triggerJobExecution('idTrigger');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/integrations/arcgis/idTrigger/trigger',
      })
    );
  });

  // Test removed - testIntegration method not implemented
  // it('testIntegration posts to /test', async () => {
  //   (coreRequest as any).mockResolvedValueOnce({ success: true });
  //   const res = await ArcGISIntegrationService.testIntegration('id2');
  //   expect(res.success).toBe(true);
  //   expect(coreRequest).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({
  //     method: 'POST',
  //     url: '/integrations/arcgis/id2/test'
  //   }));
  // });

  it('toggleIntegrationStatus posts body {enabled}', async () => {
    (coreRequest as any).mockResolvedValueOnce(undefined);
    await ArcGISIntegrationService.toggleIntegrationStatus('id3', true);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/integrations/arcgis/id3/status',
        body: { enabled: true },
      })
    );
  });

  it('deleteIntegration uses DELETE', async () => {
    (coreRequest as any).mockResolvedValueOnce(undefined);
    await ArcGISIntegrationService.deleteIntegration('id4');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'DELETE',
        url: '/integrations/arcgis/id4',
      })
    );
  });

  it('listFeatureFields hits /connections/{id}/features', async () => {
    (coreRequest as any).mockResolvedValueOnce([{ name: 'field1' }]);
    const result = await ArcGISIntegrationService.listFeatureFields('conn1');
    expect(result).toEqual([{ name: 'field1' }]);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/arcgis/connections/conn1/features',
      })
    );
  });

  it('toggleArcGISIntegrationActive hits /toggle', async () => {
    (coreRequest as any).mockResolvedValueOnce(true);
    const result = await ArcGISIntegrationService.toggleArcGISIntegrationActive('id5');
    expect(result).toBe(true);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/integrations/arcgis/id5/toggle',
      })
    );
  });

  it('createIntegration posts to /integrations/arcgis', async () => {
    const requestBody = { name: 'Test Integration' } as any;
    (coreRequest as any).mockResolvedValueOnce({ id: 'new-id' });
    const result = await ArcGISIntegrationService.createIntegration(requestBody);
    expect(result).toEqual({ id: 'new-id' });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'POST',
        url: '/integrations/arcgis',
        body: requestBody,
        mediaType: 'application/json',
      })
    );
  });

  it('updateIntegration puts to /integrations/arcgis/{id}', async () => {
    const requestBody = { name: 'Updated Integration' } as any;
    (coreRequest as any).mockResolvedValueOnce(undefined);
    await ArcGISIntegrationService.updateIntegration('id6', requestBody);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'PUT',
        url: '/integrations/arcgis/id6',
        body: requestBody,
        mediaType: 'application/json',
      })
    );
  });

  it('getAllArcGISNormalizedNames fetches list of integration names', async () => {
    const mockNames = ['Integration_A', 'integration_b', 'Integration_C'];
    (coreRequest as any).mockResolvedValueOnce(mockNames);
    const names = await ArcGISIntegrationService.getAllArcGISNormalizedNames();
    expect(names).toEqual(mockNames);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/integrations/arcgis/normalized/names',
      })
    );
  });

  it('getAllArcGISNormalizedNames returns empty array when no integrations exist', async () => {
    (coreRequest as any).mockResolvedValueOnce([]);
    const names = await ArcGISIntegrationService.getAllArcGISNormalizedNames();
    expect(names).toEqual([]);
  });

  it('getAllArcGISNormalizedNames handles names with various formats', async () => {
    const mockNames = ['Test Daily', 'test_weekly', 'Test-Monthly', 'TEST QUARTERLY'];
    (coreRequest as any).mockResolvedValueOnce(mockNames);
    const names = await ArcGISIntegrationService.getAllArcGISNormalizedNames();
    expect(names).toEqual(mockNames);
    expect(names).toHaveLength(4);
  });
});
