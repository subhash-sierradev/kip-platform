import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import AppVersion from '@/components/common/AppVersion.vue';

vi.mock('@/config/keycloak', () => ({
  getToken: () => 'mock-jwt-token',
}));

describe('AppVersion', () => {
  beforeEach(() => {
    // @ts-ignore
    globalThis.__APP_VERSION__ = '1.2.3';
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: () =>
          Promise.resolve({
            ims: { version: '0.0.1', name: 'integration-management-service' },
            ies: { version: '0.0.1', name: 'integration-execution-service' },
          }),
      })
    );
  });

  it('shows all versions and copyright year', async () => {
    const wrapper = mount(AppVersion);
    await flushPromises();
    expect(wrapper.text()).toContain('Web');
    expect(wrapper.text()).toContain('1.2.3');
    expect(wrapper.text()).toContain('IMS');
    expect(wrapper.text()).toContain('0.0.1');
    expect(wrapper.text()).toContain('IES');
    expect(wrapper.text()).toContain(`${new Date().getFullYear()}`);
  });

  it('shows loading state before fetch resolves', () => {
    const wrapper = mount(AppVersion);
    expect(wrapper.text()).toContain('IMS');
    expect(wrapper.text()).toContain('...');
    expect(wrapper.text()).toContain('IES');
  });

  it('shows unavailable when fetch fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('Network error')));
    const wrapper = mount(AppVersion);
    await flushPromises();
    expect(wrapper.text()).toContain('unavailable');
  });

  it('sends Authorization header with token', async () => {
    const fetchSpy = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ims: { version: '1.0.0' }, ies: { version: '1.0.0' } }),
    });
    vi.stubGlobal('fetch', fetchSpy);
    mount(AppVersion);
    await flushPromises();
    expect(fetchSpy).toHaveBeenCalledWith(
      expect.stringContaining('/management/version'),
      expect.objectContaining({ headers: { Authorization: 'Bearer mock-jwt-token' } })
    );
  });
});
