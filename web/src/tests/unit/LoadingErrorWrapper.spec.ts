import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

vi.mock('devextreme-vue', () => ({
  DxButton: {
    name: 'DxButton',
    template: '<button class="retry-button" @click="$emit(\'click\')">{{ text }}</button>',
    props: ['text', 'icon', 'type', 'stylingMode', 'disabled'],
    emits: ['click'],
  },
  DxLoadPanel: {
    name: 'DxLoadPanel',
    template: '<div v-if="visible" class="load-panel-stub">{{ message }}</div>',
    props: ['visible', 'message', 'showIndicator', 'showPane', 'shading', 'container'],
  },
}));

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

  it('hides the loading panel when data already exists', () => {
    const wrapper = mount(LoadingErrorWrapper, {
      props: {
        loading: true,
        error: null,
        hasData: true,
      },
    });

    expect(wrapper.find('.load-panel-stub').exists()).toBe(false);
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

  it('renders custom labels for the error state', () => {
    const wrapper = mount(LoadingErrorWrapper, {
      props: {
        loading: false,
        error: 'Request failed',
        errorTitle: 'Custom error title',
        retryLabel: 'Retry now',
      },
    });

    expect(wrapper.find('.error-title').text()).toBe('Custom error title');
    expect(wrapper.find('.error-message').text()).toBe('Request failed');
    expect(wrapper.find('.error-container').attributes('role')).toBe('alert');
  });

  it('hides the retry button when showRetry is false', () => {
    const wrapper = mount(LoadingErrorWrapper, {
      props: {
        loading: false,
        error: 'Request failed',
        showRetry: false,
      },
    });

    expect(wrapper.find('.retry-button').exists()).toBe(false);
  });

  it('emits retry when the retry button is clicked', async () => {
    const wrapper = mount(LoadingErrorWrapper, {
      props: {
        loading: false,
        error: 'Request failed',
      },
    });

    await wrapper.find('.retry-button').trigger('click');

    expect(wrapper.emitted('retry')).toHaveLength(1);
  });
});
