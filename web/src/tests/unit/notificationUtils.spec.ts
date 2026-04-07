import { AlertTriangle, CheckCircle, Info, XCircle } from 'lucide-vue-next';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  getToastIcon,
  setGlobalToast,
  type ToastMessage,
  type ToastType,
} from '@/utils/notificationUtils';
import Alert from '@/utils/notificationUtils';

describe('notificationUtils', () => {
  beforeEach(() => {
    // Reset global toast function before each test
    setGlobalToast(null as any);
    vi.clearAllMocks();
  });

  describe('getToastIcon', () => {
    it('returns correct icon for error type', () => {
      expect(getToastIcon('error')).toBe(XCircle);
    });

    it('returns correct icon for warning type', () => {
      expect(getToastIcon('warning')).toBe(AlertTriangle);
    });

    it('returns correct icon for success type', () => {
      expect(getToastIcon('success')).toBe(CheckCircle);
    });

    it('returns correct icon for info type', () => {
      expect(getToastIcon('info')).toBe(Info);
    });
  });

  describe('setGlobalToast', () => {
    it('sets global toast function', () => {
      const mockToastFn = vi.fn();
      setGlobalToast(mockToastFn);

      Alert.success('test message');
      expect(mockToastFn).toHaveBeenCalledWith('test message', 'success');
    });
  });

  describe('Alert.warning', () => {
    it('calls global toast function when available', () => {
      const mockToastFn = vi.fn();
      setGlobalToast(mockToastFn);

      Alert.warning('Warning message');
      expect(mockToastFn).toHaveBeenCalledWith('Warning message', 'warning');
    });

    it('falls back to console when global toast not available', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      Alert.warning('Warning message');
      expect(consoleSpy).toHaveBeenCalledWith('[WARNING] Warning message');

      consoleSpy.mockRestore();
    });
  });

  describe('Alert.success', () => {
    it('calls global toast function when available', () => {
      const mockToastFn = vi.fn();
      setGlobalToast(mockToastFn);

      Alert.success('Success message');
      expect(mockToastFn).toHaveBeenCalledWith('Success message', 'success');
    });

    it('falls back to console when global toast not available', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      Alert.success('Success message');
      expect(consoleSpy).toHaveBeenCalledWith('[SUCCESS] Success message');

      consoleSpy.mockRestore();
    });
  });

  describe('Alert.error', () => {
    it('calls global toast function when available', () => {
      const mockToastFn = vi.fn();
      setGlobalToast(mockToastFn);

      Alert.error('Error message');
      expect(mockToastFn).toHaveBeenCalledWith('Error message', 'error');
    });

    it('falls back to console when global toast not available', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      Alert.error('Error message');
      expect(consoleSpy).toHaveBeenCalledWith('[ERROR] Error message');

      consoleSpy.mockRestore();
    });
  });

  describe('Alert.info', () => {
    it('calls global toast function when available', () => {
      const mockToastFn = vi.fn();
      setGlobalToast(mockToastFn);

      Alert.info('Info message');
      expect(mockToastFn).toHaveBeenCalledWith('Info message', 'info');
    });

    it('falls back to console when global toast not available', () => {
      const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      Alert.info('Info message');
      expect(consoleSpy).toHaveBeenCalledWith('[INFO] Info message');

      consoleSpy.mockRestore();
    });
  });

  describe('ToastMessage interface', () => {
    it('should have correct structure', () => {
      const message: ToastMessage = {
        id: 'test-id',
        message: 'Test message',
        type: 'success',
        timestamp: Date.now(),
      };

      expect(message.id).toBe('test-id');
      expect(message.message).toBe('Test message');
      expect(message.type).toBe('success');
      expect(typeof message.timestamp).toBe('number');
    });
  });

  describe('ToastType', () => {
    it('should accept valid toast types', () => {
      const types: ToastType[] = ['success', 'error', 'warning', 'info'];

      types.forEach(type => {
        expect(['success', 'error', 'warning', 'info']).toContain(type);
      });
    });
  });

  describe('Edge cases and multiple calls', () => {
    it('handles multiple alert calls with global toast', () => {
      const mockToastFn = vi.fn();
      setGlobalToast(mockToastFn);

      Alert.success('Message 1');
      Alert.error('Message 2');
      Alert.warning('Message 3');
      Alert.info('Message 4');

      expect(mockToastFn).toHaveBeenCalledTimes(4);
      expect(mockToastFn).toHaveBeenNthCalledWith(1, 'Message 1', 'success');
      expect(mockToastFn).toHaveBeenNthCalledWith(2, 'Message 2', 'error');
      expect(mockToastFn).toHaveBeenNthCalledWith(3, 'Message 3', 'warning');
      expect(mockToastFn).toHaveBeenNthCalledWith(4, 'Message 4', 'info');
    });

    it('allows changing the global toast function', () => {
      const firstToastFn = vi.fn();
      const secondToastFn = vi.fn();

      setGlobalToast(firstToastFn);
      Alert.error('first');
      expect(firstToastFn).toHaveBeenCalledWith('first', 'error');

      setGlobalToast(secondToastFn);
      Alert.error('second');
      expect(secondToastFn).toHaveBeenCalledWith('second', 'error');
      expect(firstToastFn).toHaveBeenCalledTimes(1);
    });

    it('handles empty message strings', () => {
      const mockToastFn = vi.fn();
      setGlobalToast(mockToastFn);

      Alert.success('');
      Alert.error('');
      Alert.warning('');
      Alert.info('');

      expect(mockToastFn).toHaveBeenCalledTimes(4);
      expect(mockToastFn).toHaveBeenCalledWith('', 'success');
      expect(mockToastFn).toHaveBeenCalledWith('', 'error');
      expect(mockToastFn).toHaveBeenCalledWith('', 'warning');
      expect(mockToastFn).toHaveBeenCalledWith('', 'info');
    });

    it('handles special characters in messages', () => {
      const mockToastFn = vi.fn();
      setGlobalToast(mockToastFn);

      const specialMessage = 'Message with <html> & "quotes" \'apostrophes\'';
      Alert.success(specialMessage);

      expect(mockToastFn).toHaveBeenCalledWith(specialMessage, 'success');
    });

    it('handles long messages', () => {
      const mockToastFn = vi.fn();
      setGlobalToast(mockToastFn);

      const longMessage = 'A'.repeat(1000);
      Alert.error(longMessage);

      expect(mockToastFn).toHaveBeenCalledWith(longMessage, 'error');
    });

    it('handles multiple alert calls without global toast', () => {
      const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

      Alert.success('Message 1');
      Alert.error('Message 2');
      Alert.warning('Message 3');
      Alert.info('Message 4');

      expect(consoleWarnSpy).toHaveBeenCalledTimes(4);
      consoleWarnSpy.mockRestore();
    });
  });
});
