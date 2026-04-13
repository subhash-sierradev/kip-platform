import type { IntegrationConnectionRequest, IntegrationSecret } from '@/api/models';
import type { ArcGISIntegrationCreateUpdateRequest } from '@/api/models/ArcGISIntegrationCreateUpdateRequest';
import type { BasicAuthCredential, OAuthClientCredential } from '@/api/models/AuthCredential';
import { ServiceType } from '@/api/models/enums';
import { FieldTransformationType } from '@/api/models/FieldTransformationType';
import type { IntegrationScheduleRequest } from '@/api/models/IntegrationScheduleRequest';
import type { ArcGISFormData } from '@/types/ArcGISFormData';

import { convertLocalDateTimeToUtc, getLocalDateString, getUserTimezone } from './timezoneUtils';

type BaseSchedule = Pick<
  IntegrationScheduleRequest,
  'frequencyPattern' | 'businessTimeZone' | 'timeCalculationMode'
>;

// Helper functions to build specific credential types
function buildBasicAuthCredential(credentialData?: Record<string, string>): BasicAuthCredential {
  return {
    authType: 'BASIC_AUTH',
    username: credentialData?.username || '',
    password: credentialData?.password || '',
  };
}

function buildOAuth2Credential(credentialData?: Record<string, string>): OAuthClientCredential {
  return {
    authType: 'OAUTH2',
    clientId: credentialData?.clientId || '',
    clientSecret: credentialData?.clientSecret || '',
    tokenUrl: credentialData?.tokenUrl || '',
    scope: credentialData?.scope || '',
  };
}

function buildIntegrationSecret(form: ArcGISFormData): IntegrationSecret {
  const credentialType = (form.credentialType || 'BASIC_AUTH').toUpperCase();
  let credentials: BasicAuthCredential | OAuthClientCredential;

  switch (credentialType) {
    case 'OAUTH2':
    case 'OAUTH':
      credentials = buildOAuth2Credential({
        clientId: form.clientId,
        clientSecret: form.clientSecret,
        tokenUrl: form.tokenUrl,
        scope: form.scope,
      });
      break;
    case 'BASIC_AUTH':
    case 'USERNAME_PASSWORD':
    default:
      credentials = buildBasicAuthCredential({
        username: form.username,
        password: form.password,
      });
      break;
  }

  return {
    baseUrl: form.arcgisEndpoint,
    authType: credentialType,
    credentials,
  };
}

export function buildConnectionRequest(form: ArcGISFormData): IntegrationConnectionRequest {
  const integrationSecret = buildIntegrationSecret(form);

  return {
    name: form.connectionName?.trim() || 'Untitled Connection',
    serviceType: ServiceType.ARCGIS,
    integrationSecret,
  };
}

export function buildIntegrationRequest(
  form: ArcGISFormData,
  connectionId: string,
  _scheduleId?: string
): ArcGISIntegrationCreateUpdateRequest {
  const schedule = buildScheduleRequest(form);
  // Build field mappings from wizard entries
  const fieldMappings: ArcGISIntegrationCreateUpdateRequest['fieldMappings'] = (
    form.fieldMappings || []
  )
    .filter(
      (m: { sourceField: unknown; targetField: unknown }) =>
        typeof m.sourceField === 'string' &&
        m.sourceField.trim() !== '' &&
        typeof m.targetField === 'string' &&
        m.targetField.trim() !== ''
    )
    .map(
      (
        m: {
          id?: string;
          sourceField: string;
          targetField: string;
          transformationType?: string;
          isMandatory?: boolean;
          displayOrder?: number;
        },
        index: number
      ) => ({
        // Include ID only for existing mappings (real UUIDs), exclude frontend-generated IDs
        ...(m.id && !m.id.startsWith('field-') && { id: m.id }),
        // Backend will set integrationId after saving the integration
        sourceFieldPath: m.sourceField.trim(),
        targetFieldPath: m.targetField.trim(),
        transformationType: (m.transformationType ||
          FieldTransformationType.PASSTHROUGH) as FieldTransformationType,
        isMandatory: !!m.isMandatory,
        displayOrder: m.displayOrder !== undefined ? m.displayOrder : index,
        // Optional fields left undefined for create; backend derives types/configs as needed
        transformationConfig: undefined,
        defaultValue: undefined,
      })
    );

  return {
    name: form.name,
    description: form.description,
    itemType: form.itemType,
    itemSubtype: form.subType,
    dynamicDocumentType: form.dynamicDocument || undefined,
    connectionId,
    schedule,
    fieldMappings,
  };
}

function toHMS(time: string | undefined): string {
  if (!time) return '09:00:00';
  // Accept HH:mm or HH:mm:ss; normalize to HH:mm:ss
  const parts = time.split(':');
  if (parts.length === 2) return `${parts[0]}:${parts[1]}:00`;
  return time;
}

function normalizePattern(
  pattern: string | undefined
): IntegrationScheduleRequest['frequencyPattern'] {
  const p = (pattern || '').toUpperCase();
  switch (p) {
    case 'DAILY':
    case 'WEEKLY':
    case 'MONTHLY':
    case 'CUSTOM':
      return p as IntegrationScheduleRequest['frequencyPattern'];
    default:
      return 'DAILY';
  }
}

function parseDailyInterval(val: string | undefined): number | undefined {
  if (!val) return undefined;
  const n = parseInt(val, 10);
  return Number.isFinite(n) ? n : undefined;
}

