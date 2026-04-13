import { DateTime } from 'luxon';

export function getUserTimezone(): string {
  try {
    const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (tz === 'Asia/Calcutta') {
      return 'Asia/Kolkata';
    }
    return tz;
  } catch {
    console.warn('Failed to detect user timezone, falling back to Asia/Kolkata');
    return 'Asia/Kolkata';
  }
}

export function getTimezoneDisplayName(timezone?: string): string {
  const tz = timezone || getUserTimezone();
  try {
    const formatter = new Intl.DateTimeFormat('en', {
      timeZone: tz,
      timeZoneName: 'long',
    });
    const parts = formatter.formatToParts(new Date());
    const timeZoneName = parts.find(part => part.type === 'timeZoneName')?.value;
    return timeZoneName || tz;
  } catch {
    return tz;
  }
}

export function getTimezoneAbbreviation(timezone?: string): string {
  const tz = timezone || getUserTimezone();
  try {
    const formatter = new Intl.DateTimeFormat('en', {
      timeZone: tz,
      timeZoneName: 'short',
    });
    const parts = formatter.formatToParts(new Date());
    const timeZoneName = parts.find(part => part.type === 'timeZoneName')?.value;
    return timeZoneName || tz;
  } catch {
    return tz;
  }
}

export function formatTimezoneInfo(timezone?: string): string {
  const currentTz = timezone || getUserTimezone();
  const abbreviation = getTimezoneAbbreviation(currentTz);
  return `${currentTz} (${abbreviation})`;
}

export function isValidTimezone(timezone: string): boolean {
  try {
    Intl.DateTimeFormat(undefined, { timeZone: timezone });
    return true;
  } catch {
    return false;
  }
}

