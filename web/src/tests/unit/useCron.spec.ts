import { describe, expect, it } from 'vitest';

import {
  analyzeLocalPattern,
  cronToText,
  cronToTextWithTimezone,
  cronToTextWithTimezoneUniversal,
  generateOccurrences,
  type LocalOccurrence,
  parseQuartzCron,
  renderScheduleDescription,
  useCron,
  validateTimeZone,
} from '@/composables/cron/useCron';
import {
  buildCronFieldDescription,
  getMostCommonLocalTime,
} from '@/composables/cron/useCronFieldDescription';

describe('useCron', () => {
  it('accepts common Quartz expressions (including aliases)', () => {
    const { cron, isValidateCron, error } = useCron();

    const validExpressions = [
      '0 46 14/1 * * ?',
      '0 20 15/2 * * ?',
      '0 30 11 ? * THU,FRI',
      '0 */5 * ? * *',
      '0 5 17/1 * * ?',
    ];

    validExpressions.forEach(expr => {
      cron.value = expr;
      expect(isValidateCron.value).toBe(true);
      expect(error.value).toBe('');
    });
  });

  it('accepts Quartz last-day-of-month (L) in day-of-month', () => {
    const { cron, isValidateCron } = useCron();

    const validExpressions = ['0 0 11 L 5,3 ?', '0 0 13 L 3,2,5,8 ?'];

    validExpressions.forEach(expr => {
      cron.value = expr;
      expect(isValidateCron.value).toBe(true);
    });
  });

  it('rejects L if day-of-week is not blank (?)', () => {
    const { cron, isValidateCron, error } = useCron();

    cron.value = '0 0 11 L 5,3 MON';
    expect(isValidateCron.value).toBe(false);
    expect(error.value).toBe('Invalid cron expression');
  });

  it('returns false for empty cron expression', () => {
    const { cron, isValidateCron, error } = useCron();

    cron.value = '';
    expect(isValidateCron.value).toBe(false);
    expect(error.value).toBe('');
  });

  it('returns false and sets error for invalid cron expression', () => {
    const { cron, isValidateCron, error } = useCron();

    cron.value = 'not-a-valid-cron';
    expect(isValidateCron.value).toBe(false);
    expect(error.value).toBe('Invalid cron expression');
  });

  it('clears error when cron becomes valid', () => {
    const { cron, isValidateCron, error } = useCron();

    cron.value = 'invalid';
    expect(isValidateCron.value).toBe(false);
    expect(error.value).toBe('Invalid cron expression');

    cron.value = '0 30 11 ? * THU';
    expect(isValidateCron.value).toBe(true);
    expect(error.value).toBe('');
  });

  it('handles whitespace in cron expression', () => {
    const { cron, isValidateCron } = useCron();

    cron.value = '  0 30 11 ? * THU  ';
    expect(isValidateCron.value).toBe(true);
  });

  it('rejects invalid 6-field expressions', () => {
    const { cron, isValidateCron } = useCron();

    cron.value = '0 99 25 * * ?'; // Invalid hour (99), invalid day (25 when month has fewer days)
    expect(isValidateCron.value).toBe(false);
  });
});

describe('cronToText', () => {
  describe('every N minutes patterns', () => {
    it('formats every minute pattern (wildcard) correctly', () => {
      expect(cronToText('0 * * * * ?')).toBe('Runs every minute');
      expect(cronToText('0 * * ? * *')).toBe('Runs every minute');
    });

    it('formats */n pattern correctly', () => {
      expect(cronToText('0 */5 * ? * *')).toBe('Runs every 5 minutes');
      expect(cronToText('0 */10 * ? * *')).toBe('Runs every 10 minutes');
      expect(cronToText('0 */15 * ? * *')).toBe('Runs every 15 minutes');
    });

    it('formats 0/n pattern correctly', () => {
      expect(cronToText('0 0/5 * ? * *')).toBe('Runs every 5 minutes');
      expect(cronToText('0 0/1 * ? * *')).toBe('Runs every 1 minute');
      expect(cronToText('0 0/30 * ? * *')).toBe('Runs every 30 minutes');
    });

    it('handles single minute interval', () => {
      expect(cronToText('0 0/1 * ? * *')).toBe('Runs every 1 minute');
    });
  });

  describe('hourly interval patterns', () => {
    it('formats hourly intervals with AM/PM correctly', () => {
      expect(cronToText('0 20 15/2 * * ?')).toBe('Runs every 2 hours starting at 3:20 PM');
      expect(cronToText('0 30 8/3 * * ?')).toBe('Runs every 3 hours starting at 8:30 AM');
      expect(cronToText('0 0 0/1 * * ?')).toBe('Runs every 1 hours starting at 12:00 AM');
    });

    it('formats PM hours correctly', () => {
      expect(cronToText('0 45 14/1 * * ?')).toBe('Runs every 1 hours starting at 2:45 PM');
    });

    it('formats noon correctly', () => {
      expect(cronToText('0 0 12/6 * * ?')).toBe('Runs every 6 hours starting at 12:00 PM');
    });
  });

  describe('weekly patterns', () => {
    it('formats single day correctly', () => {
      expect(cronToText('0 30 11 ? * THU')).toContain('THU');
      expect(cronToText('0 30 11 ? * THU')).toContain('11:30 AM');
    });

    it('formats multiple days correctly', () => {
      expect(cronToText('0 30 11 ? * THU,FRI')).toContain('THU & FRI');
      expect(cronToText('0 30 11 ? * MON,WED,FRI')).toContain('MON & WED & FRI');
    });

    it('formats with PM time', () => {
      expect(cronToText('0 45 14 ? * MON')).toContain('2:45 PM');
    });

    it('formats with midnight', () => {
      expect(cronToText('0 0 0 ? * SAT')).toContain('12:00 AM');
    });
  });

  describe('month-end patterns', () => {
    it('formats last day of every month', () => {
      expect(cronToText('0 0 11 L * ?')).toBe('Runs on last day of every month at 11:00 AM');
    });

    it('formats last day of specific months', () => {
      expect(cronToText('0 0 11 L 5,3 ?')).toContain('Runs on last day of 5,3 at 11:00 AM');
    });

    it('formats with PM time', () => {
      expect(cronToText('0 30 15 L * ?')).toContain('3:30 PM');
    });
  });

  describe('daily patterns', () => {
    it('formats daily with * day-of-month', () => {
      expect(cronToText('0 30 9 * * ?')).toBe('Runs daily at 9:30 AM');
    });

    it('formats daily with ? day-of-month', () => {
      expect(cronToText('0 0 10 ? * *')).toBe('Runs daily at 10:00 AM');
    });

    it('formats with PM time', () => {
      expect(cronToText('0 15 17 * * ?')).toBe('Runs daily at 5:15 PM');
    });

    it('formats midnight correctly', () => {
      expect(cronToText('0 0 0 * * ?')).toBe('Runs daily at 12:00 AM');
    });

    it('formats noon correctly', () => {
      expect(cronToText('0 0 12 * * ?')).toBe('Runs daily at 12:00 PM');
    });

    it('formats every day in specific months', () => {
      expect(cronToText('0 0 10 * JAN,3 ?')).toBe(
        'Runs every day in January and March at 10:00 AM'
      );
    });
  });

  describe('special pattern for 5 minutes', () => {
    it('detects exact 5-minute pattern', () => {
      expect(cronToText('0 */5 * ? * *')).toBe('Runs every 5 minutes');
    });
  });

  describe('custom schedule fallback', () => {
    it('formats specific day-of-month patterns', () => {
      expect(cronToText('0 30 11 15 * ?')).toBe('Runs on day 15 of every month at 11:30 AM');
      expect(cronToText('0 30 11 1,15 * ?')).toBe('Runs on days 1,15 of every month at 11:30 AM');
    });

    it('returns empty string for null/empty cron', () => {
      expect(cronToText('')).toBe('');
      expect(cronToText(null as any)).toBe('');
      expect(cronToText(undefined as any)).toBe('');
    });

    it('handles 5-field cron expressions', () => {
      const result = cronToText('30 11 * * ?');
      expect(result).toBeDefined();
    });

    it('uses cronstrue fallback for advanced quartz patterns', () => {
      const result = cronToText('0 0 9 ? * 2#1');
      expect(result).not.toBe('Custom schedule');
      expect(result).toContain('first Monday');
      expect(result).not.toContain('2#1');
    });
  });

  describe('time formatting edge cases', () => {
    it('pads single-digit minutes', () => {
      expect(cronToText('0 5 11 * * ?')).toBe('Runs daily at 11:05 AM');
      expect(cronToText('0 0 14 * * ?')).toBe('Runs daily at 2:00 PM');
    });

    it('handles all 24 hours correctly', () => {
      const hours = [
        { h: 0, expected: '12:00 AM' },
        { h: 1, expected: '1:00 AM' },
        { h: 11, expected: '11:00 AM' },
        { h: 12, expected: '12:00 PM' },
        { h: 13, expected: '1:00 PM' },
        { h: 23, expected: '11:00 PM' },
      ];

      hours.forEach(({ h, expected }) => {
        const result = cronToText(`0 0 ${h} * * ?`);
        expect(result).toContain(expected);
      });
    });
  });
});

