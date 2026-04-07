import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import HomePage from '@/components/home/HomePage.vue';

describe('HomePage', () => {
  it('renders the main title', () => {
    const wrapper = mount(HomePage);
    const title = wrapper.find('h1');
    expect(title.exists()).toBe(true);
    expect(title.text()).toBe('Welcome to Kaseware Integration Platform');
  });

  it('renders the description paragraph', () => {
    const wrapper = mount(HomePage);
    const p = wrapper.find('p');
    expect(p.exists()).toBe(true);
    expect(p.text()).toContain('Streamline your data integration workflows');
  });

  it('renders four feature blocks with expected headings', () => {
    const wrapper = mount(HomePage);
    const features = wrapper.findAll('.feature');
    expect(features.length).toBe(4);

    const headings = features.map(f => f.find('h3').text());
    expect(headings).toEqual([
      'Real-time Integration',
      'Monitoring & Analytics',
      'Enterprise Security',
      'Webhook Management',
    ]);
  });

  it('has the component name HomePage', () => {
    const wrapper = mount(HomePage);
    const name = (wrapper.vm as any).$?.type?.name ?? (wrapper.vm as any).$options?.name;
    expect(name).toBe('HomePage');
  });
});
