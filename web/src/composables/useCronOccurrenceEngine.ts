import {
  matchesLastWeekday,
  matchesNthWeekday,
  normalizeOffsetLabel,
  parseDayOfWeekField,
  parseMonthField,
  parseNumericField,
} from './useCronFieldParsers';

export interface ParsedCron {
  original: string;
  fields: {
    second: string;
    minute: string;
    hour: string;
    dayOfMonth: string;
    month: string;
    dayOfWeek: string;
  };
  secondTokens: number[] | string;
  minuteTokens: number[] | string;
  hourTokens: number[] | string;
  domTokens: string[];
  monthTokens: string[];
  dowTokens: string[];
  isQuartzSpecial: boolean;
  isSameDayOfWeekAndMonth: boolean;
}

export interface Result<T> {
  success: boolean;
  data?: T;
  error?: string;
}

export interface LocalOccurrence {
  utcDate: Date;
  localDate: string;
  localTime: string;
  offset: string;
}

export interface PatternDescription {
  type: string;
  occurrences: LocalOccurrence[];
  uniqueDates: string[];
  uniqueTimes: string[];
  uniqueOffsets: string[];
  hasTimeVariance: boolean;
  hasOffsetVariance: boolean;
  regularityPattern?: string;
}

export function parseQuartzCron(cron: string): Result<ParsedCron> {
  const trimmed = cron?.trim();
  if (!trimmed) {
    return { success: false, error: 'Cron expression cannot be empty' };
  }

  const parts = trimmed.split(/\s+/);
  if (parts.length < 5 || parts.length > 6) {
    return { success: false, error: 'Cron must have 5 or 6 fields' };
  }

  const fields = normalizeQuartzParts(parts);
  if (!fields) {
    return { success: false, error: 'Missing required cron fields' };
  }

  return { success: true, data: buildParsedCron(cron, fields) };
}

export function validateTimeZone(timezone: string): Result<string> {
  try {
    new Intl.DateTimeFormat('en-US', { timeZone: timezone }).format(new Date());
    return { success: true, data: timezone };
  } catch {
    return { success: false, error: `Invalid timezone: ${timezone}` };
  }
}

export function generateOccurrences(
  cron: string,
  startDate: Date,
  endDate: Date,
  timezone: string,
  maxOccurrences: number = 365
): Result<LocalOccurrence[]> {
  const tzResult = validateTimeZone(timezone);
  if (!tzResult.success) {
    return { success: false, error: tzResult.error };
  }

  const parseResult = parseQuartzCron(cron);
  if (!parseResult.success) {
    return { success: false, error: parseResult.error };
  }

  try {
    const parsed = parseResult.data!;
    const utcOccurrences = generateUtcOccurrences(parsed, startDate, endDate, maxOccurrences);
    const localOccurrences = utcOccurrences
      .map(utcDate => convertToLocalOccurrence(utcDate, timezone))
      .filter((occ): occ is LocalOccurrence => occ !== null);

    return { success: true, data: localOccurrences };
  } catch (error) {
    return {
      success: false,
      error: `Failed to generate occurrences: ${error instanceof Error ? error.message : String(error)}`,
    };
  }
}

export function analyzeLocalPattern(occurrences: LocalOccurrence[]): PatternDescription {
  if (occurrences.length === 0) {
    return {
      type: 'empty',
      occurrences: [],
      uniqueDates: [],
      uniqueTimes: [],
      uniqueOffsets: [],
      hasTimeVariance: false,
      hasOffsetVariance: false,
    };
  }

  const uniqueDates = Array.from(new Set(occurrences.map(o => o.localDate)));
  const uniqueTimes = Array.from(new Set(occurrences.map(o => o.localTime)));
  const uniqueOffsets = Array.from(new Set(occurrences.map(o => o.offset)));

  const hasTimeVariance = uniqueTimes.length > 1;
  const hasOffsetVariance = uniqueOffsets.length > 1;

  let type = 'custom';
  let regularityPattern: string | undefined;

  if (uniqueTimes.length === 1) {
    if (uniqueDates.length === 1) {
      type = 'single-occurrence';
    } else if (isConsecutiveDays(uniqueDates)) {
      type = 'daily';
    } else if (isWeeklyPattern(occurrences)) {
      type = 'weekly';
    } else if (isMonthlyPattern(uniqueDates)) {
      type = 'monthly';
    } else {
      type = 'interval';
      regularityPattern = detectIntervalPattern(uniqueDates);
    }
  } else {
    type = 'time-varies';
  }

  return {
    type,
    occurrences,
    uniqueDates,
    uniqueTimes,
    uniqueOffsets,
    hasTimeVariance,
    hasOffsetVariance,
    regularityPattern,
  };
}

function normalizeQuartzParts(
  parts: string[]
): { sec: string; min: string; hour: string; dom: string; mon: string; dow: string } | null {
  const [sec, min, hour, dom, mon, dow] = parts.length === 6 ? parts : ['0', ...parts];
  if (!min || !hour || !dom || !mon || !dow) {
    return null;
  }
  return { sec, min, hour, dom, mon, dow };
}

