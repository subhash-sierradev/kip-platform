import { isValidCron } from 'cron-validator';

import { buildCronFieldDescription, getMostCommonLocalTime } from './useCronFieldDescription';
import {
  collectDayOfMonthMatches,
  parseMonthField,
  parseNumericField,
  toOrdinal,
} from './useCronFieldParsers';
import {
  analyzeLocalPattern,
  generateOccurrences,
  type LocalOccurrence,
  type ParsedCron,
  parseQuartzCron,
  type PatternDescription,
  type Result,
  validateTimeZone,
} from './useCronOccurrenceEngine';
import {
  DEFAULT_LOOKAHEAD_DAYS,
  detectDateShift,
  EXTENDED_LOOKAHEAD_DAYS,
  formatTimezoneLabel,
  getHourMinuteParts,
  getReferenceDate,
  shouldRetryWithExtendedLookahead,
} from './useCronOccurrenceUtils';
export {
  analyzeLocalPattern,
  generateOccurrences,
  type LocalOccurrence,
  type ParsedCron,
  parseQuartzCron,
  type PatternDescription,
  type Result,
  validateTimeZone,
};

export interface ScheduleDescriptionResult {
  success: boolean;
  text: string;
  timezoneLabel: string;
  isDateRangeShift: boolean;
  error?: string;
}

const CRON_VALIDATOR_OPTIONS = {
  seconds: true,
  allowBlankDay: true,
  alias: true,
  allowSevenAsSunday: true,
  allowNthWeekdayOfMonth: true,
} as const;

export function renderScheduleDescription(
  analysis: PatternDescription,
  timezone: string,
  originalCron?: string
): string {
  if (analysis.occurrences.length === 0) return 'No scheduled occurrences in the next period';

  const mostCommonTime =
    getMostCommonLocalTime(analysis.occurrences) ?? analysis.occurrences[0].localTime;

  if (originalCron) {
    const fieldDesc = buildCronFieldDescription(originalCron, timezone, analysis.occurrences);
    if (fieldDesc !== null) {
      return fieldDesc;
    }
  }

  return (
    renderPatternDescription(analysis, timezone, mostCommonTime) ??
    `Runs on a custom schedule at ${mostCommonTime}`
  );
}

function renderPatternDescription(
  analysis: PatternDescription,
  timezone: string,
  mostCommonTime: string
): string | null {
  const first = analysis.occurrences[0];

  if (analysis.type === 'single-occurrence') {
    return `Runs on ${first.localDate} at ${mostCommonTime} (${first.offset})`;
  }

  if (analysis.type === 'daily') {
    return `Runs every day at ${mostCommonTime}`;
  }

  if (analysis.type === 'weekly') {
    const weekdays = Array.from(
      new Set(
        analysis.occurrences.map(o =>
          new Intl.DateTimeFormat('en-US', { weekday: 'long', timeZone: 'UTC' }).format(
            new Date(`${o.localDate}T00:00:00Z`)
          )
        )
      )
    );
    return `Runs every ${formatNaturalList(weekdays)} at ${mostCommonTime}`;
  }

  if (analysis.type === 'monthly') {
    const days = Array.from(
      new Set(analysis.occurrences.map(o => new Date(`${o.localDate}T00:00:00Z`).getUTCDate()))
    ).sort((a, b) => a - b);
    return `Runs on the ${formatMonthDays(days)} of each month at ${mostCommonTime}`;
  }

  if (analysis.type === 'interval' && analysis.regularityPattern) {
    return `Runs ${analysis.regularityPattern} at ${mostCommonTime}`;
  }

  if (analysis.type === 'time-varies') {
    return (
      tryRenderSpecificDateShiftDescription(analysis, timezone) ??
      `Runs around ${mostCommonTime} with local time changes`
    );
  }

  return null;
}

function formatNaturalList(items: string[]): string {
  if (items.length <= 1) return items[0] || '';
  if (items.length === 2) return `${items[0]} and ${items[1]}`;
  const head = items.slice(0, -1).join(', ');
  const tail = items[items.length - 1];
  return `${head} and ${tail}`;
}

function formatMonthDays(days: number[]): string {
  const ordinals = days.map(day => toOrdinal(day));
  return formatNaturalList(ordinals);
}

