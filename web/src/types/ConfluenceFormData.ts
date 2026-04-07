export interface ConfluenceFormData {
  // Step 1 — Basic Details
  name: string;
  description: string;
  // Step 2 — Document Configuration
  itemType: string;
  subType: string;
  subTypeLabel?: string;
  dynamicDocument?: string;
  dynamicDocumentLabel?: string;
  languageCodes: string[];
  reportNameTemplate: string;
  includeTableOfContents: boolean;
  // Step 3 — Schedule
  executionDate: string | null;
  executionTime: string;
  frequencyPattern: string;
  dailyFrequency: string;
  selectedDays: string[];
  selectedMonths: number[];
  isExecuteOnMonthEnd: boolean;
  cronExpression?: string;
  businessTimeZone?: string;
  timeCalculationMode?: 'FIXED_DAY_BOUNDARY' | 'FLEXIBLE_INTERVAL';
  // Step 4 — Confluence Configuration
  confluenceSpaceKey: string;
  confluenceSpaceLabel?: string;
  confluenceSpaceKeyFolderKey?: string;
  confluenceSpaceFolderLabel?: string;
  // Step 5 — Connection
  connectionMethod?: 'new' | 'existing';
  existingConnectionId?: string;
  createdConnectionId?: string;
  connectionName?: string;
  username: string;
  password: string;
}

export type ConfluenceBasicDetailsData = Pick<ConfluenceFormData, 'name' | 'description'>;

export type ConfluenceDocumentConfigData = Pick<
  ConfluenceFormData,
  'itemType' | 'subType' | 'subTypeLabel' | 'dynamicDocument' | 'dynamicDocumentLabel'
>;

export type ConfluenceScheduleData = Pick<
  ConfluenceFormData,
  | 'executionDate'
  | 'executionTime'
  | 'frequencyPattern'
  | 'dailyFrequency'
  | 'selectedDays'
  | 'selectedMonths'
  | 'isExecuteOnMonthEnd'
  | 'cronExpression'
  | 'businessTimeZone'
  | 'timeCalculationMode'
>;

export type ConfluencePageConfigData = Pick<
  ConfluenceFormData,
  | 'confluenceSpaceKey'
  | 'confluenceSpaceLabel'
  | 'confluenceSpaceKeyFolderKey'
  | 'confluenceSpaceFolderLabel'
  | 'languageCodes'
  | 'reportNameTemplate'
  | 'includeTableOfContents'
>;

export type ConfluenceConnectionData = Pick<
  ConfluenceFormData,
  | 'connectionMethod'
  | 'existingConnectionId'
  | 'createdConnectionId'
  | 'connectionName'
  | 'username'
  | 'password'
>;
