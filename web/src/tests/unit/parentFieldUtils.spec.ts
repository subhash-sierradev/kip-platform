import { describe, expect, it } from 'vitest';

import { extractParentKey, parentLabelMatchesKey } from '@/utils/parentFieldUtils';

describe('parentFieldUtils.extractParentKey', () => {
  it('returns empty string for nullish values', () => {
    expect(extractParentKey(null)).toBe('');
    expect(extractParentKey(undefined)).toBe('');
  });

  it('returns key from object value when present', () => {
    expect(extractParentKey({ key: ' SCRUM-436 ' })).toBe('SCRUM-436');
  });

  it('returns empty string for object values without a string key', () => {
    expect(extractParentKey({ id: 123 })).toBe('');
    expect(extractParentKey({ key: 42 })).toBe('');
  });

  it('returns template token unchanged', () => {
    expect(extractParentKey('{{ fields.parent.key }}')).toBe('{{ fields.parent.key }}');
  });

  it('returns parsed key from json string payload', () => {
    expect(extractParentKey('{"key":"SCRUM-436"}')).toBe('SCRUM-436');
    expect(extractParentKey('{"key":"  SCRUM-500  "}')).toBe('SCRUM-500');
  });

  it('falls back to trimmed raw input for invalid or key-less payloads', () => {
    expect(extractParentKey(' SCRUM-777 ')).toBe('SCRUM-777');
    expect(extractParentKey('{"id":"123"}')).toBe('{"id":"123"}');
    expect(extractParentKey('{')).toBe('{');
  });
});

describe('parentFieldUtils.parentLabelMatchesKey', () => {
  it('matches exact key case-insensitively', () => {
    expect(parentLabelMatchesKey('scrum-436', 'SCRUM-436')).toBe(true);
  });

  it('matches key and summary format', () => {
    expect(parentLabelMatchesKey('SCRUM-436 - Parent one', 'SCRUM-436')).toBe(true);
  });

  it('does not match prefix collisions', () => {
    expect(parentLabelMatchesKey('SCRUM-101 - Parent one', 'SCRUM-10')).toBe(false);
  });

  it('returns false when label or key is empty after trim', () => {
    expect(parentLabelMatchesKey('   ', 'SCRUM-1')).toBe(false);
    expect(parentLabelMatchesKey('SCRUM-1 - Parent', '   ')).toBe(false);
  });
});
