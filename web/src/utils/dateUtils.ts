/**
 * Centralized date utility functions for consistent date formatting across the application
 */

export interface DateFormatOptions {
  includeTime?: boolean;
  includeSeconds?: boolean;
  includeTimezone?: boolean;
  includeWeekday?: boolean;
  format?: 'short' | 'medium' | 'long';
}

const DATE_ONLY_REGEX = /^(\d{4})-(\d{2})-(\d{2})$/;
const MIDNIGHT_DATE_TIME_REGEX =
  /^(\d{4})-(\d{2})-(\d{2})T00:00(?::00(?:\.0{1,3})?)?(?:Z|[+-]\d{2}:\d{2})?$/;

function isMatchingCalendarDate(date: Date, year: number, month: number, day: number): boolean {
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day;
}

/**
 * Parse date text while preserving date-only values (YYYY-MM-DD) as local calendar dates.
 * Midnight-only date-time encodings are also treated as date-only values.
 */
export function parseDatePreservingDateOnly(dateString: string): Date | null {
  const trimmedValue = dateString.trim();

  const dateOnlyMatch = trimmedValue.match(DATE_ONLY_REGEX);
  if (dateOnlyMatch) {
    const year = Number(dateOnlyMatch[1]);
    const month = Number(dateOnlyMatch[2]);
    const day = Number(dateOnlyMatch[3]);
    const localDate = new Date(year, month - 1, day);
    return Number.isNaN(localDate.getTime()) || !isMatchingCalendarDate(localDate, year, month, day)
      ? null
      : localDate;
  }

  if (MIDNIGHT_DATE_TIME_REGEX.test(trimmedValue)) {
    const [datePart] = trimmedValue.split('T');
    if (datePart) {
      return parseDatePreservingDateOnly(datePart);
    }
  }

  const parsedDate = new Date(trimmedValue);
  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
}

/**
 * Parse date filter boundaries from date-picker values.
 * YYYY-MM-DD values are interpreted as local start/end of day boundaries.
 */
export function parseDateOnlyBoundary(dateValue: string, endOfDay: boolean): Date | null {
  const trimmedValue = dateValue.trim();
  const dateOnlyMatch = trimmedValue.match(DATE_ONLY_REGEX);

  if (!dateOnlyMatch) {
    const parsedDate = new Date(trimmedValue);
    return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
  }

  const year = Number(dateOnlyMatch[1]);
  const month = Number(dateOnlyMatch[2]);
  const day = Number(dateOnlyMatch[3]);
  const boundaryDate = endOfDay
    ? new Date(year, month - 1, day, 23, 59, 59, 999)
    : new Date(year, month - 1, day, 0, 0, 0, 0);

  return Number.isNaN(boundaryDate.getTime()) ||
    !isMatchingCalendarDate(boundaryDate, year, month, day)
    ? null
    : boundaryDate;
}

/**
 * Parse and validate date input
 * @param dateString - ISO date string or Date object
 * @returns Valid Date object or null if invalid
 */
function parseDate(dateString: string | Date | null | undefined): Date | null {
  if (!dateString) return null;

  try {
    const date = typeof dateString === 'string' ? new Date(dateString) : dateString;
    return isNaN(date.getTime()) ? null : date;
  } catch {
    return null;
  }
}

/**
 * Build base format options for date formatting
 * @param format - Date format style
 * @param includeWeekday - Whether to include weekday name
 * @returns Base Intl.DateTimeFormatOptions
 */
function buildBaseFormatOptions(
  format: 'short' | 'medium' | 'long',
  includeWeekday: boolean = false
): Intl.DateTimeFormatOptions {
  return {
    weekday: includeWeekday ? 'short' : undefined,
    year: 'numeric',
    month: format === 'short' ? 'short' : format === 'long' ? 'long' : 'short',
    day: '2-digit',
  };
}

/**
 * Add time options to format configuration
 * @param formatOptions - Base format options to modify
 * @param includeSeconds - Whether to include seconds
 */
function addTimeOptions(formatOptions: Intl.DateTimeFormatOptions, includeSeconds: boolean): void {
  formatOptions.hour = '2-digit';
  formatOptions.minute = '2-digit';
  formatOptions.hour12 = true; // 12-hour format with AM/PM

  if (includeSeconds) {
    formatOptions.second = '2-digit';
  }
}

