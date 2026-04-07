import type { IntegrationSecret } from './IntegrationSecret';

/**
 * Integration Connection entity - generic connection for multiple service types
 */
export interface IntegrationConnection {
  /**
   * Unique identifier for the connection
   */
  id?: string;

  /**
   * Service type (JIRA, ARCGIS, etc.)
   */
  serviceType?: string;

  /**
   * Base URL for the service
   */
  baseUrl: string;

  /**
   * Integration secret containing authentication details
   */
  integrationSecret: IntegrationSecret;

  /**
   * Organization key
   */
  organizationKey?: string;

  /**
   * Connection status
   */
  status?: ConnectionStatus;

  /**
   * Display name for the connection
   */
  displayName?: string;

  /**
   * Server title from Jira
   */
  serverTitle?: string;

  /**
   * Last test timestamp
   */
  lastTestedAt?: string;

  /**
   * Fetch mode for HTTP requests (GET or POST)
   */
  fetchMode?: FetchMode;

  /**
   * Created timestamp
   */
  createdAt?: string;

  /**
   * Updated timestamp
   */
  updatedAt?: string;

  /**
   * Created by user
   */
  createdBy?: string;

  /**
   * Updated by user
   */
  updatedBy?: string;

  /**
   * Version for optimistic locking
   */
  version?: number;
}

/**
 * Connection status enumeration
 */
export enum ConnectionStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  ERROR = 'ERROR',
}

/**
 * Fetch mode enumeration for HTTP request methods
 */
export enum FetchMode {
  GET = 'GET',
  POST = 'POST',
}
