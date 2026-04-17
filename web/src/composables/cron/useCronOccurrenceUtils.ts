import { normalizeOffsetLabel } from './useCronFieldParsers';

export function getReferenceDate(referenceDate?: string | Date): Date {
  if (referenceDate instanceof Date && !Number.isNaN(referenceDate.getTime())) return referenceDate;

  if (typeof referenceDate === 'string' && referenceDate.trim()) {
    const isoDateMatch = referenceDate.trim().match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (isoDateMatch) {
      const [, year, month, day] = isoDateMatch;
      return new Date(
        Date.UTC(parseInt(year, 10), parseInt(month, 10) - 1, parseInt(day, 10), 12, 0, 0)
      );
    }

    const parsed = new Date(referenceDate);
    if (!Number.isNaN(parsed.getTime())) return parsed;
  }

  return new Date();
}

export function getHourMinuteParts(cron: string): { hour: number; minute: number } | null {
  const parts = cron.trim().split(/\s+/);
  if (parts.length !== 5 && parts.length !== 6 && parts.length !== 7) {
    return null;
  }

  const normalizedParts =
    parts.length === 7 ? parts.slice(0, 6) : parts.length === 6 ? parts : ['0', ...parts];
  const [, min, hour] = normalizedParts;
  if (!min || !hour || !/^\d+$/.test(min) || !/^\d+$/.test(hour)) {
    return null;
  }

  return {
    hour: parseInt(hour, 10),
    minute: parseInt(min, 10),
  };
}

export function detectDateShift(
  utcHour: number,
  utcMinute: number,
  timezone: string,
  referenceDate: Date
): boolean {
  try {
    const utcDate = new Date(
      Date.UTC(
        referenceDate.getUTCFullYear(),
        referenceDate.getUTCMonth(),
        referenceDate.getUTCDate(),
        utcHour,
        utcMinute,
        0
      )
    );

    const localParts = new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(utcDate);

    const localDay = localParts.find(part => part.type === 'day')?.value;
    const localMonth = localParts.find(part => part.type === 'month')?.value;
    const localYear = localParts.find(part => part.type === 'year')?.value;

    const utcDay = String(referenceDate.getUTCDate()).padStart(2, '0');
    const utcMonth = String(referenceDate.getUTCMonth() + 1).padStart(2, '0');
    const utcYear = String(referenceDate.getUTCFullYear());

    return localDay !== utcDay || localMonth !== utcMonth || localYear !== utcYear;
  } catch {
    return false;
  }
}

export function formatTimezoneLabel(timezone: string, referenceDate: Date): string {
  try {
    const offsetPart = new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      timeZoneName: 'shortOffset',
    })
      .formatToParts(referenceDate)
      .find(part => part.type === 'timeZoneName')?.value;
    if (!offsetPart) return timezone;
    return `${timezone} (${normalizeOffsetLabel(offsetPart)})`;
  } catch {
    return timezone;
  }
}

export const DEFAULT_LOOKAHEAD_DAYS = 400;
export const EXTENDED_LOOKAHEAD_DAYS = 1465;

export function shouldRetryWithExtendedLookahead(lookaheadDays: number): boolean {
  return lookaheadDays >= DEFAULT_LOOKAHEAD_DAYS && lookaheadDays < EXTENDED_LOOKAHEAD_DAYS;
}
