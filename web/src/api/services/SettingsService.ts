/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* eslint-disable */
/* tslint:disable */

import type { CacheStat } from '@/types/CacheTypes';
import type { AuditLogResponse } from '../models/AuditLogResponse';
import type { SiteConfig } from '../models/SiteConfig';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
import { IntegrationConnectionResponse } from '../models/IntegrationConnectionResponse';

export class SettingsService {
  /**
   * Clear all caches
   * Clear all caches in the system.
   * @returns string All caches cleared
   * @throws ApiError
   */
  public static clearAllCaches(): CancelablePromise<string> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: '/management/caches',
      errors: {
        401: `Unauthorized`,
        500: `Internal server error`,
      },
    });
  }

  /**
   * Clear cache by name
   * Clear a specific cache by name.
   * @returns string Cache cleared
   * @throws ApiError
   */
  public static clearCacheByName({ cacheName }: { cacheName: string }): CancelablePromise<string> {
    return __request(OpenAPI, {
      method: 'DELETE',
      url: '/management/caches/{cacheName}',
      path: {
        cacheName: cacheName,
      },
      errors: {
        401: `Unauthorized`,
        404: `Cache not found`,
        500: `Internal server error`,
      },
    });
  }

  /**
   * Get all cache stats
   * Retrieve stats for all caches.
   * @returns Record<string, CacheStat> An object mapping cache names to their statistics (size, hitRate, missRate, requestCount)
   * @throws ApiError
   */
  public static getAllCacheStats(): CancelablePromise<Record<string, CacheStat>> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/caches/stats',
      errors: {
        401: `Unauthorized`,
        500: `Internal server error`,
      },
    });
  }

  /**
   * List site configs
   * Retrieve all site-wide configuration entries.
   * @returns SiteConfig List of site configs
   * @throws ApiError
   */
  public static listSiteConfigs(): CancelablePromise<Array<SiteConfig>> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/site-configs',
      errors: {
        401: `Unauthorized`,
        500: `Internal server error`,
      },
    });
  }

  /**
   * List audit logs
   * Retrieve all audit logs for the current tenant.
   * @returns AuditLogResponse List of audit logs
   * @throws ApiError
   */
  public static listAuditLogs(): CancelablePromise<Array<AuditLogResponse>> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/audits/logs',
      errors: {
        401: `Unauthorized`,
        500: `Internal server error`,
      },
    });
  }

  /**
   * List audit logs by tenant
   * Retrieve all audit logs for a specific tenant.
   * @returns AuditLogResponse List of audit logs
   * @throws ApiError
   */
  public static listAuditLogsByTenant({
    tenantId,
  }: {
    tenantId: string;
  }): CancelablePromise<Array<AuditLogResponse>> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/audits/logs/tenants/{tenantId}',
      path: {
        tenantId: tenantId,
      },
      errors: {
        401: `Unauthorized`,
        500: `Internal server error`,
      },
    });
  }

  /**
   * List tenants with audit logs
   * Retrieve all tenant IDs that have audit logs.
   * @returns string List of tenant IDs
   * @throws ApiError
   */
  public static listTenantsWithAuditLogs(): CancelablePromise<Array<string>> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/audits/tenants',
      errors: {
        401: `Unauthorized`,
        500: `Internal server error`,
      },
    });
  }

  /**
   * Get all tenants
   * List all tenant IDs. Available only for super admins.
   * @returns string List of tenant IDs
   * @throws ApiError
   */
  public static listAllTenants(): CancelablePromise<Array<string>> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/tenants',
      errors: {
        401: `Unauthorized`,
        403: `Forbidden - Super admin access required`,
        500: `Internal server error`,
      },
    });
  }

  /**
   * Get all saved Jira connections.
   * @returns List of saved Jira connections
   */
  public static findAllConnections(): CancelablePromise<IntegrationConnectionResponse[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/connections/jira',
      errors: {
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        500: 'Internal Server Error - Failed to retrieve connections',
      },
    });
  }

  /**
   * Get all saved Jira connections for the current tenant.
   * @returns List of saved Jira connections by tenant
   */
  public static findAllConnectionsByTenant({
    tenantId,
  }: {
    tenantId: string;
  }): CancelablePromise<IntegrationConnectionResponse[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/connections/jira/tenants/{tenantId}',
      path: {
        tenantId: tenantId,
      },
      errors: {
        401: 'Unauthorized - Invalid authentication credentials',
        403: 'Forbidden - Access denied',
        500: 'Internal Server Error - Failed to retrieve connections',
      },
    });
  }
}
