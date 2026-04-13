import { describe, expect, it } from 'vitest';

import { generateOccurrences, useCron } from '@/composables/cron/useCron';
import { validateCronExpression } from '@/utils/cronValidation';

interface ExactOccurrenceExpectation {
  expression: string;
  firstOccurrenceIso: string;
}

const start = new Date('2026-01-01T00:00:00Z');
const end = new Date('2030-12-31T23:59:59Z');

const exactOccurrenceCases: ExactOccurrenceExpectation[] = [
  { expression: '0 0 12 * * ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 15 10 * * ?', firstOccurrenceIso: '2026-01-01T10:15:00.000Z' },
  { expression: '0 0/5 * * * ?', firstOccurrenceIso: '2026-01-01T00:00:00.000Z' },
  { expression: '0 0 * * * ?', firstOccurrenceIso: '2026-01-01T00:00:00.000Z' },
  { expression: '0 0 0 * * ?', firstOccurrenceIso: '2026-01-01T00:00:00.000Z' },
  { expression: '0 30 8 * * ?', firstOccurrenceIso: '2026-01-01T08:30:00.000Z' },
  { expression: '0 0 9-17 * * ?', firstOccurrenceIso: '2026-01-01T09:00:00.000Z' },
  { expression: '0 0 12 ? * MON', firstOccurrenceIso: '2026-01-05T12:00:00.000Z' },
  { expression: '0 0 12 ? * FRI', firstOccurrenceIso: '2026-01-02T12:00:00.000Z' },
  { expression: '0 0 12 1 * ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 15 * ?', firstOccurrenceIso: '2026-01-15T12:00:00.000Z' },
  { expression: '0 0 12 1 JAN ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 1 JAN,APR ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 ? JAN MON', firstOccurrenceIso: '2026-01-05T12:00:00.000Z' },
  { expression: '0 0 0 ? * SUN', firstOccurrenceIso: '2026-01-04T00:00:00.000Z' },
  { expression: '0 0 6 ? * MON-FRI', firstOccurrenceIso: '2026-01-01T06:00:00.000Z' },
  { expression: '0 0 18 ? * MON-FRI', firstOccurrenceIso: '2026-01-01T18:00:00.000Z' },
  { expression: '0 0 12 10 * ?', firstOccurrenceIso: '2026-01-10T12:00:00.000Z' },
  { expression: '0 0 12 1-10 * ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 */2 * ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 0 L * ?', firstOccurrenceIso: '2026-01-31T00:00:00.000Z' },
  { expression: '0 0 0 L-1 * ?', firstOccurrenceIso: '2026-01-30T00:00:00.000Z' },
  { expression: '0 0 0 L-2 * ?', firstOccurrenceIso: '2026-01-29T00:00:00.000Z' },
  { expression: '0 0 0 LW * ?', firstOccurrenceIso: '2026-01-30T00:00:00.000Z' },
  { expression: '0 0 9 1W * ?', firstOccurrenceIso: '2026-01-01T09:00:00.000Z' },
  { expression: '0 0 9 15W * ?', firstOccurrenceIso: '2026-01-15T09:00:00.000Z' },
  { expression: '0 0 10 ? * MON#1', firstOccurrenceIso: '2026-01-05T10:00:00.000Z' },
  { expression: '0 0 10 ? * MON#2', firstOccurrenceIso: '2026-01-12T10:00:00.000Z' },
  { expression: '0 0 10 ? * MON#3', firstOccurrenceIso: '2026-01-19T10:00:00.000Z' },
  { expression: '0 0 10 ? * MON#4', firstOccurrenceIso: '2026-01-26T10:00:00.000Z' },
  { expression: '0 0 10 ? * MON#5', firstOccurrenceIso: '2026-03-30T10:00:00.000Z' },
  { expression: '0 0 10 ? * FRI#1', firstOccurrenceIso: '2026-01-02T10:00:00.000Z' },
  { expression: '0 0 10 ? * FRI#3', firstOccurrenceIso: '2026-01-16T10:00:00.000Z' },
  { expression: '0 0 10 ? * SUN#2', firstOccurrenceIso: '2026-01-11T10:00:00.000Z' },
  { expression: '0 0 10 ? * 6L', firstOccurrenceIso: '2026-01-30T10:00:00.000Z' },
  { expression: '0 0 10 ? * 2L', firstOccurrenceIso: '2026-01-26T10:00:00.000Z' },
  { expression: '0 0 10 ? * 1L', firstOccurrenceIso: '2026-01-25T10:00:00.000Z' },
  { expression: '0 0 10 ? * 7L', firstOccurrenceIso: '2026-01-31T10:00:00.000Z' },
  { expression: '0 0 0 28-31 * ?', firstOccurrenceIso: '2026-01-28T00:00:00.000Z' },
  { expression: '0 0 0 1-31/2 * ?', firstOccurrenceIso: '2026-01-01T00:00:00.000Z' },
  { expression: '0 0/10 * * * ?', firstOccurrenceIso: '2026-01-01T00:00:00.000Z' },
  { expression: '0 */15 * * * ?', firstOccurrenceIso: '2026-01-01T00:00:00.000Z' },
  { expression: '0 0 0 ? JAN MON#1', firstOccurrenceIso: '2026-01-05T00:00:00.000Z' },
  { expression: '0 0 0 ? FEB MON#1', firstOccurrenceIso: '2026-02-02T00:00:00.000Z' },
  { expression: '0 0 0 29 FEB ?', firstOccurrenceIso: '2028-02-29T00:00:00.000Z' },
  { expression: '0 0 0 31 DEC ?', firstOccurrenceIso: '2026-12-31T00:00:00.000Z' },
  { expression: '0 0 23 * * ?', firstOccurrenceIso: '2026-01-01T23:00:00.000Z' },
  { expression: '0 59 23 * * ?', firstOccurrenceIso: '2026-01-01T23:59:00.000Z' },
  { expression: '0 0 12 ? * MON-SUN', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 ? * MON,WED,FRI', firstOccurrenceIso: '2026-01-02T12:00:00.000Z' },
  { expression: '0 0 12 1-15 * ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 */5 * ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 ? JAN-MAR MON', firstOccurrenceIso: '2026-01-05T12:00:00.000Z' },
  { expression: '0 0 12 ? * MON-FRI', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 1 JAN,APR,JUL,OCT ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 L JAN ?', firstOccurrenceIso: '2026-01-31T12:00:00.000Z' },
  { expression: '0 0 12 LW FEB ?', firstOccurrenceIso: '2026-02-27T12:00:00.000Z' },
  { expression: '0 0 12 1W MAR ?', firstOccurrenceIso: '2026-03-02T12:00:00.000Z' },
  { expression: '0 0 12 ? APR MON#2', firstOccurrenceIso: '2026-04-13T12:00:00.000Z' },
  { expression: '0 0 12 ? MAY FRI#3', firstOccurrenceIso: '2026-05-15T12:00:00.000Z' },
  { expression: '0 0 12 ? JUN 6L', firstOccurrenceIso: '2026-06-26T12:00:00.000Z' },
  { expression: '0 0 12 ? JUL 2L', firstOccurrenceIso: '2026-07-27T12:00:00.000Z' },
  { expression: '0 0 12 ? AUG 1L', firstOccurrenceIso: '2026-08-30T12:00:00.000Z' },
  { expression: '0 0 12 ? SEP 7L', firstOccurrenceIso: '2026-09-26T12:00:00.000Z' },
  { expression: '0 0 12 L-3 OCT ?', firstOccurrenceIso: '2026-10-28T12:00:00.000Z' },
  { expression: '0 0 12 L-5 NOV ?', firstOccurrenceIso: '2026-11-25T12:00:00.000Z' },
  { expression: '0 0 12 15W DEC ?', firstOccurrenceIso: '2026-12-15T12:00:00.000Z' },
  { expression: '0 0 12 ? DEC MON#4', firstOccurrenceIso: '2026-12-28T12:00:00.000Z' },
  { expression: '0 0 12 ? * TUE#1', firstOccurrenceIso: '2026-01-06T12:00:00.000Z' },
  { expression: '0 0 12 ? * WED#2', firstOccurrenceIso: '2026-01-14T12:00:00.000Z' },
  { expression: '0 0 12 ? * THU#3', firstOccurrenceIso: '2026-01-15T12:00:00.000Z' },
  { expression: '0 0 12 ? * SAT#4', firstOccurrenceIso: '2026-01-24T12:00:00.000Z' },
  { expression: '0 0 12 ? * SUN#1', firstOccurrenceIso: '2026-01-04T12:00:00.000Z' },
  { expression: '0 0 12 ? * SUN#5', firstOccurrenceIso: '2026-03-29T12:00:00.000Z' },
  { expression: '0 0 12 ? * MON-FRI/2', firstOccurrenceIso: '2026-01-02T12:00:00.000Z' },
  { expression: '0 0 12 ? JAN-MAR MON#1', firstOccurrenceIso: '2026-01-05T12:00:00.000Z' },
  { expression: '0 0 12 1,15,L * ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 1-5,10-15 * ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 */3 * ?', firstOccurrenceIso: '2026-01-01T12:00:00.000Z' },
  { expression: '0 0 12 ? * MON#1,FRI#2', firstOccurrenceIso: '2026-01-05T12:00:00.000Z' },
];

const validWithoutExactFirstOccurrence = ['0 0 12 ? DEC MON#5'];

const invalidExpressions = [
  '0 0 0 30 FEB ?',
  '0 0 0 31 APR ?',
  '0 60 10 * * ?',
  '0 0 24 * * ?',
  '0 0 -1 * * ?',
  '0 0 12 32 * ?',
  '0 0 12 ? * MON#0',
  '0 0 12 ? * MON#6',
  '0 0 12 ? * 8L',
  '0 0 12 L-40 * ?',
  '0 0 12 ? * MONL',
  '0 0 12 0 * ?',
  '0 0 12 ? * */2',
  '0 0 12 10 * MON',
  '0 0 12 ? ? MON',
  '0 0 12 L * MON',
];

describe('Quartz cron edge cases', () => {
  it('keeps the UI validator and standalone validator aligned for valid expressions', () => {
    const { cron, isValidateCron, error } = useCron();

    [
      ...exactOccurrenceCases.map(item => item.expression),
      ...validWithoutExactFirstOccurrence,
    ].forEach(expression => {
      cron.value = expression;
      expect(isValidateCron.value, expression).toBe(true);
      expect(error.value, expression).toBe('');
      expect(validateCronExpression(expression).isValid, expression).toBe(true);
    });
  });

  it('rejects invalid expressions in both validators', () => {
    const { cron, isValidateCron } = useCron();

    invalidExpressions.forEach(expression => {
      cron.value = expression;
      expect(isValidateCron.value, expression).toBe(false);
      expect(validateCronExpression(expression).isValid, expression).toBe(false);
    });
  });

  it('generates the expected first UTC occurrence for supported edge cases', () => {
    exactOccurrenceCases.forEach(({ expression, firstOccurrenceIso }) => {
      const result = generateOccurrences(expression, start, end, 'UTC', 128);
      expect(result.success, expression).toBe(true);
      expect(result.data?.length, expression).toBeGreaterThan(0);
      expect(result.data?.[0]?.utcDate.toISOString(), expression).toBe(firstOccurrenceIso);
    });
  }, 30000);

  it('finds future occurrences for valid expressions whose first fire is later in the horizon', () => {
    validWithoutExactFirstOccurrence.forEach(expression => {
      const result = generateOccurrences(expression, start, end, 'UTC', 512);
      expect(result.success, expression).toBe(true);
      expect(result.data?.length, expression).toBeGreaterThan(0);
    });
  });
});
