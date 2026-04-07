import { beforeEach, describe, expect, it, vi } from 'vitest';

import { request as coreRequest } from '@/api/core/request';
import { KwDocService } from '@/api/services/KwIntegrationService';

vi.mock('@/api/core/request', () => ({ request: vi.fn() }));

beforeEach(() => {
  (coreRequest as any).mockReset?.();
});

describe('SubItemTypeService', () => {
  it('transforms and sorts sub-item types for dropdown', async () => {
    (coreRequest as any).mockResolvedValueOnce([
      { code: 'B', displayValue: 'Bravo' },
      { code: 'A', displayValue: 'Alpha' },
    ]);
    const res = await KwDocService.getSubItemTypes();
    expect(res.map((r: any) => r.displayValue)).toEqual(['Bravo', 'Alpha']);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/kw/item-subtypes' })
    );
  });
});
