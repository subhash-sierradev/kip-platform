import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import StandardDetailPageLayout from '@/components/common/StandardDetailPageLayout.vue';

const HeaderStub = {
  name: 'DetailsPageHeader',
  props: [
    'title',
    'loading',
    'error',
    'icon',
    'version',
    'status',
    'showBackButton',
    'backButtonLabel',
    'tabs',
    'activeTab',
  ],
  emits: ['back', 'tab-change'],
  template:
    '<div data-test="header">' +
    '<button data-test="back" @click="$emit(\'back\')">back</button>' +
    '<button data-test="tab" @click="$emit(\'tab-change\', \'next\')">tab</button>' +
    '<slot name="custom-icon" />' +
    '</div>',
};

const WrapperStub = {
  name: 'LoadingErrorWrapper',
  props: [
    'loading',
    'error',
    'hasData',
    'loadingMessage',
    'errorTitle',
    'retryLabel',
    'container',
    'showRetry',
  ],
  emits: ['retry'],
  template:
    '<div data-test="wrapper">' +
    '<button data-test="retry" @click="$emit(\'retry\')">retry</button>' +
    '<slot />' +
    '</div>',
};

const ActiveTabComponent = {
  name: 'ActiveTabComponent',
  props: ['foo'],
  emits: ['status-updated', 'refresh', 'external-event'],
  template:
    '<div data-test="active-tab">' +
    '<span>{{ foo }}</span>' +
    '<button data-test="status" @click="$emit(\'status-updated\', true)">status</button>' +
    '<button data-test="refresh" @click="$emit(\'refresh\')">refresh</button>' +
    '<button data-test="external" @click="$emit(\'external-event\', \'payload\')">external</button>' +
    '</div>',
};

describe('StandardDetailPageLayout', () => {
  function mountLayout(props: Record<string, unknown> = {}, options: Record<string, unknown> = {}) {
    return mount(StandardDetailPageLayout, {
      props: {
        title: 'Detail Title',
        loading: false,
        error: null,
        ...props,
      },
      global: {
        stubs: {
          DetailsPageHeader: HeaderStub,
          LoadingErrorWrapper: WrapperStub,
        },
      },
      ...options,
    });
  }

  it('renders slot content when tabs are missing', () => {
    const wrapper = mountLayout({}, {
      slots: {
        default: '<div data-test="fallback-slot">slot content</div>',
      },
    } as any);

    expect(wrapper.find('[data-test="wrapper"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="fallback-slot"]').exists()).toBe(true);
    expect(wrapper.find('[data-test="active-tab"]').exists()).toBe(false);
  });

  it('renders active tab component when tabs and activeTab are provided', () => {
    const wrapper = mount(StandardDetailPageLayout, {
      props: {
        title: 'Detail Title',
        loading: false,
        error: null,
        tabs: [{ id: 'details', label: 'Details', component: ActiveTabComponent }],
        activeTab: 'details',
        componentProps: { foo: 'bar' },
        componentEvents: { 'external-event': () => undefined },
      },
      global: {
        stubs: {
          DetailsPageHeader: HeaderStub,
          LoadingErrorWrapper: WrapperStub,
        },
      },
    });

    expect(wrapper.find('[data-test="active-tab"]').exists()).toBe(true);
    expect(wrapper.text()).toContain('bar');
  });

  it('handles missing active tab definition by rendering no dynamic component', async () => {
    const wrapper = mount(StandardDetailPageLayout, {
      props: {
        title: 'Detail Title',
        loading: false,
        error: null,
        tabs: [{ id: 'details', label: 'Details', component: ActiveTabComponent }],
        activeTab: 'missing-tab',
      },
      global: {
        stubs: {
          DetailsPageHeader: HeaderStub,
          LoadingErrorWrapper: WrapperStub,
        },
      },
    });

    expect(wrapper.find('[data-test="active-tab"]').exists()).toBe(false);

    await wrapper.setProps({ tabs: undefined });
    expect(wrapper.find('[data-test="active-tab"]').exists()).toBe(false);
  });

  it('forwards header and wrapper events', async () => {
    const wrapper = mount(StandardDetailPageLayout, {
      props: {
        title: 'Detail Title',
        loading: false,
        error: null,
        tabs: [{ id: 'details', label: 'Details', component: ActiveTabComponent }],
        activeTab: 'details',
      },
      global: {
        stubs: {
          DetailsPageHeader: HeaderStub,
          LoadingErrorWrapper: WrapperStub,
        },
      },
    });

    await wrapper.find('[data-test="back"]').trigger('click');
    await wrapper.find('[data-test="tab"]').trigger('click');
    await wrapper.find('[data-test="retry"]').trigger('click');
    await wrapper.find('[data-test="status"]').trigger('click');
    await wrapper.find('[data-test="refresh"]').trigger('click');

    expect(wrapper.emitted('back')).toBeTruthy();
    expect(wrapper.emitted('tab-change')?.[0]).toEqual(['next']);
    expect(wrapper.emitted('retry')).toBeTruthy();
    expect(wrapper.emitted('status-updated')?.[0]).toEqual([true]);
    expect(wrapper.emitted('refresh')).toBeTruthy();
  });

  it('passes defaulted props to loading wrapper', () => {
    const wrapper = mountLayout();
    const wrapperVm = wrapper.findComponent({ name: 'LoadingErrorWrapper' });

    expect(wrapperVm.props('hasData')).toBe(false);
    expect(wrapperVm.props('loadingMessage')).toBe('Loading...');
    expect(wrapperVm.props('errorTitle')).toBe('Unable to load data');
    expect(wrapperVm.props('retryLabel')).toBe('Try Again');
    expect(wrapperVm.props('showRetry')).toBe(true);
    expect(wrapperVm.props('container')).toBe('.detail-page');
  });
});
