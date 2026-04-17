import {
  detectSubdailyInterval,
  expandDays,
  expandMonths,
  formatMinutePastHour,
  getBoundedWindowTimeRange,
  getDisplayMonths,
  getLocalDayNames,
  getRepresentativeLocalTimes,
  isStrictNumericField,
  isWildcardHourField,
  joinNatural,
  parseLocalMinute,
  resolveQuartzDow,
} from './useCronDescriptionHelpers';
import { toOrdinal } from './useCronFieldParsers';
import type { LocalOccurrence } from './useCronOccurrenceEngine';
import { parseQuartzCron } from './useCronOccurrenceEngine';

export function getMostCommonLocalTime(occurrences: LocalOccurrence[]): string | null {
  if (occurrences.length === 0) return null;
  const counts = new Map<string, number>();
  for (const occ of occurrences) {
    counts.set(occ.localTime, (counts.get(occ.localTime) ?? 0) + 1);
  }
  return [...counts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] ?? null;
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

function buildDayPhrase(dowF: string): string {
  const days = expandDays(dowF);
  if (days.length === 0) return '';
  return days.length === 1 ? `on ${days[0]}s` : `on ${joinNatural(days)}`;
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
  const attempts = [
    () => trySubdailyInterval(minF, hourF, dowF, timezone, occurrences),
    () => tryHourlyMinuteMark(minF, hourF, dowF, occurrences),
    () => tryHourlyMinuteList(minF, hourF, dowF, timezone, occurrences),
    () => tryHourlyWindow(minF, hourF, dowF, timezone, occurrences),
    () => tryHourlyInterval(minF, hourF, dowF),
    () => tryNthWeekday(dowF, localTime),
    () => tryLastWeekday(dowF, localTime),
    () => tryNearestWeekday(domF, monF, localTime),
    () => tryLastDayOffset(domF, monF, localTime),
    () => tryLastDayOfMonth(domF, monF, localTime, timezone, occurrences),
    () => trySpecificDow(domF, dowF, monF, localTime, timezone, occurrences),
    () => tryDayOfMonthStep(domF, monF, localTime),
    () => trySpecificDomList(domF, monF, localTime),
    () => trySpecificDom(domF, monF, localTime, timezone, occurrences),
    () => tryDailyWildcard(domF, dowF, localTime),
  ];

  for (const attempt of attempts) {
    const description = attempt();
    if (description !== null) {
      return description;
    }
  }

  return null;
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
  const boundedWindow = getBoundedWindowTimeRange(minF, hourF, timezone, occurrences);
  const rangePhrase = boundedWindow
    ? `between ${boundedWindow.start} and ${boundedWindow.end}`
    : null;
  const dayPhrase = buildDayPhrase(dowF);
  const parts = [label, rangePhrase, dayPhrase ? dayPhrase : null].filter(Boolean);
  return `Runs ${parts.join(' ')}`;
}

function tryHourlyMinuteMark(
  minF: string,
  hourF: string,
  dowF: string,
  occurrences: LocalOccurrence[]
): string | null {
  if (
    !isWildcardHourField(hourF) ||
    minF.includes('/') ||
    minF.includes(',') ||
    !isStrictNumericField(minF)
  ) {
    return null;
  }

  const localMinute = parseLocalMinute(occurrences[0]?.localTime ?? '');
  if (localMinute === null) {
    return null;
  }

  const dayPhrase = buildDayPhrase(dowF);
  return `Runs every hour ${formatMinutePastHour(localMinute)}${dayPhrase ? ` ${dayPhrase}` : ''}`;
}

function tryHourlyMinuteList(
  minF: string,
  hourF: string,
  dowF: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): string | null {
  if (!isWildcardHourField(hourF) || minF.includes('/') || !minF.includes(',')) {
    return null;
  }

  const minuteTokens = minF
    .split(',')
    .map(token => parseInt(token.trim(), 10))
    .filter(token => !Number.isNaN(token));
  if (minuteTokens.length < 2) {
    return null;
  }

  const times = getRepresentativeLocalTimes(minuteTokens, timezone, occurrences);
  const dayPhrase = buildDayPhrase(dowF);
  return `Runs every hour at ${joinNatural(times)}${dayPhrase ? ` ${dayPhrase}` : ''}`;
}

function tryHourlyWindow(
  minF: string,
  hourF: string,
  dowF: string,
  timezone: string,
  occurrences: LocalOccurrence[]
): string | null {
  if (
    !hourF.includes('-') ||
    minF.includes('/') ||
    minF.includes(',') ||
    !isStrictNumericField(minF)
  ) {
    return null;
  }

  const boundedWindow = getBoundedWindowTimeRange(minF, hourF, timezone, occurrences);
  if (!boundedWindow) {
    return null;
  }

  const dayPhrase = buildDayPhrase(dowF);
  return `Runs every hour between ${boundedWindow.start} and ${boundedWindow.end}${dayPhrase ? ` ${dayPhrase}` : ''}`;
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

function isLastLocalDayOfMonth(localDate: string): boolean {
  const [year, month, day] = localDate.split('-').map(Number);
  const lastDayOfMonth = new Date(Date.UTC(year, month, 0)).getUTCDate();
  return day === lastDayOfMonth;
}

function areAllLocalDatesLastDayOfMonth(occurrences: LocalOccurrence[]): boolean {
  return occurrences.length > 0 && occurrences.every(occ => isLastLocalDayOfMonth(occ.localDate));
}

function isFirstLocalDayOfMonth(localDate: string): boolean {
  return localDate.endsWith('-01');
}

function areAllLocalDatesFirstDayOfMonth(occurrences: LocalOccurrence[]): boolean {
  return occurrences.length > 0 && occurrences.every(occ => isFirstLocalDayOfMonth(occ.localDate));
}

function getDaysFromLocalMonthEnd(localDate: string): number {
  const [year, month, day] = localDate.split('-').map(Number);
  const lastDayOfMonth = new Date(Date.UTC(year, month, 0)).getUTCDate();
  return lastDayOfMonth - day;
}

function getConsistentDaysFromLocalMonthEnd(occurrences: LocalOccurrence[]): number | null {
  if (occurrences.length === 0) {
    return null;
  }

  const deltas = new Set(occurrences.map(occ => getDaysFromLocalMonthEnd(occ.localDate)));
  return deltas.size === 1 ? [...deltas][0] : null;
}

function formatLocalMonthEndLabel(daysFromMonthEnd: number): string {
  if (daysFromMonthEnd === 0) {
    return 'last';
  }

  if (daysFromMonthEnd === 1) {
    return 'second-to-last';
  }

  return `${toOrdinal(daysFromMonthEnd + 1)}-to-last`;
}

function tryShiftedSpecificDomDates(
  occurrences: LocalOccurrence[],
  timePhrase: string
): string | null {
  const localDates = getLocalDateLabels(occurrences);
  if (localDates.length === 0 || localDates.length > 6) {
    return null;
  }
  return `Runs ${timePhrase} on ${joinNatural(localDates)}`;
}

function tryShiftedMonthEndSummary(
  monF: string,
  occurrences: LocalOccurrence[],
  timePhrase: string
): string | null {
  if (!areAllLocalDatesLastDayOfMonth(occurrences)) {
    return null;
  }

  const displayMonths = getDisplayMonths(monF, occurrences);
  if (displayMonths.length > 0) {
    return `Runs ${timePhrase} on the last day of ${joinNatural(displayMonths)}`;
  }
  return `Runs ${timePhrase} on the last day of every month`;
}

function tryNearestWeekday(domF: string, monF: string, localTime: string): string | null {
  const upper = domF.toUpperCase();
  const months = expandMonths(monF);
  const monthSuffix = months.length > 0 ? ` of ${joinNatural(months)}` : ' of every month';

  if (upper === 'LW') {
    return `Runs at ${localTime} on the last weekday${monthSuffix}`;
  }

  const lastOffsetWeekdayMatch = upper.match(/^L-(\d+)W$/);
  if (lastOffsetWeekdayMatch) {
    const offset = parseInt(lastOffsetWeekdayMatch[1], 10);
    const label =
      offset === 0 ? 'last weekday' : `weekday nearest the ${toOrdinal(offset + 1)}-to-last day`;
    return `Runs at ${localTime} on the ${label}${monthSuffix}`;
  }

  const nearestWeekdayMatch = upper.match(/^(\d{1,2})W$/);
  if (!nearestWeekdayMatch) {
    return null;
  }

  const day = parseInt(nearestWeekdayMatch[1], 10);
  return `Runs at ${localTime} on the weekday nearest the ${toOrdinal(day)}${monthSuffix}`;
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
  const anchorLocalTime = occurrences[0]?.localTime ?? localTime;
  const timePhrase = `at ${anchorLocalTime}`;
  const localMonthEndOffset = getConsistentDaysFromLocalMonthEnd(occurrences);
  const displayMonths = getDisplayMonths(monF, occurrences);

  if (areAllLocalDatesFirstDayOfMonth(occurrences)) {
    if (displayMonths.length > 0) {
      return `Runs ${timePhrase} on the first day of ${joinNatural(displayMonths)} every year`;
    }
    return `Runs ${timePhrase} on the first day of every month`;
  }

  if (localMonthEndOffset !== null && localMonthEndOffset > 0) {
    const label = formatLocalMonthEndLabel(localMonthEndOffset);
    if (displayMonths.length > 0) {
      return `Runs ${timePhrase} on the ${label} day of ${joinNatural(displayMonths)} every year`;
    }
    return `Runs ${timePhrase} on the ${label} day of every month`;
  }

  if (displayMonths.length > 0) {
    return `Runs ${timePhrase} on the last day of ${joinNatural(displayMonths)}`;
  }
  return `Runs ${timePhrase} on the last day of every month`;
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
  if (!isStrictNumericField(domF)) return null;
  const domNum = parseInt(domF, 10);

  const anchorLocalTime = occurrences[0]?.localTime ?? localTime;
  const timePhrase = `at ${anchorLocalTime}`;
  const months = getDisplayMonths(monF, occurrences);
  const leapDayDescription = tryLeapDayDescription(domNum, months, timePhrase);
  if (leapDayDescription) {
    return leapDayDescription;
  }

  const shiftedDatesDescription = tryShiftedSpecificDomDates(occurrences, timePhrase);
  if (shiftedDatesDescription) return shiftedDatesDescription;

  const shiftedMonthEndSummary = tryShiftedMonthEndSummary(monF, occurrences, timePhrase);
  if (shiftedMonthEndSummary) return shiftedMonthEndSummary;

  // For recurring monthly patterns check if all local dates share a consistent shifted DOM
  const effectiveDom = getConsistentLocalDom(occurrences) ?? domNum;
  const shiftedLeapDayDescription = tryLeapDayDescription(effectiveDom, months, timePhrase);
  if (shiftedLeapDayDescription) {
    return shiftedLeapDayDescription;
  }
  return buildSpecificDomSchedule(monF, months, effectiveDom, timePhrase);
}

function tryDailyWildcard(domF: string, dowF: string, localTime: string): string | null {
  const isDomWild = domF === '*' || domF === '?';
  const days = expandDays(dowF);
  return isDomWild && days.length === 0 ? `Runs every day at ${localTime}` : null;
}

function tryDayOfMonthStep(domF: string, monF: string, localTime: string): string | null {
  const stepMatch = domF.match(/^(\*|\d{1,2})\/(\d+)$/);
  if (!stepMatch) {
    return null;
  }

  const [, startToken, stepToken] = stepMatch;
  const step = parseInt(stepToken, 10);
  if (Number.isNaN(step) || step <= 0) {
    return null;
  }

  const months = expandMonths(monF);
  if (startToken === '*') {
    return months.length > 0
      ? `Runs at ${localTime} every ${step} days in ${joinNatural(months)}`
      : `Runs at ${localTime} every ${step} days`;
  }

  const startDay = parseInt(startToken, 10);
  const startPhrase = `starting from the ${toOrdinal(startDay)}`;
  return months.length > 0
    ? `Runs at ${localTime} every ${step} days ${startPhrase} of ${joinNatural(months)} every year`
    : `Runs at ${localTime} every ${step} days ${startPhrase} of every month`;
}

function trySpecificDomList(domF: string, monF: string, localTime: string): string | null {
  const tokens = domF.split(',').map(token => token.trim());
  if (tokens.length < 2 || tokens.some(token => !isStrictNumericField(token))) {
    return null;
  }

  const dayLabels = tokens.map(token => toOrdinal(parseInt(token, 10)));
  const months = expandMonths(monF);
  return months.length > 0
    ? `Runs at ${localTime} on the ${joinNatural(dayLabels)} of ${joinNatural(months)} every year`
    : `Runs at ${localTime} on the ${joinNatural(dayLabels)} of every month`;
}

function tryLeapDayDescription(
  dayOfMonth: number,
  months: string[],
  timePhrase: string
): string | null {
  if (months.length === 1 && months[0] === 'February' && dayOfMonth === 29) {
    return `Runs ${timePhrase} on February 29th every leap year`;
  }

  return null;
}

function buildSpecificDomSchedule(
  monF: string,
  months: string[],
  dayOfMonth: number,
  timePhrase: string
): string | null {
  if (months.length > 0) {
    const monthPhrase =
      months.length === 1
        ? `${months[0]} ${toOrdinal(dayOfMonth)}`
        : `the ${toOrdinal(dayOfMonth)} of ${joinNatural(months)}`;
    return `Runs ${timePhrase} on ${monthPhrase} every year`;
  }

  return monF === '*' || monF === '?'
    ? `Runs ${timePhrase} on the ${toOrdinal(dayOfMonth)} of every month`
    : null;
}
