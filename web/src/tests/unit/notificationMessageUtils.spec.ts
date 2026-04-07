import { describe, expect, it } from 'vitest';

import { localizeEmbeddedIsoTimestamps } from '@/utils/notificationMessageUtils';

describe('notificationMessageUtils', () => {
  it('replaces embedded ISO UTC timestamps with localized display text', () => {
    const input = 'Event happened on 2026-02-25T18:32:28.820396500Z.';
    const output = localizeEmbeddedIsoTimestamps(input);

    expect(output).toBeTypeOf('string');
    expect(output).not.toContain('2026-02-25T18:32:28.820396500Z');
    // Should not leak raw ISO style markers
    expect(output).not.toContain('T18:32:28');
    expect(output).not.toContain('Z');
  });

  it('does not change message when there is no ISO timestamp', () => {
    const input = 'No timestamp here.';
    expect(localizeEmbeddedIsoTimestamps(input)).toBe(input);
  });

  it('replaces multiple embedded timestamps', () => {
    const input = 'Start 2026-02-25T00:00:00Z and end 2026-02-25T01:02:03.123456789Z.';
    const output = localizeEmbeddedIsoTimestamps(input);

    expect(output).not.toContain('2026-02-25T00:00:00Z');
    expect(output).not.toContain('2026-02-25T01:02:03.123456789Z');
  });
});
