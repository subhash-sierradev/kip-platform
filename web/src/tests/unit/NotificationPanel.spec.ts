/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { reactive } from 'vue';

const { push, markAsRead, markAllAsRead, deleteNotification } = vi.hoisted(() => ({
  push: vi.fn(),
  markAsRead: vi.fn(),
  markAllAsRead: vi.fn(),
  deleteNotification: vi.fn(),
}));

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}));

vi.mock('@/api/services/UserNotificationService', () => ({
  UserNotificationService: {
    markAsRead,
    markAllAsRead,
    deleteNotification,
  },
}));

vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    name: 'DxButton',
    props: ['text', 'disabled'],
    template:
      '<button class="dx-button" :disabled="disabled" @click="$emit(\'click\')">{{ text }}</button>',
  },
}));

const storeState = reactive({
  notifications: [] as any[],
  unreadCount: 0,
  markReadLocal: vi.fn(),
  markAllReadLocal: vi.fn(),
  refreshNotifications: vi.fn(),
  removeNotificationLocal: vi.fn(),
  removeAllNotificationsLocal: vi.fn(),
});

vi.mock('@/store/notification', () => ({
  useNotificationStore: () => storeState,
}));

import NotificationPanel from '@/components/layout/NotificationPanel.vue';

describe('NotificationPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    storeState.notifications = [];
    storeState.unreadCount = 0;
  });

  it('renders empty state and handles view-all action', async () => {
    const wrapper = mount(NotificationPanel);

    expect(wrapper.text()).toContain('No notifications');

    storeState.notifications = [
      {
        id: 'n1',
        isRead: false,
        severity: 'INFO',
        title: '1',
        message: '1',
        createdDate: new Date().toISOString(),
      },
    ] as any;

    const wrapperWithItems = mount(NotificationPanel);

    expect(wrapperWithItems.find('.view-all-btn').text()).toContain('Show All Notifications');
    await wrapperWithItems.find('.view-all-btn').trigger('click');
    expect(push).toHaveBeenCalledWith('/notifications');
    expect(wrapperWithItems.emitted('close')).toBeTruthy();
  });

  it('renders all notifications and enables scroll mode when more than three', () => {
    storeState.notifications = [
      {
        id: 'n1',
        isRead: false,
        severity: 'INFO',
        title: '1',
        message: '1',
        createdDate: new Date().toISOString(),
      },
      {
        id: 'n2',
        isRead: false,
        severity: 'INFO',
        title: '2',
        message: '2',
        createdDate: new Date().toISOString(),
      },
      {
        id: 'n3',
        isRead: false,
        severity: 'INFO',
        title: '3',
        message: '3',
        createdDate: new Date().toISOString(),
      },
      {
        id: 'n4',
        isRead: false,
        severity: 'INFO',
        title: '4',
        message: '4',
        createdDate: new Date().toISOString(),
      },
    ] as any;

    const wrapper = mount(NotificationPanel);
    expect(wrapper.findAll('.notification-item-row')).toHaveLength(4);
    expect(wrapper.find('.notification-list').classes()).toContain('notification-list-scroll');
  });

  it('marks one notification as read on click and calls API', async () => {
    storeState.notifications = [
      {
        id: 'n1',
        isRead: false,
        severity: 'INFO',
        title: 'Title',
        message: 'Message',
        createdDate: new Date().toISOString(),
      },
    ] as any;
    storeState.unreadCount = 1;

    const wrapper = mount(NotificationPanel);
    await wrapper.find('.notification-item-row').trigger('click');

    expect(storeState.markReadLocal).toHaveBeenCalledWith(['n1']);
    expect(markAsRead).toHaveBeenCalledWith(['n1']);
  });

  it('does not mark notifications as read when the selected row is already read', async () => {
    storeState.notifications = [
      {
        id: 'n1',
        isRead: true,
        severity: 'INFO',
        title: 'Title',
        message: 'Message',
        createdDate: new Date().toISOString(),
      },
    ] as any;

    const wrapper = mount(NotificationPanel);
    await wrapper.find('.notification-item-row').trigger('click');

    expect(storeState.markReadLocal).not.toHaveBeenCalled();
    expect(markAsRead).not.toHaveBeenCalled();
  });

  it('marks all unread and refreshes on API failure', async () => {
    storeState.notifications = [
      {
        id: 'n1',
        isRead: false,
        severity: 'WARNING',
        title: 'A',
        message: 'a',
        createdDate: new Date().toISOString(),
      },
      {
        id: 'n2',
        isRead: true,
        severity: 'INFO',
        title: 'B',
        message: 'b',
        createdDate: new Date().toISOString(),
      },
    ] as any;
    storeState.unreadCount = 1;
    markAllAsRead.mockRejectedValueOnce(new Error('x'));

    const wrapper = mount(NotificationPanel);
    const buttons = wrapper.findAll('.dx-button');
    await buttons[0].trigger('click');

    expect(storeState.markAllReadLocal).toHaveBeenCalled();
    expect(markAllAsRead).toHaveBeenCalled();
    expect(storeState.refreshNotifications).toHaveBeenCalled();
  });

  it('returns early when mark-all is requested with zero unread notifications', async () => {
    const wrapper = mount(NotificationPanel);

    await (wrapper.vm as any).handleMarkAllRead();

    expect(storeState.markAllReadLocal).not.toHaveBeenCalled();
    expect(markAllAsRead).not.toHaveBeenCalled();
  });

  it('marks all unread notifications without refreshing when the API succeeds', async () => {
    storeState.notifications = [
      {
        id: 'n1',
        isRead: false,
        severity: 'WARNING',
        title: 'A',
        message: 'a',
        createdDate: new Date().toISOString(),
      },
    ] as any;
    storeState.unreadCount = 1;

    const wrapper = mount(NotificationPanel);
    await (wrapper.vm as any).handleMarkAllRead();

    expect(storeState.markAllReadLocal).toHaveBeenCalled();
    expect(storeState.refreshNotifications).not.toHaveBeenCalled();
    expect((wrapper.vm as any).markingAll).toBe(false);
  });

  it('deletes a notification row and calls delete API', async () => {
    storeState.notifications = [
      {
        id: 'n1',
        isRead: true,
        severity: 'INFO',
        title: 'Title',
        message: 'Message',
        createdDate: new Date().toISOString(),
      },
    ] as any;

    const wrapper = mount(NotificationPanel);
    await wrapper.find('.row-action-btn').trigger('click');

    expect(storeState.removeNotificationLocal).toHaveBeenCalledWith('n1');
    expect(deleteNotification).toHaveBeenCalledWith('n1');
  });

  it('refreshes notifications when row delete API fails', async () => {
    storeState.notifications = [
      {
        id: 'n1',
        isRead: true,
        severity: 'INFO',
        title: 'Title',
        message: 'Message',
        createdDate: new Date().toISOString(),
      },
    ] as any;
    deleteNotification.mockRejectedValueOnce(new Error('delete failed'));

    const wrapper = mount(NotificationPanel);
    await wrapper.find('.row-action-btn').trigger('click');
    await Promise.resolve();

    expect(storeState.removeNotificationLocal).toHaveBeenCalledWith('n1');
    expect(deleteNotification).toHaveBeenCalledWith('n1');
    expect(storeState.refreshNotifications).toHaveBeenCalled();
  });

  it('localizes embedded ISO timestamps in row message text', () => {
    storeState.notifications = [
      {
        id: 'n1',
        isRead: true,
        severity: 'INFO',
        title: 'Title',
        message: 'Processed at 2026-03-05T09:52:44.922361300Z',
        createdDate: new Date().toISOString(),
      },
    ] as any;

    const wrapper = mount(NotificationPanel);
    const messageText = wrapper.find('.notification-row-message').text();

    expect(messageText).toContain('Processed at ');
    expect(messageText).not.toContain('2026-03-05T09:52:44.922361300Z');
  });
});
