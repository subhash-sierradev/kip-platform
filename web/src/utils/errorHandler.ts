import { useToastStore } from '@/store/toast';

interface ErrorLike {
  message?: unknown;
  statusText?: unknown;
  body?: unknown;
  response?: {
    data?: unknown;
  };
}

function readMessageFromBody(body: unknown): string | null {
  if (!body) {
    return null;
  }

  if (typeof body === 'string') {
    try {
      const parsed = JSON.parse(body) as { message?: string; error?: string };
      return parsed.message || parsed.error || body;
    } catch {
      return body;
    }
  }

  if (typeof body === 'object') {
    const payload = body as { message?: unknown; error?: unknown };
    if (typeof payload.message === 'string' && payload.message.trim()) {
      return payload.message;
    }
    if (typeof payload.error === 'string' && payload.error.trim()) {
      return payload.error;
    }
  }

  return null;
}

function extractErrorMessage(error: unknown, fallbackMessage: string): string {
  if (!error || typeof error !== 'object') {
    return fallbackMessage;
  }

  const errorLike = error as ErrorLike;

  const responseDataMessage = readMessageFromBody(errorLike.response?.data);
  if (responseDataMessage) {
    return responseDataMessage;
  }

  const bodyMessage = readMessageFromBody(errorLike.body);
  if (bodyMessage) {
    return bodyMessage;
  }

  if (typeof errorLike.message === 'string' && errorLike.message.trim()) {
    return errorLike.message;
  }

  if (typeof errorLike.statusText === 'string' && errorLike.statusText.trim()) {
    return errorLike.statusText;
  }

  return fallbackMessage;
}

/**
 * Shared error handler utility function
 * Logs error and displays toast notification
 */
export function handleError(error: unknown, fallbackMessage: string): void {
  console.error(fallbackMessage, error);
  const toast = useToastStore();
  const message = extractErrorMessage(error, fallbackMessage);
  toast.showError(message);
}
