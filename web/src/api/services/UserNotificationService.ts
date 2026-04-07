import type {
  AppNotification,
  NotificationCountResponse,
  NotificationTabFilters,
} from '@/types/notification';

import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class UserNotificationService {
  public static getNotifications(
    page = 0,
    size = 20,
    filters?: NotificationTabFilters
  ): CancelablePromise<{
    content: AppNotification[];
    totalElements: number;
    totalPages: number;
  }> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/notifications',
      query: {
        page,
        size,
        sort: 'createdDate,desc',
        readFilter: filters?.readFilter,
        severity: filters?.severity,
      },
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }

  public static getUnreadCount(): CancelablePromise<NotificationCountResponse> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/notifications/count',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }

  public static markAsRead(notificationIds: string[]): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'PATCH',
      url: '/management/notifications/read',
      body: { notificationIds },
      mediaType: 'application/json',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }

  public static markAllAsRead(): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'PATCH',
      url: '/management/notifications/read/all',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }

  public static deleteNotification(notificationId: string): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: '/management/notifications/{id}',
      path: { id: notificationId },
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }

  public static deleteAllNotifications(): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: '/management/notifications',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }
}
