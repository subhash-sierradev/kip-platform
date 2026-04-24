import { ROUTES } from '@/router/routes';
import type { AppNotification } from '@/types/notification';

export type ReadFilter = 'all' | 'unread' | 'read';
export type SeverityFilter = 'all' | 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR';

interface NotificationAction {
  label: string;
  target: string;
  external: boolean;
}

const SEVERITY_VALUES = new Set(['INFO', 'SUCCESS', 'WARNING', 'ERROR']);

function normalizeMetadataValue(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

export function getSeverityValue(severity: string | undefined): Exclude<SeverityFilter, 'all'> {
  const normalized = (severity ?? '').toUpperCase();
  if (SEVERITY_VALUES.has(normalized)) {
    return normalized as Exclude<SeverityFilter, 'all'>;
  }
  return 'INFO';
}

export function getSeverityClass(severity: string | undefined): string {
  return getSeverityValue(severity).toLowerCase();
}

export function applyNotificationFilters(
  notifications: AppNotification[],
  readFilter: ReadFilter,
  severityFilter: SeverityFilter
): AppNotification[] {
  return notifications.filter(notification => {
    const matchesRead =
      readFilter === 'all'
        ? true
        : readFilter === 'unread'
          ? !notification.isRead
          : notification.isRead;

    const matchesSeverity =
      severityFilter === 'all' ? true : getSeverityValue(notification.severity) === severityFilter;

    return matchesRead && matchesSeverity;
  });
}

export function formatRelativeTime(isoDate: string): string {
  if (!isoDate) return '';
  const timestamp = new Date(isoDate).getTime();
  if (Number.isNaN(timestamp)) return '';

  const differenceMs = Date.now() - timestamp;
  const minutes = Math.floor(differenceMs / 60_000);
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;

  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;

  return new Date(timestamp).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

/**
 * Finds all ISO UTC timestamps embedded in a free-text message and replaces
 * them with the user's local timezone equivalent (same format as formatAbsoluteTime).
 * Handles: 2026-02-27T17:04:51Z  /  2026-02-27T17:04:51.054684200Z  etc.
 */
export function localizeMessageTimestamps(message: string): string {
  if (!message) return message;
  return message.replace(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z/g, utcStr =>
    formatAbsoluteTime(utcStr)
  );
}

export interface ParsedErrorItem {
  index: number;
  doc: string;
  location: string;
  reason: string;
}

export type ParsedMessage =
  | { type: 'plain'; text: string }
  | { type: 'structured'; summary: string; items: ParsedErrorItem[] };

/**
 * Notification types that can produce a structured (numbered-item) message body.
 * Any type whose suffix matches one of these will attempt structured parsing.
 */
const STRUCTURED_TYPE_SUFFIXES = ['_FAILED', '_FAILURE'];

function isStructuredType(notificationType: string | undefined): boolean {
  if (!notificationType) return false;
  const upper = notificationType.toUpperCase();
  return STRUCTURED_TYPE_SUFFIXES.some(suffix => upper.endsWith(suffix));
}

/**
 * Parses a notification message body into a plain string or a structured list
 * of error-record items (e.g. ArcGIS transformation failures).
 *
 * Only attempts structured parsing when `notificationType` ends in `_FAILED`
 * (or another known structured suffix). For all other types the message is
 * returned as plain text after timestamp localization.
 *
 * Detects lines matching:
 *   N. Doc: <id>, Location: <id>: <reason>
 */
export function parseNotificationMessage(
  message: string,
  notificationType?: string
): ParsedMessage {
  if (!message) return { type: 'plain', text: '' };

  const localized = localizeMessageTimestamps(message);

  if (!isStructuredType(notificationType)) {
    return { type: 'plain', text: localized };
  }

  const lines = localized.split('\n');
  const items: ParsedErrorItem[] = [];
  const summaryLines: string[] = [];

  for (const line of lines) {
    const m = line.match(/^(\d+)\.\s+Doc:\s*([^,]+),\s*Location:\s*([^:]+):\s*(.+)$/);
    if (m) {
      items.push({
        index: parseInt(m[1], 10),
        doc: m[2].trim(),
        location: m[3].trim(),
        reason: m[4].trim(),
      });
    } else {
      summaryLines.push(line);
    }
  }

  if (items.length === 0) {
    return { type: 'plain', text: localized };
  }

  return {
    type: 'structured',
    summary: summaryLines.join('\n').trim(),
    items,
  };
}

export function formatAbsoluteTime(isoDate: string): string {
  if (!isoDate) return '';
  const timestamp = new Date(isoDate).getTime();
  if (Number.isNaN(timestamp)) return '';

  return new Date(timestamp).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function getNotificationCreatedBy(notification: AppNotification): string {
  const metadata = notification.metadata as Record<string, unknown> | undefined;
  const candidates = [
    metadata?.createdBy,
    metadata?.createdByUser,
    metadata?.triggeredBy,
    metadata?.updatedBy,
    metadata?.deletedBy,
    metadata?.enabledBy,
    metadata?.disabledBy,
    metadata?.actor,
    metadata?.user,
  ];

  for (const candidate of candidates) {
    const normalized = normalizeMetadataValue(candidate);
    if (normalized) return normalized;
  }

  return 'System';
}

export function getPrimaryAction(notification: AppNotification): NotificationAction | null {
  const metadata = notification.metadata as Record<string, unknown> | undefined;
  if (!metadata) return null;

  const label =
    normalizeMetadataValue(metadata.actionLabel) ||
    normalizeMetadataValue(metadata.ctaLabel) ||
    normalizeMetadataValue(metadata.buttonText);

  const explicitTarget =
    normalizeMetadataValue(metadata.actionUrl) ||
    normalizeMetadataValue(metadata.url) ||
    normalizeMetadataValue(metadata.targetUrl) ||
    normalizeMetadataValue(metadata.route);

  if (label && explicitTarget) {
    return { label, target: explicitTarget, external: /^https?:\/\//i.test(explicitTarget) };
  }

  // Derive route from semantic metadata fields so the frontend owns all paths
  const derived = deriveActionFromMetadata(metadata, notification.type);
  if (!derived) return null;

  // Allow an explicit label override even when the target is derived
  return {
    label: label || derived.label,
    target: derived.target,
    external: false,
  };
}

function isValidUuid(value: string): boolean {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);
}

function resolveConnectionAction(
  metadata: Record<string, unknown>
): { target: string; label: string } | null {
  const connectionId = normalizeMetadataValue(metadata.connectionId);
  if (!connectionId) return null;

  const serviceType = normalizeMetadataValue(metadata.serviceType).toUpperCase();
  if (serviceType === 'ARCGIS') {
    return { target: ROUTES.arcgisConnection, label: 'Open ArcGIS Connection' };
  }
  if (serviceType === 'JIRA') {
    return { target: ROUTES.jiraConnection, label: 'Open Jira Connection' };
  }
  if (serviceType === 'CONFLUENCE') {
    return { target: ROUTES.confluenceConnection, label: 'Open Confluence Connection' };
  }
  return null;
}

function deriveActionFromMetadata(
  metadata: Record<string, unknown>,
  eventType: string | undefined
): { target: string; label: string } | null {
  // Deleted entities no longer exist — no navigation link
  if (eventType?.endsWith('_DELETED')) return null;

  const integrationId = normalizeMetadataValue(metadata.integrationId);
  if (integrationId && isValidUuid(integrationId)) {
    const isConfluence = eventType?.startsWith('CONFLUENCE_') ?? false;
    return isConfluence
      ? {
          target: ROUTES.confluenceIntegrationDetails(integrationId),
          label: 'Open Confluence Integration',
        }
      : {
          target: ROUTES.arcgisIntegrationDetails(integrationId),
          label: 'Open ArcGIS Integration',
        };
  }

  const webhookId = normalizeMetadataValue(metadata.webhookId);
  if (webhookId) {
    return { target: ROUTES.jiraWebhookDetails(webhookId), label: 'Open Jira Webhook' };
  }

  return resolveConnectionAction(metadata);
}
