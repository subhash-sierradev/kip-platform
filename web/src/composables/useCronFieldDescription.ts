import { toOrdinal } from './useCronFieldParsers';
import type { LocalOccurrence } from './useCronOccurrenceEngine';
import { parseQuartzCron } from './useCronOccurrenceEngine';

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

function joinNatural(items: string[]): string {
  if (items.length <= 1) return items[0] ?? '';
  if (items.length === 2) return `${items[0]} and ${items[1]}`;
  return `${items.slice(0, -1).join(', ')} and ${items[items.length - 1]}`;
}

export function getMostCommonLocalTime(occurrences: LocalOccurrence[]): string | null {
  if (occurrences.length === 0) return null;
  const counts = new Map<string, number>();
  for (const occ of occurrences) {
    counts.set(occ.localTime, (counts.get(occ.localTime) ?? 0) + 1);
  }
  return [...counts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ?? null;
}

function dowTokenIndex(token: string): number {
  const upper = token.toUpperCase();
  const nameEntry = Object.entries(DOW_NAMES).find(([k]) => k === upper && k.length > 1);
  if (nameEntry) {
    const order = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    return order.indexOf(nameEntry[1]);
  }
  const n = parseInt(token, 10);
  return Number.isNaN(n) ? -1 : n % 7;
}

function addDayRange(startToken: string, endToken: string, days: string[]): void {
  const si = dowTokenIndex(startToken);
  const ei = dowTokenIndex(endToken);
  if (si === -1 || ei === -1) return;
  for (let i = si; i <= ei; i++) {
    const name = DOW_NAMES[String(i)];
    if (name && !days.includes(name)) days.push(name);
  }
}

function expandDays(field: string): string[] {
  if (field === '*' || field === '?' || !field) return [];
  if (field.includes('#') || /[0-9A-Za-z]L$/i.test(field)) return [];

  const days: string[] = [];
  for (const token of field
    .toUpperCase()
    .split(',')
    .map(t => t.trim())) {
    if (token.includes('-')) {
      const [s, e] = token.split('-');
      addDayRange(s, e, days);
      continue;
    }
    const name = DOW_NAMES[token];
    if (name && !days.includes(name)) days.push(name);
  }
  return days;
}

function expandMonths(field: string): string[] {
  if (field === '*' || field === '?') return [];
  const months: string[] = [];
  for (const token of field
    .toUpperCase()
    .split(',')
    .map(t => t.trim())) {
    if (token.includes('/')) {
      const [startTok, stepTok] = token.split('/');
      const start = MONTH_NAMES[startTok]
        ? Object.values(MONTH_NAMES).indexOf(MONTH_NAMES[startTok]) + 1
        : parseInt(startTok, 10);
      const step = parseInt(stepTok, 10);
      for (let m = start; m <= 12; m += step) {
        const name = MONTH_NAMES[String(m)];
        if (name && !months.includes(name)) months.push(name);
      }
      continue;
    }
    const name = MONTH_NAMES[token];
    if (name && !months.includes(name)) months.push(name);
  }
  return months;
}

function detectSubdailyInterval(minF: string, hourF: string): { minutes: number } | null {
  if (!minF.includes('/')) return null;
  if (hourF !== '*' && hourF !== '?' && !hourF.includes('-')) return null;
  const step = parseInt(minF.split('/')[1], 10);
  return !Number.isNaN(step) && step > 0 && step < 60 ? { minutes: step } : null;
}

function detectHourlyInterval(minF: string, hourF: string): { hours: number } | null {
  if (!hourF.includes('/') || minF.includes('/')) return null;
  if (minF !== '0' && minF !== '00') return null;
  const step = parseInt(hourF.split('/')[1], 10);
  return !Number.isNaN(step) && step > 0 ? { hours: step } : null;
}

function parseNthWeekday(dowField: string): { ordinal: string; dayName: string } | null {
  const m = dowField.match(/^([0-7A-Za-z]+)#([1-5])$/i);
  if (!m) return null;
  const dayName = resolveQuartzDow(m[1]);
  const ordinals: Record<string, string> = {
    '1': 'first',
    '2': 'second',
    '3': 'third',
    '4': 'fourth',
    '5': 'fifth',
  };
  return dayName ? { ordinal: ordinals[m[2]] ?? m[2], dayName } : null;
}

function parseLastWeekday(dowField: string): string | null {
  const m = dowField.match(/^([0-7A-Za-z]+)L$/i);
  return m ? (resolveQuartzDow(m[1]) ?? null) : null;
}

function resolveQuartzDow(token: string): string | undefined {
  const upper = token.toUpperCase();
  // Named tokens (MON, TUE etc.) resolve directly
  if (DOW_NAMES[upper] && upper.length > 1) return DOW_NAMES[upper];
  const n = parseInt(token, 10);
  if (Number.isNaN(n)) return undefined;
  // Quartz numeric DOW: 1=Sunday, 2=Monday, 3=Tuesday, ..., 7=Saturday
  const quartzOrder = [
    'Sunday',
    'Monday',
    'Tuesday',
    'Wednesday',
    'Thursday',
    'Friday',
    'Saturday',
  ];
  return quartzOrder[n - 1];
}

function buildDayPhrase(dowF: string): string {
  const days = expandDays(dowF);
  if (days.length === 0) return '';
  return days.length === 1 ? `on ${days[0]}s` : `on ${joinNatural(days)}`;
}

function buildHourRangePhrase(
  hourF: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): string | null {
  if (!hourF.includes('-') || occurrences.length === 0) return null;
  const [startH, endH] = hourF.split('-').map(Number);
  if (Number.isNaN(startH) || Number.isNaN(endH)) return null;

  const anchor = occurrences[0].utcDate;
  const fmtUtcHour = (h: number): string => {
    const d = new Date(
      Date.UTC(anchor.getUTCFullYear(), anchor.getUTCMonth(), anchor.getUTCDate(), h, 0, 0)
    );
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    }).formatToParts(d);
    const hour = parts.find(p => p.type === 'hour')?.value ?? '';
    const minute = parts.find(p => p.type === 'minute')?.value ?? '00';
    const period = parts.find(p => p.type === 'dayPeriod')?.value ?? '';
    return minute === '00' ? `${hour} ${period}` : `${hour}:${minute} ${period}`;
  };

  return `between ${fmtUtcHour(startH)} and ${fmtUtcHour(endH)}`;
}

