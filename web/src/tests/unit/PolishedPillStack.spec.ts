import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import PolishedPillStack from '@/components/common/PolishedPillStack.vue';

describe('PolishedPillStack', () => {
  it('renders count and default aria-label', () => {
    const wrapper = mount(PolishedPillStack, { props: { count: 5 } });
    expect(wrapper.text()).toContain('5');
    expect(wrapper.find('.pill-stack').attributes('aria-label')).toContain('Count: 5');
  });

  it('uses custom aria-label when provided', () => {
    const wrapper = mount(PolishedPillStack, { props: { count: 2, ariaLabel: 'Items' } });
    expect(wrapper.find('.pill-stack').attributes('aria-label')).toBe('Items');
  });
});
