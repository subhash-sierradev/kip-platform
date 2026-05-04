import { beforeEach, describe, expect, it } from 'vitest';

import { useConfluenceWizardState } from '@/composables/useConfluenceWizardState';

describe('useConfluenceWizardState', () => {
  let state: ReturnType<typeof useConfluenceWizardState>;

  beforeEach(() => {
    state = useConfluenceWizardState();
  });

  it('initializes with default form values and step validation', () => {
    expect(state.formData.name).toBe('');
    expect(state.formData.itemType).toBe('DOCUMENT');
    expect(state.formData.connectionMethod).toBe('existing');

    expect(state.stepValidation[1]).toBe(false);
    expect(state.stepValidation[2]).toBe(false);
    expect(state.stepValidation[3]).toBe(false);
    expect(state.stepValidation[4]).toBe(false);
    expect(state.stepValidation[5]).toBe(true);
  });

  it('updates unified integration data and keeps dynamic fields for dynamic subtype', () => {
    state.updateUnifiedIntegrationStepData({
      name: 'Confluence Sync',
      description: 'Sync docs',
      itemType: 'DOCUMENT',
      subType: 'DOCUMENT_FINAL_DYNAMIC',
      subTypeLabel: 'Final Dynamic',
      dynamicDocument: 'Invoice',
      dynamicDocumentLabel: 'Invoice Label',
    });

    expect(state.formData.name).toBe('Confluence Sync');
    expect(state.formData.subType).toBe('DOCUMENT_FINAL_DYNAMIC');
    expect(state.formData.dynamicDocument).toBe('Invoice');
    expect(state.formData.dynamicDocumentLabel).toBe('Invoice Label');
  });

  it('clears dynamic fields for non-dynamic subtype', () => {
    state.updateUnifiedIntegrationStepData({
      name: 'Confluence Sync',
      description: 'Sync docs',
      itemType: 'DOCUMENT',
      subType: 'STATIC_TYPE',
      subTypeLabel: 'Static',
      dynamicDocument: 'Should Clear',
      dynamicDocumentLabel: 'Should Clear',
    });

    expect(state.formData.dynamicDocument).toBe('');
    expect(state.formData.dynamicDocumentLabel).toBe('');
  });

  it('updates schedule and defaults month-end flag when omitted', () => {
    state.updateScheduleData({
      executionDate: '2026-03-19',
      executionTime: '03:30',
      frequencyPattern: 'DAILY',
      dailyFrequency: '24',
      selectedDays: ['MON'],
      selectedMonths: [1],
      cronExpression: '0 30 3 * * *',
      isExecuteOnMonthEnd: undefined,
    });

    expect(state.formData.executionDate).toBe('2026-03-19');
    expect(state.formData.executionTime).toBe('03:30');
    expect(state.formData.isExecuteOnMonthEnd).toBe(false);
  });

  it('preserves the month-end flag and exposes schedule computed fields when provided', () => {
    state.updateScheduleData({
      executionDate: '2026-04-01',
      executionTime: '11:45',
      frequencyPattern: 'MONTHLY',
      dailyFrequency: '24',
      selectedDays: ['MON'],
      selectedMonths: [1, 4],
      cronExpression: '0 45 11 1 * ?',
      isExecuteOnMonthEnd: true,
      businessTimeZone: 'UTC',
      timeCalculationMode: 'FIXED_DAY_BOUNDARY',
    });

    expect(state.formData.isExecuteOnMonthEnd).toBe(true);
    expect(state.scheduleData.value).toEqual({
      executionDate: '2026-04-01',
      executionTime: '11:45',
      frequencyPattern: 'MONTHLY',
      dailyFrequency: '24',
      selectedDays: ['MON'],
      selectedMonths: [1, 4],
      isExecuteOnMonthEnd: true,
      cronExpression: '0 45 11 1 * ?',
      businessTimeZone: 'UTC',
      timeCalculationMode: 'FIXED_DAY_BOUNDARY',
    });
  });

  it('updates page config and reflects computed data', () => {
    state.updatePageConfigData({
      confluenceSpaceKey: 'SPACE',
      confluenceSpaceLabel: 'My Space (SPACE)',
      confluenceSpaceKeyFolderKey: 'SPACE/FOLDER',
      confluenceSpaceFolderLabel: 'Parent > Folder',
      languageCodes: ['en', 'fr'],
      reportNameTemplate: 'Daily {date}',
      includeTableOfContents: false,
    });

    expect(state.pageConfigData.value).toEqual({
      confluenceSpaceKey: 'SPACE',
      confluenceSpaceLabel: 'My Space (SPACE)',
      confluenceSpaceKeyFolderKey: 'SPACE/FOLDER',
      confluenceSpaceFolderLabel: 'Parent > Folder',
      languageCodes: ['en', 'fr'],
      reportNameTemplate: 'Daily {date}',
      includeTableOfContents: false,
    });
  });

  it('tracks pending base URL in genericConnectionData', () => {
    state.handleConnectionDataUpdate({
      connectionMethod: 'new',
      baseUrl: 'https://acme.atlassian.net/wiki',
      credentialType: 'BASIC_AUTH',
      username: 'user@acme.com',
      password: 'token',
      connectionName: 'Confluence Conn',
      connected: false,
      existingConnectionId: undefined,
      createdConnectionId: 'created-1',
    });

    expect(state.genericConnectionData.value.baseUrl).toBe('https://acme.atlassian.net/wiki');
  });

  it('falls back empty credentials and exposes computed connection data', () => {
    state.handleConnectionDataUpdate({
      connectionMethod: 'existing',
      baseUrl: '',
      credentialType: 'BASIC_AUTH',
      username: undefined as any,
      password: undefined as any,
      connectionName: 'Existing Connection',
      connected: false,
      existingConnectionId: 'existing-7',
      createdConnectionId: undefined,
    });

    expect(state.genericConnectionData.value).toEqual({
      connectionMethod: 'existing',
      baseUrl: '',
      credentialType: 'BASIC_AUTH',
      username: '',
      password: '',
      connectionName: 'Existing Connection',
      connected: true,
      existingConnectionId: 'existing-7',
      createdConnectionId: undefined,
    });
  });

  it('computes active connection id for existing and created methods', () => {
    state.formData.connectionMethod = 'existing';
    state.formData.existingConnectionId = 'existing-1';
    expect(state.activeConnectionId.value).toBe('existing-1');

    state.formData.connectionMethod = 'new';
    state.formData.createdConnectionId = 'created-1';
    expect(state.activeConnectionId.value).toBe('created-1');
  });

  it('returns null when no active connection id is available', () => {
    state.formData.connectionMethod = 'existing';
    state.formData.existingConnectionId = '';
    expect(state.activeConnectionId.value).toBeNull();

    state.formData.connectionMethod = 'new';
    state.formData.createdConnectionId = '';
    expect(state.activeConnectionId.value).toBeNull();
  });

  it('exposes unified integration computed fields after updates', () => {
    state.updateUnifiedIntegrationStepData({
      name: 'Name',
      description: 'Description',
      itemType: 'DOCUMENT',
      subType: 'DOCUMENT_DRAFT_DYNAMIC',
      subTypeLabel: 'Draft Dynamic',
      dynamicDocument: 'Case Report',
      dynamicDocumentLabel: 'Case Report Label',
    });

    expect(state.unifiedIntegrationStepData.value).toEqual({
      name: 'Name',
      description: 'Description',
      itemType: 'DOCUMENT',
      subType: 'DOCUMENT_DRAFT_DYNAMIC',
      subTypeLabel: 'Draft Dynamic',
      dynamicDocument: 'Case Report',
      dynamicDocumentLabel: 'Case Report Label',
    });
  });

  it('updates step validation and fully resets wizard state', () => {
    state.setStepValidation(2, true);
    expect(state.stepValidation[2]).toBe(true);

    state.formData.name = 'Changed';
    state.handleConnectionDataUpdate({
      connectionMethod: 'new',
      baseUrl: 'https://acme.atlassian.net/wiki',
      credentialType: 'BASIC_AUTH',
      username: 'u',
      password: 'p',
      connectionName: 'Conn',
      connected: false,
      existingConnectionId: undefined,
      createdConnectionId: 'created-1',
    });
    state.resetFormData();

    expect(state.formData.name).toBe('');
    expect(state.formData.createdConnectionId).toBe('');
    expect(state.genericConnectionData.value.baseUrl).toBe('');
    expect(state.stepValidation[1]).toBe(false);
    expect(state.stepValidation[5]).toBe(true);
  });
});
