import { beforeEach, describe, expect, it, vi } from 'vitest';

const toastSpies = vi.hoisted(() => ({
  showError: vi.fn(),
  showSuccess: vi.fn(),
  showWarning: vi.fn(),
}));

const serviceSpies = vi.hoisted(() => ({
  listConfluenceIntegrations: vi.fn(),
  deleteIntegration: vi.fn(),
  createIntegration: vi.fn(),
  updateIntegration: vi.fn(),
  toggleIntegrationStatus: vi.fn(),
  triggerJobExecution: vi.fn(),
  testAndCreateConnection: vi.fn(),
  buildConfluenceConnectionRequest: vi.fn(),
  buildConfluenceIntegrationRequest: vi.fn(),
  handleError: vi.fn(),
}));

vi.mock('@/store/toast', () => ({
  useToastStore: () => toastSpies,
}));

vi.mock('@/api/services/ConfluenceIntegrationService', () => ({
  ConfluenceIntegrationService: {
    listConfluenceIntegrations: serviceSpies.listConfluenceIntegrations,
    deleteIntegration: serviceSpies.deleteIntegration,
    createIntegration: serviceSpies.createIntegration,
    updateIntegration: serviceSpies.updateIntegration,
    toggleIntegrationStatus: serviceSpies.toggleIntegrationStatus,
    triggerJobExecution: serviceSpies.triggerJobExecution,
  },
}));

vi.mock('@/api/services/IntegrationConnectionService', () => ({
  IntegrationConnectionService: {
    testAndCreateConnection: serviceSpies.testAndCreateConnection,
  },
}));

vi.mock('@/utils/confluenceIntegrationMapping', () => ({
  buildConfluenceConnectionRequest: serviceSpies.buildConfluenceConnectionRequest,
  buildConfluenceIntegrationRequest: serviceSpies.buildConfluenceIntegrationRequest,
}));

vi.mock('@/utils/errorHandler', () => ({
  handleError: serviceSpies.handleError,
}));

import {
  useConfluenceIntegrationActions,
  useConfluenceIntegrationEditor,
  useConfluenceIntegrationStatus,
  useConfluenceIntegrationTrigger,
} from '@/composables/useConfluenceIntegrationActions';

