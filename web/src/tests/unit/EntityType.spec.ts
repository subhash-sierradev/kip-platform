/* eslint-disable simple-import-sort/imports */
import { describe, it, expect } from 'vitest';

import { EntityType } from '@/api/models/EntityType';

describe('EntityType enum', () => {
  it('contains expected keys and values', () => {
    expect(EntityType.INTEGRATION).toBe('INTEGRATION');
    expect(EntityType.JIRA_WEBHOOK).toBe('JIRA_WEBHOOK');
    expect(EntityType.JIRA_WEBHOOK_EVENT).toBe('JIRA_WEBHOOK_EVENT');
    expect(EntityType.SITE_CONFIG).toBe('SITE_CONFIG');
    expect(EntityType.CACHE).toBe('CACHE');

    // Sanity: total entries
    expect(Object.keys(EntityType)).toHaveLength(5);
  });
});
