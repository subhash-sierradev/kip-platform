import type { ConfluenceIntegrationCreateUpdateRequest } from '@/api/models/ConfluenceIntegrationCreateUpdateRequest';
import type { ConfluenceIntegrationResponse } from '@/api/models/ConfluenceIntegrationResponse';
import type { ConfluenceIntegrationSummaryResponse } from '@/api/models/ConfluenceIntegrationSummaryResponse';
import type { CreationResponse } from '@/api/models/CreationResponse';
import type { IntegrationJobExecutionDto } from '@/api/models/IntegrationJobExecutionDto';
import { getToken, getUserInfo } from '@/config/keycloak';

import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export interface ConfluenceSpaceDto {
  key: string;
  name: string;
  type?: string;
  description?: string;
}

export interface ConfluencePageDto {
  id: string;
  title: string;
  parentTitle?: string;
  type?: string;
}

export class ConfluenceIntegrationService {
  private static getAuthHeaders(): Record<string, string> {
    const headers: Record<string, string> = {};
    const token = getToken();
    const user = getUserInfo();

    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
    if (user?.tenantId) {
      headers['X-TENANT-ID'] = user.tenantId;
    }
    if (user?.userId) {
      headers['X-USER-ID'] = user.userId;
    }

    return headers;
  }

  public static listConfluenceIntegrations(): Promise<ConfluenceIntegrationSummaryResponse[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/integrations/confluence',
    });
  }

  public static getConfluenceIntegrationById(id: string): Promise<ConfluenceIntegrationResponse> {
    return __request(OpenAPI, {
      method: 'GET',
      url: `/integrations/confluence/${id}`,
    });
  }

  public static createIntegration(
    requestBody: ConfluenceIntegrationCreateUpdateRequest
  ): Promise<CreationResponse> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/integrations/confluence',
      body: requestBody,
      mediaType: 'application/json',
    });
  }

  public static updateIntegration(
    id: string,
    requestBody: ConfluenceIntegrationCreateUpdateRequest
  ): Promise<void> {
    return __request(OpenAPI, {
      method: 'PUT',
      url: `/integrations/confluence/${id}`,
      body: requestBody,
      mediaType: 'application/json',
    });
  }

  public static deleteIntegration(id: string): Promise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: `/integrations/confluence/${id}`,
    });
  }

  public static getJobHistory(integrationId: string): Promise<IntegrationJobExecutionDto[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: `/integrations/confluence/${integrationId}/executions`,
      headers: this.getAuthHeaders(),
    });
  }

  public static triggerJobExecution(integrationId: string): Promise<void> {
    return __request(OpenAPI, {
      method: 'POST',
      url: `/integrations/confluence/${integrationId}/trigger`,
    });
  }

  public static retryJobExecution(integrationId: string, originalJobId: string): Promise<void> {
    return __request(OpenAPI, {
      method: 'POST',
      url: `/integrations/confluence/${integrationId}/executions/${originalJobId}/retry`,
      headers: this.getAuthHeaders(),
    });
  }

  public static toggleIntegrationStatus(integrationId: string): Promise<boolean> {
    return __request(OpenAPI, {
      method: 'POST',
      url: `/integrations/confluence/${integrationId}/toggle`,
    });
  }

  public static getAllNormalizedNames(): Promise<string[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/integrations/confluence/normalized/names',
    });
  }

  public static getSpacesByConnectionId(connectionId: string): Promise<ConfluenceSpaceDto[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: `/integrations/confluence/connections/${connectionId}/spaces`,
    });
  }

  public static getPagesByConnectionIdAndSpace(
    connectionId: string,
    spaceKey: string
  ): Promise<ConfluencePageDto[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: `/integrations/confluence/connections/${connectionId}/spaces/${spaceKey}/pages`,
    });
  }
}
