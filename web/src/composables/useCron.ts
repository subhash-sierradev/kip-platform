import { isValidCron } from 'cron-validator';
import cronstrue from 'cronstrue';
import { computed, ref } from 'vue';

import { monthlyDayText, monthlyEveryDayText } from '@/utils/cronMonthTextUtils';

import { cronToTextWithTimezoneUniversal as formatCronUniversal } from './useCronOccurrences';

const CRON_VALIDATOR_OPTIONS = {
  seconds: true,
  allowBlankDay: true,
  alias: true,
  allowSevenAsSunday: true,
  allowNthWeekdayOfMonth: true,
} as const;

function isValidQuartzCronExpression(expression: string): boolean {
  const trimmed = expression.trim();
  if (!trimmed) {
    return false;
  }

  const parts = trimmed.split(/\s+/);

  if (parts.length === 6) {
    const dayOfMonth = parts[3]?.toUpperCase();
    const dayOfWeek = parts[5]?.toUpperCase();

    // Quartz: "L" in day-of-month means last day of month.
    // `cron-validator` doesn't support `L`, so validate by sanitizing the DOM field.
    if (dayOfMonth === 'L') {
      if (dayOfWeek !== '?') {
        return false;
      }

      const sanitized = [...parts];
      sanitized[3] = '1';
      return isValidCron(sanitized.join(' '), CRON_VALIDATOR_OPTIONS);
    }
  }

  return isValidCron(trimmed, CRON_VALIDATOR_OPTIONS);
}

export function useCron() {
  const cron = ref('');
  const error = ref('');

  const isValidateCron = computed(() => {
    if (!cron.value) {
      error.value = '';
      return false;
    }

    const valid = isValidQuartzCronExpression(cron.value);

    error.value = valid ? '' : 'Invalid cron expression';
    return valid;
  });

  return { cron, isValidateCron, error };
}

function formatTime(h: string, m: string): string {
  const hourNum = parseInt(h, 10);
  const suffix = hourNum >= 12 ? 'PM' : 'AM';
  const hour12 = hourNum % 12 || 12;
  return `${hour12}:${m.padStart(2, '0')} ${suffix}`;
}

function parseCronFields(
  cron: string
): { min: string; hour: string; dom: string; mon: string; dow: string } | null {
  const trimmed = cron?.trim();
  if (!trimmed) return null;

  const parts = trimmed.split(/\s+/);
  if (parts.length !== 5 && parts.length !== 6) {
    return null;
  }

  // Handle 6-field cron (seconds + standard 5 fields)
  const [, min, hour, dom, mon, dow] = parts.length === 6 ? parts : ['', ...parts];

  if (!min || !hour || !dom || !mon || !dow) {
    return null;
  }

  return { min, hour, dom, mon, dow };
}

export interface CronDescription {
  text: string;
  timezoneLabel: string | null;
  isDateRangeShift?: boolean;
}

export function cronToTextWithTimezone(
  cron: string,
  timezone: string,
  referenceDate?: string | Date
): CronDescription {
  if (!cron?.trim()) {
    return { text: '', timezoneLabel: null, isDateRangeShift: false };
  }

  const result = formatCronUniversal(cron, timezone, referenceDate);
  if (!result.success) {
    return {
      text: cronToText(cron),
      timezoneLabel: timezone,
      isDateRangeShift: false,
    };
  }

  return {
    text: result.text,
    timezoneLabel: result.timezoneLabel,
    isDateRangeShift: result.isDateRangeShift,
  };
}

export function cronToText(cron: string): string {
  const fields = parseCronFields(cron);
  if (!fields) return '';

  const knownDescription = describeKnownPattern(fields);
  if (knownDescription) {
    return knownDescription;
  }

  return describeWithCronstrue(cron) || 'Custom schedule';
}

