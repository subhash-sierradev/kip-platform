import { describe, expect, it } from 'vitest';

import { getScheduleTextForCustomPattern } from '@/utils/cronTextFormatter';

describe('cronTextFormatter', () => {
  describe('getScheduleTextForCustomPattern', () => {
    it('should format cron expression text for CUSTOM patterns', () => {
      const scheduleData = {
        cronExpression: '0 20 15/2 * * ?',
      } as any;
      const result = getScheduleTextForCustomPattern(scheduleData);
      expect(result).toBeDefined();
      expect(result).not.toBe('');
    });

    it('should return default text when cron expression is missing', () => {
      const scheduleData = {} as any;
      const result = getScheduleTextForCustomPattern(scheduleData);
      expect(result).toBe('Custom schedule');
    });

    it('should handle various Quartz cron expressions', () => {
      const testCases = [
        { cronExpression: '0 46 14/1 * * ?', expected: /every.*hour/i },
        { cronExpression: '0 30 11 ? * THU,FRI', expected: /THU.*FRI/i },
        { cronExpression: '0 */5 * ? * *', expected: /5 minutes/i },
        { cronExpression: '0 0 11 L 5,3 ?', expected: /last day|Custom schedule/i },
      ];

      testCases.forEach(({ cronExpression, expected }) => {
        const scheduleData = { cronExpression } as any;
        const result = getScheduleTextForCustomPattern(scheduleData);
        expect(result).toMatch(expected);
      });
    });
  });
});
