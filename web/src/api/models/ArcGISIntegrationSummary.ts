export interface ArcGISIntegrationSummary {
  id: string;
  name: string;
  normalizedName?: string;
  description?: string;
  itemType: string;
  itemSubtype?: string;
  dynamicDocumentType?: string;
  connectionId: string;
  scheduleId?: string;
  isEnabled?: boolean;
  createdBy?: string;
  createdDate?: string; // ISO date-time
  lastModifiedBy?: string;
  lastModifiedDate?: string; // ISO date-time
  version?: number;
  scheduleName?: string;
  scheduleExpression?: string;
}
