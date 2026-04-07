import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import LoadingSpinner from '@/components/common/LoadingSpinner.vue';

describe('LoadingSpinner', () => {
  it('renders spinner with aria-label and three dots', () => {
    const wrapper = mount(LoadingSpinner);
    const spinner = wrapper.find('div.spinner[aria-label="loading"]');
    expect(spinner.exists()).toBe(true);
    const dots = spinner.findAll('.dot');
    expect(dots.length).toBe(3);
  });
});
