/* eslint-disable simple-import-sort/imports */
import { describe, it, expect } from 'vitest';
import { validateCronExpression, describeCronExpression } from '@/utils/cronValidation';

describe('Cron Validation Utility', () => {
  describe('validateCronExpression', () => {
    it('validates correct cron expressions', () => {
      const validExpressions = [
        '0 0 9 * * ?', // Daily at 9 AM
        '0 */5 * * * ?', // Every 5 minutes
        '0 0 14 ? * MON-FRI', // Weekdays at 2 PM
        '0 0 9 1 * ?', // Monthly on 1st at 9 AM
        '0 30 8 ? * MON', // Mondays at 8:30 AM
        '0 0 */4 * * ?', // Every 4 hours
      ];

      validExpressions.forEach(expr => {
        const result = validateCronExpression(expr);
        expect(result.isValid).toBe(true);
        expect(result.error).toBeUndefined();
      });
    });

    it('rejects invalid cron expressions', () => {
      const invalidExpressions = [
        '', // Empty
        '0 0 9', // Too few parts
        '0 0 9 * * ? *', // Too many parts
        '0 60 9 * * ?', // Invalid minute (60)
        '0 0 25 * * ?', // Invalid hour (25)
        '0 0 9 32 * ?', // Invalid day (32)
        '0 0 9 * 13 ?', // Invalid month (13)
        '0 0 9 ? * 8', // Invalid day of week (8)
        'invalid cron', // Non-numeric
      ];

      invalidExpressions.forEach(expr => {
        const result = validateCronExpression(expr);
        expect(result.isValid).toBe(false);
        expect(result.error).toBeDefined();
      });
    });

    it('validates day/dayOfWeek mutual exclusivity', () => {
      // Both day and dayOfWeek specified as ?
      let result = validateCronExpression('0 0 9 ? * ?');
      expect(result.isValid).toBe(false);
      expect(result.error).toContain('Either day or day-of-week must be specified');

      // Both day and dayOfWeek specified with values
      result = validateCronExpression('0 0 9 15 * MON');
      expect(result.isValid).toBe(false);
      expect(result.error).toContain('Either day or day-of-week should be ?');

      // Correct usage - day specified, dayOfWeek is ?
      result = validateCronExpression('0 0 9 15 * ?');
      expect(result.isValid).toBe(true);

      // Correct usage - dayOfWeek specified, day is ?
      result = validateCronExpression('0 0 9 ? * MON');
      expect(result.isValid).toBe(true);
    });

    it('validates ranges and lists', () => {
      // Valid ranges
      expect(validateCronExpression('0 0-30 9 * * ?').isValid).toBe(true);
      expect(validateCronExpression('0 0 9-17 * * ?').isValid).toBe(true);

      // Valid lists
      expect(validateCronExpression('0 0 9,12,15 * * ?').isValid).toBe(true);
      expect(validateCronExpression('0 0 9 ? * MON,WED,FRI').isValid).toBe(true);

      // Invalid ranges
      expect(validateCronExpression('0 0-70 9 * * ?').isValid).toBe(false);
      expect(validateCronExpression('0 30-10 9 * * ?').isValid).toBe(false);
    });

    it('validates step values', () => {
      // Valid step values
      expect(validateCronExpression('0 */5 * * * ?').isValid).toBe(true);
      expect(validateCronExpression('0 0 */4 * * ?').isValid).toBe(true);
      expect(validateCronExpression('0 0-30/10 9 * * ?').isValid).toBe(true);

      // Invalid step values
      expect(validateCronExpression('0 */0 * * * ?').isValid).toBe(false);
      expect(validateCronExpression('0 */-5 * * * ?').isValid).toBe(false);
    });
  });

  describe('describeCronExpression', () => {
    it('describes common cron patterns', () => {
      const patterns = [
        { cron: '0 * * * * ?', description: 'Every minute' },
        { cron: '0 */5 * * * ?', description: 'Every 5 minutes' },
        { cron: '0 0 * * * ?', description: 'Every hour' },
        { cron: '0 0 9 * * ?', description: 'Daily at 9:00 AM' },
        { cron: '0 0 9 ? * MON-FRI', description: 'Weekdays at 9:00 AM' },
        { cron: '0 0 9 1 * ?', description: 'Monthly on the 1st at 9:00 AM' },
      ];

      patterns.forEach(({ cron, description }) => {
        expect(describeCronExpression(cron)).toBe(description);
      });
    });

    it('returns null for uncommon patterns', () => {
      expect(describeCronExpression('0 0 9 15 * ?')).toBeNull();
      expect(describeCronExpression('0 30 14 ? * TUE')).toBeNull();
    });

    it('handles whitespace in expressions', () => {
      expect(describeCronExpression('  0 0 9 * * ?  ')).toBe('Daily at 9:00 AM');
    });
  });
});
