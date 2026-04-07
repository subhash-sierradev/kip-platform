import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

const pushMock = vi.fn();
const backMock = vi.fn();
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock, back: backMock }),
}));

import UnauthorizedPage from '@/components/common/UnauthorizedPage.vue';

describe('UnauthorizedPage', () => {
  it('navigates to dashboard when clicking primary button', async () => {
    const wrapper = mount(UnauthorizedPage);
    const btn = wrapper.find('.btn-primary');
    await btn.trigger('click');
    expect(pushMock).toHaveBeenCalledWith('/');
  });

  it('calls router.back when history length > 1', async () => {
    // Simulate navigation history
    history.pushState({}, '', '/foo');
    const wrapper = mount(UnauthorizedPage);
    const btn = wrapper.find('.btn.btn-outline');
    await btn.trigger('click');
    expect(backMock).toHaveBeenCalled();
  });

  it('navigates to dashboard when history length <= 1', async () => {
    // Clear any existing history
    vi.clearAllMocks();

    // Mock history.length to be 1 or less
    Object.defineProperty(window, 'history', {
      writable: true,
      value: { length: 1 },
    });

    const wrapper = mount(UnauthorizedPage);
    const btn = wrapper.find('.btn.btn-outline');
    await btn.trigger('click');
    expect(pushMock).toHaveBeenCalledWith('/');
  });
});
