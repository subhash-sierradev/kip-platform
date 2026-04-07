import type { ConnectionStatus } from './IntegrationConnection';

/**
 * Response DTO for Integration connection operations - matches backend model
 */
export interface IntegrationConnectionResponse {
  // Entity identifiers
  id?: string;
  tenantId?: string;
  version?: number;

  // Core connection fields
  name?: string;
  secretName?: string; // Updated from connectionKey
  serviceType?: string;
  fetchMode?: string;
  isDeleted?: boolean;
  lastConnectionStatus?: ConnectionStatus;
  lastConnectionMessage?: string;
  lastConnectionTest?: string; // LocalDateTime as ISO string

  // Audit fields
  createdBy?: string;
  createdDate?: string; // Instant as ISO string
  lastModifiedBy?: string;
  lastModifiedDate?: string; // Instant as ISO string

  // UI helpers
  isDuplicate?: boolean; // Flag for potential duplicates
}
