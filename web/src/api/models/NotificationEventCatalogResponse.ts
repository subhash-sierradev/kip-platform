/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */

import type { NotificationEntityType } from './NotificationEntityType';

export interface NotificationEventCatalogResponse {
  id?: string;
  eventKey?: string;
  entityType?: NotificationEntityType;
  displayName?: string;
  description?: string;
  isEnabled?: boolean;
  createdDate?: string;
}
