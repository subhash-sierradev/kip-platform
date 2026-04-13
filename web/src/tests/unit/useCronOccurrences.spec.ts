import { afterEach, describe, expect, it, vi } from 'vitest';

import type { LocalOccurrence, PatternDescription } from '@/composables/cron/useCronOccurrences';
import {
  cronToTextWithTimezoneUniversal,
  isValidQuartzCronExpression,
  renderScheduleDescription,
} from '@/composables/cron/useCronOccurrences';

function makeOccurrence(overrides: Partial<LocalOccurrence> = {}): LocalOccurrence {
  return {
    utcDate: new Date('2026-01-31T04:00:00Z'),
    localDate: '2026-01-31',
    localTime: '8:00 PM',
    offset: 'GMT-08:00',
    ...overrides,
  };
}

function makeAnalysis(
  overrides: Partial<PatternDescription> = {},
  occurrences: LocalOccurrence[] = [makeOccurrence()]
): PatternDescription {
  return {
    type: 'daily',
    occurrences,
    regularityPattern: null,
    ...overrides,
  } as PatternDescription;
}

afterEach(() => {
  vi.resetModules();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  vi.unmock('@/composables/cron/useCronOccurrenceEngine');
  vi.unmock('@/composables/cron/useCronFieldDescription');
});

describe('useCronOccurrences validation', () => {
  it('rejects blank and malformed Quartz expressions', () => {
    expect(isValidQuartzCronExpression('')).toBe(false);
    expect(isValidQuartzCronExpression('0 0 9 * *')).toBe(false);
  });

  it('rejects unsupported wildcard day-of-week steps and conflicting day fields', () => {
    expect(isValidQuartzCronExpression('0 0 9 ? * */2')).toBe(false);
    expect(isValidQuartzCronExpression('0 0 9 1 * MON')).toBe(false);
  });

  it('rejects impossible day-of-month and month combinations', () => {
    expect(isValidQuartzCronExpression('0 0 9 31 2 ?')).toBe(false);
    expect(isValidQuartzCronExpression('0 0 9 L-2W FEB ?')).toBe(true);
  });

  it('accepts wrapped day-of-week ranges after sanitizing them for validation', () => {
    expect(isValidQuartzCronExpression('0 0 9 ? * FRI-MON')).toBe(true);
  });

  it('validates nearest-weekday day-of-month tokens', () => {
    expect(isValidQuartzCronExpression('0 0 9 LW * ?')).toBe(true);
    expect(isValidQuartzCronExpression('0 0 9 L-2W * ?')).toBe(true);
    expect(isValidQuartzCronExpression('0 0 9 L-40W * ?')).toBe(false);
    expect(isValidQuartzCronExpression('0 0 9 32W * ?')).toBe(false);
    expect(isValidQuartzCronExpression('0 0 9 15W * MON')).toBe(false);
  });

  it('validates last-day offsets and last weekday tokens', () => {
    expect(isValidQuartzCronExpression('0 0 9 L-2 * ?')).toBe(true);
    expect(isValidQuartzCronExpression('0 0 9 L-40 * ?')).toBe(false);
    expect(isValidQuartzCronExpression('0 0 9 ? * 6L')).toBe(true);
  });

  it('validates nth weekday tokens and rejects invalid weekday aliases', () => {
    expect(isValidQuartzCronExpression('0 0 9 ? * MON#2')).toBe(true);
    expect(isValidQuartzCronExpression('0 0 9 ? * ZZ#2')).toBe(false);
  });
});

