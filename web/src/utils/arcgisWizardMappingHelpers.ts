// ArcGIS Integration Wizard data mapping utilities
// Provides functions for mapping between ArcGIS integration detail objects and form data
// Includes both legacy individual mapping functions and unified mapping approach
import type { ArcGISFormData } from '@/types/ArcGISFormData';

import { convertUtcTimeToUserTimezone } from './scheduleDisplayUtils';

interface DetailData {
  name?: string;
  description?: string;
  itemType?: string;
  dynamicDocumentType?: string;
  dynamicDocumentTypeLabel?: string | null;
  itemSubtype?: string;
  itemSubtypeLabel?: string;
  connectionId?: string;
}

// Use Record<string, unknown> for complex form data to avoid type conflicts
type AnyFormData = Record<string, unknown>;

interface CustomConfig {
  pattern?: string;
  week?: string;
  weekday?: string;
  dayOfMonth?: number;
  endOfMonth?: boolean;
  yearlyMonth?: string;
  yearlyDay?: string;
}

interface ScheduleData {
  executionDate?: string;
  executionTime?: string;
  frequencyPattern?: string;
  timeCalculationMode?: 'FIXED_DAY_BOUNDARY' | 'FLEXIBLE_INTERVAL';
  dailyExecutionInterval?: number | string;
  daySchedule?: string[];
  monthSchedule?: string[];
  customConfig?: CustomConfig;
  cronExpression?: string;
  isExecuteOnMonthEnd?: boolean;
  businessTimeZone?: string;
}

interface FieldMapping {
  sourceFieldPath?: string;
  sourceFieldName?: string;
  targetFieldName?: string;
  transformationType?: string;
  isMandatory?: boolean;
  sourceField?: string;
  targetField?: string;
}

// Unified ArcGIS wizard input data interface
export interface ArcGISWizardInput {
  detail?: DetailData;
  schedule?: ScheduleData;
  connection?: {
    credentialType?: string;
    fetchMode?: string;
    arcgisEndpoint?: string;
    username?: string;
    password?: string;
    clientId?: string;
    clientSecret?: string;
    tokenUrl?: string;
    scope?: string;
  };
  fieldMappings?: FieldMapping[];
  selectedDocumentId?: string;
}

export function mapDailyFrequencyRule(dailyExecutionInterval: number | string): string {
  const interval = dailyExecutionInterval.toString();
  const availableOptions = ['1', '2', '3', '4', '8', '12'];

  if (availableOptions.includes(interval)) {
    return interval;
  }

  const numInterval = Number(dailyExecutionInterval);
  if (numInterval >= 24) return '12';
  if (numInterval >= 12) return '12';
  if (numInterval >= 8) return '8';
  if (numInterval >= 4) return '4';
  return '1';
}

export function mapMonthSchedule(monthSchedule: string[]): number[] {
  const monthNameToNumber: Record<string, number> = {
    JANUARY: 1,
    FEBRUARY: 2,
    MARCH: 3,
    APRIL: 4,
    MAY: 5,
    JUNE: 6,
    JULY: 7,
    AUGUST: 8,
    SEPTEMBER: 9,
    OCTOBER: 10,
    NOVEMBER: 11,
    DECEMBER: 12,
  };
  return monthSchedule.map(month => monthNameToNumber[month]).filter(Boolean);
}

export function mapCustomConfigToFormData(
  customConfig: CustomConfig | undefined,
  formData: AnyFormData
) {
  if (customConfig) {
    formData.monthlyPattern = customConfig.pattern || 'specific-date';
    formData.monthlyWeek = customConfig.week || '';
    formData.monthlyWeekday = customConfig.weekday || '';
    formData.monthlyDay = customConfig.dayOfMonth || null;

    if (customConfig.yearlyMonth) {
      formData.yearlyMonth = customConfig.yearlyMonth;
      formData.yearlyDay = customConfig.yearlyDay;
    }
  } else {
    formData.monthlyPattern = 'specific-date';
    formData.monthlyWeek = '';
    formData.monthlyWeekday = '';
    formData.monthlyDay = null;
  }
}