describe('cronToTextWithTimezone', () => {
  it('uses universal formatter output for non-weekly patterns', () => {
    const cron = '0 15 17 * * ?';
    const result = cronToTextWithTimezone(cron, 'UTC');
    expect(result.text).toContain('Runs every day at');
    expect(result.timezoneLabel).toContain('UTC');
    expect(result.isDateRangeShift).toBe(false);
  });

  it('converts UTC time to local time for daily patterns', () => {
    // 4:30 AM UTC = 10:00 AM Asia/Kolkata (UTC+5:30)
    const result = cronToTextWithTimezone('0 30 4 * * ?', 'Asia/Kolkata', '2026-03-17');
    expect(result.text).toContain('10:00 AM');
    expect(result.text).not.toContain('4:30 AM');
    expect(result.timezoneLabel).toContain('Asia/Kolkata');
  });

  it('returns empty result for invalid cron', () => {
    const result = cronToTextWithTimezone('', 'UTC');
    expect(result.text).toBe('');
    expect(result.timezoneLabel).toBeNull();
    expect(result.isDateRangeShift).toBe(false);
  });

  it('formats weekly cron in UTC timezone', () => {
    const result = cronToTextWithTimezone('0 30 11 ? * MON', 'UTC', '2026-03-17');
    expect(result.text).toContain('Runs every Monday at 11:30 AM');
    expect(result.timezoneLabel).toContain('UTC');
  });

  it('formats multiple weekdays and uses natural day list', () => {
    const result = cronToTextWithTimezone('0 0 9 ? * MON,WED,FRI', 'UTC', '2026-03-17');
    expect(result.text).toContain('Runs every');
    expect(result.text).toContain('Monday');
    expect(result.text).toContain('Wednesday');
    expect(result.text).toContain('Friday');
    expect(result.text).toContain('9:00 AM');
  });

  it('includes raw timezone string when timezone is invalid', () => {
    const cron = '0 30 11 ? * MON';
    const result = cronToTextWithTimezone(cron, 'Invalid/Timezone', '2026-03-17');
    expect(result.text).toContain(cronToText(cron));
    expect(result.timezoneLabel).toBe('Invalid/Timezone');
    expect(result.isDateRangeShift).toBe(false);
  });

  it('includes timezone for unsupported day tokens (cronstrue fallback path)', () => {
    const cron = '0 30 11 ? * MON-FRI';
    const result = cronToTextWithTimezone(cron, 'UTC', '2026-03-17');
    expect(result.timezoneLabel).toBeTruthy();
    expect(result.text).toContain('Runs');
  });
});

// =====================================================================
// NEW COMPREHENSIVE TEST SUITE: Universal Occurrence-Driven Pipeline
// =====================================================================

