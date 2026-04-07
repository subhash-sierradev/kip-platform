import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

const initializeToast = vi.fn();
const clearAllToasts = vi.fn();
const hideToast = vi.fn();
vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    initializeToast,
    clearAllToasts,
    hideToast,
    toasts: [{ id: '1', type: 'success', message: 'Saved' }],
  }),
}));
vi.mock('@/utils/notificationUtils', () => ({
  getToastIcon: () => ({ name: 'IconStub' }),
}));

import ToastContainer from '@/components/common/ToastContainer.vue';

describe('ToastContainer', () => {
  it('initializes on mount, renders toast, and clears on unmount', async () => {
    const wrapper = mount(ToastContainer, { attachTo: document.body });
    expect(initializeToast).toHaveBeenCalled();
    expect(document.body.textContent).toContain('Saved');

    const closeBtn = document.body.querySelector('button.toast-close') as HTMLElement;
    closeBtn?.click();
    expect(hideToast).toHaveBeenCalledWith('1');

    wrapper.unmount();
    expect(clearAllToasts).toHaveBeenCalled();
  });
});
