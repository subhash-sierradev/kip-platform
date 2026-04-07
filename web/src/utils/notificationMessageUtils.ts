import { formatDate } from '@/utils/dateUtils';

const ISO_UTC_INSTANT_REGEX = /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?Z/g;

function normalizeIsoUtcInstantToMillis(isoUtcInstant: string): string {
  // Java Instant#toString() can include up to 9 fractional digits (nanoseconds).
  // JS Date parsing is not consistent with >3 fractional digits, so we truncate to millis.
  return isoUtcInstant.replace(/\.(\d+)(?=Z$)/, (_match, fraction: string) => {
    const millis = fraction.slice(0, 3);
    return `.${millis}`;
  });
}

/**
 * Replaces embedded ISO-8601 UTC instants (e.g. "2026-02-25T18:32:28.820396500Z")
 * inside a message with a localized date+time string in the user's timezone.
 */
export function localizeEmbeddedIsoTimestamps(message: string): string {
  if (!message) return message;

  return message.replace(ISO_UTC_INSTANT_REGEX, (match: string) => {
    const normalized = normalizeIsoUtcInstantToMillis(match);
    const formatted = formatDate(normalized, { includeTime: true, format: 'short' });

    // If parsing/formatting fails, do not mutate the message.
    if (formatted === 'Invalid Date' || formatted === 'N/A') {
      return match;
    }

    return formatted;
  });
}