/**
 * Build complete format options from user preferences
 * @param options - User provided options
 * @returns Complete Intl.DateTimeFormatOptions
 */
function buildCompleteFormatOptions(options: DateFormatOptions): Intl.DateTimeFormatOptions {
  const {
    includeTime = true,
    includeSeconds = false,
    includeTimezone = false,
    includeWeekday = false,
    format = 'medium',
  } = options;

  const formatOptions = buildBaseFormatOptions(format, includeWeekday);

  if (includeTime) {
    addTimeOptions(formatOptions, includeSeconds);
  }

  if (includeTimezone) {
    formatOptions.timeZoneName = 'short';
  }

  return formatOptions;
}

/**
 * Format date string to a consistent format
 * @param dateString - ISO date string or Date object
 * @param options - Formatting options
 * @returns Formatted date string
 */
export function formatDate(
  dateString: string | Date | null | undefined,
  options: DateFormatOptions = {}
): string {
  const date = parseDate(dateString);
  if (!date) return dateString ? 'Invalid Date' : 'N/A';

  try {
    const formatOptions = buildCompleteFormatOptions(options);
    return date.toLocaleDateString('en-US', formatOptions);
  } catch {
    return 'Invalid Date';
  }
}

/**
 * Format date string to UTC format (same style as formatDate but in UTC)
 * @param dateString - ISO date string or Date object
 * @param options - Formatting options
 * @returns Formatted date string in UTC timezone
 */
export function formatDateUTC(
  dateString: string | Date | null | undefined,
  options: DateFormatOptions = {}
): string {
  const date = parseDate(dateString);
  if (!date) return dateString ? 'Invalid Date' : 'N/A';

  try {
    const formatOptions = buildCompleteFormatOptions(options);
    formatOptions.timeZone = 'UTC';
    return date.toLocaleDateString('en-US', formatOptions);
  } catch {
    return 'Invalid Date';
  }
}

/**
 * Format date for display in metadata sections (no timezone)
 * @param dateString - ISO date string or Date object
 * @returns Formatted date string without timezone
 */
export function formatMetadataDate(dateString: string | Date | null | undefined): string {
  return formatDate(dateString, {
    includeTime: true,
    includeTimezone: false,
    format: 'short',
  });
}

/**
 * Format date for display in lists and cards (compact format)
 * @param dateString - ISO date string or Date object
 * @returns Compact formatted date string
 */
export function formatDisplayDate(dateString: string | Date | null | undefined): string {
  return formatDate(dateString, {
    includeTime: false,
    includeTimezone: false,
    format: 'short',
  });
}

/**
 * Format date with full details including timezone
 * @param dateString - ISO date string or Date object
 * @returns Full formatted date string with timezone
 */
export function formatFullDate(dateString: string | Date | null | undefined): string {
  return formatDate(dateString, {
    includeTime: true,
    includeTimezone: true,
    format: 'long',
  });
}

/**
 * Time constants for relative time calculation
 */
const TIME_CONSTANTS = {
  MINUTE: 60,
  HOUR: 3600,
  DAY: 86400,
  MONTH: 2592000,
  YEAR: 31536000,
} as const;

/**
 * Format relative time based on seconds difference
 * @param diffInSeconds - Time difference in seconds
 * @returns Formatted relative time string
 */
function formatRelativeTimeString(diffInSeconds: number): string {
  if (diffInSeconds < TIME_CONSTANTS.MINUTE) return 'Just now';
  if (diffInSeconds < TIME_CONSTANTS.HOUR) {
    return `${Math.floor(diffInSeconds / TIME_CONSTANTS.MINUTE)} minutes ago`;
  }
  if (diffInSeconds < TIME_CONSTANTS.DAY) {
    return `${Math.floor(diffInSeconds / TIME_CONSTANTS.HOUR)} hours ago`;
  }
  if (diffInSeconds < TIME_CONSTANTS.MONTH) {
    return `${Math.floor(diffInSeconds / TIME_CONSTANTS.DAY)} days ago`;
  }
  if (diffInSeconds < TIME_CONSTANTS.YEAR) {
    return `${Math.floor(diffInSeconds / TIME_CONSTANTS.MONTH)} months ago`;
  }
  return `${Math.floor(diffInSeconds / TIME_CONSTANTS.YEAR)} years ago`;
}

