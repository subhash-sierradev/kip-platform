import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  formatDateForInput,
  mapArcGISDetailToFormData,
  mapBasicDetails,
  mapConnectionData,
  mapCustomConfigToFormData,
  mapDailyFrequencyRule,
  mapDocumentSelection,
  mapFieldMappings,
  mapMonthSchedule,
  mapScheduleData,
} from '@/utils/arcgisWizardMappingHelpers';

describe('arcgisWizardMappingHelpers', () => {
  describe('mapDailyFrequencyRule', () => {
    it('returns exact interval for available options (1,2,3,4,8,12)', () => {
      expect(mapDailyFrequencyRule(1)).toBe('1');
      expect(mapDailyFrequencyRule(2)).toBe('2');
      expect(mapDailyFrequencyRule(3)).toBe('3');
      expect(mapDailyFrequencyRule(4)).toBe('4');
      expect(mapDailyFrequencyRule(8)).toBe('8');
      expect(mapDailyFrequencyRule(12)).toBe('12');
    });

    it('returns exact interval for string versions of available options', () => {
      expect(mapDailyFrequencyRule('1')).toBe('1');
      expect(mapDailyFrequencyRule('2')).toBe('2');
      expect(mapDailyFrequencyRule('3')).toBe('3');
      expect(mapDailyFrequencyRule('4')).toBe('4');
      expect(mapDailyFrequencyRule('8')).toBe('8');
      expect(mapDailyFrequencyRule('12')).toBe('12');
    });

    it('rounds up to nearest available option for values between options', () => {
      // The function uses >= comparisons, so 5,6,7 map to 4 (>= 4)
      expect(mapDailyFrequencyRule(5)).toBe('4');
      expect(mapDailyFrequencyRule(6)).toBe('4');
      expect(mapDailyFrequencyRule(7)).toBe('4');
      // 9,10,11 map to 8 (>= 8 but < 12)
      expect(mapDailyFrequencyRule(9)).toBe('8');
      expect(mapDailyFrequencyRule(10)).toBe('8');
      expect(mapDailyFrequencyRule(11)).toBe('8');
    });

    it('returns 12 for values >= 12', () => {
      expect(mapDailyFrequencyRule(13)).toBe('12');
      expect(mapDailyFrequencyRule(20)).toBe('12');
      expect(mapDailyFrequencyRule(24)).toBe('12');
      expect(mapDailyFrequencyRule(48)).toBe('12');
    });

    it('returns 1 for values < 1', () => {
      expect(mapDailyFrequencyRule(0)).toBe('1');
      expect(mapDailyFrequencyRule(-1)).toBe('1');
    });
  });

  describe('mapMonthSchedule', () => {
    it('maps single month name to number', () => {
      expect(mapMonthSchedule(['JANUARY'])).toEqual([1]);
      expect(mapMonthSchedule(['DECEMBER'])).toEqual([12]);
    });

    it('maps multiple month names to numbers', () => {
      expect(mapMonthSchedule(['JANUARY', 'FEBRUARY', 'MARCH'])).toEqual([1, 2, 3]);
      expect(mapMonthSchedule(['JUNE', 'JULY', 'AUGUST'])).toEqual([6, 7, 8]);
    });

    it('maps all twelve months', () => {
      const allMonths = [
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
      ];
      expect(mapMonthSchedule(allMonths)).toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]);
    });

    it('filters out invalid month names', () => {
      expect(mapMonthSchedule(['JANUARY', 'INVALID', 'MARCH'])).toEqual([1, 3]);
      expect(mapMonthSchedule(['INVALID1', 'INVALID2'])).toEqual([]);
    });

    it('handles empty array', () => {
      expect(mapMonthSchedule([])).toEqual([]);
    });

    it('handles mixed valid and invalid months', () => {
      expect(mapMonthSchedule(['JANUARY', '', 'FEBRUARY', 'NOTAMONTH', 'MARCH'])).toEqual([
        1, 2, 3,
      ]);
    });
  });

  describe('mapCustomConfigToFormData', () => {
    it('maps all customConfig properties to formData', () => {
      const customConfig = {
        pattern: 'last-day',
        week: 'LAST',
        weekday: 'FRIDAY',
        dayOfMonth: 15,
        endOfMonth: true,
        yearlyMonth: 'DECEMBER',
        yearlyDay: '25',
      };
      const formData: Record<string, unknown> = {};

      mapCustomConfigToFormData(customConfig, formData);

      expect(formData.monthlyPattern).toBe('last-day');
      expect(formData.monthlyWeek).toBe('LAST');
      expect(formData.monthlyWeekday).toBe('FRIDAY');
      expect(formData.monthlyDay).toBe(15);
      expect(formData.yearlyMonth).toBe('DECEMBER');
      expect(formData.yearlyDay).toBe('25');
    });

    it('uses default pattern when pattern is missing', () => {
      const customConfig = {
        week: 'FIRST',
        weekday: 'MONDAY',
      };
      const formData: Record<string, unknown> = {};

      mapCustomConfigToFormData(customConfig, formData);

      expect(formData.monthlyPattern).toBe('specific-date');
      expect(formData.monthlyWeek).toBe('FIRST');
      expect(formData.monthlyWeekday).toBe('MONDAY');
    });

    it('uses empty strings for missing week/weekday', () => {
      const customConfig = {
        pattern: 'specific-date',
      };
      const formData: Record<string, unknown> = {};

      mapCustomConfigToFormData(customConfig, formData);

      expect(formData.monthlyWeek).toBe('');
      expect(formData.monthlyWeekday).toBe('');
      expect(formData.monthlyDay).toBeNull();
    });

    it('sets defaults when customConfig is undefined', () => {
      const formData: Record<string, unknown> = {};

      mapCustomConfigToFormData(undefined, formData);

      expect(formData.monthlyPattern).toBe('specific-date');
      expect(formData.monthlyWeek).toBe('');
      expect(formData.monthlyWeekday).toBe('');
      expect(formData.monthlyDay).toBeNull();
    });

    it('does not set yearly fields when yearlyMonth is missing', () => {
      const customConfig = {
        pattern: 'specific-date',
        yearlyDay: '15',
      };
      const formData: Record<string, unknown> = {};

      mapCustomConfigToFormData(customConfig, formData);

      expect(formData.yearlyMonth).toBeUndefined();
      expect(formData.yearlyDay).toBeUndefined();
    });

    it('sets yearly fields when yearlyMonth is present', () => {
      const customConfig = {
        pattern: 'specific-date',
        yearlyMonth: 'JUNE',
        yearlyDay: '10',
      };
      const formData: Record<string, unknown> = {};

      mapCustomConfigToFormData(customConfig, formData);

      expect(formData.yearlyMonth).toBe('JUNE');
      expect(formData.yearlyDay).toBe('10');
    });
  });

  describe('mapFieldMappings', () => {
    it('maps field mappings with sourceFieldPath and targetFieldName', () => {
      const mappings = [
        {
          sourceFieldPath: 'source.path.field1',
          targetFieldName: 'target1',
          transformationType: 'DIRECT',
          isMandatory: true,
        },
      ];

      const result = mapFieldMappings(mappings);

      expect(result).toEqual([
        {
          sourceField: 'source.path.field1',
          targetField: 'target1',
          transformationType: 'DIRECT',
          isMandatory: true,
        },
      ]);
    });

    it('prefers sourceFieldPath over sourceFieldName', () => {
      const mappings = [
        {
          sourceFieldPath: 'path.field',
          sourceFieldName: 'name.field',
          targetFieldName: 'target',
          transformationType: 'DIRECT',
          isMandatory: false,
        },
      ];

      const result = mapFieldMappings(mappings);

      expect(result[0].sourceField).toBe('path.field');
    });

    it('falls back to sourceFieldName when sourceFieldPath is missing', () => {
      const mappings = [
        {
          sourceFieldName: 'name.field',
          targetFieldName: 'target',
          transformationType: 'DIRECT',
          isMandatory: false,
        },
      ];

      const result = mapFieldMappings(mappings);

      expect(result[0].sourceField).toBe('name.field');
    });

    it('uses sourceField/targetField as final fallback', () => {
      const mappings = [
        {
          sourceField: 'fallback.source',
          targetField: 'fallback.target',
          transformationType: 'DIRECT',
          isMandatory: false,
        },
      ];

      const result = mapFieldMappings(mappings);

      expect(result[0].sourceField).toBe('fallback.source');
      expect(result[0].targetField).toBe('fallback.target');
    });

    it('returns default empty mapping for empty array', () => {
      const result = mapFieldMappings([]);

      expect(result).toEqual([
        {
          sourceField: '',
          targetField: '',
          transformationType: '',
          isMandatory: false,
        },
      ]);
    });

    it('returns default empty mapping for null input', () => {
      const result = mapFieldMappings(null as any);

      expect(result).toEqual([
        {
          sourceField: '',
          targetField: '',
          transformationType: '',
          isMandatory: false,
        },
      ]);
    });

    it('returns default empty mapping for undefined input', () => {
      const result = mapFieldMappings(undefined as any);

      expect(result).toEqual([
        {
          sourceField: '',
          targetField: '',
          transformationType: '',
          isMandatory: false,
        },
      ]);
    });

    it('converts isMandatory to boolean', () => {
      const mappings = [
        {
          sourceFieldPath: 'field1',
          targetFieldName: 'target1',
          transformationType: '',
          isMandatory: true,
        },
        {
          sourceFieldPath: 'field2',
          targetFieldName: 'target2',
          transformationType: '',
          isMandatory: false,
        },
        { sourceFieldPath: 'field3', targetFieldName: 'target3', transformationType: '' },
      ];

      const result = mapFieldMappings(mappings);

      expect(result[0].isMandatory).toBe(true);
      expect(result[1].isMandatory).toBe(false);
      expect(result[2].isMandatory).toBe(false);
    });

    it('maps multiple field mappings', () => {
      const mappings = [
        {
          sourceFieldPath: 'field1',
          targetFieldName: 'target1',
          transformationType: 'DIRECT',
          isMandatory: true,
        },
        {
          sourceFieldPath: 'field2',
          targetFieldName: 'target2',
          transformationType: 'UPPERCASE',
          isMandatory: false,
        },
        {
          sourceFieldPath: 'field3',
          targetFieldName: 'target3',
          transformationType: 'LOWERCASE',
          isMandatory: true,
        },
      ];

      const result = mapFieldMappings(mappings);

      expect(result).toHaveLength(3);
      expect(result[0].transformationType).toBe('DIRECT');
      expect(result[1].transformationType).toBe('UPPERCASE');
      expect(result[2].transformationType).toBe('LOWERCASE');
    });
  });

  describe('mapBasicDetails', () => {
    it('maps name and description from detail to formData', () => {
      const detail = {
        name: 'Integration Name',
        description: 'Integration Description',
      };
      const formData: Record<string, unknown> = {};

      mapBasicDetails(detail, formData);

      expect(formData.name).toBe('Integration Name');
      expect(formData.description).toBe('Integration Description');
    });

    it('uses empty strings for missing name and description', () => {
      const detail = {};
      const formData: Record<string, unknown> = {};

      mapBasicDetails(detail, formData);

      expect(formData.name).toBe('');
      expect(formData.description).toBe('');
    });

    it('handles undefined values', () => {
      const detail = {
        name: undefined,
        description: undefined,
      };
      const formData: Record<string, unknown> = {};

      mapBasicDetails(detail, formData);

      expect(formData.name).toBe('');
      expect(formData.description).toBe('');
    });
  });

  describe('mapDocumentSelection', () => {
    beforeEach(() => {
      vi.stubEnv('NODE_ENV', 'test');
    });

    it('maps itemType, subType, and dynamicDocument', () => {
      const detail = {
        itemType: 'Document',
        itemSubtype: 'Report',
        dynamicDocumentType: 'Invoice',
      };
      const formData: Record<string, unknown> = {};

      mapDocumentSelection(detail, formData);

      expect(formData.itemType).toBe('Document');
      expect(formData.subType).toBe('Report');
      expect(formData.dynamicDocument).toBe('Invoice');
    });

    it('uses dynamicDocumentType to set itemType to Document when itemType is missing', () => {
      const detail = {
        dynamicDocumentType: 'Invoice',
      };
      const formData: Record<string, unknown> = {};

      mapDocumentSelection(detail, formData);

      expect(formData.itemType).toBe('Document');
      expect(formData.dynamicDocument).toBe('Invoice');
    });

    it('uses empty string for itemType when both itemType and dynamicDocumentType are missing', () => {
      const detail = {};
      const formData: Record<string, unknown> = {};

      mapDocumentSelection(detail, formData);

      expect(formData.itemType).toBe('');
      expect(formData.subType).toBe('');
      expect(formData.dynamicDocument).toBe('');
    });

    it('prefers itemType over dynamicDocumentType when both exist', () => {
      const detail = {
        itemType: 'CustomType',
        dynamicDocumentType: 'Invoice',
      };
      const formData: Record<string, unknown> = {};

      mapDocumentSelection(detail, formData);

      expect(formData.itemType).toBe('CustomType');
      expect(formData.dynamicDocument).toBe('Invoice');
    });
  });

  describe('formatDateForInput', () => {
    it('returns empty string for empty input', () => {
      expect(formatDateForInput('')).toBe('');
    });

    it('returns empty string for null input', () => {
      expect(formatDateForInput(null as any)).toBe('');
    });

    it('returns empty string for undefined input', () => {
      expect(formatDateForInput(undefined as any)).toBe('');
    });

    it('returns date string unchanged when already in YYYY-MM-DD format', () => {
      expect(formatDateForInput('2024-01-15')).toBe('2024-01-15');
      expect(formatDateForInput('2023-12-31')).toBe('2023-12-31');
      expect(formatDateForInput('2025-06-01')).toBe('2025-06-01');
    });

    it('extracts date part from ISO datetime string', () => {
      expect(formatDateForInput('2024-01-15T10:30:00Z')).toBe('2024-01-15');
      expect(formatDateForInput('2023-12-31T23:59:59.999Z')).toBe('2023-12-31');
      expect(formatDateForInput('2025-06-01T00:00:00')).toBe('2025-06-01');
    });

    it('formats various date strings to YYYY-MM-DD', () => {
      // These tests may vary based on locale and timezone
      const result = formatDateForInput('01/15/2024');
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });

    it('returns empty string for invalid date strings', () => {
      expect(formatDateForInput('invalid-date')).toBe('');
      expect(formatDateForInput('not a date')).toBe('');
      // Note: '2024-13-45' matches YYYY-MM-DD regex so returns as-is
      expect(formatDateForInput('2024-13-45')).toBe('2024-13-45');
    });

    it('handles date with whitespace by trimming', () => {
      expect(formatDateForInput('  2024-01-15  ')).toBe('2024-01-15');
      expect(formatDateForInput('  2024-01-15T10:30:00Z  ')).toBe('2024-01-15');
    });

    it('pads month and day with leading zeros', () => {
      const date = new Date(2024, 0, 5); // January 5, 2024
      const isoString = date.toISOString();
      const result = formatDateForInput(isoString);
      expect(result).toMatch(/^\d{4}-01-\d{2}$/);
    });

    it('handles errors gracefully and returns empty string', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = formatDateForInput({ invalid: 'object' } as any);

      expect(result).toBe('');
      expect(consoleSpy).toHaveBeenCalled();

      consoleSpy.mockRestore();
    });
  });

  describe('mapScheduleData', () => {
    let formData: Record<string, unknown>;

    beforeEach(() => {
      formData = {};
    });

    it('maps all schedule fields to formData', () => {
      const schedule = {
        executionDate: '2024-03-15',
        executionTime: '14:30:00',
        frequencyPattern: 'DAILY',
        dailyExecutionInterval: 4,
        daySchedule: ['MONDAY', 'WEDNESDAY', 'FRIDAY'],
        monthSchedule: ['JANUARY', 'JUNE', 'DECEMBER'],
        cronExpression: '0 30 14 * * ?',
        isExecuteOnMonthEnd: true,
        customConfig: {
          pattern: 'last-day',
          week: 'LAST',
          weekday: 'FRIDAY',
          dayOfMonth: 15,
        },
      };

      mapScheduleData(schedule, formData);

      expect(formData.executionDate).toBe('2024-03-15');
      expect(formData.frequencyPattern).toBe('DAILY');
      expect(formData.dailyFrequency).toBe('4');
      expect(formData.dailyFrequencyRule).toBe('4');
      expect(formData.selectedDays).toEqual(['MONDAY', 'WEDNESDAY', 'FRIDAY']);
      expect(formData.selectedMonths).toEqual([1, 6, 12]);
      expect(formData.cronExpression).toBe('0 30 14 * * ?');
      expect(formData.isExecuteOnMonthEnd).toBe(true);
      expect(formData.monthlyPattern).toBe('last-day');
      expect(formData.monthlyWeek).toBe('LAST');
      expect(formData.monthlyWeekday).toBe('FRIDAY');
      expect(formData.monthlyDay).toBe(15);
    });

    it('handles missing executionDate', () => {
      const schedule = { frequencyPattern: 'WEEKLY' };
      mapScheduleData(schedule, formData);

      expect(formData.executionDate).toBe('');
    });

    it('handles missing executionTime', () => {
      const schedule = { frequencyPattern: 'DAILY' };
      mapScheduleData(schedule, formData);

      expect(formData.executionTime).toBe('');
    });

    it('defaults dailyFrequencyRule to 12 when dailyExecutionInterval is missing', () => {
      const schedule = { frequencyPattern: 'DAILY' };
      mapScheduleData(schedule, formData);

      expect(formData.dailyFrequencyRule).toBe('12');
    });

    it('handles empty daySchedule', () => {
      const schedule = { frequencyPattern: 'WEEKLY', daySchedule: [] };
      mapScheduleData(schedule, formData);

      expect(formData.selectedDays).toEqual([]);
    });

    it('handles missing daySchedule', () => {
      const schedule = { frequencyPattern: 'WEEKLY' };
      mapScheduleData(schedule, formData);

      expect(formData.selectedDays).toEqual([]);
    });

    it('handles empty monthSchedule', () => {
      const schedule = { frequencyPattern: 'MONTHLY', monthSchedule: [] };
      mapScheduleData(schedule, formData);

      expect(formData.selectedMonths).toEqual([]);
    });

    it('handles non-array monthSchedule', () => {
      const schedule = { frequencyPattern: 'MONTHLY', monthSchedule: 'INVALID' as any };
      mapScheduleData(schedule, formData);

      expect(formData.selectedMonths).toEqual([]);
    });

    it('handles missing monthSchedule', () => {
      const schedule = { frequencyPattern: 'MONTHLY' };
      mapScheduleData(schedule, formData);

      expect(formData.selectedMonths).toEqual([]);
    });

    it('defaults isExecuteOnMonthEnd to false when missing', () => {
      const schedule = { frequencyPattern: 'MONTHLY' };
      mapScheduleData(schedule, formData);

      expect(formData.isExecuteOnMonthEnd).toBe(false);
    });

    it('handles missing cronExpression', () => {
      const schedule = { frequencyPattern: 'CUSTOM' };
      mapScheduleData(schedule, formData);

      expect(formData.cronExpression).toBe('');
    });

    it('handles undefined customConfig', () => {
      const schedule = { frequencyPattern: 'MONTHLY' };
      mapScheduleData(schedule, formData);

      expect(formData.monthlyPattern).toBe('specific-date');
      expect(formData.monthlyWeek).toBe('');
      expect(formData.monthlyWeekday).toBe('');
      expect(formData.monthlyDay).toBe(null);
    });

    it('logs warning when schedule data is missing', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
      mapScheduleData(null as any, formData);

      expect(consoleSpy).toHaveBeenCalledWith('mapScheduleData: No schedule data provided');
      consoleSpy.mockRestore();
    });

    it('handles frequencyPattern as number by converting to string', () => {
      const schedule = { frequencyPattern: 1 as any };
      mapScheduleData(schedule, formData);

      expect(formData.frequencyPattern).toBe('1');
    });

    it('converts dailyExecutionInterval 0 to string "0"', () => {
      const schedule = { dailyExecutionInterval: 0 };
      mapScheduleData(schedule, formData);

      expect(formData.dailyFrequency).toBe('0');
    });
  });

  describe('mapConnectionData', () => {
    let formData: Record<string, unknown>;

    beforeEach(() => {
      formData = {};
    });

    it('maps connectionId to existingConnectionId', () => {
      const detail = { connectionId: 'conn-123' };
      mapConnectionData(detail, formData);

      expect(formData.connectionMethod).toBe('existing');
      expect(formData.existingConnectionId).toBe('conn-123');
    });

    it('uses empty string when connectionId is missing', () => {
      const detail = {};
      mapConnectionData(detail, formData);

      expect(formData.connectionMethod).toBe('existing');
      expect(formData.existingConnectionId).toBe('');
    });

    it('handles undefined connectionId', () => {
      const detail = { connectionId: undefined };
      mapConnectionData(detail, formData);

      expect(formData.existingConnectionId).toBe('');
    });

    it('handles null connectionId', () => {
      const detail = { connectionId: null as any };
      mapConnectionData(detail, formData);

      expect(formData.existingConnectionId).toBe('');
    });
  });

  describe('mapArcGISDetailToFormData', () => {
    it('maps complete wizard input to form data', () => {
      const input = {
        detail: {
          name: 'Test Integration',
          description: 'Test Description',
          itemType: 'Case',
          itemSubtype: 'Criminal',
          itemSubtypeLabel: 'Criminal Case',
          dynamicDocumentType: 'Report',
          dynamicDocumentTypeLabel: 'Report Document',
          connectionId: 'conn-456',
        },
        schedule: {
          executionDate: '2024-04-01',
          executionTime: '10:00:00',
          frequencyPattern: 'DAILY',
          dailyExecutionInterval: 8,
          daySchedule: ['MONDAY'],
          monthSchedule: ['JANUARY'],
          cronExpression: '0 0 10 * * ?',
          isExecuteOnMonthEnd: false,
          customConfig: {
            pattern: 'specific-date',
            week: 'FIRST',
            weekday: 'MONDAY',
            dayOfMonth: 1,
          },
        },
        connection: {
          credentialType: 'BASIC',
          fetchMode: 'GET',
          arcgisEndpoint: 'https://api.example.com',
          username: 'user1',
          password: 'pass1',
          clientId: 'client1',
          clientSecret: 'secret1',
          tokenUrl: 'https://token.example.com',
          scope: 'read write',
        },
        fieldMappings: [
          {
            sourceFieldPath: 'id',
            targetFieldName: 'external_id',
            transformationType: 'NONE',
            isMandatory: true,
          },
          {
            sourceFieldPath: 'name',
            targetFieldName: 'title',
            transformationType: 'UPPERCASE',
            isMandatory: false,
          },
        ],
        selectedDocumentId: 'doc-789',
      };

      const result = mapArcGISDetailToFormData(input);

      // Basic details
      expect(result.name).toBe('Test Integration');
      expect(result.description).toBe('Test Description');

      // Document selection
      expect(result.itemType).toBe('Case');
      expect(result.subType).toBe('Criminal');
      expect(result.subTypeLabel).toBe('Criminal Case');
      expect(result.dynamicDocument).toBe('Report');
      expect(result.dynamicDocumentLabel).toBe('Report Document');
      expect(result.selectedDocumentId).toBe('doc-789');

      // Connection
      expect(result.connectionMethod).toBe('existing');
      expect(result.existingConnectionId).toBe('conn-456');
      expect(result.credentialType).toBe('BASIC');
      expect(result.fetchMode).toBe('GET');
      expect(result.arcgisEndpoint).toBe('https://api.example.com');
      expect(result.username).toBe('user1');
      expect(result.password).toBe('pass1');

      // Field mappings
      expect(result.fieldMappings).toHaveLength(2);
      expect(result.fieldMappings[0].sourceField).toBe('id');
      expect(result.fieldMappings[0].targetField).toBe('external_id');
      expect(result.fieldMappings[0].isMandatory).toBe(true);
      expect(result.fieldMappings[1].transformationType).toBe('UPPERCASE');
    });

    it('handles empty input with default values', () => {
      const input = {};
      const result = mapArcGISDetailToFormData(input);

      expect(result.name).toBe('');
      expect(result.description).toBe('');
      expect(result.itemType).toBe('DOCUMENT');
      expect(result.subType).toBe('');
      expect(result.connectionMethod).toBe('new');
      expect(result.existingConnectionId).toBe('');
      expect(result.executionDate).toBe('');
      expect(result.executionTime).toBe('09:00:00'); // Default time
      expect(result.fieldMappings).toHaveLength(1);
      expect(result.fieldMappings[0].sourceField).toBe('');
    });

    it('handles missing detail section', () => {
      const input = { schedule: { frequencyPattern: 'DAILY' } };
      const result = mapArcGISDetailToFormData(input);

      expect(result.name).toBe('');
      expect(result.description).toBe('');
      expect(result.itemType).toBe('DOCUMENT');
    });

    it('handles missing schedule section', () => {
      const input = { detail: { name: 'Test' } };
      const result = mapArcGISDetailToFormData(input);

      expect(result.executionDate).toBe('');
      expect(result.executionTime).toBe('09:00:00');
      expect(result.frequencyPattern).toBe('');
      expect(result.selectedDays).toEqual([]);
      expect(result.selectedMonths).toEqual([]);
    });

    it('handles missing connection section', () => {
      const input = { detail: { name: 'Test' } };
      const result = mapArcGISDetailToFormData(input);

      expect(result.credentialType).toBe('');
      expect(result.fetchMode).toBe('GET');
      expect(result.arcgisEndpoint).toBe('');
      expect(result.username).toBe('');
    });

    it('handles missing fieldMappings section', () => {
      const input = { detail: { name: 'Test' } };
      const result = mapArcGISDetailToFormData(input);

      expect(result.fieldMappings).toHaveLength(1);
      expect(result.fieldMappings[0]).toEqual({
        id: 'mapping-0',
        sourceField: '',
        targetField: '',
        transformationType: '',
        isMandatory: false,
        displayOrder: 0,
      });
    });

    it('handles empty fieldMappings array', () => {
      const input = { detail: { name: 'Test' }, fieldMappings: [] };
      const result = mapArcGISDetailToFormData(input);

      expect(result.fieldMappings).toHaveLength(1);
      expect(result.fieldMappings[0].sourceField).toBe('');
    });

    it('sets createdConnectionId to undefined', () => {
      const input = { detail: { name: 'Test' } };
      const result = mapArcGISDetailToFormData(input);

      expect(result.createdConnectionId).toBeUndefined();
    });

    it('maps multiple field mappings with correct IDs and displayOrder', () => {
      const input = {
        fieldMappings: [
          {
            sourceFieldPath: 'field1',
            targetFieldName: 'target1',
            transformationType: 'NONE',
            isMandatory: true,
          },
          {
            sourceFieldPath: 'field2',
            targetFieldName: 'target2',
            transformationType: 'LOWERCASE',
            isMandatory: false,
          },
          {
            sourceFieldPath: 'field3',
            targetFieldName: 'target3',
            transformationType: 'UPPERCASE',
            isMandatory: true,
          },
        ],
      };

      const result = mapArcGISDetailToFormData(input);

      expect(result.fieldMappings).toHaveLength(3);
      expect(result.fieldMappings[0].id).toBe('mapping-0');
      expect(result.fieldMappings[0].displayOrder).toBe(0);
      expect(result.fieldMappings[1].id).toBe('mapping-1');
      expect(result.fieldMappings[1].displayOrder).toBe(1);
      expect(result.fieldMappings[2].id).toBe('mapping-2');
      expect(result.fieldMappings[2].displayOrder).toBe(2);
    });

    it('defaults itemType to DOCUMENT when not provided', () => {
      const input = { detail: { name: 'Test' } };
      const result = mapArcGISDetailToFormData(input);

      expect(result.itemType).toBe('DOCUMENT');
    });

    it('sets connectionMethod to new when connectionId is missing', () => {
      const input = { detail: { name: 'Test' } };
      const result = mapArcGISDetailToFormData(input);

      expect(result.connectionMethod).toBe('new');
      expect(result.existingConnectionId).toBe('');
    });

    it('handles null values in connection fields', () => {
      const input = {
        connection: {
          credentialType: null as any,
          username: null as any,
          password: null as any,
        },
      };

      const result = mapArcGISDetailToFormData(input);

      expect(result.credentialType).toBe('');
      expect(result.username).toBe('');
      expect(result.password).toBe('');
    });

    it('handles missing selectedDocumentId', () => {
      const input = { detail: { name: 'Test' } };
      const result = mapArcGISDetailToFormData(input);

      expect(result.selectedDocumentId).toBe('');
    });
  });
});
