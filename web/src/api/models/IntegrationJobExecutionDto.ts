export interface IntegrationJobExecutionDto {
  id: string;
  scheduleId: string;
  originalJobId?: string;
  retryAttempt?: number;

  triggeredBy: 'SCHEDULER' | 'MANUAL' | 'RETRY' | 'API';

  windowStart?: string;
  windowEnd?: string;

  status: 'RUNNING' | 'SUCCESS' | 'FAILED' | 'ABORTED';

  startedAt: string;
  completedAt?: string;

  lastAttemptTime: string;
  lastSuccessTime?: string;

  addedRecords: number;
  updatedRecords: number;
  failedRecords: number;
  totalRecords?: number;

  errorMessage?: string;
  triggeredByUser?: string;
  executionMetadata?: Record<string, string>;
}
