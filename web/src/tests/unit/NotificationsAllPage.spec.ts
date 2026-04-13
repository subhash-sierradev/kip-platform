/* eslint-disable simple-import-sort/imports */
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import { nextTick, reactive } from 'vue';
import * as notificationDisplay from '@/utils/notificationDisplay';

const { markAsRead, markAllAsRead, deleteNotification, deleteAllNotifications, push } = vi.hoisted(
  () => ({
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
    deleteNotification: vi.fn(),
    deleteAllNotifications: vi.fn(),
    push: vi.fn(),
  })
);

vi.mock('vue-router', () => ({
  useRouter: () => ({ push }),
}));

vi.mock('@/api/services/UserNotificationService', () => ({
  UserNotificationService: {
    markAsRead,
    markAllAsRead,
    deleteNotification,
    deleteAllNotifications,
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
  unreadCount: 0,
  filteredLoaded: true,
  filteredLoadingMore: false,
  filteredHasMore: false,
  filteredNotifications: [] as any[],
  tabCounts: {
    all: 0,
    unread: 0,
    read: 0,
    error: 0,
    info: 0,
    success: 0,
    warning: 0,
  },
  fetchCount: vi.fn(),
  fetchTabNotifications: vi.fn(),
  refreshNotifications: vi.fn(),
  refreshActiveTabNotifications: vi.fn(),
  loadMoreTabNotifications: vi.fn(),
  getTabCount: vi.fn((tab: string) => (storeState.tabCounts as any)[tab] ?? 0),
  markReadLocal: vi.fn(),
  markAllReadLocal: vi.fn(),
  removeNotificationLocal: vi.fn(),
  removeAllNotificationsLocal: vi.fn(),
});

vi.mock('@/store/notification', () => ({
  useNotificationStore: () => storeState,
}));

import NotificationsAllPage from '@/components/notifications/NotificationsAllPage.vue';

function createNotification(overrides: Record<string, unknown> = {}) {
  return {
    id: 'n1',
    isRead: false,
    severity: 'INFO',
    type: 'INTEGRATION_CONNECTION_CREATED',
    title: 'New event',
    message: 'Message',
    createdDate: new Date().toISOString(),
    ...overrides,
  };
}

describe('NotificationsAllPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Element.prototype.scrollIntoView = vi.fn();
    storeState.unreadCount = 0;
    storeState.filteredLoaded = true;
    storeState.filteredLoadingMore = false;
    storeState.filteredHasMore = false;
    storeState.filteredNotifications = [];
    storeState.tabCounts = {
      all: 0,
      unread: 0,
      read: 0,
      error: 0,
      info: 0,
      success: 0,
      warning: 0,
    };
    markAsRead.mockResolvedValue(undefined);
    markAllAsRead.mockResolvedValue(undefined);
    deleteNotification.mockResolvedValue(undefined);
    deleteAllNotifications.mockResolvedValue(undefined);
  });

  it('shows empty state and triggers initial fetch when not loaded', async () => {
    storeState.filteredLoaded = false;
    const wrapper = mount(NotificationsAllPage);
    await Promise.resolve();
    await new Promise(resolve => setTimeout(resolve, 0));
    expect(storeState.fetchCount).toHaveBeenCalled();
    expect(storeState.fetchTabNotifications).toHaveBeenCalled();
    expect(wrapper.find('.notifications-loading').exists()).toBe(true);
  });

  it('marks one unread item as read on click', async () => {
    storeState.filteredNotifications = [createNotification()] as any;
    storeState.tabCounts.all = 1;
    storeState.unreadCount = 1;

    const wrapper = mount(NotificationsAllPage);
    await wrapper.find('.notification-card').trigger('click');
    await Promise.resolve();

    expect(storeState.markReadLocal).toHaveBeenCalledWith(['n1']);
    expect(markAsRead).toHaveBeenCalledWith(['n1']);
  });

  it('renders the unread indicator only for unread notifications', async () => {
    storeState.filteredNotifications = [
      createNotification({ id: 'n1', isRead: false }),
      createNotification({ id: 'n2', isRead: true }),
    ] as any;
    storeState.tabCounts.all = 2;

    const wrapper = mount(NotificationsAllPage);
    expect(wrapper.findAll('.unread-indicator')).toHaveLength(1);
  });

  it('marks all unread and supports load-more interaction', async () => {
    storeState.filteredNotifications = [
      createNotification({
        id: 'n1',
        isRead: false,
        severity: 'ERROR',
        type: 'A',
        title: 'A',
        message: 'a',
      }),
      createNotification({
        id: 'n2',
        isRead: true,
        severity: 'SUCCESS',
        type: 'B',
        title: 'B',
        message: 'b',
      }),
    ] as any;
    storeState.tabCounts.all = 2;
    storeState.unreadCount = 1;
    storeState.filteredHasMore = true;

    const wrapper = mount(NotificationsAllPage);
    const buttons = wrapper.findAll('.dx-button');
    const markAllButton = buttons.find(button => button.text().includes('Mark all read'));
    const loadMoreButton = buttons.find(button => button.text().includes('Load more'));

    expect(markAllButton).toBeDefined();
    expect(loadMoreButton).toBeDefined();

    await markAllButton!.trigger('click');

    expect(storeState.markAllReadLocal).toHaveBeenCalled();
    expect(markAllAsRead).toHaveBeenCalled();

    await loadMoreButton!.trigger('click');
    expect(storeState.loadMoreTabNotifications).toHaveBeenCalled();
  });

  it('deletes selected notification and calls API', async () => {
    storeState.filteredNotifications = [
      createNotification({
        id: 'n1',
        isRead: true,
        severity: 'INFO',
        type: 'A',
        title: 'A',
        message: 'a',
      }),
    ] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    const deleteButton = wrapper.findAll('.dx-button').find(button => button.text().trim() === '');

    expect(deleteButton).toBeDefined();
    await deleteButton!.trigger('click');

    expect(storeState.removeNotificationLocal).toHaveBeenCalledWith('n1');
    expect(deleteNotification).toHaveBeenCalledWith('n1');
  });

  it('refreshes when single delete API fails', async () => {
    storeState.filteredNotifications = [
      createNotification({
        id: 'n1',
        isRead: true,
        severity: 'INFO',
        type: 'A',
        title: 'A',
        message: 'a',
      }),
    ] as any;
    storeState.tabCounts.all = 1;
    deleteNotification.mockRejectedValueOnce(new Error('delete failed'));

    const wrapper = mount(NotificationsAllPage);
    const deleteButton = wrapper.findAll('.dx-button').find(button => button.text().trim() === '');
    await deleteButton!.trigger('click');
    await Promise.resolve();

    expect(storeState.removeNotificationLocal).toHaveBeenCalledWith('n1');
    expect(deleteNotification).toHaveBeenCalledWith('n1');
    expect(storeState.refreshActiveTabNotifications).toHaveBeenCalled();
  });

  it('deletes all notifications and calls API', async () => {
    storeState.filteredNotifications = [
      createNotification({
        id: 'n1',
        isRead: false,
        severity: 'ERROR',
        type: 'A',
        title: 'A',
        message: 'a',
      }),
      createNotification({
        id: 'n2',
        isRead: true,
        severity: 'SUCCESS',
        type: 'B',
        title: 'B',
        message: 'b',
      }),
    ] as any;
    storeState.tabCounts.all = 2;

    const wrapper = mount(NotificationsAllPage);
    const deleteAllButton = wrapper
      .findAll('.dx-button')
      .find(button => button.text().includes('Delete all'));

    expect(deleteAllButton).toBeDefined();
    await deleteAllButton!.trigger('click');

    expect(storeState.removeAllNotificationsLocal).toHaveBeenCalled();
    expect(deleteAllNotifications).toHaveBeenCalled();
  });

  it('refreshes when delete-all API fails', async () => {
    storeState.filteredNotifications = [
      createNotification({
        id: 'n1',
        isRead: false,
        severity: 'ERROR',
        type: 'A',
        title: 'A',
        message: 'a',
      }),
    ] as any;
    storeState.tabCounts.all = 1;
    deleteAllNotifications.mockRejectedValueOnce(new Error('delete all failed'));

    const wrapper = mount(NotificationsAllPage);
    const deleteAllButton = wrapper
      .findAll('.dx-button')
      .find(button => button.text().includes('Delete all'));
    await deleteAllButton!.trigger('click');
    await Promise.resolve();

    expect(storeState.removeAllNotificationsLocal).toHaveBeenCalled();
    expect(deleteAllNotifications).toHaveBeenCalled();
    expect(storeState.refreshActiveTabNotifications).toHaveBeenCalled();
  });

  it('refreshes when mark-as-read API fails', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: false })] as any;
    storeState.tabCounts.all = 1;
    storeState.unreadCount = 1;
    markAsRead.mockRejectedValueOnce(new Error('mark read failed'));

    const wrapper = mount(NotificationsAllPage);
    await wrapper.find('.notification-card').trigger('click');
    await Promise.resolve();

    expect(storeState.markReadLocal).toHaveBeenCalledWith(['n1']);
    expect(markAsRead).toHaveBeenCalledWith(['n1']);
    expect(storeState.refreshNotifications).toHaveBeenCalled();
    expect(storeState.refreshActiveTabNotifications).toHaveBeenCalled();
  });

  it('supports keyboard navigation immediately on landing with ArrowUp and ArrowDown', async () => {
    storeState.filteredNotifications = [
      createNotification({ id: 'n1', isRead: true, title: 'First' }),
      createNotification({ id: 'n2', isRead: true, title: 'Second' }),
      createNotification({ id: 'n3', isRead: true, title: 'Third' }),
    ] as any;
    storeState.tabCounts.all = 3;

    const wrapper = mount(NotificationsAllPage, { attachTo: document.body });
    await Promise.resolve();

    const listPanel = wrapper.find('.notifications-list-panel');

    await listPanel.trigger('keydown', { key: 'ArrowUp' });
    expect(wrapper.find('.notification-card.selected').attributes('data-notification-id')).toBe(
      'n3'
    );

    await listPanel.trigger('keydown', { key: 'ArrowDown' });
    expect(wrapper.find('.notification-card.selected').attributes('data-notification-id')).toBe(
      'n3'
    );

    wrapper.unmount();
  });

  it('selects the first notification on ArrowDown when nothing is selected yet', async () => {
    storeState.filteredNotifications = [
      createNotification({ id: 'n1', isRead: true, title: 'First' }),
      createNotification({ id: 'n2', isRead: true, title: 'Second' }),
    ] as any;
    storeState.tabCounts.all = 2;

    const wrapper = mount(NotificationsAllPage, { attachTo: document.body });
    const listPanel = wrapper.find('.notifications-list-panel');

    await listPanel.trigger('keydown', { key: 'ArrowDown', preventDefault: vi.fn() });
    await nextTick();

    expect((wrapper.vm as any).selectedNotificationId).toBe('n1');
    wrapper.unmount();
  });

  it('ignores arrow-key navigation when there are no filtered notifications', async () => {
    const wrapper = mount(NotificationsAllPage);
    await nextTick();

    await (wrapper.vm as any).handleListKeydown({
      key: 'ArrowDown',
      preventDefault: vi.fn(),
    });

    expect((wrapper.vm as any).selectedNotificationId).toBe(null);
  });

  it('still calls server mark-all when unread count indicates unread notifications', async () => {
    storeState.filteredNotifications = [
      createNotification({ id: 'n1', isRead: true }),
      createNotification({ id: 'n2', isRead: true }),
    ] as any;
    storeState.tabCounts.all = 2;
    storeState.unreadCount = 1;

    const wrapper = mount(NotificationsAllPage);
    const markAllButton = wrapper
      .findAll('.dx-button')
      .find(button => button.text().includes('Mark all read'));

    await markAllButton!.trigger('click');

    expect(storeState.markAllReadLocal).toHaveBeenCalled();
  });

  it('does not call mark-all when unread count is zero', async () => {
    const wrapper = mount(NotificationsAllPage);

    await (wrapper.vm as any).handleMarkAllRead();

    expect(storeState.markAllReadLocal).not.toHaveBeenCalled();
    expect(markAllAsRead).not.toHaveBeenCalled();
  });

  it('does not call delete-all when the active tab count is zero', async () => {
    const wrapper = mount(NotificationsAllPage);

    await (wrapper.vm as any).handleDeleteAll();

    expect(storeState.removeAllNotificationsLocal).not.toHaveBeenCalled();
    expect(deleteAllNotifications).not.toHaveBeenCalled();
  });

  it('renders the unread badge and disables load-more while filtered loading is active', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: false })] as any;
    storeState.filteredHasMore = true;
    storeState.filteredLoadingMore = true;
    storeState.tabCounts.unread = 1;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    expect(wrapper.text()).toContain('Unread');
    expect(wrapper.find('.filter-tab-badge').text()).toBe('1');

    const loadMoreButton = wrapper
      .findAll('.dx-button')
      .find(button => button.text().includes('Load more'));

    expect(loadMoreButton?.attributes('disabled')).toBeDefined();
  });

  it('returns early from markNotificationsAsRead when all provided ids are empty', async () => {
    const wrapper = mount(NotificationsAllPage);

    await (wrapper.vm as any).markNotificationsAsRead(['', '']);

    expect(storeState.markReadLocal).not.toHaveBeenCalled();
    expect(markAsRead).not.toHaveBeenCalled();
  });

  it('does not re-fetch when clicking the already active filter tab', async () => {
    const wrapper = mount(NotificationsAllPage);
    vi.clearAllMocks();

    await (wrapper.vm as any).handleFilterTabChange('all');

    expect(storeState.fetchTabNotifications).not.toHaveBeenCalled();
    expect(markAsRead).not.toHaveBeenCalled();
  });

  it('switches tabs, clears selection, and fetches the new tab', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    vi.clearAllMocks();

    await (wrapper.vm as any).handleFilterTabChange('success');

    expect(storeState.fetchTabNotifications).toHaveBeenCalledWith('success');
    expect((wrapper.vm as any).selectedNotificationId).toBe(null);
  });

  describe('unread tab deferred mark-as-read behavior', () => {
    it('does not mark as read immediately when selecting an item in Unread tab', async () => {
      storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: false })] as any;
      storeState.tabCounts.unread = 1;
      storeState.unreadCount = 1;

      const wrapper = mount(NotificationsAllPage);
      // Simulate clicking on the unread tab first
      const unreadTab = wrapper.findAll('button').find(button => button.text().includes('Unread'));
      await unreadTab!.trigger('click');
      await Promise.resolve();

      // Click the notification
      await wrapper.find('.notification-card').trigger('click');
      await Promise.resolve();

      // Should only call markReadLocal and API when the component unmounts or switches notifications
      // NOT when selecting in the unread tab
      expect(markAsRead).not.toHaveBeenCalled();
    });

    it('marks previously selected notification as read when switching to another in Unread tab', async () => {
      storeState.filteredNotifications = [
        createNotification({ id: 'n1', isRead: false }),
        createNotification({ id: 'n2', isRead: false }),
      ] as any;
      storeState.tabCounts.unread = 2;
      storeState.unreadCount = 2;

      const wrapper = mount(NotificationsAllPage);
      // Switch to unread tab
      const unreadTab = wrapper.findAll('button').find(button => button.text().includes('Unread'));
      await unreadTab!.trigger('click');
      await Promise.resolve();

      // Click first notification
      const cards = wrapper.findAll('.notification-card');
      await cards[0].trigger('click');
      await Promise.resolve();
      vi.clearAllMocks();

      // Click second notification
      await cards[1].trigger('click');
      await Promise.resolve();

      // Now first notification should be marked as read
      expect(storeState.markReadLocal).toHaveBeenCalledWith(['n1']);
      expect(markAsRead).toHaveBeenCalledWith(['n1']);
    });

    it('marks current notification as read when switching away from Unread tab', async () => {
      storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: false })] as any;
      storeState.tabCounts.unread = 1;
      storeState.unreadCount = 1;

      const wrapper = mount(NotificationsAllPage);
      // Switch to unread tab
      const unreadTab = wrapper.findAll('button').find(button => button.text().includes('Unread'));
      await unreadTab!.trigger('click');
      await Promise.resolve();

      // Click notification
      await wrapper.find('.notification-card').trigger('click');
      await Promise.resolve();
      vi.clearAllMocks();

      // Switch to another tab (e.g., All)
      const allTab = wrapper.findAll('button').find(button => button.text() === 'All');
      await allTab!.trigger('click');
      await Promise.resolve();

      // The selected notification should now be marked as read
      expect(storeState.markReadLocal).toHaveBeenCalledWith(['n1']);
      expect(markAsRead).toHaveBeenCalledWith(['n1']);
    });
  });

  it('opens external primary action in a new window', async () => {
    const getPrimaryActionSpy = vi
      .spyOn(notificationDisplay, 'getPrimaryAction')
      .mockReturnValue({ label: 'Open External', target: 'https://example.com', external: true });
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    const primaryActionLink = wrapper.find('.details-action-link');

    expect(primaryActionLink.exists()).toBe(true);
    await primaryActionLink.trigger('click');

    expect(openSpy).toHaveBeenCalledWith('https://example.com', '_blank', 'noopener,noreferrer');
    expect(push).not.toHaveBeenCalled();

    getPrimaryActionSpy.mockRestore();
    openSpy.mockRestore();
  });

  it('does nothing when no primary action is available', async () => {
    const getPrimaryActionSpy = vi
      .spyOn(notificationDisplay, 'getPrimaryAction')
      .mockReturnValue(null);

    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    await (wrapper.vm as any).handlePrimaryAction();

    expect(push).not.toHaveBeenCalled();
    getPrimaryActionSpy.mockRestore();
  });

  it('routes to internal primary action target', async () => {
    const getPrimaryActionSpy = vi
      .spyOn(notificationDisplay, 'getPrimaryAction')
      .mockReturnValue({ label: 'Open Details', target: '/notifications/1', external: false });

    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    const primaryActionLink = wrapper.find('.details-action-link');

    expect(primaryActionLink.exists()).toBe(true);
    await primaryActionLink.trigger('click');

    expect(push).toHaveBeenCalledWith('/notifications/1');

    getPrimaryActionSpy.mockRestore();
  });

  it('renders localized embedded ISO timestamp in list message text', async () => {
    storeState.filteredNotifications = [
      createNotification({
        id: 'n1',
        isRead: true,
        message: 'Completed at 2026-03-05T09:52:44.922361300Z.',
      }),
    ] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    const messageText = wrapper.find('.notification-card-message').text();

    expect(messageText).toContain('Completed at ');
    expect(messageText).not.toContain('2026-03-05T09:52:44.922361300Z');
  });

  it('falls back to Just now when relative time formatting returns an empty string', () => {
    const relativeSpy = vi.spyOn(notificationDisplay, 'formatRelativeTime').mockReturnValue('');

    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    expect(wrapper.text()).toContain('Just now');

    relativeSpy.mockRestore();
  });

  it('renders structured parsed messages in the details panel', () => {
    const parseSpy = vi.spyOn(notificationDisplay, 'parseNotificationMessage').mockReturnValue({
      type: 'structured',
      summary: '2 records failed',
      items: [
        { index: 1, doc: 'DOC-1', location: 'LOC-1', reason: 'First reason' },
        { index: 2, doc: 'DOC-2', location: 'LOC-2', reason: 'Second reason' },
      ],
    } as any);

    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);

    expect(wrapper.find('.details-message-structured').exists()).toBe(true);
    expect(wrapper.text()).toContain('2 records failed');
    expect(wrapper.text()).toContain('First reason');
    expect(wrapper.text()).toContain('Second reason');

    parseSpy.mockRestore();
  });

  it('marks the selected unread notification as read when the page unmounts', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: false })] as any;
    storeState.tabCounts.unread = 1;
    storeState.unreadCount = 1;

    const wrapper = mount(NotificationsAllPage);
    await (wrapper.vm as any).handleFilterTabChange('unread');
    await nextTick();
    (wrapper.vm as any).selectedNotificationId = 'n1';
    vi.clearAllMocks();

    wrapper.unmount();
    await Promise.resolve();

    expect(storeState.markReadLocal).toHaveBeenCalledWith(['n1']);
    expect(markAsRead).toHaveBeenCalledWith(['n1']);
  });

  it('supports resizing the split view within min and max bounds', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage, { attachTo: document.body });
    await nextTick();

    const container = wrapper.find('.notifications-content').element as HTMLElement;
    Object.defineProperty(container, 'getBoundingClientRect', {
      configurable: true,
      value: () => ({ left: 100, width: 1000 }),
    });

    const handle = wrapper.find('.notifications-resize-handle');
    await handle.trigger('mousedown', { clientX: 450, preventDefault: vi.fn() });
    document.dispatchEvent(new MouseEvent('mousemove', { clientX: 150 }));
    await nextTick();
    expect((wrapper.find('.notifications-list-panel').element as HTMLElement).style.width).toBe(
      '320px'
    );

    document.dispatchEvent(new MouseEvent('mousemove', { clientX: 900 }));
    await nextTick();
    expect((wrapper.find('.notifications-list-panel').element as HTMLElement).style.width).toBe(
      '580px'
    );

    document.dispatchEvent(new MouseEvent('mouseup'));
    wrapper.unmount();
  });

  it('ignores resize movement when not actively resizing', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    const initialWidth = (wrapper.vm as any).listPanelStyle.width;

    (wrapper.vm as any).resizePanels({ clientX: 999 } as MouseEvent);

    expect((wrapper.vm as any).listPanelStyle.width).toBe(initialWidth);
  });

  it('does not call mark-as-read again when selecting an already-read notification', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    vi.clearAllMocks();

    await wrapper.find('.notification-card').trigger('click');
    await nextTick();

    expect(storeState.markReadLocal).not.toHaveBeenCalled();
    expect(markAsRead).not.toHaveBeenCalled();
  });

  it('clears the selected notification when the filtered list becomes empty', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    await nextTick();

    (wrapper.vm as any).selectedNotificationId = 'n1';

    storeState.filteredNotifications = [] as any;
    storeState.tabCounts.all = 0;
    await nextTick();

    expect((wrapper.vm as any).selectedNotificationId).toBe(null);
    expect(wrapper.find('.notifications-empty').exists()).toBe(true);
  });

  it('shows the detail empty state when the selected notification id no longer exists', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    await nextTick();

    (wrapper.vm as any).selectedNotificationId = 'missing-id';
    await nextTick();

    expect(wrapper.find('.details-empty').exists()).toBe(true);
  });

  it('clears the selection immediately when deleting the currently selected notification', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    await nextTick();

    await wrapper.find('.notification-card').trigger('click');
    await nextTick();

    expect((wrapper.vm as any).selectedNotificationId).toBe('n1');

    await (wrapper.vm as any).handleDelete('n1');

    expect((wrapper.vm as any).selectedNotificationId).toBe(null);
    expect(storeState.removeNotificationLocal).toHaveBeenCalledWith('n1');
    expect(deleteNotification).toHaveBeenCalledWith('n1');
  });

  it('ignores non-arrow key presses in the notification list', async () => {
    storeState.filteredNotifications = [createNotification({ id: 'n1', isRead: true })] as any;
    storeState.tabCounts.all = 1;

    const wrapper = mount(NotificationsAllPage);
    const listPanel = wrapper.find('.notifications-list-panel');
    (wrapper.vm as any).selectedNotificationId = 'n1';
    const selectedBefore = (wrapper.vm as any).selectedNotificationId;

    await listPanel.trigger('keydown', { key: 'Enter', preventDefault: vi.fn() });
    await nextTick();

    expect((wrapper.vm as any).selectedNotificationId).toBe(selectedBefore);
    expect(storeState.markReadLocal).not.toHaveBeenCalled();
  });
});
