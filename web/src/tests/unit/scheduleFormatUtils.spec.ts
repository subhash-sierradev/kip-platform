import { describe, expect, it } from 'vitest';

import {
  formatDailySchedule,
  formatExecutionDate,
  formatNameArray,
  formatScheduleByPattern,
  formatTime,
  getScheduleText,
  parseScheduleArray,
  type ScheduleInfo,
} from '@/utils/scheduleFormatUtils';

describe('scheduleFormatUtils', () => {
  describe('parseScheduleArray', () => {
    it('should return empty array for undefined input', () => {
      expect(parseScheduleArray(undefined)).toEqual([]);
    });

    it('should return empty array for null input', () => {
      expect(parseScheduleArray(null as any)).toEqual([]);
    });

    it('should return empty array for empty string', () => {
      expect(parseScheduleArray('')).toEqual([]);
    });

    it('should return the same array if input is already an array', () => {
      const input = ['MONDAY', 'TUESDAY', 'WEDNESDAY'];
      expect(parseScheduleArray(input)).toEqual(input);
    });

    it('should parse valid JSON string to array', () => {
      const input = '["MONDAY", "TUESDAY", "WEDNESDAY"]';
      expect(parseScheduleArray(input)).toEqual(['MONDAY', 'TUESDAY', 'WEDNESDAY']);
    });

    it('should return single-item array for non-JSON, non-empty strings', () => {
      const input = 'not-valid-json';
      expect(parseScheduleArray(input)).toEqual(['not-valid-json']);
    });

    it('should split comma-separated values', () => {
      const input = 'THU,FRI';
      expect(parseScheduleArray(input)).toEqual(['THU', 'FRI']);
    });

    it('should trim whitespace around comma-separated values', () => {
      const input = ' JAN , MAR ,  MAY ';
      expect(parseScheduleArray(input)).toEqual(['JAN', 'MAR', 'MAY']);
    });

    it('should fallback to comma-split for malformed JSON-like strings', () => {
      const input = '["MONDAY", "TUESDAY"'; // Missing closing bracket
      expect(parseScheduleArray(input)).toEqual(['["MONDAY"', '"TUESDAY"']);
    });
  });

  describe('formatNameArray', () => {
    it('should format single item with proper capitalization', () => {
      expect(formatNameArray(['MONDAY'])).toBe('Monday');
    });

    it('should format multiple items with commas', () => {
      expect(formatNameArray(['MONDAY', 'TUESDAY', 'WEDNESDAY'])).toBe(
        'Monday, Tuesday, Wednesday'
      );
    });

    it('should handle lowercase input', () => {
      expect(formatNameArray(['monday', 'tuesday'])).toBe('Monday, Tuesday');
    });

    it('should handle mixed case input', () => {
      expect(formatNameArray(['MoNdAy', 'TuEsDaY'])).toBe('Monday, Tuesday');
    });

    it('should return empty string for empty array', () => {
      expect(formatNameArray([])).toBe('');
    });

    it('should handle single character items', () => {
      expect(formatNameArray(['A', 'B', 'C'])).toBe('A, B, C');
    });
  });

  describe('formatTime', () => {
    it('should return default time for undefined input', () => {
      expect(formatTime()).toBe('09:00 AM');
    });

    it('should return default time for empty string', () => {
      expect(formatTime('')).toBe('09:00 AM');
    });

    it('should format morning time correctly', () => {
      expect(formatTime('09:30:00')).toBe('9:30 AM');
    });

    it('should format afternoon time correctly', () => {
      expect(formatTime('14:45:00')).toBe('2:45 PM');
    });

    it('should handle midnight (00:00)', () => {
      expect(formatTime('00:00:00')).toBe('12:00 AM');
    });

    it('should handle noon (12:00)', () => {
      expect(formatTime('12:00:00')).toBe('12:00 PM');
    });

    it('should handle 1 AM', () => {
      expect(formatTime('01:15:00')).toBe('1:15 AM');
    });

    it('should handle 11 PM', () => {
      expect(formatTime('23:59:00')).toBe('11:59 PM');
    });
  });

  describe('formatDailySchedule', () => {
    it('should return "Daily at time" for interval of 1', () => {
      expect(formatDailySchedule(1, '9:00 AM')).toBe('Runs every 1 hour starting at 9:00 AM');
    });

    it('should return "Daily at time" for interval of 24', () => {
      expect(formatDailySchedule(24, '2:30 PM')).toBe('Runs every 24 hours starting at 2:30 PM');
    });

    it('should return "Every X hours at time" for other intervals', () => {
      expect(formatDailySchedule(6, '10:00 AM')).toBe('Runs every 6 hours starting at 10:00 AM');
    });

    it('should handle interval of 2', () => {
      expect(formatDailySchedule(2, '8:15 AM')).toBe('Runs every 2 hours starting at 8:15 AM');
    });

    it('should handle interval of 12', () => {
      expect(formatDailySchedule(12, '6:00 PM')).toBe('Runs every 12 hours starting at 6:00 PM');
    });
  });

  describe('formatScheduleByPattern', () => {
    const baseScheduleInfo: ScheduleInfo = {
      dailyExecutionInterval: 1,
      executionTime: '09:00:00',
    };

    describe('DAILY pattern', () => {
      it('should format daily schedule with interval 1', () => {
        const scheduleInfo = { ...baseScheduleInfo, dailyExecutionInterval: 1 };
        expect(formatScheduleByPattern('DAILY', scheduleInfo, '9:00 AM')).toBe(
          'Runs every 1 hour starting at 9:00 AM'
        );
      });

      it('should format daily schedule with interval 24', () => {
        const scheduleInfo = { ...baseScheduleInfo, dailyExecutionInterval: 24 };
        expect(formatScheduleByPattern('DAILY', scheduleInfo, '9:00 AM')).toBe(
          'Runs every 24 hours starting at 9:00 AM'
        );
      });

      it('should format daily schedule with custom interval', () => {
        const scheduleInfo = { ...baseScheduleInfo, dailyExecutionInterval: 6 };
        expect(formatScheduleByPattern('DAILY', scheduleInfo, '9:00 AM')).toBe(
          'Runs every 6 hours starting at 9:00 AM'
        );
      });
    });

    describe('WEEKLY pattern', () => {
      it('should format weekly schedule with specified days', () => {
        const scheduleInfo = {
          ...baseScheduleInfo,
          daySchedule: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
        };
        expect(formatScheduleByPattern('WEEKLY', scheduleInfo, '9:00 AM')).toBe(
          'Weekly on Monday, Wednesday, Friday at 9:00 AM'
        );
      });

      it('should use default days when daySchedule is empty', () => {
        const scheduleInfo = { ...baseScheduleInfo, daySchedule: [] };
        expect(formatScheduleByPattern('WEEKLY', scheduleInfo, '9:00 AM')).toBe(
          'Weekly on Mon, Tue, Wed, Thu, Fri at 9:00 AM'
        );
      });

      it('should handle daySchedule as JSON string', () => {
        const scheduleInfo = {
          ...baseScheduleInfo,
          daySchedule: '["TUESDAY", "THURSDAY"]',
        };
        expect(formatScheduleByPattern('WEEKLY', scheduleInfo, '9:00 AM')).toBe(
          'Weekly on Tuesday, Thursday at 9:00 AM'
        );
      });
    });

    describe('MONTHLY pattern', () => {
      it('should format monthly schedule with specified months', () => {
        const scheduleInfo = {
          ...baseScheduleInfo,
          monthSchedule: ['JANUARY', 'MARCH', 'MAY'],
        };
        expect(formatScheduleByPattern('MONTHLY', scheduleInfo, '9:00 AM')).toBe(
          'Monthly in January, March, May at 9:00 AM'
        );
      });

      it('should use default month when monthSchedule is empty', () => {
        const scheduleInfo = { ...baseScheduleInfo, monthSchedule: [] };
        expect(formatScheduleByPattern('MONTHLY', scheduleInfo, '9:00 AM')).toBe(
          'Monthly at 9:00 AM'
        );
      });
    });

    describe('YEARLY pattern', () => {
      it('should format yearly schedule with specified month', () => {
        const scheduleInfo = {
          ...baseScheduleInfo,
          monthSchedule: ['DECEMBER'],
        };
        expect(formatScheduleByPattern('YEARLY', scheduleInfo, '9:00 AM')).toBe(
          'Yearly in December at 9:00 AM'
        );
      });

      it('should use default month when monthSchedule is empty', () => {
        const scheduleInfo = { ...baseScheduleInfo, monthSchedule: [] };
        expect(formatScheduleByPattern('YEARLY', scheduleInfo, '9:00 AM')).toBe(
          'Yearly in Jan at 9:00 AM'
        );
      });

      it('should use first month when multiple months provided', () => {
        const scheduleInfo = {
          ...baseScheduleInfo,
          monthSchedule: ['JUNE', 'JULY', 'AUGUST'],
        };
        expect(formatScheduleByPattern('YEARLY', scheduleInfo, '9:00 AM')).toBe(
          'Yearly in June at 9:00 AM'
        );
      });
    });

    describe('default/unknown pattern', () => {
      it('should return hourly format for interval 1', () => {
        const scheduleInfo = { ...baseScheduleInfo, dailyExecutionInterval: 1 };
        expect(formatScheduleByPattern('UNKNOWN', scheduleInfo, '9:00 AM')).toBe(
          'Hourly at 9:00 AM'
        );
      });

      it('should return interval format for other intervals', () => {
        const scheduleInfo = { ...baseScheduleInfo, dailyExecutionInterval: 4 };
        expect(formatScheduleByPattern('CUSTOM', scheduleInfo, '9:00 AM')).toBe(
          'Every 4 hours at 9:00 AM'
        );
      });
    });

    describe('edge cases', () => {
      it('should handle missing dailyExecutionInterval (defaults to 1)', () => {
        const scheduleInfo = { executionTime: '09:00:00' };
        expect(formatScheduleByPattern('DAILY', scheduleInfo, '9:00 AM')).toBe(
          'Runs every 1 hour starting at 9:00 AM'
        );
      });
    });
  });

  describe('getScheduleText', () => {
    it('should return "Not scheduled" when frequencyPattern is missing', () => {
      const scheduleInfo: ScheduleInfo = {};
      expect(getScheduleText(scheduleInfo)).toBe('Not scheduled');
    });

    it('should return "Not scheduled" when frequencyPattern is empty', () => {
      const scheduleInfo: ScheduleInfo = { frequencyPattern: '' };
      expect(getScheduleText(scheduleInfo)).toBe('Not scheduled');
    });

    it('should format daily schedule correctly', () => {
      const scheduleInfo: ScheduleInfo = {
        frequencyPattern: 'DAILY',
        executionTime: '14:30:00',
        dailyExecutionInterval: 1,
      };
      expect(getScheduleText(scheduleInfo)).toBe('Runs every 1 hour starting at 2:30 PM');
    });

    it('should format weekly schedule correctly', () => {
      const scheduleInfo: ScheduleInfo = {
        frequencyPattern: 'WEEKLY',
        executionTime: '09:00:00',
        daySchedule: ['MONDAY', 'FRIDAY'],
      };
      expect(getScheduleText(scheduleInfo)).toBe('Weekly on Monday, Friday at 9:00 AM');
    });

    it('should use default execution time when missing', () => {
      const scheduleInfo: ScheduleInfo = {
        frequencyPattern: 'DAILY',
      };
      expect(getScheduleText(scheduleInfo)).toBe('Runs every 1 hour starting at 09:00 AM');
    });
  });

  describe('formatExecutionDate', () => {
    it('should format ISO date string correctly', () => {
      expect(formatExecutionDate('2025-12-19')).toBe('Dec 19, 2025');
    });

    it('should format another date correctly', () => {
      expect(formatExecutionDate('2024-01-15')).toBe('Jan 15, 2024');
    });

    it('should handle different month formats', () => {
      expect(formatExecutionDate('2025-06-30')).toBe('Jun 30, 2025');
    });
  });
});