function tryRenderSpecificDateShiftDescription(
  analysis: PatternDescription,
  _timezone: string
): string | null {
  const { occurrences } = analysis;
  if (occurrences.length < 2) return null;
  const uniqueMonthDayPairs = Array.from(
    new Set(occurrences.map(o => o.localDate.slice(5)))
  ).sort();
  if (uniqueMonthDayPairs.length > 12) return null;
  const mostCommonTime = getMostCommonLocalTime(occurrences);
  if (!mostCommonTime) return null;

  const allLastDay = uniqueMonthDayPairs.every(mmdd => {
    const [mm, dd] = mmdd.split('-').map(Number);
    return dd === new Date(Date.UTC(2024, mm, 0)).getUTCDate();
  });

  const uniqueMonths = Array.from(
    new Set(uniqueMonthDayPairs.map(mmdd => mmdd.slice(0, 2)))
  ).sort();

  if (allLastDay && uniqueMonths.length > 0) {
    const monthNames = uniqueMonths.map(mm =>
      new Intl.DateTimeFormat('en-US', { month: 'long', timeZone: 'UTC' }).format(
        new Date(Date.UTC(2024, Number(mm) - 1, 1))
      )
    );
    return `Runs at ${mostCommonTime} on the last day of ${formatNaturalList(monthNames)}`;
  }

  if (uniqueMonthDayPairs.length <= 6) {
    const dateLabels = uniqueMonthDayPairs.map(mmdd => {
      const [mm, dd] = mmdd.split('-').map(Number);
      const monthName = new Intl.DateTimeFormat('en-US', { month: 'long', timeZone: 'UTC' }).format(
        new Date(Date.UTC(2024, mm - 1, 1))
      );
      return `${monthName} ${toOrdinal(dd)}`;
    });
    return `Runs at ${mostCommonTime} on ${formatNaturalList(dateLabels)}`;
  }

  return null;
}

export function cronToTextWithTimezoneUniversal(
  cron: string,
  timezone: string,
  referenceDate?: string | Date,
  lookaheadDays: number = DEFAULT_LOOKAHEAD_DAYS
): ScheduleDescriptionResult {
  const tzResult = validateTimeZone(timezone);
  if (!tzResult.success) {
    return {
      success: false,
      text: '',
      timezoneLabel: timezone,
      isDateRangeShift: false,
      error: tzResult.error,
    };
  }

  if (!isValidQuartzCronExpression(cron)) {
    return {
      success: false,
      text: '',
      timezoneLabel: timezone,
      isDateRangeShift: false,
      error: 'Invalid cron expression',
    };
  }

  try {
    const resolvedReferenceDate = getReferenceDate(referenceDate);
    const startDate = resolvedReferenceDate;
    const endDate = new Date(startDate);
    endDate.setUTCDate(endDate.getUTCDate() + lookaheadDays);

    const occurrenceResult = resolveOccurrencesForDescription(
      cron,
      startDate,
      endDate,
      timezone,
      lookaheadDays
    );
    if (!occurrenceResult.success) {
      return occurrenceResult.errorResult;
    }

    const occurrences = occurrenceResult.occurrences;

    if (occurrences.length === 0) {
      return {
        success: true,
        text: 'No scheduled occurrences in the next period',
        timezoneLabel: timezone,
        isDateRangeShift: false,
      };
    }

    const description = renderScheduleDescription(analyzeLocalPattern(occurrences), timezone, cron);
    const fieldParts = getHourMinuteParts(cron);
    const isDateRangeShift =
      fieldParts !== null
        ? detectDateShift(fieldParts.hour, fieldParts.minute, timezone, resolvedReferenceDate)
        : false;

    return {
      success: true,
      text: description,
      timezoneLabel: formatTimezoneLabel(timezone, occurrences[0].utcDate),
      isDateRangeShift,
    };
  } catch (error) {
    return {
      success: false,
      text: '',
      timezoneLabel: timezone,
      isDateRangeShift: false,
      error: `Failed to process cron: ${error instanceof Error ? error.message : String(error)}`,
    };
  }
}

function buildOccurrenceErrorResult(
  timezone: string,
  error: string | undefined
): { success: false; errorResult: ScheduleDescriptionResult } {
  return {
    success: false,
    errorResult: {
      success: false,
      text: '',
      timezoneLabel: timezone,
      isDateRangeShift: false,
      error,
    },
  };
}

function resolveOccurrencesForDescription(
  cron: string,
  startDate: Date,
  endDate: Date,
  timezone: string,
  lookaheadDays: number
):
  | {
      success: true;
      occurrences: LocalOccurrence[];
    }
  | {
      success: false;
      errorResult: ScheduleDescriptionResult;
    } {
  let occResult = generateOccurrences(cron, startDate, endDate, timezone, 100);
  if (!occResult.success) {
    return buildOccurrenceErrorResult(timezone, occResult.error);
  }

  let occurrences = occResult.data || [];
  if (occurrences.length > 0 || !shouldRetryWithExtendedLookahead(lookaheadDays)) {
    return { success: true, occurrences };
  }

  const extendedEndDate = new Date(startDate);
  extendedEndDate.setUTCDate(extendedEndDate.getUTCDate() + EXTENDED_LOOKAHEAD_DAYS);
  occResult = generateOccurrences(cron, startDate, extendedEndDate, timezone, 100);
  if (!occResult.success) {
    return buildOccurrenceErrorResult(timezone, occResult.error);
  }

  occurrences = occResult.data || [];
  return { success: true, occurrences };
}

