import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import IntegrationsPage from '@/components/inbound/IntegrationsPage.vue';

describe('IntegrationsPage.vue', () => {
  it('renders the page title and description', () => {
    const wrapper = mount(IntegrationsPage);
    const title = wrapper.find('h1');
    expect(title.exists()).toBe(true);
    expect(title.text()).toBe('Integrations');

    const desc = wrapper.find('.container > p');
    expect(desc.exists()).toBe(true);
    expect(desc.text()).toBe('Configure and manage your data integration workflows.');
  });

  it('shows four feature cards with correct headings', () => {
    const wrapper = mount(IntegrationsPage);
    const features = wrapper.findAll('.feature');
    expect(features.length).toBe(4);

    const headings = features.map(f => f.find('h3').text());
    expect(headings).toEqual([
      'Data Connections',
      'Workflow Automation',
      'Real-time Monitoring',
      'Error Handling',
    ]);
  });

  it('exposes the component name', () => {
    const wrapper = mount(IntegrationsPage);
    // Vue exposes name via options for SFCs
    expect((wrapper.vm as any).$options.name).toBe('IntegrationsPage');
  });
});
