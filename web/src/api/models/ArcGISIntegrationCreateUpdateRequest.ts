import type { IntegrationFieldMappingDto } from './IntegrationFieldMappingDto';
import type { IntegrationScheduleRequest } from './IntegrationScheduleRequest';

/**
 * ArcGIS Integration Create/Update Request DTO
 * Matches backend com.integration.model.dto.request.ArcGISIntegrationCreateUpdateRequest
 */
export interface ArcGISIntegrationCreateUpdateRequest {
  name: string;
  description?: string;
  itemType: string;
  itemSubtype: string;
  dynamicDocumentType?: string;
  connectionId: string;
  schedule: IntegrationScheduleRequest;
  fieldMappings: IntegrationFieldMappingDto[];
}
