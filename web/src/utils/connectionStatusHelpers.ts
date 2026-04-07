/**
 * Helper utilities for connection status display
 */

import type { IntegrationConnectionResponse } from '@/api/models/IntegrationConnectionResponse';
import type { SavedConnection } from '@/types/ConnectionStepData';

/**
 * Get connection status details for display
 */
export function getConnectionStatus(connection: SavedConnection) {
  if (!connection.lastConnectionStatus) {
    return { label: 'Not Tested', severity: 'neutral' as const };
  }

  switch (connection.lastConnectionStatus.toUpperCase()) {
    case 'SUCCESS':
      return { label: 'Active', severity: 'success' as const };
    case 'FAILED':
      return { label: 'Failed', severity: 'error' as const };
    default:
      return { label: connection.lastConnectionStatus, severity: 'info' as const };
  }
}

/**
 * Format last tested date for display
 */
export function formatLastTested(dateString?: string): string {
  if (!dateString) return 'Never tested';

  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    return date.toLocaleDateString();
  } catch {
    return 'Unknown';
  }
}

/**
 * Transform API connection response to SavedConnection format
 */
export function transformToSavedConnection(conn: IntegrationConnectionResponse): SavedConnection {
  return {
    id: conn.id ?? '',
    name: conn.name ?? '',
    secretName: conn.secretName,
    baseUrl: '', // Not available in response - would need separate call
    lastConnectionStatus: conn.lastConnectionStatus ?? 'UNKNOWN',
    lastConnectionTest: conn.lastConnectionTest,
  };
}
