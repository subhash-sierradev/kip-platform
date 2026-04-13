import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import { defineComponent, h } from 'vue';

import { ServiceType } from '@/api/models/enums';
import ArcGISConnectionPage from '@/components/admin/arcgisconnections/ArcGISConnectionPage.vue';
import AdminConfluenceConnectionPage from '@/components/admin/confluenceconnections/ConfluenceConnectionPage.vue';
import OutboundConfluenceConnectionPage from '@/components/outbound/confluenceintegration/admin/confluenceconnections/ConfluenceConnectionPage.vue';

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

describe('Connection page wrappers', () => {
  it('renders ServiceConnectionPage with the ArcGIS service type', () => {
    const wrapper = mount(ArcGISConnectionPage);

    expect(wrapper.find('.service-connection-page-stub').attributes('data-service-type')).toBe(
      ServiceType.ARCGIS
    );
  });

  it('renders ServiceConnectionPage with the admin Confluence service type', () => {
    const wrapper = mount(AdminConfluenceConnectionPage);

    expect(wrapper.find('.service-connection-page-stub').attributes('data-service-type')).toBe(
      ServiceType.CONFLUENCE
    );
  });

  it('renders ServiceConnectionPage with the outbound Confluence service type', () => {
    const wrapper = mount(OutboundConfluenceConnectionPage);

    expect(wrapper.find('.service-connection-page-stub').attributes('data-service-type')).toBe(
      ServiceType.CONFLUENCE
    );
  });
});
