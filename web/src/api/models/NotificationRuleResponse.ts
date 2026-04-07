/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */

import type { NotificationSeverity } from './NotificationSeverity';

export interface NotificationRuleResponse {
  id?: string;
  tenantId?: string;
  integrationId?: string;
  eventId?: string;
  eventKey?: string;
  entityType?: string;
  severity?: NotificationSeverity;
  isEnabled?: boolean;
  createdDate?: string;
  createdBy?: string;
  lastModifiedDate?: string;
  lastModifiedBy?: string;
}
