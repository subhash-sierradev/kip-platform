/* eslint-disable simple-import-sort/imports */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock timezone to be deterministic
// Asia/Kolkata is UTC+5:30, so times are converted: local time - 5:30 = UTC
vi.mock('@/utils/timezoneUtils', async () => {
  const actual = await vi.importActual('@/utils/timezoneUtils');
  return {
    ...actual,
    getUserTimezone: () => 'Asia/Kolkata',
    // Mock conversion for deterministic testing across all environments
    // Simulates Asia/Kolkata (UTC+5:30) to UTC conversion
    convertUserTimezoneToUtc: (localTimeStr: string) => {
      const [hhStr, mmStr, ssStr] = localTimeStr.split(':');
      const hours = parseInt(hhStr || '0', 10);
      const minutes = parseInt(mmStr || '0', 10);
      const seconds = parseInt(ssStr || '0', 10);

      // Convert IST (UTC+5:30) to UTC by subtracting 5 hours 30 minutes
      const totalMinutes = hours * 60 + minutes - 330; // 330 = 5*60 + 30

      // Handle negative values with proper modulo
      const utcHours = ((Math.floor(totalMinutes / 60) % 24) + 24) % 24;
      const utcMinutes = ((totalMinutes % 60) + 60) % 60;

      return `${String(utcHours).padStart(2, '0')}:${String(utcMinutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    },
  };
});

import { buildIntegrationRequest, buildConnectionRequest } from '@/utils/arcgisIntegrationMapping';

describe('arcgisIntegrationMapping', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2025-02-05T10:30:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('buildConnectionRequest', () => {
    it('builds connection with BASIC_AUTH credentials', () => {
      const form: any = {
        connectionName: 'My ArcGIS Connection',
        credentialType: 'BASIC_AUTH',
        username: 'testuser',
        password: 'testpass',
        arcgisEndpoint: 'https://arcgis.example.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.name).toBe('My ArcGIS Connection');
      expect(req.serviceType).toBe('ARCGIS');
      expect(req.integrationSecret.baseUrl).toBe('https://arcgis.example.com');
      expect(req.integrationSecret.authType).toBe('BASIC_AUTH');
      expect(req.integrationSecret.credentials).toEqual({
        authType: 'BASIC_AUTH',
        username: 'testuser',
        password: 'testpass',
      });
    });

    it('builds connection with USERNAME_PASSWORD variant (maps to BASIC_AUTH)', () => {
      const form: any = {
        connectionName: 'Test Connection',
        credentialType: 'USERNAME_PASSWORD',
        username: 'user1',
        password: 'pass1',
        arcgisEndpoint: 'https://server.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.integrationSecret.authType).toBe('USERNAME_PASSWORD');
      expect(req.integrationSecret.credentials).toEqual({
        authType: 'BASIC_AUTH',
        username: 'user1',
        password: 'pass1',
      });
    });

    it('builds connection with OAUTH2 credentials', () => {
      const form: any = {
        connectionName: 'OAuth Connection',
        credentialType: 'OAUTH2',
        clientId: 'client123',
        clientSecret: 'secret456',
        tokenUrl: 'https://oauth.example.com/token',
        scope: 'read write',
        arcgisEndpoint: 'https://arcgis.oauth.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.name).toBe('OAuth Connection');
      expect(req.integrationSecret.authType).toBe('OAUTH2');
      expect(req.integrationSecret.credentials).toEqual({
        authType: 'OAUTH2',
        clientId: 'client123',
        clientSecret: 'secret456',
        tokenUrl: 'https://oauth.example.com/token',
        scope: 'read write',
      });
    });

    it('builds connection with OAUTH variant (normalized to OAUTH2)', () => {
      const form: any = {
        connectionName: 'OAuth Alt',
        credentialType: 'OAUTH',
        clientId: 'cli',
        clientSecret: 'sec',
        tokenUrl: 'https://token.url',
        scope: 'api',
        arcgisEndpoint: 'https://arcgis.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.integrationSecret.authType).toBe('OAUTH');
      expect(req.integrationSecret.credentials).toEqual({
        authType: 'OAUTH2',
        clientId: 'cli',
        clientSecret: 'sec',
        tokenUrl: 'https://token.url',
        scope: 'api',
      });
    });

    it('defaults to BASIC_AUTH when credentialType is undefined', () => {
      const form: any = {
        connectionName: 'Default Auth',
        credentialType: undefined,
        username: 'user',
        password: 'pass',
        arcgisEndpoint: 'https://arcgis.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.integrationSecret.authType).toBe('BASIC_AUTH'); // Defaults to BASIC_AUTH
      expect(req.integrationSecret.credentials).toEqual({
        authType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
      });
    });

    it('defaults to BASIC_AUTH for unknown credential types', () => {
      const form: any = {
        connectionName: 'Unknown Type',
        credentialType: 'SOME_UNKNOWN_TYPE',
        username: 'user',
        password: 'pass',
        arcgisEndpoint: 'https://arcgis.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.integrationSecret.authType).toBe('SOME_UNKNOWN_TYPE');
      expect(req.integrationSecret.credentials.authType).toBe('BASIC_AUTH');
    });

    it('handles missing connection name with default', () => {
      const form: any = {
        connectionName: '',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        arcgisEndpoint: 'https://arcgis.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.name).toBe('Untitled Connection');
    });

    it('trims whitespace from connection name', () => {
      const form: any = {
        connectionName: '  Trimmed Name  ',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        arcgisEndpoint: 'https://arcgis.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.name).toBe('Trimmed Name');
    });

    it('handles missing credential fields with empty strings for BASIC_AUTH', () => {
      const form: any = {
        connectionName: 'Empty Creds',
        credentialType: 'BASIC_AUTH',
        arcgisEndpoint: 'https://arcgis.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.integrationSecret.credentials).toEqual({
        authType: 'BASIC_AUTH',
        username: '',
        password: '',
      });
    });

    it('handles missing credential fields with empty strings for OAUTH2', () => {
      const form: any = {
        connectionName: 'Empty OAuth',
        credentialType: 'OAUTH2',
        arcgisEndpoint: 'https://arcgis.com',
      };

      const req = buildConnectionRequest(form);
      expect(req.integrationSecret.credentials).toEqual({
        authType: 'OAUTH2',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
      });
    });
  });

  describe('buildIntegrationRequest - schedules', () => {
    it('builds MONTHLY schedule with month names and normalized time', () => {
      const form: any = {
        name: 'Monthly Integration',
        description: 'Desc',
        itemType: 'DOCUMENT',
        subType: 'Subtype',
        dynamicDocument: undefined,
        executionDate: '2025-12-19',
        executionTime: '12:00',
        frequencyPattern: 'MONTHLY',
        selectedMonths: [1, 6, 12],
        isExecuteOnMonthEnd: false,
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-1');
      const schedule = req.schedule;
      expect(schedule.frequencyPattern).toBe('MONTHLY');
      expect(schedule.executionDate).toBe('2025-12-19');
      expect(schedule.executionTime).toBe('06:30:00'); // 12:00 IST → 06:30 UTC
      expect(schedule.monthSchedule).toEqual(['JANUARY', 'JUNE', 'DECEMBER']);
      expect(schedule.daySchedule).toBeUndefined();
      expect(schedule.isExecuteOnMonthEnd).toBe(false);
      expect(schedule.businessTimeZone).toBe('Asia/Kolkata');
    });

    it('builds MONTHLY schedule with null executionDate when month-end is enabled', () => {
      const form: any = {
        name: 'Monthly Month-End Integration',
        description: 'Desc',
        itemType: 'DOCUMENT',
        subType: 'Subtype',
        dynamicDocument: undefined,
        executionDate: '2025-12-19',
        executionTime: '12:00',
        frequencyPattern: 'MONTHLY',
        selectedMonths: [1, 6, 12],
        isExecuteOnMonthEnd: true,
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-1');
      const schedule = req.schedule;
      expect(schedule.frequencyPattern).toBe('MONTHLY');
      expect(schedule.executionDate).toBeNull();
      expect(schedule.executionTime).toBe('06:30:00'); // 12:00 IST → 06:30 UTC
      expect(schedule.monthSchedule).toEqual(['JANUARY', 'JUNE', 'DECEMBER']);
      expect(schedule.daySchedule).toBeUndefined();
      expect(schedule.isExecuteOnMonthEnd).toBe(true);
      expect(schedule.businessTimeZone).toBe('Asia/Kolkata');
    });

    it('builds DAILY schedule with default 24-hour interval when not provided', () => {
      const form: any = {
        name: 'Daily Integration',
        description: 'Desc',
        itemType: 'DOCUMENT',
        subType: 'Subtype',
        executionDate: '2025-01-01',
        executionTime: '08:15',
        frequencyPattern: 'DAILY',
        dailyFrequency: '',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-2');
      const schedule = req.schedule;
      expect(schedule.frequencyPattern).toBe('DAILY');
      expect(schedule.executionDate).toBe('2025-01-01');
      expect(schedule.executionTime).toBe('02:45:00'); // 08:15 IST → 02:45 UTC
      expect(schedule.dailyExecutionInterval).toBe(24);
      expect(schedule.daySchedule).toBeUndefined();
      expect(schedule.monthSchedule).toBeUndefined();
      expect(schedule.businessTimeZone).toBe('Asia/Kolkata');
    });

    it('builds WEEKLY schedule with selected days and no months', () => {
      const form: any = {
        name: 'Weekly Integration',
        description: 'Desc',
        itemType: 'DOCUMENT',
        subType: 'Subtype',
        executionDate: '2025-06-01',
        executionTime: '09:00',
        frequencyPattern: 'WEEKLY',
        selectedDays: ['MON', 'WED'],
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-3');
      const schedule = req.schedule;
      expect(schedule.frequencyPattern).toBe('WEEKLY');
      expect(schedule.executionDate).toBe('2025-06-01');
      expect(schedule.executionTime).toBe('03:30:00'); // 09:00 IST → 03:30 UTC
      expect(schedule.daySchedule).toEqual(['MON', 'WED']);
      expect(schedule.monthSchedule).toBeUndefined();
      expect(schedule.businessTimeZone).toBe('Asia/Kolkata');
    });

    it('builds CUSTOM schedule passing through cronExpression', () => {
      const form: any = {
        name: 'Custom Integration',
        description: 'Desc',
        itemType: 'DOCUMENT',
        subType: 'Subtype',
        executionDate: '2025-07-04',
        executionTime: '10:00',
        frequencyPattern: 'CUSTOM',
        cronExpression: '0 0 9 * * ?',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-4');
      const schedule = req.schedule;
      expect(schedule.frequencyPattern).toBe('CUSTOM');
      expect(schedule.executionDate).toBe('2025-07-04');
      expect(schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(schedule.cronExpression).toBe('0 0 9 * * ?');
      expect(schedule.daySchedule).toBeUndefined();
      expect(schedule.monthSchedule).toBeUndefined();
      expect(schedule.businessTimeZone).toBe('Asia/Kolkata');
    });

    it('builds schedule with ONCE pattern (not normalized, defaults to DAILY)', () => {
      const form: any = {
        name: 'Once Integration',
        description: 'Run once',
        itemType: 'DOCUMENT',
        subType: 'Subtype',
        executionDate: '2025-12-25',
        executionTime: '14:30',
        frequencyPattern: 'ONCE',
        isExecuteOnMonthEnd: true,
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-5');
      const schedule = req.schedule;
      // ONCE is not in normalizePattern switch, defaults to DAILY
      expect(schedule.frequencyPattern).toBe('DAILY');
      expect(schedule.executionDate).toBe('2025-12-25');
      expect(schedule.executionTime).toBe('09:00:00'); // 14:30 IST → 09:00 UTC
      expect(schedule.isExecuteOnMonthEnd).toBe(true);
      expect(schedule.dailyExecutionInterval).toBe(24); // DAILY default
    });

    it('builds schedule with missing executionTime defaults to 09:00:00', () => {
      const form: any = {
        name: 'Missing Time',
        description: 'No time',
        itemType: 'DOCUMENT',
        executionDate: '2025-06-01',
        frequencyPattern: 'DAILY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-6');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('03:30:00'); // Default 09:00 IST → 03:30 UTC
    });

    it('builds DAILY schedule with specific dailyFrequency interval', () => {
      const form: any = {
        name: 'Daily 12h',
        description: 'Every 12 hours',
        itemType: 'DOCUMENT',
        executionDate: '2025-03-01',
        executionTime: '06:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '12',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-8');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('00:30:00'); // 06:00 IST → 00:30 UTC
      expect(schedule.dailyExecutionInterval).toBe(12);
    });

    it('builds DAILY schedule with 0 dailyFrequency (valid edge case)', () => {
      const form: any = {
        name: 'Daily 0',
        description: 'Zero interval',
        itemType: 'DOCUMENT',
        executionDate: '2025-03-01',
        executionTime: '06:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '0',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-9');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('00:30:00'); // 06:00 IST → 00:30 UTC
      expect(schedule.dailyExecutionInterval).toBe(0);
    });

    it('builds DAILY schedule with invalid dailyFrequency defaults to 24', () => {
      const form: any = {
        name: 'Daily Invalid',
        description: 'Invalid interval',
        itemType: 'DOCUMENT',
        executionDate: '2025-03-01',
        executionTime: '06:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: 'not-a-number',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-10');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('00:30:00'); // 06:00 IST → 00:30 UTC
      expect(schedule.dailyExecutionInterval).toBe(24);
    });

    it('builds WEEKLY schedule with empty selectedDays array', () => {
      const form: any = {
        name: 'Weekly No Days',
        description: 'No days selected',
        itemType: 'DOCUMENT',
        executionDate: '2025-05-01',
        executionTime: '08:00',
        frequencyPattern: 'WEEKLY',
        selectedDays: [],
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-11');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('02:30:00'); // 08:00 IST → 02:30 UTC
      expect(schedule.daySchedule).toBeUndefined(); // Empty array becomes undefined
    });

    it('builds WEEKLY schedule with no selectedDays field', () => {
      const form: any = {
        name: 'Weekly Missing Days',
        description: 'Days field missing',
        itemType: 'DOCUMENT',
        executionDate: '2025-05-01',
        executionTime: '08:00',
        frequencyPattern: 'WEEKLY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-12');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('02:30:00'); // 08:00 IST → 02:30 UTC
      expect(schedule.daySchedule).toBeUndefined();
    });

    it('builds MONTHLY schedule with empty selectedMonths array', () => {
      const form: any = {
        name: 'Monthly No Months',
        description: 'No months selected',
        itemType: 'DOCUMENT',
        executionDate: '2025-06-01',
        executionTime: '10:00',
        frequencyPattern: 'MONTHLY',
        selectedMonths: [],
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-13');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(schedule.monthSchedule).toEqual([]); // Empty array
    });

    it('builds MONTHLY schedule with no selectedMonths field', () => {
      const form: any = {
        name: 'Monthly Missing Months',
        description: 'Months field missing',
        itemType: 'DOCUMENT',
        executionDate: '2025-06-01',
        executionTime: '10:00',
        frequencyPattern: 'MONTHLY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-14');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(schedule.monthSchedule).toEqual([]); // Empty array
    });

    it('converts month numbers to names correctly (boundary cases)', () => {
      const form: any = {
        name: 'Monthly All Months',
        description: 'All 12 months',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '00:00',
        frequencyPattern: 'MONTHLY',
        selectedMonths: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-15');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('18:30:00'); // 00:00 IST → 18:30 UTC (previous day)
      expect(schedule.monthSchedule).toEqual([
        'JANUARY',
        'FEBRUARY',
        'MARCH',
        'APRIL',
        'MAY',
        'JUNE',
        'JULY',
        'AUGUST',
        'SEPTEMBER',
        'OCTOBER',
        'NOVEMBER',
        'DECEMBER',
      ]);
    });

    it('handles invalid month numbers (falls back to JANUARY)', () => {
      const form: any = {
        name: 'Monthly Invalid',
        description: 'Invalid months',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '00:00',
        frequencyPattern: 'MONTHLY',
        selectedMonths: [0, 13, -1, 15],
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-16');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('18:30:00'); // 00:00 IST → 18:30 UTC
      expect(schedule.monthSchedule).toEqual(['JANUARY', 'JANUARY', 'JANUARY', 'JANUARY']);
    });

    it('builds CUSTOM schedule with missing cronExpression', () => {
      const form: any = {
        name: 'Custom No Cron',
        description: 'No cron provided',
        itemType: 'DOCUMENT',
        executionDate: '2025-08-01',
        executionTime: '12:00',
        frequencyPattern: 'CUSTOM',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-17');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('06:30:00'); // 12:00 IST → 06:30 UTC
      expect(schedule.cronExpression).toBeUndefined();
    });

    it('normalizes lowercase frequencyPattern to uppercase', () => {
      const form: any = {
        name: 'Lowercase Pattern',
        description: 'Lowercase weekly',
        itemType: 'DOCUMENT',
        executionDate: '2025-09-01',
        executionTime: '15:00',
        frequencyPattern: 'weekly',
        selectedDays: ['TUE'],
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-18');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('09:30:00'); // 15:00 IST → 09:30 UTC
      expect(schedule.frequencyPattern).toBe('WEEKLY');
    });

    it('defaults unknown frequencyPattern to DAILY', () => {
      const form: any = {
        name: 'Unknown Pattern',
        description: 'Unknown frequency',
        itemType: 'DOCUMENT',
        executionDate: '2025-10-01',
        executionTime: '16:00',
        frequencyPattern: 'UNKNOWN',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-19');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('10:30:00'); // 16:00 IST → 10:30 UTC
      expect(schedule.frequencyPattern).toBe('DAILY');
      expect(schedule.dailyExecutionInterval).toBe(24);
    });

    it('defaults missing frequencyPattern to DAILY', () => {
      const form: any = {
        name: 'No Pattern',
        description: 'Missing frequency',
        itemType: 'DOCUMENT',
        executionDate: '2025-11-01',
        executionTime: '17:00',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-20');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('11:30:00'); // 17:00 IST → 11:30 UTC
      expect(schedule.frequencyPattern).toBe('DAILY');
    });

    it('normalizes HH:mm time format to HH:mm:ss', () => {
      const form: any = {
        name: 'Time HHmm',
        description: 'Two-part time',
        itemType: 'DOCUMENT',
        executionDate: '2025-12-01',
        executionTime: '18:45',
        frequencyPattern: 'DAILY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-21');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('13:15:00'); // 18:45 IST → 13:15 UTC
    });

    it('preserves HH:mm:ss time format', () => {
      const form: any = {
        name: 'Time HHmmss',
        description: 'Three-part time',
        itemType: 'DOCUMENT',
        executionDate: '2025-12-01',
        executionTime: '18:45:30',
        frequencyPattern: 'DAILY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-22');
      const schedule = req.schedule;
      expect(schedule.executionTime).toBe('13:15:30'); // 18:45:30 IST → 13:15:30 UTC
    });
  });

  describe('buildIntegrationRequest - field mappings', () => {
    it('filters out field mappings with empty sourceField', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          { sourceField: '', targetField: 'target1', transformationType: 'PASSTHROUGH' },
          { sourceField: 'source2', targetField: 'target2', transformationType: 'PASSTHROUGH' },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-23');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings).toHaveLength(1);
      expect(req.fieldMappings[0].sourceFieldPath).toBe('source2');
    });

    it('filters out field mappings with whitespace-only sourceField', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          { sourceField: '   ', targetField: 'target1', transformationType: 'PASSTHROUGH' },
          { sourceField: 'source2', targetField: 'target2', transformationType: 'PASSTHROUGH' },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-24');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings).toHaveLength(1);
    });

    it('filters out field mappings with empty targetField', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          { sourceField: 'source1', targetField: '', transformationType: 'PASSTHROUGH' },
          { sourceField: 'source2', targetField: 'target2', transformationType: 'PASSTHROUGH' },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-25');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings).toHaveLength(1);
      expect(req.fieldMappings[0].targetFieldPath).toBe('target2');
    });

    it('filters out field mappings with non-string sourceField', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          { sourceField: null, targetField: 'target1', transformationType: 'PASSTHROUGH' },
          { sourceField: 123, targetField: 'target2', transformationType: 'PASSTHROUGH' },
          { sourceField: 'source3', targetField: 'target3', transformationType: 'PASSTHROUGH' },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-26');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings).toHaveLength(1);
      expect(req.fieldMappings[0].sourceFieldPath).toBe('source3');
    });

    it('filters out field mappings with non-string targetField', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          { sourceField: 'source1', targetField: null, transformationType: 'PASSTHROUGH' },
          { sourceField: 'source2', targetField: undefined, transformationType: 'PASSTHROUGH' },
          { sourceField: 'source3', targetField: 'target3', transformationType: 'PASSTHROUGH' },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-27');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings).toHaveLength(1);
    });

    it('trims whitespace from sourceField and targetField', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          {
            sourceField: '  source1  ',
            targetField: '  target1  ',
            transformationType: 'PASSTHROUGH',
          },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-28');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings[0].sourceFieldPath).toBe('source1');
      expect(req.fieldMappings[0].targetFieldPath).toBe('target1');
    });

    it('includes real UUID id but excludes frontend-generated id', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          { id: 'field-123', sourceField: 'source1', targetField: 'target1' },
          {
            id: '550e8400-e29b-41d4-a716-446655440000',
            sourceField: 'source2',
            targetField: 'target2',
          },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-29');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings[0].id).toBeUndefined(); // Frontend ID excluded
      expect(req.fieldMappings[1].id).toBe('550e8400-e29b-41d4-a716-446655440000'); // Real UUID included
    });

    it('sets displayOrder from field or uses index', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          { sourceField: 'source1', targetField: 'target1', displayOrder: 5 },
          { sourceField: 'source2', targetField: 'target2' },
          { sourceField: 'source3', targetField: 'target3', displayOrder: 0 },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-30');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings[0].displayOrder).toBe(5);
      expect(req.fieldMappings[1].displayOrder).toBe(1); // Index used
      expect(req.fieldMappings[2].displayOrder).toBe(0); // Explicit 0 preserved
    });

    it('sets isMandatory to boolean', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          { sourceField: 'source1', targetField: 'target1', isMandatory: true },
          { sourceField: 'source2', targetField: 'target2', isMandatory: false },
          { sourceField: 'source3', targetField: 'target3' },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-31');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings[0].isMandatory).toBe(true);
      expect(req.fieldMappings[1].isMandatory).toBe(false);
      expect(req.fieldMappings[2].isMandatory).toBe(false); // Falsy becomes false
    });

    it('defaults transformationType to PASSTHROUGH', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [{ sourceField: 'source1', targetField: 'target1' }],
      };

      const req = buildIntegrationRequest(form, 'conn-32');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings[0].transformationType).toBe('PASSTHROUGH');
    });

    it('preserves non-PASSTHROUGH transformationType', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [
          { sourceField: 'source1', targetField: 'target1', transformationType: 'UPPERCASE' },
        ],
      };

      const req = buildIntegrationRequest(form, 'conn-33');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings[0].transformationType).toBe('UPPERCASE');
    });

    it('sets transformationConfig and defaultValue to undefined', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [{ sourceField: 'source1', targetField: 'target1' }],
      };

      const req = buildIntegrationRequest(form, 'conn-34');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings[0].transformationConfig).toBeUndefined();
      expect(req.fieldMappings[0].defaultValue).toBeUndefined();
    });

    it('handles empty fieldMappings array', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-35');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings).toEqual([]);
    });

    it('handles undefined fieldMappings', () => {
      const form: any = {
        name: 'Integration',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
      };

      const req = buildIntegrationRequest(form, 'conn-36');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.fieldMappings).toEqual([]);
    });
  });

  describe('buildIntegrationRequest - integration fields', () => {
    it('sets name and description', () => {
      const form: any = {
        name: 'Test Integration',
        description: 'Test Description',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-37');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.name).toBe('Test Integration');
      expect(req.description).toBe('Test Description');
    });

    it('sets itemType and itemSubtype', () => {
      const form: any = {
        name: 'Test',
        description: 'Test',
        itemType: 'FORM',
        subType: 'CUSTOM_SUBTYPE',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-38');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.itemType).toBe('FORM');
      expect(req.itemSubtype).toBe('CUSTOM_SUBTYPE');
    });

    it('sets dynamicDocumentType to undefined when empty', () => {
      const form: any = {
        name: 'Test',
        description: 'Test',
        itemType: 'DOCUMENT',
        dynamicDocument: '',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-39');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.dynamicDocumentType).toBeUndefined();
    });

    it('sets dynamicDocumentType when provided', () => {
      const form: any = {
        name: 'Test',
        description: 'Test',
        itemType: 'DOCUMENT',
        dynamicDocument: 'CustomDocument',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-40');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.dynamicDocumentType).toBe('CustomDocument');
    });

    it('sets connectionId from argument', () => {
      const form: any = {
        name: 'Test',
        description: 'Test',
        itemType: 'DOCUMENT',
        executionDate: '2025-01-01',
        executionTime: '10:00',
        frequencyPattern: 'DAILY',
        fieldMappings: [],
      };

      const req = buildIntegrationRequest(form, 'conn-41');
      expect(req.schedule.executionTime).toBe('04:30:00'); // 10:00 IST → 04:30 UTC
      expect(req.connectionId).toBe('conn-41');
    });
  });
});
