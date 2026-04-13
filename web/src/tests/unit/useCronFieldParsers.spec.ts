import { describe, expect, it } from 'vitest';

import {
  collectDayOfMonthMatches,
  matchesLastWeekday,
  matchesNthWeekday,
  normalizeOffsetLabel,
  parseDayOfWeekField,
  parseMonthField,
  parseNumericField,
  resolveNearestWeekdayDate,
  resolveQuartzDowIndex,
  toOrdinal,
} from '@/composables/cron/useCronFieldParsers';

describe('useCronFieldParsers', () => {
  describe('parseNumericField', () => {
    it('expands wildcard and question fields across the full range', () => {
      expect(parseNumericField('*', 1, 5)).toEqual([1, 2, 3, 4, 5]);
      expect(parseNumericField('?', 0, 2)).toEqual([0, 1, 2]);
    });

    it('parses ranges, steps, single values, and ignores invalid tokens', () => {
      expect(parseNumericField('1-3, 2, 6/2, 20, nope', 0, 10)).toEqual([1, 2, 3, 6, 8, 10]);
      expect(parseNumericField('*/4', 0, 12)).toEqual([0, 4, 8, 12]);
      expect(parseNumericField('5/0,7', 0, 10)).toEqual([7]);
    });
  });

  describe('parseMonthField', () => {
    it('supports wildcards, aliases, ranges, and wrapped step ranges', () => {
      expect(parseMonthField('*')).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]);
      expect(parseMonthField('JAN, MAR-MAY, SEP/2')).toEqual([1, 3, 4, 5, 9, 11]);
      expect(parseMonthField('NOV-FEB/2')).toEqual([1, 11]);
    });

    it('ignores invalid month steps and invalid single tokens', () => {
      expect(parseMonthField('JAN/0,13,XYZ')).toEqual([]);
    });
  });

  describe('resolveQuartzDowIndex', () => {
    it('maps named, numeric, wrapped, and invalid day-of-week tokens', () => {
      expect(resolveQuartzDowIndex('SUN')).toBe(0);
      expect(resolveQuartzDowIndex('2')).toBe(1);
      expect(resolveQuartzDowIndex('8')).toBe(0);
      expect(resolveQuartzDowIndex('0')).toBe(0);
      expect(resolveQuartzDowIndex('-1')).toBe(-1);
      expect(resolveQuartzDowIndex('bad')).toBe(-1);
    });
  });

  describe('weekday matching', () => {
    it('matches nth weekdays only when both weekday and ordinal match', () => {
      expect(matchesNthWeekday(8, 1, 'MON', 2)).toBe(true);
      expect(matchesNthWeekday(8, 2, 'MON', 2)).toBe(false);
      expect(matchesNthWeekday(8, 1, 'MON', 1)).toBe(false);
      expect(matchesNthWeekday(8, 1, 'BAD', 2)).toBe(false);
    });

    it('matches the last weekday of a month correctly', () => {
      expect(matchesLastWeekday(26, 1, 1, 2024, 'MON')).toBe(true);
      expect(matchesLastWeekday(19, 1, 1, 2024, 'MON')).toBe(false);
      expect(matchesLastWeekday(26, 2, 1, 2024, 'MON')).toBe(false);
      expect(matchesLastWeekday(26, 1, 2024, 2024, 'BAD')).toBe(false);
    });
  });

  describe('resolveNearestWeekdayDate', () => {
    it('handles LW, L-offset weekdays, and numbered weekday expressions', () => {
      expect(resolveNearestWeekdayDate('LW', 2024, 1)).toBe(29);
      expect(resolveNearestWeekdayDate('L-2W', 2024, 1)).toBe(27);
      expect(resolveNearestWeekdayDate('1W', 2022, 9)).toBe(3);
      expect(resolveNearestWeekdayDate('15W', 2024, 5)).toBe(14);
    });

    it('returns null for unsupported or impossible nearest weekday expressions', () => {
      expect(resolveNearestWeekdayDate('32W', 2024, 0)).toBeNull();
      expect(resolveNearestWeekdayDate('L-40W', 2024, 1)).toBeNull();
      expect(resolveNearestWeekdayDate('MON', 2024, 0)).toBeNull();
    });
  });

  describe('collectDayOfMonthMatches', () => {
    it('expands wildcard and question fields to the full valid month length', () => {
      expect(collectDayOfMonthMatches('*', 2024, 1)).toHaveLength(29);
      expect(collectDayOfMonthMatches('?', 2023, 1)).toHaveLength(28);
    });

    it('collects nearest weekdays, last-day tokens, ranges, steps, and deduplicates values', () => {
      expect(collectDayOfMonthMatches('1W,L,L-2,10-12,20/5,31', 2022, 0)).toEqual([
        3, 10, 11, 12, 20, 25, 29, 30, 31,
      ]);
    });

    it('clamps invalid values to the actual month length and ignores impossible offsets', () => {
      expect(collectDayOfMonthMatches('30,28-31,L-40', 2021, 1)).toEqual([28]);
    });
  });

  describe('parseDayOfWeekField', () => {
    it('expands wildcard, ranges, steps, and wrapped ranges', () => {
      expect(parseDayOfWeekField('*')).toEqual([0, 1, 2, 3, 4, 5, 6]);
      expect(parseDayOfWeekField('MON-WED,FRI')).toEqual([1, 2, 3, 5]);
      expect(parseDayOfWeekField('*/2')).toEqual([0, 2, 4, 6]);
      expect(parseDayOfWeekField('FRI-MON')).toEqual([0, 1, 5, 6]);
    });

    it('ignores invalid step tokens and supports numeric tokens', () => {
      expect(parseDayOfWeekField('MON/0,7')).toEqual([6]);
      expect(parseDayOfWeekField('2,4')).toEqual([1, 3]);
    });
  });

  describe('format helpers', () => {
    it('formats ordinals with English suffixes', () => {
      expect(toOrdinal(1)).toBe('1st');
      expect(toOrdinal(2)).toBe('2nd');
      expect(toOrdinal(3)).toBe('3rd');
      expect(toOrdinal(4)).toBe('4th');
      expect(toOrdinal(11)).toBe('11th');
      expect(toOrdinal(12)).toBe('12th');
      expect(toOrdinal(13)).toBe('13th');
      expect(toOrdinal(21)).toBe('21st');
    });

    it('normalizes GMT and UTC offsets consistently', () => {
      expect(normalizeOffsetLabel('GMT')).toBe('GMT+00:00');
      expect(normalizeOffsetLabel('UTC')).toBe('GMT+00:00');
      expect(normalizeOffsetLabel('GMT+5')).toBe('GMT+05:00');
      expect(normalizeOffsetLabel('GMT-4:30')).toBe('GMT-04:30');
      expect(normalizeOffsetLabel('America/Chicago')).toBe('America/Chicago');
    });
  });
});