describe('useConfluenceIntegrationActions', () => {
  const form = {
    name: 'Confluence Integration',
    description: '',
    itemType: 'DOCUMENT',
    subType: '',
    languageCodes: ['en'],
    reportNameTemplate: 'Template',
    includeTableOfContents: true,
    executionDate: null,
    executionTime: '02:00',
    frequencyPattern: 'DAILY',
    dailyFrequency: '24',
    selectedDays: [],
    selectedMonths: [],
    isExecuteOnMonthEnd: false,
    confluenceSpaceKey: 'ABC',
    username: 'user@acme.com',
    password: 'token',
    connectionMethod: 'existing',
    existingConnectionId: 'existing-1',
  } as any;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads all integrations and returns data', async () => {
    serviceSpies.listConfluenceIntegrations.mockResolvedValueOnce([{ id: '1' }]);
    const actions = useConfluenceIntegrationActions();

    const result = await actions.getAllIntegrations();

    expect(result).toEqual([{ id: '1' }]);
    expect(actions.loading.value).toBe(false);
  });

  it('returns empty list on load failure', async () => {
    serviceSpies.listConfluenceIntegrations.mockRejectedValueOnce(new Error('fail'));
    const actions = useConfluenceIntegrationActions();

    const result = await actions.getAllIntegrations();

    expect(result).toEqual([]);
    expect(serviceSpies.handleError).toHaveBeenCalledWith(
      expect.any(Error),
      'Failed to load Confluence integrations'
    );
  });

  it('deletes integration and shows success toast', async () => {
    serviceSpies.deleteIntegration.mockResolvedValueOnce(undefined);
    const actions = useConfluenceIntegrationActions();

    const ok = await actions.deleteIntegration('id-1');

    expect(ok).toBe(true);
    expect(toastSpies.showSuccess).toHaveBeenCalledWith('Confluence integration deleted');
  });

  it('handles delete failure', async () => {
    serviceSpies.deleteIntegration.mockRejectedValueOnce(new Error('fail'));
    const actions = useConfluenceIntegrationActions();

    const ok = await actions.deleteIntegration('id-1');

    expect(ok).toBe(false);
    expect(serviceSpies.handleError).toHaveBeenCalledWith(
      expect.any(Error),
      'Failed to delete Confluence integration'
    );
  });

  it('creates integration using existing connection id', async () => {
    serviceSpies.buildConfluenceIntegrationRequest.mockReturnValueOnce({ name: 'req' });
    serviceSpies.createIntegration.mockResolvedValueOnce({ id: 'int-1' });
    const actions = useConfluenceIntegrationActions();

    const id = await actions.createIntegrationFromWizard(form, 'https://acme.atlassian.net/wiki');

    expect(id).toBe('int-1');
    expect(serviceSpies.buildConfluenceConnectionRequest).not.toHaveBeenCalled();
    expect(serviceSpies.buildConfluenceIntegrationRequest).toHaveBeenCalledWith(form, 'existing-1');
    expect(toastSpies.showSuccess).toHaveBeenCalledWith(
      'Confluence integration created successfully'
    );
  });

  it('creates connection when needed then creates integration', async () => {
    const newForm = {
      ...form,
      connectionMethod: 'new',
      existingConnectionId: '',
      createdConnectionId: '',
    };
    serviceSpies.buildConfluenceConnectionRequest.mockReturnValueOnce({ request: true });
    serviceSpies.testAndCreateConnection.mockResolvedValueOnce({ id: 'conn-22' });
    serviceSpies.buildConfluenceIntegrationRequest.mockReturnValueOnce({ name: 'req' });
    serviceSpies.createIntegration.mockResolvedValueOnce({ id: 'int-2' });

    const actions = useConfluenceIntegrationActions();
    const id = await actions.createIntegrationFromWizard(
      newForm,
      'https://acme.atlassian.net/wiki'
    );

    expect(id).toBe('int-2');
    expect(serviceSpies.testAndCreateConnection).toHaveBeenCalledWith({
      requestBody: { request: true },
    });
    expect(serviceSpies.buildConfluenceIntegrationRequest).toHaveBeenCalledWith(newForm, 'conn-22');
  });

  it('handles duplicate-name conflict during create', async () => {
    serviceSpies.buildConfluenceIntegrationRequest.mockReturnValueOnce({ name: 'req' });
    serviceSpies.createIntegration.mockRejectedValueOnce({ status: 409 });
    const actions = useConfluenceIntegrationActions();

    const id = await actions.createIntegrationFromWizard(form, 'https://acme.atlassian.net/wiki');

    expect(id).toBe(null);
    expect(toastSpies.showError).toHaveBeenCalledWith(
      'Confluence Integration Name already Exists.'
    );
  });

  it('shows warning when create succeeds but no id is returned', async () => {
    serviceSpies.buildConfluenceIntegrationRequest.mockReturnValueOnce({ name: 'req' });
    serviceSpies.createIntegration.mockResolvedValueOnce({});
    const actions = useConfluenceIntegrationActions();

    const id = await actions.createIntegrationFromWizard(form, 'https://acme.atlassian.net/wiki');

    expect(id).toBe(null);
    expect(toastSpies.showWarning).toHaveBeenCalledWith('Integration created but no ID returned');
  });
});

describe('useConfluenceIntegrationEditor', () => {
  const form = {
    name: 'Confluence Integration',
    description: '',
    itemType: 'DOCUMENT',
    subType: '',
    languageCodes: ['en'],
    reportNameTemplate: 'Template',
    includeTableOfContents: true,
    executionDate: null,
    executionTime: '02:00',
    frequencyPattern: 'DAILY',
    dailyFrequency: '24',
    selectedDays: [],
    selectedMonths: [],
    isExecuteOnMonthEnd: false,
    confluenceSpaceKey: 'ABC',
    username: 'user@acme.com',
    password: 'token',
    connectionMethod: 'existing',
    existingConnectionId: 'existing-1',
  } as any;

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('updates integration using existing connection', async () => {
    serviceSpies.buildConfluenceIntegrationRequest.mockReturnValueOnce({ name: 'req' });
    serviceSpies.updateIntegration.mockResolvedValueOnce(undefined);
    const editor = useConfluenceIntegrationEditor();

    const ok = await editor.updateIntegrationFromWizard(
      'int-1',
      form,
      'https://acme.atlassian.net/wiki'
    );

    expect(ok).toBe(true);
    expect(serviceSpies.updateIntegration).toHaveBeenCalledWith('int-1', { name: 'req' });
    expect(toastSpies.showSuccess).toHaveBeenCalledWith(
      'Confluence integration updated successfully'
    );
  });

  it('creates a new connection during update when method is new', async () => {
    const newForm = {
      ...form,
      connectionMethod: 'new',
      existingConnectionId: '',
      createdConnectionId: '',
    };
    serviceSpies.buildConfluenceConnectionRequest.mockReturnValueOnce({ request: true });
    serviceSpies.testAndCreateConnection.mockResolvedValueOnce({ id: 'conn-2' });
    serviceSpies.buildConfluenceIntegrationRequest.mockReturnValueOnce({ name: 'req' });
    serviceSpies.updateIntegration.mockResolvedValueOnce(undefined);
    const editor = useConfluenceIntegrationEditor();

    const ok = await editor.updateIntegrationFromWizard(
      'int-2',
      newForm,
      'https://acme.atlassian.net/wiki'
    );

    expect(ok).toBe(true);
    expect(serviceSpies.testAndCreateConnection).toHaveBeenCalled();
    expect(serviceSpies.buildConfluenceIntegrationRequest).toHaveBeenCalledWith(newForm, 'conn-2');
  });

  it('handles duplicate-name conflict during update', async () => {
    serviceSpies.buildConfluenceIntegrationRequest.mockReturnValueOnce({ name: 'req' });
    serviceSpies.updateIntegration.mockRejectedValueOnce({ status: 409 });
    const editor = useConfluenceIntegrationEditor();

    const ok = await editor.updateIntegrationFromWizard(
      'int-1',
      form,
      'https://acme.atlassian.net/wiki'
    );

    expect(ok).toBe(false);
    expect(toastSpies.showError).toHaveBeenCalledWith(
      'Confluence Integration Name already Exists.'
    );
  });
});

