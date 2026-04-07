/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */

import type { CreateNotificationRuleRequest } from './CreateNotificationRuleRequest';

export interface BatchCreateNotificationRuleRequest {
  rules: Array<CreateNotificationRuleRequest>;
  recipientType: string;
  userIds?: Array<string>;
}
