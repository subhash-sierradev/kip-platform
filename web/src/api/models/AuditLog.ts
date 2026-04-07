/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */

import type { EntityType } from './EntityType';

export interface AuditLog {
  id?: string;
  entityType?: EntityType;
  entityId?: string;
  action?: string;
  performedBy?: string;
  tenantId?: string;
  timestamp?: string;
}
