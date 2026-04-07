import { FieldTransformationType } from './FieldTransformationType';

/**
 * Integration Field Mapping DTO
 * Matches backend com.integration.model.dto.IntegrationFieldMappingDto
 */
export interface IntegrationFieldMappingDto {
  id?: string;
  integrationId?: string;
  sourceFieldPath: string;
  targetFieldPath: string;
  transformationType: FieldTransformationType;
  transformationConfig?: Record<string, unknown>;
  isMandatory?: boolean;
  defaultValue?: string;
  validationRules?: string;
  metadata?: Record<string, unknown>;
  /** Order for displaying this field mapping in UI */
  displayOrder?: number;
}
