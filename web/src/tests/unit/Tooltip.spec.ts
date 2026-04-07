import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

import Tooltip from '@/components/common/Tooltip.vue';

describe('CommonTooltip', () => {
  it('renders menu-style tooltip with title/description', () => {
    const wrapper = mount(Tooltip, {
      props: {
        visible: true,
        x: 10,
        y: 20,
        id: 'tip',
        menuStyle: true,
        title: 'T',
        description: 'D',
      },
    });
    const tip = wrapper.find('[role="tooltip"]');
    expect(tip.exists()).toBe(true);
    expect(wrapper.text()).toContain('T');
    expect(wrapper.text()).toContain('D');
  });

  it('renders plain tooltip with slot or text', () => {
    const wrapper = mount(Tooltip, {
      props: { visible: true, x: 0, y: 0 },
      slots: { default: 'Hello' },
    });
    expect(wrapper.find('[role="tooltip"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('Hello');
  });

  it('warns when menuStyle without title/description', () => {
    const spy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    mount(Tooltip, { props: { visible: true, x: 0, y: 0, menuStyle: true } });
    expect(spy).toHaveBeenCalled();
    spy.mockRestore();
  });

  it('handles invisible state', () => {
    const wrapper = mount(Tooltip, {
      props: { visible: false, x: 10, y: 20, id: 'hidden-tip' },
    });
    expect(wrapper.find('[role="tooltip"]').exists()).toBe(false);
  });

  it('handles different positioning', () => {
    const wrapper = mount(Tooltip, {
      props: { visible: true, x: 100, y: 200, id: 'positioned-tip' },
    });
    const tooltip = wrapper.find('[role="tooltip"]');
    expect(tooltip.exists()).toBe(true);
  });

  it('handles tooltip without menuStyle', () => {
    const wrapper = mount(Tooltip, {
      props: { visible: true, x: 0, y: 0, menuStyle: false },
      slots: { default: 'Simple tooltip' },
    });
    expect(wrapper.text()).toContain('Simple tooltip');
  });

  it('handles missing props gracefully', () => {
    const wrapper = mount(Tooltip, {
      props: { visible: true, x: 100, y: 200 },
    });
    expect(wrapper.exists()).toBe(true);
  });

  it('handles only title in menuStyle', () => {
    const wrapper = mount(Tooltip, {
      props: { visible: true, x: 0, y: 0, menuStyle: true, title: 'Only Title' },
    });
    expect(wrapper.text()).toContain('Only Title');
  });

  it('handles only description in menuStyle', () => {
    const wrapper = mount(Tooltip, {
      props: { visible: true, x: 0, y: 0, menuStyle: true, description: 'Only Description' },
    });
    expect(wrapper.text()).toContain('Only Description');
  });
});
