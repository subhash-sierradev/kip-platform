import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
import { KwItemSubtypeDto } from '../models';
import type { KwDocField } from '../models/KwDocField';
import type { KwDynamicDocType } from '../models/KwDynamicDocType';

export class KwDocService {
  public static getDynamicDocuments(
    type: string,
    subType: string
  ): CancelablePromise<KwDynamicDocType[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/kw/dynamic-documents-types',
      query: {
        type,
        subType,
      },
      errors: {
        400: 'Bad Request - Invalid parameters',
        401: 'Unauthorized - Authentication required',
        403: 'Forbidden - Access denied',
        404: 'Not Found - Endpoint not available',
        500: 'Internal Server Error - Failed to fetch dynamic documents',
      },
    }).then((response: any) => {
      return response as KwDynamicDocType[];
    }) as CancelablePromise<KwDynamicDocType[]>;
  }

  public static getSubItemTypes(): CancelablePromise<KwItemSubtypeDto[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/kw/item-subtypes',
      errors: {
        400: 'Bad Request - Invalid parameters',
        401: 'Unauthorized - Authentication required',
        403: 'Forbidden - Access denied',
        404: 'Not Found - Endpoint not available',
        500: 'Internal Server Error - Failed to fetch sub-item types',
      },
    }).then((response: any) => {
      return response as KwItemSubtypeDto[];
    }) as CancelablePromise<KwItemSubtypeDto[]>;
  }

  /**
   * Get source field mappings for Kaseware document fields
   * @returns Promise with array of field mapping data
   */
  public static getSourceFieldMappings(): CancelablePromise<KwDocField[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/kw/source-field-mappings',
      errors: {
        400: 'Bad Request - Invalid parameters',
        401: 'Unauthorized - Authentication required',
        403: 'Forbidden - Access denied',
        404: 'Not Found - Endpoint not available',
        500: 'Internal Server Error - Failed to fetch source field mappings',
      },
    }).then((response: any) => {
      return response as KwDocField[];
    }) as CancelablePromise<KwDocField[]>;
  }
}
