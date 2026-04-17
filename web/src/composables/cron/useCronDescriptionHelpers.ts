import { parseNumericField } from './useCronFieldParsers';
import type { LocalOccurrence } from './useCronOccurrenceEngine';

const DOW_NAMES: Record<string, string> = {
  '0': 'Sunday',
  '1': 'Monday',
  '2': 'Tuesday',
  '3': 'Wednesday',
  '4': 'Thursday',
  '5': 'Friday',
  '6': 'Saturday',
  '7': 'Sunday',
  SUN: 'Sunday',
  MON: 'Monday',
  TUE: 'Tuesday',
  WED: 'Wednesday',
  THU: 'Thursday',
  FRI: 'Friday',
  SAT: 'Saturday',
};

const MONTH_NAMES: Record<string, string> = {
  '1': 'January',
  '2': 'February',
  '3': 'March',
  '4': 'April',
  '5': 'May',
  '6': 'June',
  '7': 'July',
  '8': 'August',
  '9': 'September',
  '10': 'October',
  '11': 'November',
  '12': 'December',
  JAN: 'January',
  FEB: 'February',
  MAR: 'March',
  APR: 'April',
  MAY: 'May',
  JUN: 'June',
  JUL: 'July',
  AUG: 'August',
  SEP: 'September',
  OCT: 'October',
  NOV: 'November',
  DEC: 'December',
};

export function joinNatural(items: string[]): string {
  if (items.length <= 1) return items[0] ?? '';
  if (items.length === 2) return `${items[0]} and ${items[1]}`;
  return `${items.slice(0, -1).join(', ')} and ${items[items.length - 1]}`;
}

export function isWildcardHourField(hourF: string): boolean {
  return hourF === '*' || hourF === '?';
}

export function isStrictNumericField(value: string): boolean {
  return /^\d+$/.test(value.trim());
}

export function parseLocalMinute(localTime: string): number | null {
  const match = localTime.match(/^(\d{1,2}):(\d{2})\s([AP]M)$/);
  if (!match) {
    return null;
  }

  return parseInt(match[2], 10);
}

export function formatMinutePastHour(minute: number): string {
  return minute === 0
    ? 'on the hour'
    : `at ${String(minute).padStart(2, '0')} minutes past the hour`;
}

function formatUtcSlotAsLocalTime(
  timezone: string,
  anchorUtcDate: Date,
  hour: number,
  minute: number
): string {
  const slot = new Date(
    Date.UTC(
      anchorUtcDate.getUTCFullYear(),
      anchorUtcDate.getUTCMonth(),
      anchorUtcDate.getUTCDate(),
      hour,
      minute,
      0
    )
  );
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: timezone,
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  }).formatToParts(slot);
  const localHour = parts.find(part => part.type === 'hour')?.value ?? '';
  const localMinute = parts.find(part => part.type === 'minute')?.value ?? '00';
  const period = parts.find(part => part.type === 'dayPeriod')?.value ?? '';
  return localMinute === '00' ? `${localHour} ${period}` : `${localHour}:${localMinute} ${period}`;
}

export function getRepresentativeLocalTimes(
  minuteTokens: number[],
  timezone: string,
  occurrences: LocalOccurrence[]
): string[] {
  if (occurrences.length === 0) {
    return [];
  }

  const anchorUtcDate = occurrences[0].utcDate;
  return minuteTokens.map(minute => formatUtcSlotAsLocalTime(timezone, anchorUtcDate, 0, minute));
}

export function getBoundedWindowTimeRange(
  minF: string,
  hourF: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): { start: string; end: string } | null {
  if (occurrences.length === 0 || isWildcardHourField(hourF) || hourF.includes('/')) {
    return null;
  }

  const hours = parseNumericField(hourF, 0, 23);
  const minutes = parseNumericField(minF, 0, 59);
  if (hours.length === 0 || minutes.length === 0) {
    return null;
  }

  const anchorUtcDate = occurrences[0].utcDate;
  return {
    start: formatUtcSlotAsLocalTime(timezone, anchorUtcDate, hours[0], minutes[0]),
    end: formatUtcSlotAsLocalTime(
      timezone,
      anchorUtcDate,
      hours[hours.length - 1],
      minutes[minutes.length - 1]
    ),
  };
}

