import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

// capture toast spies via closure
const toastSpies = {
  showError: vi.fn(),
  showSuccess: vi.fn(),
  showWarning: vi.fn(),
};

vi.mock('@/store/toast', () => ({
  useToastStore: () => toastSpies,
}));

// mock service methods
vi.mock('@/api/services/ArcGISIntegrationService', () => ({
  ArcGISIntegrationService: {
    listArcGISIntegrations: vi.fn(),
    deleteIntegration: vi.fn(),
    cloneIntegration: vi.fn(),
    createIntegration: vi.fn(),
    updateIntegration: vi.fn(),
    triggerJobExecution: vi.fn(),
    toggleArcGISIntegrationActive: vi.fn(),
  },
}));

vi.mock('@/api/services/IntegrationConnectionService', () => ({
  IntegrationConnectionService: {
    testAndCreateConnection: vi.fn(),
  },
}));

vi.mock('@/utils/arcgisIntegrationMapping', () => ({
  buildConnectionRequest: vi.fn(),
  buildIntegrationRequest: vi.fn(),
}));

vi.mock('@/utils/errorHandler', () => ({
  handleError: vi.fn(),
}));

import { ArcGISIntegrationService } from '@/api/services/ArcGISIntegrationService';
import { IntegrationConnectionService } from '@/api/services/IntegrationConnectionService';
import {
  useArcGISIntegrationActions,
  useArcGISIntegrationEditor,
  useArcGISIntegrationStatus,
  useArcGISIntegrationTrigger,
} from '@/composables/useArcGISIntegrationActions';
import { buildConnectionRequest, buildIntegrationRequest } from '@/utils/arcgisIntegrationMapping';

