/**
 * Helper utilities for connection verification
 */

import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';

/**
 * Verify an existing connection with API
 */
export async function verifyConnectionWithApi(connectionId: string): Promise<{
  success: boolean;
  message: string;
}> {
  if (!connectionId) {
    return {
      success: false,
      message: 'Please select a connection',
    };
  }

  try {
    const response = await IntegrationConnectionService.testExistingConnection({
      connectionId,
    });

    const success = response.success === true;
    const message =
      response.message ||
      (success ? 'Connection verified successfully' : 'Connection verification failed');

    return { success, message };
  } catch (err: unknown) {
    console.error('Error verifying connection:', err);
    const error = err as { body?: { message?: string }; message?: string };
    const bodyMessage = error.body?.message;
    return {
      success: false,
      message: bodyMessage || error.message || 'Failed to verify connection',
    };
  }
}
