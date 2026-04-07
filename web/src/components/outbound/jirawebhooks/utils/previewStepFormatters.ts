import type { CustomFieldMapping } from '@/api/services/JiraWebhookService';
import { formatDisplayDate, formatMetadataDate } from '@/utils/dateUtils';

type JsonMap = Record<string, unknown>;

function isObjectRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function getValueFromPath(path: string, json: JsonMap): unknown {
  try {
    let current: unknown = json;

    for (const key of path.split('.')) {
      if (current === null || current === undefined) {
        return undefined;
      }

      const numericIndex = Number(key);
      const isNumericKey = Number.isInteger(numericIndex) && String(numericIndex) === key;
      if (isNumericKey) {
        if (!Array.isArray(current)) {
          return undefined;
        }
        current = current[numericIndex];
        continue;
      }

      if (!isObjectRecord(current)) {
        return undefined;
      }

      current = current[key];
    }

    return current;
  } catch {
    return undefined;
  }
}

export function resolveTemplate(template: string, jsonSample: string): string {
  if (!template) {
    return '';
  }

  try {
    const json: JsonMap = JSON.parse(jsonSample || '{}');
    return template.replace(/\{\{(.*?)\}\}/g, (_, rawPath: string) => {
      const path = rawPath.trim();
      const value = getValueFromPath(path, json);

      if (value === undefined) {
        return `{{${path}}}`;
      }
      if (value === null) {
        return '';
      }
      if (typeof value === 'object') {
        try {
          return JSON.stringify(value, null, 2);
        } catch {
          return String(value);
        }
      }

      return String(value);
    });
  } catch {
    return template;
  }
}

export function formatJsonSample(jsonSample: string): string {
  try {
    return JSON.stringify(JSON.parse(jsonSample || '{}'), null, 2);
  } catch {
    return jsonSample;
  }
}

function parseJsonSample(jsonSample: string): JsonMap {
  try {
    return JSON.parse(jsonSample || '{}');
  } catch {
    return {};
  }
}

function resolveJsonTemplate(template: string, json: JsonMap): string {
  if (!template) {
    return '';
  }

  return template.replace(/\{\{\s*([\w.]+)\s*\}\}/g, (match: string, path: string) => {
    const value = getValueFromPath(path, json);
    if (value === undefined) {
      return match;
    }
    if (value === null) {
      return '';
    }
    if (typeof value === 'object') {
      try {
        return JSON.stringify(value);
      } catch {
        return String(value);
      }
    }
    return String(value);
  });
}

function resolveJsonSmart(template: string, json: JsonMap): unknown {
  if (!template) {
    return '';
  }

  const trimmed = template.trim();
  const pureMatch = trimmed.match(/^\{\{\s*([\w.]+)\s*\}\}$/);
  if (pureMatch) {
    return getValueFromPath(pureMatch[1], json);
  }

  return resolveJsonTemplate(template, json);
}

function formatArrayValue(value: unknown): string[] {
  const source = Array.isArray(value) ? value.join(',') : String(value ?? '');
  return source
    .split(',')
    .map(token => token.trim())
    .filter(token => token.length > 0);
}

function formatNumberValue(value: unknown): string {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) ? String(numberValue) : String(value ?? '');
}

function formatBooleanValue(value: unknown): string {
  if (typeof value === 'boolean') {
    return value ? 'True' : 'False';
  }

  const normalized = String(value ?? '').toLowerCase();
  if (normalized === 'true') {
    return 'True';
  }
  if (normalized === 'false') {
    return 'False';
  }

  return String(value ?? '');
}

function formatDateTimeValue(value: unknown): string {
  if (!value) {
    return '';
  }

  try {
    const raw = String(value);
    const date = value instanceof Date ? value : new Date(raw);
    if (!Number.isFinite(date.getTime())) {
      return raw;
    }

    const hasTime = raw.includes('T') || /\d{2}:\d{2}/.test(raw);
    return hasTime ? formatMetadataDate(date) : formatDisplayDate(date);
  } catch {
    return String(value);
  }
}

function formatDefaultValue(value: unknown): string {
  if (typeof value === 'object') {
    try {
      return JSON.stringify(value);
    } catch {
      return String(value ?? '');
    }
  }

  return String(value ?? '');
}

