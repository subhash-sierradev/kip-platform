import { beforeEach, describe, expect, it, vi } from 'vitest';

import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import { verifyConnectionWithApi } from '@/utils/connectionVerificationHelpers';

vi.mock('@/api/services/IntegrationConnectionService');

describe('connectionVerificationHelpers', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe('verifyConnectionWithApi', () => {
    it('returns error when connectionId is empty string', async () => {
      const result = await verifyConnectionWithApi('');

      expect(result).toEqual({
        success: false,
        message: 'Please select a connection',
      });
      expect(IntegrationConnectionService.testExistingConnection).not.toHaveBeenCalled();
    });

    it('returns error when connectionId is undefined', async () => {
      const result = await verifyConnectionWithApi(undefined as any);

      expect(result).toEqual({
        success: false,
        message: 'Please select a connection',
      });
      expect(IntegrationConnectionService.testExistingConnection).not.toHaveBeenCalled();
    });

    it('returns error when connectionId is null', async () => {
      const result = await verifyConnectionWithApi(null as any);

      expect(result).toEqual({
        success: false,
        message: 'Please select a connection',
      });
      expect(IntegrationConnectionService.testExistingConnection).not.toHaveBeenCalled();
    });

    it('returns success when API returns SUCCESS status with custom message', async () => {
      const mockResponse = {
        statusCode: 200,
        success: true,
        connectionStatus: 'SUCCESS',
        message: 'Custom success message',
      } as any;

      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue(
        mockResponse
      );

      const result = await verifyConnectionWithApi('conn-123');

      expect(result).toEqual({
        success: true,
        message: 'Custom success message',
      });
      expect(IntegrationConnectionService.testExistingConnection).toHaveBeenCalledWith({
        connectionId: 'conn-123',
      });
    });

    it('returns success with default message when API returns SUCCESS without message', async () => {
      const mockResponse = {
        statusCode: 200,
        success: true,
        connectionStatus: 'SUCCESS',
        message: '',
      } as any;

      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue(
        mockResponse
      );

      const result = await verifyConnectionWithApi('conn-456');

      expect(result).toEqual({
        success: true,
        message: 'Connection verified successfully',
      });
    });

    it('returns failure when API returns FAILED status with custom message', async () => {
      const mockResponse = {
        statusCode: 400,
        success: false,
        connectionStatus: 'FAILED',
        message: 'Invalid credentials',
      } as any;

      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue(
        mockResponse
      );

      const result = await verifyConnectionWithApi('conn-789');

      expect(result).toEqual({
        success: false,
        message: 'Invalid credentials',
      });
    });

    it('returns failure with default message when API returns FAILED without message', async () => {
      const mockResponse = {
        statusCode: 400,
        success: false,
        connectionStatus: 'FAILED',
        message: '',
      } as any;

      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue(
        mockResponse
      );

      const result = await verifyConnectionWithApi('conn-failed');

      expect(result).toEqual({
        success: false,
        message: 'Connection verification failed',
      });
    });

    it('handles non-SUCCESS/FAILED status codes with custom message', async () => {
      const mockResponse = {
        statusCode: 202,
        success: false,
        connectionStatus: 'PENDING',
        message: 'Connection test in progress',
      } as any;

      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue(
        mockResponse
      );

      const result = await verifyConnectionWithApi('conn-pending');

      expect(result).toEqual({
        success: false,
        message: 'Connection test in progress',
      });
    });

    it('handles API error with error message', async () => {
      const error = new Error('Network timeout');
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockRejectedValue(error);

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await verifyConnectionWithApi('conn-error');

      expect(result).toEqual({
        success: false,
        message: 'Network timeout',
      });
      expect(consoleSpy).toHaveBeenCalledWith('Error verifying connection:', error);

      consoleSpy.mockRestore();
    });

    it('prefers backend error body message when available', async () => {
      const apiErrorLike = {
        message: 'Service Unavailable',
        body: {
          message: 'External service is temporarily unavailable. Please try again later.',
        },
      };

      vi.mocked(IntegrationConnectionService.testExistingConnection).mockRejectedValue(
        apiErrorLike
      );

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await verifyConnectionWithApi('conn-error-body');

      expect(result).toEqual({
        success: false,
        message: 'External service is temporarily unavailable. Please try again later.',
      });

      consoleSpy.mockRestore();
    });

    it('handles API error without error message', async () => {
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockRejectedValue({});

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await verifyConnectionWithApi('conn-unknown-error');

      expect(result).toEqual({
        success: false,
        message: 'Failed to verify connection',
      });

      consoleSpy.mockRestore();
    });

    it('handles API error with non-Error object', async () => {
      vi.mocked(IntegrationConnectionService.testExistingConnection).mockRejectedValue(
        'String error'
      );

      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      const result = await verifyConnectionWithApi('conn-string-error');

      expect(result).toEqual({
        success: false,
        message: 'Failed to verify connection',
      });

      consoleSpy.mockRestore();
    });

    it('handles lowercase SUCCESS status', async () => {
      const mockResponse = {
        statusCode: 200,
        success: true,
        connectionStatus: 'success',
        message: 'OK',
      } as any;

      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue(
        mockResponse
      );

      const result = await verifyConnectionWithApi('conn-lower');

      // We now read response.success (boolean) directly, so true === true regardless of connectionStatus casing
      expect(result).toEqual({
        success: true,
        message: 'OK',
      });
    });

    it('passes correct connectionId to API', async () => {
      const mockResponse = {
        statusCode: 200,
        success: true,
        connectionStatus: 'SUCCESS',
        message: '',
      } as any;

      vi.mocked(IntegrationConnectionService.testExistingConnection).mockResolvedValue(
        mockResponse
      );

      await verifyConnectionWithApi('specific-connection-id-123');

      expect(IntegrationConnectionService.testExistingConnection).toHaveBeenCalledWith({
        connectionId: 'specific-connection-id-123',
      });
    });
  });
});
