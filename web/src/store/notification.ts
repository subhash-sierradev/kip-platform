import { defineStore } from 'pinia';

import { UserNotificationService } from '@/api/services/UserNotificationService';
import type {
  AppNotification,
  NotificationCountResponse,
  NotificationTabFilters,
} from '@/types/notification';

const NOTIFICATION_TOAST_DISMISS_MS = 6_000;
const PAGE_SIZE = 20;

export type NotificationFilterTab =
  | 'all'
  | 'unread'
  | 'read'
  | 'error'
  | 'info'
  | 'success'
  | 'warning';

type NotificationTabCounts = Record<NotificationFilterTab, number>;

interface NotificationState {
  unreadCount: number;
  notifications: AppNotification[];
  notificationToasts: AppNotification[];
  loaded: boolean;
  currentPage: number;
  hasMore: boolean;
  totalElements: number;
  loadingMore: boolean;
  tabCounts: NotificationTabCounts;
  activeTab: NotificationFilterTab;
  filteredNotifications: AppNotification[];
  filteredLoaded: boolean;
  filteredCurrentPage: number;
  filteredHasMore: boolean;
  filteredTotalElements: number;
  filteredLoadingMore: boolean;
}

function emptyTabCounts(): NotificationTabCounts {
  return {
    all: 0,
    unread: 0,
    read: 0,
    error: 0,
    info: 0,
    success: 0,
    warning: 0,
  };
}

function getFiltersForTab(tab: NotificationFilterTab): NotificationTabFilters | undefined {
  if (tab === 'all') return undefined;
  if (tab === 'unread') return { readFilter: 'UNREAD' };
  if (tab === 'read') return { readFilter: 'READ' };
  return { severity: tab.toUpperCase() as NotificationTabFilters['severity'] };
}

function mapCountResponseToTabs(response: NotificationCountResponse): NotificationTabCounts {
  return {
    all: response.allCount ?? 0,
    unread: response.unreadCount ?? 0,
    read: response.readCount ?? 0,
    error: response.errorCount ?? 0,
    info: response.infoCount ?? 0,
    success: response.successCount ?? 0,
    warning: response.warningCount ?? 0,
  };
}

function severityToTab(notification: AppNotification): NotificationFilterTab {
  return (notification.severity ?? 'INFO').toLowerCase() as NotificationFilterTab;
}

function matchesTab(notification: AppNotification, tab: NotificationFilterTab): boolean {
  if (tab === 'all') return true;
  if (tab === 'unread') return !notification.isRead;
  if (tab === 'read') return notification.isRead;
  return (notification.severity ?? '').toUpperCase() === tab.toUpperCase();
}

