export interface ArcGISFormData {
  connectionName?: string;
  name: string;
  description: string;
  itemType: string;
  subType: string;
  subTypeLabel?: string;
  dynamicDocument?: string;
  dynamicDocumentLabel?: string;
  selectedDocumentId: string;
  executionDate: string | null;
  executionTime: string;
  frequencyPattern: string;
  dailyFrequency: string;
  selectedDays?: string[];
  selectedMonths?: number[];
  monthlyPattern?: string;
  monthlyWeek?: string;
  monthlyWeekday?: string;
  monthlyDay?: number | null;
  isExecuteOnMonthEnd?: boolean;
  cronExpression?: string;
  businessTimeZone?: string;
  timeCalculationMode?: 'FIXED_DAY_BOUNDARY' | 'FLEXIBLE_INTERVAL';

  arcgisEndpoint: string;
  fetchMode: string;
  credentialType: string; // Always 'OAUTH2' for ArcGIS integrations
  username: string;
  password: string;
  clientId: string;
  clientSecret: string;
  tokenUrl: string;
  scope: string;
  fieldMappings: FieldMapping[];
  connectionMethod?: 'new' | 'existing';
  existingConnectionId?: string;
  createdConnectionId?: string;
}

export interface FieldMapping {
  id: string;
  sourceField: string;
  targetField: string;
  transformationType?: string;
  isMandatory: boolean;
  displayOrder?: number;
  isDefault?: boolean; // Flag to identify mandatory default mapping (id -> external_location_id)
}

export interface WizardStep {
  key: string;
  title: string;
}

export interface BasicDetailsData {
  name: string;
  description: string;
}

export interface DocumentSelectionData {
  itemType: string;
  subType: string;
  subTypeLabel?: string;
  dynamicDocument?: string;
  dynamicDocumentLabel?: string;
}

export interface ScheduleConfigurationData {
  executionDate: string | null;
  executionTime: string;
  frequencyPattern: string;
  dailyFrequency: string;
  dailyFrequencyRule?: string;
  selectedDays?: string[];
  selectedMonths?: number[];
  monthlyPattern?: string;
  monthlyWeek?: string;
  monthlyWeekday?: string;
  monthlyDay?: number | null;
  isExecuteOnMonthEnd?: boolean;
  cronExpression?: string;
  businessTimeZone?: string;
  timeCalculationMode?: 'FIXED_DAY_BOUNDARY' | 'FLEXIBLE_INTERVAL';
}

export interface ConnectionData {
  connectionName?: string;
  credentialType: string; // Always 'OAUTH2' for ArcGIS integrations
  fetchMode: string;
  arcgisEndpoint: string;
  username: string;
  password: string;
  clientId: string;
  clientSecret: string;
  tokenUrl: string;
  scope: string;
  connectionMethod?: 'new' | 'existing';
  existingConnectionId?: string;
  createdConnectionId?: string;
}

export interface FieldMappingData {
  fieldMappings: FieldMapping[];
}
