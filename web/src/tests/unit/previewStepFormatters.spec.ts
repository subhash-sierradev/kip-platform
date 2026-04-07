import { describe, expect, it, vi } from 'vitest';

vi.mock('@/utils/dateUtils', () => ({
  formatDisplayDate: vi.fn(() => 'DISPLAY_DATE'),
  formatMetadataDate: vi.fn(() => 'META_DATE'),
}));

import {
  formatCustomValue,
  formatJsonSample,
  formatParentName,
  getFieldLabel,
  resolveTemplate,
  toSprintMap,
  toUserMap,
} from '@/components/outbound/jirawebhooks/utils/previewStepFormatters';

describe('previewStepFormatters', () => {
  it('resolveTemplate resolves nested paths and preserves unresolved placeholders', () => {
    const jsonSample = JSON.stringify({ fields: { summary: 'Issue summary' } });
    const output = resolveTemplate(
      'Summary: {{ fields.summary }} / {{ fields.missing }}',
      jsonSample
    );
    expect(output).toBe('Summary: Issue summary / {{fields.missing}}');
  });

  it('resolveTemplate returns original template when json is invalid', () => {
    const template = 'Summary: {{ fields.summary }}';
    expect(resolveTemplate(template, '{invalid-json')).toBe(template);
  });

  it('formatJsonSample pretty prints valid json and returns raw value for invalid json', () => {
    expect(formatJsonSample('{"a":1}')).toContain('\n  "a": 1\n');
    expect(formatJsonSample('{oops')).toBe('{oops');
  });

  it('formatCustomValue maps users and arrays from json templates', () => {
    const userMap = toUserMap([{ accountId: 'u1', displayName: 'Alice' }]);
    const sprintMap = toSprintMap([]);

    const userField = {
      jiraFieldKey: 'assignee',
      jiraFieldLabel: 'Assignee',
      type: 'user',
      valueSource: 'json',
      value: '{{fields.assigneeId}}',
    } as any;

    const labelsField = {
      jiraFieldKey: 'labels',
      jiraFieldLabel: 'Labels',
      type: 'array',
      valueSource: 'json',
      value: '{{fields.labels}}',
    } as any;

    const jsonSample = JSON.stringify({ fields: { assigneeId: 'u1', labels: ['one', 'two'] } });

    expect(formatCustomValue(userField, jsonSample, userMap, sprintMap)).toBe('Alice');
    expect(formatCustomValue(labelsField, jsonSample, userMap, sprintMap)).toEqual(['one', 'two']);
  });

  it('formatCustomValue uses sprint labels and datetime formatter', () => {
    const userMap = toUserMap([]);
    const sprintMap = toSprintMap([{ value: '67', label: 'Sprint 24 (active)' }]);

    const sprintField = {
      jiraFieldKey: 'customfield_10021',
      jiraFieldLabel: 'Sprint',
      type: 'string',
      valueSource: 'literal',
      value: '67',
    } as any;

    const dateTimeField = {
      jiraFieldKey: 'duedate',
      jiraFieldLabel: 'Due Date',
      type: 'datetime',
      valueSource: 'literal',
      value: '2025-05-01T08:30:00.000Z',
    } as any;

    expect(formatCustomValue(sprintField, '{}', userMap, sprintMap)).toBe('Sprint 24 (active)');
    expect(formatCustomValue(dateTimeField, '{}', userMap, sprintMap)).toBe('META_DATE');
  });

  it('formatParentName returns key for literal or json-source values', () => {
    const literal = formatParentName(
      [
        {
          jiraFieldKey: 'parent',
          jiraFieldLabel: 'Parent',
          valueSource: 'literal',
          value: '{"key":"SCRUM-436"}',
        } as any,
      ],
      '{}'
    );

    const fromJson = formatParentName(
      [
        {
          jiraFieldKey: 'parent',
          jiraFieldLabel: 'Parent',
          valueSource: 'json',
          value: '{{fields.parent}}',
        } as any,
      ],
      JSON.stringify({ fields: { parent: { key: 'SCRUM-999' } } })
    );

    expect(literal).toBe('SCRUM-436');
    expect(fromJson).toBe('SCRUM-999');
  });

  it('formatParentName and getFieldLabel return fallbacks when values are missing', () => {
    expect(formatParentName([], '{}')).toBe('Not Selected');
    expect(getFieldLabel({ jiraFieldLabel: '  ' } as any)).toBe('Custom Field');
  });

  it('resolveTemplate stringifies object placeholders and handles null placeholders', () => {
    const jsonSample = JSON.stringify({
      fields: {
        parent: { key: 'SCRUM-22' },
        optionalValue: null,
      },
    });

    const output = resolveTemplate(
      'Parent={{fields.parent}};Optional={{fields.optionalValue}}',
      jsonSample
    );
    expect(output).toContain('"key": "SCRUM-22"');
    expect(output).toContain('Optional=');
  });

  it('formatCustomValue covers number, boolean, fallback and multiuser branches', () => {
    const userMap = toUserMap([
      { accountId: 'u1', displayName: 'Alice' },
      { accountId: 'u2', displayName: 'Bob' },
    ]);
    const sprintMap = toSprintMap([]);

    const numberField = {
      jiraFieldKey: 'num',
      jiraFieldLabel: 'Number',
      type: 'number',
      valueSource: 'literal',
      value: '123.5',
    } as any;

    const booleanField = {
      jiraFieldKey: 'flag',
      jiraFieldLabel: 'Flag',
      type: 'boolean',
      valueSource: 'literal',
      value: 'true',
    } as any;

    const multiUserField = {
      jiraFieldKey: 'watchers',
      jiraFieldLabel: 'Watchers',
      type: 'multiuser',
      valueSource: 'literal',
      value: 'u1,u3',
    } as any;

    const unknownTypeField = {
      jiraFieldKey: 'unknown',
      jiraFieldLabel: 'Unknown',
      type: 'custom-unknown-type',
      valueSource: 'literal',
      value: { nested: true },
    } as any;

    expect(formatCustomValue(numberField, '{}', userMap, sprintMap)).toBe('123.5');
    expect(formatCustomValue(booleanField, '{}', userMap, sprintMap)).toBe('True');
    expect(formatCustomValue(multiUserField, '{}', userMap, sprintMap)).toBe('Alice, u3');
    expect(formatCustomValue(unknownTypeField, '{}', userMap, sprintMap)).toBe('{"nested":true}');
  });

  it('formatCustomValue returns raw datetime when date cannot be parsed', () => {
    const rawDateTimeField = {
      jiraFieldKey: 'raw-date',
      jiraFieldLabel: 'Raw Date',
      type: 'datetime',
      valueSource: 'literal',
      value: 'not-a-real-date',
    } as any;

    expect(formatCustomValue(rawDateTimeField, '{}', toUserMap([]), toSprintMap([]))).toBe(
      'not-a-real-date'
    );
  });

  it('formatParentName returns raw parent string when json parsing fails', () => {
    const value = formatParentName(
      [
        {
          jiraFieldKey: 'parent',
          jiraFieldLabel: 'Parent',
          valueSource: 'literal',
          value: 'SCRUM-501',
        } as any,
      ],
      '{}'
    );

    expect(value).toBe('SCRUM-501');
  });

  it('getFieldLabel returns trimmed custom label', () => {
    expect(getFieldLabel({ jiraFieldLabel: '  Epic Link  ' } as any)).toBe('Epic Link');
  });

  it('resolveTemplate returns empty output for empty template input', () => {
    expect(resolveTemplate('', '{}')).toBe('');
  });

  it('formatCustomValue covers display-date branch and unresolved json placeholders', () => {
    const dateOnlyField = {
      jiraFieldKey: 'duedate',
      jiraFieldLabel: 'Due Date',
      type: 'date',
      valueSource: 'literal',
      value: '2025-05-01',
    } as any;

    const unresolvedJsonField = {
      jiraFieldKey: 'description',
      jiraFieldLabel: 'Description',
      type: 'string',
      valueSource: 'json',
      value: '{{fields.missingPath}}',
    } as any;

    expect(formatCustomValue(dateOnlyField, '{}', toUserMap([]), toSprintMap([]))).toBe(
      'DISPLAY_DATE'
    );
    expect(formatCustomValue(unresolvedJsonField, '{}', toUserMap([]), toSprintMap([]))).toBe('');
  });

  it('formatCustomValue handles multiuser arrays and invalid numeric values', () => {
    const userMap = toUserMap([
      { accountId: 'u1', displayName: 'Alice' },
      { accountId: 'u2', displayName: 'Bob' },
    ]);

    const multiUserArrayField = {
      jiraFieldKey: 'watchers',
      jiraFieldLabel: 'Watchers',
      type: 'multiuser',
      valueSource: 'literal',
      value: ['u1', 'u2'],
    } as any;

    const invalidNumberField = {
      jiraFieldKey: 'estimate',
      jiraFieldLabel: 'Estimate',
      type: 'number',
      valueSource: 'literal',
      value: 'not-a-number',
    } as any;

    expect(formatCustomValue(multiUserArrayField, '{}', userMap, toSprintMap([]))).toBe(
      'Alice, Bob'
    );
    expect(formatCustomValue(invalidNumberField, '{}', userMap, toSprintMap([]))).toBe(
      'not-a-number'
    );
  });

  it('formatCustomValue handles sprint arrays and defaults empty parent field values', () => {
    const sprintField = {
      jiraFieldKey: 'customfield_10021',
      jiraFieldLabel: 'Sprint',
      type: 'sprint',
      valueSource: 'literal',
      value: ['67', '99'],
    } as any;

    const sprintMap = toSprintMap([
      { value: '67', label: 'Sprint 24' },
      { value: '99', label: 'Sprint 25' },
    ]);

    const parent = formatParentName(
      [
        {
          jiraFieldKey: 'parent',
          jiraFieldLabel: 'Parent',
          valueSource: 'literal',
          value: '   ',
        } as any,
      ],
      '{}'
    );

    expect(formatCustomValue(sprintField, '{}', toUserMap([]), sprintMap)).toBe(
      'Sprint 24, Sprint 25'
    );
    expect(parent).toBe('');
  });

  it('formatParentName handles object parent values without a key', () => {
    const value = formatParentName(
      [
        {
          jiraFieldKey: 'parent',
          jiraFieldLabel: 'Parent',
          valueSource: 'literal',
          value: { id: '10045' },
        } as any,
      ],
      '{}'
    );

    expect(value).toBe('[object Object]');
  });

  it('formatCustomValue formats boolean false value', () => {
    const field = {
      jiraFieldKey: 'flag',
      jiraFieldLabel: 'Flag',
      type: 'boolean',
      valueSource: 'literal',
      value: 'false',
    } as any;

    expect(formatCustomValue(field, '{}', toUserMap([]), toSprintMap([]))).toBe('False');
  });

  it('formatParentName returns empty when json template path is unresolved', () => {
    const value = formatParentName(
      [
        {
          jiraFieldKey: 'parent',
          jiraFieldLabel: 'Parent',
          valueSource: 'json',
          value: '{{fields.parentMissing}}',
        } as any,
      ],
      JSON.stringify({ fields: { other: 'value' } })
    );

    expect(value).toBe('');
  });
});