describe('useArcGISIntegrationActions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getAllIntegrations', () => {
    it('returns data and toggles loading', async () => {
      const items = [{ id: '1' }, { id: '2' }] as any;
      (ArcGISIntegrationService.listArcGISIntegrations as any).mockResolvedValueOnce(items);
      const { loading, getAllIntegrations } = useArcGISIntegrationActions();
      expect(loading.value).toBe(false);
      const resultPromise = getAllIntegrations();
      expect(loading.value).toBe(true);
      const result = await resultPromise;
      expect(result).toEqual(items);
      expect(loading.value).toBe(false);
    });

    it('handles error, returns empty', async () => {
      (ArcGISIntegrationService.listArcGISIntegrations as any).mockRejectedValueOnce(
        new Error('fail')
      );
      const { getAllIntegrations } = useArcGISIntegrationActions();
      const result = await getAllIntegrations();
      expect(result).toEqual([]);
      // handleError is called internally which shows error toast
    });
  });

  describe('deleteIntegration', () => {
    it('success shows toast and returns true', async () => {
      (ArcGISIntegrationService.deleteIntegration as any).mockResolvedValueOnce(undefined);
      const { deleteIntegration } = useArcGISIntegrationActions();
      const ok = await deleteIntegration('abc');
      expect(ok).toBe(true);
      expect(toastSpies.showSuccess).toHaveBeenCalledWith('ArcGIS integration deleted');
    });

    it('error returns false', async () => {
      (ArcGISIntegrationService.deleteIntegration as any).mockRejectedValueOnce(new Error('nope'));
      const { deleteIntegration } = useArcGISIntegrationActions();
      const ok = await deleteIntegration('abc');
      expect(ok).toBe(false);
      // handleError is called internally which shows error toast
    });
  });

  describe('createIntegrationFromWizard', () => {
    it('creates integration with existing connection', async () => {
      const form = {
        connectionMethod: 'existing',
        existingConnectionId: 'conn-123',
        name: 'Test Integration',
      } as any;

      (buildIntegrationRequest as any).mockReturnValue({ name: 'Test Integration' });
      (ArcGISIntegrationService.createIntegration as any).mockResolvedValue({ id: 'int-456' });

      const { createIntegrationFromWizard } = useArcGISIntegrationActions();
      const result = await createIntegrationFromWizard(form);

      expect(result).toBe('int-456');
      expect(buildIntegrationRequest).toHaveBeenCalledWith(form, 'conn-123');
      expect(ArcGISIntegrationService.createIntegration).toHaveBeenCalled();
      expect(toastSpies.showSuccess).toHaveBeenCalledWith(
        'ArcGIS integration created successfully'
      );
    });

    it('creates integration with createdConnectionId', async () => {
      const form = {
        connectionMethod: 'new',
        createdConnectionId: 'conn-789',
        name: 'Test Integration',
      } as any;

      (buildIntegrationRequest as any).mockReturnValue({ name: 'Test Integration' });
      (ArcGISIntegrationService.createIntegration as any).mockResolvedValue({ id: 'int-999' });

      const { createIntegrationFromWizard } = useArcGISIntegrationActions();
      const result = await createIntegrationFromWizard(form);

      expect(result).toBe('int-999');
      expect(buildIntegrationRequest).toHaveBeenCalledWith(form, 'conn-789');
      expect(IntegrationConnectionService.testAndCreateConnection).not.toHaveBeenCalled();
    });

    it('creates new connection when needed', async () => {
      const form = {
        connectionMethod: 'new',
        name: 'Test Integration',
      } as any;

      (buildConnectionRequest as any).mockReturnValue({ secretName: 'conn' });
      (IntegrationConnectionService.testAndCreateConnection as any).mockResolvedValue({
        id: 'new-conn-123',
      });
      (buildIntegrationRequest as any).mockReturnValue({ name: 'Test Integration' });
      (ArcGISIntegrationService.createIntegration as any).mockResolvedValue({ id: 'int-new' });

      const { createIntegrationFromWizard } = useArcGISIntegrationActions();
      const result = await createIntegrationFromWizard(form);

      expect(result).toBe('int-new');
      expect(buildConnectionRequest).toHaveBeenCalledWith(form);
      expect(IntegrationConnectionService.testAndCreateConnection).toHaveBeenCalled();
      expect(buildIntegrationRequest).toHaveBeenCalledWith(form, 'new-conn-123');
    });

    it('handles missing connection ID error', async () => {
      const form = {
        connectionMethod: 'new',
        name: 'Test Integration',
        // No createdConnectionId - will try to create connection
      } as any;

      (buildConnectionRequest as any).mockReturnValue({ secretName: 'test' });
      (IntegrationConnectionService.testAndCreateConnection as any).mockResolvedValue({}); // No ID in response

      const { createIntegrationFromWizard } = useArcGISIntegrationActions();
      const result = await createIntegrationFromWizard(form);

      expect(result).toBe(null);
      // handleError is called when connectionId is undefined
    });

    it('handles 409 conflict error (duplicate name)', async () => {
      const form = {
        connectionMethod: 'existing',
        existingConnectionId: 'conn-123',
        name: 'Duplicate',
      } as any;

      (buildIntegrationRequest as any).mockReturnValue({ name: 'Duplicate' });
      (ArcGISIntegrationService.createIntegration as any).mockRejectedValue({ status: 409 });

      const { createIntegrationFromWizard } = useArcGISIntegrationActions();
      const result = await createIntegrationFromWizard(form);

      expect(result).toBe(null);
      expect(toastSpies.showError).toHaveBeenCalledWith('ArcGIS Integration Name already Exists.');
    });

    it('handles generic creation error', async () => {
      const form = {
        connectionMethod: 'existing',
        existingConnectionId: 'conn-123',
      } as any;

      (buildIntegrationRequest as any).mockReturnValue({});
      (ArcGISIntegrationService.createIntegration as any).mockRejectedValue(
        new Error('Server error')
      );

      const { createIntegrationFromWizard } = useArcGISIntegrationActions();
      const result = await createIntegrationFromWizard(form);

      expect(result).toBe(null);
      // handleError is called for generic errors
    });

    it('handles missing ID in response with warning', async () => {
      const form = {
        connectionMethod: 'existing',
        existingConnectionId: 'conn-123',
      } as any;

      (buildIntegrationRequest as any).mockReturnValue({});
      (ArcGISIntegrationService.createIntegration as any).mockResolvedValue({});

      const { createIntegrationFromWizard } = useArcGISIntegrationActions();
      const result = await createIntegrationFromWizard(form);

      expect(result).toBe(null);
      expect(toastSpies.showWarning).toHaveBeenCalledWith('Integration created but no ID returned');
    });
  });
});