function buildParsedCron(
  cron: string,
  fields: { sec: string; min: string; hour: string; dom: string; mon: string; dow: string }
): ParsedCron {
  const { sec, min, hour, dom, mon, dow } = fields;
  return {
    original: cron,
    fields: {
      second: sec,
      minute: min,
      hour,
      dayOfMonth: dom,
      month: mon,
      dayOfWeek: dow,
    },
    secondTokens: tokenizeFieldToNumbers(sec),
    minuteTokens: tokenizeFieldToNumbers(min),
    hourTokens: tokenizeFieldToNumbers(hour),
    domTokens: dom.split(',').map(t => t.trim()),
    monthTokens: mon.split(',').map(t => t.trim()),
    dowTokens: dow.split(',').map(t => t.trim()),
    isQuartzSpecial: /[LW#]/.test(dom) || /[L#]/.test(dow),
    isSameDayOfWeekAndMonth: dom !== '?' && dom !== '*' && dow !== '?' && dow !== '*',
  };
}

function tokenizeFieldToNumbers(field: string): number[] | string {
  if (field === '*' || field === '?') {
    return '*';
  }
  if (!field) {
    return [];
  }
  if (field.includes('/')) {
    return field;
  }

  const numbers: number[] = [];
  for (const token of field.split(',').map(t => t.trim())) {
    if (token.includes('-')) {
      const [start, end] = token.split('-').map(t => parseInt(t.trim(), 10));
      for (let i = start; i <= end; i++) {
        if (!numbers.includes(i)) {
          numbers.push(i);
        }
      }
      continue;
    }

    const value = parseInt(token, 10);
    if (!Number.isNaN(value) && !numbers.includes(value)) {
      numbers.push(value);
    }
  }

  return numbers.length > 0 ? numbers : field;
}

function resolveDowMatch(
  date: number,
  utcDay: number,
  month: number,
  year: number,
  dowField: string,
  dows: number[],
  nthMatch: RegExpMatchArray | null,
  lastMatch: RegExpMatchArray | null
): boolean {
  if (nthMatch) return matchesNthWeekday(date, utcDay, nthMatch[1], parseInt(nthMatch[2], 10));
  if (lastMatch) return matchesLastWeekday(date, utcDay, month, year, lastMatch[1]);
  return dows.includes(utcDay) || dows.length === 0;
}

function generateUtcOccurrences(
  parsed: ParsedCron,
  startDate: Date,
  endDate: Date,
  maxOccurrences: number
): Date[] {
  const occurrences: Date[] = [];
  const current = new Date(startDate);
  const hours = parseNumericField(parsed.fields.hour, 0, 23);
  const minutes = parseNumericField(parsed.fields.minute, 0, 59);
  const months = parseMonthField(parsed.fields.month);
  const dowField = parsed.fields.dayOfWeek;
  const nthMatch = dowField.match(/^([0-7A-Za-z]+)#([1-5])$/i);
  const lastMatch = dowField.match(/^([0-7A-Za-z]+)L$/i);
  const dows = nthMatch || lastMatch ? [] : parseDayOfWeekField(dowField);

  while (current <= endDate && occurrences.length < maxOccurrences) {
    const year = current.getUTCFullYear();
    const month = current.getUTCMonth();
    const date = current.getUTCDate();
    const day = current.getUTCDay();
    const matchesMonth = months.includes(month + 1);
    const matchesDow = resolveDowMatch(date, day, month, year, dowField, dows, nthMatch, lastMatch);
    const matchesDom = matchesDayOfMonth(date, parsed.fields.dayOfMonth, year, month);
    const domDowLogic = parsed.isSameDayOfWeekAndMonth
      ? matchesDom || matchesDow
      : matchesDom && matchesDow;

    if (matchesMonth && domDowLogic) {
      addOccurrencesForDay(
        occurrences,
        year,
        month,
        date,
        hours,
        minutes,
        startDate,
        endDate,
        maxOccurrences
      );
    }
    current.setUTCDate(current.getUTCDate() + 1);
  }

  return occurrences;
}

function addOccurrencesForDay(
  occurrences: Date[],
  year: number,
  month: number,
  date: number,
  hourTokens: number[],
  minuteTokens: number[],
  startDate: Date,
  endDate: Date,
  maxOccurrences: number
): void {
  for (const hour of hourTokens) {
    for (const minute of minuteTokens) {
      if (occurrences.length >= maxOccurrences) {
        return;
      }
      const occurrence = new Date(Date.UTC(year, month, date, hour, minute, 0));
      if (occurrence >= startDate && occurrence <= endDate) {
        occurrences.push(occurrence);
      }
    }
  }
}

function matchesDayOfMonth(date: number, domField: string, year: number, month: number): boolean {
  if (domField === '*' || domField === '?') {
    return true;
  }
  if (domField === 'L') {
    return date === new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  }

  // L-n: n days before the last day of the month
  const lnMatch = domField.toUpperCase().match(/^L-(\d+)$/);
  if (lnMatch) {
    const lastDay = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
    return date === lastDay - parseInt(lnMatch[1], 10);
  }

  for (const token of domField.split(',').map(t => t.trim())) {
    if (token.includes('-')) {
      const [start, end] = token.split('-').map(t => parseInt(t.trim(), 10));
      if (date >= start && date <= end) {
        return true;
      }
      continue;
    }

    const value = parseInt(token, 10);
    if (value === date) {
      return true;
    }
  }

  return false;
}

function convertToLocalOccurrence(utcDate: Date, timezone: string): LocalOccurrence | null {
  try {
    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: 'numeric',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
      timeZoneName: 'shortOffset',
    });

    const parts = formatter.formatToParts(utcDate);
    const extracted = extractDateTimeParts(parts);
    if (!extracted) {
      return null;
    }

    const localDate = `${extracted.year}-${extracted.month}-${extracted.day}`;
    return {
      utcDate,
      localDate,
      localTime: formatLocalTime(extracted.hour, extracted.minute),
      offset: normalizeOffsetLabel(extracted.offset),
    };
  } catch {
    return null;
  }
}