function dowTokenIndex(token: string): number {
  const upper = token.toUpperCase();
  const nameEntry = Object.entries(DOW_NAMES).find(([key]) => key === upper && key.length > 1);
  if (nameEntry) {
    const order = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    return order.indexOf(nameEntry[1]);
  }
  const parsed = parseInt(token, 10);
  return Number.isNaN(parsed) ? -1 : parsed % 7;
}

function addDayRange(startToken: string, endToken: string, days: string[]): void {
  const startIndex = dowTokenIndex(startToken);
  const endIndex = dowTokenIndex(endToken);
  if (startIndex === -1 || endIndex === -1) return;
  for (let index = startIndex; index <= endIndex; index++) {
    const name = DOW_NAMES[String(index)];
    if (name && !days.includes(name)) days.push(name);
  }
}

export function expandDays(field: string): string[] {
  if (field === '*' || field === '?' || !field) return [];
  if (field.includes('#') || /[0-9A-Za-z]L$/i.test(field)) return [];

  const days: string[] = [];
  for (const token of field
    .toUpperCase()
    .split(',')
    .map(part => part.trim())) {
    if (token.includes('-')) {
      const [start, end] = token.split('-');
      addDayRange(start, end, days);
      continue;
    }
    const name = DOW_NAMES[token];
    if (name && !days.includes(name)) days.push(name);
  }
  return days;
}

export function expandMonths(field: string): string[] {
  if (field === '*' || field === '?') return [];
  const months: string[] = [];
  for (const token of field
    .toUpperCase()
    .split(',')
    .map(part => part.trim())) {
    if (token.includes('/')) {
      const [startToken, stepToken] = token.split('/');
      const start = MONTH_NAMES[startToken]
        ? Object.values(MONTH_NAMES).indexOf(MONTH_NAMES[startToken]) + 1
        : parseInt(startToken, 10);
      const step = parseInt(stepToken, 10);
      for (let month = start; month <= 12; month += step) {
        const name = MONTH_NAMES[String(month)];
        if (name && !months.includes(name)) months.push(name);
      }
      continue;
    }
    const name = MONTH_NAMES[token];
    if (name && !months.includes(name)) months.push(name);
  }
  return months;
}

export function detectSubdailyInterval(minF: string, hourF: string): { minutes: number } | null {
  if (!minF.includes('/')) return null;
  if (hourF.includes('/')) return null;
  const step = parseInt(minF.split('/')[1], 10);
  return !Number.isNaN(step) && step > 0 && step < 60 ? { minutes: step } : null;
}

export function resolveQuartzDow(token: string): string | undefined {
  const upper = token.toUpperCase();
  if (DOW_NAMES[upper] && upper.length > 1) return DOW_NAMES[upper];
  const parsed = parseInt(token, 10);
  if (Number.isNaN(parsed)) return undefined;
  const quartzOrder = [
    'Sunday',
    'Monday',
    'Tuesday',
    'Wednesday',
    'Thursday',
    'Friday',
    'Saturday',
  ];
  return quartzOrder[parsed - 1];
}

function getLocalMonthNames(occurrences: LocalOccurrence[]): string[] {
  const uniqueMonths = Array.from(
    new Set(occurrences.map(occurrence => occurrence.localDate.slice(5, 7)))
  ).sort();
  return uniqueMonths.map(month =>
    new Intl.DateTimeFormat('en-US', { month: 'long', timeZone: 'UTC' }).format(
      new Date(Date.UTC(2024, Number(month) - 1, 1))
    )
  );
}

export function getDisplayMonths(monF: string, occurrences: LocalOccurrence[]): string[] {
  const months = expandMonths(monF);
  if (months.length === 0) {
    return months;
  }

  const localMonths = getLocalMonthNames(occurrences);
  const shifted = localMonths.some(month => !months.includes(month));
  return shifted ? localMonths : months;
}

export function getLocalDayNames(occurrences: LocalOccurrence[]): string[] | null {
  const dayOrder = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  const localDaysOfWeek = new Set<number>();
  let anyShifted = false;
  for (const occurrence of occurrences) {
    const utcDay = occurrence.utcDate.getUTCDay();
    const localDay = new Date(`${occurrence.localDate}T00:00:00Z`).getUTCDay();
    localDaysOfWeek.add(localDay);
    if (utcDay !== localDay) anyShifted = true;
  }
  if (!anyShifted) return null;
  return [...localDaysOfWeek].sort((left, right) => left - right).map(day => dayOrder[day]);
}