function tryAllDescriptions(
  minF: string,
  hourF: string,
  domF: string,
  monF: string,
  dowF: string,
  localTime: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): string | null {
  return (
    trySubdailyInterval(minF, hourF, dowF, timezone, occurrences) ??
    tryHourlyInterval(minF, hourF, dowF) ??
    tryNthWeekday(dowF, localTime) ??
    tryLastWeekday(dowF, localTime) ??
    tryLastDayOffset(domF, monF, localTime) ??
    tryLastDayOfMonth(domF, monF, localTime, timezone, occurrences) ??
    trySpecificDow(domF, dowF, monF, localTime, timezone, occurrences) ??
    trySpecificDom(domF, monF, localTime, timezone, occurrences) ??
    tryDailyWildcard(domF, dowF, localTime) ??
    null
  );
}

export function buildCronFieldDescription(
  cron: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): string | null {
  const parsed = parseQuartzCron(cron);
  if (!parsed.success || !parsed.data) return null;

  const {
    minute: minF,
    hour: hourF,
    dayOfMonth: domF,
    month: monF,
    dayOfWeek: dowF,
  } = parsed.data.fields;

  const localTime = getMostCommonLocalTime(occurrences);
  if (!localTime) return null;

  return tryAllDescriptions(minF, hourF, domF, monF, dowF, localTime, timezone, occurrences);
}

function trySubdailyInterval(
  minF: string,
  hourF: string,
  dowF: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): string | null {
  const sub = detectSubdailyInterval(minF, hourF);
  if (!sub) return null;
  const label = `every ${sub.minutes} minute${sub.minutes !== 1 ? 's' : ''}`;
  const rangePhrase = buildHourRangePhrase(hourF, timezone, occurrences);
  const dayPhrase = buildDayPhrase(dowF);
  const parts = [label, rangePhrase, dayPhrase ? dayPhrase : null].filter(Boolean);
  return `Runs ${parts.join(' ')}`;
}

function tryHourlyInterval(minF: string, hourF: string, dowF: string): string | null {
  const h = detectHourlyInterval(minF, hourF);
  if (!h) return null;
  const dayPhrase = buildDayPhrase(dowF);
  return `Runs every ${h.hours} hour${h.hours !== 1 ? 's' : ''}${dayPhrase ? ` ${dayPhrase}` : ''}`;
}

function tryNthWeekday(dowF: string, localTime: string): string | null {
  const nth = parseNthWeekday(dowF);
  return nth ? `Runs on the ${nth.ordinal} ${nth.dayName} of every month at ${localTime}` : null;
}

function tryLastWeekday(dowF: string, localTime: string): string | null {
  const last = parseLastWeekday(dowF);
  return last ? `Runs on the last ${last} of every month at ${localTime}` : null;
}

function getLocalMonthNames(occurrences: LocalOccurrence[]): string[] {
  const uniqueMonths = Array.from(new Set(occurrences.map(o => o.localDate.slice(5, 7)))).sort();
  return uniqueMonths.map(mm =>
    new Intl.DateTimeFormat('en-US', { month: 'long', timeZone: 'UTC' }).format(
      new Date(Date.UTC(2024, Number(mm) - 1, 1))
    )
  );
}

function getLocalDayNames(occurrences: LocalOccurrence[]): string[] | null {
  const dayOrder = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  const localDows = new Set<number>();
  let anyShifted = false;
  for (const occ of occurrences) {
    const utcDow = occ.utcDate.getUTCDay();
    const localDow = new Date(`${occ.localDate}T00:00:00Z`).getUTCDay();
    localDows.add(localDow);
    if (utcDow !== localDow) anyShifted = true;
  }
  if (!anyShifted) return null;
  return [...localDows].sort((a, b) => a - b).map(d => dayOrder[d]);
}

