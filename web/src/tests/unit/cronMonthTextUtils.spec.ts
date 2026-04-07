import { describe, expect, it } from 'vitest';

import { monthlyDayText, monthlyEveryDayText } from '@/utils/cronMonthTextUtils';

describe('cronMonthTextUtils', () => {
  describe('monthlyDayText', () => {
    it('formats every month single day', () => {
      expect(monthlyDayText('15', '*', '11:30 AM')).toBe(
        'Runs on day 15 of every month at 11:30 AM'
      );
    });

    it('formats every month multiple days', () => {
      expect(monthlyDayText('1,15', '*', '9:00 AM')).toBe(
        'Runs on days 1,15 of every month at 9:00 AM'
      );
    });

    it('treats whitespace-padded wildcard as every month', () => {
      expect(monthlyDayText('15', ' * ', '11:30 AM')).toBe(
        'Runs on day 15 of every month at 11:30 AM'
      );
    });

    it('formats single numeric month as yearly schedule', () => {
      expect(monthlyDayText('7', '3', '8:00 PM')).toBe('Runs every year on March 7 at 8:00 PM');
    });

    it('formats comma-separated numeric months with conjunction', () => {
      expect(monthlyDayText('7', '3,5', '8:00 PM')).toBe(
        'Runs on day 7 of March and May at 8:00 PM'
      );
    });

    it('formats alias and full month names', () => {
      expect(monthlyDayText('7', 'JAN,September', '8:00 PM')).toBe(
        'Runs on day 7 of January and September at 8:00 PM'
      );
    });

    it('keeps unknown month tokens as-is', () => {
      expect(monthlyDayText('7', '13,FOO', '8:00 PM')).toBe(
        'Runs on day 7 of 13 and FOO at 8:00 PM'
      );
    });

    it('trims whitespace from out-of-range numeric tokens in comma-separated list', () => {
      // '12 , 13' splits into ['12 ', ' 13']; '12 ' is valid (December), ' 13' is out-of-range
      // fallback must return token.trim() = '13', not ' 13'
      expect(monthlyDayText('7', '12 , 13', '8:00 PM')).toBe(
        'Runs on day 7 of December and 13 at 8:00 PM'
      );
    });

    it('trims whitespace from unknown month-name tokens in comma-separated list', () => {
      // 'JAN , FOO ' splits into ['JAN ', ' FOO ']; JAN resolves normally, FOO is unknown
      // fallback must return token.trim() = 'FOO', not ' FOO '
      expect(monthlyDayText('7', 'JAN , FOO ', '8:00 PM')).toBe(
        'Runs on day 7 of January and FOO at 8:00 PM'
      );
    });
  });

  describe('monthlyEveryDayText', () => {
    it('treats wildcard as every month', () => {
      expect(monthlyEveryDayText('*', '6:00 AM')).toBe('Runs daily at 6:00 AM');
    });

    it('treats blank month as every month', () => {
      expect(monthlyEveryDayText('   ', '6:00 AM')).toBe('Runs daily at 6:00 AM');
    });

    it('treats whitespace-padded wildcard as every month', () => {
      expect(monthlyEveryDayText(' * ', '6:00 AM')).toBe('Runs daily at 6:00 AM');
    });

    it('formats numeric month list', () => {
      expect(monthlyEveryDayText('2,4,12', '6:00 AM')).toBe(
        'Runs every day in February, April and December at 6:00 AM'
      );
    });

    it('formats mixed month aliases', () => {
      expect(monthlyEveryDayText('AUG,nov', '6:00 AM')).toBe(
        'Runs every day in August and November at 6:00 AM'
      );
    });

    it('falls back to raw month token for unknown values', () => {
      expect(monthlyEveryDayText('BAD', '6:00 AM')).toBe('Runs every day in BAD at 6:00 AM');
    });

    it('falls back to raw text when month list is effectively empty', () => {
      expect(monthlyEveryDayText(' , ', '6:00 AM')).toBe('Runs every day in  ,  at 6:00 AM');
    });
  });
});
