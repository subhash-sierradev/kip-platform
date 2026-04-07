import type { IntegrationScheduleRequest } from './IntegrationScheduleRequest';

export interface ConfluenceIntegrationCreateUpdateRequest {
  name: string;
  description?: string;
  itemType: string;
  itemSubtype: string;
  dynamicDocumentType?: string;
  dynamicDocumentTypeLabel?: string;
  languageCodes: string[];
  reportNameTemplate: string;
  confluenceSpaceKey: string;
  confluenceSpaceKeyFolderKey?: string;
  includeTableOfContents: boolean;
  connectionId: string;
  schedule: IntegrationScheduleRequest;
}
