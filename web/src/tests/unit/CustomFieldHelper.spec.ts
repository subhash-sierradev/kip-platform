import { describe, expect, it } from 'vitest';

import {
  computeSelectedUserNames,
  deriveAllowedValues,
  deriveType,
  filterUsers,
  generateUid,
  INCLUDE_KEYS,
  isRowsValid,
  type JiraApiField,
  type JiraUser,
  mapAndFilterFields,
  previewText,
  sortFields,
  toggleArrayValueValue,
} from '@/components/outbound/jirawebhooks/utils/CustomFieldHelper';

describe('CustomFieldHelper (unit)', () => {
  describe('deriveType', () => {
    const cf = (custom: string, type = ''): JiraApiField => ({
      schemaDetails: { custom, type },
      custom: true,
    });

    it('maps direct system types', () => {
      expect(deriveType({ schemaDetails: { type: 'number' } })).toBe('number');
      expect(deriveType({ schemaDetails: { type: 'boolean' } })).toBe('boolean');
      expect(deriveType({ schemaDetails: { type: 'string' } })).toBe('string');
      expect(deriveType({ schemaDetails: { type: 'date' } })).toBe('date');
      expect(deriveType({ schemaDetails: { type: 'datetime' } })).toBe('datetime');
      expect(deriveType({ schemaDetails: { type: 'user' } })).toBe('user');
    });

    it('maps custom user picker types', () => {
      expect(deriveType(cf('multiuserpicker'))).toBe('multiuser');
      expect(deriveType(cf('people'))).toBe('multiuser');
      expect(deriveType(cf('userpicker'))).toBe('user');
    });

    it('maps custom option/labels', () => {
      expect(deriveType(cf('multicheckboxes'))).toBe('multichecklist');
      expect(deriveType(cf('labels'))).toBe('labels');
      expect(deriveType(cf('select', 'option'))).toBe('option');
    });

    it('maps date/datetime/number custom types', () => {
      expect(deriveType(cf('datepicker'))).toBe('date');
      expect(deriveType(cf('datetime'))).toBe('datetime');
      expect(deriveType(cf('float'))).toBe('number');
    });

    it('maps service desk and agile custom types', () => {
      expect(deriveType(cf('sd-request-feedback-date'))).toBe('datetime');
      expect(deriveType(cf('sd-request-participants'))).toBe('multiuser');
      expect(deriveType(cf('sd-customerrequesttype'))).toBe('option');
      expect(deriveType(cf('sd-request-feedback'))).toBe('number');
      expect(deriveType(cf('sd-customer-organizations'))).toBe('array');
      expect(deriveType(cf('gh-epic-label'))).toBe('string');
      expect(deriveType(cf('gh-epic-status'))).toBe('option');
      expect(deriveType(cf('gh-epic-link'))).toBe('string');
      expect(deriveType(cf('gh-sprint'))).toBe('sprint');
      expect(deriveType(cf('jsw-story-points'))).toBe('number');
    });

    it('maps miscellaneous custom types', () => {
      expect(deriveType(cf('devsummarycf'))).toBe('object');
      expect(deriveType(cf('vulnerabilitycf'))).toBe('string');
      expect(deriveType(cf('forms-total-field-cftype'))).toBe('number');
      expect(deriveType(cf('forms-open-field-cftype'))).toBe('number');
      expect(deriveType(cf('forms-locked-field-cftype'))).toBe('number');
    });

    it('falls back to array when type is array', () => {
      expect(deriveType({ schemaDetails: { type: 'array' } })).toBe('array');
    });

    it('defaults to string', () => {
      expect(deriveType({})).toBe('string');
    });
  });

  describe('deriveAllowedValues', () => {
    it('returns values from string array', () => {
      const f: JiraApiField = { allowedValues: ['A', 'B'] };
      expect(deriveAllowedValues(f)).toEqual(['A', 'B']);
    });
    it('returns values from object array', () => {
      const f: JiraApiField = { allowedValues: [{ value: 'X' }, { name: 'Y' }, { id: 'Z' }] };
      expect(deriveAllowedValues(f)).toEqual(['X', 'Y', 'Z']);
    });
  });

  describe('mapAndFilterFields + sortFields', () => {
    it('filters exclude keys case-insensitively and maps meta', () => {
      const api: JiraApiField[] = [
        { key: 'Summary', name: 'Summary', custom: false, schemaDetails: { type: 'string' } },
        {
          key: 'customfield_10001',
          name: 'My CF',
          custom: true,
          schemaDetails: { custom: 'userpicker' },
        },
      ];
      const include = [...INCLUDE_KEYS, 'customfield_10001'];
      const mapped = mapAndFilterFields(api, include);
      // Summary excluded
      expect(mapped.find(m => m.key.toLowerCase() === 'summary')).toBeUndefined();
      // Custom field present and typed
      const cf = mapped.find(m => m.key === 'customfield_10001')!;
      expect(cf.name).toBe('My CF');
      expect(cf.isCustom).toBe(true);
      expect(cf.type).toBe('user');
    });

    it('sorts system before custom and alphabetically', () => {
      const metas = [
        { key: 'b', name: 'B', type: 'string', allowedValues: [], isCustom: false },
        { key: 'a', name: 'A', type: 'string', allowedValues: [], isCustom: true },
        { key: 'c', name: 'C', type: 'string', allowedValues: [], isCustom: false },
      ];
      const sorted = [...metas].sort(sortFields);
      // system first (b, c) then custom (a), and within groups alphabetical
      expect(sorted.map(m => m.key)).toEqual(['b', 'c', 'a']);
    });
  });

  describe('isRowsValid', () => {
    it('treats empty field as valid regardless of value', () => {
      expect(isRowsValid([{ field: '', value: '' }])).toBe(true);
      expect(isRowsValid([{ field: '', value: 'x' }])).toBe(true);
    });
    it('requires non-empty value when field selected', () => {
      expect(isRowsValid([{ field: 'a', value: '' }])).toBe(false);
      expect(isRowsValid([{ field: 'a', value: 'x' }])).toBe(true);
    });
  });

  describe('toggleArrayValueValue', () => {
    it('adds and removes items with join/trim', () => {
      let v = '';
      v = toggleArrayValueValue(v, 'A');
      expect(v).toBe('A');
      v = toggleArrayValueValue(v, 'B');
      expect(v).toBe('A, B');
      v = toggleArrayValueValue(v, 'A');
      expect(v).toBe('B');
    });
  });

  describe('computeSelectedUserNames + filterUsers', () => {
    const users: JiraUser[] = [
      { accountId: 'u1', displayName: 'Alice' },
      { accountId: 'u2', displayName: 'Bob' },
      { accountId: 'u3', displayName: 'Charlie' },
    ];

    it('maps ids to display names', () => {
      expect(computeSelectedUserNames('u1,u3', users)).toEqual(['Alice', 'Charlie']);
    });

    it('filters users by query substring (case-insensitive)', () => {
      expect(filterUsers(users, 'ali').map(u => u.displayName)).toEqual(['Alice']);
      expect(filterUsers(users, '').length).toBe(users.length);
    });
  });

  describe('previewText', () => {
    it('prefers parsed over raw when non-empty', () => {
      expect(previewText('Parsed', 'Raw')).toBe('Parsed');
      expect(previewText('   ', 'Raw')).toBe('Raw');
      expect(previewText(undefined, '')).toBe('Preview is empty');
    });
  });

  describe('generateUid', () => {
    it('returns a string containing a dash', () => {
      const id = generateUid();
      expect(typeof id).toBe('string');
      expect(id.includes('-')).toBe(true);
    });
  });
});
