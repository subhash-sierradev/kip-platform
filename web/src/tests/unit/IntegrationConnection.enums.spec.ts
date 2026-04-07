/* eslint-disable simple-import-sort/imports */
import { describe, it, expect } from 'vitest';

import { ConnectionStatus, FetchMode } from '@/api/models/IntegrationConnection';

describe('IntegrationConnection enums', () => {
  it('ConnectionStatus has expected values', () => {
    expect(ConnectionStatus.ACTIVE).toBe('ACTIVE');
    expect(ConnectionStatus.INACTIVE).toBe('INACTIVE');
    expect(ConnectionStatus.SUCCESS).toBe('SUCCESS');
    expect(ConnectionStatus.FAILED).toBe('FAILED');
    expect(ConnectionStatus.ERROR).toBe('ERROR');

    expect(Object.keys(ConnectionStatus)).toHaveLength(5);
  });

  it('FetchMode has expected values', () => {
    expect(FetchMode.GET).toBe('GET');
    expect(FetchMode.POST).toBe('POST');
    expect(Object.keys(FetchMode)).toHaveLength(2);
  });
});
