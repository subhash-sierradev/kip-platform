export type DropdownPlacement = 'up' | 'down';

export function decideVerticalPlacement(
  triggerEl: HTMLElement | null,
  dropdownEl: HTMLElement | null,
  maxHeight = 240
): DropdownPlacement {
  if (!triggerEl) {
    return 'down';
  }

  const rect = triggerEl.getBoundingClientRect();
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight;
  const estimatedHeight = Math.min(dropdownEl?.scrollHeight || maxHeight, maxHeight) + 12;
  const spaceBelow = viewportHeight - rect.bottom;
  const spaceAbove = rect.top;

  return spaceBelow < estimatedHeight && spaceAbove > spaceBelow ? 'up' : 'down';
}

export function decidePlacementInContainer(
  triggerEl: HTMLElement | null,
  dropdownEl: HTMLElement | null,
  containerEl: HTMLElement | null,
  maxHeight = 240
): DropdownPlacement {
  if (!triggerEl) {
    return 'down';
  }

  const rect = triggerEl.getBoundingClientRect();
  const containerRect = containerEl?.getBoundingClientRect();
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight;
  const estimatedHeight = Math.min(dropdownEl?.scrollHeight || maxHeight, maxHeight) + 12;

  const spaceBelow = containerRect
    ? containerRect.bottom - rect.bottom
    : viewportHeight - rect.bottom;
  const spaceAbove = containerRect ? rect.top - containerRect.top : rect.top;

  return spaceBelow < estimatedHeight && spaceAbove > spaceBelow ? 'up' : 'down';
}