export function mapFieldMappings(fieldMappings: FieldMapping[]): Array<{
  sourceField: string;
  targetField: string;
  transformationType: string;
  isMandatory: boolean;
}> {
  // Handle empty or invalid field mappings by returning at least one default mapping
  if (!fieldMappings || fieldMappings.length === 0) {
    return [{ sourceField: '', targetField: '', transformationType: '', isMandatory: false }];
  }

  return fieldMappings.map((m: FieldMapping) => ({
    sourceField: m.sourceFieldPath || m.sourceFieldName || m.sourceField || '',
    targetField: m.targetFieldName || m.targetField || '',
    transformationType: m.transformationType || '',
    isMandatory: !!m.isMandatory,
  }));
}

export function mapBasicDetails(detail: DetailData, formData: AnyFormData) {
  formData.name = detail.name || '';
  formData.description = detail.description || '';
}

export function mapDocumentSelection(detail: DetailData, formData: AnyFormData) {
  // Debug logging for field mapping verification
  if (process.env.NODE_ENV === 'development') {
    console.warn('Document Selection Mapping:', {
      detail: {
        itemType: detail.itemType,
        itemSubtype: detail.itemSubtype,
        dynamicDocumentType: detail.dynamicDocumentType,
      },
      mapped: {
        itemType: detail.itemType || (detail.dynamicDocumentType ? 'Document' : ''),
        subType: detail.itemSubtype || '',
        dynamicDocument: detail.dynamicDocumentType || '',
      },
    });
  }

  formData.itemType = detail.itemType || (detail.dynamicDocumentType ? 'Document' : '');
  formData.subType = detail.itemSubtype || '';
  formData.subTypeLabel = detail.itemSubtypeLabel || '';
  formData.dynamicDocument = detail.dynamicDocumentType || '';
  formData.dynamicDocumentLabel = detail.dynamicDocumentTypeLabel || '';
}

// Utility function to format date for HTML date input (YYYY-MM-DD)
export function formatDateForInput(dateString: string): string {
  if (!dateString) return '';

  try {
    const dateStr = dateString.trim();

    // Check if already in YYYY-MM-DD format (LocalDate serialization from Spring Boot)
    if (/^\d{4}-\d{2}-\d{2}$/.test(dateStr)) {
      return dateStr;
    }

    // Handle ISO datetime strings (extract date part)
    if (dateStr.includes('T')) {
      const datePart = dateStr.split('T')[0];
      return datePart;
    }

    // Handle various date formats by converting to Date object
    const date = new Date(dateString);

    // Check if the date is valid
    if (isNaN(date.getTime())) {
      return '';
    }

    // Format as YYYY-MM-DD for HTML date input
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  } catch (error) {
    console.error('[ARCGIS_DEBUG] formatDateForInput: Error formatting date:', dateString, error);
    return '';
  }
}

export function mapScheduleData(schedule: ScheduleData, formData: AnyFormData) {
  if (!schedule) {
    console.warn('mapScheduleData: No schedule data provided');
    return;
  }

  applyExecutionDate(schedule, formData);
  applyExecutionTime(schedule, formData);
  applyFrequencyFields(schedule, formData);
  applyScheduleSelections(schedule, formData);
  applyCronAndFlags(schedule, formData);
  if (schedule.timeCalculationMode) {
    formData.timeCalculationMode = schedule.timeCalculationMode;
  }
  mapCustomConfigToFormData(schedule.customConfig, formData);
  if (schedule.businessTimeZone) {
    formData.businessTimeZone = schedule.businessTimeZone;
  }
}

function applyExecutionDate(schedule: ScheduleData, formData: AnyFormData): void {
  formData.executionDate = formatDateForInput(schedule.executionDate || '');
}

function applyExecutionTime(schedule: ScheduleData, formData: AnyFormData): void {
  formData.executionTime = schedule.executionTime
    ? convertUtcTimeToUserTimezone(schedule.executionTime, schedule.executionDate)
    : '';
}

function applyFrequencyFields(schedule: ScheduleData, formData: AnyFormData): void {
  formData.frequencyPattern = (schedule.frequencyPattern || '').toString();
  formData.dailyFrequency = (schedule.dailyExecutionInterval ?? '').toString();
  formData.dailyFrequencyRule = schedule.dailyExecutionInterval
    ? mapDailyFrequencyRule(schedule.dailyExecutionInterval)
    : '12';
}

function applyScheduleSelections(schedule: ScheduleData, formData: AnyFormData): void {
  formData.selectedDays = schedule.daySchedule || [];
  formData.selectedMonths =
    schedule.monthSchedule && Array.isArray(schedule.monthSchedule)
      ? mapMonthSchedule(schedule.monthSchedule)
      : [];
}

