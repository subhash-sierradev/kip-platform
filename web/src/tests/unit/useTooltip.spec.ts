/* eslint-disable simple-import-sort/imports */
import { describe, it, expect } from 'vitest';

import { useTooltip } from '@/composables/useTooltip';

describe('useTooltip', () => {
  it('initializes with hidden tooltip and default coordinates', () => {
    const { tooltip } = useTooltip();
    expect(tooltip.value.visible).toBe(false);
    expect(tooltip.value.text).toBe('');
    expect(tooltip.value.x).toBe(0);
    expect(tooltip.value.y).toBe(0);
  });

  it('shows tooltip with text and offset coordinates', () => {
    const { tooltip, showTooltip } = useTooltip();
    const ev = new MouseEvent('mousemove', { clientX: 10, clientY: 20 });

    showTooltip(ev, 'hello');

    expect(tooltip.value.visible).toBe(true);
    expect(tooltip.value.text).toBe('hello');
    expect(tooltip.value.x).toBe(22); // 10 + 12
    expect(tooltip.value.y).toBe(32); // 20 + 12
  });

  it('moves tooltip updating coordinates with same offset', () => {
    const { tooltip, showTooltip, moveTooltip } = useTooltip();
    showTooltip(new MouseEvent('mousemove', { clientX: 0, clientY: 0 }), 't');

    moveTooltip(new MouseEvent('mousemove', { clientX: 100, clientY: 200 }));

    expect(tooltip.value.x).toBe(112);
    expect(tooltip.value.y).toBe(212);
  });

  it('hides tooltip when hideTooltip is called', () => {
    const { tooltip, showTooltip, hideTooltip } = useTooltip();
    showTooltip(new MouseEvent('mousemove', { clientX: 1, clientY: 1 }), 't');
    expect(tooltip.value.visible).toBe(true);

    hideTooltip();
    expect(tooltip.value.visible).toBe(false);
  });
});
