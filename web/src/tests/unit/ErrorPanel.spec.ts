import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import ErrorPanel from '@/components/common/ErrorPanel.vue';

describe('ErrorPanel', () => {
  it('shows message and has alert role', () => {
    const wrapper = mount(ErrorPanel, { props: { message: 'Oops' } });
    expect(wrapper.text()).toContain('Oops');
    expect(wrapper.attributes('role')).toBe('alert');
  });
});
