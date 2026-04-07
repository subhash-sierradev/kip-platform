/**
 * Schedule formatting utilities for ArcGIS integrations
 * Handles formatting of various schedule patterns (DAILY, WEEKLY, MONTHLY, YEARLY)
 */

import { parseDatePreservingDateOnly } from './dateUtils';

export interface ScheduleInfo {
  frequencyPattern?: string;
  dailyExecutionInterval?: number;
  executionTime?: string;
  daySchedule?: string | string[];
  monthSchedule?: string | string[];
}

/**
 * Helper function to parse schedule array safely
 * @param schedule - Schedule data that could be string, array, or undefined
 * @returns Array of schedule items
 */
export const parseScheduleArray = (schedule: string | string[] | undefined): string[] => {
  if (!schedule) return [];
  if (Array.isArray(schedule)) return schedule;

  const raw = schedule.trim();
  if (raw.length === 0) return [];

  // If JSON array-like, try JSON.parse first
  if (raw.startsWith('[')) {
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        return parsed.map(v => String(v)).filter(v => v.length > 0);
      }
    } catch {
      // Ignore JSON parse errors and fallback to comma-split
    }
  }

  // Handle comma-separated values like "THU,FRI" or "JAN,MAR,MAY"
  return raw
    .split(',')
    .map(v => v.trim())
    .filter(v => v.length > 0);
};

/**
 * Helper function to format names from arrays with proper capitalization
 * @param items - Array of strings to format
 * @returns Formatted comma-separated string
 */
export const formatNameArray = (items: string[]): string =>
  items.map(item => item.charAt(0).toUpperCase() + item.slice(1).toLowerCase()).join(', ');

/**
 * Helper function to sort months chronologically (January to December)
 * @param months - Array of month names, abbreviations, or numbers
 * @returns Sorted array of full month names
 */
export const sortMonthsChronologically = (months: string[]): string[] => {
  const monthMap: Record<string, number> = {
    JANUARY: 0,
    JAN: 0,
    '1': 0,
    FEBRUARY: 1,
    FEB: 1,
    '2': 1,
    MARCH: 2,
    MAR: 2,
    '3': 2,
    APRIL: 3,
    APR: 3,
    '4': 3,
    MAY: 4,
    '5': 4,
    JUNE: 5,
    JUN: 5,
    '6': 5,
    JULY: 6,
    JUL: 6,
    '7': 6,
    AUGUST: 7,
    AUG: 7,
    '8': 7,
    SEPTEMBER: 8,
    SEP: 8,
    '9': 8,
    OCTOBER: 9,
    OCT: 9,
    '10': 9,
    NOVEMBER: 10,
    NOV: 10,
    '11': 10,
    DECEMBER: 11,
    DEC: 11,
    '12': 11,
  };

  const monthIndexMap: Record<number, string> = {
    0: 'January',
    1: 'February',
    2: 'March',
    3: 'April',
    4: 'May',
    5: 'June',
    6: 'July',
    7: 'August',
    8: 'September',
    9: 'October',
    10: 'November',
    11: 'December',
  };

  // Convert to indices, sort, remove duplicates, then convert back
  const sortedIndices = months
    .map((month: string) => {
      const monthStr = String(month).toUpperCase();
      return monthMap[monthStr] !== undefined ? monthMap[monthStr] : -1;
    })
    .filter((idx: number) => idx >= 0)
    .sort((a: number, b: number) => a - b);

  const uniqueIndices = Array.from(new Set(sortedIndices));
  return uniqueIndices.map((idx: number) => monthIndexMap[idx]);
};

/**
 * Helper function to format time string for display
 * @param executionTime - Time string in HH:mm:ss format
 * @returns Formatted time string
 */
export const formatTime = (executionTime?: string): string => {
  if (!executionTime) return '09:00 AM';

  const [hours, minutes] = executionTime.split(':');
  const hour24 = parseInt(hours, 10);
  const hour12 = hour24 === 0 ? 12 : hour24 > 12 ? hour24 - 12 : hour24;
  const ampm = hour24 >= 12 ? 'PM' : 'AM';

  return `${hour12}:${minutes} ${ampm}`;
};

/**
 * Helper function to format daily schedule with interval handling
 * @param interval - Daily execution interval in hours
 * @param time - Formatted time string
 * @returns Formatted daily schedule string
 */
export const formatDailySchedule = (interval: number, time: string): string => {
  return `Runs every ${interval} hour${interval > 1 ? 's' : ''} starting at ${time}`;
};

/**
 * Main function to handle schedule formatting by pattern with consistent messaging
 * @param pattern - Schedule frequency pattern
 * @param scheduleInfo - Schedule configuration information
 * @param time - Formatted time string
 * @returns Formatted schedule description
 */
export const formatScheduleByPattern = (
  pattern: string,
  scheduleInfo: ScheduleInfo,
  time: string
): string => {
  const interval = scheduleInfo.dailyExecutionInterval || 1;
  switch (pattern) {
    case 'DAILY':
      return formatDailySchedule(interval, time);
    case 'WEEKLY': {
      const days = parseScheduleArray(scheduleInfo.daySchedule);
      const dayNames = days.length > 0 ? formatNameArray(days) : 'Mon, Tue, Wed, Thu, Fri';
      return `Weekly on ${dayNames} at ${time}`;
    }
    case 'MONTHLY': {
      const months = parseScheduleArray(scheduleInfo.monthSchedule);
      const sortedMonths = months.length > 0 ? sortMonthsChronologically(months) : [];
      if (sortedMonths.length === 0) {
        return `Monthly at ${time}`;
      }
      const monthNames = formatNameArray(sortedMonths);
      return `Monthly in ${monthNames} at ${time}`;
    }
    case 'YEARLY': {
      const months = parseScheduleArray(scheduleInfo.monthSchedule);
      const yearMonth = months.length > 0 ? months[0] : 'JAN';
      return `Yearly in ${yearMonth.charAt(0) + yearMonth.slice(1).toLowerCase()} at ${time}`;
    }
    default:
      return interval === 1 ? `Hourly at ${time}` : `Every ${interval} hours at ${time}`;
  }
};

/**
 * Main function to get formatted schedule text for an integration
 * @param scheduleInfo - Schedule configuration information
 * @returns Formatted schedule description or "Not scheduled"
 */
export const getScheduleText = (scheduleInfo: ScheduleInfo): string => {
  if (!scheduleInfo.frequencyPattern) {
    return 'Not scheduled';
  }

  const time = formatTime(scheduleInfo.executionTime);
  return formatScheduleByPattern(scheduleInfo.frequencyPattern, scheduleInfo, time);
};

/**
 * Helper function to format execution date for display
 * @param dateString - ISO date string
 * @returns Formatted date string
 */
export const formatExecutionDate = (dateString: string): string => {
  const date = parseDatePreservingDateOnly(dateString);
  if (!date) {
    return dateString;
  }

  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
};
