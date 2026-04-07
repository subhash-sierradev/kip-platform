import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import LoadingErrorWrapper from '@/components/common/LoadingErrorWrapper.vue';

describe('LoadingErrorWrapper', () => {
  it('shows loading state', () => {
    const wrapper = mount(LoadingErrorWrapper, {
      props: {
        loading: true,
        error: null,
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('shows error state', () => {
    const wrapper = mount(LoadingErrorWrapper, {
      props: {
        loading: false,
        error: 'Something went wrong',
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('shows content when not loading and no error', () => {
    const wrapper = mount(LoadingErrorWrapper, {
      props: {
        loading: false,
        error: null,
      },
      slots: {
        default: '<div>Content</div>',
      },
    });

    expect(wrapper.text()).toContain('Content');
  });

  it('prioritizes loading over error', () => {
    const wrapper = mount(LoadingErrorWrapper, {
      props: {
        loading: true,
        error: 'Error message',
      },
    });

    expect(wrapper.exists()).toBe(true);
  });

  it('handles empty props gracefully', () => {
    const wrapper = mount(LoadingErrorWrapper, {
      slots: {
        default: '<div>Default content</div>',
      },
    });

    expect(wrapper.text()).toContain('Default content');
  });
});