export const useNotificationStore = defineStore('notifications', {
  state: (): NotificationState => ({
    unreadCount: 0,
    notifications: [],
    notificationToasts: [],
    loaded: false,
    currentPage: 0,
    hasMore: false,
    totalElements: 0,
    loadingMore: false,
    tabCounts: emptyTabCounts(),
    activeTab: 'all',
    filteredNotifications: [],
    filteredLoaded: false,
    filteredCurrentPage: 0,
    filteredHasMore: false,
    filteredTotalElements: 0,
    filteredLoadingMore: false,
  }),

  actions: {
    getTabCount(tab: NotificationFilterTab): number {
      return this.tabCounts[tab] ?? 0;
    },

    setActiveTab(tab: NotificationFilterTab): void {
      this.activeTab = tab;
    },

    setTabCount(tab: NotificationFilterTab, count: number): void {
      this.tabCounts = { ...this.tabCounts, [tab]: Math.max(0, count) };
    },

    async fetchCount(): Promise<void> {
      try {
        const response = await UserNotificationService.getUnreadCount();
        this.unreadCount = response.unreadCount ?? 0;
        this.tabCounts = mapCountResponseToTabs(response);
      } catch {
        // silent - next refresh will retry
      }
    },

    async fetchNotifications(): Promise<void> {
      try {
        const page = await UserNotificationService.getNotifications(0, PAGE_SIZE);
        this.notifications = page.content ?? [];
        this.totalElements = page.totalElements ?? this.notifications.length;
        this.currentPage = 0;
        this.hasMore = (page.totalPages ?? 0) > 1;
        this.setTabCount('all', this.totalElements);
      } catch {
        // silent
      } finally {
        this.loaded = true;
      }
    },

    async loadMoreNotifications(): Promise<void> {
      if (this.loadingMore || !this.hasMore) return;

      this.loadingMore = true;
      try {
        const nextPage = this.currentPage + 1;
        const page = await UserNotificationService.getNotifications(nextPage, PAGE_SIZE);
        const totalPages = page.totalPages ?? 0;
        const newItems = (page.content ?? []).filter(
          n => !this.notifications.some(existing => existing.id === n.id)
        );
        this.notifications = [...this.notifications, ...newItems];
        this.currentPage = nextPage;
        this.totalElements = page.totalElements ?? this.notifications.length;
        this.hasMore =
          totalPages > 0
            ? nextPage + 1 < totalPages
            : this.notifications.length < this.totalElements;
        this.setTabCount('all', this.totalElements);
      } catch {
        // silent
      } finally {
        this.loadingMore = false;
      }
    },

    async fetchTabNotifications(tab?: NotificationFilterTab): Promise<void> {
      const selectedTab = tab ?? this.activeTab;
      this.activeTab = selectedTab;
      try {
        const page = await UserNotificationService.getNotifications(
          0,
          PAGE_SIZE,
          getFiltersForTab(selectedTab)
        );
        this.filteredNotifications = page.content ?? [];
        this.filteredTotalElements = page.totalElements ?? this.filteredNotifications.length;
        this.filteredCurrentPage = 0;
        this.filteredHasMore = (page.totalPages ?? 0) > 1;
        this.setTabCount(selectedTab, this.filteredTotalElements);
      } catch {
        // silent
      } finally {
        this.filteredLoaded = true;
      }
    },

    async loadMoreTabNotifications(): Promise<void> {
      if (this.filteredLoadingMore || !this.filteredHasMore) return;

      this.filteredLoadingMore = true;
      try {
        const nextPage = this.filteredCurrentPage + 1;
        const page = await UserNotificationService.getNotifications(
          nextPage,
          PAGE_SIZE,
          getFiltersForTab(this.activeTab)
        );
        const totalPages = page.totalPages ?? 0;
        const newItems = (page.content ?? []).filter(
          n => !this.filteredNotifications.some(existing => existing.id === n.id)
        );
        this.filteredNotifications = [...this.filteredNotifications, ...newItems];
        this.filteredCurrentPage = nextPage;
        this.filteredTotalElements = page.totalElements ?? this.filteredNotifications.length;
        this.filteredHasMore =
          totalPages > 0
            ? nextPage + 1 < totalPages
            : this.filteredNotifications.length < this.filteredTotalElements;
        this.setTabCount(this.activeTab, this.filteredTotalElements);
      } catch {
        // silent
      } finally {
        this.filteredLoadingMore = false;
      }
    },

    async refreshNotifications(): Promise<void> {
      await Promise.all([this.fetchNotifications(), this.fetchCount()]);
    },

    async refreshActiveTabNotifications(): Promise<void> {
      await Promise.all([this.fetchTabNotifications(this.activeTab), this.fetchCount()]);
    },

    addIncoming(notification: AppNotification): void {
      if (this.notifications.some(n => n.id === notification.id)) return;

      const normalizedNotification: AppNotification = {
        ...notification,
        createdDate: notification.createdDate || new Date().toISOString(),
      };

      this.notifications = [normalizedNotification, ...this.notifications];
      this.totalElements++;
      this.setTabCount('all', this.getTabCount('all') + 1);
      this.setTabCount(
        severityToTab(normalizedNotification),
        this.getTabCount(severityToTab(normalizedNotification)) + 1
      );

      if (normalizedNotification.isRead) {
        this.setTabCount('read', this.getTabCount('read') + 1);
      } else {
        this.unreadCount++;
        this.setTabCount('unread', this.getTabCount('unread') + 1);
      }

      if (matchesTab(normalizedNotification, this.activeTab)) {
        this.filteredNotifications = [normalizedNotification, ...this.filteredNotifications];
        this.filteredTotalElements++;
        this.filteredHasMore = this.filteredNotifications.length < this.filteredTotalElements;
      }
    },

    addNotificationToast(notification: AppNotification): void {
      const normalizedNotification: AppNotification = {
        ...notification,
        createdDate: notification.createdDate || new Date().toISOString(),
      };
      this.notificationToasts = [normalizedNotification, ...this.notificationToasts];
      setTimeout(
        () => this.removeNotificationToast(notification.id),
        NOTIFICATION_TOAST_DISMISS_MS
      );
    },

    removeNotificationToast(id: string): void {
      this.notificationToasts = this.notificationToasts.filter(toast => toast.id !== id);
    },

    markReadLocal(ids: string[]): void {
      const idSet = new Set(ids);
      let decremented = 0;

      this.notifications = this.notifications.map(item => {
        if (idSet.has(item.id) && !item.isRead) {
          decremented++;
          return { ...item, isRead: true };
        }
        return item;
      });

      if (decremented > 0) {
        this.unreadCount = Math.max(0, this.unreadCount - decremented);
        this.setTabCount('unread', this.getTabCount('unread') - decremented);
        this.setTabCount('read', this.getTabCount('read') + decremented);
      }

      if (this.activeTab === 'unread') {
        let removedFromFiltered = 0;
        this.filteredNotifications = this.filteredNotifications.filter(item => {
          if (idSet.has(item.id)) {
            removedFromFiltered++;
            return false;
          }
          return true;
        });
        if (removedFromFiltered > 0) {
          this.filteredTotalElements = Math.max(
            0,
            this.filteredTotalElements - removedFromFiltered
          );
          this.filteredHasMore = this.filteredNotifications.length < this.filteredTotalElements;
        }
        return;
      }

      this.filteredNotifications = this.filteredNotifications.map(item =>
        idSet.has(item.id) ? { ...item, isRead: true } : item
      );
    },

    async markNotificationsAsRead(ids: string[]): Promise<void> {
      this.markReadLocal(ids);
      try {
        await UserNotificationService.markAsRead(ids);
      } catch {
        await this.refreshActiveTabNotifications();
      }
    },

    markAllReadLocal(): void {
      const markedUnreadCount = this.notifications.reduce(
        (count, item) => (!item.isRead ? count + 1 : count),
        0
      );

      this.notifications = this.notifications.map(item => ({ ...item, isRead: true }));
      this.filteredNotifications = this.filteredNotifications.map(item => ({
        ...item,
        isRead: true,
      }));

      if (markedUnreadCount === 0) return;

      this.unreadCount = 0;
      this.setTabCount('unread', this.getTabCount('unread') - markedUnreadCount);
      this.setTabCount('read', this.getTabCount('read') + markedUnreadCount);

      if (this.activeTab === 'unread') {
        this.filteredNotifications = [];
        this.filteredTotalElements = 0;
        this.filteredHasMore = false;
      }
    },

    removeNotificationLocal(id: string): void {
      const notification = this.notifications.find(item => item.id === id);
      if (!notification) return;

      this.notifications = this.notifications.filter(item => item.id !== id);

      const notificationSeverityTab = severityToTab(notification);
      const matchesActiveTab =
        this.activeTab === 'all' ||
        (this.activeTab === 'unread' && !notification.isRead) ||
        (this.activeTab === 'read' && notification.isRead) ||
        this.activeTab === notificationSeverityTab;

      const wasInFiltered = this.filteredNotifications.some(item => item.id === id);
      this.filteredNotifications = this.filteredNotifications.filter(item => item.id !== id);

      this.totalElements = Math.max(0, this.totalElements - 1);
      this.hasMore = this.notifications.length < this.totalElements;

      if (matchesActiveTab && wasInFiltered) {
        this.filteredTotalElements = Math.max(0, this.filteredTotalElements - 1);
        this.filteredHasMore = this.filteredNotifications.length < this.filteredTotalElements;
      }

      this.setTabCount('all', this.getTabCount('all') - 1);
      this.setTabCount(
        severityToTab(notification),
        this.getTabCount(severityToTab(notification)) - 1
      );

      if (notification.isRead) {
        this.setTabCount('read', this.getTabCount('read') - 1);
      } else {
        this.unreadCount = Math.max(0, this.unreadCount - 1);
        this.setTabCount('unread', this.getTabCount('unread') - 1);
      }
    },

    removeAllNotificationsLocal(): void {
      this.notifications = [];
      this.filteredNotifications = [];
      this.totalElements = 0;
      this.filteredTotalElements = 0;
      this.unreadCount = 0;
      this.hasMore = false;
      this.filteredHasMore = false;
      this.currentPage = 0;
      this.filteredCurrentPage = 0;
      this.tabCounts = emptyTabCounts();
    },
  },
});