describe('useConfluenceIntegrationStatus', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns null for invalid integration id', async () => {
    const statusApi = useConfluenceIntegrationStatus();

    const result = await statusApi.toggleIntegrationStatus('', true);

    expect(result).toBe(null);
    expect(toastSpies.showError).toHaveBeenCalledWith('Invalid integration id');
  });

  it('uses API boolean response and shows enabled toast', async () => {
    serviceSpies.toggleIntegrationStatus.mockResolvedValueOnce(true);
    const statusApi = useConfluenceIntegrationStatus();

    const result = await statusApi.toggleIntegrationStatus('id-1', false);

    expect(result).toBe(true);
    expect(toastSpies.showSuccess).toHaveBeenCalledWith(
      'Confluence integration has been enabled and is now active'
    );
  });

  it('falls back to inverting currentEnabled when API does not return boolean', async () => {
    serviceSpies.toggleIntegrationStatus.mockResolvedValueOnce({});
    const statusApi = useConfluenceIntegrationStatus();

    const result = await statusApi.toggleIntegrationStatus('id-1', true);

    expect(result).toBe(false);
    expect(toastSpies.showWarning).toHaveBeenCalledWith(
      'Confluence integration has been disabled and will no longer execute'
    );
  });

  it('returns null when new status cannot be determined', async () => {
    serviceSpies.toggleIntegrationStatus.mockResolvedValueOnce({});
    const statusApi = useConfluenceIntegrationStatus();

    const result = await statusApi.toggleIntegrationStatus('id-1', undefined);

    expect(result).toBe(null);
    expect(toastSpies.showError).toHaveBeenCalledWith(
      'Failed to determine new status. Please refresh and try again.'
    );
  });
});

describe('useConfluenceIntegrationTrigger', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns early and shows error when integration id is missing', async () => {
    const trigger = useConfluenceIntegrationTrigger();

    await trigger.triggerJobExecution('');

    expect(toastSpies.showError).toHaveBeenCalledWith('Invalid integration id');
    expect(serviceSpies.triggerJobExecution).not.toHaveBeenCalled();
  });

  it('triggers execution and shows success toast', async () => {
    serviceSpies.triggerJobExecution.mockResolvedValueOnce(undefined);
    const trigger = useConfluenceIntegrationTrigger();

    await trigger.triggerJobExecution('id-1', 'My Integration');

    expect(toastSpies.showSuccess).toHaveBeenCalledWith(
      'My Integration has been triggered successfully'
    );
  });

  it('extracts and shows parsed API error message from body JSON', async () => {
    serviceSpies.triggerJobExecution.mockRejectedValueOnce({
      body: JSON.stringify({ message: 'Job already in progress' }),
    });
    const trigger = useConfluenceIntegrationTrigger();

    await trigger.triggerJobExecution('id-1', 'My Integration');

    expect(toastSpies.showError).toHaveBeenCalledWith('Job already in progress');
  });

  it('falls back to body string when JSON parse fails', async () => {
    serviceSpies.triggerJobExecution.mockRejectedValueOnce({ body: 'Raw backend error' });
    const trigger = useConfluenceIntegrationTrigger();

    await trigger.triggerJobExecution('id-1');

    expect(toastSpies.showError).toHaveBeenCalledWith('Raw backend error');
  });
});