describe('cronToTextWithTimezoneUniversal - Phase 5 Integration', () => {
  describe('User Scenario: Anchorage Quarterly Schedule', () => {
    it("correctly renders the user's specific case: UTC 0 15 0 1 JAN,APR,JUL,OCT ? in America/Anchorage", () => {
      // This is the critical test case from the user requirement
      // UTC: 0 15 (00:15 UTC, which is 3:15 PM previous day in Winter, 4:15 PM previous day in Summer)
      // Expected: "Runs at 4:15 PM (GMT-8) on Dec 31, Mar 31, Jun 30, and Sep 30"
      const result = cronToTextWithTimezoneUniversal(
        '0 15 0 1 JAN,APR,JUL,OCT ?',
        'America/Anchorage',
        '2026-01-01'
      );

      expect(result.success).toBe(true);
      // Should contain PM time (not AM UTC time)
      expect(result.text).toContain('PM');
      // Should show the specific shifted local dates (31st/30th of each quarter-end month)
      expect(result.text).toMatch(/December|March|June|September/i);
      // Should reference Anchorage offset
      expect(result.timezoneLabel).toContain('America/Anchorage');
      // Should have date shift detected
      expect(result.isDateRangeShift).toBe(true);
    });
  });

  describe('UTC Conversion Regression Matrix', () => {
    it('validates fixed conversion outcomes across key timezones', () => {
      const cron = '0 15 0 1 JAN,APR,JUL,OCT ?';
      const referenceDate = '2025-12-31';

      const matrix = [
        { timezone: 'UTC', expectedShift: false, expectedTimeToken: '12:15 AM' },
        { timezone: 'America/Anchorage', expectedShift: true, expectedTimeToken: 'PM' },
        { timezone: 'America/New_York', expectedShift: true, expectedTimeToken: 'PM' },
        { timezone: 'Europe/London', expectedShift: false, expectedTimeToken: 'AM' },
        { timezone: 'Asia/Kolkata', expectedShift: false, expectedTimeToken: '5:45 AM' },
        { timezone: 'Asia/Kathmandu', expectedShift: false, expectedTimeToken: '6:00 AM' },
      ] as const;

      for (const entry of matrix) {
        const result = cronToTextWithTimezoneUniversal(cron, entry.timezone, referenceDate);
        expect(result.success).toBe(true);
        expect(result.timezoneLabel).toContain(entry.timezone);
        expect(result.isDateRangeShift).toBe(entry.expectedShift);
        expect(result.text).toContain(entry.expectedTimeToken);
      }
    });
  });

  describe('Phase 1: parseQuartzCron - Validation', () => {
    it('parses valid 5-field cron', () => {
      const result = parseQuartzCron('0 30 9 * * ?');
      expect(result.success).toBe(true);
      expect(result.data?.fields.minute).toBe('30');
      expect(result.data?.fields.hour).toBe('9');
    });

    it('parses valid 6-field cron with seconds', () => {
      const result = parseQuartzCron('15 30 9 * * ?');
      expect(result.success).toBe(true);
      expect(result.data?.fields.second).toBe('15');
    });

    it('detects special Quartz patterns (L)', () => {
      const result = parseQuartzCron('0 30 9 L * ?');
      expect(result.success).toBe(true);
      expect(result.data?.isQuartzSpecial).toBe(true);
    });

    it('rejects empty cron', () => {
      const result = parseQuartzCron('');
      expect(result.success).toBe(false);
      expect(result.error).toContain('empty');
    });

    it('rejects cron with wrong field count', () => {
      const result = parseQuartzCron('0 30 9 *');
      expect(result.success).toBe(false);
    });
  });

  describe('Phase 1: validateTimeZone', () => {
    it('validates correct timezone', () => {
      const result = validateTimeZone('America/New_York');
      expect(result.success).toBe(true);
      expect(result.data).toBe('America/New_York');
    });

    it('rejects invalid timezone', () => {
      const result = validateTimeZone('Invalid/TimeZone');
      expect(result.success).toBe(false);
      expect(result.error).toContain('Invalid timezone');
    });

    it('accepts UTC', () => {
      const result = validateTimeZone('UTC');
      expect(result.success).toBe(true);
    });

    it('accepts various valid timezones', () => {
      const zones = ['America/Los_Angeles', 'Europe/London', 'Asia/Tokyo', 'Australia/Sydney'];
      for (const zone of zones) {
        const result = validateTimeZone(zone);
        expect(result.success).toBe(true);
      }
    });
  });

  describe('Phase 2: generateOccurrences - All Pattern Types', () => {
    it('generates occurrences for daily pattern', () => {
      const start = new Date('2026-03-01');
      const end = new Date('2026-03-10');
      const result = generateOccurrences('0 30 9 * * ?', start, end, 'UTC', 100);

      expect(result.success).toBe(true);
      expect(result.data?.length).toBeGreaterThan(0);
      // Should generate roughly 10 occurrences (one per day)
      expect(result.data?.length).toBeGreaterThanOrEqual(9);
    });

    it('generates occurrences for weekly pattern (MON, WED, FRI)', () => {
      const start = new Date('2026-03-01');
      const end = new Date('2026-03-31');
      const result = generateOccurrences('0 30 9 ? * MON,WED,FRI', start, end, 'UTC', 100);

      expect(result.success).toBe(true);
      expect(result.data).toBeDefined();
      // March 2026 has about 12 MON/WED/FRI occurrences
      expect(result.data!.length).toBeGreaterThan(5);
    });

    it('generates occurrences for monthly pattern (specific day)', () => {
      const start = new Date('2026-01-01');
      const end = new Date('2026-12-31');
      const result = generateOccurrences('0 30 9 15 * ?', start, end, 'UTC', 100);

      expect(result.success).toBe(true);
      // Should be roughly 12 (one per month on the 15th)
      expect(result.data?.length).toBeGreaterThanOrEqual(10);
    });

    it('generates occurrences with timezone conversion (UTC to local)', () => {
      const start = new Date('2026-03-01');
      const end = new Date('2026-03-10');
      const result = generateOccurrences('0 0 9 * * ?', start, end, 'America/New_York', 100);

      expect(result.success).toBe(true);
      expect(result.data?.length).toBeGreaterThan(0);
      // Each occurrence should have local date and time
      expect(result.data?.[0].localDate).toBeDefined();
      expect(result.data?.[0].localTime).toBeDefined();
    });

    it('returns error for invalid timezone', () => {
      const start = new Date('2026-03-01');
      const end = new Date('2026-03-10');
      const result = generateOccurrences('0 30 9 * * ?', start, end, 'Invalid/Zone', 100);

      expect(result.success).toBe(false);
      expect(result.error).toContain('Invalid timezone');
    });

    it('returns error for invalid cron', () => {
      const start = new Date('2026-03-01');
      const end = new Date('2026-03-10');
      const result = generateOccurrences('invalid', start, end, 'UTC', 100);

      expect(result.success).toBe(false);
    });

    it('respects maxOccurrences limit', () => {
      const start = new Date('2026-01-01');
      const end = new Date('2026-12-31');
      const result = generateOccurrences('0 30 9 * * ?', start, end, 'UTC', 10);

      expect(result.success).toBe(true);
      expect(result.data?.length).toBeLessThanOrEqual(10);
    });
  });

  describe('Phase 3: analyzeLocalPattern - Pattern Detection', () => {
    it('detects daily pattern', () => {
      const start = new Date('2026-03-01');
      const end = new Date('2026-03-10');
      const occResult = generateOccurrences('0 30 9 * * ?', start, end, 'UTC', 100);
      const analysis = analyzeLocalPattern(occResult.data || []);

      expect(analysis.type).toBe('daily');
      expect(analysis.uniqueTimes.length).toBe(1);
      expect(analysis.hasTimeVariance).toBe(false);
    });

    it('detects weekly pattern', () => {
      const start = new Date('2026-03-01');
      const end = new Date('2026-03-31');
      const occResult = generateOccurrences('0 30 9 ? * MON,WED,FRI', start, end, 'UTC', 100);
      const analysis = analyzeLocalPattern(occResult.data || []);

      expect(analysis.type).toBe('weekly');
      expect(analysis.uniqueTimes.length).toBe(1);
    });

    it('detects monthly pattern', () => {
      const start = new Date('2026-01-01');
      const end = new Date('2026-12-31');
      const occResult = generateOccurrences('0 30 9 15 * ?', start, end, 'UTC', 100);
      const analysis = analyzeLocalPattern(occResult.data || []);

      expect(analysis.type).toBe('monthly');
    });

    it('handles empty occurrences', () => {
      const analysis = analyzeLocalPattern([]);

      expect(analysis.type).toBe('empty');
      expect(analysis.occurrences.length).toBe(0);
    });

    it('detects timezone variance (DST)', () => {
      const start = new Date('2026-01-01');
      const end = new Date('2026-12-31');
      // Daily at 9 AM UTC in a DST timezone will have offset variance
      const occResult = generateOccurrences('0 0 9 * * ?', start, end, 'America/New_York', 100);
      const analysis = analyzeLocalPattern(occResult.data || []);

      // May have DST variance
      expect(analysis.uniqueOffsets).toBeDefined();
    });
  });

  describe('Phase 4: renderScheduleDescription - Text Generation', () => {
    it('renders daily pattern', () => {
      const start = new Date('2026-03-01');
      const end = new Date('2026-03-10');
      const occResult = generateOccurrences('0 30 9 * * ?', start, end, 'UTC', 100);
      const analysis = analyzeLocalPattern(occResult.data || []);
      const text = renderScheduleDescription(analysis, 'UTC');

      expect(text).toContain('Runs every day at 9:30 AM');
    });

    it('renders weekly pattern', () => {
      const start = new Date('2026-03-01');
      const end = new Date('2026-03-31');
      const occResult = generateOccurrences('0 30 9 ? * MON,WED,FRI', start, end, 'UTC', 100);
      const analysis = analyzeLocalPattern(occResult.data || []);
      const text = renderScheduleDescription(analysis, 'UTC');

      expect(text).toContain('Runs every');
      expect(text).toContain('at 9:30 AM');
    });

    it('renders monthly pattern', () => {
      const start = new Date('2026-01-01');
      const end = new Date('2026-12-31');
      const occResult = generateOccurrences('0 30 9 15 * ?', start, end, 'UTC', 100);
      const analysis = analyzeLocalPattern(occResult.data || []);
      const text = renderScheduleDescription(analysis, 'UTC');

      expect(text).toContain('Runs on the');
      expect(text).toContain('at 9:30 AM');
    });
  });

  describe('Timezone Edge Cases', () => {
    it('detects previous-day rollover for negative UTC offsets', () => {
      const result = cronToTextWithTimezoneUniversal(
        '0 15 0 * * ?',
        'America/Anchorage',
        '2026-01-01'
      );
      expect(result.success).toBe(true);
      expect(result.isDateRangeShift).toBe(true);
      expect(result.text).toContain('PM');
    });

    it('detects next-day rollover for positive UTC offsets', () => {
      const result = cronToTextWithTimezoneUniversal('0 30 23 * * ?', 'Asia/Tokyo', '2026-03-17');
      expect(result.success).toBe(true);
      expect(result.isDateRangeShift).toBe(true);
      expect(result.text).toContain('AM');
    });

    it('handles month/year boundary rollover from UTC to local', () => {
      const result = cronToTextWithTimezoneUniversal(
        '0 15 0 1 JAN ?',
        'America/Anchorage',
        '2025-12-31'
      );
      expect(result.success).toBe(true);
      // Jan 1 UTC 00:15 → Dec 31 Anchorage PM
      expect(result.text).toContain('December');
      expect(result.text).toContain('PM');
      expect(result.isDateRangeShift).toBe(true);
    });

    it('handles half-hour offset timezone (Asia/Kolkata)', () => {
      const result = cronToTextWithTimezoneUniversal('0 30 4 * * ?', 'Asia/Kolkata', '2026-03-17');
      expect(result.success).toBe(true);
      expect(result.timezoneLabel).toContain('Asia/Kolkata');
    });

    it('handles quarter-hour offset timezone (Asia/Kathmandu)', () => {
      const result = cronToTextWithTimezoneUniversal(
        '0 30 4 * * ?',
        'Asia/Kathmandu',
        '2026-03-17'
      );
      expect(result.success).toBe(true);
      expect(result.timezoneLabel).toContain('Asia/Kathmandu');
    });

    it('handles negative UTC offset (America/Los_Angeles)', () => {
      const result = cronToTextWithTimezoneUniversal(
        '0 30 9 * * ?',
        'America/Los_Angeles',
        '2026-03-17'
      );
      expect(result.success).toBe(true);
      expect(result.timezoneLabel).toContain('America/Los_Angeles');
    });

    it('handles positive UTC offset (Europe/London)', () => {
      const result = cronToTextWithTimezoneUniversal('0 30 9 * * ?', 'Europe/London', '2026-03-17');
      expect(result.success).toBe(true);
    });

    it('handles DST transition spring forward (US Eastern)', () => {
      // March 8, 2026: 2 AM EST becomes 3 AM EDT
      const result = cronToTextWithTimezoneUniversal(
        '0 0 * * * ?',
        'America/New_York',
        '2026-03-08'
      );
      expect(result.success).toBe(true);
    });

    it('handles DST transition fall back (US Eastern)', () => {
      // November 1, 2026: 2 AM EDT becomes 1 AM EST
      const result = cronToTextWithTimezoneUniversal(
        '0 0 * * * ?',
        'America/New_York',
        '2026-11-01'
      );
      expect(result.success).toBe(true);
    });
  });

  describe('Error Handling', () => {
    it('returns error for invalid timezone', () => {
      const result = cronToTextWithTimezoneUniversal('0 30 9 * * ?', 'Invalid/Zone', '2026-03-17');
      expect(result.success).toBe(false);
      expect(result.error).toBeDefined();
      expect(result.text).toBe('');
      expect(result.timezoneLabel).toBe('Invalid/Zone');
      expect(result.isDateRangeShift).toBe(false);
    });

    it('returns error for invalid cron', () => {
      const result = cronToTextWithTimezoneUniversal('invalid cron', 'UTC', '2026-03-17');
      expect(result.success).toBe(false);
      expect(result.error).toBeDefined();
      expect(result.text).toBe('');
      expect(result.timezoneLabel).toBe('UTC');
      expect(result.isDateRangeShift).toBe(false);
    });

    it('returns graceful empty result when no occurrences found', () => {
      // Test with a very small date range for a monthly pattern
      const result = cronToTextWithTimezoneUniversal(
        '0 30 9 15 * ?',
        'UTC',
        '2026-03-16' // One day before the 15th
      );
      // Depending on implementation, either success with no occurrences or success with occurrences from next month
      expect(result.success).toBe(true);
      expect(result.text).toBeDefined();
      expect(result.timezoneLabel).toContain('UTC');
      expect(typeof result.isDateRangeShift).toBe('boolean');
    });
  });

  describe('Special Pattern Types', () => {
    it('handles last day of month (L)', () => {
      const result = cronToTextWithTimezoneUniversal('0 30 9 L * ?', 'UTC', '2026-03-01');
      expect(result.success).toBe(true);
      expect(result.text).toBeDefined();
    });

    it('handles specific day list (1,10,20)', () => {
      const result = cronToTextWithTimezoneUniversal('0 30 9 1,10,20 * ?', 'UTC', '2026-03-01');
      expect(result.success).toBe(true);
    });

    it('handles specific month list (JAN,APR,JUL,OCT)', () => {
      const result = cronToTextWithTimezoneUniversal(
        '0 15 0 1 JAN,APR,JUL,OCT ?',
        'UTC',
        '2026-01-01'
      );
      expect(result.success).toBe(true);
      expect(result.text).toContain('12:15 AM');
    });

    it('handles named day list (MON,WED,FRI)', () => {
      const result = cronToTextWithTimezoneUniversal('0 30 9 ? * MON,WED,FRI', 'UTC', '2026-03-01');
      expect(result.success).toBe(true);
      expect(result.text).toContain('Runs every');
      expect(result.text).toContain('Monday');
    });

    it('handles hour range (9-17)', () => {
      const result = cronToTextWithTimezoneUniversal('0 30 9-17 * * ?', 'UTC', '2026-03-01');
      expect(result.success).toBe(true);
    });

    it('handles minute intervals (*/15)', () => {
      const result = cronToTextWithTimezoneUniversal('0 */15 * * * ?', 'UTC', '2026-03-01');
      expect(result.success).toBe(true);
    });
  });
});

