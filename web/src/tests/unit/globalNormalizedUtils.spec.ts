import { describe, expect, it } from 'vitest';

import {
  checkDuplicateName,
  normalizeIntegrationNameForCompare,
} from '@/utils/globalNormalizedUtils';

describe('globalNormalizedUtils', () => {
  describe('normalizeIntegrationNameForCompare', () => {
    describe('basic normalization', () => {
      it('should convert to lowercase', () => {
        expect(normalizeIntegrationNameForCompare('TEST')).toBe('test');
        expect(normalizeIntegrationNameForCompare('Test')).toBe('test');
        expect(normalizeIntegrationNameForCompare('TeSt')).toBe('test');
      });

      it('should trim whitespace from start and end', () => {
        expect(normalizeIntegrationNameForCompare('  test  ')).toBe('test');
        expect(normalizeIntegrationNameForCompare('\ttest\n')).toBe('test');
        expect(normalizeIntegrationNameForCompare('   ')).toBe('');
      });

      it('should replace spaces with underscores', () => {
        expect(normalizeIntegrationNameForCompare('test daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('Test Daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('test   daily')).toBe('test_daily');
      });

      it('should keep underscores as underscores', () => {
        expect(normalizeIntegrationNameForCompare('test_daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('Test_Daily')).toBe('test_daily');
      });

      it('should replace hyphens with underscores', () => {
        expect(normalizeIntegrationNameForCompare('test-daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('Test-Daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('test---daily')).toBe('test_daily');
      });
    });

    describe('separator handling', () => {
      it('should treat spaces, underscores, and hyphens as equivalent', () => {
        const test1 = normalizeIntegrationNameForCompare('test daily');
        const test2 = normalizeIntegrationNameForCompare('test_daily');
        const test3 = normalizeIntegrationNameForCompare('test-daily');

        expect(test1).toBe(test2);
        expect(test2).toBe(test3);
        expect(test1).toBe('test_daily');
      });

      it('should collapse multiple consecutive separators into single underscore', () => {
        expect(normalizeIntegrationNameForCompare('test   daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('test___daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('test---daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('test _ - daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('test  __  --  daily')).toBe('test_daily');
      });

      it('should handle mixed separators', () => {
        expect(normalizeIntegrationNameForCompare('test_daily-report')).toBe('test_daily_report');
        expect(normalizeIntegrationNameForCompare('test-daily_report')).toBe('test_daily_report');
        expect(normalizeIntegrationNameForCompare('test daily-report_name')).toBe(
          'test_daily_report_name'
        );
      });
    });

    describe('edge cases', () => {
      it('should handle null or undefined gracefully', () => {
        expect(normalizeIntegrationNameForCompare('')).toBe('');
      });

      it('should handle single character', () => {
        expect(normalizeIntegrationNameForCompare('a')).toBe('a');
        expect(normalizeIntegrationNameForCompare('A')).toBe('a');
      });

      it('should handle numbers', () => {
        expect(normalizeIntegrationNameForCompare('test123')).toBe('test123');
        expect(normalizeIntegrationNameForCompare('Test 123 Daily')).toBe('test_123_daily');
      });

      it('should remove all special characters', () => {
        expect(normalizeIntegrationNameForCompare('test@daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('test#daily')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('test!@#$%daily')).toBe('test_daily');
      });

      it('should handle only separators', () => {
        expect(normalizeIntegrationNameForCompare('   ')).toBe('');
        expect(normalizeIntegrationNameForCompare('___')).toBe('');
        expect(normalizeIntegrationNameForCompare('---')).toBe('');
        expect(normalizeIntegrationNameForCompare('_ - _')).toBe('');
        expect(normalizeIntegrationNameForCompare('!@#$%')).toBe('');
      });

      it('should handle real-world examples', () => {
        expect(normalizeIntegrationNameForCompare('Jira Webhook Integration')).toBe(
          'jira_webhook_integration'
        );
        expect(normalizeIntegrationNameForCompare('jira-webhook-integration')).toBe(
          'jira_webhook_integration'
        );
        expect(normalizeIntegrationNameForCompare('jira_webhook_integration')).toBe(
          'jira_webhook_integration'
        );
        expect(normalizeIntegrationNameForCompare('JIRA_WEBHOOK_INTEGRATION')).toBe(
          'jira_webhook_integration'
        );
      });
    });
  });

  describe('checkDuplicateName', () => {
    describe('basic duplicate detection', () => {
      it('should detect exact duplicate', () => {
        const allNames = ['test_daily', 'test_weekly'];
        expect(checkDuplicateName('test_daily', allNames)).toBe(true);
        expect(checkDuplicateName('test_weekly', allNames)).toBe(true);
      });

      it('should return false when no duplicate found', () => {
        const allNames = ['test_daily', 'test_weekly'];
        expect(checkDuplicateName('test_monthly', allNames)).toBe(false);
      });

      it('should return false for empty input name', () => {
        const allNames = ['test_daily'];
        expect(checkDuplicateName('', allNames)).toBe(false);
      });

      it('should return false for empty array', () => {
        expect(checkDuplicateName('test_daily', [])).toBe(false);
      });
    });

    describe('normalization in duplicate detection', () => {
      it('should treat spaces and underscores as equivalent', () => {
        const allNames = ['test_daily'];
        expect(checkDuplicateName('test daily', allNames)).toBe(true);
        expect(checkDuplicateName('test-daily', allNames)).toBe(true);
      });

      it('should be case-insensitive', () => {
        const allNames = ['test_daily'];
        expect(checkDuplicateName('test_daily', allNames)).toBe(true);
        expect(checkDuplicateName('TEST_DAILY', allNames)).toBe(true);
        expect(checkDuplicateName('Test Daily', allNames)).toBe(true);
      });

      it('should detect duplicates across different separator styles', () => {
        const allNames = ['test_daily_report'];
        expect(checkDuplicateName('test daily report', allNames)).toBe(true);
        expect(checkDuplicateName('test-daily-report', allNames)).toBe(true);
        expect(checkDuplicateName('TEST DAILY REPORT', allNames)).toBe(true);
      });

      it('should detect duplicates with multiple consecutive separators', () => {
        const allNames = ['test_daily'];
        expect(checkDuplicateName('test   daily', allNames)).toBe(true);
        expect(checkDuplicateName('test___daily', allNames)).toBe(true);
        expect(checkDuplicateName('test---daily', allNames)).toBe(true);
      });

      it('should work when callers pass a normalized list', () => {
        const rawDbNames = ['Test Daily', 'Weekly-Report', 'Monthly_Stats'];
        const allNames = rawDbNames.map(normalizeIntegrationNameForCompare);

        expect(checkDuplicateName('test_daily', allNames)).toBe(true);
        expect(checkDuplicateName('weekly report', allNames)).toBe(true);
        expect(checkDuplicateName('MONTHLY-STATS', allNames)).toBe(true);
      });

      it('should handle names with leading/trailing whitespace in array', () => {
        const rawDbNames = ['  test_daily  ', 'test_weekly'];
        const allNames = rawDbNames.map(normalizeIntegrationNameForCompare);

        expect(checkDuplicateName('test_daily', allNames)).toBe(true);
        expect(checkDuplicateName('test daily', allNames)).toBe(true);
      });

      it('should handle names with mixed separators in array', () => {
        const rawDbNames = ['test-daily_report', 'weekly report-summary'];
        const allNames = rawDbNames.map(normalizeIntegrationNameForCompare);

        expect(checkDuplicateName('test_daily_report', allNames)).toBe(true);
        expect(checkDuplicateName('weekly-report-summary', allNames)).toBe(true);
      });
    });

    describe('edit mode behavior', () => {
      it('should allow keeping original name in edit mode', () => {
        const allNames = ['original_name', 'other_name'];
        expect(checkDuplicateName('original_name', allNames, 'original_name', true)).toBe(false);
      });

      it('should allow keeping original name with different separators', () => {
        const allNames = ['original_name', 'other_name'];
        expect(checkDuplicateName('original name', allNames, 'original_name', true)).toBe(false);
      });

      it('should allow keeping original name with different case', () => {
        const allNames = ['original_name', 'other_name'];
        expect(checkDuplicateName('ORIGINAL_NAME', allNames, 'original_name', true)).toBe(false);
      });

      it('should detect duplicate when changing to different name in edit mode', () => {
        const allNames = ['original_name', 'other_name'];
        expect(checkDuplicateName('other_name', allNames, 'original_name', true)).toBe(true);
      });

      it('should detect duplicate when new name matches another in edit mode', () => {
        const allNames = ['existing_name', 'another_name'];
        expect(checkDuplicateName('existing_name', allNames, 'different_name', true)).toBe(true);
      });

      it('should not be in edit mode by default', () => {
        const allNames = ['test_name'];
        expect(checkDuplicateName('test_name', allNames)).toBe(true);
      });
    });

    describe('create mode behavior (editMode: false)', () => {
      it('should always detect duplicates regardless of originalName', () => {
        const allNames = ['test_name'];
        expect(checkDuplicateName('test_name', allNames, 'test_name', false)).toBe(true);
      });

      it('should detect all duplicates in create mode', () => {
        const allNames = ['daily', 'weekly', 'monthly'];
        expect(checkDuplicateName('daily', allNames)).toBe(true);
        expect(checkDuplicateName('weekly', allNames)).toBe(true);
        expect(checkDuplicateName('monthly', allNames)).toBe(true);
        expect(checkDuplicateName('yearly', allNames)).toBe(false);
      });
    });

    describe('real-world scenarios', () => {
      it('should handle Jira webhook names', () => {
        const rawExistingWebhooks = [
          'Jira_Webhook_Integration',
          'test-webhook',
          'production webhook',
        ];
        const existingWebhooks = rawExistingWebhooks.map(normalizeIntegrationNameForCompare);

        // Should detect duplicates
        expect(checkDuplicateName('Jira Webhook Integration', existingWebhooks)).toBe(true);
        expect(checkDuplicateName('test-webhook', existingWebhooks)).toBe(true);
        expect(checkDuplicateName('Production Webhook', existingWebhooks)).toBe(true);

        // Should not detect as duplicates
        expect(checkDuplicateName('Jira Webhook', existingWebhooks)).toBe(false);
        expect(checkDuplicateName('new webhook', existingWebhooks)).toBe(false);
      });

      it('should handle ArcGIS integration names', () => {
        const rawExistingIntegrations = [
          'ArcGIS_Map_Sync',
          'arcgis-layer-integration',
          'ArcGIS Database Connector',
        ];
        const existingIntegrations = rawExistingIntegrations.map(
          normalizeIntegrationNameForCompare
        );

        // Edit scenario: update integration name
        expect(
          checkDuplicateName('ArcGIS Map Sync', existingIntegrations, 'ArcGIS_Map_Sync', true)
        ).toBe(false); // Allowed - keeping original

        expect(
          checkDuplicateName(
            'ArcGIS Layer Integration',
            existingIntegrations,
            'ArcGIS_Map_Sync',
            true
          )
        ).toBe(true); // Not allowed - conflicts with existing
      });

      it('should handle clone scenario (editMode: false with originalName present)', () => {
        const allNames = ['original_webhook'];

        // Clone creates new webhook from existing, so original name is source
        // But editMode: false, so should still detect if name matches existing
        expect(checkDuplicateName('Original Webhook', allNames, 'Original_Webhook', false)).toBe(
          true
        ); // Duplicate detected because editMode is false
      });
    });

    describe('edge cases', () => {
      it('should handle whitespace in originalName', () => {
        const allNames = ['test_daily'];
        expect(checkDuplicateName('test_daily', allNames, '  test daily  ', true)).toBe(false); // Should trim and normalize originalName
      });

      it('should handle undefined originalName in edit mode', () => {
        const allNames = ['test_daily', 'test_weekly'];
        expect(checkDuplicateName('test_daily', allNames, undefined, true)).toBe(true); // Should treat as duplicate since originalName is not set
      });

      it('should handle empty string originalName in edit mode', () => {
        const allNames = ['test_daily', 'test_weekly'];
        expect(checkDuplicateName('test_daily', allNames, '', true)).toBe(true); // Empty original name should not allow duplicates
      });

      it('should handle array with single name', () => {
        expect(checkDuplicateName('test', ['test'])).toBe(true);
        expect(checkDuplicateName('test', ['other'])).toBe(false);
      });

      it("should handle array with duplicates (shouldn't happen but should work)", () => {
        const allNames = ['test_daily', 'test_daily', 'test_daily'];
        expect(checkDuplicateName('test_daily', allNames)).toBe(true);
      });

      it('should handle very long names', () => {
        const longName = 'Very Long Integration Name With Multiple Words And Separators';
        const allNames = [normalizeIntegrationNameForCompare(longName)];
        expect(checkDuplicateName(longName, allNames)).toBe(true);
      });

      it('should handle names with numbers and mixed content', () => {
        const allNames = ['test_123_daily', 'webhook_v2_integration'];
        expect(checkDuplicateName('Test 123 Daily', allNames)).toBe(true);
        expect(checkDuplicateName('webhook-v2-integration', allNames)).toBe(true);
        expect(checkDuplicateName('test 456 daily', allNames)).toBe(false);
      });

      it('should handle names with only numbers', () => {
        const allNames = ['123', '456_789'];
        expect(checkDuplicateName('123', allNames)).toBe(true);
        expect(checkDuplicateName('456 789', allNames)).toBe(true);
        expect(checkDuplicateName('456-789', allNames)).toBe(true);
      });

      it('should handle names with leading/trailing separators', () => {
        expect(normalizeIntegrationNameForCompare('_test_daily_')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare(' test daily ')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('-test-daily-')).toBe('test_daily');
        expect(normalizeIntegrationNameForCompare('___test___')).toBe('test');
      });

      it('should handle null-like values gracefully', () => {
        const allNames = ['test_daily'];
        expect(checkDuplicateName('', allNames)).toBe(false);
        expect(checkDuplicateName('   ', allNames)).toBe(false);
      });

      it('should handle comparison when array has empty/whitespace entries', () => {
        const allNames = ['test_daily', '', '', 'test_weekly'];
        expect(checkDuplicateName('test_daily', allNames)).toBe(true);
        expect(checkDuplicateName('test_weekly', allNames)).toBe(true);
        expect(checkDuplicateName('test_monthly', allNames)).toBe(false);
      });

      it('should maintain consistent normalization across multiple calls', () => {
        const name1 = 'Test Daily Integration';
        const name2 = 'test_daily_integration';
        const name3 = 'TEST-DAILY-INTEGRATION';

        const normalized1 = normalizeIntegrationNameForCompare(name1);
        const normalized2 = normalizeIntegrationNameForCompare(name2);
        const normalized3 = normalizeIntegrationNameForCompare(name3);

        expect(normalized1).toBe(normalized2);
        expect(normalized2).toBe(normalized3);
        expect(normalized1).toBe('test_daily_integration');
      });
    });
  });
});