/**
 * Get relative time string (e.g., "2 hours ago", "3 days ago")
 * @param dateString - ISO date string or Date object
 * @returns Relative time string
 */
export function getRelativeTime(dateString: string | Date | null | undefined): string {
  const date = parseDate(dateString);
  if (!date) return dateString ? 'Invalid Date' : 'N/A';

  try {
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    return formatRelativeTimeString(diffInSeconds);
  } catch {
    return 'Invalid Date';
  }
}

/**
 * Check if a date is today
 * @param dateString - ISO date string or Date object
 * @returns True if the date is today
 */
export function isToday(dateString: string | Date | null | undefined): boolean {
  const date = parseDate(dateString);
  if (!date) return false;

  try {
    const today = new Date();

    return (
      date.getDate() === today.getDate() &&
      date.getMonth() === today.getMonth() &&
      date.getFullYear() === today.getFullYear()
    );
  } catch {
    return false;
  }
}

/**
 * Export default format function for backward compatibility
 */
export default formatMetadataDate;

/**
 * Format date for application display (e.g., "Jan 21, 2025")
 * @param dateString - ISO date string or Date object
 * @returns Formatted date string in "Jan 21, 2025" format
 */
export function formatSimpleDate(dateString: string | Date | null | undefined): string {
  const date = parseDate(dateString);
  if (!date) return dateString ? 'Invalid Date' : 'N/A';

  try {
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
    });
  } catch {
    return 'Invalid Date';
  }
}

/**
 * Format time for application display (e.g., "09:00 AM")
 * @param timeString - Time string in various formats or Date object
 * @returns Formatted time string in "09:00 AM" format
 */
export function formatTime(timeString: string | Date | null | undefined): string {
  if (!timeString) return 'N/A';

  try {
    let date: Date;

    if (typeof timeString === 'string') {
      // Handle HH:MM format
      if (/^\d{1,2}:\d{2}$/.test(timeString)) {
        const [hours, minutes] = timeString.split(':').map(Number);
        date = new Date();
        date.setHours(hours, minutes, 0, 0);
      }
      // Handle HH:MM:SS format
      else if (/^\d{1,2}:\d{2}:\d{2}$/.test(timeString)) {
        const [hours, minutes, seconds] = timeString.split(':').map(Number);
        date = new Date();
        date.setHours(hours, minutes, seconds, 0);
      }
      // Handle full date string or ISO string
      else {
        date = new Date(timeString);
      }
    } else {
      date = timeString;
    }

    if (isNaN(date.getTime())) {
      // If still invalid, try to return the original string if it looks like a time
      if (typeof timeString === 'string' && /\d/.test(timeString)) {
        return timeString;
      }
      return 'Invalid Time';
    }

    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    // Fallback: if it's a string that looks like time, return it as-is
    if (typeof timeString === 'string' && /\d/.test(timeString)) {
      return timeString;
    }
    return 'Invalid Time';
  }
}

export function formatKeyTime(
  startedAt: string | Date | null | undefined,
  completedAt?: string | Date | null | undefined
): string {
  if (!startedAt) return '-';

  const startDate = parseDate(startedAt);
  if (!startDate) return 'Invalid Date';

  const endDate = completedAt ? parseDate(completedAt) : null;

  // Format start date and time
  const startFormatted = formatMetadataDate(startedAt);

  if (!endDate) return startFormatted;

  // Check if dates are on the same day
  const sameDay =
    startDate.getFullYear() === endDate.getFullYear() &&
    startDate.getMonth() === endDate.getMonth() &&
    startDate.getDate() === endDate.getDate();

  if (sameDay) {
    // Same day: show full date-time for start, only time for end
    const endTime = endDate.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
    return `${startFormatted} \u2192 ${endTime}`;
  } else {
    // Different day: show full date-time for both
    const endFormatted = formatMetadataDate(completedAt);
    return `${startFormatted} \u2192 ${endFormatted}`;
  }
}