function getConsistentLocalDom(occurrences: LocalOccurrence[]): number | null {
  if (occurrences.length === 0) return null;
  const localDoms = new Set(occurrences.map(o => Number(o.localDate.slice(8))));
  const utcDoms = new Set(occurrences.map(o => o.utcDate.getUTCDate()));
  if (localDoms.size !== 1) return null;
  const localDom = [...localDoms][0];
  if (utcDoms.size === 1 && [...utcDoms][0] === localDom) return null;
  return localDom;
}

function getLocalDateLabels(occurrences: LocalOccurrence[]): string[] {
  const uniquePairs = Array.from(new Set(occurrences.map(o => o.localDate.slice(5)))).sort();
  const localDays = new Set(uniquePairs.map(p => Number(p.slice(3))));
  const utcDays = new Set(occurrences.map(o => o.utcDate.getUTCDate()));
  const shifted = [...localDays].some(d => !utcDays.has(d));
  if (!shifted) return [];

  return uniquePairs.map(mmdd => {
    const [mm, dd] = mmdd.split('-').map(Number);
    const monthName = new Intl.DateTimeFormat('en-US', { month: 'long', timeZone: 'UTC' }).format(
      new Date(Date.UTC(2024, mm - 1, 1))
    );
    return `${monthName} ${toOrdinal(dd)}`;
  });
}

function tryLastDayOffset(domF: string, monF: string, localTime: string): string | null {
  const m = domF.match(/^L-(\d+)$/i);
  if (!m) return null;
  const n = parseInt(m[1], 10);
  const label = n === 1 ? 'second-to-last' : `${toOrdinal(n + 1)}-to-last`;
  const months = expandMonths(monF);
  return months.length > 0
    ? `Runs at ${localTime} on the ${label} day of ${joinNatural(months)} every year`
    : `Runs at ${localTime} on the ${label} day of every month`;
}

function tryLastDayOfMonth(
  domF: string,
  monF: string,
  localTime: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): string | null {
  if (domF.toUpperCase() !== 'L') return null;
  // Use local occurrence months only when a timezone shift actually changes the month
  const months = expandMonths(monF);
  if (months.length > 0) {
    const localMonths = getLocalMonthNames(occurrences);
    const shifted = localMonths.some(m => !months.includes(m));
    const displayMonths = shifted ? localMonths : months;
    return `Runs at ${localTime} on the last day of ${joinNatural(displayMonths)}`;
  }
  return `Runs at ${localTime} on the last day of every month`;
}

function trySpecificDow(
  domF: string,
  dowF: string,
  monF: string,
  localTime: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): string | null {
  const utcDays = expandDays(dowF);
  if (utcDays.length === 0) return null;
  const shiftedDays = getLocalDayNames(occurrences);
  const days = shiftedDays ?? utcDays;
  const isDomWild = domF === '*' || domF === '?';
  const months = expandMonths(monF);
  const dayPhrase = days.length === 1 ? `every ${days[0]}` : `every ${joinNatural(days)}`;
  if (isDomWild) {
    return months.length > 0
      ? `Runs ${dayPhrase} in ${joinNatural(months)} at ${localTime}`
      : `Runs ${dayPhrase} at ${localTime}`;
  }
  return `Runs ${dayPhrase} at ${localTime}`;
}

function trySpecificDom(
  domF: string,
  monF: string,
  localTime: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): string | null {
  const domNum = parseInt(domF, 10);
  if (Number.isNaN(domNum)) return null;

  // Check if local dates differ from UTC dates (timezone day-shift)
  const localDates = getLocalDateLabels(occurrences);
  if (localDates.length > 0 && localDates.length <= 6) {
    // Dates have shifted — describe using actual local occurrence dates
    return `Runs at ${localTime} on ${joinNatural(localDates)}`;
  }

  // For recurring monthly patterns check if all local dates share a consistent shifted DOM
  const effectiveDom = getConsistentLocalDom(occurrences) ?? domNum;
  const months = expandMonths(monF);
  if (months.length > 0) {
    const monthPhrase =
      months.length === 1
        ? `${months[0]} ${toOrdinal(effectiveDom)}`
        : `the ${toOrdinal(effectiveDom)} of ${joinNatural(months)}`;
    return `Runs at ${localTime} on ${monthPhrase} every year`;
  }
  const isMonthWild = monF === '*' || monF === '?';
  return isMonthWild
    ? `Runs at ${localTime} on the ${toOrdinal(effectiveDom)} of every month`
    : null;
}

function tryDailyWildcard(domF: string, dowF: string, localTime: string): string | null {
  const isDomWild = domF === '*' || domF === '?';
  const days = expandDays(dowF);
  return isDomWild && days.length === 0 ? `Runs every day at ${localTime}` : null;
}