describe('useArcGISIntegrationEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('updateIntegrationFromWizard', () => {
    it('updates integration with existing connection', async () => {
      const form = {
        connectionMethod: 'existing',
        existingConnectionId: 'conn-123',
        name: 'Updated Integration',
      } as any;

      (buildIntegrationRequest as any).mockReturnValue({ name: 'Updated Integration' });
      (ArcGISIntegrationService.updateIntegration as any).mockResolvedValue(undefined);

      const { updateIntegrationFromWizard } = useArcGISIntegrationEditor();
      const result = await updateIntegrationFromWizard('int-456', form);

      expect(result).toBe(true);
      expect(buildIntegrationRequest).toHaveBeenCalledWith(form, 'conn-123');
      expect(ArcGISIntegrationService.updateIntegration).toHaveBeenCalledWith('int-456', {
        name: 'Updated Integration',
      });
      expect(toastSpies.showSuccess).toHaveBeenCalledWith(
        'ArcGIS integration updated successfully'
      );
    });

    it('updates integration with createdConnectionId', async () => {
      const form = {
        connectionMethod: 'new',
        createdConnectionId: 'conn-789',
      } as any;

      (buildIntegrationRequest as any).mockReturnValue({});
      (ArcGISIntegrationService.updateIntegration as any).mockResolvedValue(undefined);

      const { updateIntegrationFromWizard } = useArcGISIntegrationEditor();
      const result = await updateIntegrationFromWizard('int-999', form);

      expect(result).toBe(true);
      expect(buildIntegrationRequest).toHaveBeenCalledWith(form, 'conn-789');
      expect(IntegrationConnectionService.testAndCreateConnection).not.toHaveBeenCalled();
    });

    it('creates new connection for update when method is new', async () => {
      const form = {
        connectionMethod: 'new',
        name: 'Update with new connection',
      } as any;

      (buildConnectionRequest as any).mockReturnValue({ secretName: 'new-conn' });
      (IntegrationConnectionService.testAndCreateConnection as any).mockResolvedValue({
        id: 'new-conn-999',
      });
      (buildIntegrationRequest as any).mockReturnValue({});
      (ArcGISIntegrationService.updateIntegration as any).mockResolvedValue(undefined);

      const { updateIntegrationFromWizard } = useArcGISIntegrationEditor();
      const result = await updateIntegrationFromWizard('int-update', form);

      expect(result).toBe(true);
      expect(buildConnectionRequest).toHaveBeenCalledWith(form);
      expect(IntegrationConnectionService.testAndCreateConnection).toHaveBeenCalled();
      expect(buildIntegrationRequest).toHaveBeenCalledWith(form, 'new-conn-999');
    });

    it('handles missing connection ID error', async () => {
      const form = {
        connectionMethod: 'existing',
        name: 'No connection',
        // Missing existingConnectionId
      } as any;

      const { updateIntegrationFromWizard } = useArcGISIntegrationEditor();
      const result = await updateIntegrationFromWizard('int-fail', form);

      expect(result).toBe(false);
      // handleError is called when connectionId is undefined
    });

    it('handles 409 conflict error on update', async () => {
      const form = {
        connectionMethod: 'existing',
        existingConnectionId: 'conn-123',
        name: 'Duplicate',
      } as any;

      (buildIntegrationRequest as any).mockReturnValue({});
      (ArcGISIntegrationService.updateIntegration as any).mockRejectedValue({ status: 409 });

      const { updateIntegrationFromWizard } = useArcGISIntegrationEditor();
      const result = await updateIntegrationFromWizard('int-dup', form);

      expect(result).toBe(false);
      expect(toastSpies.showError).toHaveBeenCalledWith('ArcGIS Integration Name already Exists.');
    });

    it('handles generic update error', async () => {
      const form = {
        connectionMethod: 'existing',
        existingConnectionId: 'conn-123',
      } as any;

      (buildIntegrationRequest as any).mockReturnValue({});
      (ArcGISIntegrationService.updateIntegration as any).mockRejectedValue(
        new Error('Update failed')
      );

      const { updateIntegrationFromWizard } = useArcGISIntegrationEditor();
      const result = await updateIntegrationFromWizard('int-err', form);

      expect(result).toBe(false);
      // handleError is called for generic errors
    });
  });
});

