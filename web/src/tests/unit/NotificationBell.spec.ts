import { mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import NotificationBell from '@/components/layout/NotificationBell.vue';

const unreadCount = { value: 120 };

vi.mock('@/store/notification', () => ({
  useNotificationStore: () => ({
    get unreadCount() {
      return unreadCount.value;
    },
  }),
}));

vi.mock('lucide-vue-next', () => ({
  Bell: {
    template: '<svg class="bell-icon" />',
  },
}));

vi.mock('@/components/layout/NotificationPanel.vue', () => ({
  default: {
    name: 'NotificationPanel',
    template:
      '<div class="notification-panel"><button class="close-panel" @click="$emit(\'close\')">close</button></div>',
    emits: ['close'],
  },
}));

describe('NotificationBell', () => {
  afterEach(() => {
    unreadCount.value = 120;
  });

  it('shows a capped badge and toggles the notification panel', async () => {
    const wrapper = mount(NotificationBell, {
      attachTo: document.body,
      global: {
        stubs: {
          Transition: false,
        },
      },
    });

    expect(wrapper.find('.notification-badge').text()).toBe('99+');

    await wrapper.find('.notification-container').trigger('click');
    expect((wrapper.vm as any).showPanel).toBe(true);

    wrapper.findComponent({ name: 'NotificationPanel' }).vm.$emit('close');
    await nextTick();
    expect((wrapper.vm as any).showPanel).toBe(false);

    wrapper.unmount();
  });

  it('closes on outside clicks and removes the document listener on unmount', async () => {
    const addEventListenerSpy = vi.spyOn(document, 'addEventListener');
    const removeEventListenerSpy = vi.spyOn(document, 'removeEventListener');

    unreadCount.value = 3;
    const wrapper = mount(NotificationBell, {
      attachTo: document.body,
      global: {
        stubs: {
          Transition: false,
        },
      },
    });

    expect(addEventListenerSpy).toHaveBeenCalledWith('click', expect.any(Function));

    await wrapper.find('.notification-container').trigger('click');
    expect((wrapper.vm as any).showPanel).toBe(true);

    const clickHandler = addEventListenerSpy.mock.calls.find(call => call[0] === 'click')?.[1] as
      | ((event: Event) => void)
      | undefined;
    clickHandler?.({ target: document.body } as unknown as Event);
    await nextTick();
    expect((wrapper.vm as any).showPanel).toBe(false);

    wrapper.unmount();

    expect(removeEventListenerSpy).toHaveBeenCalledWith('click', expect.any(Function));
  });
});