export function buildScheduleRequest(form: ArcGISFormData): IntegrationScheduleRequest {
  const frequencyPattern = normalizePattern(form.frequencyPattern);

  // Build base schedule with common fields
  const baseSchedule: BaseSchedule = {
    businessTimeZone: form.businessTimeZone || getUserTimezone(),
    frequencyPattern,
    timeCalculationMode: form.timeCalculationMode || 'FLEXIBLE_INTERVAL',
  };

  // Build pattern-specific schedule
  switch (frequencyPattern) {
    case 'ONCE':
      return buildOnceSchedule(form, baseSchedule);
    case 'DAILY':
      return buildDailySchedule(form, baseSchedule);
    case 'WEEKLY':
      return buildWeeklySchedule(form, baseSchedule);
    case 'MONTHLY':
      return buildMonthlySchedule(form, baseSchedule);
    case 'CUSTOM':
      return buildCustomSchedule(form, baseSchedule);
    default:
      return buildDailySchedule(form, baseSchedule); // fallback
  }
}

function buildOnceSchedule(form: ArcGISFormData, base: BaseSchedule): IntegrationScheduleRequest {
  const localDate = form.executionDate || getLocalDateString();
  const localTime = toHMS(form.executionTime);
  // Convert local date+time together to UTC — handles midnight crossing across timezone boundaries
  const { utcDate: executionDate, utcTime: executionTime } = convertLocalDateTimeToUtc(
    localDate,
    localTime
  );

  return {
    ...base,
    executionDate,
    executionTime,
    dailyExecutionInterval: undefined,
    daySchedule: undefined,
    monthSchedule: undefined,
    isExecuteOnMonthEnd: form.isExecuteOnMonthEnd || false,
    cronExpression: undefined,
  };
}

function buildDailySchedule(form: ArcGISFormData, base: BaseSchedule): IntegrationScheduleRequest {
  const localDate = form.executionDate || getLocalDateString();
  const localTime = toHMS(form.executionTime);
  // Convert local date+time together to UTC — handles midnight crossing across timezone boundaries
  const { utcDate: executionDate, utcTime: executionTime } = convertLocalDateTimeToUtc(
    localDate,
    localTime
  );
  let dailyExecutionInterval = parseDailyInterval(form.dailyFrequency);

  // Default to 24 hours if not specified
  if (dailyExecutionInterval === undefined) {
    dailyExecutionInterval = 24;
  }

  return {
    ...base,
    executionDate,
    executionTime,
    dailyExecutionInterval: dailyExecutionInterval,
    daySchedule: undefined,
    monthSchedule: undefined,
    isExecuteOnMonthEnd: form.isExecuteOnMonthEnd || false,
    cronExpression: undefined,
  };
}

function buildWeeklySchedule(form: ArcGISFormData, base: BaseSchedule): IntegrationScheduleRequest {
  const localDate = form.executionDate || getLocalDateString();
  const localTime = toHMS(form.executionTime);
  // Convert local date+time together to UTC — handles midnight crossing across timezone boundaries
  const { utcDate: executionDate, utcTime: executionTime } = convertLocalDateTimeToUtc(
    localDate,
    localTime
  );
  const daySchedule = form.selectedDays || [];

  return {
    ...base,
    executionDate,
    executionTime,
    dailyExecutionInterval: undefined,
    daySchedule: daySchedule.length > 0 ? daySchedule : undefined,
    monthSchedule: undefined,
    isExecuteOnMonthEnd: form.isExecuteOnMonthEnd || false,
    cronExpression: undefined,
  };
}

function buildMonthlySchedule(
  form: ArcGISFormData,
  base: BaseSchedule
): IntegrationScheduleRequest {
  const localDate = form.isExecuteOnMonthEnd ? null : form.executionDate || getLocalDateString();
  const localTime = toHMS(form.executionTime);
  // Convert local date+time together to UTC — handles midnight crossing across timezone boundaries
  const { utcDate: utcExecutionDate, utcTime: executionTime } = convertLocalDateTimeToUtc(
    localDate || getLocalDateString(),
    localTime
  );
  // Preserve null for end-of-month schedules; use the UTC date otherwise
  const executionDate = form.isExecuteOnMonthEnd ? null : utcExecutionDate;
  const monthSchedule = form.selectedMonths?.map((m: number) => getMonthName(m)) || [];

  return {
    ...base,
    executionDate,
    executionTime,
    dailyExecutionInterval: undefined,
    daySchedule: undefined,
    monthSchedule,
    isExecuteOnMonthEnd: form.isExecuteOnMonthEnd || false,
    cronExpression: undefined,
  };
}

function buildCustomSchedule(form: ArcGISFormData, base: BaseSchedule): IntegrationScheduleRequest {
  // Use the user-provided cron expression if available, otherwise use yearly cron
  const cronExpression = form.cronExpression;
  const localDate = form.executionDate || getLocalDateString();
  const localTime = toHMS(form.executionTime);
  // Convert local date+time together to UTC — handles midnight crossing across timezone boundaries
  const { utcDate: executionDate, utcTime: executionTime } = convertLocalDateTimeToUtc(
    localDate,
    localTime
  );

  return {
    ...base,
    executionDate,
    executionTime,
    dailyExecutionInterval: undefined,
    daySchedule: undefined,
    monthSchedule: undefined,
    isExecuteOnMonthEnd: form.isExecuteOnMonthEnd || false,
    cronExpression,
  };
}

function getMonthName(monthNumber: number): string {
  const months = [
    'JANUARY',
    'FEBRUARY',
    'MARCH',
    'APRIL',
    'MAY',
    'JUNE',
    'JULY',
    'AUGUST',
    'SEPTEMBER',
    'OCTOBER',
    'NOVEMBER',
    'DECEMBER',
  ];
  return months[monthNumber - 1] || 'JANUARY';
}