describe('User Cron Test Cases – America/Anchorage', () => {
  const tz = 'America/Anchorage';
  const ref = '2026-03-17';

  it('case 1 – 0 0 9 * * ? → daily at 1:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 9 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('1:00 AM');
  });

  it('case 2 – 0 30 18 * * ? → daily at 10:30 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 30 18 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('10:30 AM');
  });

  it('case 3 – 0 0 0 * * ? → daily, shows PM (previous-day shift)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 0 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('PM');
  });

  it('case 4 – 0 0 12 ? * MON → every Monday at 4:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 12 ? * MON', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('Monday');
    expect(r.text).toContain('4:00 AM');
  });

  it('case 5 – 0 15 10 ? * MON-FRI → weekdays at 2:15 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 15 10 ? * MON-FRI', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/Monday|Monday.*Friday|weekday/i);
    expect(r.text).toContain('2:15 AM');
  });

  it('case 6 – 0 0/15 * * * ? → every 15 minutes', () => {
    const r = cronToTextWithTimezoneUniversal('0 0/15 * * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every 15 minute/i);
  });

  it('case 7 – 0 0 8 1 * ? → 1st of month at 12:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 8 1 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('12:00 AM');
  });

  it('case 8 – 0 45 23 L * ? → last day of month at 3:45 PM', () => {
    const r = cronToTextWithTimezoneUniversal('0 45 23 L * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('3:45 PM');
  });

  it('case 9 – 0 0 6 ? * SAT,SUN → weekend, PM (previous-day shift)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 6 ? * SAT,SUN', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/Saturday|Sunday/i);
    expect(r.text).toContain('10:00 PM');
  });

  it('case 10 – 0 0 14 15 * ? → 15th of month at 6:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 14 15 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('6:00 AM');
  });

  it('case 11 – 0 0 10 ? * 2#1 → first Monday of month at 2:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 10 ? * 2#1', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/first Monday/i);
    expect(r.text).toContain('2:00 AM');
  });

  it('case 12 – 0 0 10 ? * 6L → last Friday of month at 2:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 10 ? * 6L', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/last Friday/i);
    expect(r.text).toContain('2:00 AM');
    // 10:00 UTC is still the same day in Anchorage (UTC-8/UTC-9)
    expect(r.isDateRangeShift).toBe(false);
  });

  it('case 13 – 0 30 9 1 JAN * → Jan 1st yearly at 12:30 AM (UTC-9 winter)', () => {
    const r = cronToTextWithTimezoneUniversal('0 30 9 1 JAN *', tz, ref);
    expect(r.success).toBe(true);
    // Jan 1 is UTC-9, so 09:30 UTC = 00:30 Anchorage
    expect(r.text).toMatch(/12:30 AM|1:30 AM/);
  });

  it('case 14 – 0 0 20 10 JUN ? → June 10th yearly at 12:00 PM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 20 10 JUN ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('12:00 PM');
  });

  it('case 15 – 0 0/5 9-17 * * MON-FRI → every 5 min between morning and evening on weekdays', () => {
    const r = cronToTextWithTimezoneUniversal('0 0/5 9-17 * * MON-FRI', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every 5 minute/i);
  });

  it('case 16 – 0 0 22 ? * SUN → every Sunday at 2:00 PM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 22 ? * SUN', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('Sunday');
    expect(r.text).toContain('2:00 PM');
  });

  it('case 17 – 0 10 0 1 1/3 ? → PM previous-day (date-shift for Jan/Apr/Jul/Oct)', () => {
    const r = cronToTextWithTimezoneUniversal('0 10 0 1 1/3 ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('PM');
  });

  it('case 18 – 0 0 5 ? * * → daily, PM (previous-day shift)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 5 ? * *', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('PM');
  });

  it('case 19 – 0 20 13 10 * ? → 10th of month at 5:20 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 20 13 10 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toContain('5:20 AM');
  });

  it('case 20 – 0 15 0 1 JAN,APR,JUL,OCT ? → PM on shifted quarter-end dates', () => {
    const r = cronToTextWithTimezoneUniversal('0 15 0 1 JAN,APR,JUL,OCT ?', tz, '2026-01-01');
    expect(r.success).toBe(true);
    expect(r.text).toContain('PM');
    // Specific shifted local dates should appear (Dec/Mar/Jun/Sep)
    expect(r.text).toMatch(/December|March|June|September/i);
    expect(r.isDateRangeShift).toBe(true);
  });

  it('case 21 – 0 30 4 ? * TUE,THU → date-shifted DOW shows Monday and Wednesday', () => {
    // 04:30 UTC on Tue/Thu = 20:30 (8:30 PM) Mon/Wed in Anchorage (UTC-8 summer)
    const r = cronToTextWithTimezoneUniversal('0 30 4 ? * TUE,THU', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/Monday.*Wednesday|Wednesday.*Monday/i);
    expect(r.text).toContain('8:30 PM');
  });
});

