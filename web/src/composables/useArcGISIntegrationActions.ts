/* eslint max-lines-per-function: ["error", { "max": 120 }] */
import { ref } from 'vue';

import type { ArcGISIntegrationSummaryResponse } from '@/api/models/ArcGISIntegrationSummaryResponse';
import { ArcGISIntegrationService } from '@/api/services/ArcGISIntegrationService';
import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import { useToastStore } from '@/store/toast';
import type { ArcGISFormData } from '@/types/ArcGISFormData';
import { buildConnectionRequest, buildIntegrationRequest } from '@/utils/arcgisIntegrationMapping';
import { handleError } from '@/utils/errorHandler';
import { createWithLoading, withLoadingRef } from '@/utils/loadingUtils';

// Helper functions
function determineNewStatus(apiResult: unknown, currentEnabled?: boolean): boolean | null {
  if (typeof apiResult === 'boolean') {
    return apiResult;
  }
  if (typeof currentEnabled === 'boolean') {
    return !currentEnabled;
  }
  return null;
}

function showStatusToast(toast: ReturnType<typeof useToastStore>, enabled: boolean): void {
  if (enabled) {
    toast.showSuccess('ArcGIS integration has been enabled and is now active');
  } else {
    toast.showWarning('ArcGIS integration has been disabled and will no longer execute');
  }
}

function tryParseErrorBody(body: string | Record<string, unknown>): string | null {
  try {
    const errorBody = typeof body === 'string' ? JSON.parse(body) : body;

    if (typeof errorBody === 'object' && errorBody !== null) {
      const bodyObj = errorBody as Record<string, unknown>;
      return (bodyObj.message as string) || (bodyObj.error as string) || null;
    }
  } catch {
    // If parsing fails, return null
  }

  return null;
}

function parseErrorMessage(error: unknown, defaultPrefix: string): string {
  if (!error || typeof error !== 'object') {
    return defaultPrefix;
  }

  const errorObj = error as Record<string, unknown>;

  // Try to extract from body
  if ('body' in errorObj && errorObj.body) {
    const parsedMessage = tryParseErrorBody(errorObj.body as string | Record<string, unknown>);
    if (parsedMessage) {
      return parsedMessage;
    }

    if (typeof errorObj.body === 'string') {
      return errorObj.body;
    }
  }

  // Try standard Error message
  if ('message' in errorObj && typeof errorObj.message === 'string') {
    return errorObj.message;
  }

  // Try HTTP statusText
  if ('statusText' in errorObj && typeof errorObj.statusText === 'string') {
    return errorObj.statusText;
  }

  return defaultPrefix;
}

// ArcGIS Integration management composable
export function useArcGISIntegrationActions() {
  const toast = useToastStore();
  const loading = ref(false);

  /* ----------------------------------------------------
   * Helpers
   * -------------------------------------------------- */

  /* ----------------------------------------------------
   * API actions
   * -------------------------------------------------- */
  async function getAllIntegrations(): Promise<ArcGISIntegrationSummaryResponse[]> {
    return withLoadingRef(loading, async () => {
      try {
        return await ArcGISIntegrationService.listArcGISIntegrations();
      } catch (error) {
        handleError(error, 'Failed to load ArcGIS integrations');
        return [];
      }
    });
  }

  async function deleteIntegration(integrationId: string): Promise<boolean> {
    return withLoadingRef(loading, async () => {
      try {
        await ArcGISIntegrationService.deleteIntegration(integrationId);
        toast.showSuccess('ArcGIS integration deleted');
        return true;
      } catch (error) {
        handleError(error, 'Failed to delete ArcGIS integration');
        return false;
      }
    });
  }

  async function createIntegrationFromWizard(form: ArcGISFormData): Promise<string | null> {
    return withLoadingRef(loading, async () => {
      try {
        // Resolve connection id: use existing if selected, else create/test
        let connectionId: string | undefined = undefined;
        type ConnectionMethod = 'new' | 'existing';
        const method = (form as { connectionMethod?: ConnectionMethod }).connectionMethod;
        const existingId = (form as { existingConnectionId?: string }).existingConnectionId;
        const createdId = (form as { createdConnectionId?: string }).createdConnectionId;

        if (method === 'existing' && existingId) {
          connectionId = existingId;
        } else {
          // Prefer the connectionId created during Step 4 test flow
          if (createdId) {
            connectionId = createdId;
          } else {
            const connectionReq = buildConnectionRequest(form);
            const connection = await IntegrationConnectionService.testAndCreateConnection({
              requestBody: connectionReq,
            });
            connectionId = connection?.id;
          }
        }
        if (!connectionId) throw new Error('No connection selected or created');

        // 2) Build and POST ArcGIS integration
        const integrationReq = buildIntegrationRequest(form, connectionId);
        const created = await ArcGISIntegrationService.createIntegration(integrationReq);
        const newId = created?.id ?? null;

        if (newId) {
          toast.showSuccess('ArcGIS integration created successfully');
          return newId;
        }
        toast.showWarning('Integration created but no ID returned');
        return null;
      } catch (error) {
        // Show a specific toast when backend enforces unique name (HTTP 409)
        const status = (error as { status?: number })?.status;
        if (status === 409) {
          toast.showError('ArcGIS Integration Name already Exists.');
          return null;
        }
        handleError(error, 'Failed to create ArcGIS integration');
        return null;
      }
    });
  }

  return {
    loading,
    getAllIntegrations,
    deleteIntegration,
    createIntegrationFromWizard,
  };
}

