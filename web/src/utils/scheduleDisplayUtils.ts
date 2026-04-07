import { parseDatePreservingDateOnly } from './dateUtils';
import { getUserTimezone } from './timezoneUtils';

export const MONTH_LABELS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

/**
 * Convert UTC execution time to user's current timezone
 * @param utcTimeStr - UTC time in HH:mm:ss format
 * @param executionDate - Execution date in YYYY-MM-DD format
 * @returns Time string in HH:mm format converted to user's timezone
 */
export function convertUtcTimeToUserTimezone(utcTimeStr: string, executionDate?: string): string {
  try {
    const [hhStr, mmStr] = utcTimeStr.split(':');
    const hours = parseInt(hhStr || '0', 10);
    const minutes = parseInt(mmStr || '0', 10);
    const dateStr = executionDate || new Date().toISOString().split('T')[0];

    // Create UTC instant
    const [yyyy, mm, dd] = dateStr.split('-').map(Number);
    const utcInstant = new Date(Date.UTC(yyyy, mm - 1, dd, hours, minutes, 0));

    // Format to user's timezone
    const userTz = getUserTimezone();
    const formattedTime = new Intl.DateTimeFormat('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
      timeZone: userTz,
    }).format(utcInstant);

    return formattedTime; // Returns HH:mm format
  } catch (error) {
    console.error('convertUtcTimeToUserTimezone: Error converting time:', utcTimeStr, error);
    return utcTimeStr; // Fallback to original if conversion fails
  }
}

/**
 * Format date-only values without timezone day shifting.
 *
 * For YYYY-MM-DD values we construct a local date directly instead of
 * passing through UTC parsing to preserve the selected calendar day.
 */
export function formatDateOnlyDisplay(
  dateString: string | null | undefined,
  formatOptions: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
  },
  emptyValue: string = 'N/A',
  invalidValue: string = 'Invalid Date'
): string {
  if (!dateString) return emptyValue;

  const parsedDate = parseDatePreservingDateOnly(String(dateString));
  if (!parsedDate) return invalidValue;

  try {
    return parsedDate.toLocaleDateString('en-US', formatOptions);
  } catch {
    return invalidValue;
  }
}

const WEEKDAY_ABBR_ORDER = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'] as const;
type WeekdayAbbr = (typeof WEEKDAY_ABBR_ORDER)[number];

const DAY_MAP: Record<string, WeekdayAbbr> = {
  MON: 'MON',
  MONDAY: 'MON',
  TUE: 'TUE',
  TUESDAY: 'TUE',
  WED: 'WED',
  WEDNESDAY: 'WED',
  THU: 'THU',
  THURSDAY: 'THU',
  FRI: 'FRI',
  FRIDAY: 'FRI',
  SAT: 'SAT',
  SATURDAY: 'SAT',
  SUN: 'SUN',
  SUNDAY: 'SUN',
};

function normalizeDayToken(token: string): WeekdayAbbr | null {
  const s = token.toUpperCase();
  if (DAY_MAP[s]) return DAY_MAP[s];
  if (/^\d+$/.test(s)) {
    const n = Math.max(0, Math.min(6, parseInt(s, 10)));
    const map = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'] as const;
    return map[n] as WeekdayAbbr;
  }
  return null;
}

export function formatSelectedDaysDisplay(raw: unknown): string {
  const arr: string[] = Array.isArray(raw)
    ? (raw as unknown[]).map(v => String(v))
    : typeof raw === 'string'
      ? raw
          .split(',')
          .map(s => s.trim())
          .filter(Boolean)
      : [];
  const normalized = arr
    .map(t => normalizeDayToken(String(t)))
    .filter((v): v is WeekdayAbbr => v !== null);
  const unique = Array.from(new Set(normalized));
  unique.sort((a, b) => WEEKDAY_ABBR_ORDER.indexOf(a) - WEEKDAY_ABBR_ORDER.indexOf(b));
  return unique.join(', ');
}