describe('useArcGISIntegrationTrigger', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('triggerJobExecution', () => {
    it('triggers job execution successfully with name', async () => {
      (ArcGISIntegrationService.triggerJobExecution as any).mockResolvedValue(undefined);

      const { triggerJobExecution } = useArcGISIntegrationTrigger();
      await triggerJobExecution('int-123', 'My Integration');

      expect(ArcGISIntegrationService.triggerJobExecution).toHaveBeenCalledWith('int-123');
      expect(toastSpies.showSuccess).toHaveBeenCalledWith(
        'My Integration has been triggered successfully'
      );
    });

    it('triggers job execution successfully without name', async () => {
      (ArcGISIntegrationService.triggerJobExecution as any).mockResolvedValue(undefined);

      const { triggerJobExecution } = useArcGISIntegrationTrigger();
      await triggerJobExecution('int-456');

      expect(toastSpies.showSuccess).toHaveBeenCalledWith(
        'Integration has been triggered successfully'
      );
    });

    it('handles invalid integration id', async () => {
      const { triggerJobExecution } = useArcGISIntegrationTrigger();
      await triggerJobExecution('');

      expect(ArcGISIntegrationService.triggerJobExecution).not.toHaveBeenCalled();
      expect(toastSpies.showError).toHaveBeenCalledWith('Invalid integration id');
    });

    it('handles trigger error with name', async () => {
      (ArcGISIntegrationService.triggerJobExecution as any).mockRejectedValue(
        new Error('Trigger failed')
      );

      const { triggerJobExecution } = useArcGISIntegrationTrigger();
      await triggerJobExecution('int-789', 'Failed Integration');

      expect(console.error).toHaveBeenCalled();
      expect(toastSpies.showError).toHaveBeenCalledWith('Trigger failed');
    });

    it('handles trigger error without name', async () => {
      (ArcGISIntegrationService.triggerJobExecution as any).mockRejectedValue(
        new Error('Trigger failed')
      );

      const { triggerJobExecution } = useArcGISIntegrationTrigger();
      await triggerJobExecution('int-999');

      expect(toastSpies.showError).toHaveBeenCalledWith('Trigger failed');
    });

    it('extracts error text from JSON body strings, body objects, and status text', async () => {
      const { triggerJobExecution } = useArcGISIntegrationTrigger();

      (ArcGISIntegrationService.triggerJobExecution as any).mockRejectedValueOnce({
        body: JSON.stringify({ message: 'Job already running' }),
      });
      await triggerJobExecution('int-body-json', 'Named Integration');
      expect(toastSpies.showError).toHaveBeenLastCalledWith('Job already running');

      (ArcGISIntegrationService.triggerJobExecution as any).mockRejectedValueOnce({
        body: { error: 'Body object error' },
      });
      await triggerJobExecution('int-body-object', 'Named Integration');
      expect(toastSpies.showError).toHaveBeenLastCalledWith('Body object error');

      (ArcGISIntegrationService.triggerJobExecution as any).mockRejectedValueOnce({
        statusText: 'Gateway Timeout',
      });
      await triggerJobExecution('int-status-text', 'Named Integration');
      expect(toastSpies.showError).toHaveBeenLastCalledWith('Gateway Timeout');
    });

    it('falls back to raw body strings and default trigger prefixes', async () => {
      const { triggerJobExecution } = useArcGISIntegrationTrigger();

      (ArcGISIntegrationService.triggerJobExecution as any).mockRejectedValueOnce({
        body: 'Raw backend error',
      });
      await triggerJobExecution('int-raw-body', 'Named Integration');
      expect(toastSpies.showError).toHaveBeenLastCalledWith('Raw backend error');

      (ArcGISIntegrationService.triggerJobExecution as any).mockRejectedValueOnce('boom');
      await triggerJobExecution('int-fallback');
      expect(toastSpies.showError).toHaveBeenLastCalledWith('Failed to trigger Integration');
    });
  });
});