// Separate composable for editing/updating integration to keep functions small
export function useArcGISIntegrationEditor() {
  const toast = useToastStore();
  const loading = ref(false);
  const withLoading = createWithLoading(loading);

  async function updateIntegrationFromWizard(
    integrationId: string,
    form: ArcGISFormData
  ): Promise<boolean> {
    return withLoading(async () => {
      try {
        let connectionId: string | undefined = undefined;
        type ConnectionMethod = 'new' | 'existing';
        const method = (form as { connectionMethod?: ConnectionMethod }).connectionMethod;
        const existingId = (form as { existingConnectionId?: string }).existingConnectionId;
        const createdId = (form as { createdConnectionId?: string }).createdConnectionId;

        if (method === 'existing' && existingId) {
          connectionId = existingId;
        } else if (createdId) {
          connectionId = createdId;
        } else if (method === 'new') {
          const connectionReq = buildConnectionRequest(form);
          const connection = await IntegrationConnectionService.testAndCreateConnection({
            requestBody: connectionReq,
          });
          connectionId = connection?.id;
        }

        if (!connectionId) throw new Error('No connection selected or created');

        const integrationReq = buildIntegrationRequest(form, connectionId);
        await ArcGISIntegrationService.updateIntegration(integrationId, integrationReq);
        toast.showSuccess('ArcGIS integration updated successfully');
        return true;
      } catch (error) {
        const status = (error as { status?: number })?.status;
        if (status === 409) {
          toast.showError('ArcGIS Integration Name already Exists.');
          return false;
        }
        handleError(error, 'Failed to update ArcGIS integration');
        return false;
      }
    });
  }

  return {
    loading,
    updateIntegrationFromWizard,
  };
}

// Separate composable for manual triggers
export function useArcGISIntegrationTrigger() {
  const toast = useToastStore();
  const loading = ref(false);
  const withLoading = createWithLoading(loading);

  async function triggerJobExecution(
    integrationId: string,
    integrationName?: string
  ): Promise<void> {
    if (!integrationId) {
      toast.showError('Invalid integration id');
      return;
    }

    return withLoading(async () => {
      try {
        await ArcGISIntegrationService.triggerJobExecution(integrationId);
        const name = integrationName || 'Integration';
        toast.showSuccess(`${name} has been triggered successfully`);
      } catch (error: unknown) {
        const name = integrationName || 'Integration';
        console.error('Failed to trigger ArcGIS integration:', error);

        const errorMessage = parseErrorMessage(error, `Failed to trigger ${name}`);
        toast.showError(errorMessage);
      }
    });
  }

  return {
    loading,
    triggerJobExecution,
  };
}

// Separate composable for status management
export function useArcGISIntegrationStatus() {
  const toast = useToastStore();
  const loading = ref(false);
  const withLoading = createWithLoading(loading);

  async function toggleIntegrationStatus(
    integrationId: string,
    currentEnabled?: boolean
  ): Promise<boolean | null> {
    if (!integrationId) {
      toast.showError('Invalid integration id');
      return null;
    }
    return withLoading(async () => {
      try {
        const apiResult =
          await ArcGISIntegrationService.toggleArcGISIntegrationActive(integrationId);
        const newEnabled = determineNewStatus(apiResult, currentEnabled);

        if (newEnabled === null) {
          console.error(
            'toggleIntegrationStatus: currentEnabled is undefined and API did not return a boolean'
          );
          toast.showError('Failed to determine new status. Please refresh and try again.');
          return null;
        }

        showStatusToast(toast, newEnabled);
        return newEnabled;
      } catch (error) {
        console.error('Failed to toggle ArcGIS integration status:', error);
        toast.showError('Failed to update ArcGIS integration status');
        return null;
      }
    });
  }

  return {
    loading,
    toggleIntegrationStatus,
  };
}
