export interface IntegrationScheduleResponse {
  id?: string;
  executionDate: string | null; // YYYY-MM-DD (required unless end-of-month)
  executionTime: string; // HH:mm:ss (required)
  businessTimeZone?: string; // IANA timezone stored on schedule
  timeCalculationMode?: 'FIXED_DAY_BOUNDARY' | 'FLEXIBLE_INTERVAL';
  frequencyPattern: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM';
  dailyExecutionInterval?: number;
  daySchedule?: string[];
  monthSchedule?: string[];
  isExecuteOnMonthEnd: boolean;
  cronExpression?: string;
}
