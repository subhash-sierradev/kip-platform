import { afterEach, describe, expect, it } from 'vitest';

import {
  decidePlacementInContainer,
  decideVerticalPlacement,
} from '@/components/outbound/jirawebhooks/utils/dropdownPlacementUtils';

describe('dropdownPlacementUtils', () => {
  const originalInnerHeightDescriptor = Object.getOwnPropertyDescriptor(window, 'innerHeight');

  afterEach(() => {
    if (originalInnerHeightDescriptor) {
      Object.defineProperty(window, 'innerHeight', originalInnerHeightDescriptor);
      return;
    }

    Reflect.deleteProperty(window, 'innerHeight');
  });

  it('decideVerticalPlacement returns down when trigger is missing', () => {
    expect(decideVerticalPlacement(null, null)).toBe('down');
  });

  it('decideVerticalPlacement returns up when there is not enough space below', () => {
    const trigger = document.createElement('div');
    const dropdown = document.createElement('div');

    Object.defineProperty(dropdown, 'scrollHeight', { value: 220, configurable: true });
    trigger.getBoundingClientRect = () =>
      ({
        top: 500,
        bottom: 560,
      }) as DOMRect;

    Object.defineProperty(window, 'innerHeight', { value: 600, configurable: true });

    expect(decideVerticalPlacement(trigger, dropdown)).toBe('up');
  });

  it('decidePlacementInContainer uses container bounds when provided', () => {
    const trigger = document.createElement('div');
    const dropdown = document.createElement('div');
    const container = document.createElement('div');

    Object.defineProperty(dropdown, 'scrollHeight', { value: 220, configurable: true });
    trigger.getBoundingClientRect = () =>
      ({
        top: 260,
        bottom: 300,
      }) as DOMRect;
    container.getBoundingClientRect = () =>
      ({
        top: 100,
        bottom: 330,
      }) as DOMRect;

    Object.defineProperty(window, 'innerHeight', { value: 1200, configurable: true });

    expect(decidePlacementInContainer(trigger, dropdown, container)).toBe('up');
  });

  it('decidePlacementInContainer falls back to viewport when container missing', () => {
    const trigger = document.createElement('div');
    const dropdown = document.createElement('div');

    Object.defineProperty(dropdown, 'scrollHeight', { value: 120, configurable: true });
    trigger.getBoundingClientRect = () =>
      ({
        top: 100,
        bottom: 150,
      }) as DOMRect;

    Object.defineProperty(window, 'innerHeight', { value: 900, configurable: true });

    expect(decidePlacementInContainer(trigger, dropdown, null)).toBe('down');
  });
});
