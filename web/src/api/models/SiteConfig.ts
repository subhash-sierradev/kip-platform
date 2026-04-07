/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */

/* tslint:disable */

export type ConfigType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON' | 'TIMESTAMP';

export interface SiteConfig {
  id: string;
  configKey: string;
  configValue: string;
  type: ConfigType;
  description?: string;
  createdDate: string;
  lastModifiedDate: string;
  createdBy: string;
  lastModifiedBy: string;
  tenantId: string;
  version: number;
  isDeleted: boolean;
}
