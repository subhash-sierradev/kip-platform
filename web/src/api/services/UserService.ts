/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* eslint-disable */
/* tslint:disable */

import type { UserProfileResponse } from '../models/UserProfileResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class UserService {
  public static getUsers(): CancelablePromise<UserProfileResponse[]> {
    return __request(OpenAPI, {
      method: 'GET',
      url: '/management/users',
      errors: { 401: 'Unauthorized', 403: 'Forbidden', 500: 'Internal server error' },
    });
  }
}
