export interface AppNotification {
  id: string;
  tenantId: string;
  userId: string;
  type: string;
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';
  title: string;
  message: string;
  metadata?: Record<string, unknown>;
  isRead: boolean;
  createdDate: string;
}

export type NotificationReadFilter = 'ALL' | 'UNREAD' | 'READ';
export type NotificationSeverityFilter = 'INFO' | 'WARNING' | 'ERROR' | 'SUCCESS';

export interface NotificationTabFilters {
  readFilter?: NotificationReadFilter;
  severity?: NotificationSeverityFilter;
}

export interface NotificationCountResponse {
  allCount: number;
  unreadCount: number;
  readCount: number;
  errorCount: number;
  infoCount: number;
  successCount: number;
  warningCount: number;
}
