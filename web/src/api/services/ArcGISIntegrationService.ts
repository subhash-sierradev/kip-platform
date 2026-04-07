import type { ArcGISFeatureField } from '@/api/models/ArcGISFeatureField';
import type { ArcGISIntegrationCreateUpdateRequest } from '@/api/models/ArcGISIntegrationCreateUpdateRequest';
import type { ArcGISIntegrationDto } from '@/api/models/ArcGISIntegrationDto';
import type { ArcGISIntegrationResponse } from '@/api/models/ArcGISIntegrationResponse';
import type { ArcGISIntegrationSummaryResponse } from '@/api/models/ArcGISIntegrationSummaryResponse';
import type { CreationResponse } from '@/api/models/CreationResponse';

import type { ApiError } from '../core/ApiError';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
import type { IntegrationFieldMappingDto } from '../models/IntegrationFieldMappingDto';
import type { IntegrationJobExecutionDto } from '../models/IntegrationJobExecutionDto';

export class ArcGISIntegrationService {
  public static listArcGISIntegrations(): Promise<ArcGISIntegrationSummaryResponse[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/integrations/arcgis',
      headers: {
        // X-TENANT-ID and Authorization are set globally or via interceptors
      },
    });
  }

  /**
   * List ArcGIS feature fields for the configured layer/feature service.
   * Returns raw field metadata; UI should use the `name` for dropdown.
   */
  public static listFeatureFields(connectionId: string): Promise<ArcGISFeatureField[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: `/integrations/arcgis/connections/${connectionId}/features`,
    });
  }

  public static async getArcGISIntegrationById(id: string): Promise<ArcGISIntegrationResponse> {
    try {
      const resp = await __request(OpenAPI, {
        method: 'GET',
        url: `/integrations/arcgis/${id}`,
      });
      return resp as ArcGISIntegrationResponse;
    } catch (err: unknown) {
      const apiErr = err as Partial<ApiError>;
      let message = 'Failed to load integration details. Please try again.';
      if (typeof apiErr.status === 'number') {
        switch (apiErr.status) {
          case 403:
            message = 'Access denied. You may not have permission to view this integration.';
            break;
          case 404:
            message = 'Integration not found. It may have been deleted or the ID is incorrect.';
            break;
          default:
            message = apiErr.message || message;
        }
      } else if ((err as { message?: string }).message) {
        message = (err as { message?: string }).message as string;
      }
      throw new Error(message);
    }
  }

  /**
   * Get job execution history for a specific ArcGIS integration
   */
  public static getJobHistory(integrationId: string): Promise<IntegrationJobExecutionDto[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: `/integrations/arcgis/${integrationId}/executions`,
    });
  }

  /**
   * Get field mappings for a specific ArcGIS integration
   */
  public static getFieldMappings(integrationId: string): Promise<IntegrationFieldMappingDto[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: `/integrations/arcgis/${integrationId}/field-mappings`,
    });
  }

  /**
   * Manually trigger job execution for a specific ArcGIS integration
   */
  public static triggerJobExecution(integrationId: string): Promise<void> {
    return __request(OpenAPI, {
      method: 'POST',
      url: `/integrations/arcgis/${integrationId}/trigger`,
    });
  }

  /**
   * Enable/Disable ArcGIS integration
   */
  public static toggleIntegrationStatus(integrationId: string, enabled: boolean): Promise<void> {
    return __request(OpenAPI, {
      method: 'POST',
      url: `/integrations/arcgis/${integrationId}/status`,
      body: { enabled },
    });
  }

  /**
   * Delete ArcGIS integration
   */
  public static deleteIntegration(integrationId: string): Promise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: `/integrations/arcgis/${integrationId}`,
    });
  }

  /**
   * Toggle ArcGIS integration active status
   */
  public static toggleArcGISIntegrationActive(integrationId: string): Promise<boolean> {
    return __request(OpenAPI, {
      method: 'POST',
      url: `/integrations/arcgis/${integrationId}/toggle`,
    });
  }

  /**
   * Create ArcGIS integration
   */
  public static createIntegration(
    requestBody: ArcGISIntegrationCreateUpdateRequest
  ): Promise<CreationResponse> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/integrations/arcgis',
      body: requestBody,
      mediaType: 'application/json',
    });
  }

  /**
   * Update ArcGIS integration
   */
  public static updateIntegration(
    integrationId: string,
    requestBody: ArcGISIntegrationCreateUpdateRequest
  ): Promise<void> {
    return __request(OpenAPI, {
      method: 'PUT',
      url: `/integrations/arcgis/${integrationId}`,
      body: requestBody,
      mediaType: 'application/json',
    });
  }

  public static getAllArcGISNormalizedNames(): Promise<string[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/integrations/arcgis/normalized/names',
    });
  }
}

// Type guards
function isRecord(obj: unknown): obj is Record<string, unknown> {
  return typeof obj === 'object' && obj !== null;
}

function isArcGISIntegrationDto(obj: unknown): obj is ArcGISIntegrationDto {
  if (!isRecord(obj)) return false;
  return (
    typeof obj.id === 'string' &&
    typeof obj.name === 'string' &&
    typeof obj.itemType === 'string' &&
    typeof obj.connectionId === 'string'
  );
}