describe('useCronOccurrences descriptions', () => {
  it('returns a no-occurrence description for empty analyses', () => {
    expect(renderScheduleDescription(makeAnalysis({ occurrences: [] }, []), 'UTC')).toBe(
      'No scheduled occurrences in the next period'
    );
  });

  it('renders weekly, monthly, and interval pattern descriptions', () => {
    const weeklyOccurrences = [
      makeOccurrence({ localDate: '2026-02-02' }),
      makeOccurrence({ localDate: '2026-02-04' }),
      makeOccurrence({ localDate: '2026-02-06' }),
    ];

    expect(
      renderScheduleDescription(makeAnalysis({ type: 'weekly' }, weeklyOccurrences), 'UTC')
    ).toBe('Runs every Monday, Wednesday and Friday at 8:00 PM');

    const monthlyOccurrences = [
      makeOccurrence({ localDate: '2026-01-01' }),
      makeOccurrence({ localDate: '2026-01-15' }),
      makeOccurrence({ localDate: '2026-01-31' }),
    ];

    expect(
      renderScheduleDescription(makeAnalysis({ type: 'monthly' }, monthlyOccurrences), 'UTC')
    ).toBe('Runs on the 1st, 15th and 31st of each month at 8:00 PM');

    expect(
      renderScheduleDescription(
        makeAnalysis({ type: 'interval', regularityPattern: 'every 2 hours' }),
        'UTC'
      )
    ).toBe('Runs every 2 hours at 8:00 PM');
  });

  it('renders single occurrences with the explicit local date and offset', () => {
    const analysis = makeAnalysis({ type: 'single-occurrence' });

    expect(renderScheduleDescription(analysis, 'UTC')).toBe(
      'Runs on 2026-01-31 at 8:00 PM (GMT-08:00)'
    );
  });

  it('renders specific shifted local dates for small time-varies schedules', () => {
    const occurrences = [
      makeOccurrence({ localDate: '2026-01-31', utcDate: new Date('2026-02-01T04:00:00Z') }),
      makeOccurrence({ localDate: '2026-03-31', utcDate: new Date('2026-04-01T04:00:00Z') }),
    ];

    expect(
      renderScheduleDescription(makeAnalysis({ type: 'time-varies' }, occurrences), 'UTC')
    ).toBe('Runs at 8:00 PM on the last day of January and March');
  });

  it('falls back when time-varies schedules span too many distinct local dates', () => {
    const occurrences = Array.from({ length: 13 }, (_, index) =>
      makeOccurrence({
        localDate: `2026-${String(index + 1).padStart(2, '0')}-15`,
        utcDate: new Date(Date.UTC(2026, index, 15, 4, 0, 0)),
      })
    );

    expect(
      renderScheduleDescription(makeAnalysis({ type: 'time-varies' }, occurrences), 'UTC')
    ).toBe('Runs around 8:00 PM with local time changes');
  });

  it('falls back when time-varies schedules do not have enough occurrences for a date-shift summary', () => {
    expect(renderScheduleDescription(makeAnalysis({ type: 'time-varies' }), 'UTC')).toBe(
      'Runs around 8:00 PM with local time changes'
    );
  });

  it('falls back to a custom schedule description when no specific pattern matches', () => {
    expect(
      renderScheduleDescription(
        makeAnalysis({ type: 'custom' as PatternDescription['type'] }),
        'UTC'
      )
    ).toBe('Runs on a custom schedule at 8:00 PM');
  });

  it('uses pattern rendering when a cron field description is unavailable', async () => {
    vi.doMock('@/composables/cron/useCronFieldDescription', async () => {
      const actual = await vi.importActual<
        typeof import('@/composables/cron/useCronFieldDescription')
      >('@/composables/cron/useCronFieldDescription');

      return {
        ...actual,
        buildCronFieldDescription: vi.fn(() => null),
      };
    });

    const { renderScheduleDescription: renderWithMock } =
      await import('@/composables/cron/useCronOccurrences');

    expect(renderWithMock(makeAnalysis(), 'UTC', '0 0 20 * * ?')).toBe('Runs every day at 8:00 PM');
  });

  it('falls back to the first occurrence time when no most-common local time is available', async () => {
    vi.doMock('@/composables/cron/useCronFieldDescription', async () => {
      const actual = await vi.importActual<
        typeof import('@/composables/cron/useCronFieldDescription')
      >('@/composables/cron/useCronFieldDescription');

      return {
        ...actual,
        buildCronFieldDescription: vi.fn(() => null),
        getMostCommonLocalTime: vi.fn(() => null),
      };
    });

    const { renderScheduleDescription: renderWithMock } =
      await import('@/composables/cron/useCronOccurrences');

    expect(
      renderWithMock(
        makeAnalysis({ type: 'interval', regularityPattern: 'every weekday' }, [
          makeOccurrence({ localTime: '6:15 AM' }),
        ]),
        'UTC'
      )
    ).toBe('Runs every weekday at 6:15 AM');
  });
});

