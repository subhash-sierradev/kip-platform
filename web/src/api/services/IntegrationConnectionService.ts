import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
import type { ConnectionDependentsResponse } from '../models/ConnectionDependentsResponse';
import type { ConnectionTestResponse } from '../models/ConnectionTestResponse';
import type { IntegrationConnectionRequest } from '../models/IntegrationConnectionRequest';
import type { IntegrationConnectionResponse } from '../models/IntegrationConnectionResponse';

/**
 * Service for Integration connection management operations including
 * testing connections, creating connections, and retrieving saved connections.
 * Supports multiple service types (Jira, ArcGIS, etc.)
 */
export class IntegrationConnectionService {
  /**
   * Test and create Integration connection with provided credentials.
   * Tests the connection and creates it if successful.
   * @param requestBody Connection request containing base URL and credentials
   * @returns Connection response with test result and created connection details
   */
  public static testAndCreateConnection({
    requestBody,
  }: {
    requestBody: IntegrationConnectionRequest;
  }): CancelablePromise<IntegrationConnectionResponse> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/integrations/connections/test-connection',
      body: requestBody,
      mediaType: 'application/json',
      errors: {
        400: 'Bad Request - Invalid connection parameters',
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        500: 'Internal Server Error - Connection test failed',
      },
    });
  }

  /**
   * Get all saved Integration connections for the current tenant.
   * Optionally filter by service type, e.g. 'JIRA', 'ARCGIS'.
   * @param serviceType Optional service type filter
   * @returns List of saved Integration connections
   */
  public static getAllConnections({
    serviceType,
  }: {
    serviceType?: string;
  } = {}): CancelablePromise<IntegrationConnectionResponse[]> {
    const query: Record<string, any> = {};
    if (serviceType) query.serviceType = serviceType;
    return __request(OpenAPI, {
      method: 'GET',
      url: '/integrations/connections',
      query: Object.keys(query).length ? query : undefined,
      errors: {
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        500: 'Internal Server Error - Failed to retrieve connections',
      },
    });
  }

  /**
   * Get a specific Integration connection by ID.
   * @param connectionId The unique identifier of the connection
   * @returns The requested Integration connection
   */
  public static getConnectionById({
    connectionId,
  }: {
    connectionId: string;
  }): CancelablePromise<IntegrationConnectionResponse> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/integrations/connections/{connectionId}',
      path: {
        connectionId,
      },
      errors: {
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        404: 'Not Found - Connection not found',
        500: 'Internal Server Error - Failed to retrieve connection',
      },
    });
  }

  /**
   * Get dependents (integrations/webhooks) for a connection.
   * Used to block deletion when non-deleted dependents exist.
   */
  public static getConnectionDependents({
    connectionId,
  }: {
    connectionId: string;
  }): CancelablePromise<ConnectionDependentsResponse> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/integrations/connections/{connectionId}/dependents',
      path: {
        connectionId,
      },
      errors: {
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        404: 'Not Found - Connection not found',
        500: 'Internal Server Error - Failed to retrieve dependents',
      },
    });
  }

  /**
   * Update an existing Jira connection.
   * @param connectionId The unique identifier of the connection
   * @param requestBody Updated connection details
   * @returns Updated connection details
   */
  public static updateConnection({
    connectionId,
    requestBody,
  }: {
    connectionId: string;
    requestBody: IntegrationConnectionRequest;
  }): CancelablePromise<IntegrationConnectionResponse> {
    return __request(OpenAPI, {
      method: 'PUT',
      url: '/integrations/connections/{connectionId}',
      path: {
        connectionId,
      },
      body: requestBody,
      mediaType: 'application/json',
      errors: {
        400: 'Bad Request - Invalid connection parameters',
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        404: 'Not Found - Connection not found',
        500: 'Internal Server Error - Failed to update connection',
      },
    });
  }

  /**
   * Delete an Integration connection.
   * @param connectionId The unique identifier of the connection
   * @returns Success confirmation
   */
  public static deleteConnection({
    connectionId,
  }: {
    connectionId: string;
  }): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: '/integrations/connections/{connectionId}',
      path: {
        connectionId,
      },
      errors: {
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        409: 'Conflict - Connection cannot be deleted due to existing dependents',
        404: 'Not Found - Connection not found',
        500: 'Internal Server Error - Failed to delete connection',
      },
    });
  }

  /**
   * Test an existing Integration connection without creating a new one.
   * @param connectionId The unique identifier of the connection to test
   * @returns Connection test result
   */
  public static testExistingConnection({
    connectionId,
  }: {
    connectionId: string;
  }): CancelablePromise<ConnectionTestResponse> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/integrations/connections/{connectionId}/test',
      path: {
        connectionId,
      },
      errors: {
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        404: 'Not Found - Connection not found',
        500: 'Internal Server Error - Connection test failed',
      },
    });
  }

  /**
   * Rotate a connection secret (basic auth password or oauth2 client secret).
   * @param connectionId The unique identifier of the connection
   * @param requestBody The new secret value
   */
  public static rotateConnectionSecret({
    connectionId,
    requestBody,
  }: {
    connectionId: string;
    requestBody: { newSecret: string };
  }): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'PUT',
      url: '/integrations/connections/{connectionId}/secret',
      path: { connectionId },
      body: requestBody,
      mediaType: 'application/json',
      errors: {
        400: 'Bad Request - Invalid request body',
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        404: 'Not Found - Connection not found',
        500: 'Internal Server Error - Failed to rotate connection secret',
      },
    });
  }
}
