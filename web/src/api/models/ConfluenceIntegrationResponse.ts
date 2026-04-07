import type { IntegrationScheduleResponse } from './IntegrationScheduleResponse';
import type { LanguageDto } from './LanguageDto';

export interface ConfluenceIntegrationResponse {
  id: string;
  name: string;
  normalizedName: string;
  description?: string;
  itemType: string;
  itemSubtype: string;
  itemSubtypeLabel?: string | null;
  dynamicDocumentType?: string | null;
  dynamicDocumentTypeLabel?: string | null;
  languages: LanguageDto[];
  languageCodes: string[];
  reportNameTemplate: string;
  confluenceSpaceKey: string;
  confluenceSpaceLabel?: string | null;
  confluenceSpaceKeyFolderKey?: string | null;
  confluenceSpaceFolderLabel?: string | null;
  includeTableOfContents: boolean;
  connectionId: string;
  connectionName?: string | null;
  schedule: IntegrationScheduleResponse;
  tenantId: string;
  isEnabled: boolean;
  createdBy: string;
  createdDate: string;
  lastModifiedBy: string;
  lastModifiedDate: string;
  nextRunAtUtc?: string;
  version: number;
}