function sanitizeDomField(parts: string[]): string[] {
  const sanitized = [...parts];
  sanitized[3] = '1';
  return sanitized;
}

function sanitizeDowField(parts: string[], dayOfWeek: string): string[] {
  const sanitized = [...parts];
  sanitized[3] = '1';
  // Use first digit from token if numeric; otherwise substitute '1' (any valid DOW digit satisfies cron-validator)
  sanitized[5] = /^\d/.test(dayOfWeek) ? dayOfWeek.charAt(0) : '1';
  return sanitized;
}

function sanitizeDowNthField(parts: string[], dayOfWeek: string): string[] {
  const sanitized = [...parts];
  sanitized[3] = '1';
  // Use first digit from token if numeric; otherwise substitute '1' (any valid DOW digit satisfies cron-validator)
  sanitized[5] = /^\d/.test(dayOfWeek) ? dayOfWeek.charAt(0) : '1';
  return sanitized;
}

// Valid DOW token: a single digit 0-7 or a 3-letter abbreviation (SUN-SAT)
const VALID_DOW_TOKEN = /^([0-7]|SUN|MON|TUE|WED|THU|FRI|SAT)$/i;

const QUARTZ_DAY_ORDER = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'] as const;

function resolveDowOrdinal(token: string): number {
  const upper = token.toUpperCase();
  const index = QUARTZ_DAY_ORDER.indexOf(upper as (typeof QUARTZ_DAY_ORDER)[number]);
  if (index !== -1) {
    return index + 1;
  }
  const parsed = parseInt(token, 10);
  if (Number.isNaN(parsed)) {
    return -1;
  }
  return parsed === 0 ? 1 : parsed;
}

function expandDowWrapRange(token: string): string[] {
  const [startToken, endToken] = token.split('-').map(part => part.trim().toUpperCase());
  const start = resolveDowOrdinal(startToken);
  const end = resolveDowOrdinal(endToken);
  if (start === -1 || end === -1) {
    return [token];
  }
  if (start <= end) {
    return [token];
  }

  const expanded: string[] = [];
  for (let value = start; value <= 7; value++) {
    expanded.push(QUARTZ_DAY_ORDER[value - 1]);
  }
  for (let value = 1; value <= end; value++) {
    expanded.push(QUARTZ_DAY_ORDER[value - 1]);
  }
  return expanded;
}

function sanitizeDayOfMonthToken(token: string): string {
  const upper = token.toUpperCase();
  if (upper === 'L' || upper === 'LW') {
    return '1';
  }
  if (/^L-(\d+)(W)?$/i.test(upper)) {
    return '1';
  }
  if (/^(\d{1,2})W$/i.test(upper)) {
    return '1';
  }
  return upper;
}

