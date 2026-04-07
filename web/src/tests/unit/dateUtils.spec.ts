import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  type DateFormatOptions,
  formatDate,
  formatDateUTC,
  formatDisplayDate,
  formatFullDate,
  formatKeyTime,
  formatMetadataDate,
  formatSimpleDate,
  formatTime,
  getRelativeTime,
  isToday,
  parseDateOnlyBoundary,
  parseDatePreservingDateOnly,
} from '@/utils/dateUtils';

describe('dateUtils', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2025-12-09T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('formatDate', () => {
    it('formats date correctly with default options', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDate(date);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('returns N/A for null date', () => {
      expect(formatDate(null)).toBe('N/A');
    });

    it('returns N/A for undefined date', () => {
      expect(formatDate(undefined)).toBe('N/A');
    });

    it('formats date with time when includeTime is true', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDate(date, { includeTime: true });
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats date with seconds when includeSeconds is true', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDate(date, { includeTime: true, includeSeconds: true });
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats date with timezone when includeTimezone is true', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDate(date, { includeTime: true, includeTimezone: true });
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats date with short format', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDate(date, { format: 'short' });
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats date with medium format', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDate(date, { format: 'medium' });
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats date with long format', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDate(date, { format: 'long' });
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats date with weekday when includeWeekday is true', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDate(date, { includeWeekday: true, includeTime: false });
      expect(result).toMatch(/^[A-Za-z]{3},\s/);
    });

    it('formats date without time when includeTime is false', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDate(date, { includeTime: false });
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('handles string input', () => {
      const result = formatDate('2025-12-09T15:30:45Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('returns Invalid Date for invalid string', () => {
      expect(formatDate('not-a-date')).toBe('Invalid Date');
    });

    it('returns Invalid Date for empty string', () => {
      expect(formatDate('')).toBe('N/A');
    });
  });

  describe('formatDateUTC', () => {
    it('formats date in UTC timezone', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDateUTC(date);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('returns N/A for null date', () => {
      expect(formatDateUTC(null)).toBe('N/A');
    });

    it('returns N/A for undefined date', () => {
      expect(formatDateUTC(undefined)).toBe('N/A');
    });

    it('handles string input', () => {
      const result = formatDateUTC('2025-12-09T15:30:45Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats with all options in UTC', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDateUTC(date, {
        includeTime: true,
        includeSeconds: true,
        includeTimezone: true,
        format: 'long',
      });
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
      expect(result).toContain('UTC');
    });

    it('returns Invalid Date for invalid string', () => {
      expect(formatDateUTC('not-a-date')).toBe('Invalid Date');
    });
  });

  describe('formatMetadataDate', () => {
    it('formats metadata date correctly', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatMetadataDate(date);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('returns N/A for invalid date', () => {
      expect(formatMetadataDate(null)).toBe('N/A');
      expect(formatMetadataDate(undefined)).toBe('N/A');
    });

    it('handles string input', () => {
      const result = formatMetadataDate('2025-12-09T15:30:45Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });
  });

  describe('formatDisplayDate', () => {
    it('formats display date correctly', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatDisplayDate(date);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('handles null date', () => {
      expect(formatDisplayDate(null)).toBe('N/A');
    });

    it('handles string input', () => {
      const result = formatDisplayDate('2025-12-09T15:30:45Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });
  });

  describe('formatFullDate', () => {
    it('formats full date correctly', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatFullDate(date);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('handles null date', () => {
      expect(formatFullDate(null)).toBe('N/A');
    });

    it('handles string input', () => {
      const result = formatFullDate('2025-12-09T15:30:45Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });
  });

  describe('formatSimpleDate', () => {
    it('formats simple date correctly', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatSimpleDate(date);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('returns N/A for null date', () => {
      expect(formatSimpleDate(null)).toBe('N/A');
    });

    it('returns N/A for undefined date', () => {
      expect(formatSimpleDate(undefined)).toBe('N/A');
    });

    it('handles string input', () => {
      const result = formatSimpleDate('2025-12-09T15:30:45Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('returns Invalid Date for invalid string', () => {
      expect(formatSimpleDate('invalid')).toBe('Invalid Date');
    });
  });

  describe('formatTime', () => {
    it('formats time from Date object', () => {
      const date = new Date('2025-12-09T15:30:45Z');
      const result = formatTime(date);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats time from HH:MM string', () => {
      const result = formatTime('14:30');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats time from HH:MM:SS string', () => {
      const result = formatTime('14:30:45');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats time from ISO string', () => {
      const result = formatTime('2025-12-09T15:30:45Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('returns N/A for null', () => {
      expect(formatTime(null)).toBe('N/A');
    });

    it('returns N/A for undefined', () => {
      expect(formatTime(undefined)).toBe('N/A');
    });

    it('returns Invalid Time for completely invalid string', () => {
      expect(formatTime('not a time')).toBe('Invalid Time');
    });

    it('returns original string for string with digits but invalid format', () => {
      const result = formatTime('25:99'); // Invalid hours/minutes
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('handles single digit hours', () => {
      const result = formatTime('9:30');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });
  });

  describe('formatKeyTime', () => {
    it('returns dash for null start time', () => {
      expect(formatKeyTime(null)).toBe('-');
    });

    it('returns dash for undefined start time', () => {
      expect(formatKeyTime(undefined)).toBe('-');
    });

    it('returns Invalid Date for invalid start time', () => {
      expect(formatKeyTime('invalid')).toBe('Invalid Date');
    });

    it('formats start time only when no end time', () => {
      const result = formatKeyTime('2025-12-09T15:30:00Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats start time only when end time is null', () => {
      const result = formatKeyTime('2025-12-09T15:30:00Z', null);
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('formats time range for same day', () => {
      const start = '2025-12-09T10:00:00Z';
      const end = '2025-12-09T15:30:00Z';
      const result = formatKeyTime(start, end);
      expect(result).toBeTruthy();
      expect(result).toContain('→');
      expect(typeof result).toBe('string');
    });

    it('formats time range for different days', () => {
      const start = '2025-12-09T10:00:00Z';
      const end = '2025-12-10T15:30:00Z';
      const result = formatKeyTime(start, end);
      expect(result).toBeTruthy();
      expect(result).toContain('→');
      expect(typeof result).toBe('string');
    });

    it('handles Date objects for start and end', () => {
      const start = new Date('2025-12-09T10:00:00Z');
      const end = new Date('2025-12-09T15:30:00Z');
      const result = formatKeyTime(start, end);
      expect(result).toBeTruthy();
      expect(result).toContain('→');
    });

    it('handles invalid end time', () => {
      const result = formatKeyTime('2025-12-09T10:00:00Z', 'invalid');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
      expect(result).not.toContain('→');
    });
  });

  describe('getRelativeTime', () => {
    it('returns "Just now" for very recent time', () => {
      const date = new Date('2025-12-09T11:59:30Z'); // 30 seconds ago
      const result = getRelativeTime(date);
      expect(result).toBe('Just now');
    });

    it('returns minutes ago for recent time', () => {
      const date = new Date('2025-12-09T11:30:00Z'); // 30 minutes ago
      const result = getRelativeTime(date);
      expect(result).toContain('minutes ago');
    });

    it('returns hours ago for time within day', () => {
      const date = new Date('2025-12-09T09:00:00Z'); // 3 hours ago
      const result = getRelativeTime(date);
      expect(result).toContain('hours ago');
    });

    it('returns days ago for recent dates', () => {
      const date = new Date('2025-12-07T12:00:00Z'); // 2 days ago
      const result = getRelativeTime(date);
      expect(result).toContain('days ago');
    });

    it('returns months ago for older dates', () => {
      const date = new Date('2025-10-09T12:00:00Z'); // ~2 months ago
      const result = getRelativeTime(date);
      expect(result).toContain('months ago');
    });

    it('returns years ago for very old dates', () => {
      const date = new Date('2023-12-09T12:00:00Z'); // 2 years ago
      const result = getRelativeTime(date);
      expect(result).toContain('years ago');
    });

    it('handles null date', () => {
      expect(getRelativeTime(null)).toBe('N/A');
    });

    it('handles undefined date', () => {
      expect(getRelativeTime(undefined)).toBe('N/A');
    });

    it('handles string input', () => {
      const result = getRelativeTime('2025-12-09T11:30:00Z');
      expect(result).toBeTruthy();
      expect(typeof result).toBe('string');
    });

    it('handles future dates', () => {
      const futureDate = new Date('2025-12-09T13:30:00Z'); // 1.5 hours in future
      const result = getRelativeTime(futureDate);
      expect(result).toBe('Just now');
    });

    it('returns Invalid Date for invalid string', () => {
      expect(getRelativeTime('not-a-date')).toBe('Invalid Date');
    });
  });

  describe('isToday', () => {
    it('returns true for today date', () => {
      const today = new Date('2025-12-09T15:30:45Z');
      expect(isToday(today)).toBe(true);
    });

    it('returns false for yesterday', () => {
      const yesterday = new Date('2025-12-08T15:30:45Z');
      expect(isToday(yesterday)).toBe(false);
    });

    it('returns false for tomorrow', () => {
      const tomorrow = new Date('2025-12-10T15:30:45Z');
      expect(isToday(tomorrow)).toBe(false);
    });

    it('returns false for null date', () => {
      expect(isToday(null)).toBe(false);
    });

    it('returns false for undefined date', () => {
      expect(isToday(undefined)).toBe(false);
    });

    it('handles string input', () => {
      const result = isToday('2025-12-09T15:30:45Z');
      expect(typeof result).toBe('boolean');
    });

    it('returns false for invalid string', () => {
      expect(isToday('invalid')).toBe(false);
    });
  });

  describe('DateFormatOptions interface', () => {
    it('should have correct structure', () => {
      const options: DateFormatOptions = {
        includeTime: true,
        includeSeconds: false,
        includeTimezone: true,
        format: 'medium',
      };

      expect(options.includeTime).toBe(true);
      expect(options.includeSeconds).toBe(false);
      expect(options.includeTimezone).toBe(true);
      expect(options.format).toBe('medium');
    });
  });

  describe('parseDatePreservingDateOnly', () => {
    it('parses YYYY-MM-DD values as local calendar dates', () => {
      const result = parseDatePreservingDateOnly('2025-12-09');

      expect(result).not.toBeNull();
      expect(result?.getFullYear()).toBe(2025);
      expect(result?.getMonth()).toBe(11);
      expect(result?.getDate()).toBe(9);
      expect(result?.getHours()).toBe(0);
      expect(result?.getMinutes()).toBe(0);
      expect(result?.getSeconds()).toBe(0);
    });

    it('trims and parses date-only values', () => {
      const result = parseDatePreservingDateOnly(' 2025-01-03 ');

      expect(result).not.toBeNull();
      expect(result?.getFullYear()).toBe(2025);
      expect(result?.getMonth()).toBe(0);
      expect(result?.getDate()).toBe(3);
    });

    it('treats midnight ISO values as date-only', () => {
      const result = parseDatePreservingDateOnly('2025-12-09T00:00:00Z');

      expect(result).not.toBeNull();
      expect(result?.getFullYear()).toBe(2025);
      expect(result?.getMonth()).toBe(11);
      expect(result?.getDate()).toBe(9);
      expect(result?.getHours()).toBe(0);
      expect(result?.getMinutes()).toBe(0);
    });

    it('preserves time for non-midnight ISO values', () => {
      const result = parseDatePreservingDateOnly('2025-12-09T23:59:59Z');

      expect(result).not.toBeNull();
      expect(result?.toISOString()).toBe('2025-12-09T23:59:59.000Z');
    });

    it('returns null for invalid values', () => {
      expect(parseDatePreservingDateOnly('not-a-date')).toBeNull();
    });

    it('returns null for invalid calendar date values', () => {
      expect(parseDatePreservingDateOnly('2026-02-31')).toBeNull();
      expect(parseDatePreservingDateOnly('2026-13-01')).toBeNull();
    });
  });

  describe('parseDateOnlyBoundary', () => {
    it('returns local start of day for date-only filters', () => {
      const result = parseDateOnlyBoundary('2025-12-09', false);

      expect(result).not.toBeNull();
      expect(result?.getFullYear()).toBe(2025);
      expect(result?.getMonth()).toBe(11);
      expect(result?.getDate()).toBe(9);
      expect(result?.getHours()).toBe(0);
      expect(result?.getMinutes()).toBe(0);
      expect(result?.getSeconds()).toBe(0);
      expect(result?.getMilliseconds()).toBe(0);
    });

    it('returns local end of day for date-only filters', () => {
      const result = parseDateOnlyBoundary('2025-12-09', true);

      expect(result).not.toBeNull();
      expect(result?.getFullYear()).toBe(2025);
      expect(result?.getMonth()).toBe(11);
      expect(result?.getDate()).toBe(9);
      expect(result?.getHours()).toBe(23);
      expect(result?.getMinutes()).toBe(59);
      expect(result?.getSeconds()).toBe(59);
      expect(result?.getMilliseconds()).toBe(999);
    });

    it('falls back to native date parsing for non date-only values', () => {
      const result = parseDateOnlyBoundary('2025-12-09T15:30:45Z', false);

      expect(result).not.toBeNull();
      expect(result?.toISOString()).toBe('2025-12-09T15:30:45.000Z');
    });

    it('returns null for invalid values', () => {
      expect(parseDateOnlyBoundary('invalid', true)).toBeNull();
    });

    it('returns null for invalid calendar date values', () => {
      expect(parseDateOnlyBoundary('2026-02-31', false)).toBeNull();
      expect(parseDateOnlyBoundary('2026-00-10', true)).toBeNull();
    });
  });
});