// Sorted west→east; label includes UTC offset so users can search by city, country, or offset.
export function getCommonTimezones(): Array<{ value: string; label: string }> {
  return [
    { value: 'Pacific/Honolulu', label: '(UTC-10:00) Hawaii (Pacific/Honolulu)' },
    { value: 'America/Anchorage', label: '(UTC-09:00) Alaska (America/Anchorage)' },
    { value: 'America/Los_Angeles', label: '(UTC-08:00) Pacific Time (America/Los_Angeles)' },
    { value: 'America/Vancouver', label: '(UTC-08:00) Pacific Time (America/Vancouver)' },
    { value: 'America/Denver', label: '(UTC-07:00) Mountain Time (America/Denver)' },
    { value: 'America/Edmonton', label: '(UTC-07:00) Mountain Time (America/Edmonton)' },
    { value: 'America/Phoenix', label: '(UTC-07:00) Arizona - No DST (America/Phoenix)' },
    { value: 'America/Chicago', label: '(UTC-06:00) Central Time (America/Chicago)' },
    { value: 'America/Winnipeg', label: '(UTC-06:00) Central Time (America/Winnipeg)' },
    { value: 'America/Mexico_City', label: '(UTC-06:00) Central Time (America/Mexico_City)' },
    { value: 'America/New_York', label: '(UTC-05:00) Eastern Time (America/New_York)' },
    { value: 'America/Toronto', label: '(UTC-05:00) Eastern Time (America/Toronto)' },
    { value: 'America/Bogota', label: '(UTC-05:00) Colombia (America/Bogota)' },
    { value: 'America/Lima', label: '(UTC-05:00) Peru (America/Lima)' },
    { value: 'America/Halifax', label: '(UTC-04:00) Atlantic Time (America/Halifax)' },
    { value: 'America/Santiago', label: '(UTC-04:00) Chile (America/Santiago)' },
    { value: 'America/St_Johns', label: '(UTC-03:30) Newfoundland (America/St_Johns)' },
    { value: 'America/Sao_Paulo', label: '(UTC-03:00) Brasília (America/Sao_Paulo)' },
    {
      value: 'America/Argentina/Buenos_Aires',
      label: '(UTC-03:00) Argentina (America/Argentina/Buenos_Aires)',
    },
    { value: 'UTC', label: '(UTC+00:00) UTC (Coordinated Universal Time)' },
    { value: 'Europe/London', label: '(UTC+00:00) London (Europe/London)' },
    { value: 'Africa/Casablanca', label: '(UTC+00:00) Morocco (Africa/Casablanca)' },
    { value: 'Europe/Paris', label: '(UTC+01:00) Paris (Europe/Paris)' },
    { value: 'Europe/Berlin', label: '(UTC+01:00) Berlin (Europe/Berlin)' },
    { value: 'Europe/Madrid', label: '(UTC+01:00) Madrid (Europe/Madrid)' },
    { value: 'Europe/Rome', label: '(UTC+01:00) Rome (Europe/Rome)' },
    { value: 'Europe/Amsterdam', label: '(UTC+01:00) Amsterdam (Europe/Amsterdam)' },
    { value: 'Europe/Zurich', label: '(UTC+01:00) Zurich (Europe/Zurich)' },
    { value: 'Europe/Stockholm', label: '(UTC+01:00) Stockholm (Europe/Stockholm)' },
    { value: 'Africa/Lagos', label: '(UTC+01:00) West Africa (Africa/Lagos)' },
    { value: 'Europe/Helsinki', label: '(UTC+02:00) Helsinki (Europe/Helsinki)' },
    { value: 'Europe/Athens', label: '(UTC+02:00) Athens (Europe/Athens)' },
    { value: 'Europe/Bucharest', label: '(UTC+02:00) Bucharest (Europe/Bucharest)' },
    { value: 'Africa/Cairo', label: '(UTC+02:00) Cairo (Africa/Cairo)' },
    { value: 'Africa/Johannesburg', label: '(UTC+02:00) South Africa (Africa/Johannesburg)' },
    { value: 'Europe/Moscow', label: '(UTC+03:00) Moscow (Europe/Moscow)' },
    { value: 'Europe/Istanbul', label: '(UTC+03:00) Istanbul (Europe/Istanbul)' },
    { value: 'Asia/Riyadh', label: '(UTC+03:00) Riyadh (Asia/Riyadh)' },
    { value: 'Africa/Nairobi', label: '(UTC+03:00) East Africa (Africa/Nairobi)' },
    { value: 'Asia/Tehran', label: '(UTC+03:30) Tehran (Asia/Tehran)' },
    { value: 'Asia/Dubai', label: '(UTC+04:00) Dubai (Asia/Dubai)' },
    { value: 'Asia/Kabul', label: '(UTC+04:30) Kabul (Asia/Kabul)' },
    { value: 'Asia/Karachi', label: '(UTC+05:00) Karachi (Asia/Karachi)' },
    { value: 'Asia/Kolkata', label: '(UTC+05:30) India (Asia/Kolkata)' },
    { value: 'Asia/Colombo', label: '(UTC+05:30) Sri Lanka (Asia/Colombo)' },
    { value: 'Asia/Kathmandu', label: '(UTC+05:45) Nepal (Asia/Kathmandu)' },
    { value: 'Asia/Dhaka', label: '(UTC+06:00) Bangladesh (Asia/Dhaka)' },
    { value: 'Asia/Bangkok', label: '(UTC+07:00) Bangkok (Asia/Bangkok)' },
    { value: 'Asia/Jakarta', label: '(UTC+07:00) Jakarta (Asia/Jakarta)' },
    { value: 'Asia/Shanghai', label: '(UTC+08:00) China (Asia/Shanghai)' },
    { value: 'Asia/Hong_Kong', label: '(UTC+08:00) Hong Kong (Asia/Hong_Kong)' },
    { value: 'Asia/Singapore', label: '(UTC+08:00) Singapore (Asia/Singapore)' },
    { value: 'Asia/Taipei', label: '(UTC+08:00) Taipei (Asia/Taipei)' },
    { value: 'Asia/Manila', label: '(UTC+08:00) Manila (Asia/Manila)' },
    { value: 'Australia/Perth', label: '(UTC+08:00) Perth (Australia/Perth)' },
    { value: 'Asia/Tokyo', label: '(UTC+09:00) Tokyo (Asia/Tokyo)' },
    { value: 'Asia/Seoul', label: '(UTC+09:00) Seoul (Asia/Seoul)' },
    { value: 'Australia/Adelaide', label: '(UTC+09:30) Adelaide (Australia/Adelaide)' },
    { value: 'Australia/Sydney', label: '(UTC+10:00) Sydney (Australia/Sydney)' },
    { value: 'Australia/Melbourne', label: '(UTC+10:00) Melbourne (Australia/Melbourne)' },
    { value: 'Australia/Brisbane', label: '(UTC+10:00) Brisbane (Australia/Brisbane)' },
    { value: 'Pacific/Auckland', label: '(UTC+12:00) Auckland (Pacific/Auckland)' },
    { value: 'Pacific/Fiji', label: '(UTC+12:00) Fiji (Pacific/Fiji)' },
  ];
}

