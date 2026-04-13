/* eslint-disable simple-import-sort/imports */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

vi.mock('@/api/services/UserNotificationService', () => ({
  UserNotificationService: {
    getUnreadCount: vi.fn(),
    getNotifications: vi.fn(),
    markAsRead: vi.fn(),
  },
}));

import { UserNotificationService } from '@/api/services/UserNotificationService';
import { useNotificationStore } from '@/store/notification';

const makeNotification = (id: string, isRead = false) => ({
  id,
  title: `Title ${id}`,
  message: `Message ${id}`,
  severity: 'INFO',
  type: 'INTEGRATION_CONNECTION_CREATED',
  createdDate: new Date().toISOString(),
  isRead,
});

describe('notification store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.useFakeTimers();
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('fetches unread count and handles service errors silently', async () => {
    (UserNotificationService.getUnreadCount as any).mockResolvedValueOnce({
      allCount: 12,
      unreadCount: 4,
      readCount: 8,
      errorCount: 1,
      infoCount: 6,
      successCount: 3,
      warningCount: 2,
    });
    const store = useNotificationStore();

    await store.fetchCount();
    expect(store.unreadCount).toBe(4);

    (UserNotificationService.getUnreadCount as any).mockRejectedValueOnce(new Error('boom'));
    await store.fetchCount();
    expect(store.unreadCount).toBe(4);
    expect(store.getTabCount('all')).toBe(12);
    expect(store.getTabCount('error')).toBe(1);
  });

  it('defaults missing count fields to zero when fetching counts', async () => {
    (UserNotificationService.getUnreadCount as any).mockResolvedValueOnce({
      successCount: 5,
    });

    const store = useNotificationStore();
    await store.fetchCount();

    expect(store.unreadCount).toBe(0);
    expect(store.getTabCount('all')).toBe(0);
    expect(store.getTabCount('read')).toBe(0);
    expect(store.getTabCount('success')).toBe(5);
    expect(store.getTabCount('warning')).toBe(0);
  });

  it('fetches first page notifications and sets pagination state', async () => {
    (UserNotificationService.getNotifications as any).mockResolvedValueOnce({
      content: [makeNotification('n1'), makeNotification('n2', true)],
      totalElements: 3,
      totalPages: 1,
    });

    const store = useNotificationStore();
    await store.fetchNotifications();

    expect(store.loaded).toBe(true);
    expect(store.notifications).toHaveLength(2);
    expect(store.totalElements).toBe(3);
    expect(store.currentPage).toBe(0);
    expect(store.hasMore).toBe(false);
  });

  it('falls back to content length when the first notifications page omits totalElements', async () => {
    (UserNotificationService.getNotifications as any).mockResolvedValueOnce({
      content: [makeNotification('n1'), makeNotification('n2')],
      totalPages: 1,
    });

    const store = useNotificationStore();
    await store.fetchNotifications();

    expect(store.totalElements).toBe(2);
    expect(store.getTabCount('all')).toBe(2);
  });

  it('loads next page, deduplicates by id, and updates hasMore', async () => {
    const store = useNotificationStore();
    store.notifications = [makeNotification('n1')] as any;
    store.currentPage = 0 as any;
    store.hasMore = true as any;

    (UserNotificationService.getNotifications as any).mockResolvedValueOnce({
      content: [makeNotification('n1'), makeNotification('n3')],
      totalElements: 2,
      totalPages: 1,
    });

    await store.loadMoreNotifications();

    expect(store.notifications.map(n => n.id)).toEqual(['n1', 'n3']);
    expect(store.currentPage).toBe(1);
    expect(store.hasMore).toBe(false);
    expect(store.loadingMore).toBe(false);
  });

  it('fetches tab notifications and sets filtered pagination state', async () => {
    (UserNotificationService.getNotifications as any).mockResolvedValueOnce({
      content: [makeNotification('n1'), makeNotification('n2', true)],
      totalElements: 10,
      totalPages: 2,
    });

    const store = useNotificationStore();
    await store.fetchTabNotifications('success');

    expect(store.activeTab).toBe('success');
    expect(store.filteredLoaded).toBe(true);
    expect(store.filteredNotifications).toHaveLength(2);
    expect(store.filteredTotalElements).toBe(10);
    expect(store.filteredHasMore).toBe(true);
    expect(store.getTabCount('success')).toBe(10);
  });

  it('uses the correct API filters for all notification tabs', async () => {
    const store = useNotificationStore();
    (UserNotificationService.getNotifications as any).mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 1,
    });

    store.activeTab = 'all' as any;
    await store.fetchTabNotifications();

    store.activeTab = 'unread' as any;
    await store.fetchTabNotifications();

    store.activeTab = 'read' as any;
    await store.fetchTabNotifications();

    store.activeTab = 'warning' as any;
    await store.fetchTabNotifications();

    expect(UserNotificationService.getNotifications).toHaveBeenNthCalledWith(1, 0, 20, undefined);
    expect(UserNotificationService.getNotifications).toHaveBeenNthCalledWith(2, 0, 20, {
      readFilter: 'UNREAD',
    });
    expect(UserNotificationService.getNotifications).toHaveBeenNthCalledWith(3, 0, 20, {
      readFilter: 'READ',
    });
    expect(UserNotificationService.getNotifications).toHaveBeenNthCalledWith(4, 0, 20, {
      severity: 'WARNING',
    });
  });

  it('loads more tab notifications and updates filtered pagination state', async () => {
    const store = useNotificationStore();
    store.activeTab = 'error' as any;
    store.filteredNotifications = [makeNotification('n1')] as any;
    store.filteredCurrentPage = 0 as any;
    store.filteredHasMore = true as any;

    (UserNotificationService.getNotifications as any).mockResolvedValueOnce({
      content: [makeNotification('n1'), makeNotification('n2')],
      totalElements: 3,
      totalPages: 2,
    });

    await store.loadMoreTabNotifications();

    expect(store.filteredNotifications.map(n => n.id)).toEqual(['n1', 'n2']);
    expect(store.filteredCurrentPage).toBe(1);
    expect(store.filteredHasMore).toBe(false);
    expect(store.getTabCount('error')).toBe(3);
  });

  it('prevents concurrent loadMore and handles loadMore failures silently', async () => {
    const store = useNotificationStore();
    store.hasMore = true as any;

    store.loadingMore = true as any;
    await store.loadMoreNotifications();
    expect(UserNotificationService.getNotifications).not.toHaveBeenCalled();

    store.loadingMore = false as any;
    (UserNotificationService.getNotifications as any).mockRejectedValueOnce(new Error('x'));
    await store.loadMoreNotifications();
    expect(store.loadingMore).toBe(false);
  });

  it('returns early when loading more notifications is disabled because there are no more pages', async () => {
    const store = useNotificationStore();
    store.hasMore = false as any;

    await store.loadMoreNotifications();

    expect(UserNotificationService.getNotifications).not.toHaveBeenCalled();
  });

  it('returns early for tab loadMore when disabled or already loading', async () => {
    const store = useNotificationStore();
    store.filteredHasMore = false as any;
    await store.loadMoreTabNotifications();
    expect(UserNotificationService.getNotifications).not.toHaveBeenCalled();

    store.filteredHasMore = true as any;
    store.filteredLoadingMore = true as any;
    await store.loadMoreTabNotifications();
    expect(UserNotificationService.getNotifications).not.toHaveBeenCalled();
  });

  it('resets filtered loading state when loading more tab notifications fails', async () => {
    const store = useNotificationStore();
    store.activeTab = 'warning' as any;
    store.filteredHasMore = true as any;
    (UserNotificationService.getNotifications as any).mockRejectedValueOnce(new Error('boom'));

    await store.loadMoreTabNotifications();

    expect(store.filteredLoadingMore).toBe(false);
    expect(store.filteredNotifications).toEqual([]);
  });

  it('adds incoming notifications, avoids duplicates, and updates unread count', () => {
    const store = useNotificationStore();
    store.unreadCount = 0 as any;

    store.addIncoming(makeNotification('n1', false) as any);
    expect(store.notifications).toHaveLength(1);
    expect(store.unreadCount).toBe(1);

    store.addIncoming(makeNotification('n1', false) as any);
    expect(store.notifications).toHaveLength(1);

    store.addIncoming(makeNotification('n2', true) as any);
    expect(store.notifications).toHaveLength(2);
    expect(store.unreadCount).toBe(1);
  });

  it('does not append incoming notification to filtered list when active tab does not match', () => {
    const store = useNotificationStore();
    store.activeTab = 'error' as any;
    store.filteredNotifications = [makeNotification('seed', false)] as any;

    store.addIncoming({ ...makeNotification('info-read', true), severity: 'INFO' } as any);

    expect(store.notifications.map(n => n.id)).toContain('info-read');
    expect(store.filteredNotifications.map(n => n.id)).toEqual(['seed']);
    expect(store.getTabCount('read')).toBe(1);
  });

  it('appends incoming unread notifications to the unread filtered tab', () => {
    const store = useNotificationStore();
    store.activeTab = 'unread' as any;
    store.filteredNotifications = [] as any;
    store.filteredTotalElements = 0 as any;

    store.addIncoming({ ...makeNotification('unread-1', false), createdDate: '' } as any);

    expect(store.filteredNotifications.map(n => n.id)).toEqual(['unread-1']);
    expect(store.filteredTotalElements).toBe(1);
    expect(store.filteredHasMore).toBe(false);
    expect(store.getTabCount('unread')).toBe(1);
  });

  it('appends incoming notifications to the all tab filtered list', () => {
    const store = useNotificationStore();
    store.activeTab = 'all' as any;
    store.filteredNotifications = [makeNotification('seed', true)] as any;
    store.filteredTotalElements = 1 as any;

    store.addIncoming(makeNotification('all-2', false) as any);

    expect(store.filteredNotifications.map(n => n.id)).toEqual(['all-2', 'seed']);
    expect(store.filteredTotalElements).toBe(2);
  });

  it('appends incoming notifications to matching read and severity-filtered tabs', () => {
    const store = useNotificationStore();
    store.activeTab = 'read' as any;

    store.addIncoming({ ...makeNotification('read-1', true), createdDate: '' } as any);
    expect(store.filteredNotifications.map(n => n.id)).toEqual(['read-1']);

    store.activeTab = 'info' as any;
    store.filteredNotifications = [] as any;
    store.filteredTotalElements = 0 as any;

    store.addIncoming({ ...makeNotification('info-1', false), severity: undefined } as any);

    expect(store.filteredNotifications).toEqual([]);
    expect(store.getTabCount('info')).toBe(2);
  });

  it('adds and auto-removes toast notifications', () => {
    const store = useNotificationStore();
    store.addNotificationToast(makeNotification('t1', false) as any);

    expect(store.notificationToasts).toHaveLength(1);
    vi.advanceTimersByTime(6000);
    expect(store.notificationToasts).toHaveLength(0);
  });

  it('preserves provided toast timestamps and ignores removal of unknown toast ids', () => {
    const store = useNotificationStore();
    const createdDate = '2026-04-10T08:00:00.000Z';

    store.addNotificationToast({ ...makeNotification('t2', false), createdDate } as any);
    store.removeNotificationToast('missing');

    expect(store.notificationToasts).toHaveLength(1);
    expect(store.notificationToasts[0].createdDate).toBe(createdDate);
  });

  it('marks selected notifications read locally and updates unread count with floor at zero', () => {
    const store = useNotificationStore();
    store.notifications = [
      makeNotification('n1', false),
      makeNotification('n2', false),
      makeNotification('n3', true),
    ] as any;
    store.unreadCount = 2 as any;

    store.markReadLocal(['n1', 'n3']);
    expect(store.notifications.find(n => n.id === 'n1')?.isRead).toBe(true);
    expect(store.notifications.find(n => n.id === 'n2')?.isRead).toBe(false);
    expect(store.unreadCount).toBe(1);

    store.markReadLocal(['n1', 'n2']);
    expect(store.unreadCount).toBe(0);
  });

  it('removes read notifications from filtered list when active tab is unread', () => {
    const store = useNotificationStore();
    store.activeTab = 'unread' as any;
    store.notifications = [makeNotification('n1', false), makeNotification('n2', false)] as any;
    store.filteredNotifications = [
      makeNotification('n1', false),
      makeNotification('n2', false),
    ] as any;
    store.unreadCount = 2 as any;

    store.markReadLocal(['n1']);

    expect(store.filteredNotifications.map(n => n.id)).toEqual(['n2']);
    expect(store.getTabCount('read')).toBe(1);
  });

  it('keeps unread filtered totals unchanged when marked ids are not present in the filtered list', () => {
    const store = useNotificationStore();
    store.activeTab = 'unread' as any;
    store.notifications = [makeNotification('n1', false), makeNotification('n2', false)] as any;
    store.filteredNotifications = [makeNotification('n2', false)] as any;
    store.filteredTotalElements = 1 as any;
    store.unreadCount = 2 as any;

    store.markReadLocal(['n1']);

    expect(store.filteredNotifications.map(n => n.id)).toEqual(['n2']);
    expect(store.filteredTotalElements).toBe(1);
    expect(store.unreadCount).toBe(1);
  });

  it('marks all notifications read locally and updates unread count', () => {
    const store = useNotificationStore();
    store.notifications = [makeNotification('a', false), makeNotification('b', true)] as any;
    store.unreadCount = 1 as any;

    store.markAllReadLocal();

    expect(store.notifications.every(n => n.isRead)).toBe(true);
    expect(store.unreadCount).toBe(0);
  });

  it('clears unread filtered list on mark-all when active tab is unread', () => {
    const store = useNotificationStore();
    store.activeTab = 'unread' as any;
    store.notifications = [makeNotification('a', false), makeNotification('b', false)] as any;
    store.filteredNotifications = [
      makeNotification('a', false),
      makeNotification('b', false),
    ] as any;
    store.filteredTotalElements = 2 as any;
    store.filteredHasMore = true as any;
    store.unreadCount = 2 as any;

    store.markAllReadLocal();

    expect(store.filteredNotifications).toHaveLength(0);
    expect(store.filteredTotalElements).toBe(0);
    expect(store.filteredHasMore).toBe(false);
  });

  it('returns early in markAllReadLocal when there are no unread notifications', () => {
    const store = useNotificationStore();
    store.notifications = [makeNotification('a', true), makeNotification('b', true)] as any;
    store.filteredNotifications = [makeNotification('a', true)] as any;
    store.unreadCount = 0 as any;

    store.markAllReadLocal();

    expect(store.unreadCount).toBe(0);
    expect(store.getTabCount('read')).toBe(0);
  });

  it('keeps state unchanged when removing unknown notification id', () => {
    const store = useNotificationStore();
    store.notifications = [makeNotification('n1', false)] as any;
    store.totalElements = 1 as any;

    store.removeNotificationLocal('missing');

    expect(store.notifications).toHaveLength(1);
    expect(store.totalElements).toBe(1);
  });

  it('removes read notifications and decrements read tab count', () => {
    const store = useNotificationStore();
    store.notifications = [makeNotification('read-1', true)] as any;
    store.filteredNotifications = [makeNotification('read-1', true)] as any;
    store.totalElements = 1 as any;
    store.filteredTotalElements = 1 as any;
    store.tabCounts = {
      all: 1,
      unread: 0,
      read: 1,
      error: 0,
      info: 1,
      success: 0,
      warning: 0,
    } as any;

    store.removeNotificationLocal('read-1');

    expect(store.notifications).toHaveLength(0);
    expect(store.getTabCount('read')).toBe(0);
    expect(store.getTabCount('all')).toBe(0);
  });

  it('removes matching notifications from the all tab filtered list', () => {
    const store = useNotificationStore();
    const notification = { ...makeNotification('all-1', false), severity: 'INFO' } as any;
    store.activeTab = 'all' as any;
    store.notifications = [notification] as any;
    store.filteredNotifications = [notification] as any;
    store.totalElements = 1 as any;
    store.filteredTotalElements = 1 as any;
    store.unreadCount = 1 as any;
    store.tabCounts = {
      all: 1,
      unread: 1,
      read: 0,
      error: 0,
      info: 1,
      success: 0,
      warning: 0,
    } as any;

    store.removeNotificationLocal('all-1');

    expect(store.filteredNotifications).toHaveLength(0);
    expect(store.filteredTotalElements).toBe(0);
    expect(store.getTabCount('all')).toBe(0);
    expect(store.getTabCount('unread')).toBe(0);
  });

  it('setActiveTab and setTabCount floor behavior', () => {
    const store = useNotificationStore();
    store.setActiveTab('warning');
    expect(store.activeTab).toBe('warning');

    store.setTabCount('warning', -20);
    expect(store.getTabCount('warning')).toBe(0);
  });

  it('fetchNotifications and fetchTabNotifications mark loaded flags on failure', async () => {
    const store = useNotificationStore();
    (UserNotificationService.getNotifications as any).mockRejectedValue(new Error('fail'));

    await store.fetchNotifications();
    await store.fetchTabNotifications('all');

    expect(store.loaded).toBe(true);
    expect(store.filteredLoaded).toBe(true);
  });

  it('loadMoreNotifications uses totalElements fallback when totalPages is missing', async () => {
    const store = useNotificationStore();
    store.notifications = [makeNotification('n1')] as any;
    store.totalElements = 3 as any;
    store.hasMore = true as any;

    (UserNotificationService.getNotifications as any).mockResolvedValueOnce({
      content: [makeNotification('n2')],
      totalElements: 3,
      totalPages: 0,
    });

    await store.loadMoreNotifications();

    expect(store.notifications.map(n => n.id)).toEqual(['n1', 'n2']);
    expect(store.hasMore).toBe(true);
  });

  it('loadMoreTabNotifications uses totalElements fallback when totalPages is missing', async () => {
    const store = useNotificationStore();
    store.activeTab = 'info' as any;
    store.filteredNotifications = [makeNotification('n1')] as any;
    store.filteredTotalElements = 3 as any;
    store.filteredHasMore = true as any;

    (UserNotificationService.getNotifications as any).mockResolvedValueOnce({
      content: [makeNotification('n2')],
      totalElements: 3,
      totalPages: 0,
    });

    await store.loadMoreTabNotifications();

    expect(store.filteredNotifications.map(n => n.id)).toEqual(['n1', 'n2']);
    expect(store.filteredHasMore).toBe(true);
  });

  it('refresh methods call expected fetch actions', async () => {
    const store = useNotificationStore();
    const fetchNotificationsSpy = vi.spyOn(store, 'fetchNotifications').mockResolvedValue();
    const fetchCountSpy = vi.spyOn(store, 'fetchCount').mockResolvedValue();
    const fetchTabSpy = vi.spyOn(store, 'fetchTabNotifications').mockResolvedValue();
    store.activeTab = 'error' as any;

    await store.refreshNotifications();
    await store.refreshActiveTabNotifications();

    expect(fetchNotificationsSpy).toHaveBeenCalledTimes(1);
    expect(fetchCountSpy).toHaveBeenCalledTimes(2);
    expect(fetchTabSpy).toHaveBeenCalledWith('error');
  });

  it('removeNotificationLocal handles unread active-tab and severity active-tab branches', () => {
    const unread = { ...makeNotification('u1', false), severity: 'WARNING' } as any;
    const errorRead = { ...makeNotification('e1', true), severity: 'ERROR' } as any;

    const store = useNotificationStore();
    store.notifications = [unread, errorRead] as any;
    store.filteredNotifications = [unread, errorRead] as any;
    store.totalElements = 2 as any;
    store.filteredTotalElements = 2 as any;
    store.unreadCount = 1 as any;
    store.tabCounts = {
      all: 2,
      unread: 1,
      read: 1,
      error: 1,
      info: 0,
      success: 0,
      warning: 1,
    } as any;

    store.activeTab = 'unread' as any;
    store.removeNotificationLocal('u1');
    expect(store.unreadCount).toBe(0);
    expect(store.filteredTotalElements).toBe(1);

    store.activeTab = 'error' as any;
    store.removeNotificationLocal('e1');
    expect(store.getTabCount('error')).toBe(0);
    expect(store.getTabCount('read')).toBe(0);
    expect(store.getTabCount('all')).toBe(0);
  });

  it('does not decrement filtered totals when removing a notification outside the active filtered tab', () => {
    const store = useNotificationStore();
    const warningUnread = { ...makeNotification('w1', false), severity: 'WARNING' } as any;
    store.activeTab = 'error' as any;
    store.notifications = [warningUnread] as any;
    store.filteredNotifications = [warningUnread] as any;
    store.totalElements = 1 as any;
    store.filteredTotalElements = 1 as any;
    store.unreadCount = 1 as any;
    store.tabCounts = {
      all: 1,
      unread: 1,
      read: 0,
      error: 0,
      info: 0,
      success: 0,
      warning: 1,
    } as any;

    store.removeNotificationLocal('w1');

    expect(store.filteredNotifications).toHaveLength(0);
    expect(store.filteredTotalElements).toBe(1);
    expect(store.getTabCount('warning')).toBe(0);
    expect(store.getTabCount('all')).toBe(0);
  });

  it('markReadLocal updates filtered list outside unread tab', () => {
    const store = useNotificationStore();
    store.activeTab = 'all' as any;
    store.notifications = [makeNotification('n1', false), makeNotification('n2', false)] as any;
    store.filteredNotifications = [makeNotification('n1', false)] as any;
    store.unreadCount = 2 as any;

    store.markReadLocal(['n1']);

    expect(store.filteredNotifications[0].isRead).toBe(true);
    expect(store.unreadCount).toBe(1);
  });

  it('markNotificationsAsRead applies optimistic update and calls API', async () => {
    (UserNotificationService.markAsRead as any).mockResolvedValueOnce(undefined);
    const store = useNotificationStore();
    store.notifications = [makeNotification('n1', false)] as any;
    store.filteredNotifications = [makeNotification('n1', false)] as any;
    store.unreadCount = 1 as any;

    await store.markNotificationsAsRead(['n1']);

    expect(store.notifications.find(n => n.id === 'n1')?.isRead).toBe(true);
    expect(store.unreadCount).toBe(0);
    expect(UserNotificationService.markAsRead).toHaveBeenCalledWith(['n1']);
  });

  it('markNotificationsAsRead rolls back on API failure', async () => {
    (UserNotificationService.markAsRead as any).mockRejectedValueOnce(new Error('fail'));
    (UserNotificationService.getNotifications as any).mockResolvedValueOnce({
      content: [makeNotification('n1', false)],
      totalElements: 1,
      totalPages: 1,
    });
    (UserNotificationService.getUnreadCount as any).mockResolvedValueOnce({
      allCount: 1,
      unreadCount: 1,
      readCount: 0,
      errorCount: 0,
      infoCount: 1,
      successCount: 0,
      warningCount: 0,
    });
    const store = useNotificationStore();
    store.activeTab = 'all' as any;
    store.notifications = [makeNotification('n1', false)] as any;
    store.filteredNotifications = [makeNotification('n1', false)] as any;
    store.unreadCount = 1 as any;

    await store.markNotificationsAsRead(['n1']);

    expect(UserNotificationService.markAsRead).toHaveBeenCalledWith(['n1']);
    expect(store.unreadCount).toBe(1);
  });

  it('resets all notification collections and pagination state', () => {
    const store = useNotificationStore();
    store.notifications = [makeNotification('n1', false)] as any;
    store.filteredNotifications = [makeNotification('n2', true)] as any;
    store.notificationToasts = [makeNotification('toast-1', false)] as any;
    store.totalElements = 3 as any;
    store.filteredTotalElements = 2 as any;
    store.unreadCount = 2 as any;
    store.hasMore = true as any;
    store.filteredHasMore = true as any;
    store.currentPage = 4 as any;
    store.filteredCurrentPage = 5 as any;
    store.tabCounts = {
      all: 3,
      unread: 2,
      read: 1,
      error: 1,
      info: 1,
      success: 1,
      warning: 0,
    } as any;

    store.removeAllNotificationsLocal();

    expect(store.notifications).toEqual([]);
    expect(store.filteredNotifications).toEqual([]);
    expect(store.totalElements).toBe(0);
    expect(store.filteredTotalElements).toBe(0);
    expect(store.unreadCount).toBe(0);
    expect(store.hasMore).toBe(false);
    expect(store.filteredHasMore).toBe(false);
    expect(store.currentPage).toBe(0);
    expect(store.filteredCurrentPage).toBe(0);
    expect(store.getTabCount('all')).toBe(0);
    expect(store.getTabCount('success')).toBe(0);
  });
});
