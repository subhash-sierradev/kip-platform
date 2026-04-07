import { OpenAPI } from '@/api/core/OpenAPI';
import { request as __request } from '@/api/core/request';

/**
 * API response interface for credential type data
 */
export interface CredentialTypeResponse {
  credentialAuthType: string;
  displayName: string;
  isEnabled: boolean;
  requiredFields: string[];
}

/**
 * Processed credential type for dropdown usage
 */
export interface CredentialTypeOption {
  value: string;
  label: string;
  description: string;
  isEnabled: boolean;
  requiredFields: string[];
  code: string; // Original code from API response
}

/**
 * Service for managing credential types from the backend API.
 * Provides endpoints for fetching dynamic dropdown options for authentication methods.
 */
export class CredentialTypeService {
  /**
   * Fetch all credential types from the backend API.
   * Transforms the response into a format suitable for dropdown consumption.
   * @returns Promise with array of credential types for dropdown options
   */
  public static getCredentialTypes(): Promise<CredentialTypeOption[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/credential-types',
      errors: {
        400: 'Bad Request - Invalid parameters',
        401: 'Unauthorized - Authentication required',
        403: 'Forbidden - Access denied',
        404: 'Not Found - Endpoint not available',
        500: 'Internal Server Error - Failed to fetch credential types',
      },
    }).then((response: any) => {
      // Transform the response for dropdown usage
      const data = response as CredentialTypeResponse[];
      return data.map(item => ({
        value: item.credentialAuthType,
        label: item.displayName,
        description: item.displayName,
        isEnabled: item.isEnabled,
        requiredFields: item.requiredFields,
        code: item.credentialAuthType, // Use credentialAuthType as code
      }));
    });
  }

  /**
   * Get enabled credential types only
   * @returns Promise with array of enabled credential types
   */
  public static getEnabledCredentialTypes(): Promise<CredentialTypeOption[]> {
    return this.getCredentialTypes().then(types => types.filter(type => type.isEnabled));
  }
}
