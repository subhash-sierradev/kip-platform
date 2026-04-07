interface ResolverOptions {
  preserveUnresolved?: boolean;
}

type PathToken =
  | { type: 'prop'; value: string }
  | { type: 'index'; value: number }
  | { type: 'wildcard' };

const PURE_PLACEHOLDER_REGEX = /^\{\{\s*([^{}]+?)\s*}}$/;
const PLACEHOLDER_REGEX = /\{\{\s*([^{}]+?)\s*}}/g;
const TOKEN_REGEX = /([^.[\]]+)|\[(.*?)\]/g;

export function parseJsonSafe(jsonText: string): Record<string, unknown> {
  try {
    const parsed = JSON.parse(jsonText || '{}');
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

export function resolveTemplateWithJson(
  template: string,
  jsonText: string,
  options: ResolverOptions = {}
): string {
  const json = parseJsonSafe(jsonText);
  return resolveTemplateWithObject(template, json, options);
}

export function resolveTemplateWithObject(
  template: string,
  json: Record<string, unknown>,
  options: ResolverOptions = {}
): string {
  if (!template) return '';

  return template.replace(PLACEHOLDER_REGEX, (match: string, rawPath: string) => {
    const value = resolvePathValue(json, String(rawPath || '').trim());
    if (value === undefined) {
      return options.preserveUnresolved ? match : '';
    }
    return valueToDisplayString(value);
  });
}

export function resolvePurePlaceholderValue(
  template: string,
  json: Record<string, unknown>
): unknown {
  const match = template.trim().match(PURE_PLACEHOLDER_REGEX);
  if (!match) return undefined;
  return resolvePathValue(json, match[1].trim());
}

export function resolvePathValue(root: unknown, path: string): unknown {
  const tokens = tokenizePath(path);
  if (tokens.length === 0) return undefined;

  let currentValues: unknown[] = [root];

  for (const token of tokens) {
    const nextValues: unknown[] = [];

    for (const current of currentValues) {
      collectNextValues(current, token, nextValues);
    }

    if (nextValues.length === 0) {
      const wildcardFromArray = token.type === 'wildcard' && currentValues.some(Array.isArray);
      if (wildcardFromArray) {
        return [];
      }
      return undefined;
    }

    currentValues = nextValues;
  }

  if (currentValues.length === 1) {
    return currentValues[0];
  }

  return currentValues;
}

function tokenizePath(path: string): PathToken[] {
  const tokens: PathToken[] = [];
  const source = String(path || '').trim();
  if (!source) return tokens;

  let match: RegExpExecArray | null;
  while ((match = TOKEN_REGEX.exec(source)) !== null) {
    if (match[1]) {
      tokens.push({ type: 'prop', value: match[1].trim() });
      continue;
    }

    const bracketValue = String(match[2] || '').trim();
    if (!bracketValue) {
      tokens.push({ type: 'wildcard' });
      continue;
    }

    if (/^\d+$/.test(bracketValue)) {
      tokens.push({ type: 'index', value: Number(bracketValue) });
      continue;
    }

    if (
      (bracketValue.startsWith("'") && bracketValue.endsWith("'")) ||
      (bracketValue.startsWith('"') && bracketValue.endsWith('"'))
    ) {
      tokens.push({ type: 'prop', value: bracketValue.slice(1, -1) });
      continue;
    }

    tokens.push({ type: 'prop', value: bracketValue });
  }

  return tokens;
}

function collectNextValues(current: unknown, token: PathToken, target: unknown[]) {
  if (current === null || current === undefined) return;

  if (token.type === 'prop') {
    collectPropertyValues(current, token.value, target);
    return;
  }

  if (token.type === 'wildcard') {
    if (Array.isArray(current)) {
      target.push(...current);
    }
    return;
  }

  if (token.type === 'index') {
    if (Array.isArray(current)) {
      const next = current[token.value];
      if (next !== undefined) target.push(next);
    }
  }
}

function collectPropertyValues(current: unknown, key: string, target: unknown[]) {
  if (!key) return;

  if (Array.isArray(current)) {
    if (/^\d+$/.test(key)) {
      const next = current[Number(key)];
      if (next !== undefined) {
        target.push(next);
      }
    }
    return;
  }

  if (typeof current === 'object' && Object.prototype.hasOwnProperty.call(current, key)) {
    target.push((current as Record<string, unknown>)[key]);
  }
}

function valueToDisplayString(value: unknown): string {
  if (value === null || value === undefined) return '';

  if (Array.isArray(value)) {
    const parts = value.map(arrayItemToDisplayString).filter(item => item.length > 0);
    return parts.join(', ');
  }

  if (typeof value === 'object') {
    return objectToDisplayString(value as Record<string, unknown>);
  }

  return String(value);
}

function arrayItemToDisplayString(value: unknown): string {
  if (value === null || value === undefined) return '';
  if (Array.isArray(value)) {
    return value
      .map(arrayItemToDisplayString)
      .filter(item => item.length > 0)
      .join(', ');
  }
  if (typeof value === 'object') {
    return objectToDisplayString(value as Record<string, unknown>);
  }
  return String(value).trim();
}

function objectToDisplayString(value: Record<string, unknown>): string {
  const preferred = pickPreferredField(value);
  if (preferred !== null && preferred !== undefined) return String(preferred);
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

function pickPreferredField(value: Record<string, unknown>): unknown {
  const candidates = ['name', 'displayName', 'key'];
  for (const candidate of candidates) {
    if (Object.prototype.hasOwnProperty.call(value, candidate)) {
      return value[candidate];
    }
  }
  return undefined;
}
