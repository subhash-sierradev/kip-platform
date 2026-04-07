export type JobExecutionStatus = 'SCHEDULED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'ABORTED';

/**
 * Optional execution summary fields that may be provided by the backend.
 * Note: The backend only exposes flat properties; there is no nested `execution`
 * object in the API response. This type is aligned with the actual API contract.
 */
export interface ArcGISIntegrationExecutionSummary {
  // Nested execution block (preferred)
  execution?: {
    lastAttemptTimeUtc?: string; // ISO-8601 UTC
    lastSuccessTimeUtc?: string; // ISO-8601 UTC
    lastStatus?: JobExecutionStatus;
    nextRunAtUtc?: string; // ISO-8601 UTC
  };

  // Flat properties (compatibility)
  lastAttemptTimeUtc?: string; // ISO-8601 UTC
  lastSuccessTimeUtc?: string; // ISO-8601 UTC
  lastStatus?: JobExecutionStatus;
  nextRunAtUtc?: string; // ISO-8601 UTC
}
