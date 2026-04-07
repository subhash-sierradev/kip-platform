/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* eslint-disable */
/* tslint:disable */

import type { BatchCreateNotificationRuleRequest } from '../models/BatchCreateNotificationRuleRequest';
import type { NotificationEventCatalogResponse } from '../models/NotificationEventCatalogResponse';
import type { NotificationRuleResponse } from '../models/NotificationRuleResponse';
import type { NotificationTemplateResponse } from '../models/NotificationTemplateResponse';
import type { CreateEventCatalogRequest } from '../models/CreateEventCatalogRequest';
import type { CreateNotificationRuleRequest } from '../models/CreateNotificationRuleRequest';
import type { CreateNotificationTemplateRequest } from '../models/CreateNotificationTemplateRequest';
import type { RecipientPolicyResponse } from '../models/RecipientPolicyResponse';
import type { CreateRecipientPolicyRequest } from '../models/CreateRecipientPolicyRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class NotificationAdminService {
  public static getEvents(): CancelablePromise<NotificationEventCatalogResponse[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/notifications/events',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }

  public static createEvent({
    requestBody,
  }: {
    requestBody: CreateEventCatalogRequest;
  }): CancelablePromise<NotificationEventCatalogResponse> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/management/notifications/events',
      body: requestBody,
      errors: { 400: 'Bad request', 401: 'Unauthorized', 500: 'Internal server error' },
    });
  }

  public static deactivateEvent({ id }: { id: string }): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: '/management/notifications/events/{id}',
      path: { id },
      errors: { 401: 'Unauthorized', 404: 'Not found', 500: 'Internal server error' },
    });
  }

  public static getRules(): CancelablePromise<NotificationRuleResponse[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/notifications/rules',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }

  public static createRule({
    requestBody,
  }: {
    requestBody: CreateNotificationRuleRequest;
  }): CancelablePromise<NotificationRuleResponse> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/management/notifications/rules',
      body: requestBody,
      errors: { 400: 'Bad request', 401: 'Unauthorized', 500: 'Internal server error' },
    });
  }

  public static deleteRule({ id }: { id: string }): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: '/management/notifications/rules/{id}',
      path: { id },
      errors: { 401: 'Unauthorized', 404: 'Not found', 500: 'Internal server error' },
    });
  }

  public static updateRule({
    id,
    requestBody,
  }: {
    id: string;
    requestBody: { severity: string };
  }): CancelablePromise<NotificationRuleResponse> {
    return __request(OpenAPI, {
      method: 'PUT',
      url: '/management/notifications/rules/{id}',
      path: { id },
      body: requestBody,
      errors: {
        400: 'Bad request',
        401: 'Unauthorized',
        404: 'Not found',
        500: 'Internal server error',
      },
    });
  }

  public static toggleRule({ id }: { id: string }): CancelablePromise<NotificationRuleResponse> {
    return __request(OpenAPI, {
      method: 'PATCH',
      url: '/management/notifications/rules/{id}/toggle',
      path: { id },
      errors: { 401: 'Unauthorized', 404: 'Not found', 500: 'Internal server error' },
    });
  }

  public static createRulesBatch({
    requestBody,
  }: {
    requestBody: BatchCreateNotificationRuleRequest;
  }): CancelablePromise<NotificationRuleResponse[]> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/management/notifications/rules/batch',
      body: requestBody,
      errors: { 400: 'Bad request', 401: 'Unauthorized', 500: 'Internal server error' },
    });
  }

  public static getTemplates(): CancelablePromise<NotificationTemplateResponse[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/notifications/templates',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }

  public static createTemplate({
    requestBody,
  }: {
    requestBody: CreateNotificationTemplateRequest;
  }): CancelablePromise<NotificationTemplateResponse> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/management/notifications/templates',
      body: requestBody,
      errors: { 400: 'Bad request', 401: 'Unauthorized', 500: 'Internal server error' },
    });
  }

  public static getPolicies(): CancelablePromise<RecipientPolicyResponse[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/notifications/policies',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }

  public static createPolicy({
    requestBody,
  }: {
    requestBody: CreateRecipientPolicyRequest;
  }): CancelablePromise<RecipientPolicyResponse> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/management/notifications/policies',
      body: requestBody,
      errors: { 400: 'Bad request', 401: 'Unauthorized', 500: 'Internal server error' },
    });
  }

  public static updatePolicy({
    id,
    requestBody,
  }: {
    id: string;
    requestBody: CreateRecipientPolicyRequest;
  }): CancelablePromise<RecipientPolicyResponse> {
    return __request(OpenAPI, {
      method: 'PUT',
      url: '/management/notifications/policies/{id}',
      path: { id },
      body: requestBody,
      errors: {
        400: 'Bad request',
        401: 'Unauthorized',
        404: 'Not found',
        500: 'Internal server error',
      },
    });
  }

  public static deletePolicy({ id }: { id: string }): CancelablePromise<void> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: '/management/notifications/policies/{id}',
      path: { id },
      errors: { 401: 'Unauthorized', 404: 'Not found', 500: 'Internal server error' },
    });
  }

  public static resetRulesToDefaults(): CancelablePromise<{
    message: string;
    rulesRestored: number;
  }> {
    return __request(OpenAPI, {
      method: 'POST',
      url: '/management/notifications/rules/reset-defaults',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }
}
