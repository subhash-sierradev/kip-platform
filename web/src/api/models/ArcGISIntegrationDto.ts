export interface ArcGISIntegrationDto {
  id: string;
  name: string;
  normalizedName?: string;
  description?: string;
  itemType: string;
  itemSubtype?: string;
  dynamicDocumentType?: string;
  connectionId: string;
  scheduleId?: string;
  isScheduled?: boolean;
  autoStart?: boolean;
  tenantId?: string;
  isDeleted?: boolean;
  isEnabled?: boolean | null;
  createdBy?: string;
  createdDate?: string;
  lastModifiedBy?: string;
  lastModifiedDate?: string;
  version?: number;
}
