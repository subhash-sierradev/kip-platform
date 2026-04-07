import { describe, expect, it } from 'vitest';

import { sidebarMenuDescriptions } from '@/components/layout/sidebarMenuDescriptions';

describe('sidebarMenuDescriptions', () => {
  it('contains core menu descriptions', () => {
    expect(sidebarMenuDescriptions.inbound.title).toBe('Inbound');
    expect(sidebarMenuDescriptions.outbound.title).toBe('Outbound');
    expect(sidebarMenuDescriptions.admin.title).toBe('Admin');
  });

  it('has non-empty descriptions', () => {
    for (const key of Object.keys(sidebarMenuDescriptions)) {
      const entry = sidebarMenuDescriptions[key];
      expect(entry.title.length).toBeGreaterThan(0);
      expect(entry.description.length).toBeGreaterThan(0);
    }
  });
});
