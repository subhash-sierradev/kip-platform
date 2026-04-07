import type { AxiosRequestConfig } from 'axios';
import axios from 'axios';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  cleanupGlobalLoader,
  setupGlobalLoader,
  withLoading,
  withLoadingMessage,
} from '@/utils/setupGlobalLoader';

// Mock the useGlobalLoading composable
const mockSetLoading = vi.fn();
const mockResetLoading = vi.fn();

vi.mock('@/composables/useGlobalLoading', () => ({
  useGlobalLoading: () => ({
    setLoading: mockSetLoading,
    resetLoading: mockResetLoading,
  }),
}));

// Mock axios interceptors properly with factory function
vi.mock('axios', () => ({
  default: {
    interceptors: {
      request: {
        use: vi.fn(),
      },
      response: {
        use: vi.fn(),
      },
    },
  },
}));

describe('setupGlobalLoader', () => {
  let requestInterceptor: any;
  let responseInterceptor: any;
  const mockedAxios = vi.mocked(axios);

  beforeEach(() => {
    vi.clearAllMocks();

    // Capture interceptor functions when they're registered
    (mockedAxios.interceptors.request.use as any).mockImplementation(
      (onFulfilled: any, onRejected: any) => {
        requestInterceptor = { onFulfilled, onRejected };
        return 1; // Mock interceptor ID
      }
    );

    (mockedAxios.interceptors.response.use as any).mockImplementation(
      (onFulfilled: any, onRejected: any) => {
        responseInterceptor = { onFulfilled, onRejected };
        return 2; // Mock interceptor ID
      }
    );

    // Mock eject methods for cleanup
    (mockedAxios.interceptors.request.eject as any) = vi.fn();
    (mockedAxios.interceptors.response.eject as any) = vi.fn();
  });

  afterEach(() => {
    // Clean up after each test to prevent interceptor stacking
    cleanupGlobalLoader();
  });

  describe('setupGlobalLoader', () => {
    it('should register request and response interceptors and return cleanup interface', () => {
      const result = setupGlobalLoader();

      expect(mockedAxios.interceptors.request.use).toHaveBeenCalledWith(
        expect.any(Function),
        expect.any(Function)
      );
      expect(mockedAxios.interceptors.response.use).toHaveBeenCalledWith(
        expect.any(Function),
        expect.any(Function)
      );
      expect(result).toEqual({
        cleanup: expect.any(Function),
        isSetup: true,
      });
    });

    it('should prevent multiple registrations and warn', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      const first = setupGlobalLoader();
      const second = setupGlobalLoader();

      expect(first.isSetup).toBe(true);
      expect(second.isSetup).toBe(true);
      expect(consoleSpy).toHaveBeenCalledWith(
        'setupGlobalLoader: Interceptors already registered. Use cleanup() first if you need to re-register.'
      );
      expect(mockedAxios.interceptors.request.use).toHaveBeenCalledTimes(1);
      expect(mockedAxios.interceptors.response.use).toHaveBeenCalledTimes(1);

      consoleSpy.mockRestore();
    });

    it('should generate intelligent loading message for request', () => {
      setupGlobalLoader();

      const config: AxiosRequestConfig = {
        method: 'post',
        url: '/api/jira-webhooks',
      };

      requestInterceptor.onFulfilled(config);

      expect(mockSetLoading).toHaveBeenCalledWith(true, 'Creating jira webhooks...');
    });

    it('should use custom loading message when provided', () => {
      setupGlobalLoader();

      const config: AxiosRequestConfig = {
        method: 'get',
        url: '/api/data',
        loadingMessage: 'Custom loading...',
      };

      requestInterceptor.onFulfilled(config);

      expect(mockSetLoading).toHaveBeenCalledWith(true, 'Custom loading...');
    });

    it('should skip global loader when requested', () => {
      setupGlobalLoader();

      const config: AxiosRequestConfig = {
        method: 'get',
        url: '/api/data',
        skipGlobalLoader: true,
      };

      const result = requestInterceptor.onFulfilled(config);

      expect(mockSetLoading).not.toHaveBeenCalled();
      expect(result).toBe(config);
    });

    it('should handle request interceptor errors', async () => {
      setupGlobalLoader();

      const error = new Error('Request error');

      await expect(requestInterceptor.onRejected(error)).rejects.toThrow('Request error');
      expect(mockSetLoading).toHaveBeenCalledWith(false);
    });

    it('should handle successful responses', () => {
      setupGlobalLoader();

      const response = { data: { success: true }, status: 200 };
      const result = responseInterceptor.onFulfilled(response);

      expect(mockSetLoading).toHaveBeenCalledWith(false);
      expect(result).toBe(response);
    });

    it('should handle response errors', async () => {
      setupGlobalLoader();

      const error = new Error('Response error');

      await expect(responseInterceptor.onRejected(error)).rejects.toThrow('Response error');
      expect(mockSetLoading).toHaveBeenCalledWith(false);
    });
  });

  describe('intelligent message generation', () => {
    beforeEach(() => {
      setupGlobalLoader();
    });

    it('should generate appropriate messages for different HTTP methods', () => {
      const testCases = [
        { method: 'GET', url: '/api/users', expected: 'Loading users...' },
        { method: 'POST', url: '/api/users', expected: 'Creating users...' },
        { method: 'PUT', url: '/api/users/12345678', expected: 'Updating users...' }, // 8 chars for ID
        { method: 'PATCH', url: '/api/users/12345678', expected: 'Updating users...' }, // 8 chars for ID
        { method: 'DELETE', url: '/api/users/12345678', expected: 'Deleting users...' },
      ];

      testCases.forEach(({ method, url, expected }) => {
        vi.clearAllMocks();
        requestInterceptor.onFulfilled({ method: method.toLowerCase(), url });
        expect(mockSetLoading).toHaveBeenCalledWith(true, expected);
      });
    });

    it('should handle special endpoint patterns', () => {
      const testCases = [
        { url: '/api/test-connection', expected: 'Testing connection...' },
        { url: '/api/validate', expected: 'Validating...' },
        { url: '/api/search', expected: 'Searching...' },
        { url: '/api/export', expected: 'Exporting...' },
        { url: '/api/import', expected: 'Importing...' },
        { url: '/api/sync', expected: 'Synchronizing...' },
      ];

      testCases.forEach(({ url, expected }) => {
        vi.clearAllMocks();
        requestInterceptor.onFulfilled({ method: 'get', url });
        expect(mockSetLoading).toHaveBeenCalledWith(true, expected);
      });
    });

    it('should clean resource names properly', () => {
      const testCases = [
        { url: '/api/jira-webhooks', expected: 'Loading jira webhooks...' },
        { url: '/api/master_data', expected: 'Loading item...' }, // master_data is detected as ID, fallback to 'item'
        { url: '/api/user-profiles', expected: 'Loading user profiles...' }, // avoid test-connection pattern
      ];

      testCases.forEach(({ url, expected }) => {
        vi.clearAllMocks();
        requestInterceptor.onFulfilled({ method: 'get', url });
        expect(mockSetLoading).toHaveBeenCalledWith(true, expected);
      });
    });

    it('should detect ID segments and use appropriate resource names', () => {
      const testCases = [
        { url: '/api/users/12345678', expected: 'Loading users...' }, // 8 chars detected as ID
        { url: '/api/webhooks/abc123def456', expected: 'Loading webhooks...' }, // 12 chars detected as ID
        { url: '/api/items/uuid-style-id-here', expected: 'Loading uuid style id here...' }, // only lowercase letters and hyphens, not detected as ID
        { url: '/api/data', expected: 'Loading data...' },
      ];

      testCases.forEach(({ url, expected }) => {
        vi.clearAllMocks();
        requestInterceptor.onFulfilled({ method: 'get', url });
        expect(mockSetLoading).toHaveBeenCalledWith(true, expected);
      });
    });

    it('should handle POST requests differently for ID endpoints', () => {
      const testCases = [
        { url: '/api/users', method: 'post', expected: 'Creating users...' },
        { url: '/api/users/12345678', method: 'post', expected: 'Updating users...' }, // 8 chars = ID, so updating
      ];

      testCases.forEach(({ url, method, expected }) => {
        vi.clearAllMocks();
        requestInterceptor.onFulfilled({ method, url });
        expect(mockSetLoading).toHaveBeenCalledWith(true, expected);
      });
    });

    it('should handle empty or invalid URLs gracefully', () => {
      const testCases = [
        { url: '', expected: 'Loading data...' },
        { url: '/', expected: 'Loading data...' },
        { url: '/api/', expected: 'Loading data...' },
        { url: undefined, expected: 'Loading data...' },
      ];

      testCases.forEach(({ url, expected }) => {
        vi.clearAllMocks();
        requestInterceptor.onFulfilled({ method: 'get', url });
        expect(mockSetLoading).toHaveBeenCalledWith(true, expected);
      });
    });
  });

  describe('cleanupGlobalLoader', () => {
    it('should clean up interceptors and reset state', () => {
      const result = setupGlobalLoader();
      expect(result.isSetup).toBe(true);

      cleanupGlobalLoader();

      expect(mockedAxios.interceptors.request.eject).toHaveBeenCalledWith(1);
      expect(mockedAxios.interceptors.response.eject).toHaveBeenCalledWith(2);
      expect(mockResetLoading).toHaveBeenCalled();
    });

    it('should allow re-registration after cleanup', () => {
      setupGlobalLoader();
      expect(mockedAxios.interceptors.request.use).toHaveBeenCalledTimes(1);

      cleanupGlobalLoader();

      const newResult = setupGlobalLoader();
      expect(newResult.isSetup).toBe(true);
      expect(mockedAxios.interceptors.request.use).toHaveBeenCalledTimes(2);
    });

    it('should handle cleanup when interceptors are null', () => {
      // Call cleanup without setting up first
      expect(() => cleanupGlobalLoader()).not.toThrow();
      expect(mockResetLoading).toHaveBeenCalled();
    });
  });

  describe('withLoadingMessage', () => {
    it('should add loading message to config', () => {
      const baseConfig: AxiosRequestConfig = { method: 'get', url: '/api/test' };
      const result = withLoadingMessage(baseConfig, 'Custom message...');

      expect(result).toEqual({
        method: 'get',
        url: '/api/test',
        loadingMessage: 'Custom message...',
      });
    });

    it('should set skipGlobalLoader when loading message is null', () => {
      const baseConfig: AxiosRequestConfig = { method: 'get', url: '/api/test' };
      const result = withLoadingMessage(baseConfig, null);

      expect(result).toEqual({
        method: 'get',
        url: '/api/test',
        skipGlobalLoader: true,
      });
    });

    it('should work with empty config', () => {
      const result = withLoadingMessage({}, 'Test message...');

      expect(result).toEqual({
        loadingMessage: 'Test message...',
      });
    });

    it('should work with no config parameter', () => {
      const result = withLoadingMessage(undefined as any, 'Test message...');

      expect(result).toEqual({
        loadingMessage: 'Test message...',
      });
    });
  });
});