function describeKnownPattern(fields: {
  min: string;
  hour: string;
  dom: string;
  mon: string;
  dow: string;
}): string | null {
  const { min, hour, dom, mon, dow } = fields;

  if (min === '*' && hour === '*') {
    return 'Runs every minute';
  }

  if (isEveryNMinutes(min)) {
    return everyNMinutesText(min);
  }

  if (isHourlyInterval(hour)) {
    return hourlyIntervalText(hour, min);
  }

  if (isWeekly(dow)) {
    return weeklyText(dow, hour, min);
  }

  if (isMonthEnd(dom)) {
    return monthEndText(mon, hour, min);
  }

  if (isPlainDaily(dom, mon, dow)) {
    return `Runs daily at ${formatTime(hour, min)}`;
  }

  if (isEveryDayInSpecificMonths(dom, mon, dow)) {
    return monthlyEveryDayText(mon, formatTime(hour, min));
  }

  if (isSpecificDayOfMonth(dom, dow)) {
    return monthlyDayText(dom, mon, formatTime(hour, min));
  }

  return null;
}

function describeWithCronstrue(cron: string): string | null {
  const trimmed = cron.trim();
  if (!trimmed) {
    return null;
  }
  try {
    const description = cronstrue.toString(trimmed, {
      throwExceptionOnParseError: false,
      verbose: true,
      dayOfWeekStartIndexZero: false,
      monthStartIndexZero: false,
      use24HourTimeFormat: false,
    });
    if (!description || /^an error/i.test(description)) {
      return null;
    }
    const normalized = description.replace(/\.$/, '');
    const lowerStart = normalized.charAt(0).toLowerCase() + normalized.slice(1);
    return `Runs ${lowerStart}`;
  } catch {
    return null;
  }
}

function isEveryNMinutes(min: string) {
  return min.includes('/');
}

function everyNMinutesText(min: string) {
  // Handle both */n and 0/n patterns
  if (min.startsWith('*/')) {
    return `Runs every ${min.replace('*/', '')} minutes`;
  }
  // Handle patterns like 0/1, 0/5, etc.
  const parts = min.split('/');
  if (parts.length === 2) {
    const interval = parts[1];
    return `Runs every ${interval} ${interval === '1' ? 'minute' : 'minutes'}`;
  }
  return `Runs every ${min} minutes`;
}

function isHourlyInterval(hour: string) {
  return hour.includes('/');
}

function hourlyIntervalText(hour: string, min: string) {
  const [start, interval] = hour.split('/');
  return `Runs every ${interval} hours starting at ${formatTime(start, min)}`;
}

function isWeekly(dow: string) {
  if (!dow || dow === '*' || dow === '?') {
    return false;
  }

  return /^([A-Z]{3}|[0-7])(,([A-Z]{3}|[0-7]))*$/i.test(dow);
}

function weeklyText(dow: string, hour: string, min: string) {
  const days = dow.split(',').join(' & ');
  return `Runs every ${days} at ${formatTime(hour, min)}`;
}

function isMonthEnd(dom: string) {
  return dom === 'L';
}

function monthEndText(mon: string, hour: string, min: string) {
  const monthText = mon === '*' || !mon ? 'every month' : mon;
  return `Runs on last day of ${monthText} at ${formatTime(hour, min)}`;
}

function isDaily(dom: string, dow: string) {
  return (dom === '*' || dom === '?') && (dow === '*' || dow === '?');
}

function isPlainDaily(dom: string, mon: string, dow: string) {
  return mon === '*' && isDaily(dom, dow);
}

function isEveryDayInSpecificMonths(dom: string, mon: string, dow: string) {
  if (dow !== '?' || dom !== '*' || !mon || mon === '*') {
    return false;
  }
  return /^([A-Z]{3,9}|\d{1,2})(,([A-Z]{3,9}|\d{1,2}))*$/i.test(mon);
}

function isSpecificDayOfMonth(dom: string, dow: string) {
  if (dow !== '?') {
    return false;
  }

  return /^\d+(,\d+)*$/.test(dom);
}

export { cronToTextWithTimezoneUniversal } from './useCronOccurrences';
export {
  analyzeLocalPattern,
  generateOccurrences,
  type LocalOccurrence,
  type ParsedCron,
  parseQuartzCron,
  type PatternDescription,
  renderScheduleDescription,
  type Result,
  type ScheduleDescriptionResult,
  validateTimeZone,
} from './useCronOccurrences';
