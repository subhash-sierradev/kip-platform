import { IntegrationScheduleResponse } from './IntegrationScheduleResponse';

export interface ArcGISIntegrationResponse {
  id: string; // UUID
  name: string;
  normalizedName: string;
  description: string;
  itemType: string;
  itemSubtype: string;
  itemSubtypeLabel: string;
  dynamicDocumentType: string;
  dynamicDocumentTypeLabel: string | null;

  connectionId: string; // UUID
  schedule: IntegrationScheduleResponse;

  tenantId: string;
  isEnabled: boolean;

  createdBy: string;
  createdDate: string; // ISO-8601 Instant
  lastModifiedBy: string;
  lastModifiedDate: string; // ISO-8601 Instant
  nextRunAtUtc?: string; // ISO-8601 Instant (UTC) - computed for enabled integrations

  version: number;
}
