/* eslint-disable simple-import-sort/imports */
import { describe, it, expect } from 'vitest';
import { siteConfigColumns } from '@/components/admin/adminColumn';

describe('adminColumn', () => {
  it('siteConfigColumns contains expected fields and captions', () => {
    const fields = siteConfigColumns.map(c => c.dataField);
    const captions = siteConfigColumns.map(c => c.caption);
    expect(fields).toEqual(['id', 'value', 'type', 'description', 'lastModifiedDate']);
    expect(captions).toEqual(['Key', 'Value', 'Type', 'Description', 'Last Modified']);

    // Templates present for specific fields
    const keyCol = siteConfigColumns.find(c => c.dataField === 'id');
    const valueCol = siteConfigColumns.find(c => c.dataField === 'value');
    const typeCol = siteConfigColumns.find(c => c.dataField === 'type');
    const dateCol = siteConfigColumns.find(c => c.dataField === 'lastModifiedDate');
    expect(keyCol?.cellTemplate).toBe('keyTemplate');
    expect(valueCol?.cellTemplate).toBe('valueTemplate');
    expect(typeCol?.cellTemplate).toBe('typeTemplate');
    expect(dateCol?.cellTemplate).toBe('dateTemplate');
  });
});
