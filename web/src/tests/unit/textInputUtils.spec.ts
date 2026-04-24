import { describe, expect, it } from 'vitest';

import { syncTextInputValue } from '@/utils/textInputUtils';

function createInputEvent(element: HTMLInputElement | HTMLTextAreaElement): Event {
  let capturedEvent: Event | undefined;
  element.addEventListener('input', event => {
    capturedEvent = event;
  });
  element.dispatchEvent(new Event('input', { bubbles: true }));

  if (!capturedEvent) {
    throw new Error('Failed to create input event');
  }

  return capturedEvent;
}

describe('textInputUtils', () => {
  describe('syncTextInputValue', () => {
    it('returns the original value when it is within the default max length', () => {
      const input = document.createElement('input');
      input.value = 'Valid integration name';
      const event = createInputEvent(input);

      const value = syncTextInputValue(event);

      expect(value).toBe('Valid integration name');
      expect(input.value).toBe('Valid integration name');
    });

    it('truncates the input value to the default max length and syncs the DOM value', () => {
      const input = document.createElement('input');
      input.value = 'a'.repeat(125);
      const event = createInputEvent(input);

      const value = syncTextInputValue(event);

      expect(value).toHaveLength(100);
      expect(input.value).toHaveLength(100);
      expect(value).toBe(input.value);
    });

    it('honors a custom max length', () => {
      const input = document.createElement('input');
      input.value = 'ArcGISWebhook';
      const event = createInputEvent(input);

      const value = syncTextInputValue(event, 6);

      expect(value).toBe('ArcGIS');
      expect(input.value).toBe('ArcGIS');
    });

    it('returns an empty string for non-input targets', () => {
      const textarea = document.createElement('textarea');
      const event = createInputEvent(textarea);

      const value = syncTextInputValue(event);

      expect(value).toBe('');
    });
  });
});
