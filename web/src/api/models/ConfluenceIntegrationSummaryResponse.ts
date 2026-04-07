import type { JobExecutionStatus } from '@/types/ArcGISIntegrationExecution';

export interface ConfluenceIntegrationSummaryResponse {
  id: string;
  name: string;
  itemType: string;
  itemSubtype: string;
  itemSubtypeLabel?: string | null;
  dynamicDocumentType?: string | null;
  dynamicDocumentTypeLabel?: string | null;
  languageCodes: string[];
  confluenceSpaceKey: string;
  reportNameTemplate: string;
  frequencyPattern: string;
  executionTime: string;
  createdDate: string;
  createdBy: string;
  lastModifiedDate: string;
  lastModifiedBy: string;
  isEnabled: boolean;
  lastAttemptTimeUtc?: string;
  lastStatus?: JobExecutionStatus;
  nextRunAtUtc?: string;
}
