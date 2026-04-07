/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */

import type { AuditResult } from './AuditResult';
import type { EntityType } from './EntityType';

export interface AuditLogResponse {
  id?: string;
  entityType?: EntityType;
  entityId?: string;
  entityName?: string;
  action?: string;
  result?: AuditResult;
  performedBy?: string;
  tenantId?: string;
  clientIpAddress?: string;
  timestamp?: string;
  details?: string;
}