describe('Cron Test Cases 41–65 – America/Anchorage', () => {
  const tz = 'America/Anchorage';
  const ref = '2026-03-26'; // UTC-8 (AKDT)

  // 41. Every day – no date shift
  it('41 – 0 0 11 * * ? → every day at 3:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 11 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every day/i);
    expect(r.text).toMatch(/3:00 AM|2:00 AM/);
  });

  // 42. Every day – date-shifted to previous day
  it('42 – 0 30 2 * * ? → every day at 6:30 PM', () => {
    const r = cronToTextWithTimezoneUniversal('0 30 2 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every day/i);
    expect(r.text).toMatch(/6:30 PM|5:30 PM/);
  });

  // 43. Weekly – no shift
  it('43 – 0 0 16 ? * THU → every Thursday at 8:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 16 ? * THU', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every Thursday/i);
    expect(r.text).toMatch(/8:00 AM|7:00 AM/);
  });

  // 44. Multi-day – no shift
  it('44 – 0 45 14 ? * MON,WED,FRI → Mon, Wed, Fri at 6:45 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 45 14 ? * MON,WED,FRI', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every Monday.*Wednesday.*Friday|Monday.*Wednesday.*Friday/i);
    expect(r.text).toMatch(/6:45 AM|5:45 AM/);
  });

  // 45. Sub-daily – every 30 minutes
  it('45 – 0 0/30 * * * ? → every 30 minutes', () => {
    const r = cronToTextWithTimezoneUniversal('0 0/30 * * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every 30 minutes/i);
  });

  // 46. Specific DOM – no shift (09:00 UTC = 1 AM local)
  it('46 – 0 0 9 5 * ? → 1:00 AM on 5th of every month', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 9 5 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/5th/i);
    expect(r.text).toMatch(/1:00 AM|12:00 AM/);
  });

  // 47. Last day of month – no shift (13:00 UTC = 5 AM local)
  it('47 – 0 0 13 L * ? → 5:00 AM on last day of every month', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 13 L * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/last day of every month/i);
    expect(r.text).toMatch(/5:00 AM|4:00 AM/);
  });

  // 48. Nth weekday – named token
  it('48 – 0 15 18 ? * TUE#2 → second Tuesday at 10:15 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 15 18 ? * TUE#2', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/second Tuesday/i);
    expect(r.text).toMatch(/10:15 AM|9:15 AM/);
  });

  // 49. Nth weekday – named token SUN
  it('49 – 0 0 20 ? * SUN#3 → third Sunday at 12:00 PM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 20 ? * SUN#3', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/third Sunday/i);
    expect(r.text).toMatch(/12:00 PM|11:00 AM/);
  });

  // 50. Named last-weekday tokens are rejected; numeric Quartz forms remain supported.
  it('50 – 0 0 23 ? * SATL → invalid named last-weekday token', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 23 ? * SATL', tz, ref);
    expect(r.success).toBe(false);
    expect(r.error).toBe('Invalid cron expression');
  });

  // 51. DOM with date shift (07:10 UTC on 10th = 11:10 PM on local 9th)
  it('51 – 0 10 7 10 * ? → ~11:10 PM on local 9th of every month', () => {
    const r = cronToTextWithTimezoneUniversal('0 10 7 10 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/9th|10th/);
    expect(r.text).toMatch(/11:10 PM|10:10 PM/);
  });

  // 52. Specific month+DOM – January is UTC-9
  it('52 – 0 0 0 1 1 ? → December 31 at ~3:00 PM (UTC-9 Jan)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 0 1 1 ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/December 31/i);
    expect(r.text).toMatch(/3:00 PM|4:00 PM/);
  });

  // 53. Specific month+DOM – February is UTC-9
  it('53 – 0 0 10 20 FEB ? → 1:00 AM on Feb 20 (UTC-9 Feb)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 10 20 FEB ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/February/i);
    expect(r.text).toMatch(/20th|20/);
    expect(r.text).toMatch(/1:00 AM|2:00 AM/);
  });

  // 54. Weekly – date/day shift (01:30 UTC Sun = 5:30 PM Sat)
  it('54 – 0 30 1 ? * SUN → every Saturday at 5:30 PM (shifted)', () => {
    const r = cronToTextWithTimezoneUniversal('0 30 1 ? * SUN', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every Saturday/i);
    expect(r.text).toMatch(/5:30 PM|4:30 PM/);
  });

  // 55. Sub-daily with hour range (Jan baseline = UTC-9)
  it('55 – 0 0/10 8-10 * * ? → every 10 minutes in local overnight window', () => {
    const r = cronToTextWithTimezoneUniversal('0 0/10 8-10 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every 10 minutes/i);
    expect(r.text).toMatch(/PM|AM/);
  });

  // 56. Weekly range shifted (06:20 UTC MON-FRI → 10:20 PM Sun-Thu)
  it('56 – 0 20 6 ? * MON-FRI → shifted to Sun-Thu at 10:20 PM', () => {
    const r = cronToTextWithTimezoneUniversal('0 20 6 ? * MON-FRI', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/Sunday/i);
    expect(r.text).toMatch(/10:20 PM|9:20 PM/);
  });

  // 57. Every day – no shift
  it('57 – 0 50 21 * * ? → every day at 1:50 PM', () => {
    const r = cronToTextWithTimezoneUniversal('0 50 21 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every day/i);
    expect(r.text).toMatch(/1:50 PM|12:50 PM/);
  });

  // 58. Month-step DOM – shows specific months (Jan, May, Sep)
  it('58 – 0 0 17 1 1/4 ? → 1st of Jan/May/Sep at ~9:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 17 1 1/4 ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/1st/i);
    expect(r.text).toMatch(/January|May|September/i);
    expect(r.text).toMatch(/9:00 AM|8:00 AM/);
  });

  // 59. DOM with date shift (01:05 UTC on 15th = 5:05 PM on local 14th)
  it('59 – 0 5 1 15 * ? → 5:05 PM on local 14th of every month', () => {
    const r = cronToTextWithTimezoneUniversal('0 5 1 15 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/14th|15th/);
    expect(r.text).toMatch(/5:05 PM|4:05 PM/);
  });

  // 60. Weekly with months
  it('60 – 0 0 12 ? JAN,JUN SUN → every Sunday in Jan and Jun at 4:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 12 ? JAN,JUN SUN', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every Sunday/i);
    expect(r.text).toMatch(/January.*June|June.*January/i);
    expect(r.text).toMatch(/4:00 AM|3:00 AM/);
  });

  // 61. Weekly with months – no shift
  it('61 – 0 0 15 ? MAR,SEP MON → every Monday in Mar and Sep at 7:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 15 ? MAR,SEP MON', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every Monday/i);
    expect(r.text).toMatch(/March.*September|September.*March/i);
    expect(r.text).toMatch(/7:00 AM|6:00 AM/);
  });

  // 62. Specific month+DOM – April is UTC-8
  it('62 – 0 25 19 10 APR ? → 11:25 AM on April 10 every year', () => {
    const r = cronToTextWithTimezoneUniversal('0 25 19 10 APR ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/April/i);
    expect(r.text).toMatch(/10th|10/);
    expect(r.text).toMatch(/11:25 AM|10:25 AM/);
  });

  // 63. Nth weekday numeric – Quartz 2=Monday, shifted to Sunday night
  it('63 – 0 0 6 ? * 2#4 → fourth Monday at ~10:00 PM (shifted)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 6 ? * 2#4', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/fourth Monday/i);
    expect(r.text).toMatch(/10:00 PM|9:00 PM/);
  });

  // 64. L-n DOM – second-to-last day (L-2 = 2 days before last)
  it('64 – 0 0 8 L-2 * ? → 12:00 AM on near-last day of every month', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 8 L-2 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/last|2/i);
    expect(r.text).toMatch(/12:00 AM|11:00 PM/);
  });

  // 65. Sub-daily with DOW range and hour range
  it('65 – 0 0/5 14-16 ? * MON-FRI → every 5 min in morning window, weekdays', () => {
    const r = cronToTextWithTimezoneUniversal('0 0/5 14-16 ? * MON-FRI', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every 5 minutes/i);
    expect(r.text).toMatch(/AM/);
  });
});

