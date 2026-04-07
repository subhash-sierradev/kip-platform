import type { JiraWebhook } from '@/types/JiraWebhook';

import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
import { api } from '../OpenAPI';

export interface CustomFieldMapping {
  _id: string;
  jiraFieldKey: string;
  jiraFieldLabel?: string;
  type?:
    | 'string'
    | 'number'
    | 'option'
    | 'array'
    | 'datetime'
    | 'date'
    | 'user'
    | 'multiuser'
    | 'url'
    | 'boolean'
    | 'object';
  required?: boolean;
  valueSource: 'literal' | 'json';
  value: string;
}

export interface JiraFieldMapping {
  id?: string;
  jiraFieldId: string;
  jiraFieldName: string;
  displayLabel: string | null;
  dataType: string;
  template: string;
  required: boolean;
  defaultValue: string | null;
  metadata: any;
}

export interface WebhookExecution {
  id: string;
  tenantId: string;
  triggeredBy: string;
  triggeredAt: string;
  webhookId: string;
  incomingPayload: string;
  transformedPayload: string;
  responseStatusCode: number;
  responseBody: string;
  status: string;
  retryAttempt: number;
  originalEventId: string;
  jiraIssueUrl?: string;
}

export interface JiraWebhookDetail {
  id: string;
  name: string;
  description: string;
  webhookUrl: string;
  connectionId: string;
  jiraFieldMappings: JiraFieldMapping[];
  samplePayload: string;
  isEnabled: boolean;
  isDeleted: boolean;
  normalizedName: string;
  createdBy: string;
  createdDate: string;
  lastModifiedBy: string;
  lastModifiedDate: string;
  tenantId: string;
  version: number;
}

export class JiraWebhookService {
  /**
   * Get webhook details by ID
   */
  static async getWebhookById(webhookId: string): Promise<JiraWebhookDetail> {
    try {
      const response = await api.get(`/webhooks/jira/${webhookId}`);
      return response.data;
    } catch (error: any) {
      console.error('API Error:', error.response?.status, error.response?.data);
      throw new Error(error.response?.data?.message || `Failed to fetch webhook: ${error.message}`);
    }
  }

  /**
   * Test webhook functionality
   */
  static async testWebhook(
    webhookId: string,
    samplePayload?: string
  ): Promise<{
    success: boolean;
    responseCode?: number;
    responseBody?: string;
    errorMessage?: string;
  }> {
    try {
      return await this.executeWebhookTest(webhookId, samplePayload || '');
    } catch (error: any) {
      return this.handleTestError(error);
    }
  }

  /**
   * Create error response for test webhook
   */
  private static createErrorResponse(message: string): {
    success: boolean;
    errorMessage: string;
  } {
    return {
      success: false,
      errorMessage: message,
    };
  }

  /**
   * Handle test webhook errors
   */
  private static handleTestError(error: any): {
    success: boolean;
    responseCode: number;
    errorMessage: string;
  } {
    console.error('Test webhook error:', error.response?.status, error.response?.data);
    return {
      success: false,
      responseCode: error?.response?.status ?? 500,
      errorMessage: error?.response?.data?.message || error.message || 'Webhook execution failed',
    };
  }

  /**
   * Execute the webhook test with payload
   */
  private static async executeWebhookTest(
    webhookId: string,
    payload: string
  ): Promise<{
    success: boolean;
    responseCode: number;
    responseBody: string;
  }> {
    await api.post(`/webhooks/jira/execute/${webhookId}`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });

    return {
      success: true,
      responseCode: 200,
      responseBody:
        'Webhook executed successfully with sample payload. Check webhook history for processing details.',
    };
  }

  /**
   * Get webhook execution history
   */
  static async getWebhookExecutions(webhookId: string): Promise<WebhookExecution[]> {
    try {
      const response = await api.get(`/webhooks/jira/triggers/${webhookId}`);
      return response.data;
    } catch (error: any) {
      console.error('Get executions error:', error.response?.status, error.response?.data);
      throw new Error(
        error.response?.data?.message || `Failed to fetch executions: ${error.message}`
      );
    }
  }

  /**
   * List Jira webhooks
   * @returns JiraWebhook[]
   */
  public static listJiraWebhooks(): Promise<JiraWebhook[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/webhooks/jira',
      headers: {
        // X-TENANT-ID and Authorization are set globally or via interceptors
      },
    });
  }

  /**
   * Delete Jira webhook by ID
   * @param id webhook ID
   */
  public static deleteJiraWebhook(id: string): Promise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: `/webhooks/jira/${id}`,
      headers: {
        // X-TENANT-ID and Authorization are set globally or via interceptors
      },
    });
  }

  /**
   * Create Jira webhook
   */
  public static async createWebhook(requestBody: {
    name: string;
    connectionId: string;
    description?: string;
    fieldsMapping: JiraFieldMapping[];
    samplePayload: string;
  }): Promise<{ name: string; webhookUrl: string }> {
    try {
      const response = await api.post('/webhooks/jira', requestBody);
      return response.data;
    } catch (error: any) {
      console.error('Create webhook error:', error.response?.status, error.response?.data);
      throw new Error(
        error.response?.data?.message || `Failed to create webhook: ${error.message}`
      );
    }
  }

  /**
   * Update Jira webhook
   */
  public static async updateWebhook(
    id: string,
    requestBody: {
      name: string;
      connectionId: string;
      description?: string;
      fieldsMapping: JiraFieldMapping[];
      samplePayload: string;
    }
  ): Promise<void> {
    try {
      await api.put(`/webhooks/jira/${id}`, requestBody);
    } catch (error: any) {
      console.error('Update webhook error:', error.response?.status, error.response?.data);
      throw new Error(
        error.response?.data?.message || `Failed to update webhook: ${error.message}`
      );
    }
  }

  /**
   * Toggle webhook active status
   * Enable or disable the webhook by its ID.
   */
  public static toggleWebhookActive({ id }: { id: string }): Promise<boolean> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/webhooks/jira/{id}/active',
      path: {
        id,
      },
      errors: {
        401: 'Unauthorized',
        404: 'Webhook not found',
        500: 'Internal server error',
      },
    });
  }

  /**
   * Retry a failed webhook event
   */
  public static retryWebhookEvent({ eventId }: { eventId: string }): Promise<void> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/webhooks/jira/triggers/retry/{eventId}',
      path: {
        eventId,
      },
      errors: {
        400: 'Invalid request',
        401: 'Unauthorized',
        404: 'Event not found',
        500: 'Internal server error',
      },
    });
  }

  /**
   * Check if a webhook execution can be retried
   */
  public static canRetryExecution(execution: WebhookExecution): boolean {
    return execution.status === 'FAILED';
  }

  public static getAllJiraNormalizedNames(): Promise<string[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/webhooks/jira/normalized/names',
    });
  }
}
