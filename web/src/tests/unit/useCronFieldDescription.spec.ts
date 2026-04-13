import { describe, expect, it } from 'vitest';

import {
  buildCronFieldDescription,
  getMostCommonLocalTime,
} from '@/composables/cron/useCronFieldDescription';
import type { LocalOccurrence } from '@/composables/cron/useCronOccurrenceEngine';

function makeOccurrence(
  utcDate: string,
  localDate: string,
  localTime: string,
  offset: string = 'GMT+00:00'
): LocalOccurrence {
  return {
    utcDate: new Date(utcDate),
    localDate,
    localTime,
    offset,
  };
}

describe('useCronFieldDescription', () => {
  describe('getMostCommonLocalTime', () => {
    it('returns null for empty occurrences and the most common local time otherwise', () => {
      expect(getMostCommonLocalTime([])).toBeNull();
      expect(
        getMostCommonLocalTime([
          makeOccurrence('2024-01-01T10:00:00Z', '2024-01-01', '10:00 AM'),
          makeOccurrence('2024-01-02T10:00:00Z', '2024-01-02', '10:00 AM'),
          makeOccurrence('2024-01-03T11:00:00Z', '2024-01-03', '11:00 AM'),
        ])
      ).toBe('10:00 AM');
    });
  });

  describe('buildCronFieldDescription', () => {
    it('returns null for invalid cron expressions or when no local time can be derived', () => {
      expect(buildCronFieldDescription('bad cron', 'UTC', [])).toBeNull();
      expect(buildCronFieldDescription('0 0 10 * * ?', 'UTC', [])).toBeNull();
    });

    it('falls back to daily wildcard descriptions for invalid weekday tokens', () => {
      const occurrences = [makeOccurrence('2024-01-01T10:00:00Z', '2024-01-01', '10:00 AM')];

      expect(buildCronFieldDescription('0 0 10 ? * BAD#2', 'UTC', occurrences)).toBe(
        'Runs every day at 10:00 AM'
      );
      expect(buildCronFieldDescription('0 0 10 ? * BADL', 'UTC', occurrences)).toBe(
        'Runs every day at 10:00 AM'
      );
    });

    it('describes sub-daily and hourly interval schedules', () => {
      const intervalOccurrences = [makeOccurrence('2024-01-01T09:00:00Z', '2024-01-01', '9:00 AM')];

      expect(
        buildCronFieldDescription('0 */15 9-17 ? * MON-FRI', 'UTC', intervalOccurrences)
      ).toContain('Runs every 15 minutes');
      expect(buildCronFieldDescription('0 */15 * ? * *', 'UTC', intervalOccurrences)).toBe(
        'Runs every 15 minutes'
      );
      expect(buildCronFieldDescription('0 0 0/2 ? * MON', 'UTC', intervalOccurrences)).toBe(
        'Runs every 2 hours on Mondays'
      );
      expect(buildCronFieldDescription('0 */1 * ? * *', 'UTC', intervalOccurrences)).toBe(
        'Runs every 1 minute'
      );
      expect(buildCronFieldDescription('0 0 0/1 ? * *', 'UTC', intervalOccurrences)).toBe(
        'Runs every 1 hour'
      );
    });

    it('describes nth and last weekday schedules', () => {
      const occurrences = [makeOccurrence('2024-01-08T10:00:00Z', '2024-01-08', '10:00 AM')];

      expect(buildCronFieldDescription('0 0 10 ? * MON#2', 'UTC', occurrences)).toBe(
        'Runs on the second Monday of every month at 10:00 AM'
      );
      expect(buildCronFieldDescription('0 0 10 ? * FRIL', 'UTC', occurrences)).toBe(
        'Runs on the last Friday of every month at 10:00 AM'
      );
    });

    it('describes nearest weekday and last-day-offset schedules', () => {
      const occurrences = [makeOccurrence('2024-01-15T10:00:00Z', '2024-01-15', '10:00 AM')];

      expect(buildCronFieldDescription('0 0 10 LW * ?', 'UTC', occurrences)).toBe(
        'Runs at 10:00 AM on the last weekday of every month'
      );
      expect(buildCronFieldDescription('0 0 10 L-0W * ?', 'UTC', occurrences)).toBe(
        'Runs at 10:00 AM on the last weekday of every month'
      );
      expect(buildCronFieldDescription('0 0 10 L-2W JAN,MAR ?', 'UTC', occurrences)).toBe(
        'Runs at 10:00 AM on the weekday nearest the 3rd-to-last day of January and March'
      );
      expect(buildCronFieldDescription('0 0 10 15W * ?', 'UTC', occurrences)).toBe(
        'Runs at 10:00 AM on the weekday nearest the 15th of every month'
      );
      expect(buildCronFieldDescription('0 0 10 L-2 JAN,MAR ?', 'UTC', occurrences)).toBe(
        'Runs at 10:00 AM on the 3rd-to-last day of January and March every year'
      );
      expect(buildCronFieldDescription('0 0 10 15W JAN ?', 'UTC', occurrences)).toBe(
        'Runs at 10:00 AM on the weekday nearest the 15th of January'
      );
    });

    it('describes last-day schedules that shift to first day or month-end offsets locally', () => {
      const shiftedToFirstDay = [
        makeOccurrence('2024-01-31T23:30:00Z', '2024-02-01', '12:30 AM', 'GMT+01:00'),
        makeOccurrence('2024-02-29T23:30:00Z', '2024-03-01', '12:30 AM', 'GMT+01:00'),
      ];
      const shiftedToMonthEndOffset = [
        makeOccurrence('2024-01-31T22:00:00Z', '2024-01-30', '10:00 PM', 'GMT-02:00'),
        makeOccurrence('2024-02-29T22:00:00Z', '2024-02-28', '10:00 PM', 'GMT-02:00'),
      ];

      expect(buildCronFieldDescription('0 30 23 L * ?', 'UTC', shiftedToFirstDay)).toBe(
        'Runs at 12:30 AM on the first day of every month'
      );
      expect(buildCronFieldDescription('0 0 22 L * ?', 'UTC', shiftedToMonthEndOffset)).toBe(
        'Runs at 10:00 PM on the second-to-last day of every month'
      );
      expect(buildCronFieldDescription('0 0 22 L JAN,MAR ?', 'UTC', shiftedToMonthEndOffset)).toBe(
        'Runs at 10:00 PM on the second-to-last day of January and February every year'
      );
      expect(buildCronFieldDescription('0 30 23 L JAN,MAR ?', 'UTC', shiftedToFirstDay)).toBe(
        'Runs at 12:30 AM on the first day of February and March every year'
      );
      expect(
        buildCronFieldDescription('0 0 22 L JAN,MAR ?', 'UTC', [
          makeOccurrence('2024-01-31T22:00:00Z', '2024-01-31', '10:00 PM', 'GMT+00:00'),
          makeOccurrence('2024-03-31T22:00:00Z', '2024-03-31', '10:00 PM', 'GMT+00:00'),
        ])
      ).toBe('Runs at 10:00 PM on the last day of January and March');
    });

    it('describes specific day-of-week schedules with and without month filters', () => {
      const mondayOccurrences = [makeOccurrence('2024-01-01T10:00:00Z', '2024-01-01', '10:00 AM')];

      expect(buildCronFieldDescription('0 0 10 ? JAN,APR MON,WED', 'UTC', mondayOccurrences)).toBe(
        'Runs every Monday and Wednesday in January and April at 10:00 AM'
      );
      expect(buildCronFieldDescription('0 0 10 15 * MON', 'UTC', mondayOccurrences)).toBe(
        'Runs every Monday at 10:00 AM'
      );
      expect(buildCronFieldDescription('0 0 10 ? * MON-FRI', 'UTC', mondayOccurrences)).toBe(
        'Runs every Monday, Tuesday, Wednesday, Thursday and Friday at 10:00 AM'
      );
      expect(
        buildCronFieldDescription('0 0 10 ? * MON', 'America/Anchorage', [
          makeOccurrence('2024-01-01T02:00:00Z', '2023-12-31', '6:00 PM', 'GMT-08:00'),
          makeOccurrence('2024-01-08T02:00:00Z', '2024-01-07', '6:00 PM', 'GMT-08:00'),
        ])
      ).toBe('Runs every Sunday at 6:00 PM');
    });

    it('describes specific day-of-month schedules including shifted local dates', () => {
      const shiftedLocalDates = [
        makeOccurrence('2024-01-01T00:15:00Z', '2023-12-31', '4:15 PM', 'GMT-08:00'),
        makeOccurrence('2024-04-01T00:15:00Z', '2024-03-31', '4:15 PM', 'GMT-08:00'),
        makeOccurrence('2024-07-01T00:15:00Z', '2024-06-30', '4:15 PM', 'GMT-08:00'),
      ];
      const monthFiltered = [makeOccurrence('2024-01-15T10:00:00Z', '2024-01-15', '10:00 AM')];

      const shiftedDescription = buildCronFieldDescription(
        '0 15 0 1 JAN,APR,JUL ? ',
        'America/Anchorage',
        shiftedLocalDates
      );

      expect(shiftedDescription).toContain('Runs at 4:15 PM on');
      expect(shiftedDescription).toContain('December 31st');
      expect(shiftedDescription).toContain('March 31st');
      expect(shiftedDescription).toContain('June 30th');
      expect(buildCronFieldDescription('0 0 10 15 JAN,APR ? ', 'UTC', monthFiltered)).toBe(
        'Runs at 10:00 AM on the 15th of January and April every year'
      );
      expect(buildCronFieldDescription('0 0 10 15 * ? ', 'UTC', monthFiltered)).toBe(
        'Runs at 10:00 AM on the 15th of every month'
      );
      expect(buildCronFieldDescription('0 0 10 15 BAD ? ', 'UTC', monthFiltered)).toBeNull();
      expect(
        buildCronFieldDescription('0 0 10 15 JAN ? ', 'America/Anchorage', [
          makeOccurrence('2024-01-16T02:00:00Z', '2024-01-15', '6:00 PM', 'GMT-08:00'),
        ])
      ).toBe('Runs at 6:00 PM on January 15th');
      expect(
        buildCronFieldDescription('0 0 10 15 * ? ', 'America/Anchorage', [
          makeOccurrence('2024-01-16T02:00:00Z', '2024-01-15', '6:00 PM', 'GMT-08:00'),
          makeOccurrence('2024-02-16T02:00:00Z', '2024-02-15', '6:00 PM', 'GMT-08:00'),
          makeOccurrence('2024-03-16T02:00:00Z', '2024-03-15', '6:00 PM', 'GMT-08:00'),
          makeOccurrence('2024-04-16T02:00:00Z', '2024-04-15', '6:00 PM', 'GMT-08:00'),
          makeOccurrence('2024-05-16T02:00:00Z', '2024-05-15', '6:00 PM', 'GMT-08:00'),
          makeOccurrence('2024-06-16T02:00:00Z', '2024-06-15', '6:00 PM', 'GMT-08:00'),
          makeOccurrence('2024-07-16T02:00:00Z', '2024-07-15', '6:00 PM', 'GMT-08:00'),
        ])
      ).toBe('Runs at 6:00 PM on the 15th of every month');
    });

    it('summarizes shifted month-end local dates when there are too many explicit dates to list', () => {
      const shiftedMonthEndDates = [
        makeOccurrence('2024-01-01T02:00:00Z', '2023-12-31', '6:00 PM', 'GMT-08:00'),
        makeOccurrence('2024-02-01T02:00:00Z', '2024-01-31', '6:00 PM', 'GMT-08:00'),
        makeOccurrence('2024-03-01T02:00:00Z', '2024-02-29', '6:00 PM', 'GMT-08:00'),
        makeOccurrence('2024-04-01T02:00:00Z', '2024-03-31', '6:00 PM', 'GMT-08:00'),
        makeOccurrence('2024-05-01T02:00:00Z', '2024-04-30', '6:00 PM', 'GMT-08:00'),
        makeOccurrence('2024-06-01T02:00:00Z', '2024-05-31', '6:00 PM', 'GMT-08:00'),
        makeOccurrence('2024-07-01T02:00:00Z', '2024-06-30', '6:00 PM', 'GMT-08:00'),
      ];

      expect(
        buildCronFieldDescription('0 0 2 1 * ? ', 'America/Anchorage', shiftedMonthEndDates)
      ).toBe('Runs at 6:00 PM on the last day of every month');
    });

    it('describes the second-to-last day branch for last-day offsets', () => {
      const occurrences = [makeOccurrence('2024-01-30T10:00:00Z', '2024-01-30', '10:00 AM')];

      expect(buildCronFieldDescription('0 0 10 L-1 * ?', 'UTC', occurrences)).toBe(
        'Runs at 10:00 AM on the second-to-last day of every month'
      );
    });

    it('describes daily wildcard schedules', () => {
      const occurrences = [makeOccurrence('2024-01-01T10:00:00Z', '2024-01-01', '10:00 AM')];
      expect(buildCronFieldDescription('0 0 10 * * ?', 'UTC', occurrences)).toBe(
        'Runs every day at 10:00 AM'
      );
      expect(buildCronFieldDescription('0 0 10 ? * ?', 'UTC', occurrences)).toBe(
        'Runs every day at 10:00 AM'
      );
    });

    it('describes specific weekdays with month-step filters and ranged hours', () => {
      const occurrences = [makeOccurrence('2024-01-01T09:00:00Z', '2024-01-01', '9:00 AM')];

      expect(buildCronFieldDescription('0 */20 9-17 ? JAN/2 MON-FRI', 'UTC', occurrences)).toBe(
        'Runs every 20 minutes between 9 AM and 5 PM on Monday, Tuesday, Wednesday, Thursday and Friday'
      );
      expect(buildCronFieldDescription('0 0 10 ? JAN/2 MON', 'UTC', occurrences)).toBe(
        'Runs every Monday in January, March, May, July, September and November at 9:00 AM'
      );
    });
  });
});
