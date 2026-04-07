export function normalizeIntegrationNameForCompare(value: string): string {
  if (!value || value.trim() === '') {
    return '';
  }
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '');
}

// Duplicate name checker using names loaded on mount
export function checkDuplicateName(
  name: string,
  allNormalizedNames: string[],
  originalName?: string,
  editMode?: boolean
): boolean {
  const target = normalizeIntegrationNameForCompare(name);
  if (!target) return false;

  const original = normalizeIntegrationNameForCompare(originalName || '');

  return allNormalizedNames.some((current: string) => {
    // In edit mode, allow keeping the original name
    const normalizedCurrent = normalizeIntegrationNameForCompare(current);
    if (editMode && original && normalizedCurrent === original) {
      return false;
    }

    return normalizedCurrent === target;
  });
}
