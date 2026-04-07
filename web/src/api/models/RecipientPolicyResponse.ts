/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */

import type { RecipientUserInfo } from './RecipientUserInfo';

export interface RecipientPolicyResponse {
  id?: string;
  tenantId?: string;
  ruleId?: string;
  eventKey?: string;
  recipientType?: string;
  createdDate?: string;
  createdBy?: string;
  lastModifiedDate?: string;
  lastModifiedBy?: string;
  users?: Array<RecipientUserInfo>;
}
