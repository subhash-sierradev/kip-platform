import { describe, expect, it } from 'vitest';

import type { LocalOccurrence } from '@/composables/cron/useCronOccurrenceEngine';
import {
  analyzeLocalPattern,
  generateOccurrences,
  parseQuartzCron,
  validateTimeZone,
} from '@/composables/cron/useCronOccurrenceEngine';

function makeOccurrence(
  localDate: string,
  localTime: string = '10:00 AM',
  offset: string = 'GMT+00:00'
): LocalOccurrence {
  return {
    utcDate: new Date(`${localDate}T10:00:00Z`),
    localDate,
    localTime,
    offset,
  };
}

describe('useCronOccurrenceEngine', () => {
  describe('parseQuartzCron', () => {
    it('rejects empty and malformed cron expressions', () => {
      expect(parseQuartzCron('').success).toBe(false);
      expect(parseQuartzCron('0 0 10 *').error).toBe('Cron must have 5 or 6 fields');
      expect(parseQuartzCron('0 0 10 * * ? extra').error).toBe('Cron must have 5 or 6 fields');
    });

    it('parses 5-field and 6-field Quartz cron expressions', () => {
      const fiveField = parseQuartzCron('0 10 * * ?');
      expect(fiveField.success).toBe(true);
      expect(fiveField.data?.fields.second).toBe('0');
      expect(fiveField.data?.fields.minute).toBe('0');
      expect(fiveField.data?.isQuartzSpecial).toBe(false);

      const sixField = parseQuartzCron('0 0 10 L * MON');
      expect(sixField.success).toBe(true);
      expect(sixField.data?.fields.dayOfMonth).toBe('L');
      expect(sixField.data?.isQuartzSpecial).toBe(true);
      expect(sixField.data?.isSameDayOfWeekAndMonth).toBe(true);
    });
  });

  describe('validateTimeZone', () => {
    it('validates good timezones and rejects invalid ones', () => {
      expect(validateTimeZone('UTC')).toEqual({ success: true, data: 'UTC' });
      expect(validateTimeZone('Bad/Timezone').success).toBe(false);
    });
  });

  describe('generateOccurrences', () => {
    it('returns validation errors for invalid timezone and cron inputs', () => {
      const start = new Date('2024-01-01T00:00:00Z');
      const end = new Date('2024-01-03T23:59:59Z');

      expect(generateOccurrences('0 0 10 * * ?', start, end, 'Bad/Timezone').success).toBe(false);
      expect(generateOccurrences('bad cron', start, end, 'UTC').success).toBe(false);
    });

    it('generates local occurrences for a simple daily schedule', () => {
      const start = new Date('2024-01-01T00:00:00Z');
      const end = new Date('2024-01-03T23:59:59Z');
      const result = generateOccurrences('0 0 10 * * ?', start, end, 'UTC');

      expect(result.success).toBe(true);
      expect(result.data).toHaveLength(3);
      expect(result.data?.map(occ => occ.localDate)).toEqual([
        '2024-01-01',
        '2024-01-02',
        '2024-01-03',
      ]);
      expect(result.data?.[0].localTime).toBe('10:00 AM');
      expect(result.data?.[0].offset).toBe('GMT+00:00');
    });

    it('uses OR logic when both day-of-month and day-of-week are restricted', () => {
      const start = new Date('2024-01-01T00:00:00Z');
      const end = new Date('2024-01-15T23:59:59Z');
      const result = generateOccurrences('0 0 10 1 * MON', start, end, 'UTC');

      expect(result.success).toBe(true);
      expect(result.data?.map(occ => occ.localDate)).toEqual([
        '2024-01-01',
        '2024-01-08',
        '2024-01-15',
      ]);
    });

    it('honors the maxOccurrences limit', () => {
      const start = new Date('2024-01-01T00:00:00Z');
      const end = new Date('2024-01-10T23:59:59Z');
      const result = generateOccurrences('0 0 10 * * ?', start, end, 'UTC', 2);

      expect(result.success).toBe(true);
      expect(result.data).toHaveLength(2);
    });

    it('generates nth-weekday and last-weekday Quartz schedules', () => {
      const start = new Date('2024-01-01T00:00:00Z');
      const end = new Date('2024-03-31T23:59:59Z');

      const nthWeekday = generateOccurrences('0 0 10 ? * MON#2', start, end, 'UTC');
      expect(nthWeekday.success).toBe(true);
      expect(nthWeekday.data?.map(occ => occ.localDate)).toEqual([
        '2024-01-08',
        '2024-02-12',
        '2024-03-11',
      ]);

      const lastWeekday = generateOccurrences('0 0 10 ? * FRIL', start, end, 'UTC');
      expect(lastWeekday.success).toBe(true);
      expect(lastWeekday.data?.map(occ => occ.localDate)).toEqual([
        '2024-01-26',
        '2024-02-23',
        '2024-03-29',
      ]);
    });

    it('supports stepped minute fields and month filters from five-field cron input', () => {
      const start = new Date('2024-01-01T00:00:00Z');
      const end = new Date('2024-01-01T02:59:59Z');
      const result = generateOccurrences('0 */30 0-1 1 JAN ?', start, end, 'UTC');

      expect(result.success).toBe(true);
      expect(result.data?.map(occ => occ.localTime)).toEqual([
        '12:00 AM',
        '12:30 AM',
        '1:00 AM',
        '1:30 AM',
      ]);
    });
  });

  describe('analyzeLocalPattern', () => {
    it('returns an empty pattern when there are no occurrences', () => {
      expect(analyzeLocalPattern([])).toEqual({
        type: 'empty',
        occurrences: [],
        uniqueDates: [],
        uniqueTimes: [],
        uniqueOffsets: [],
        hasTimeVariance: false,
        hasOffsetVariance: false,
      });
    });

    it('classifies a single occurrence', () => {
      const pattern = analyzeLocalPattern([makeOccurrence('2024-01-01')]);
      expect(pattern.type).toBe('single-occurrence');
    });

    it('classifies daily consecutive schedules', () => {
      const pattern = analyzeLocalPattern([
        makeOccurrence('2024-01-01'),
        makeOccurrence('2024-01-02'),
        makeOccurrence('2024-01-03'),
      ]);
      expect(pattern.type).toBe('daily');
    });

    it('classifies weekly schedules based on recurring weekdays', () => {
      const pattern = analyzeLocalPattern([
        makeOccurrence('2024-01-01'),
        makeOccurrence('2024-01-08'),
        makeOccurrence('2024-01-15'),
        makeOccurrence('2024-01-22'),
        makeOccurrence('2024-01-29'),
        makeOccurrence('2024-02-05'),
        makeOccurrence('2024-02-12'),
      ]);
      expect(pattern.type).toBe('weekly');
    });

    it('classifies monthly schedules when only a small set of month days repeat', () => {
      const pattern = analyzeLocalPattern([
        makeOccurrence('2024-01-15'),
        makeOccurrence('2024-02-15'),
        makeOccurrence('2024-03-15'),
      ]);
      expect(pattern.type).toBe('monthly');
    });

    it('classifies regular non-consecutive intervals', () => {
      const pattern = analyzeLocalPattern([
        makeOccurrence('2024-01-01'),
        makeOccurrence('2024-01-15'),
        makeOccurrence('2024-01-29'),
        makeOccurrence('2024-02-12'),
      ]);
      expect(pattern.type).toBe('interval');
      expect(pattern.regularityPattern).toBe('every 14 days');
    });

    it('classifies varying local times and offsets correctly', () => {
      const pattern = analyzeLocalPattern([
        makeOccurrence('2024-01-01', '10:00 AM', 'GMT+00:00'),
        makeOccurrence('2024-01-02', '11:00 AM', 'GMT+01:00'),
      ]);

      expect(pattern.type).toBe('time-varies');
      expect(pattern.hasTimeVariance).toBe(true);
      expect(pattern.hasOffsetVariance).toBe(true);
    });

    it('keeps irregular schedules as interval patterns without a regularity label', () => {
      const pattern = analyzeLocalPattern([
        makeOccurrence('2024-01-01'),
        makeOccurrence('2024-01-05'),
        makeOccurrence('2024-01-12'),
        makeOccurrence('2024-01-22'),
      ]);

      expect(pattern.type).toBe('interval');
      expect(pattern.regularityPattern).toBe('');
      expect(pattern.hasTimeVariance).toBe(false);
    });
  });
});
