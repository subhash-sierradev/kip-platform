import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

import TabNavigation from '@/components/common/TabNavigation.vue';

const tabs = [
  { id: 'events', label: 'Events' },
  { id: 'templates', label: 'Templates' },
  { id: 'rules', label: 'Rules' },
  { id: 'recipients', label: 'Recipients' },
];

describe('TabNavigation', () => {
  it('renders all tabs and active state', () => {
    const wrapper = mount(TabNavigation, {
      props: {
        tabs,
        activeTab: 'events',
      },
    });

    const buttons = wrapper.findAll('button.tab-button');
    expect(buttons).toHaveLength(4);
    expect(buttons[0].classes()).toContain('active');
    expect(buttons[1].attributes('tabindex')).toBe('-1');
  });

  it('emits tab-change on click for non-active tab and does not emit for active tab', async () => {
    const wrapper = mount(TabNavigation, {
      props: {
        tabs,
        activeTab: 'events',
      },
    });

    await wrapper.findAll('button.tab-button')[0].trigger('click');
    expect(wrapper.emitted('tab-change')).toBeFalsy();

    await wrapper.findAll('button.tab-button')[1].trigger('click');
    expect(wrapper.emitted('tab-change')).toEqual([['templates']]);
  });

  it('handles ArrowRight, ArrowLeft, Home, End key navigation', async () => {
    const wrapper = mount(TabNavigation, {
      props: {
        tabs,
        activeTab: 'events',
      },
      attachTo: document.body,
    });

    const first = wrapper.findAll('button.tab-button')[0];

    await first.trigger('keydown', { key: 'ArrowRight' });
    await first.trigger('keydown', { key: 'ArrowLeft' });
    await first.trigger('keydown', { key: 'Home' });
    await first.trigger('keydown', { key: 'End' });

    const emissions = (wrapper.emitted('tab-change') || []).map(args => args[0]);
    expect(emissions).toContain('templates');
    expect(emissions).toContain('recipients');

    wrapper.unmount();
  });

  it('ignores unsupported keyboard key', async () => {
    const wrapper = mount(TabNavigation, {
      props: {
        tabs,
        activeTab: 'events',
      },
    });

    const first = wrapper.findAll('button.tab-button')[0];
    await first.trigger('keydown', { key: 'Enter' });
    expect(wrapper.emitted('tab-change')).toBeFalsy();
  });

  it('emits mouse interaction events', async () => {
    const wrapper = mount(TabNavigation, {
      props: {
        tabs,
        activeTab: 'events',
      },
    });

    const first = wrapper.findAll('button.tab-button')[0];
    await first.trigger('mouseenter');
    await first.trigger('mousemove');
    await first.trigger('mouseleave');

    expect(wrapper.emitted('tab-mouseenter')?.[0]?.[0]).toBe('events');
    expect(wrapper.emitted('tab-mousemove')).toBeTruthy();
    expect(wrapper.emitted('tab-mouseleave')).toBeTruthy();
  });

  it('focuses clicked tab after emit', async () => {
    const getByIdSpy = vi.spyOn(document, 'getElementById');
    const focusSpy = vi.fn();

    getByIdSpy.mockImplementation((id: string) => {
      if (id === 'tab-templates') {
        return { focus: focusSpy } as unknown as HTMLElement;
      }
      return null;
    });

    const wrapper = mount(TabNavigation, {
      props: {
        tabs,
        activeTab: 'events',
      },
    });

    await wrapper.findAll('button.tab-button')[1].trigger('click');
    await Promise.resolve();

    expect(getByIdSpy).toHaveBeenCalledWith('tab-templates');
    expect(focusSpy).toHaveBeenCalled();
    getByIdSpy.mockRestore();
  });
});
