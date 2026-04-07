import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/api/core/request', () => ({ request: vi.fn() }));

import { request as coreRequest } from '@/api/core/request';
import { NotificationAdminService } from '@/api/services/NotificationAdminService';
import { UserNotificationService } from '@/api/services/UserNotificationService';
import { UserService } from '@/api/services/UserService';

describe('Notification and User API services', () => {
  beforeEach(() => {
    (coreRequest as any).mockReset?.();
    (coreRequest as any).mockResolvedValue(undefined);
  });

  it('calls all NotificationAdminService endpoints with expected request config', async () => {
    await NotificationAdminService.getEvents();
    await NotificationAdminService.createEvent({ requestBody: { eventKey: 'E1' } as any });
    await NotificationAdminService.deactivateEvent({ id: 'event-1' });

    await NotificationAdminService.getRules();
    await NotificationAdminService.createRule({ requestBody: { eventId: 'event-1' } as any });
    await NotificationAdminService.deleteRule({ id: 'rule-1' });
    await NotificationAdminService.toggleRule({ id: 'rule-1' });
    await NotificationAdminService.createRulesBatch({ requestBody: { rules: [] } as any });

    await NotificationAdminService.getTemplates();
    await NotificationAdminService.createTemplate({ requestBody: { eventId: 'event-1' } as any });

    await NotificationAdminService.getPolicies();
    await NotificationAdminService.createPolicy({ requestBody: { ruleId: 'rule-1' } as any });
    await NotificationAdminService.deletePolicy({ id: 'policy-1' });

    expect(coreRequest).toHaveBeenCalledTimes(13);

    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/management/notifications/events' })
    );
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'POST', url: '/management/notifications/events' })
    );
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'PATCH',
        url: '/management/notifications/rules/{id}/toggle',
        path: { id: 'rule-1' },
      })
    );
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'DELETE',
        url: '/management/notifications/policies/{id}',
        path: { id: 'policy-1' },
      })
    );
  });

  it('calls UserNotificationService endpoints with defaults and payload', async () => {
    await UserNotificationService.getNotifications();
    await UserNotificationService.getUnreadCount();
    await UserNotificationService.markAsRead(['n1', 'n2']);
    await UserNotificationService.markAllAsRead();

    expect(coreRequest).toHaveBeenCalledTimes(4);
    expect(coreRequest).toHaveBeenNthCalledWith(
      1,
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/management/notifications',
        query: {
          page: 0,
          size: 20,
          sort: 'createdDate,desc',
          readFilter: undefined,
          severity: undefined,
        },
      })
    );
    expect(coreRequest).toHaveBeenNthCalledWith(
      2,
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/management/notifications/count' })
    );
    expect(coreRequest).toHaveBeenNthCalledWith(
      3,
      expect.anything(),
      expect.objectContaining({
        method: 'PATCH',
        url: '/management/notifications/read',
        body: { notificationIds: ['n1', 'n2'] },
        mediaType: 'application/json',
      })
    );
    expect(coreRequest).toHaveBeenNthCalledWith(
      4,
      expect.anything(),
      expect.objectContaining({
        method: 'PATCH',
        url: '/management/notifications/read/all',
      })
    );
  });

  it('calls UserNotificationService with custom page and size params', async () => {
    await UserNotificationService.getNotifications(2, 50);
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/management/notifications',
        query: {
          page: 2,
          size: 50,
          sort: 'createdDate,desc',
          readFilter: undefined,
          severity: undefined,
        },
      })
    );
  });

  it('calls UserNotificationService with filter params', async () => {
    await UserNotificationService.getNotifications(0, 20, {
      readFilter: 'UNREAD',
      severity: 'ERROR',
    });
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'GET',
        url: '/management/notifications',
        query: {
          page: 0,
          size: 20,
          sort: 'createdDate,desc',
          readFilter: 'UNREAD',
          severity: 'ERROR',
        },
      })
    );
  });

  it('calls UserNotificationService deleteNotification with correct path', async () => {
    await UserNotificationService.deleteNotification('notif-99');
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'DELETE',
        url: '/management/notifications/{id}',
        path: { id: 'notif-99' },
      })
    );
  });

  it('calls UserNotificationService deleteAllNotifications', async () => {
    await UserNotificationService.deleteAllNotifications();
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        method: 'DELETE',
        url: '/management/notifications',
      })
    );
  });

  it('calls UserService getUsers endpoint', async () => {
    await UserService.getUsers();
    expect(coreRequest).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ method: 'GET', url: '/management/users' })
    );
  });
});