function extractDateTimeParts(parts: Intl.DateTimeFormatPart[]): {
  year: string;
  month: string;
  day: string;
  hour: string;
  minute: string;
  offset: string;
} | null {
  const values = {
    year: parts.find(p => p.type === 'year')?.value,
    month: parts.find(p => p.type === 'month')?.value,
    day: parts.find(p => p.type === 'day')?.value,
    hour: parts.find(p => p.type === 'hour')?.value,
    minute: parts.find(p => p.type === 'minute')?.value,
    offset: parts.find(p => p.type === 'timeZoneName')?.value,
  };

  if (Object.values(values).some(v => !v)) {
    return null;
  }

  return values as {
    year: string;
    month: string;
    day: string;
    hour: string;
    minute: string;
    offset: string;
  };
}

function formatLocalTime(hour: string, minute: string): string {
  const parsed = parseInt(hour, 10);
  // Intl with hour12:false returns "24" for midnight in some environments (CLDR spec)
  const hour24 = parsed === 24 ? 0 : parsed;
  const hour12 = hour24 % 12 || 12;
  const period = hour24 >= 12 ? 'PM' : 'AM';
  return `${hour12}:${minute} ${period}`;
}

function isConsecutiveDays(dates: string[]): boolean {
  if (dates.length < 2) {
    return false;
  }
  const sorted = dates.slice().sort();
  const first = new Date(`${sorted[0]}T00:00:00Z`);
  const last = new Date(`${sorted[sorted.length - 1]}T00:00:00Z`);
  const expected = Math.ceil((last.getTime() - first.getTime()) / (1000 * 60 * 60 * 24)) + 1;
  return sorted.length === expected;
}

function isWeeklyPattern(occurrences: LocalOccurrence[]): boolean {
  if (occurrences.length < 7) {
    return false;
  }
  const dows = occurrences.map(o => new Date(`${o.localDate}T00:00:00Z`).getUTCDay());
  const uniqueDows = new Set(dows);
  if (uniqueDows.size > 5) {
    return false;
  }

  const firstDate = new Date(`${occurrences[0].localDate}T00:00:00Z`);
  const match = occurrences
    .slice(1)
    .map(o => new Date(`${o.localDate}T00:00:00Z`))
    .find(d => d.getUTCDay() === firstDate.getUTCDay());

  if (!match) {
    return false;
  }

  const diff = Math.abs((match.getTime() - firstDate.getTime()) / (1000 * 60 * 60 * 24));
  return diff >= 6 && diff <= 8;
}

function isMonthlyPattern(dates: string[]): boolean {
  if (dates.length < 2) {
    return false;
  }
  const uniqueDays = new Set(dates.map(d => new Date(`${d}T00:00:00Z`).getUTCDate()));
  return uniqueDays.size <= 3;
}

function detectIntervalPattern(dates: string[]): string {
  if (dates.length < 2) {
    return '';
  }

  const sorted = dates.slice().sort();
  const intervals: number[] = [];
  for (let i = 1; i < sorted.length; i++) {
    const curr = new Date(sorted[i]);
    const prev = new Date(sorted[i - 1]);
    const diffDays = Math.round((curr.getTime() - prev.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays > 0) {
      intervals.push(diffDays);
    }
  }

  if (intervals.length === 0) {
    return '';
  }

  const avgInterval = Math.round(intervals.reduce((a, b) => a + b, 0) / intervals.length);
  const consistent = intervals.every(i => Math.abs(i - avgInterval) <= 1);
  return consistent ? `every ${avgInterval} day${avgInterval > 1 ? 's' : ''}` : '';
}