export function getCurrentTimeInTimezone(timezone?: string): string {
  const tz = timezone || getUserTimezone();

  try {
    return new Intl.DateTimeFormat('en-GB', {
      timeZone: tz,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).format(new Date());
  } catch {
    return new Date().toLocaleTimeString();
  }
}

export function convertUserTimezoneToUtc(
  localTimeStr: string,
  executionDate?: string,
  timezone?: string
): string {
  try {
    const [hhStr, mmStr, ssStr] = localTimeStr.split(':');
    const dateStr = executionDate || new Date().toISOString().split('T')[0];
    const userTz = timezone || getUserTimezone();
    const [yyyy, mm, dd] = dateStr.split('-').map(Number);

    const dt = DateTime.fromObject(
      {
        year: yyyy,
        month: mm,
        day: dd,
        hour: parseInt(hhStr || '0', 10),
        minute: parseInt(mmStr || '0', 10),
        second: parseInt(ssStr || '0', 10),
      },
      { zone: userTz }
    );

    if (!dt.isValid) {
      throw new Error(`Invalid DateTime: ${dt.invalidReason ?? 'unknown'}`);
    }

    return dt.toUTC().toFormat('HH:mm:ss');
  } catch (error) {
    console.error('convertUserTimezoneToUtc: Error converting time:', localTimeStr, error);
    return localTimeStr;
  }
}

// Returns YYYY-MM-DD in the given (or browser) timezone — not toISOString() which gives the UTC date.
export function getLocalDateString(timezone?: string): string {
  const tz = timezone || getUserTimezone();
  return DateTime.now().setZone(tz).toISODate() ?? new Date().toISOString().slice(0, 10);
}

// Converts a local wall-clock date+time to UTC. Handles midnight crossings and DST transitions.
export function convertLocalDateTimeToUtc(
  localDate: string,
  localTime: string,
  timezone?: string
): { utcDate: string; utcTime: string } {
  try {
    const [hhStr, mmStr, ssStr] = localTime.split(':');
    const userTz = timezone || getUserTimezone();
    const [yyyy, mm, dd] = localDate.split('-').map(Number);

    const dt = DateTime.fromObject(
      {
        year: yyyy,
        month: mm,
        day: dd,
        hour: parseInt(hhStr || '0', 10),
        minute: parseInt(mmStr || '0', 10),
        second: parseInt(ssStr || '0', 10),
      },
      { zone: userTz }
    );

    if (!dt.isValid) {
      throw new Error(`Invalid DateTime: ${dt.invalidReason ?? 'unknown'}`);
    }

    const utc = dt.toUTC();

    return {
      utcDate: utc.toFormat('yyyy-MM-dd'),
      utcTime: utc.toFormat('HH:mm:ss'),
    };
  } catch (error) {
    console.error('convertLocalDateTimeToUtc: Error converting:', localDate, localTime, error);
    return { utcDate: localDate, utcTime: localTime };
  }
}

// Converts a UTC date+time pair to the local wall-clock date and time in the given (or browser) timezone.
// Both inputs must be UTC — they are combined into a single UTC instant before converting.
export function convertUtcDateTimeToLocal(
  utcDate: string,
  utcTime: string,
  timezone?: string
): { localDate: string; localTime: string } {
  try {
    const [hhStr, mmStr, ssStr] = utcTime.split(':');
    const hours = parseInt(hhStr || '0', 10);
    const minutes = parseInt(mmStr || '0', 10);
    const seconds = parseInt(ssStr || '0', 10);
    const [yyyy, mm, dd] = utcDate.split('-').map(Number);

    const utcInstant = new Date(Date.UTC(yyyy, mm - 1, dd, hours, minutes, seconds));
    const userTz = timezone || getUserTimezone();

    const localDate = new Intl.DateTimeFormat('en-CA', {
      timeZone: userTz,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).format(utcInstant);

    const localTime = new Intl.DateTimeFormat('en-US', {
      timeZone: userTz,
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    })
      .format(utcInstant)
      .replace('24:', '00:');

    return { localDate, localTime };
  } catch (error) {
    console.error('convertUtcDateTimeToLocal: Error converting:', utcDate, utcTime, error);
    return { localDate: utcDate, localTime: utcTime.slice(0, 5) };
  }
}