function applyCronAndFlags(schedule: ScheduleData, formData: AnyFormData): void {
  formData.cronExpression = schedule.cronExpression || '';
  formData.isExecuteOnMonthEnd = schedule.isExecuteOnMonthEnd || false;
}

export function mapConnectionData(detail: DetailData, formData: AnyFormData) {
  formData.connectionMethod = 'existing';
  formData.existingConnectionId = detail.connectionId || '';
}

// Helper functions for default values
function getDefaultScheduleValues(): Pick<
  ArcGISFormData,
  | 'executionDate'
  | 'executionTime'
  | 'frequencyPattern'
  | 'dailyFrequency'
  | 'selectedDays'
  | 'selectedMonths'
  | 'monthlyPattern'
  | 'monthlyWeek'
  | 'monthlyWeekday'
  | 'monthlyDay'
  | 'isExecuteOnMonthEnd'
  | 'cronExpression'
  | 'timeCalculationMode'
> {
  return {
    executionDate: '',
    executionTime: '09:00:00',
    frequencyPattern: '',
    dailyFrequency: '',
    selectedDays: [],
    selectedMonths: [],
    monthlyPattern: 'specific-date',
    monthlyWeek: '',
    monthlyWeekday: '',
    monthlyDay: null,
    isExecuteOnMonthEnd: false,
    cronExpression: undefined,
    timeCalculationMode: 'FLEXIBLE_INTERVAL',
  };
}

function getDefaultFieldMapping(): Array<{
  id: string;
  sourceField: string;
  targetField: string;
  transformationType: string;
  isMandatory: boolean;
  displayOrder: number;
}> {
  return [
    {
      id: 'mapping-0',
      sourceField: '',
      targetField: '',
      transformationType: '',
      isMandatory: false,
      displayOrder: 0,
    },
  ];
}

// Helper function to map basic details section
function mapBasicDetailsSection(
  detail: DetailData | undefined
): Pick<ArcGISFormData, 'name' | 'description'> {
  return {
    name: detail?.name || '',
    description: detail?.description || '',
  };
}

function stringOrEmpty(value: string | null | undefined): string {
  return value ?? '';
}

function stringOrDefault(value: string | null | undefined, defaultValue: string): string {
  return value ?? defaultValue;
}

// Helper function to map document selection section
function mapDocumentSelectionSection(
  detail: DetailData | undefined,
  selectedDocumentId?: string
): Pick<
  ArcGISFormData,
  | 'itemType'
  | 'subType'
  | 'subTypeLabel'
  | 'dynamicDocument'
  | 'dynamicDocumentLabel'
  | 'selectedDocumentId'
> {
  return {
    itemType: stringOrDefault(detail?.itemType, 'DOCUMENT'),
    subType: stringOrEmpty(detail?.itemSubtype),
    subTypeLabel: stringOrEmpty(detail?.itemSubtypeLabel),
    dynamicDocument: detail?.dynamicDocumentType,
    dynamicDocumentLabel: stringOrEmpty(detail?.dynamicDocumentTypeLabel),
    selectedDocumentId: stringOrEmpty(selectedDocumentId),
  };
}

// Helper function to map schedule section
function mapScheduleSection(
  schedule: ScheduleData | undefined
): Pick<
  ArcGISFormData,
  | 'executionDate'
  | 'executionTime'
  | 'frequencyPattern'
  | 'dailyFrequency'
  | 'selectedDays'
  | 'selectedMonths'
  | 'monthlyPattern'
  | 'monthlyWeek'
  | 'monthlyWeekday'
  | 'monthlyDay'
  | 'isExecuteOnMonthEnd'
  | 'cronExpression'
  | 'timeCalculationMode'
> {
  if (!schedule) {
    return getDefaultScheduleValues();
  }
  return {
    executionDate: formatDateForInput(schedule.executionDate || ''),
    executionTime: schedule.executionTime || '',
    frequencyPattern: (schedule.frequencyPattern || '').toString(),
    dailyFrequency: (schedule.dailyExecutionInterval ?? '').toString(),
    selectedDays: schedule.daySchedule || [],
    selectedMonths:
      schedule.monthSchedule && Array.isArray(schedule.monthSchedule)
        ? mapMonthSchedule(schedule.monthSchedule)
        : [],
    timeCalculationMode: schedule.timeCalculationMode || 'FLEXIBLE_INTERVAL',
    cronExpression: undefined,
  };
}

