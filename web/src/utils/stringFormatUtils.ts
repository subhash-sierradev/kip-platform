function validateStringInput(input: unknown, defaultValue = ''): string {
  return typeof input === 'string' && input.trim() ? input.trim() : defaultValue;
}

export function splitCamelCase(str: string): string {
  const validated = validateStringInput(str);
  if (!validated) {
    return '';
  }

  return validated
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/([A-Z])([A-Z][a-z])/g, '$1 $2')
    .replace(/([a-zA-Z])([0-9])/g, '$1 $2')
    .replace(/([0-9])([a-zA-Z])/g, '$1 $2')
    .trim()
    .split(/\s+/)
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
}

export function getInitials(name: string): string {
  const validated = validateStringInput(name, '?');
  if (validated === '?') {
    return '?';
  }

  const words = validated.split(/\s+/).filter(word => word.length > 0);
  return words
    .slice(0, 2)
    .map(word => word.charAt(0).toUpperCase())
    .join('');
}

export function capitalizeName(name: string): string {
  const validated = validateStringInput(name);
  if (!validated) {
    return '';
  }

  return validated
    .split(/\s+/)
    .filter(word => word.length > 0)
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
}

export function capitalizeFirst(str: string): string {
  const validated = validateStringInput(str);
  if (!validated) {
    return '';
  }

  return validated.charAt(0).toUpperCase() + validated.slice(1).toLowerCase();
}

export function getFirstName(fullName: string): string {
  const validated = validateStringInput(fullName);
  if (!validated) {
    return '';
  }

  const words = validated.split(/\s+/).filter(word => word.length > 0);
  return words.length > 0 ? words[0] : '';
}

export function toPlainText(input: unknown, defaultValue = ''): string {
  const value = typeof input === 'string' ? input : '';
  if (!value) {
    return defaultValue;
  }

  // Fast path: no obvious HTML/entities
  if (!/[<&]/.test(value)) {
    return value.trim();
  }

  try {
    if (typeof DOMParser !== 'undefined') {
      const doc = new DOMParser().parseFromString(value, 'text/html');
      const text = doc.body?.textContent ?? '';
      const normalized = text.replace(/\s+/g, ' ').trim();
      return normalized || defaultValue;
    }
  } catch {
    // Fall through to regex-based stripping
  }

  const stripped = value
    .replace(/<[^>]*>/g, '')
    .replace(/\s+/g, ' ')
    .trim();
  return stripped || defaultValue;
}
