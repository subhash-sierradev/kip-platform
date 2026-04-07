import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import IncomingJsonPanel from '@/components/outbound/jirawebhooks/wizard/IncomingJsonPanel.vue';

describe('IncomingJsonPanel', () => {
  it('shows empty message when no sample JSON provided', () => {
    const wrapper = mount(IncomingJsonPanel, {
      props: { jsonSample: '' },
    });

    expect(wrapper.find('.ms-json-empty').exists()).toBe(true);
    expect(wrapper.text()).toContain('No sample JSON provided');
  });

  it('renders formatted JSON lines when JSON is valid', () => {
    const wrapper = mount(IncomingJsonPanel, {
      props: { jsonSample: '{"foo":{"bar":1}}' },
    });

    const keyText = wrapper
      .findAll('.ms-json-key')
      .map(node => node.text())
      .join(' ');
    expect(keyText).toContain('foo');
    expect(keyText).toContain('bar');
  });

  it('renders raw content when JSON is invalid', () => {
    const wrapper = mount(IncomingJsonPanel, {
      props: { jsonSample: '{invalid json' },
    });

    expect(wrapper.text()).toContain('{invalid json');
  });
});
