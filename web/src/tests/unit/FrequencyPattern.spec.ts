/* eslint-disable simple-import-sort/imports */
import { describe, it, expect } from 'vitest';

import { FrequencyPattern } from '@/api/models/FrequencyPattern';

describe('FrequencyPattern enum', () => {
  it('contains expected keys and values', () => {
    expect(FrequencyPattern.DAILY).toBe('DAILY');
    expect(FrequencyPattern.WEEKLY).toBe('WEEKLY');
    expect(FrequencyPattern.MONTHLY).toBe('MONTHLY');
    expect(FrequencyPattern.CRON_EXPRESSION).toBe('CUSTOM');
    expect(Object.keys(FrequencyPattern)).toHaveLength(4);
  });
});
