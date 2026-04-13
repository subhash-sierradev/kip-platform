import { beforeEach, describe, expect, it, vi } from 'vitest';

import { handleError } from '@/utils/errorHandler';

// Mock toast store
const mockShowError = vi.fn();
vi.mock('@/store/toast', () => ({
  useToastStore: () => ({
    showError: mockShowError,
  }),
}));

describe('errorHandler', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  describe('handleError', () => {
    it('logs error to console and shows toast with error message', () => {
      const error = new Error('Test error message');
      const fallback = 'Fallback message';

      handleError(error, fallback);

      expect(console.error).toHaveBeenCalledWith(fallback, error);
      expect(mockShowError).toHaveBeenCalledWith('Test error message');
    });

    it('uses fallback message when error is not an Error instance', () => {
      const error = 'String error';
      const fallback = 'Fallback message';

      handleError(error, fallback);

      expect(console.error).toHaveBeenCalledWith(fallback, error);
      expect(mockShowError).toHaveBeenCalledWith(fallback);
    });

    it('handles null error', () => {
      const fallback = 'Fallback message';

      handleError(null, fallback);

      expect(console.error).toHaveBeenCalledWith(fallback, null);
      expect(mockShowError).toHaveBeenCalledWith(fallback);
    });

    it('handles undefined error', () => {
      const fallback = 'Fallback message';

      handleError(undefined, fallback);

      expect(console.error).toHaveBeenCalledWith(fallback, undefined);
      expect(mockShowError).toHaveBeenCalledWith(fallback);
    });

    it('handles number as error', () => {
      const fallback = 'Fallback message';

      handleError(404, fallback);

      expect(console.error).toHaveBeenCalledWith(fallback, 404);
      expect(mockShowError).toHaveBeenCalledWith(fallback);
    });

    it('handles object without message property', () => {
      const error = { code: 500, status: 'Internal Server Error' };
      const fallback = 'Fallback message';

      handleError(error, fallback);

      expect(console.error).toHaveBeenCalledWith(fallback, error);
      expect(mockShowError).toHaveBeenCalledWith(fallback);
    });

    it('extracts message from Error subclass', () => {
      class CustomError extends Error {
        constructor(message: string) {
          super(message);
          this.name = 'CustomError';
        }
      }

      const error = new CustomError('Custom error message');
      const fallback = 'Fallback message';

      handleError(error, fallback);

      expect(console.error).toHaveBeenCalledWith(fallback, error);
      expect(mockShowError).toHaveBeenCalledWith('Custom error message');
    });

    it('prefers backend message from body object', () => {
      const error = {
        status: 400,
        body: {
          message:
            'Confluence does not support CUSTOM frequency with FIXED_DAY_BOUNDARY. Use DAILY, WEEKLY, or MONTHLY frequency.',
        },
        message: 'Bad Request',
      };

      handleError(error, 'Failed to update Confluence integration');

      expect(mockShowError).toHaveBeenCalledWith(
        'Confluence does not support CUSTOM frequency with FIXED_DAY_BOUNDARY. Use DAILY, WEEKLY, or MONTHLY frequency.'
      );
    });

    it('extracts backend message from stringified JSON body', () => {
      const error = {
        status: 400,
        body: JSON.stringify({ message: 'Detailed backend validation message' }),
        message: 'Bad Request',
      };

      handleError(error, 'Failed to update Confluence integration');

      expect(mockShowError).toHaveBeenCalledWith('Detailed backend validation message');
    });

    it('extracts backend message from axios-style response data', () => {
      const error = {
        response: {
          data: {
            message: 'Validation failed from response data',
          },
        },
        message: 'Request failed with status code 400',
      };

      handleError(error, 'Fallback message');

      expect(mockShowError).toHaveBeenCalledWith('Validation failed from response data');
    });

    it('extracts the backend error field from a parsed string body', () => {
      const error = {
        body: JSON.stringify({ error: 'Body error field message' }),
      };

      handleError(error, 'Fallback message');

      expect(mockShowError).toHaveBeenCalledWith('Body error field message');
    });

    it('uses the raw body string when it is not valid JSON', () => {
      const error = {
        body: 'plain backend failure',
      };

      handleError(error, 'Fallback message');

      expect(mockShowError).toHaveBeenCalledWith('plain backend failure');
    });

    it('falls back to statusText when message fields are missing', () => {
      const error = {
        statusText: 'Bad Gateway',
      };

      handleError(error, 'Fallback message');

      expect(mockShowError).toHaveBeenCalledWith('Bad Gateway');
    });

    it('uses the body error property when the body object has no message field', () => {
      const error = {
        body: {
          error: 'Body object error',
        },
      };

      handleError(error, 'Fallback message');

      expect(mockShowError).toHaveBeenCalledWith('Body object error');
    });
  });
});
