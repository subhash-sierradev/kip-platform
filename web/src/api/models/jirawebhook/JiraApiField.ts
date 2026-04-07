import { JiraApiFieldAllowedValue } from './JiraApiFieldAllowedValue';

export interface JiraApiField {
  key?: string;
  id?: string;
  name?: string;
  custom?: boolean;
  allowedValues?: Array<JiraApiFieldAllowedValue | string>;
  schema?: string;
  schemaDetails?: {
    type?: string;
    custom?: string;
  };
}