function sanitizeDayOfWeekToken(token: string): string[] {
  const upper = token.toUpperCase();
  const nthMatch = upper.match(/^([0-7A-Z]+)#([1-5])$/i);
  if (nthMatch) {
    return [nthMatch[1]];
  }

  if (upper.includes('-')) {
    return expandDowWrapRange(upper);
  }

  return [upper];
}

function sanitizeExpressionForCronValidator(parts: string[]): string {
  const sanitized = [...parts];
  sanitized[3] = parts[3]
    .split(',')
    .map(token => sanitizeDayOfMonthToken(token.trim()))
    .join(',');
  sanitized[5] = parts[5]
    .split(',')
    .flatMap(token => sanitizeDayOfWeekToken(token.trim()))
    .join(',');
  return sanitized.join(' ');
}

function hasUnsupportedWildcardDowStep(dayOfWeek: string): boolean {
  return /^\*\/\d+$/i.test(dayOfWeek.trim());
}

function hasInvalidDayFieldCombination(dayOfMonth: string, dayOfWeek: string): boolean {
  if (dayOfMonth === '?' || dayOfWeek === '?') {
    return false;
  }

  return dayOfMonth !== '*' && dayOfWeek !== '*';
}

function hasPotentialCalendarMatch(dayOfMonth: string, monthField: string): boolean {
  if (dayOfMonth === '?' || dayOfMonth === '*') {
    return true;
  }

  const months = parseMonthField(monthField);
  if (months.length === 0) {
    return false;
  }

  const candidateYears = [2024, 2025];
  return candidateYears.some(year =>
    months.some(month => collectDayOfMonthMatches(dayOfMonth, year, month - 1).length > 0)
  );
}

function hasValidCronPartCount(parts: string[]): boolean {
  return parts.length >= 5 && parts.length <= 7;
}

function normalizePartsForQuartzValidation(parts: string[]): string[] {
  if (parts.length === 7) {
    return parts.slice(0, 6);
  }

  if (parts.length === 6) {
    return parts;
  }

  return ['0', ...parts];
}

function isValidYearField(yearField: string | undefined): boolean {
  if (!yearField || yearField === '*' || yearField === '?') {
    return true;
  }

  return parseNumericField(yearField, 1970, 2199).length > 0;
}

function passesCustomQuartzValidation(parts: string[]): boolean {
  const dayOfMonth = parts[3]?.toUpperCase() ?? '';
  const month = parts[4]?.toUpperCase() ?? '';
  const dayOfWeek = parts[5]?.toUpperCase() ?? '';

  return (
    !hasInvalidDayFieldCombination(dayOfMonth, dayOfWeek) &&
    !hasUnsupportedWildcardDowStep(dayOfWeek) &&
    hasPotentialCalendarMatch(dayOfMonth, month)
  );
}

function validateQuartzSpecialOrSanitized(parts: string[]): boolean {
  const special = validateQuartzSpecialFields(parts);
  if (special !== null) {
    return special;
  }

  return isValidCron(sanitizeExpressionForCronValidator(parts), CRON_VALIDATOR_OPTIONS);
}

function validateNearestWeekdayDom(parts: string[], dom: string, dow: string): boolean | null {
  if (dom === 'LW') {
    return dow === '?' && isValidCron(sanitizeDomField(parts).join(' '), CRON_VALIDATOR_OPTIONS);
  }

  const lOffsetWeekdayMatch = /^L-(\d+)W$/i.exec(dom);
  if (lOffsetWeekdayMatch) {
    const offset = parseInt(lOffsetWeekdayMatch[1], 10);
    if (offset < 0 || offset > 30) {
      return false;
    }
    return dow === '?' && isValidCron(sanitizeDomField(parts).join(' '), CRON_VALIDATOR_OPTIONS);
  }

  const nearestWeekdayMatch = /^(\d{1,2})W$/i.exec(dom);
  if (!nearestWeekdayMatch) {
    return null;
  }

  const day = parseInt(nearestWeekdayMatch[1], 10);
  if (day < 1 || day > 31) {
    return false;
  }

  return dow === '?' && isValidCron(sanitizeDomField(parts).join(' '), CRON_VALIDATOR_OPTIONS);
}

function validateLastDom(parts: string[], dom: string, dow: string): boolean | null {
  if (dom === 'L') {
    return dow === '?' && isValidCron(sanitizeDomField(parts).join(' '), CRON_VALIDATOR_OPTIONS);
  }
  const lOffsetMatch = /^L-(\d+)$/.exec(dom);
  if (!lOffsetMatch) return null;
  const offset = parseInt(lOffsetMatch[1], 10);
  if (offset < 0 || offset > 31) return false;
  return dow === '?' && isValidCron(sanitizeDomField(parts).join(' '), CRON_VALIDATOR_OPTIONS);
}

function validateLastWeekday(parts: string[], dow: string): boolean | null {
  const m = /^([0-7])L$/i.exec(dow);
  if (!m) return null;
  if (!VALID_DOW_TOKEN.test(m[1])) return false;
  return isValidCron(sanitizeDowField(parts, dow).join(' '), CRON_VALIDATOR_OPTIONS);
}

function validateNthWeekday(parts: string[], dow: string): boolean | null {
  const m = /^([0-7A-Za-z]+)#([1-5])$/i.exec(dow);
  if (!m) return null;
  if (!VALID_DOW_TOKEN.test(m[1])) return false;
  return isValidCron(sanitizeDowNthField(parts, dow).join(' '), CRON_VALIDATOR_OPTIONS);
}

function validateQuartzSpecialFields(parts: string[]): boolean | null {
  const dom = parts[3]?.toUpperCase() ?? '';
  const dow = parts[5]?.toUpperCase() ?? '';
  return (
    validateNearestWeekdayDom(parts, dom, dow) ??
    validateLastDom(parts, dom, dow) ??
    validateLastWeekday(parts, dow) ??
    validateNthWeekday(parts, dow) ??
    null
  );
}

export function isValidQuartzCronExpression(expression: string): boolean {
  const trimmed = expression.trim();
  if (!trimmed) return false;

  const parts = trimmed.split(/\s+/);
  if (!hasValidCronPartCount(parts)) {
    return false;
  }

  if (!isValidYearField(parts[6])) {
    return false;
  }

  const normalizedParts = normalizePartsForQuartzValidation(parts);

  if (!passesCustomQuartzValidation(normalizedParts)) {
    return false;
  }

  return validateQuartzSpecialOrSanitized(normalizedParts);
}
