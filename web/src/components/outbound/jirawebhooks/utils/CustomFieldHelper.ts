import { FieldMeta } from '@/api/models/jirawebhook/FieldMeta';
import { JiraApiField } from '@/api/models/jirawebhook/JiraApiField';
import { JiraApiFieldAllowedValue } from '@/api/models/jirawebhook/JiraApiFieldAllowedValue';
import { JiraUser } from '@/api/models/jirawebhook/JiraUser';

export type { JiraApiField, JiraUser };

export const INCLUDE_KEYS: string[] = [
  'creator',
  'duedate',
  'timeestimate',
  'reporter',
  'resolutiondate',
  'timespent',
  'updated',
  'workratio',
  'customfield_10003',
  'customfield_10008',
  'customfield_10009',
  'customfield_10015',
  'customfield_10016',
  'customfield_10020',
  'customfield_10022',
  'customfield_10023',
  'customfield_10036',
  'customfield_10041',
  'customfield_10044',
  'customfield_10045',
  'customfield_10046',
  'customfield_10048',
  'customfield_10051',
  'customfield_10052',
  'customfield_10053',
];

export function generateUid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`;
}

function includesAny(source: string, needles: string[]): boolean {
  for (const n of needles) if (source.includes(n)) return true;
  return false;
}

function typeFromDirect(type: string): string | null {
  const directTypeMap: Record<string, string> = {
    number: 'number',
    boolean: 'boolean',
    string: 'string',
    date: 'date',
    datetime: 'datetime',
    user: 'user',
    project: 'project',
    issuetype: 'issuetype',
    priority: 'priority',
    resolution: 'resolution',
    securitylevel: 'securitylevel',
    status: 'status',
    progress: 'progress',
  };
  return directTypeMap[type] ?? null;
}

function typeFromCustom(custom: string, type: string): string | null {
  const rules: Array<{ test: (c: string, t: string) => boolean; value: string }> = [
    { test: c => includesAny(c, ['atlassian-team']), value: 'team' },
    { test: c => includesAny(c, ['multiuserpicker', 'people']), value: 'multiuser' },
    { test: c => includesAny(c, ['userpicker']), value: 'user' },
    { test: c => includesAny(c, ['multicheckboxes']), value: 'multichecklist' },
    { test: (c, t) => c.includes('select') && t === 'option', value: 'option' },
    { test: c => includesAny(c, ['labels']), value: 'labels' },
    { test: c => includesAny(c, ['datepicker']), value: 'date' },
    { test: c => includesAny(c, ['datetime']), value: 'datetime' },
    { test: c => includesAny(c, ['float']), value: 'number' },
    { test: c => includesAny(c, ['sd-request-feedback-date']), value: 'datetime' },
    { test: c => includesAny(c, ['sd-request-participants']), value: 'multiuser' },
    { test: c => includesAny(c, ['sd-customerrequesttype']), value: 'option' },
    { test: c => includesAny(c, ['sd-request-feedback']), value: 'number' },
    { test: c => includesAny(c, ['sd-customer-organizations']), value: 'array' },
    { test: c => includesAny(c, ['gh-epic-label']), value: 'string' },
    { test: c => includesAny(c, ['gh-epic-status']), value: 'option' },
    { test: c => includesAny(c, ['gh-epic-link']), value: 'string' },
    { test: c => includesAny(c, ['gh-sprint']), value: 'sprint' },
    { test: c => includesAny(c, ['jsw-story-points']), value: 'number' },
    { test: c => includesAny(c, ['devsummarycf']), value: 'object' },
    { test: c => includesAny(c, ['vulnerabilitycf']), value: 'string' },
    {
      test: c =>
        includesAny(c, [
          'forms-total-field-cftype',
          'forms-open-field-cftype',
          'forms-locked-field-cftype',
        ]),
      value: 'number',
    },
    { test: (_, t) => t === 'array', value: 'array' },
  ];
  for (const rule of rules) {
    if (rule.test(custom, type)) return rule.value;
  }
  return null;
}

export function deriveType(f: JiraApiField): string {
  const type = String(f?.schemaDetails?.type || f?.schema || '').toLowerCase();
  const custom = String(f?.schemaDetails?.custom || '').toLowerCase();
  const direct = typeFromDirect(type);
  if (direct) return direct;
  const fromCustom = typeFromCustom(custom, type);
  return fromCustom ?? 'string';
}

export function deriveAllowedValues(f: JiraApiField): string[] {
  return Array.isArray(f?.allowedValues)
    ? f.allowedValues.map((v: JiraApiFieldAllowedValue | string) =>
        typeof v === 'string' ? v : (v?.value ?? v?.name ?? v?.id ?? '')
      )
    : [];
}

export function mapAndFilterFields(apiFields: JiraApiField[], includeKeys: string[]): FieldMeta[] {
  const includeSet = new Set((includeKeys || []).map(k => String(k).toLowerCase()));

  return (apiFields || [])
    .filter((f: JiraApiField) => {
      const lowerKey = String(f?.key || f?.id || '').toLowerCase();
      if (!lowerKey) return false;
      return includeSet.has(lowerKey);
    })
    .map((f: JiraApiField) => ({
      key: String(f.key || f.id || ''),
      name: String(f.name || ''),
      type: deriveType(f),
      allowedValues: deriveAllowedValues(f),
      isCustom: f.custom === true,
    }));
}

export function sortFields(a: FieldMeta, b: FieldMeta): number {
  if (a.isCustom !== b.isCustom) return a.isCustom ? 1 : -1;
  return a.name.localeCompare(b.name);
}

export function isRowsValid(rows: Array<{ field: string; value: string }>): boolean {
  return rows.every(r => !r.field || r.value !== '');
}

export function toggleArrayValueValue(currentValue: string, opt: string): string {
  const arr = currentValue ? currentValue.split(',').map(v => v.trim()) : [];
  return arr.includes(opt) ? arr.filter(v => v !== opt).join(', ') : [...arr, opt].join(', ');
}

export function parseSelectedIds(value: string): string[] {
  return (value || '').split(',').filter(Boolean);
}

export function computeSelectedUserNames(value: string, users: JiraUser[]): string[] {
  const ids = parseSelectedIds(value);
  return users.filter(u => ids.includes(u.accountId)).map(u => u.displayName);
}

export function filterUsers(users: JiraUser[], query: string): JiraUser[] {
  const q = (query || '').toLowerCase();
  if (!q) return users;
  return users.filter(u => u.displayName.toLowerCase().includes(q));
}

export function previewText(parsed?: string, raw?: string): string {
  const p = typeof parsed === 'string' ? parsed.trim() : '';
  const r = typeof raw === 'string' ? raw : '';
  const text = p.length ? p : r;
  return text || 'Preview is empty';
}
