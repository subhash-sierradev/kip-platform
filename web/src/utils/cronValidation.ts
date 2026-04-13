/**
 * Cron expression validation utility
 * Provides client-side validation for cron expressions
 */

import { isValidQuartzCronExpression } from '@/composables/cron/useCronOccurrences';

/**
 * Validates a cron expression format
 * @param cronExpression - The cron expression to validate (format: "second minute hour day month dayOfWeek")
 * @returns Object with isValid boolean and error message if invalid
 */
export function validateCronExpression(cronExpression: string): {
  isValid: boolean;
  error?: string;
} {
  const basicValidation = validateBasicFormat(cronExpression);
  if (!basicValidation.isValid) {
    return basicValidation;
  }

  const parts = cronExpression.trim().split(/\s+/);
  const [, , , day, , dayOfWeek] = parts;

  const dayValidation = validateDayFields(day, dayOfWeek);
  if (!dayValidation.isValid) {
    return dayValidation;
  }

  return isValidQuartzCronExpression(cronExpression)
    ? { isValid: true }
    : { isValid: false, error: 'Invalid cron expression' };
}

/**
 * Validates basic cron expression format
 */
function validateBasicFormat(cronExpression: string): { isValid: boolean; error?: string } {
  if (!cronExpression || cronExpression.trim().length === 0) {
    return { isValid: false, error: 'Cron expression is required' };
  }

  const parts = cronExpression.trim().split(/\s+/);
  if (parts.length !== 6) {
    return {
      isValid: false,
      error: 'Cron expression must have 6 parts: second minute hour day month dayOfWeek',
    };
  }

  return { isValid: true };
}

/**
 * Validates day and dayOfWeek field relationships
 */
function validateDayFields(day: string, dayOfWeek: string): { isValid: boolean; error?: string } {
  if (day === '?' && dayOfWeek === '?') {
    return { isValid: false, error: 'Either day or day-of-week must be specified (not both ?)' };
  }

  if (day !== '?' && dayOfWeek !== '?' && !isWildcard(day) && !isWildcard(dayOfWeek)) {
    return {
      isValid: false,
      error: 'Either day or day-of-week should be ? when the other is specified',
    };
  }

  return { isValid: true };
}

/**
 * Checks if a field is a wildcard or contains wildcard-based expressions
 * @param field - The field to check
 * @returns boolean indicating if it's a wildcard
 */
function isWildcard(field: string): boolean {
  return field === '*' || field.startsWith('*/');
}

/**
 * Provides example cron expressions for user reference
 */
export const cronExamples = {
  everyMinute: '0 * * * * ?',
  every5Minutes: '0 */5 * * * ?',
  everyHour: '0 0 * * * ?',
  dailyAt9AM: '0 0 9 * * ?',
  weekdaysAt9AM: '0 0 9 ? * MON-FRI',
  monthlyFirst: '0 0 9 1 * ?',
  everyWeekdayAt2PM: '0 0 14 ? * MON-FRI',
};

/**
 * Gets a user-friendly description of common cron patterns
 * @param cronExpression - The cron expression to describe
 * @returns Human-readable description or null if not a common pattern
 */
export function describeCronExpression(cronExpression: string): string | null {
  const trimmed = cronExpression.trim();

  // Map of common patterns to descriptions
  const descriptions: Record<string, string> = {
    '0 * * * * ?': 'Every minute',
    '0 */5 * * * ?': 'Every 5 minutes',
    '0 */10 * * * ?': 'Every 10 minutes',
    '0 */15 * * * ?': 'Every 15 minutes',
    '0 */30 * * * ?': 'Every 30 minutes',
    '0 0 * * * ?': 'Every hour',
    '0 0 */2 * * ?': 'Every 2 hours',
    '0 0 */4 * * ?': 'Every 4 hours',
    '0 0 */6 * * ?': 'Every 6 hours',
    '0 0 */12 * * ?': 'Every 12 hours',
    '0 0 9 * * ?': 'Daily at 9:00 AM',
    '0 0 12 * * ?': 'Daily at 12:00 PM',
    '0 0 18 * * ?': 'Daily at 6:00 PM',
    '0 0 9 ? * MON-FRI': 'Weekdays at 9:00 AM',
    '0 0 9 1 * ?': 'Monthly on the 1st at 9:00 AM',
    '0 0 9 L * ?': 'Monthly on the last day at 9:00 AM',
  };

  return descriptions[trimmed] || null;
}
