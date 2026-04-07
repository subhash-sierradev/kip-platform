import { describe, expect, it } from 'vitest';

import { useActionTooltip } from '@/components/outbound/jirawebhooks/wizard/composables/useActionTooltip';

describe('useActionTooltip', () => {
  it('initializes tooltip state and id', () => {
    const state = useActionTooltip('row-1');

    expect(state.actionTipVisible.value).toBe(false);
    expect(state.actionTipText.value).toBe('');
    expect(state.actionTipId).toBe('row-tooltip-row-1');
  });

  it('onActionEnter shows tooltip and positions it with offsets', () => {
    const state = useActionTooltip('row-2');

    state.onActionEnter({
      text: 'Insert fields',
      event: { clientX: 20, clientY: 30 } as MouseEvent,
    });

    expect(state.actionTipVisible.value).toBe(true);
    expect(state.actionTipText.value).toBe('Insert fields');
    expect(state.actionTipX.value).toBe(32);
    expect(state.actionTipY.value).toBe(42);
  });

  it('onActionMove updates tooltip coordinates and onActionLeave hides it', () => {
    const state = useActionTooltip('row-3');

    state.onActionMove({ clientX: 50, clientY: 60 } as MouseEvent);
    expect(state.actionTipX.value).toBe(62);
    expect(state.actionTipY.value).toBe(72);

    state.onActionLeave();
    expect(state.actionTipVisible.value).toBe(false);
  });
});