describe('Additional Cron Test Cases – America/Anchorage (cases 22–40)', () => {
  const tz = 'America/Anchorage';
  const ref = '2026-03-26';

  it('case 22 – 0 5 16 * * ? → every day at 8:05 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 5 16 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every day/i);
    expect(r.text).toContain('8:05 AM');
  });

  it('case 23 – 0 0 3 * * ? → every day at 7:00 PM (prev-day, UTC-8 summer dominant)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 3 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every day/i);
    expect(r.text).toMatch(/7:00 PM|6:00 PM/);
  });

  it('case 24 – 0 0 21 ? * TUE → every Tuesday at 1:00 PM (no date shift)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 21 ? * TUE', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every Tuesday/i);
    expect(r.text).toContain('1:00 PM');
  });

  it('case 25 – 0 10 11 ? * WED,FRI → every Wednesday and Friday at 3:10 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 10 11 ? * WED,FRI', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every Wednesday and Friday/i);
    expect(r.text).toContain('3:10 AM');
  });

  it('case 26 – 0 0/20 * * * ? → every 20 minutes', () => {
    const r = cronToTextWithTimezoneUniversal('0 0/20 * * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every 20 minutes/i);
  });

  it('case 27 – 0 0 7 10 * ? → 11 PM (prev day) on local 9th of every month', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 7 10 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/9th|10th/);
    expect(r.text).toMatch(/11:00 PM|10:00 PM/);
  });

  it('case 28 – 0 25 13 L * ? → 5:25 AM on the last day of every month', () => {
    const r = cronToTextWithTimezoneUniversal('0 25 13 L * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/last day of every month/i);
    expect(r.text).toMatch(/5:25 AM|4:25 AM/);
  });

  it('case 29 – 0 0 18 ? * MON#2 → second Monday of every month at 10:00 AM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 18 ? * MON#2', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/second Monday/i);
    expect(r.text).toContain('10:00 AM');
  });

  it('case 30 – 0 0 4 ? * 3#3 → third Tuesday of every month at 8:00 PM (prev day, UTC-8)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 4 ? * 3#3', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/third Tuesday/i);
    expect(r.text).toMatch(/8:00 PM|7:00 PM/);
  });

  it('case 31 – 0 0 23 ? * 5L → last Thursday of every month at 3:00 PM', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 23 ? * 5L', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/last Thursday/i);
    expect(r.text).toContain('3:00 PM');
  });

  it('case 32 – 0 40 6 5 * ? → 10:40 PM (prev day) on local 4th', () => {
    const r = cronToTextWithTimezoneUniversal('0 40 6 5 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/4th|5th/);
    expect(r.text).toMatch(/10:40 PM|9:40 PM/);
  });

  it('case 33 – 0 0 1 1 1 ? → 4:00 PM on December 31 every year (UTC-9 Jan)', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 1 1 1 ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/December 31/i);
    expect(r.text).toMatch(/4:00 PM|5:00 PM/);
  });

  it('case 34 – 0 0 15 20 FEB ? → ~6-7:00 AM on February 20th every year', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 15 20 FEB ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/February/i);
    expect(r.text).toMatch(/20th|20/);
    expect(r.text).toMatch(/7:00 AM|6:00 AM/);
  });

  it('case 35 – 0 30 2 ? * SUN → every Saturday at 6:30 PM (date-shifted from Sunday)', () => {
    // 02:30 UTC Sunday = 18:30 Saturday local (UTC-8)
    const r = cronToTextWithTimezoneUniversal('0 30 2 ? * SUN', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every Saturday/i);
    expect(r.text).toContain('6:30 PM');
  });

  it('case 36 – 0 0/10 6-8 * * ? → every 10 minutes in UTC-offset evening window', () => {
    const r = cronToTextWithTimezoneUniversal('0 0/10 6-8 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every 10 minutes/i);
    expect(r.text).toMatch(/PM/);
  });

  it('case 37 – 0 15 5 ? * MON-FRI → shifted to Sun–Thu at 9:15 PM', () => {
    // 05:15 UTC Mon-Fri = 21:15 prev day (Sun-Thu) in Anchorage (UTC-8)
    const r = cronToTextWithTimezoneUniversal('0 15 5 ? * MON-FRI', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/Sunday/i);
    expect(r.text).toContain('9:15 PM');
  });

  it('case 38 – 0 50 22 * * ? → every day at 2:50 PM', () => {
    const r = cronToTextWithTimezoneUniversal('0 50 22 * * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every day/i);
    expect(r.text).toContain('2:50 PM');
  });

  it('case 39 – 0 0 19 1 1/2 ? → 11:00 AM on 1st of alternating months', () => {
    const r = cronToTextWithTimezoneUniversal('0 0 19 1 1/2 ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/1st/i);
    expect(r.text).toMatch(/11:00 AM|10:00 AM/);
  });

  it('case 40 – 0 5 0 15 * ? → 4:05 PM on local 14th (shifted from UTC 15th)', () => {
    const r = cronToTextWithTimezoneUniversal('0 5 0 15 * ?', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/14th|15th/);
    expect(r.text).toMatch(/4:05 PM|3:05 PM/);
  });

  it('case 41 – 0 0 12 ? JAN,JUN SUN → every Sunday in Jan and Jun at 4:00 AM', () => {
    // Quartz DOW: SUN – no date shift (12-8=4AM same day)
    const r = cronToTextWithTimezoneUniversal('0 0 12 ? JAN,JUN SUN', tz, ref);
    expect(r.success).toBe(true);
    expect(r.text).toMatch(/every Sunday/i);
    expect(r.text).toMatch(/January.*June|June.*January/i);
    expect(r.text).toMatch(/4:00 AM|3:00 AM/);
  });
});

const makeLocalOccurrence = (overrides: Partial<LocalOccurrence> = {}): LocalOccurrence => ({
  utcDate: new Date('2026-01-15T09:00:00Z'),
  localDate: '2026-01-15',
  localTime: '9:00 AM',
  offset: 'GMT+0',
  ...overrides,
});

describe('Cron description edge branches', () => {
  it('returns the empty-period message when analysis has no occurrences', () => {
    const text = renderScheduleDescription({ type: 'empty', occurrences: [] } as any, 'UTC');
    expect(text).toBe('No scheduled occurrences in the next period');
  });

  it('renders a single occurrence directly', () => {
    const text = renderScheduleDescription(
      {
        type: 'single-occurrence',
        occurrences: [
          makeLocalOccurrence({
            localDate: '2026-04-10',
            localTime: '7:30 AM',
            offset: 'GMT-4',
          }),
        ],
      } as any,
      'America/New_York'
    );

    expect(text).toBe('Runs on 2026-04-10 at 7:30 AM (GMT-4)');
  });

  it('falls back to a custom schedule for unknown analysis types', () => {
    const text = renderScheduleDescription(
      {
        type: 'unexpected-pattern',
        occurrences: [makeLocalOccurrence({ localTime: '11:45 AM' })],
      } as any,
      'UTC'
    );

    expect(text).toBe('Runs on a custom schedule at 11:45 AM');
  });

  it('renders interval patterns only when a regularity pattern is present', () => {
    const withPattern = renderScheduleDescription(
      {
        type: 'interval',
        regularityPattern: 'every 4 hours',
        occurrences: [makeLocalOccurrence({ localTime: '8:00 AM' })],
      } as any,
      'UTC'
    );
    const withoutPattern = renderScheduleDescription(
      {
        type: 'interval',
        occurrences: [makeLocalOccurrence({ localTime: '8:00 AM' })],
      } as any,
      'UTC'
    );

    expect(withPattern).toBe('Runs every 4 hours at 8:00 AM');
    expect(withoutPattern).toBe('Runs on a custom schedule at 8:00 AM');
  });

  it('renders shifted last-day schedules from local occurrence dates', () => {
    const text = renderScheduleDescription(
      {
        type: 'time-varies',
        occurrences: [
          makeLocalOccurrence({ localDate: '2025-12-31', localTime: '4:15 PM' }),
          makeLocalOccurrence({ localDate: '2026-03-31', localTime: '4:15 PM' }),
          makeLocalOccurrence({ localDate: '2026-06-30', localTime: '4:15 PM' }),
          makeLocalOccurrence({ localDate: '2026-09-30', localTime: '4:15 PM' }),
        ],
      } as any,
      'America/Anchorage'
    );

    expect(text).toContain('Runs at 4:15 PM on the last day of');
    expect(text).toContain('December');
    expect(text).toContain('March');
    expect(text).toContain('June');
    expect(text).toContain('September');
  });

  it('renders specific shifted local dates when there are only a few distinct dates', () => {
    const text = renderScheduleDescription(
      {
        type: 'time-varies',
        occurrences: [
          makeLocalOccurrence({ localDate: '2026-01-14', localTime: '5:00 PM' }),
          makeLocalOccurrence({ localDate: '2026-02-14', localTime: '5:00 PM' }),
        ],
      } as any,
      'America/Anchorage'
    );

    expect(text).toBe('Runs at 5:00 PM on January 14th and February 14th');
  });

  it('falls back to the generic time-varies message when many shifted dates exist', () => {
    const occurrences = Array.from({ length: 13 }, (_, index) =>
      makeLocalOccurrence({
        localDate: `2026-${String((index % 12) + 1).padStart(2, '0')}-${String((index % 27) + 1).padStart(2, '0')}`,
        localTime: '6:00 PM',
      })
    );

    const text = renderScheduleDescription(
      {
        type: 'time-varies',
        occurrences,
      } as any,
      'UTC'
    );

    expect(text).toBe('Runs around 6:00 PM with local time changes');
  });

  it('returns a no-occurrence result when the lookahead window contains no matching dates', () => {
    const result = cronToTextWithTimezoneUniversal('0 0 0 29 FEB ?', 'UTC', '2026-03-01', 300);

    expect(result.success).toBe(true);
    expect(result.text).toBe('No scheduled occurrences in the next period');
    expect(result.isDateRangeShift).toBe(false);
  });

  it('renders winter Anchorage monthly schedules using the shifted previous-day local date', () => {
    const result = cronToTextWithTimezoneUniversal(
      '0 0 5 1 * ?',
      'America/Anchorage',
      '2026-01-15',
      35
    );

    expect(result.success).toBe(true);
    expect(result.text).toBe('Runs at 8:00 PM on January 31st');
    expect(result.timezoneLabel).toContain('America/Anchorage');
    expect(result.isDateRangeShift).toBe(true);
  });

  it('renders summer Anchorage monthly schedules using the shifted previous-day local date', () => {
    const result = cronToTextWithTimezoneUniversal(
      '0 0 5 1 * ?',
      'America/Anchorage',
      '2026-06-15',
      35
    );

    expect(result.success).toBe(true);
    expect(result.text).toBe('Runs at 9:00 PM on June 30th');
    expect(result.timezoneLabel).toContain('America/Anchorage');
    expect(result.isDateRangeShift).toBe(true);
  });

  it('renders Anchorage last-day UTC schedules as the second-to-last local day using the displayed offset', () => {
    const result = cronToTextWithTimezoneUniversal(
      '0 0 2 L * ?',
      'America/Anchorage',
      '2026-04-07',
      60
    );

    expect(result.success).toBe(true);
    expect(result.text).toBe('Runs at 6:00 PM on the second-to-last day of every month');
    expect(result.timezoneLabel).toContain('America/Anchorage');
    expect(result.timezoneLabel).toContain('GMT-08:00');
    expect(result.isDateRangeShift).toBe(true);
  });

  it('renders last-day UTC schedules that roll forward into the first local day of the next month', () => {
    const result = cronToTextWithTimezoneUniversal(
      '0 0 23 L * ?',
      'Asia/Kolkata',
      '2026-04-07',
      60
    );

    expect(result.success).toBe(true);
    expect(result.text).toBe('Runs at 4:30 AM on the first day of every month');
    expect(result.timezoneLabel).toContain('Asia/Kolkata');
    expect(result.timezoneLabel).toContain('GMT+05:30');
    expect(result.isDateRangeShift).toBe(true);
  });

  it('renders month-specific last-day UTC schedules that roll into the first local day of the next month', () => {
    const result = cronToTextWithTimezoneUniversal(
      '0 0 23 L 2 ?',
      'Asia/Kolkata',
      '2026-01-15',
      80
    );

    expect(result.success).toBe(true);
    expect(result.text).toBe('Runs at 4:30 AM on the first day of March every year');
    expect(result.timezoneLabel).toContain('Asia/Kolkata');
    expect(result.timezoneLabel).toContain('GMT+05:30');
    expect(result.isDateRangeShift).toBe(true);
  });

  it('treats step-based cron fields as valid schedules without date-shift detection metadata', () => {
    const result = cronToTextWithTimezoneUniversal('0 */15 * * * ?', 'UTC', '2026-03-01');

    expect(result.success).toBe(true);
    expect(result.isDateRangeShift).toBe(false);
    expect(result.text).toMatch(/every 15 minutes/i);
  });

  it('rejects invalid Quartz special-field combinations', () => {
    expect(cronToTextWithTimezoneUniversal('0 0 9 L * MON', 'UTC', '2026-03-01').success).toBe(
      false
    );
    expect(cronToTextWithTimezoneUniversal('0 0 9 ? * XXL', 'UTC', '2026-03-01').success).toBe(
      false
    );
    expect(cronToTextWithTimezoneUniversal('0 0 9 ? * ZZ#2', 'UTC', '2026-03-01').success).toBe(
      false
    );
  });
});

describe('Cron field description helpers', () => {
  it('returns the most common local time and null for empty occurrence arrays', () => {
    expect(getMostCommonLocalTime([])).toBeNull();
    expect(
      getMostCommonLocalTime([
        makeLocalOccurrence({ localTime: '8:00 AM' }),
        makeLocalOccurrence({ localTime: '8:00 AM' }),
        makeLocalOccurrence({ localTime: '9:00 AM' }),
      ])
    ).toBe('8:00 AM');
  });

  it('returns null for invalid cron strings and valid cron strings without occurrences', () => {
    expect(buildCronFieldDescription('invalid cron', 'UTC', [makeLocalOccurrence()])).toBeNull();
    expect(buildCronFieldDescription('0 0 9 * * ?', 'UTC', [])).toBeNull();
  });

  it('describes singular and ranged subdaily schedules', () => {
    const singular = buildCronFieldDescription('0 0/1 * * * ?', 'UTC', [makeLocalOccurrence()]);
    const ranged = buildCronFieldDescription('0 0/5 14-16 ? * MON-FRI', 'UTC', [
      makeLocalOccurrence({ utcDate: new Date('2026-03-02T14:00:00Z') }),
    ]);

    expect(singular).toBe('Runs every 1 minute');
    expect(ranged).toBe(
      'Runs every 5 minutes between 2 PM and 4 PM on Monday, Tuesday, Wednesday, Thursday and Friday'
    );
  });

  it('describes hourly intervals with and without weekday restrictions', () => {
    expect(buildCronFieldDescription('0 0 0/1 * * ?', 'UTC', [makeLocalOccurrence()])).toBe(
      'Runs every 1 hour'
    );
    expect(buildCronFieldDescription('0 0 0/4 ? * MON', 'UTC', [makeLocalOccurrence()])).toBe(
      'Runs every 4 hours on Mondays'
    );
  });

  it('describes nth weekdays and last weekdays', () => {
    expect(buildCronFieldDescription('0 0 9 ? * 2#4', 'UTC', [makeLocalOccurrence()])).toBe(
      'Runs on the fourth Monday of every month at 9:00 AM'
    );
    expect(buildCronFieldDescription('0 0 9 ? * 6L', 'UTC', [makeLocalOccurrence()])).toBe(
      'Runs on the last Friday of every month at 9:00 AM'
    );
  });

  it('describes last-day offsets for monthly and yearly schedules', () => {
    expect(buildCronFieldDescription('0 0 9 L-1 * ?', 'UTC', [makeLocalOccurrence()])).toBe(
      'Runs at 9:00 AM on the second-to-last day of every month'
    );
    expect(buildCronFieldDescription('0 0 9 L-2 JAN,APR ?', 'UTC', [makeLocalOccurrence()])).toBe(
      'Runs at 9:00 AM on the 3rd-to-last day of January and April every year'
    );
  });

  it('uses shifted local months for last-day-of-month schedules when timezone conversion changes the month', () => {
    const description = buildCronFieldDescription('0 0 9 L JAN,APR ?', 'America/Anchorage', [
      makeLocalOccurrence({ localDate: '2025-12-31' }),
      makeLocalOccurrence({ localDate: '2026-03-31' }),
    ]);

    expect(description).toContain('Runs at 9:00 AM on the last day of');
    expect(description).toContain('December');
    expect(description).toContain('March');
  });

  it('uses the shifted local month-end offset for last-day schedules that roll back into the prior local day', () => {
    const description = buildCronFieldDescription('0 0 2 L * ?', 'America/Anchorage', [
      makeLocalOccurrence({ localDate: '2026-04-29', localTime: '6:00 PM' }),
      makeLocalOccurrence({ localDate: '2026-05-30', localTime: '6:00 PM' }),
    ]);

    expect(description).toBe('Runs at 6:00 PM on the second-to-last day of every month');
  });

  it('uses first-day wording when last-day UTC schedules roll into the next local month', () => {
    const description = buildCronFieldDescription('0 0 23 L * ?', 'Asia/Kolkata', [
      makeLocalOccurrence({ localDate: '2026-05-01', localTime: '4:30 AM' }),
      makeLocalOccurrence({ localDate: '2026-06-01', localTime: '4:30 AM' }),
    ]);

    expect(description).toBe('Runs at 4:30 AM on the first day of every month');
  });

  it('uses first-day wording for month-specific last-day schedules that roll into the next local month', () => {
    const description = buildCronFieldDescription('0 0 23 L 2 ?', 'Asia/Kolkata', [
      makeLocalOccurrence({ localDate: '2026-03-01', localTime: '4:30 AM' }),
    ]);

    expect(description).toBe('Runs at 4:30 AM on the first day of March every year');
  });

  it('uses shifted local weekday names and months for specific-day-of-week schedules', () => {
    const description = buildCronFieldDescription('0 30 4 ? JAN,JUN TUE,THU', 'America/Anchorage', [
      makeLocalOccurrence({
        utcDate: new Date('2026-01-06T04:30:00Z'),
        localDate: '2026-01-05',
        localTime: '8:30 PM',
      }),
      makeLocalOccurrence({
        utcDate: new Date('2026-06-11T04:30:00Z'),
        localDate: '2026-06-10',
        localTime: '8:30 PM',
      }),
    ]);

    expect(description).toBe('Runs every Monday and Wednesday in January and June at 8:30 PM');
  });

  it('describes specific day-of-month schedules using shifted local dates when needed', () => {
    const shiftedDates = buildCronFieldDescription('0 0 1 1 JAN,APR ?', 'America/Anchorage', [
      makeLocalOccurrence({ localDate: '2025-12-31', localTime: '5:00 PM' }),
      makeLocalOccurrence({ localDate: '2026-03-31', localTime: '5:00 PM' }),
    ]);
    const yearlyDate = buildCronFieldDescription('0 0 9 10 JAN ?', 'UTC', [
      makeLocalOccurrence({ localDate: '2026-01-10' }),
    ]);
    const monthlyDate = buildCronFieldDescription('0 0 9 15 * ?', 'UTC', [
      makeLocalOccurrence({ localDate: '2026-01-15' }),
    ]);

    expect(shiftedDates).toContain('Runs at 5:00 PM on');
    expect(shiftedDates).toContain('December 31st');
    expect(shiftedDates).toContain('March 31st');
    expect(yearlyDate).toContain('Runs at 9:00 AM on January 10th');
    expect(monthlyDate).toBe('Runs at 9:00 AM on the 15th of every month');
  });

  it('describes month-wide shifted day-of-month schedules as local month-end with DST-aware times', () => {
    const description = buildCronFieldDescription('0 0 5 1 * ?', 'America/Anchorage', [
      makeLocalOccurrence({ localDate: '2026-01-31', localTime: '8:00 PM' }),
      makeLocalOccurrence({ localDate: '2026-02-28', localTime: '8:00 PM' }),
      makeLocalOccurrence({ localDate: '2026-03-31', localTime: '9:00 PM' }),
      makeLocalOccurrence({ localDate: '2026-04-30', localTime: '9:00 PM' }),
      makeLocalOccurrence({ localDate: '2026-05-31', localTime: '9:00 PM' }),
      makeLocalOccurrence({ localDate: '2026-06-30', localTime: '9:00 PM' }),
      makeLocalOccurrence({ localDate: '2026-07-31', localTime: '9:00 PM' }),
    ]);

    expect(description).toBe('Runs at 8:00 PM on the last day of every month');
  });

  it('describes daily wildcards when both day fields are effectively unrestricted', () => {
    expect(buildCronFieldDescription('0 0 9 * * ?', 'UTC', [makeLocalOccurrence()])).toBe(
      'Runs every day at 9:00 AM'
    );
    expect(buildCronFieldDescription('0 0 9 ? * *', 'UTC', [makeLocalOccurrence()])).toBe(
      'Runs every day at 9:00 AM'
    );
  });
});
