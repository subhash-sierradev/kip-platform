/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */

import type { NotificationSeverity } from './NotificationSeverity';

export interface CreateNotificationRuleRequest {
  eventId: string;
  severity: NotificationSeverity;
  isEnabled: boolean;
}
