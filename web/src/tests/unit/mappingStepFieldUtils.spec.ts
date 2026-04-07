import { describe, expect, it, vi } from 'vitest';

vi.mock('@/components/outbound/jirawebhooks/utils/CustomFieldHelper', () => ({
  generateUid: vi.fn(() => 'uid-fixed'),
}));

import {
  type MappingRow,
  mergeRowsIntoCustomFields,
  toNonParentRows,
} from '@/components/outbound/jirawebhooks/utils/mappingStepFieldUtils';

describe('mappingStepFieldUtils', () => {
  it('toNonParentRows excludes parent field and maps editable rows', () => {
    const rows = toNonParentRows([
      {
        jiraFieldKey: 'parent',
        jiraFieldLabel: 'Parent',
        value: '{"key":"SCRUM-1"}',
        valueSource: 'literal',
      } as any,
      {
        jiraFieldKey: 'customfield_10010',
        jiraFieldLabel: 'Environment',
        value: 'Sandbox',
        valueSource: 'literal',
        type: 'string',
      } as any,
    ]);

    expect(rows).toEqual([
      {
        field: 'customfield_10010',
        value: 'Sandbox',
        label: 'Environment',
        type: 'string',
      },
    ]);
  });

  it('mergeRowsIntoCustomFields updates existing mapping and keeps parent mapping', () => {
    const nextRows: MappingRow[] = [
      { field: 'customfield_10010', value: '{{fields.env}}', label: 'Environment', type: 'string' },
    ];

    const merged = mergeRowsIntoCustomFields(nextRows, [
      {
        _id: 'row-1',
        jiraFieldKey: 'customfield_10010',
        jiraFieldLabel: 'Environment',
        value: 'Old',
        valueSource: 'literal',
        type: 'string',
      } as any,
      {
        _id: 'parent-1',
        jiraFieldKey: 'parent',
        jiraFieldLabel: 'Parent',
        value: '{"key":"SCRUM-7"}',
        valueSource: 'literal',
        type: 'object',
      } as any,
    ]);

    const updated = merged.find(field => field.jiraFieldKey === 'customfield_10010');
    const parent = merged.find(field => field.jiraFieldKey === 'parent');

    expect(updated?.value).toBe('{{fields.env}}');
    expect(updated?.valueSource).toBe('json');
    expect(parent?.value).toBe('{"key":"SCRUM-7"}');
  });

  it('mergeRowsIntoCustomFields creates new rows and ignores blank row keys', () => {
    const merged = mergeRowsIntoCustomFields(
      [
        { field: '  ', value: 'skip-me' },
        { field: 'customfield_10011', value: '42', label: 'Estimate', type: 'number' },
      ],
      []
    );

    expect(merged).toHaveLength(1);
    expect(merged[0]).toMatchObject({
      _id: 'uid-fixed',
      jiraFieldKey: 'customfield_10011',
      jiraFieldLabel: 'Estimate',
      value: '42',
      valueSource: 'literal',
      type: 'number',
    });
  });

  it('mergeRowsIntoCustomFields falls back existing valueSource when value is not a template', () => {
    const merged = mergeRowsIntoCustomFields(
      [{ field: 'customfield_20001', value: 'plain-text', label: 'Notes', type: 'string' }],
      [
        {
          _id: 'row-legacy',
          jiraFieldKey: 'customfield_20001',
          jiraFieldLabel: 'Notes',
          value: 'old',
          valueSource: 'literal',
          type: 'string',
        } as any,
      ]
    );

    expect(merged).toHaveLength(1);
    expect(merged[0].valueSource).toBe('literal');
    expect(merged[0].jiraFieldLabel).toBe('Notes');
  });

  it('mergeRowsIntoCustomFields returns empty output when no usable rows or parent exist', () => {
    const merged = mergeRowsIntoCustomFields([{ field: '', value: '' }], []);
    expect(merged).toEqual([]);
  });

  it('toNonParentRows handles undefined input', () => {
    expect(toNonParentRows(undefined)).toEqual([]);
  });

  it('mergeRowsIntoCustomFields keeps only one field for duplicate keys (last wins)', () => {
    const merged = mergeRowsIntoCustomFields(
      [
        { field: 'customfield_30001', value: 'first', label: 'Duplicate 1' },
        { field: 'customfield_30001', value: 'second', label: 'Duplicate 2' },
      ],
      []
    );

    expect(merged).toHaveLength(1);
    expect(merged[0].jiraFieldKey).toBe('customfield_30001');
    expect(merged[0].value).toBe('second');
    expect(merged[0].jiraFieldLabel).toBe('Duplicate 2');
  });

  it('does not append extra parent when parent is already present in merged rows', () => {
    const merged = mergeRowsIntoCustomFields(
      [{ field: 'parent', value: '{"key":"SCRUM-111"}', label: 'Parent', type: 'object' }],
      [
        {
          _id: 'parent-existing',
          jiraFieldKey: 'parent',
          jiraFieldLabel: 'Parent',
          value: '{"key":"SCRUM-100"}',
          valueSource: 'literal',
          type: 'object',
        } as any,
      ]
    );

    expect(merged.filter(field => field.jiraFieldKey === 'parent')).toHaveLength(1);
    expect(merged[0].value).toBe('{"key":"SCRUM-111"}');
  });

  it('defaults valueSource to literal for updated existing row without valueSource', () => {
    const merged = mergeRowsIntoCustomFields(
      [{ field: 'customfield_40001', value: 'plain', type: 'string' }],
      [
        {
          _id: 'legacy-no-source',
          jiraFieldKey: 'customfield_40001',
          jiraFieldLabel: 'Legacy',
          value: 'previous',
          type: 'string',
        } as any,
      ]
    );

    expect(merged[0].valueSource).toBe('literal');
    expect(merged[0].jiraFieldLabel).toBe('Legacy');
  });
});
