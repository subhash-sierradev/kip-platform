/**
 * Timezone utilities for handling user timezone detection and conversion
 */

/**
 * Get the user's current timezone from the browser
 */
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

/**
 * Get timezone display name (e.g., "India Standard Time")
 */
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

/**
 * Get timezone abbreviation (e.g., "IST")
 */
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

/**
 * Format timezone info for display
 */
export function formatTimezoneInfo(timezone?: string): string {
  const currentTz = timezone || getUserTimezone();
  const abbreviation = getTimezoneAbbreviation(currentTz);
  return `${currentTz} (${abbreviation})`;
}

/**
 * Check if a timezone is valid
 */
export function isValidTimezone(timezone: string): boolean {
  try {
    Intl.DateTimeFormat(undefined, { timeZone: timezone });
    return true;
  } catch {
    return false;
  }
}

/**
 * Get common timezone options for selection
 */
export function getCommonTimezones(): Array<{ value: string; label: string }> {
  // Sorted by UTC offset (west → east) with offset in label for quick scanning.
  // Users can type city, country, or offset number in the DxSelectBox search.
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

/**
 * Format current time in user's timezone
 */
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

/**
 * Convert user's local time to UTC time
 * Frontend sends times in UTC to backend to avoid double-conversion issues
 * @param localTimeStr - Local time in HH:mm or HH:mm:ss format
 * @param executionDate - Execution date in YYYY-MM-DD format (defaults to today)
 * @param timezone - User's timezone (defaults to browser timezone)
 * @returns UTC time string in HH:mm:ss format
 */
export function convertUserTimezoneToUtc(
  localTimeStr: string,
  executionDate?: string,
  timezone?: string
): string {
  try {
    const [hhStr, mmStr, ssStr] = localTimeStr.split(':');
    const hours = parseInt(hhStr || '0', 10);
    const minutes = parseInt(mmStr || '0', 10);
    const seconds = parseInt(ssStr || '0', 10);
    const dateStr = executionDate || new Date().toISOString().split('T')[0];
    const userTz = timezone || getUserTimezone();

    // Parse date components
    const [yyyy, mm, dd] = dateStr.split('-').map(Number);

    // Create two formatter instances to compare the same instant in different timezones
    // This gives us the offset between the user's timezone and UTC
    const dummyDate = new Date(`${dateStr}T12:00:00`);
    const offsetMinutes = getTimezoneOffsetMinutes(userTz, dummyDate);

    // Create UTC date with the local time values
    const localAsUtcDate = new Date(Date.UTC(yyyy, mm - 1, dd, hours, minutes, seconds));

    // Apply timezone offset to get actual UTC time
    // If timezone is ahead of UTC (e.g., IST +5:30), we subtract the offset
    const utcTimestamp = localAsUtcDate.getTime() - offsetMinutes * 60 * 1000;
    const utcDate = new Date(utcTimestamp);

    const utcHours = utcDate.getUTCHours();
    const utcMinutes = utcDate.getUTCMinutes();
    const utcSeconds = utcDate.getUTCSeconds();

    return `${String(utcHours).padStart(2, '0')}:${String(utcMinutes).padStart(2, '0')}:${String(utcSeconds).padStart(2, '0')}`;
  } catch (error) {
    console.error('convertUserTimezoneToUtc: Error converting time:', localTimeStr, error);
    return localTimeStr; // Fallback to original if conversion fails
  }
}

/**
 * Get timezone offset in minutes for a specific date
 * @param timezone - IANA timezone identifier
 * @param date - The date to calculate offset for (needed for DST)
 * @returns Offset in minutes (positive for ahead of UTC, negative for behind)
 */
function getTimezoneOffsetMinutes(timezone: string, date: Date): number {
  try {
    // Create two date strings representing the same instant
    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: timezone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    });

    const parts = formatter.formatToParts(date);
    const tzParts: Record<string, string> = {};
    parts.forEach(part => {
      if (part.type !== 'literal') {
        tzParts[part.type] = part.value;
      }
    });

    // Create a date from the timezone parts
    const tzDate = new Date(
      `${tzParts.year}-${tzParts.month}-${tzParts.day}T${tzParts.hour}:${tzParts.minute}:${tzParts.second}Z`
    );

    // Create UTC date from the same instant
    const utcDate = new Date(date.toISOString());

    // Calculate the difference
    const diffMs = tzDate.getTime() - utcDate.getTime();
    return Math.round(diffMs / (1000 * 60));
  } catch (error) {
    console.error('getTimezoneOffsetMinutes: Error calculating offset:', error);
    // Fallback: use standard Date offset method
    return -date.getTimezoneOffset();
  }
}