describe('useCronOccurrences public formatting', () => {
  it('returns an invalid-timezone error before processing the cron expression', async () => {
    vi.doMock('@/composables/cron/useCronOccurrenceEngine', async () => {
      const actual = await vi.importActual<
        typeof import('@/composables/cron/useCronOccurrenceEngine')
      >('@/composables/cron/useCronOccurrenceEngine');

      return {
        ...actual,
        validateTimeZone: vi.fn(() => ({ success: false, error: 'bad timezone' })),
      };
    });

    const { cronToTextWithTimezoneUniversal: formatWithMock } =
      await import('@/composables/cron/useCronOccurrences');

    expect(formatWithMock('0 0 9 * * ?', 'Mars/Base', '2026-03-17')).toStrictEqual({
      success: false,
      text: '',
      timezoneLabel: 'Mars/Base',
      isDateRangeShift: false,
      error: 'bad timezone',
    });
  });

  it('returns a no-occurrence result when the engine generates no future runs', async () => {
    vi.doMock('@/composables/cron/useCronOccurrenceEngine', async () => {
      const actual = await vi.importActual<
        typeof import('@/composables/cron/useCronOccurrenceEngine')
      >('@/composables/cron/useCronOccurrenceEngine');

      return {
        ...actual,
        generateOccurrences: vi.fn(() => ({ success: true, data: [] })),
        validateTimeZone: vi.fn(() => ({ success: true, data: 'UTC' })),
      };
    });

    const { cronToTextWithTimezoneUniversal: formatWithMock } =
      await import('@/composables/cron/useCronOccurrences');

    expect(formatWithMock('0 0 9 * * ?', 'UTC', '2026-03-17')).toStrictEqual({
      success: true,
      text: 'No scheduled occurrences in the next period',
      timezoneLabel: 'UTC',
      isDateRangeShift: false,
    });
  });

  it('accepts parseable non-ISO reference dates', () => {
    const result = cronToTextWithTimezoneUniversal(
      '0 30 9 * * ?',
      'UTC',
      'March 17, 2026 00:00:00 UTC'
    );

    expect(result.success).toBe(true);
    expect(result.text).toContain('Runs every day at 9:30 AM');
    expect(result.isDateRangeShift).toBe(false);
  });

  it('falls back to the current date when the reference date is invalid', () => {
    const result = cronToTextWithTimezoneUniversal('0 0 9 * * ?', 'UTC', 'not-a-date');

    expect(result.success).toBe(true);
    expect(result.text).toContain('Runs every day at 9:00 AM');
    expect(result.isDateRangeShift).toBe(false);
  });

  it('treats hour ranges as valid schedules without date-shift metadata', () => {
    const result = cronToTextWithTimezoneUniversal('0 0 9-17 * * ?', 'UTC', '2026-03-17');

    expect(result.success).toBe(true);
    expect(result.isDateRangeShift).toBe(false);
    expect(result.text).toContain('Runs every day at');
  });

  it('flags date-range shifts when the localized date differs from the UTC reference day', () => {
    const result = cronToTextWithTimezoneUniversal(
      '0 0 1 * * ?',
      'America/Anchorage',
      '2026-03-17'
    );

    expect(result.success).toBe(true);
    expect(result.isDateRangeShift).toBe(true);
    expect(result.timezoneLabel).toContain('America/Anchorage');
  });

  it('returns the occurrence engine error when generation fails', async () => {
    vi.doMock('@/composables/cron/useCronOccurrenceEngine', async () => {
      const actual = await vi.importActual<
        typeof import('@/composables/cron/useCronOccurrenceEngine')
      >('@/composables/cron/useCronOccurrenceEngine');

      return {
        ...actual,
        generateOccurrences: vi.fn(() => ({ success: false, error: 'engine failed' })),
      };
    });

    const { cronToTextWithTimezoneUniversal: formatWithMock } =
      await import('@/composables/cron/useCronOccurrences');

    expect(formatWithMock('0 0 9 * * ?', 'UTC', '2026-03-17')).toStrictEqual({
      success: false,
      text: '',
      timezoneLabel: 'UTC',
      isDateRangeShift: false,
      error: 'engine failed',
    });
  });

  it('stringifies thrown non-Error failures from the occurrence pipeline', async () => {
    vi.doMock('@/composables/cron/useCronOccurrenceEngine', async () => {
      const actual = await vi.importActual<
        typeof import('@/composables/cron/useCronOccurrenceEngine')
      >('@/composables/cron/useCronOccurrenceEngine');

      return {
        ...actual,
        generateOccurrences: vi.fn(() => {
          throw 'boom';
        }),
      };
    });

    const { cronToTextWithTimezoneUniversal: formatWithMock } =
      await import('@/composables/cron/useCronOccurrences');
    const result = formatWithMock('0 0 9 * * ?', 'UTC', '2026-03-17');

    expect(result.success).toBe(false);
    expect(result.error).toBe('Failed to process cron: boom');
  });

  it('falls back to the raw timezone label when no offset token is available', async () => {
    vi.doMock('@/composables/cron/useCronOccurrenceEngine', async () => {
      const actual = await vi.importActual<
        typeof import('@/composables/cron/useCronOccurrenceEngine')
      >('@/composables/cron/useCronOccurrenceEngine');

      return {
        ...actual,
        analyzeLocalPattern: vi.fn(() => makeAnalysis()),
        generateOccurrences: vi.fn(() => ({ success: true, data: [makeOccurrence()] })),
        validateTimeZone: vi.fn(() => ({ success: true, data: 'UTC' })),
      };
    });

    const originalDateTimeFormat = Intl.DateTimeFormat;
    vi.stubGlobal('Intl', {
      ...Intl,
      DateTimeFormat: function MockDateTimeFormat(
        _locale: string,
        options?: Intl.DateTimeFormatOptions
      ) {
        if (options?.timeZoneName === 'shortOffset') {
          return {
            formatToParts: () => [],
          };
        }

        return new originalDateTimeFormat('en-US', options);
      },
    });

    const { cronToTextWithTimezoneUniversal: formatWithMock } =
      await import('@/composables/cron/useCronOccurrences');
    const result = formatWithMock('0 */15 * * * ?', 'UTC', '2026-03-17');

    expect(result.success).toBe(true);
    expect(result.timezoneLabel).toBe('UTC');
  });

  it('falls back to the raw timezone label when formatting the offset throws', async () => {
    const originalDateTimeFormat = Intl.DateTimeFormat;
    vi.stubGlobal('Intl', {
      ...Intl,
      DateTimeFormat: function ThrowingDateTimeFormat(
        locale: string,
        options?: Intl.DateTimeFormatOptions
      ) {
        if (options?.timeZoneName === 'shortOffset') {
          throw new Error('offset unavailable');
        }

        return new originalDateTimeFormat(locale, options);
      },
    });

    const result = cronToTextWithTimezoneUniversal(
      '0 0 9 * * ?',
      'UTC',
      new Date('2026-03-17T00:00:00Z')
    );

    expect(result.success).toBe(true);
    expect(result.timezoneLabel).toBe('UTC');
  });
});

describe('useCronOccurrences validation edge cases', () => {
  it('rejects cron expressions with invalid months and invalid nth weekday aliases', () => {
    expect(isValidQuartzCronExpression('0 0 9 ? FOO MON')).toBe(false);
    expect(isValidQuartzCronExpression('0 0 9 ? * MONDAY#2')).toBe(false);
  });

  it('rejects invalid last-day and nearest-weekday combinations', () => {
    expect(isValidQuartzCronExpression('0 0 9 L * MON')).toBe(false);
    expect(isValidQuartzCronExpression('0 0 9 L-32 * ?')).toBe(false);
    expect(isValidQuartzCronExpression('0 0 9 LW * MON')).toBe(false);
  });
});
