import { describe, expect, it } from 'vitest';

import { cronToTextWithTimezoneUniversal } from '@/composables/cron/useCronOccurrences';

const timezone = 'Asia/Kolkata';
const defaultReferenceDate = '2026-04-13';

interface CronSuccessCase {
  expression: string;
  expectedText: string;
  referenceDate?: string;
}

interface CronInvalidCase {
  expression: string;
  expectedError: string;
  referenceDate?: string;
}

const successCases: CronSuccessCase[] = [
  {
    expression: '0 0 * * * ?',
    expectedText: 'Runs every hour at 30 minutes past the hour',
  },
  {
    expression: '0 0/1 * * * ?',
    expectedText: 'Runs every 1 minute',
  },
  {
    expression: '0 0 0/1 * * ?',
    expectedText: 'Runs every 1 hour',
  },
  {
    expression: '0 15 10 * * ?',
    expectedText: 'Runs every day at 3:45 PM',
  },
  {
    expression: '0 0 12 ? * MON-FRI',
    expectedText: 'Runs every Monday, Tuesday, Wednesday, Thursday and Friday at 5:30 PM',
  },
  {
    expression: '0 0 12 1 * ?',
    expectedText: 'Runs at 5:30 PM on the 1st of every month',
  },
  {
    expression: '0 0 0 L * ?',
    expectedText: 'Runs at 5:30 AM on the last day of every month',
  },
  {
    expression: '0 0 12 ? * SUN',
    expectedText: 'Runs every Sunday at 5:30 PM',
  },
  {
    expression: '0 0/30 * * * ?',
    expectedText: 'Runs every 30 minutes',
  },
  {
    expression: '0 10,20,30 * * * ?',
    expectedText: 'Runs every hour at 5:40 AM, 5:50 AM and 6 AM',
  },
  {
    expression: '0 0 9-17 * * ?',
    expectedText: 'Runs every hour between 2:30 PM and 10:30 PM',
  },
  {
    expression: '0 0 12 ? * MON#2',
    expectedText: 'Runs on the second Monday of every month at 5:30 PM',
  },
  {
    expression: '0 0 0 1 1 ? *',
    expectedText: 'Runs at 5:30 AM on January 1st every year',
  },
  {
    expression: '0 0/5 14 * * ?',
    expectedText: 'Runs every 5 minutes between 7:30 PM and 8:25 PM',
  },
  {
    expression: '0 0 10 L-1 * ?',
    expectedText: 'Runs at 3:30 PM on the second-to-last day of every month',
  },
  {
    expression: '0 0 8 ? * MON,WED,FRI',
    expectedText: 'Runs every Monday, Wednesday and Friday at 1:30 PM',
  },
  {
    expression: '0 0 12 15 * ?',
    expectedText: 'Runs at 5:30 PM on the 15th of every month',
  },
  {
    expression: '0 0 6 ? * SAT,SUN',
    expectedText: 'Runs every Saturday and Sunday at 11:30 AM',
  },
  {
    expression: '0 0 18 ? * MON-FRI',
    expectedText: 'Runs every Monday, Tuesday, Wednesday, Thursday and Friday at 11:30 PM',
  },
  {
    expression: '0 0 0 29 2 ?',
    expectedText: 'Runs at 5:30 AM on February 29th every leap year',
  },
  {
    expression: '0 0 23 * * ?',
    expectedText: 'Runs every day at 4:30 AM',
  },
  {
    expression: '0 45 6 ? * TUE',
    expectedText: 'Runs every Tuesday at 12:15 PM',
  },
  {
    expression: '0 0 1 1/2 * ?',
    expectedText: 'Runs at 6:30 AM every 2 days starting from the 1st of every month',
  },
  {
    expression: '0 0 7 ? * 2L',
    expectedText: 'Runs on the last Monday of every month at 12:30 PM',
  },
  {
    expression: '0 0 9 LW * ?',
    expectedText: 'Runs at 2:30 PM on the last weekday of every month',
  },
  {
    expression: '0 0 10 1W * ?',
    expectedText: 'Runs at 3:30 PM on the weekday nearest the 1st of every month',
  },
  {
    expression: '0 0/15 8-10 * * ?',
    expectedText: 'Runs every 15 minutes between 1:30 PM and 4:15 PM',
  },
  {
    expression: '0 30 14 1 * ?',
    expectedText: 'Runs at 8:00 PM on the 1st of every month',
  },
  {
    expression: '0 0 5 ? 1,6,12 MON',
    expectedText: 'Runs every Monday in January, June and December at 10:30 AM',
  },
  {
    expression: '0 20 10 L 1,2,3,4,5,6,7,8,12 ?',
    expectedText:
      'Runs at 3:50 PM on the last day of January, February, March, April, May, June, July, August and December',
  },
  {
    expression: '0 0 8 L 1,3,5,7,8,10,12 ?',
    expectedText:
      'Runs at 1:30 PM on the last day of January, March, May, July, August, October and December',
  },
  {
    expression: '0 30 14 L 2,4,6,9,11 ?',
    expectedText:
      'Runs at 8:00 PM on the last day of February, April, June, September and November',
  },
  {
    expression: '0 20 10 ? * MON,TUE,WED,THU,FRI',
    expectedText: 'Runs every Monday, Tuesday, Wednesday, Thursday and Friday at 3:50 PM',
  },
  {
    expression: '0 0 9 ? * SAT,SUN',
    expectedText: 'Runs every Saturday and Sunday at 2:30 PM',
  },
  {
    expression: '0 15 6 ? * MON,WED,FRI',
    expectedText: 'Runs every Monday, Wednesday and Friday at 11:45 AM',
  },
  {
    expression: '0 0 10 ? * MON-FRI',
    expectedText: 'Runs every Monday, Tuesday, Wednesday, Thursday and Friday at 3:30 PM',
  },
  {
    expression: '0 0 8-18 * * ?',
    expectedText: 'Runs every hour between 1:30 PM and 11:30 PM',
  },
  {
    expression: '0 0/15 9-17 * * ?',
    expectedText: 'Runs every 15 minutes between 2:30 PM and 11:15 PM',
  },
  {
    expression: '0 0 12 L * ?',
    expectedText: 'Runs at 5:30 PM on the last day of every month',
  },
  {
    expression: '0 30 23 * * ?',
    expectedText: 'Runs every day at 5:00 AM',
  },
];

const invalidCases: CronInvalidCase[] = [
  {
    expression: '0 0 0 31 4,6,9,11 ?',
    expectedError: 'Invalid cron expression',
  },
];

describe('Asia/Kolkata cron regression matrix', () => {
  successCases.forEach(({ expression, expectedText, referenceDate }) => {
    it(`formats ${expression}`, () => {
      const result = cronToTextWithTimezoneUniversal(
        expression,
        timezone,
        referenceDate ?? defaultReferenceDate
      );

      expect(result.success).toBe(true);
      expect(result.text).toBe(expectedText);
      expect(result.timezoneLabel).toContain('Asia/Kolkata');
      expect(result.timezoneLabel).toContain('GMT+05:30');
    });
  });

  invalidCases.forEach(({ expression, expectedError, referenceDate }) => {
    it(`rejects ${expression}`, () => {
      const result = cronToTextWithTimezoneUniversal(
        expression,
        timezone,
        referenceDate ?? defaultReferenceDate
      );

      expect(result.success).toBe(false);
      expect(result.error).toBe(expectedError);
    });
  });
});
