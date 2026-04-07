/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */

import type { ConnectionDependentItemResponse } from './ConnectionDependentItemResponse';
import type { ServiceType } from './enums';

export type ConnectionDependentsResponse = {
  serviceType?: ServiceType;
  integrations?: Array<ConnectionDependentItemResponse>;
};
