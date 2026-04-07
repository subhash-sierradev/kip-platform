import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import type { IntegrationConnectionResponse } from '@/api/models/IntegrationConnectionResponse';
import type { SavedConnection } from '@/types/ConnectionStepData';
import {
  formatLastTested,
  getConnectionStatus,
  transformToSavedConnection,
} from '@/utils/connectionStatusHelpers';

describe('connectionStatusHelpers', () => {
  describe('getConnectionStatus', () => {
    it('returns Not Tested status when lastConnectionStatus is undefined', () => {
      const connection: SavedConnection = {
        id: '1',
        name: 'Test Connection',
        secretName: 'secret',
        baseUrl: 'https://api.example.com',
        lastConnectionStatus: '' as any,
      };

      const result = getConnectionStatus(connection);

      expect(result).toEqual({
        label: 'Not Tested',
        severity: 'neutral',
      });
    });

    it('returns Not Tested status when lastConnectionStatus is null', () => {
      const connection: SavedConnection = {
        id: '1',
        name: 'Test Connection',
        secretName: 'secret',
        baseUrl: 'https://api.example.com',
        lastConnectionStatus: null as any,
      };

      const result = getConnectionStatus(connection);

      expect(result).toEqual({
        label: 'Not Tested',
        severity: 'neutral',
      });
    });

    it('returns Active status for SUCCESS (uppercase)', () => {
      const connection: SavedConnection = {
        id: '1',
        name: 'Test Connection',
        secretName: 'secret',
        baseUrl: 'https://api.example.com',
        lastConnectionStatus: 'SUCCESS',
      };

      const result = getConnectionStatus(connection);

      expect(result).toEqual({
        label: 'Active',
        severity: 'success',
      });
    });

    it('returns Active status for success (lowercase)', () => {
      const connection: SavedConnection = {
        id: '1',
        name: 'Test Connection',
        secretName: 'secret',
        baseUrl: 'https://api.example.com',
        lastConnectionStatus: 'success',
      };

      const result = getConnectionStatus(connection);

      expect(result).toEqual({
        label: 'Active',
        severity: 'success',
      });
    });

    it('returns Active status for SuCcEsS (mixed case)', () => {
      const connection: SavedConnection = {
        id: '1',
        name: 'Test Connection',
        secretName: 'secret',
        baseUrl: 'https://api.example.com',
        lastConnectionStatus: 'SuCcEsS',
      };

      const result = getConnectionStatus(connection);

      expect(result).toEqual({
        label: 'Active',
        severity: 'success',
      });
    });

    it('returns Failed status for FAILED (uppercase)', () => {
      const connection: SavedConnection = {
        id: '1',
        name: 'Test Connection',
        secretName: 'secret',
        baseUrl: 'https://api.example.com',
        lastConnectionStatus: 'FAILED',
      };

      const result = getConnectionStatus(connection);

      expect(result).toEqual({
        label: 'Failed',
        severity: 'error',
      });
    });

    it('returns Failed status for failed (lowercase)', () => {
      const connection: SavedConnection = {
        id: '1',
        name: 'Test Connection',
        secretName: 'secret',
        baseUrl: 'https://api.example.com',
        lastConnectionStatus: 'failed',
      };

      const result = getConnectionStatus(connection);

      expect(result).toEqual({
        label: 'Failed',
        severity: 'error',
      });
    });

    it('returns original status with info severity for unknown status', () => {
      const connection: SavedConnection = {
        id: '1',
        name: 'Test Connection',
        secretName: 'secret',
        baseUrl: 'https://api.example.com',
        lastConnectionStatus: 'PENDING',
      };

      const result = getConnectionStatus(connection);

      expect(result).toEqual({
        label: 'PENDING',
        severity: 'info',
      });
    });

    it('returns original status with info severity for arbitrary custom status', () => {
      const connection: SavedConnection = {
        id: '1',
        name: 'Test Connection',
        secretName: 'secret',
        baseUrl: 'https://api.example.com',
        lastConnectionStatus: 'IN_PROGRESS',
      };

      const result = getConnectionStatus(connection);

      expect(result).toEqual({
        label: 'IN_PROGRESS',
        severity: 'info',
      });
    });
  });

  describe('formatLastTested', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('returns "Never tested" when dateString is undefined', () => {
      expect(formatLastTested(undefined)).toBe('Never tested');
    });

    it('returns "Never tested" when dateString is empty', () => {
      expect(formatLastTested('')).toBe('Never tested');
    });

    it('returns "Just now" for dates less than 1 minute ago', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-15T09:59:30Z').toISOString();
      expect(formatLastTested(dateString)).toBe('Just now');
    });

    it('returns minutes ago for dates less than 60 minutes ago', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-15T09:45:00Z').toISOString();
      expect(formatLastTested(dateString)).toBe('15m ago');
    });

    it('returns 1 minute ago for exactly 1 minute', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-15T09:59:00Z').toISOString();
      expect(formatLastTested(dateString)).toBe('1m ago');
    });

    it('returns 59 minutes ago for 59 minutes', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-15T09:01:00Z').toISOString();
      expect(formatLastTested(dateString)).toBe('59m ago');
    });

    it('returns hours ago for dates less than 24 hours ago', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-15T07:00:00Z').toISOString();
      expect(formatLastTested(dateString)).toBe('3h ago');
    });

    it('returns 1 hour ago for exactly 1 hour', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-15T09:00:00Z').toISOString();
      expect(formatLastTested(dateString)).toBe('1h ago');
    });

    it('returns 23 hours ago for 23 hours', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-14T11:00:00Z').toISOString();
      expect(formatLastTested(dateString)).toBe('23h ago');
    });

    it('returns days ago for dates less than 7 days ago', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-13T10:00:00Z').toISOString();
      expect(formatLastTested(dateString)).toBe('2d ago');
    });

    it('returns 1 day ago for exactly 1 day', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-14T10:00:00Z').toISOString();
      expect(formatLastTested(dateString)).toBe('1d ago');
    });

    it('returns 6 days ago for 6 days', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-09T10:00:00Z').toISOString();
      expect(formatLastTested(dateString)).toBe('6d ago');
    });

    it('returns formatted date for dates 7 days or older', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2024-01-01T10:00:00Z').toISOString();
      const result = formatLastTested(dateString);

      // Should be a localized date string
      expect(result).toMatch(/\d{1,2}\/\d{1,2}\/\d{4}/);
    });

    it('returns formatted date for dates several months ago', () => {
      const now = new Date('2024-01-15T10:00:00Z');
      vi.setSystemTime(now);

      const dateString = new Date('2023-06-01T10:00:00Z').toISOString();
      const result = formatLastTested(dateString);

      expect(result).toMatch(/\d{1,2}\/\d{1,2}\/\d{4}/);
    });

    it('returns "Invalid Date" for invalid date string', () => {
      // Invalid date strings return toLocaleDateString() which is 'Invalid Date'
      expect(formatLastTested('invalid-date')).toBe('Invalid Date');
    });

    it('returns "Invalid Date" for malformed date string', () => {
      // Malformed date strings return toLocaleDateString() which is 'Invalid Date'
      expect(formatLastTested('2024-99-99T99:99:99Z')).toBe('Invalid Date');
    });

    it('returns "Invalid Date" for non-date string', () => {
      // Non-date strings return toLocaleDateString() which is 'Invalid Date'
      expect(formatLastTested('not a date at all')).toBe('Invalid Date');
    });
  });

  describe('transformToSavedConnection', () => {
    it('transforms complete IntegrationConnectionResponse to SavedConnection', () => {
      const response: IntegrationConnectionResponse = {
        id: '123',
        name: 'Jira Connection',
        secretName: 'jira-secret',
        lastConnectionStatus: 'SUCCESS' as any,
        lastConnectionTest: '2024-01-15T10:00:00Z',
      };

      const result = transformToSavedConnection(response);

      expect(result).toEqual({
        id: '123',
        name: 'Jira Connection',
        secretName: 'jira-secret',
        baseUrl: '',
        lastConnectionStatus: 'SUCCESS',
        lastConnectionTest: '2024-01-15T10:00:00Z',
      });
    });

    it('handles undefined id by setting empty string', () => {
      const response: IntegrationConnectionResponse = {
        id: undefined,
        name: 'Test Connection',
        secretName: 'test-secret',
      };

      const result = transformToSavedConnection(response);

      expect(result.id).toBe('');
    });

    it('handles null id by setting empty string', () => {
      const response: IntegrationConnectionResponse = {
        id: null as any,
        name: 'Test Connection',
        secretName: 'test-secret',
      };

      const result = transformToSavedConnection(response);

      expect(result.id).toBe('');
    });

    it('handles undefined name by setting empty string', () => {
      const response: IntegrationConnectionResponse = {
        id: '123',
        name: undefined,
        secretName: 'test-secret',
      };

      const result = transformToSavedConnection(response);

      expect(result.name).toBe('');
    });

    it('handles null name by setting empty string', () => {
      const response: IntegrationConnectionResponse = {
        id: '123',
        name: null as any,
        secretName: 'test-secret',
      };

      const result = transformToSavedConnection(response);

      expect(result.name).toBe('');
    });

    it('handles undefined lastConnectionStatus by setting UNKNOWN', () => {
      const response: IntegrationConnectionResponse = {
        id: '123',
        name: 'Test Connection',
        secretName: 'test-secret',
        lastConnectionStatus: undefined,
      };

      const result = transformToSavedConnection(response);

      expect(result.lastConnectionStatus).toBe('UNKNOWN');
    });

    it('handles null lastConnectionStatus by setting UNKNOWN', () => {
      const response: IntegrationConnectionResponse = {
        id: '123',
        name: 'Test Connection',
        secretName: 'test-secret',
        lastConnectionStatus: null as any,
      };

      const result = transformToSavedConnection(response);

      expect(result.lastConnectionStatus).toBe('UNKNOWN');
    });

    it('preserves lastConnectionTest when provided', () => {
      const testDate = '2024-01-15T10:00:00Z';
      const response: IntegrationConnectionResponse = {
        id: '123',
        name: 'Test Connection',
        secretName: 'test-secret',
        lastConnectionTest: testDate,
      };

      const result = transformToSavedConnection(response);

      expect(result.lastConnectionTest).toBe(testDate);
    });

    it('handles undefined lastConnectionTest', () => {
      const response: IntegrationConnectionResponse = {
        id: '123',
        name: 'Test Connection',
        secretName: 'test-secret',
        lastConnectionTest: undefined,
      };

      const result = transformToSavedConnection(response);

      expect(result.lastConnectionTest).toBeUndefined();
    });

    it('always sets baseUrl to empty string', () => {
      const response: IntegrationConnectionResponse = {
        id: '123',
        name: 'Test Connection',
        secretName: 'test-secret',
      };

      const result = transformToSavedConnection(response);

      expect(result.baseUrl).toBe('');
    });

    it('handles minimal response with only required fields', () => {
      const response: IntegrationConnectionResponse = {
        secretName: 'test-secret',
      };

      const result = transformToSavedConnection(response);

      expect(result).toEqual({
        id: '',
        name: '',
        secretName: 'test-secret',
        baseUrl: '',
        lastConnectionStatus: 'UNKNOWN',
        lastConnectionTest: undefined,
      });
    });
  });
});