describe('useArcGISIntegrationStatus', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('toggleIntegrationStatus', () => {
    it('toggles status to enabled when API returns true', async () => {
      (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockResolvedValue(true);

      const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
      const result = await toggleIntegrationStatus('int-123', false);

      expect(result).toBe(true);
      expect(ArcGISIntegrationService.toggleArcGISIntegrationActive).toHaveBeenCalledWith(
        'int-123'
      );
      expect(toastSpies.showSuccess).toHaveBeenCalledWith(
        'ArcGIS integration has been enabled and is now active'
      );
    });

    it('toggles status to disabled when API returns false', async () => {
      (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockResolvedValue(false);

      const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
      const result = await toggleIntegrationStatus('int-456', true);

      expect(result).toBe(false);
      expect(toastSpies.showWarning).toHaveBeenCalledWith(
        'ArcGIS integration has been disabled and will no longer execute'
      );
    });

    it('infers status when API returns non-boolean and currentEnabled is true', async () => {
      (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockResolvedValue({});

      const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
      const result = await toggleIntegrationStatus('int-789', true);

      expect(result).toBe(false);
      expect(toastSpies.showWarning).toHaveBeenCalled();
    });

    it('infers status when API returns non-boolean and currentEnabled is false', async () => {
      (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockResolvedValue({});

      const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
      const result = await toggleIntegrationStatus('int-999', false);

      expect(result).toBe(true);
      expect(toastSpies.showSuccess).toHaveBeenCalled();
    });

    it('handles indeterminate state when API and currentEnabled both undefined', async () => {
      (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockResolvedValue({});

      const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
      const result = await toggleIntegrationStatus('int-unknown');

      expect(result).toBe(null);
      expect(console.error).toHaveBeenCalled();
      expect(toastSpies.showError).toHaveBeenCalledWith(
        'Failed to determine new status. Please refresh and try again.'
      );
    });

    it('handles invalid integration id', async () => {
      const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
      const result = await toggleIntegrationStatus('');

      expect(result).toBe(null);
      expect(ArcGISIntegrationService.toggleArcGISIntegrationActive).not.toHaveBeenCalled();
      expect(toastSpies.showError).toHaveBeenCalledWith('Invalid integration id');
    });

    it('handles toggle API error', async () => {
      (ArcGISIntegrationService.toggleArcGISIntegrationActive as any).mockRejectedValue(
        new Error('Toggle failed')
      );

      const { toggleIntegrationStatus } = useArcGISIntegrationStatus();
      const result = await toggleIntegrationStatus('int-err', true);

      expect(result).toBe(null);
      expect(console.error).toHaveBeenCalled();
      expect(toastSpies.showError).toHaveBeenCalledWith(
        'Failed to update ArcGIS integration status'
      );
    });
  });
});
