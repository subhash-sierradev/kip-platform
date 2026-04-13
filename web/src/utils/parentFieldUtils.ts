export function extractParentKey(value: unknown): string {
  if (value === null || value === undefined) {
    return '';
  }

  if (typeof value === 'object') {
    const keyValue = (value as { key?: unknown }).key;
    return typeof keyValue === 'string' ? keyValue.trim() : '';
  }

  const raw = String(value).trim();
  if (!raw) {
    return '';
  }

  if (raw.startsWith('{{') && raw.endsWith('}}')) {
    return raw;
  }

  try {
    const parsed = JSON.parse(raw) as { key?: unknown };
    if (parsed && typeof parsed.key === 'string' && parsed.key.trim()) {
      return parsed.key.trim();
    }
  } catch {
    // Fall through to raw value.
  }

  return raw;
}

export function parentLabelMatchesKey(label: string, key: string): boolean {
  const normalizedLabel = String(label || '')
    .trim()
    .toLowerCase();
  const normalizedKey = String(key || '')
    .trim()
    .toLowerCase();

  if (!normalizedLabel || !normalizedKey) {
    return false;
  }

  return normalizedLabel === normalizedKey || normalizedLabel.startsWith(`${normalizedKey} -`);
}