// Helper function to get default connection values
function getDefaultConnectionCredentials(): Pick<
  ArcGISFormData,
  | 'credentialType'
  | 'fetchMode'
  | 'arcgisEndpoint'
  | 'username'
  | 'password'
  | 'clientId'
  | 'clientSecret'
  | 'tokenUrl'
  | 'scope'
> {
  return {
    credentialType: '',
    fetchMode: 'GET',
    arcgisEndpoint: '',
    username: '',
    password: '',
    clientId: '',
    clientSecret: '',
    tokenUrl: '',
    scope: '',
  };
}

// Helper function to map connection credentials
function mapConnectionCredentials(
  connection: ArcGISWizardInput['connection']
): Pick<
  ArcGISFormData,
  | 'credentialType'
  | 'fetchMode'
  | 'arcgisEndpoint'
  | 'username'
  | 'password'
  | 'clientId'
  | 'clientSecret'
  | 'tokenUrl'
  | 'scope'
> {
  if (!connection) {
    return getDefaultConnectionCredentials();
  }

  return {
    credentialType: connection.credentialType || '',
    fetchMode: connection.fetchMode || 'GET',
    arcgisEndpoint: connection.arcgisEndpoint || '',
    username: connection.username || '',
    password: connection.password || '',
    clientId: connection.clientId || '',
    clientSecret: connection.clientSecret || '',
    tokenUrl: connection.tokenUrl || '',
    scope: connection.scope || '',
  };
}

// Helper function to map connection method
function mapConnectionMethod(
  detail: DetailData | undefined
): Pick<ArcGISFormData, 'connectionMethod' | 'existingConnectionId'> {
  return {
    connectionMethod: detail?.connectionId ? 'existing' : 'new',
    existingConnectionId: detail?.connectionId || '',
  };
}

// Helper function to map connection section
function mapConnectionSection(
  detail: DetailData | undefined,
  connection: ArcGISWizardInput['connection']
): Pick<
  ArcGISFormData,
  | 'credentialType'
  | 'fetchMode'
  | 'arcgisEndpoint'
  | 'username'
  | 'password'
  | 'clientId'
  | 'clientSecret'
  | 'tokenUrl'
  | 'scope'
  | 'connectionMethod'
  | 'existingConnectionId'
> {
  const credentials = mapConnectionCredentials(connection);
  const connectionSettings = mapConnectionMethod(detail);

  return {
    ...credentials,
    ...connectionSettings,
  };
}

// Helper function to map field mappings section
function mapFieldMappingsSection(fieldMappings: FieldMapping[] | undefined): Array<{
  id: string;
  sourceField: string;
  targetField: string;
  transformationType: string;
  isMandatory: boolean;
  displayOrder: number;
}> {
  if (!fieldMappings || fieldMappings.length === 0) {
    return getDefaultFieldMapping();
  }

  return mapFieldMappings(fieldMappings).map((mapping, index) => ({
    id: `mapping-${index}`,
    sourceField: mapping.sourceField,
    targetField: mapping.targetField,
    transformationType: mapping.transformationType,
    isMandatory: mapping.isMandatory,
    displayOrder: index,
  }));
}

/**
 * Unified ArcGIS mapping function that consolidates all wizard step mappings
 * into a single comprehensive function returning a complete ArcGISFormData object.
 *
 * @param input - Partial or complete ArcGIS wizard data from various steps
 * @returns Complete ArcGISFormData object with all fields properly mapped
 */

export function mapArcGISDetailToFormData(input: ArcGISWizardInput): ArcGISFormData {
  const basicDetails = mapBasicDetailsSection(input.detail);
  const documentSelection = mapDocumentSelectionSection(input.detail, input.selectedDocumentId);
  const scheduleData = mapScheduleSection(input.schedule);
  const connectionData = mapConnectionSection(input.detail, input.connection);
  const fieldMappings = mapFieldMappingsSection(input.fieldMappings);

  return {
    ...basicDetails,
    ...documentSelection,
    ...scheduleData,
    ...connectionData,
    fieldMappings,
    createdConnectionId: undefined,
  };
}
