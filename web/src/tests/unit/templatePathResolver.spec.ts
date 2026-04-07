import { describe, expect, it } from 'vitest';

import {
  parseJsonSafe,
  resolvePathValue,
  resolvePurePlaceholderValue,
  resolveTemplateWithJson,
  resolveTemplateWithObject,
} from '@/components/outbound/jirawebhooks/utils/templatePathResolver';

describe('templatePathResolver', () => {
  describe('parseJsonSafe', () => {
    it('returns parsed object for valid object json', () => {
      expect(parseJsonSafe('{"a":1}')).toEqual({ a: 1 });
    });

    it('returns empty object for invalid or non-object json', () => {
      expect(parseJsonSafe('{bad')).toEqual({});
      expect(parseJsonSafe('[]')).toEqual([]);
      expect(parseJsonSafe('"text"')).toEqual({});
      expect(parseJsonSafe('null')).toEqual({});
    });
  });

  describe('resolveTemplateWithObject', () => {
    it('returns empty string for empty template', () => {
      expect(resolveTemplateWithObject('', { a: 1 })).toBe('');
    });

    it('resolves scalar placeholders', () => {
      const output = resolveTemplateWithObject('ID {{id}} / Name {{user.name}}', {
        id: 42,
        user: { name: 'Alice' },
      });
      expect(output).toBe('ID 42 / Name Alice');
    });

    it('removes unresolved placeholders by default and can preserve when requested', () => {
      expect(resolveTemplateWithObject('Issue {{missing}}', {})).toBe('Issue ');
      expect(resolveTemplateWithObject('Issue {{missing}}', {}, { preserveUnresolved: true })).toBe(
        'Issue {{missing}}'
      );
    });

    it('formats object values using preferred fields name/displayName/key', () => {
      const base = { obj1: { name: 'N' }, obj2: { displayName: 'DN' }, obj3: { key: 'K' } };
      expect(resolveTemplateWithObject('{{obj1}}|{{obj2}}|{{obj3}}', base)).toBe('N|DN|K');
    });

    it('falls back to json string for object without preferred fields', () => {
      const output = resolveTemplateWithObject('{{meta}}', { meta: { a: 1 } });
      expect(output).toBe('{"a":1}');
    });

    it('formats arrays and nested arrays while filtering empty values', () => {
      const output = resolveTemplateWithObject('{{vals}}', {
        vals: ['a', null, '  ', ['b', '', ['c']], { name: 'D' }],
      } as any);
      expect(output).toBe('a, b, c, D');
    });
  });

  describe('resolveTemplateWithJson', () => {
    it('parses json and resolves placeholders', () => {
      const output = resolveTemplateWithJson('Ticket {{issue.key}}', '{"issue":{"key":"SCRUM-1"}}');
      expect(output).toBe('Ticket SCRUM-1');
    });

    it('treats invalid json as empty object', () => {
      expect(resolveTemplateWithJson('Ticket {{issue.key}}', '{bad')).toBe('Ticket ');
    });
  });

  describe('resolvePurePlaceholderValue', () => {
    it('returns undefined for non-pure template and resolved value for pure one', () => {
      expect(resolvePurePlaceholderValue('Hello {{a}}', { a: 1 })).toBeUndefined();
      expect(resolvePurePlaceholderValue(' {{ a.b }} ', { a: { b: 'x' } })).toBe('x');
    });
  });

  describe('resolvePathValue', () => {
    const root = {
      user: { profile: { name: 'Alice', tags: ['one', 'two'] } },
      serials: [{ filingNumberDisplay: '2025-1' }, { filingNumberDisplay: '2025-2' }],
      emptySerials: [] as Array<Record<string, unknown>>,
      data: [{ key: 'A' }, { key: 'B' }],
      map: { 'complex.key': 'VALUE' },
    };

    it('returns undefined for empty path', () => {
      expect(resolvePathValue(root, '')).toBeUndefined();
      expect(resolvePathValue(root, '   ')).toBeUndefined();
    });

    it('supports dot and index notation', () => {
      expect(resolvePathValue(root, 'user.profile.name')).toBe('Alice');
      expect(resolvePathValue(root, 'user.profile.tags[1]')).toBe('two');
      expect(resolvePathValue(root, 'serials[0].filingNumberDisplay')).toBe('2025-1');
    });

    it('supports wildcard notation and returns flattened values', () => {
      const result = resolvePathValue(root, 'serials[].filingNumberDisplay');
      expect(result).toEqual(['2025-1', '2025-2']);
    });

    it('returns empty array when wildcard applied to an empty array', () => {
      const result = resolvePathValue(root, 'emptySerials[].filingNumberDisplay');
      expect(result).toEqual([]);
    });

    it('returns undefined for unresolved non-wildcard paths', () => {
      expect(resolvePathValue(root, 'serials[20].filingNumberDisplay')).toBeUndefined();
      expect(resolvePathValue(root, 'unknown.path')).toBeUndefined();
      expect(resolvePathValue(root, 'user.profile.tags[10]')).toBeUndefined();
    });

    it('supports bracket property expressions for quoted and unquoted keys', () => {
      const obj = {
        map: {
          alpha: { value: 1 },
          'complex.key': { value: 2 },
        },
      };

      expect(resolvePathValue(obj, 'map[alpha].value')).toBe(1);
      expect(resolvePathValue(obj, 'map["complex.key"].value')).toBe(2);
      expect(resolvePathValue(obj, "map['complex.key'].value")).toBe(2);
    });

    it('handles numeric property lookup against array current values', () => {
      expect(resolvePathValue({ arr: ['x', 'y', 'z'] }, 'arr.1')).toBe('y');
    });

    it('returns collection when wildcard leaves multiple terminal values', () => {
      const value = resolvePathValue(root, 'data[].key');
      expect(value).toEqual(['A', 'B']);
    });
  });
});
