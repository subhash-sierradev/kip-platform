/* eslint max-lines-per-function: ["error", { "max": 120 }] */
import { ref } from 'vue';

import type { ConfluenceIntegrationSummaryResponse } from '@/api/models/ConfluenceIntegrationSummaryResponse';
import { ConfluenceIntegrationService } from '@/api/services/ConfluenceIntegrationService';
import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import { useToastStore } from '@/store/toast';
import type { ConfluenceFormData } from '@/types/ConfluenceFormData';
import {
  buildConfluenceConnectionRequest,
  buildConfluenceIntegrationRequest,
} from '@/utils/confluenceIntegrationMapping';
import { handleError } from '@/utils/errorHandler';
import { createWithLoading, withLoadingRef } from '@/utils/loadingUtils';

// eslint-disable-next-line complexity
function parseErrorMessage(error: unknown, defaultPrefix: string): string {
  if (!error || typeof error !== 'object') return defaultPrefix;
  const errorObj = error as Record<string, unknown>;
  if ('body' in errorObj && errorObj.body) {
    try {
      const parsed = typeof errorObj.body === 'string' ? JSON.parse(errorObj.body) : errorObj.body;
      if (typeof parsed === 'object' && parsed !== null) {
        const p = parsed as Record<string, unknown>;
        const msg = (p.message as string) || (p.error as string);
        if (msg) return msg;
      }
    } catch {
      /* ignore */
    }
    if (typeof errorObj.body === 'string') return errorObj.body;
  }
  if ('message' in errorObj && typeof errorObj.message === 'string') return errorObj.message;
  if ('statusText' in errorObj && typeof errorObj.statusText === 'string')
    return errorObj.statusText;
  return defaultPrefix;
}

export function useConfluenceIntegrationActions() {
  const toast = useToastStore();
  const loading = ref(false);

  async function getAllIntegrations(): Promise<ConfluenceIntegrationSummaryResponse[]> {
    return withLoadingRef(loading, async () => {
      try {
        return await ConfluenceIntegrationService.listConfluenceIntegrations();
      } catch (error) {
        handleError(error, 'Failed to load Confluence integrations');
        return [];
      }
    });
  }

  async function deleteIntegration(integrationId: string): Promise<boolean> {
    return withLoadingRef(loading, async () => {
      try {
        await ConfluenceIntegrationService.deleteIntegration(integrationId);
        toast.showSuccess('Confluence integration deleted');
        return true;
      } catch (error) {
        handleError(error, 'Failed to delete Confluence integration');
        return false;
      }
    });
  }

  async function createIntegrationFromWizard(
    form: ConfluenceFormData,
    baseUrl: string
  ): Promise<string | null> {
    return withLoadingRef(loading, async () => {
      try {
        let connectionId: string | undefined;
        const method = form.connectionMethod;
        const existingId = form.existingConnectionId;
        const createdId = form.createdConnectionId;

        if (method === 'existing' && existingId) {
          connectionId = existingId;
        } else if (createdId) {
          connectionId = createdId;
        } else {
          const connectionReq = buildConfluenceConnectionRequest(form, baseUrl);
          const connection = await IntegrationConnectionService.testAndCreateConnection({
            requestBody: connectionReq,
          });
          connectionId = connection?.id;
        }

        if (!connectionId) throw new Error('No connection selected or created');

        const integrationReq = buildConfluenceIntegrationRequest(form, connectionId);
        const created = await ConfluenceIntegrationService.createIntegration(integrationReq);
        const newId = created?.id ?? null;

        if (newId) {
          toast.showSuccess('Confluence integration created successfully');
          return newId;
        }
        toast.showWarning('Integration created but no ID returned');
        return null;
      } catch (error) {
        const status = (error as { status?: number })?.status;
        if (status === 409) {
          toast.showError('Confluence Integration Name already Exists.');
          return null;
        }
        handleError(error, 'Failed to create Confluence integration');
        return null;
      }
    });
  }

  return { loading, getAllIntegrations, deleteIntegration, createIntegrationFromWizard };
}

export function useConfluenceIntegrationEditor() {
  const toast = useToastStore();
  const loading = ref(false);
  const withLoading = createWithLoading(loading);

  async function updateIntegrationFromWizard(
    integrationId: string,
    form: ConfluenceFormData,
    baseUrl: string
  ): Promise<boolean> {
    return withLoading(async () => {
      try {
        let connectionId: string | undefined;
        const method = form.connectionMethod;
        const existingId = form.existingConnectionId;
        const createdId = form.createdConnectionId;

        if (method === 'existing' && existingId) {
          connectionId = existingId;
        } else if (createdId) {
          connectionId = createdId;
        } else if (method === 'new') {
          const connectionReq = buildConfluenceConnectionRequest(form, baseUrl);
          const connection = await IntegrationConnectionService.testAndCreateConnection({
            requestBody: connectionReq,
          });
          connectionId = connection?.id;
        }

        if (!connectionId) throw new Error('No connection selected or created');

        const integrationReq = buildConfluenceIntegrationRequest(form, connectionId);
        await ConfluenceIntegrationService.updateIntegration(integrationId, integrationReq);
        toast.showSuccess('Confluence integration updated successfully');
        return true;
      } catch (error) {
        const status = (error as { status?: number })?.status;
        if (status === 409) {
          toast.showError('Confluence Integration Name already Exists.');
          return false;
        }
        handleError(error, 'Failed to update Confluence integration');
        return false;
      }
    });
  }

  return { loading, updateIntegrationFromWizard };
}

export function useConfluenceIntegrationStatus() {
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
        const apiResult = await ConfluenceIntegrationService.toggleIntegrationStatus(integrationId);
        const newEnabled =
          typeof apiResult === 'boolean'
            ? apiResult
            : typeof currentEnabled === 'boolean'
              ? !currentEnabled
              : null;

        if (newEnabled === null) {
          toast.showError('Failed to determine new status. Please refresh and try again.');
          return null;
        }

        if (newEnabled) {
          toast.showSuccess('Confluence integration has been enabled and is now active');
        } else {
          toast.showWarning('Confluence integration has been disabled and will no longer execute');
        }
        return newEnabled;
      } catch (error) {
        handleError(error, 'Failed to update Confluence integration status');
        return null;
      }
    });
  }

  return { loading, toggleIntegrationStatus };
}

export function useConfluenceIntegrationTrigger() {
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
        await ConfluenceIntegrationService.triggerJobExecution(integrationId);
        const name = integrationName || 'Integration';
        toast.showSuccess(`${name} has been triggered successfully`);
      } catch (error: unknown) {
        const name = integrationName || 'Integration';
        const errorMessage = parseErrorMessage(error, `Failed to trigger ${name}`);
        toast.showError(errorMessage);
      }
    });
  }

  return { loading, triggerJobExecution };
}
