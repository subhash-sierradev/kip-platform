import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import { defineComponent, h } from 'vue';

import { ServiceType } from '@/api/models/enums';
import JiraConnectionPage from '@/components/admin/jiraconnections/JiraConnectionPage.vue';

vi.mock('@/components/common/ServiceConnectionPage.vue', () => {
  const ServiceConnectionPageStub = defineComponent({
    name: 'ServiceConnectionPage',
    props: {
      serviceType: {
        type: String,
        required: true,
      },
    },
    setup(props) {
      return () =>
        h('div', { class: 'service-connection-page-stub', 'data-service-type': props.serviceType });
    },
  });

  return { default: ServiceConnectionPageStub };
});

describe('JiraConnectionPage.vue', () => {
  it('renders ServiceConnectionPage with JIRA service type', () => {
    const wrapper = mount(JiraConnectionPage);

    const stub = wrapper.find('.service-connection-page-stub');
    expect(stub.exists()).toBe(true);
    expect(stub.attributes('data-service-type')).toBe(ServiceType.JIRA);
  });
});
