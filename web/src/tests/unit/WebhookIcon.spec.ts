import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import WebhookIcon from '@/components/common/WebhookIcon.vue';

describe('WebhookIcon', () => {
  it('renders with default size (medium)', () => {
    const wrapper = mount(WebhookIcon);
    const svg = wrapper.find('svg');
    expect(svg.attributes('width')).toBe('24px');
    expect(svg.attributes('height')).toBe('24px');
  });

  it('renders small and large sizes', () => {
    const small = mount(WebhookIcon, { props: { size: 'small' } });
    expect(small.find('svg').attributes('width')).toBe('20px');
    expect(small.find('svg').attributes('height')).toBe('20px');

    const large = mount(WebhookIcon, { props: { size: 'large' } });
    expect(large.find('svg').attributes('width')).toBe('28px');
    expect(large.find('svg').attributes('height')).toBe('28px');
  });
});
