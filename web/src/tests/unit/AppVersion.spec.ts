import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import AppVersion from '@/components/common/AppVersion.vue';

describe('AppVersion', () => {
  it('shows version and copyright year', () => {
    // Inject build-time version fallback
    // @ts-ignore
    globalThis.__APP_VERSION__ = '1.2.3';
    const wrapper = mount(AppVersion);
    expect(wrapper.text()).toContain('KIP v1.2.3');
    expect(wrapper.text()).toContain(`${new Date().getFullYear()}`);
  });
});
