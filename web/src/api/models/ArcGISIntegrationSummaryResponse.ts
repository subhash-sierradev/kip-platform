import { JobExecutionStatus } from '@/types/ArcGISIntegrationExecution';

export interface ArcGISIntegrationSummaryResponse {
  id: string; // UUID
  name: string;
  itemType: string;
  itemSubtype: string;
  itemSubtypeLabel: string;
  dynamicDocumentType: string;
  dynamicDocumentTypeLabel: string | null;

  frequencyPattern: string;
  dailyExecutionInterval: number;
  executionDate: string | null; // ISO date string (yyyy-MM-dd)
  executionTime: string; // HH:mm
  daySchedule: string;
  monthSchedule: string;
  isExecuteOnMonthEnd: boolean;
  cronExpression: string;

  createdDate: string; // ISO-8601 Instant
  createdBy: string;
  lastModifiedDate: string; // ISO-8601 Instant
  lastModifiedBy: string;

  isEnabled: boolean;

  lastAttemptTimeUtc?: string; // ISO-8601 UTC
  lastStatus?: JobExecutionStatus;
  nextRunAtUtc?: string; // ISO-8601 UTC
}
