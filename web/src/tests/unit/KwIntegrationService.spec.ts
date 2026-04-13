import { describe, expect, it, vi } from 'vitest';

const requestMock = vi.hoisted(() => vi.fn());

vi.mock('@/api/core/request', () => ({
  request: requestMock,
}));

import { KwDocService } from '@/api/services/KwIntegrationService';

describe('KwDocService', () => {
  it('requests dynamic documents and returns the response payload', async () => {
    const response = [{ id: 'doc-1', name: 'Dynamic Doc' }];
    requestMock.mockResolvedValueOnce(response);

    await expect(KwDocService.getDynamicDocuments('CASE', 'REPORT')).resolves.toEqual(response);
    expect(requestMock).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/kw/dynamic-documents-types',
        query: { type: 'CASE', subType: 'REPORT' },
      })
    );
  });

  it('requests sub item types', async () => {
    const response = [{ code: 'DOC' }];
    requestMock.mockResolvedValueOnce(response);

    await expect(KwDocService.getSubItemTypes()).resolves.toEqual(response);
    expect(requestMock).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/kw/item-subtypes' })
    );
  });

  it('requests source field mappings', async () => {
    const response = [{ fieldName: 'title' }];
    requestMock.mockResolvedValueOnce(response);

    await expect(KwDocService.getSourceFieldMappings()).resolves.toEqual(response);
    expect(requestMock).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/kw/source-field-mappings' })
    );
  });
});
