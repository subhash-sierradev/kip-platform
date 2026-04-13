import { flushPromises, mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import NotificationsAllPage from '@/components/notifications/NotificationsAllPage.vue';
import { useNotificationStore } from '@/store/notification';
import type { AppNotification, NotificationTabFilters } from '@/types/notification';

const notificationServiceMocks = vi.hoisted(() => ({
  getUnreadCountMock: vi.fn(),
  getNotificationsMock: vi.fn(),
  markAsReadMock: vi.fn(),
  markAllAsReadMock: vi.fn(),
  deleteNotificationMock: vi.fn(),
  deleteAllNotificationsMock: vi.fn(),
}));

vi.mock('@/api/services/UserNotificationService', () => ({
  UserNotificationService: {
    getUnreadCount: notificationServiceMocks.getUnreadCountMock,
    getNotifications: notificationServiceMocks.getNotificationsMock,
    markAsRead: notificationServiceMocks.markAsReadMock,
    markAllAsRead: notificationServiceMocks.markAllAsReadMock,
    deleteNotification: notificationServiceMocks.deleteNotificationMock,
    deleteAllNotifications: notificationServiceMocks.deleteAllNotificationsMock,
  },
}));

vi.mock('devextreme-vue/button', () => ({
  DxButton: {
    name: 'DxButton',
    props: ['text', 'disabled', 'icon'],
    template:
      '<button class="dx-button" :disabled="disabled" @click="$emit(\'click\')">{{ text || icon || "icon" }}</button>',
  },
}));

function createNotification(id: string, overrides: Partial<AppNotification> = {}): AppNotification {
  return {
    id,
    tenantId: 'tenant-1',
    userId: 'user-1',
    type: 'INTEGRATION_CONNECTION_CREATED',
    severity: 'INFO',
    title: `Notification ${id}`,
    message: `Message ${id}`,
    metadata: {},
    isRead: false,
    createdDate: new Date('2026-04-10T10:00:00.000Z').toISOString(),
    ...overrides,
  };
}

const allNotifications = [
  createNotification('all-1', { severity: 'ERROR', title: 'Error one' }),
  createNotification('all-2', { isRead: true, severity: 'SUCCESS', title: 'Success two' }),
];

const unreadNotifications = [
  createNotification('unread-1', { severity: 'ERROR', title: 'Unread one' }),
  createNotification('unread-2', { severity: 'WARNING', title: 'Unread two' }),
];

const errorNotificationsPageOne = [
  createNotification('error-1', { severity: 'ERROR', title: 'Error page one' }),
];

const errorNotificationsPageTwo = [
  createNotification('error-1', { severity: 'ERROR', title: 'Error page one' }),
  createNotification('error-2', { severity: 'ERROR', title: 'Error page two' }),
];

const errorNotificationsPageThree = [
  createNotification('error-3', { severity: 'ERROR', title: 'Error page three' }),
];

function buildNotificationsResponse(
  page: number,
  filters?: NotificationTabFilters
): { content: AppNotification[]; totalElements: number; totalPages: number } {
  if (!filters) {
    return {
      content: page === 0 ? allNotifications : [],
      totalElements: 2,
      totalPages: 1,
    };
  }

  if (filters.readFilter === 'UNREAD') {
    return {
      content: page === 0 ? unreadNotifications : [],
      totalElements: 2,
      totalPages: 1,
    };
  }

  if (filters.severity === 'ERROR') {
    if (page === 0) {
      return {
        content: errorNotificationsPageOne,
        totalElements: 3,
        totalPages: 1,
      };
    }
    if (page === 1) {
      return {
        content: errorNotificationsPageTwo,
        totalElements: 3,
        totalPages: 0,
      };
    }
    return {
      content: errorNotificationsPageThree,
      totalElements: 3,
      totalPages: 0,
    };
  }

  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
  };
}

async function mountPage() {
  const pinia = createPinia();
  setActivePinia(pinia);

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>home</div>' } },
      { path: '/notifications', component: NotificationsAllPage },
      { path: '/target/details', component: { template: '<div>target-details</div>' } },
    ],
  });

  await router.push('/notifications');
  await router.isReady();

  const wrapper = mount(NotificationsAllPage, {
    global: {
      plugins: [pinia, router],
    },
    attachTo: document.body,
  });

  await flushPromises();
  return { wrapper, router, store: useNotificationStore() };
}