describe('withLoading', () => {
  beforeEach(() => {
    mockSetLoading.mockClear();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should set loading to true with default message, execute operation, and clean up on success', async () => {
    const mockOperation = vi.fn().mockResolvedValue('success result');

    const result = await withLoading(mockOperation);

    expect(mockSetLoading).toHaveBeenCalledTimes(2);
    expect(mockSetLoading).toHaveBeenNthCalledWith(1, true, 'Processing...');
    expect(mockSetLoading).toHaveBeenNthCalledWith(2, false);
    expect(mockOperation).toHaveBeenCalledOnce();
    expect(result).toBe('success result');
  });

  it('should set loading to true with custom message when provided', async () => {
    const mockOperation = vi.fn().mockResolvedValue('result');
    const customMessage = 'Custom loading message...';

    await withLoading(mockOperation, customMessage);

    expect(mockSetLoading).toHaveBeenNthCalledWith(1, true, customMessage);
    expect(mockSetLoading).toHaveBeenNthCalledWith(2, false);
  });

  it('should clean up loading state even when operation throws an error', async () => {
    const error = new Error('Operation failed');
    const mockOperation = vi.fn().mockRejectedValue(error);

    await expect(withLoading(mockOperation)).rejects.toThrow('Operation failed');

    expect(mockSetLoading).toHaveBeenCalledTimes(2);
    expect(mockSetLoading).toHaveBeenNthCalledWith(1, true, 'Processing...');
    expect(mockSetLoading).toHaveBeenNthCalledWith(2, false);
  });

  it('should clean up loading state even when operation throws a non-Error', async () => {
    const mockOperation = vi.fn().mockRejectedValue('string error');

    await expect(withLoading(mockOperation)).rejects.toBe('string error');

    expect(mockSetLoading).toHaveBeenCalledTimes(2);
    expect(mockSetLoading).toHaveBeenNthCalledWith(1, true, 'Processing...');
    expect(mockSetLoading).toHaveBeenNthCalledWith(2, false);
  });

  it('should handle operations that return different data types', async () => {
    // Test with object
    const objectResult = { id: 1, name: 'test' };
    const mockObjectOperation = vi.fn().mockResolvedValue(objectResult);
    const objResult = await withLoading(mockObjectOperation);
    expect(objResult).toEqual(objectResult);

    // Test with array
    const arrayResult = [1, 2, 3];
    const mockArrayOperation = vi.fn().mockResolvedValue(arrayResult);
    const arrResult = await withLoading(mockArrayOperation);
    expect(arrResult).toEqual(arrayResult);

    // Test with null/undefined
    const mockNullOperation = vi.fn().mockResolvedValue(null);
    const nullResult = await withLoading(mockNullOperation);
    expect(nullResult).toBeNull();

    // Reset mock to check all calls
    mockSetLoading.mockClear();

    const mockUndefinedOperation = vi.fn().mockResolvedValue(undefined);
    const undefinedResult = await withLoading(mockUndefinedOperation);
    expect(undefinedResult).toBeUndefined();

    // Verify loading state was managed correctly for the last operation
    expect(mockSetLoading).toHaveBeenCalledTimes(2);
    expect(mockSetLoading).toHaveBeenNthCalledWith(1, true, 'Processing...');
    expect(mockSetLoading).toHaveBeenNthCalledWith(2, false);
  });

  it('should maintain loading state isolation between concurrent calls', async () => {
    const operation1 = vi
      .fn()
      .mockImplementation(() => new Promise(resolve => setTimeout(() => resolve('result1'), 50)));
    const operation2 = vi
      .fn()
      .mockImplementation(() => new Promise(resolve => setTimeout(() => resolve('result2'), 100)));

    // Start both operations concurrently
    const promise1 = withLoading(operation1, 'Loading 1...');
    const promise2 = withLoading(operation2, 'Loading 2...');

    const [result1, result2] = await Promise.all([promise1, promise2]);

    expect(result1).toBe('result1');
    expect(result2).toBe('result2');

    // Each withLoading call should have set loading true and false
    expect(mockSetLoading).toHaveBeenCalledTimes(4);
    expect(mockSetLoading).toHaveBeenCalledWith(true, 'Loading 1...');
    expect(mockSetLoading).toHaveBeenCalledWith(true, 'Loading 2...');
    expect(mockSetLoading).toHaveBeenCalledWith(false);
  });

  it('should handle synchronous operations that complete immediately', async () => {
    const mockSyncOperation = vi.fn().mockResolvedValue('immediate result');

    const result = await withLoading(mockSyncOperation);

    expect(result).toBe('immediate result');
    expect(mockSetLoading).toHaveBeenCalledTimes(2);
    expect(mockSetLoading).toHaveBeenNthCalledWith(1, true, 'Processing...');
    expect(mockSetLoading).toHaveBeenNthCalledWith(2, false);
  });

  it('should preserve the original error when operation fails', async () => {
    const originalError = new Error('Original error message');
    originalError.name = 'CustomError';
    const mockOperation = vi.fn().mockRejectedValue(originalError);

    try {
      await withLoading(mockOperation, 'Custom message...');
      expect.fail('Should have thrown an error');
    } catch (error) {
      expect(error).toBe(originalError);
      expect(error).toBeInstanceOf(Error);
      expect((error as Error).message).toBe('Original error message');
      expect((error as Error).name).toBe('CustomError');
    }

    // Verify cleanup still occurred
    expect(mockSetLoading).toHaveBeenNthCalledWith(2, false);
  });
});
