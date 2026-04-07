import { describe, expect, it } from 'vitest';

import type { AppNotification } from '@/types/notification';
import {
  applyNotificationFilters,
  formatAbsoluteTime,
  formatRelativeTime,
  getNotificationCreatedBy,
  getPrimaryAction,
  getSeverityClass,
  getSeverityValue,
  localizeMessageTimestamps,
  parseNotificationMessage,
} from '@/utils/notificationDisplay';

const baseNotification: AppNotification = {
  id: '1',
  tenantId: 'tenant-1',
  userId: 'user-1',
  type: 'TASK',
  severity: 'WARNING',
  title: 'Task reminder',
  message: 'Task is due soon',
  isRead: false,
  createdDate: new Date().toISOString(),
};

describe('notificationDisplay utilities', () => {
  it('maps severity values and css class safely', () => {
    expect(getSeverityValue('warning')).toBe('WARNING');
    expect(getSeverityValue('invalid')).toBe('INFO');
    expect(getSeverityClass('ERROR')).toBe('error');
  });

  it('applies read and severity filters together', () => {
    const notifications: AppNotification[] = [
      { ...baseNotification, id: '1', severity: 'WARNING', isRead: false },
      { ...baseNotification, id: '2', severity: 'INFO', isRead: true },
      { ...baseNotification, id: '3', severity: 'WARNING', isRead: true },
    ];

    const filtered = applyNotificationFilters(notifications, 'read', 'WARNING');
    expect(filtered).toHaveLength(1);
    expect(filtered[0].id).toBe('3');
  });

  it('formats relative and absolute time', () => {
    const tenMinutesAgo = new Date(Date.now() - 10 * 60_000).toISOString();
    expect(formatRelativeTime(tenMinutesAgo)).toContain('m ago');
    expect(formatAbsoluteTime(tenMinutesAgo)).not.toBe('');
  });

  it('resolves created by and primary action from metadata', () => {
    const notification: AppNotification = {
      ...baseNotification,
      metadata: {
        createdBy: 'Automation Bot',
        actionLabel: 'Open Task',
        actionUrl: '/tasks/123',
      },
    };

    expect(getNotificationCreatedBy(notification)).toBe('Automation Bot');
    expect(getPrimaryAction(notification)).toEqual({
      label: 'Open Task',
      target: '/tasks/123',
      external: false,
    });
  });

  it('getPrimaryAction marks external URLs correctly and handles missing metadata', () => {
    const externalNotification: AppNotification = {
      ...baseNotification,
      metadata: {
        actionLabel: 'View Report',
        actionUrl: 'https://external.example.com/report',
      },
    };
    expect(getPrimaryAction(externalNotification)).toEqual({
      label: 'View Report',
      target: 'https://external.example.com/report',
      external: true,
    });

    const noMetadata: AppNotification = { ...baseNotification, metadata: undefined };
    expect(getPrimaryAction(noMetadata)).toBeNull();
  });

  it('applyNotificationFilters handles unread and all filters', () => {
    const notifications: AppNotification[] = [
      { ...baseNotification, id: '1', isRead: false },
      { ...baseNotification, id: '2', isRead: true },
    ];
    const unread = applyNotificationFilters(notifications, 'unread', 'all');
    expect(unread.map(n => n.id)).toEqual(['1']);

    const all = applyNotificationFilters(notifications, 'all', 'all');
    expect(all).toHaveLength(2);
  });

  it('formatRelativeTime handles edge cases', () => {
    expect(formatRelativeTime('')).toBe('');
    expect(formatRelativeTime('not-a-date')).toBe('');

    const justNow = new Date(Date.now() - 30_000).toISOString();
    expect(formatRelativeTime(justNow)).toBe('Just now');

    const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60_000).toISOString();
    expect(formatRelativeTime(twoHoursAgo)).toContain('h ago');

    const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60_000).toISOString();
    expect(formatRelativeTime(threeDaysAgo)).toContain('d ago');

    const tenDaysAgo = new Date(Date.now() - 10 * 24 * 60 * 60_000).toISOString();
    // beyond 7 days should fall back to formatted date string
    expect(formatRelativeTime(tenDaysAgo)).not.toBe('');
  });

  it('localizeMessageTimestamps replaces ISO timestamps and is a no-op on plain text', () => {
    const plain = 'No timestamps here';
    expect(localizeMessageTimestamps(plain)).toBe(plain);

    const withTs = 'Updated at 2026-02-27T17:04:51Z by system';
    const result = localizeMessageTimestamps(withTs);
    expect(result).not.toContain('2026-02-27T17:04:51Z');
    expect(result).toContain('Updated at ');
    expect(result).toContain(' by system');
  });

  it('parseNotificationMessage returns plain for non-structured type', () => {
    const result = parseNotificationMessage('Hello world', 'TASK');
    expect(result).toEqual({ type: 'plain', text: 'Hello world' });
  });

  it('parseNotificationMessage returns plain for empty message', () => {
    const result = parseNotificationMessage('');
    expect(result).toEqual({ type: 'plain', text: '' });
  });

  it('parseNotificationMessage parses structured failure message', () => {
    const msg = [
      'Batch processing failed for 2 records.',
      '1. Doc: doc-abc, Location: loc-xyz: Invalid coordinates',
      '2. Doc: doc-def, Location: loc-uvw: Missing field',
    ].join('\n');

    const result = parseNotificationMessage(msg, 'ARCGIS_SYNC_FAILED');
    expect(result.type).toBe('structured');
    if (result.type === 'structured') {
      expect(result.items).toHaveLength(2);
      expect(result.items[0]).toMatchObject({
        index: 1,
        doc: 'doc-abc',
        location: 'loc-xyz',
        reason: 'Invalid coordinates',
      });
      expect(result.summary).toContain('Batch processing failed');
    }
  });

  it('parseNotificationMessage falls back to plain when no items match structured pattern', () => {
    const result = parseNotificationMessage('Some generic failure text', 'ARCGIS_SYNC_FAILED');
    expect(result).toEqual({ type: 'plain', text: 'Some generic failure text' });
  });
});