function formatParentValue(value: unknown): string {
  if (value === null || value === undefined) {
    return '';
  }

  if (typeof value === 'object') {
    const keyValue = (value as { key?: unknown }).key;
    return typeof keyValue === 'string' ? keyValue : String(value);
  }

  const raw = String(value).trim();
  if (!raw) {
    return '';
  }

  try {
    const parsed = JSON.parse(raw) as { key?: unknown };
    if (parsed && typeof parsed.key === 'string' && parsed.key.trim()) {
      return parsed.key.trim();
    }
  } catch {
    // ignore parse error and return raw value below
  }

  return raw;
}

function formatUser(value: unknown, userMap: Map<string, string>): string {
  if (!value) {
    return '';
  }

  const userId = String(value);
  return userMap.get(userId) ?? userId;
}

function formatMultiUser(value: unknown, userMap: Map<string, string>): string {
  if (!value) {
    return '';
  }

  const userIds = Array.isArray(value)
    ? value.map(item => String(item))
    : String(value)
        .split(',')
        .map(item => item.trim());

  return userIds.map(userId => userMap.get(userId) ?? userId).join(', ');
}

function formatSprint(value: unknown, sprintMap: Map<string, string>): string {
  if (!value) {
    return '';
  }

  const sprintIds = Array.isArray(value)
    ? value.map(item => String(item).trim()).filter(Boolean)
    : String(value)
        .split(',')
        .map(item => item.trim())
        .filter(Boolean);

  return sprintIds.map(sprintId => sprintMap.get(sprintId) ?? sprintId).join(', ');
}

export function formatCustomValue(
  customField: CustomFieldMapping,
  jsonSample: string,
  userMap: Map<string, string>,
  sprintMap: Map<string, string>
): string | string[] {
  const json = parseJsonSample(jsonSample);
  const baseValue =
    customField.valueSource === 'json'
      ? resolveJsonSmart(String(customField.value ?? ''), json)
      : customField.value;
  const value = baseValue === null || baseValue === undefined ? '' : baseValue;

  const fieldType = (customField.type || 'string').toLowerCase();
  const isSprintByLabel = (customField.jiraFieldLabel || '').trim().toLowerCase() === 'sprint';
  if ((fieldType === 'number' || fieldType === 'string') && isSprintByLabel) {
    return formatSprint(value, sprintMap);
  }

  const formatters: Record<string, (input: unknown) => string | string[]> = {
    array: formatArrayValue,
    number: formatNumberValue,
    boolean: formatBooleanValue,
    datetime: formatDateTimeValue,
    date: formatDateTimeValue,
    sprint: input => formatSprint(input, sprintMap),
    user: input => formatUser(input, userMap),
    multiuser: input => formatMultiUser(input, userMap),
    string: formatDefaultValue,
  };

  const formatter = formatters[fieldType] || formatDefaultValue;
  return formatter(value);
}

export function formatParentName(customFields: CustomFieldMapping[], jsonSample: string): string {
  const parentField = (customFields || []).find(
    customField =>
      String(customField.jiraFieldKey || '')
        .trim()
        .toLowerCase() === 'parent'
  );
  if (!parentField) {
    return 'Not Selected';
  }

  const json = parseJsonSample(jsonSample);
  const baseValue =
    parentField.valueSource === 'json'
      ? resolveJsonSmart(String(parentField.value ?? ''), json)
      : parentField.value;

  return formatParentValue(baseValue === null || baseValue === undefined ? '' : baseValue);
}

export function getFieldLabel(customField: CustomFieldMapping): string {
  const label = (customField.jiraFieldLabel || '').trim();
  return label || 'Custom Field';
}

export function toUserMap(
  users: Array<{ accountId: string; displayName: string }>
): Map<string, string> {
  const map = new Map<string, string>();
  users.forEach(user => {
    map.set(user.accountId, user.displayName);
  });
  return map;
}

export function toSprintMap(sprints: Array<{ value: string; label: string }>): Map<string, string> {
  const map = new Map<string, string>();
  sprints.forEach(sprint => {
    map.set(String(sprint.value), sprint.label);
  });
  return map;
}
