export function syncTextInputValue(event: Event, maxLength = 100): string {
  const target = event.target;

  if (!(target instanceof HTMLInputElement)) {
    return '';
  }

  const value = target.value.slice(0, maxLength);
  if (target.value !== value) {
    target.value = value;
  }

  return value;
}
