/**
 * Cron expression validation utility
 * Provides client-side validation for cron expressions
 */

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
  const [second, minute, hour, day, month, dayOfWeek] = parts;

  const fieldValidation = validateCronFields(second, minute, hour, day, month, dayOfWeek);
  if (!fieldValidation.isValid) {
    return fieldValidation;
  }

  const dayValidation = validateDayFields(day, dayOfWeek);
  if (!dayValidation.isValid) {
    return dayValidation;
  }

  return { isValid: true };
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
 * Day of week name mapping
 */
const dayOfWeekMap: Record<string, number> = {
  SUN: 1,
  MON: 2,
  TUE: 3,
  WED: 4,
  THU: 5,
  FRI: 6,
  SAT: 7,
};

/**
 * Validates individual cron fields
 */
function validateCronFields(
  second: string,
  minute: string,
  hour: string,
  day: string,
  month: string,
  dayOfWeek: string
): { isValid: boolean; error?: string } {
  if (!isValidCronField(second, 0, 59, 'second')) {
    return { isValid: false, error: 'Invalid second field (0-59)' };
  }

  if (!isValidCronField(minute, 0, 59, 'minute')) {
    return { isValid: false, error: 'Invalid minute field (0-59)' };
  }

  if (!isValidCronField(hour, 0, 23, 'hour')) {
    return { isValid: false, error: 'Invalid hour field (0-23)' };
  }

  if (!isValidCronField(day, 1, 31, 'day', true)) {
    return { isValid: false, error: 'Invalid day field (1-31 or ?)' };
  }

  if (!isValidCronField(month, 1, 12, 'month')) {
    return { isValid: false, error: 'Invalid month field (1-12)' };
  }

  if (!isValidDayOfWeekField(dayOfWeek)) {
    return { isValid: false, error: 'Invalid day of week field (1-7 or ?)' };
  }

  return { isValid: true };
}

/**
 * Validates day of week field specifically (supports both numeric 1-7 and day names)
 */
function isValidDayOfWeekField(field: string): boolean {
  if (!field) return false;

  if (field === '*' || field === '?') {
    return true;
  }

  if (field.includes('-')) {
    return validateDayOfWeekRange(field);
  }

  if (field.includes(',')) {
    return validateDayOfWeekList(field);
  }

  if (field.includes('/')) {
    return validateStep(field, 1, 7, 'dayOfWeek', true);
  }

  // Check if it's a single day name or number
  const dayValue = convertDayOfWeekToNumber(field);
  return dayValue !== null && dayValue >= 1 && dayValue <= 7;
}

/**
 * Converts day of week name to number (e.g., 'MON' -> 2)
 */
function convertDayOfWeekToNumber(day: string): number | null {
  if (dayOfWeekMap[day.toUpperCase()]) {
    return dayOfWeekMap[day.toUpperCase()];
  }

  const num = parseInt(day, 10);
  return !isNaN(num) && num >= 1 && num <= 7 ? num : null;
}

/**
 * Validates day of week range (e.g., 'MON-FRI' or '2-6')
 */
function validateDayOfWeekRange(field: string): boolean {
  const rangeParts = field.split('-');
  if (rangeParts.length !== 2) return false;

  const start = convertDayOfWeekToNumber(rangeParts[0]);
  const end = convertDayOfWeekToNumber(rangeParts[1]);

  return start !== null && end !== null && start <= end;
}

/**
 * Validates day of week list (e.g., 'MON,WED,FRI' or '1,3,5')
 */
function validateDayOfWeekList(field: string): boolean {
  const listParts = field.split(',');
  return listParts.every(part => {
    const dayValue = convertDayOfWeekToNumber(part.trim());
    return dayValue !== null;
  });
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
 * Validates an individual cron field
 * @param field - The field value to validate
 * @param min - Minimum allowed value
 * @param max - Maximum allowed value
 * @param fieldName - Name of the field for error reporting
 * @param allowQuestion - Whether ? is allowed for this field
 * @returns boolean indicating if the field is valid
 */
function isValidCronField(
  field: string,
  min: number,
  max: number,
  fieldName: string,
  allowQuestion = false
): boolean {
  if (!field) return false;

  if (isBasicCronSymbol(field, allowQuestion)) {
    return true;
  }

  if (field.includes('-')) {
    return validateRange(field, min, max);
  }

  if (field.includes(',')) {
    return validateList(field, min, max);
  }

  if (field.includes('/')) {
    return validateStep(field, min, max, fieldName, allowQuestion);
  }

  return validateSingleNumber(field, min, max);
}

/**
 * Checks for basic cron symbols
 */
function isBasicCronSymbol(field: string, allowQuestion: boolean): boolean {
  if (field === '*') return true;
  if (allowQuestion && field === '?') return true;
  return false;
}

/**
 * Validates range format (e.g., '1-5')
 */
function validateRange(field: string, min: number, max: number): boolean {
  const rangeParts = field.split('-');
  if (rangeParts.length !== 2) return false;

  const start = parseInt(rangeParts[0], 10);
  const end = parseInt(rangeParts[1], 10);
  return !isNaN(start) && !isNaN(end) && start >= min && end <= max && start <= end;
}

/**
 * Validates list format (e.g., '1,3,5')
 */
function validateList(field: string, min: number, max: number): boolean {
  const listParts = field.split(',');
  return listParts.every(part => {
    const num = parseInt(part.trim(), 10);
    return !isNaN(num) && num >= min && num <= max;
  });
}

/**
 * Validates step format (e.g., asterisk/5, 1-10/2)
 */
function validateStep(
  field: string,
  min: number,
  max: number,
  fieldName: string,
  allowQuestion: boolean
): boolean {
  const stepParts = field.split('/');
  if (stepParts.length !== 2) return false;

  const step = parseInt(stepParts[1], 10);
  if (isNaN(step) || step <= 0) return false;

  const base = stepParts[0];
  if (base === '*') return true;

  return isValidCronField(base, min, max, fieldName, allowQuestion);
}

/**
 * Validates single number
 */
function validateSingleNumber(field: string, min: number, max: number): boolean {
  const num = parseInt(field, 10);
  return !isNaN(num) && num >= min && num <= max;
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
