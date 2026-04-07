/**
 * Integration Schedule Request DTO
 * Matches backend IntegrationScheduleDto structure exactly
 */
export interface IntegrationScheduleRequest {
  id?: string;
  executionDate: string | null; // YYYY-MM-DD (required unless end-of-month)
  executionTime: string; // HH:mm:ss (required)
  businessTimeZone: string; // required — IANA timezone for day boundary calculations
  timeCalculationMode?: 'FIXED_DAY_BOUNDARY' | 'FLEXIBLE_INTERVAL';
  frequencyPattern: 'ONCE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM';
  dailyExecutionInterval?: number;
  daySchedule?: string[];
  monthSchedule?: string[];
  isExecuteOnMonthEnd: boolean;
  cronExpression?: string;
}