describe('NotificationsAllPage integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    Element.prototype.scrollIntoView = vi.fn();
    notificationServiceMocks.getUnreadCountMock.mockResolvedValue({
      allCount: 2,
      unreadCount: 2,
      readCount: 0,
      errorCount: 1,
      infoCount: 0,
      successCount: 1,
      warningCount: 1,
    });
    notificationServiceMocks.getNotificationsMock.mockImplementation(
      async (page: number, _size: number, filters?: NotificationTabFilters) =>
        buildNotificationsResponse(page, filters)
    );
    notificationServiceMocks.markAsReadMock.mockResolvedValue(undefined);
    notificationServiceMocks.markAllAsReadMock.mockResolvedValue(undefined);
    notificationServiceMocks.deleteNotificationMock.mockResolvedValue(undefined);
    notificationServiceMocks.deleteAllNotificationsMock.mockResolvedValue(undefined);
  });

  it('loads unread notifications, marks the previous unread item on selection change, and flushes on unmount', async () => {
    const { wrapper, store } = await mountPage();

    const unreadTab = wrapper
      .findAll('.filter-tab')
      .find(button => button.text().includes('Unread'));
    expect(unreadTab).toBeDefined();

    await unreadTab!.trigger('click');
    await flushPromises();

    expect(notificationServiceMocks.getNotificationsMock).toHaveBeenCalledWith(0, 20, {
      readFilter: 'UNREAD',
    });
    expect(store.filteredNotifications.map(notification => notification.id)).toEqual([
      'unread-1',
      'unread-2',
    ]);

    const cards = wrapper.findAll('.notification-card');
    expect(cards).toHaveLength(2);

    await cards[0].trigger('click');
    await flushPromises();
    await cards[1].trigger('click');
    await flushPromises();

    expect(notificationServiceMocks.markAsReadMock).toHaveBeenCalledWith(['unread-1']);
    expect(store.filteredNotifications.map(notification => notification.id)).toEqual(['unread-2']);

    wrapper.unmount();
    await flushPromises();

    expect(notificationServiceMocks.markAsReadMock).toHaveBeenLastCalledWith(['unread-2']);
  });

  it('uses the real notification store for incoming items and paginated filtered loads without duplicates', async () => {
    const { wrapper, store } = await mountPage();

    const errorTab = wrapper.findAll('.filter-tab').find(button => button.text().includes('Error'));
    expect(errorTab).toBeDefined();

    await errorTab!.trigger('click');
    await flushPromises();

    expect(store.activeTab).toBe('error');
    expect(store.filteredNotifications.map(notification => notification.id)).toEqual(['error-1']);

    store.addIncoming(
      createNotification('incoming-error', {
        severity: 'ERROR',
        title: 'Incoming error',
        type: 'INTEGRATION_FAILED',
      })
    );
    store.addIncoming(
      createNotification('incoming-read', {
        severity: 'INFO',
        isRead: true,
        title: 'Incoming read',
      })
    );

    expect(store.unreadCount).toBe(3);
    expect(store.getTabCount('all')).toBe(4);
    expect(store.getTabCount('error')).toBe(4);
    expect(store.getTabCount('read')).toBe(1);
    expect(store.filteredNotifications[0]?.id).toBe('incoming-error');

    store.filteredHasMore = true;
    store.filteredCurrentPage = 0;
    store.filteredTotalElements = 3;
    await store.loadMoreTabNotifications();
    expect(store.filteredNotifications.map(notification => notification.id)).toEqual([
      'incoming-error',
      'error-1',
      'error-2',
    ]);
    expect(store.filteredHasMore).toBe(false);

    store.hasMore = true;
    store.currentPage = 0;
    store.totalElements = 3;
    notificationServiceMocks.getNotificationsMock.mockImplementationOnce(async (page: number) => ({
      content:
        page === 1
          ? [
              allNotifications[1],
              createNotification('all-3', { isRead: true, title: 'Read three' }),
            ]
          : [],
      totalElements: 3,
      totalPages: 0,
    }));

    await store.loadMoreNotifications();

    expect(store.notifications.map(notification => notification.id)).toEqual([
      'incoming-read',
      'incoming-error',
      'all-2',
      'all-3',
    ]);
    expect(store.hasMore).toBe(false);
  });

  it('refreshes store-backed data when mark-all and delete-all API requests fail', async () => {
    notificationServiceMocks.markAllAsReadMock.mockRejectedValueOnce(new Error('mark all failed'));
    notificationServiceMocks.deleteAllNotificationsMock.mockRejectedValueOnce(
      new Error('delete all failed')
    );

    const { wrapper, store } = await mountPage();
    const refreshSpy = vi.spyOn(store, 'refreshActiveTabNotifications');

    const markAllButton = wrapper
      .findAll('.dx-button')
      .find(button => button.text().includes('Mark all read'));
    const deleteAllButton = wrapper
      .findAll('.dx-button')
      .find(button => button.text().includes('Delete all'));

    expect(markAllButton).toBeDefined();
    expect(deleteAllButton).toBeDefined();

    await markAllButton!.trigger('click');
    await flushPromises();
    await deleteAllButton!.trigger('click');
    await flushPromises();

    expect(notificationServiceMocks.markAllAsReadMock).toHaveBeenCalled();
    expect(notificationServiceMocks.deleteAllNotificationsMock).toHaveBeenCalledTimes(1);
    expect(refreshSpy).toHaveBeenCalledTimes(2);
  });
});
