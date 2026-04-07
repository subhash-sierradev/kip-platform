import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  convertUtcTimeToUserTimezone,
  formatDateOnlyDisplay,
  formatSelectedDaysDisplay,
} from '@/utils/scheduleDisplayUtils';
import * as timezoneUtils from '@/utils/timezoneUtils';

describe('scheduleDisplayUtils', () => {
  describe('convertUtcTimeToUserTimezone', () => {
    beforeEach(() => {
      vi.restoreAllMocks();
    });

    it('converts valid UTC time string to user timezone', () => {
      vi.spyOn(timezoneUtils, 'getUserTimezone').mockReturnValue('America/New_York');

      const result = convertUtcTimeToUserTimezone('14:30:00');

      // Result should be a time string in HH:MM format
      expect(result).toMatch(/^\d{1,2}:\d{2}$/);
    });

    it('uses provided executionDate for time conversion', () => {
      vi.spyOn(timezoneUtils, 'getUserTimezone').mockReturnValue('Asia/Kolkata');

      // executionDate parameter expects YYYY-MM-DD string format
      const executionDate = '2024-01-15';
      const result = convertUtcTimeToUserTimezone('09:00:00', executionDate);

      expect(result).toMatch(/^\d{1,2}:\d{2}$/);
    });

    it('returns original string when time format is invalid', () => {
      const invalidTime = 'invalid-time';
      const result = convertUtcTimeToUserTimezone(invalidTime);

      expect(result).toBe(invalidTime);
    });

    it('converts even malformed time strings when parsed as numbers', () => {
      // Even "invalid" times like 25:70:99 get parsed by parseInt as 25 hours, 70 minutes
      // This creates a valid Date object, so conversion succeeds
      const invalidTime = '25:70:99';
      const result = convertUtcTimeToUserTimezone(invalidTime);

      // Should return a time string, not the original (conversion happens)
      expect(result).toMatch(/^\d{1,2}:\d{2}$/);
    });

    it('handles midnight UTC time (00:00:00)', () => {
      vi.spyOn(timezoneUtils, 'getUserTimezone').mockReturnValue('UTC');

      const result = convertUtcTimeToUserTimezone('00:00:00');

      expect(result).toMatch(/^\d{1,2}:\d{2}$/);
    });

    it('handles end of day UTC time (23:59:59)', () => {
      vi.spyOn(timezoneUtils, 'getUserTimezone').mockReturnValue('UTC');

      const result = convertUtcTimeToUserTimezone('23:59:59');

      expect(result).toMatch(/^\d{1,2}:\d{2}$/);
    });

    it('returns original string when timezone conversion throws error', () => {
      vi.spyOn(timezoneUtils, 'getUserTimezone').mockReturnValue('Invalid/Timezone');

      const timeStr = '12:00:00';
      const result = convertUtcTimeToUserTimezone(timeStr);

      // Should return original string on error
      expect(result).toBe(timeStr);
    });

    it('converts empty string to 00:00 time', () => {
      vi.spyOn(timezoneUtils, 'getUserTimezone').mockReturnValue('UTC');

      // Empty string gets parsed as 0:0 which is midnight
      const result = convertUtcTimeToUserTimezone('');

      expect(result).toMatch(/^\d{1,2}:\d{2}$/);
    });

    it('handles time string with single digit hours', () => {
      vi.spyOn(timezoneUtils, 'getUserTimezone').mockReturnValue('UTC');

      const result = convertUtcTimeToUserTimezone('9:30:00');

      expect(result).toMatch(/^\d{1,2}:\d{2}$/);
    });

    it('handles different timezone offsets correctly', () => {
      vi.spyOn(timezoneUtils, 'getUserTimezone').mockReturnValue('Pacific/Auckland');

      const result = convertUtcTimeToUserTimezone('10:00:00');

      expect(result).toMatch(/^\d{1,2}:\d{2}$/);
    });
  });

  describe('formatSelectedDaysDisplay', () => {
    it('handles mixed case strings and sorts to MON, TUE, WED order', () => {
      const input = ['monday', 'WED', 'fri'];
      expect(formatSelectedDaysDisplay(input)).toBe('MON, WED, FRI');
    });

    it('deduplicates normalized values', () => {
      const input = ['MON', 'monday', 'MONDAY', 'Mon'];
      expect(formatSelectedDaysDisplay(input)).toBe('MON');
    });

    it('parses numeric inputs (array) and maps/clamps correctly', () => {
      const input = [2, 0, 1, 9]; // 9 clamps to 6 (SAT)
      expect(formatSelectedDaysDisplay(input)).toBe('MON, TUE, SAT, SUN');
    });

    it('parses comma-separated string with mixed tokens', () => {
      const input = 'Tue, mon, MONDAY, 3';
      // Tue -> TUE, mon/MONDAY -> MON, 3 -> WED; sorted by order
      expect(formatSelectedDaysDisplay(input)).toBe('MON, TUE, WED');
    });

    it('filters unknown tokens and keeps valid numeric token', () => {
      const input = 'abc, 9'; // abc ignored, 9 -> clamps to 6 -> SAT
      expect(formatSelectedDaysDisplay(input)).toBe('SAT');
    });

    it('returns empty string for non-array/non-string input', () => {
      expect(formatSelectedDaysDisplay(undefined)).toBe('');

      expect(formatSelectedDaysDisplay(null)).toBe('');

      expect(formatSelectedDaysDisplay(42)).toBe('');

      expect(formatSelectedDaysDisplay({})).toBe('');
    });

    it('handles empty array input', () => {
      expect(formatSelectedDaysDisplay([])).toBe('');
    });

    it('handles empty string input', () => {
      expect(formatSelectedDaysDisplay('')).toBe('');
    });

    it('handles string with only commas', () => {
      expect(formatSelectedDaysDisplay(',,,')).toBe('');
    });

    it('handles all seven days in weekday order starting with Monday', () => {
      const input = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
      // WEEKDAY_ABBR_ORDER sorts as MON, TUE, WED, THU, FRI, SAT, SUN
      expect(formatSelectedDaysDisplay(input)).toBe('MON, TUE, WED, THU, FRI, SAT, SUN');
    });

    it('handles numeric day 0 as Sunday', () => {
      expect(formatSelectedDaysDisplay([0])).toBe('SUN');
    });

    it('handles numeric day 6 as Saturday', () => {
      expect(formatSelectedDaysDisplay([6])).toBe('SAT');
    });

    it('handles day name abbreviations', () => {
      const input = ['MON', 'TUE', 'WED'];
      expect(formatSelectedDaysDisplay(input)).toBe('MON, TUE, WED');
    });

    it('handles full day names', () => {
      const input = ['Monday', 'Tuesday', 'Wednesday'];
      expect(formatSelectedDaysDisplay(input)).toBe('MON, TUE, WED');
    });
  });

  describe('formatDateOnlyDisplay', () => {
    it('formats YYYY-MM-DD date-only values as local calendar date', () => {
      const output = formatDateOnlyDisplay('2026-03-20');
      expect(output).toContain('Mar');
      expect(output).toContain('20');
      expect(output).toContain('2026');
    });

    it('formats ISO datetime by preserving the date portion', () => {
      const output = formatDateOnlyDisplay('2026-03-20T00:00:00Z');
      expect(output).toContain('Mar');
      expect(output).toContain('20');
      expect(output).toContain('2026');
    });

    it('returns provided empty fallback for empty values', () => {
      expect(formatDateOnlyDisplay('', undefined, 'Not configured')).toBe('Not configured');
    });

    it('returns invalid fallback for unparseable values', () => {
      expect(formatDateOnlyDisplay('not-a-date')).toBe('Invalid Date');
    });
  });
});
