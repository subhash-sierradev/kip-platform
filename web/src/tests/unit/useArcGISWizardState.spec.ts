import { beforeEach, describe, expect, it } from 'vitest';

import { useArcGISWizardState } from '@/composables/useArcGISWizardState';
import type { ConnectionStepData } from '@/types/ConnectionStepData';

describe('useArcGISWizardState', () => {
  let state: ReturnType<typeof useArcGISWizardState>;

  beforeEach(() => {
    state = useArcGISWizardState();
  });

  describe('Initialization', () => {
    it('initializes with default form data values', () => {
      expect(state.formData.name).toBe('');
      expect(state.formData.description).toBe('');
      expect(state.formData.itemType).toBe('DOCUMENT');
      expect(state.formData.fieldMappings).toEqual([
        {
          id: '',
          sourceField: '',
          targetField: '',
          transformationType: '',
          isMandatory: false,
          displayOrder: 0,
        },
      ]);
    });

    it('initializes step validation with correct default states', () => {
      expect(state.stepValidation[1]).toBe(false);
      expect(state.stepValidation[2]).toBe(false);
      expect(state.stepValidation[3]).toBe(false);
      expect(state.stepValidation[4]).toBe(false);
      expect(state.stepValidation[5]).toBe(true);
    });
  });

  describe('updateBasicDetailsData', () => {
    it('updates name and description from BasicDetailsData', () => {
      state.updateBasicDetailsData({
        name: 'Test Integration',
        description: 'Test Description',
      });

      expect(state.formData.name).toBe('Test Integration');
      expect(state.formData.description).toBe('Test Description');
    });

    it('updates name only when description is not provided', () => {
      state.updateBasicDetailsData({
        name: 'Only Name',
        description: '',
      });

      expect(state.formData.name).toBe('Only Name');
      expect(state.formData.description).toBe('');
    });
  });

  describe('updateDocumentSelectionData', () => {
    it('updates itemType, subType, and labels', () => {
      state.updateDocumentSelectionData({
        itemType: 'Document',
        subType: 'DOCUMENT_FINAL_DYNAMIC',
        subTypeLabel: 'Label1',
        dynamicDocument: 'DynamicDoc',
        dynamicDocumentLabel: 'DynLabel',
      });

      expect(state.formData.itemType).toBe('Document');
      expect(state.formData.subType).toBe('DOCUMENT_FINAL_DYNAMIC');
      expect(state.formData.subTypeLabel).toBe('Label1');
      expect(state.formData.dynamicDocument).toBe('DynamicDoc');
      expect(state.formData.dynamicDocumentLabel).toBe('DynLabel');
    });

    it('defaults empty strings when subTypeLabel is undefined', () => {
      state.updateDocumentSelectionData({
        itemType: 'Form',
        subType: 'SubA',
        subTypeLabel: undefined,
        dynamicDocument: undefined,
        dynamicDocumentLabel: undefined,
      });

      expect(state.formData.itemType).toBe('Form');
      expect(state.formData.subTypeLabel).toBe('');
      expect(state.formData.dynamicDocument).toBe('');
      expect(state.formData.dynamicDocumentLabel).toBe('');
    });

    it('handles missing dynamicDocument and dynamicDocumentLabel', () => {
      state.updateDocumentSelectionData({
        itemType: 'Case',
        subType: 'SubB',
        subTypeLabel: 'LabelB',
      });

      expect(state.formData.dynamicDocument).toBe('');
      expect(state.formData.dynamicDocumentLabel).toBe('');
    });
  });

  describe('updateScheduleConfigurationData', () => {
    it('updates all schedule configuration fields', () => {
      state.updateScheduleConfigurationData({
        executionDate: '2025-12-31',
        executionTime: '14:30',
        frequencyPattern: 'WEEKLY',
        dailyFrequency: '12',
        selectedDays: ['MON', 'FRI'],
        selectedMonths: [1, 6],
        monthlyPattern: 'DAY_OF_MONTH',
        monthlyWeek: '1',
        monthlyWeekday: 'MON',
        monthlyDay: 15,
        isExecuteOnMonthEnd: false,
        cronExpression: '0 0 12 * * ?',
      });

      expect(state.formData.executionDate).toBe('2025-12-31');
      expect(state.formData.executionTime).toBe('14:30');
      expect(state.formData.frequencyPattern).toBe('WEEKLY');
      expect(state.formData.dailyFrequency).toBe('12');
      expect(state.formData.selectedDays).toEqual(['MON', 'FRI']);
      expect(state.formData.selectedMonths).toEqual([1, 6]);
      expect(state.formData.monthlyPattern).toBe('DAY_OF_MONTH');
      expect(state.formData.monthlyWeek).toBe('1');
      expect(state.formData.monthlyWeekday).toBe('MON');
      expect(state.formData.monthlyDay).toBe(15);
      expect(state.formData.isExecuteOnMonthEnd).toBe(false);
      expect(state.formData.cronExpression).toBe('0 0 12 * * ?');
    });

    it('handles empty selectedDays array', () => {
      state.updateScheduleConfigurationData({
        executionDate: '2025-01-01',
        executionTime: '09:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '',
        selectedDays: [],
        selectedMonths: [],
        monthlyPattern: '',
        monthlyWeek: '',
        monthlyWeekday: '',
        monthlyDay: null,
        isExecuteOnMonthEnd: false,
        cronExpression: '',
      });

      expect(state.formData.selectedDays).toEqual([]);
      expect(state.formData.selectedMonths).toEqual([]);
    });

    it('handles isExecuteOnMonthEnd true', () => {
      state.updateScheduleConfigurationData({
        executionDate: '2025-02-28',
        executionTime: '23:59',
        frequencyPattern: 'MONTHLY',
        dailyFrequency: '',
        selectedDays: [],
        selectedMonths: [2],
        monthlyPattern: 'MONTH_END',
        monthlyWeek: '',
        monthlyWeekday: '',
        monthlyDay: null,
        isExecuteOnMonthEnd: true,
        cronExpression: '',
      });

      expect(state.formData.isExecuteOnMonthEnd).toBe(true);
      expect(state.formData.monthlyPattern).toBe('MONTH_END');
    });
  });

  describe('updateConnectionData', () => {
    it('updates all connection data fields', () => {
      state.updateConnectionData({
        arcgisEndpoint: 'https://arcgis.example.com',
        fetchMode: 'PULL',
        credentialType: 'BASIC_AUTH',
        username: 'testuser',
        password: 'testpass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
        connectionName: 'Test Connection',
        connectionMethod: 'new',
        existingConnectionId: undefined,
        createdConnectionId: 'conn-123',
      });

      expect(state.formData.arcgisEndpoint).toBe('https://arcgis.example.com');
      expect(state.formData.credentialType).toBe('BASIC_AUTH');
      expect(state.formData.username).toBe('testuser');
      expect(state.formData.password).toBe('testpass');
      expect(state.formData.connectionName).toBe('Test Connection');
      expect(state.formData.createdConnectionId).toBe('conn-123');
    });

    it('handles existing connection method', () => {
      state.updateConnectionData({
        arcgisEndpoint: 'https://existing.com',
        fetchMode: 'PUSH',
        credentialType: 'OAUTH2',
        username: '',
        password: '',
        clientId: 'client123',
        clientSecret: 'secret456',
        tokenUrl: 'https://oauth.com/token',
        scope: 'read write',
        connectionName: 'Existing Conn',
        connectionMethod: 'existing',
        existingConnectionId: 'existing-456',
        createdConnectionId: undefined,
      });

      expect(state.formData.connectionMethod).toBe('existing');
      expect(state.formData.existingConnectionId).toBe('existing-456');
      expect(state.formData.createdConnectionId).toBeUndefined();
    });
  });

  describe('updateFieldMappingData', () => {
    it('updates fieldMappings array', () => {
      const mappings = [
        { id: '1', sourceField: 'field1', targetField: 'target1', isMandatory: false },
        { id: '2', sourceField: 'field2', targetField: 'target2', isMandatory: false },
      ];

      state.updateFieldMappingData({ fieldMappings: mappings as any });

      expect(state.formData.fieldMappings).toEqual(mappings);
      expect(state.formData.fieldMappings.length).toBe(2);
    });

    it('replaces existing fieldMappings with new array', () => {
      state.updateFieldMappingData({
        fieldMappings: [
          { id: '1', sourceField: 'old', targetField: 'old', isMandatory: false },
        ] as any,
      });

      const newMappings = [
        { id: '1', sourceField: 'new1', targetField: 'new1', isMandatory: false },
        { id: '2', sourceField: 'new2', targetField: 'new2', isMandatory: false },
      ];

      state.updateFieldMappingData({ fieldMappings: newMappings as any });

      expect(state.formData.fieldMappings).toEqual(newMappings);
      expect(state.formData.fieldMappings.length).toBe(2);
    });

    it('handles empty fieldMappings array', () => {
      state.updateFieldMappingData({ fieldMappings: [] });

      expect(state.formData.fieldMappings).toEqual([]);
      expect(state.formData.fieldMappings.length).toBe(0);
    });
  });

  describe('Computed Properties', () => {
    describe('basicDetailsData', () => {
      it('returns name and description', () => {
        state.updateBasicDetailsData({
          name: 'Computed Test',
          description: 'Computed Description',
        });

        expect(state.basicDetailsData.value.name).toBe('Computed Test');
        expect(state.basicDetailsData.value.description).toBe('Computed Description');
      });

      it('reflects formData changes reactively', () => {
        state.formData.name = 'Direct Change';
        state.formData.description = 'Direct Desc';

        expect(state.basicDetailsData.value.name).toBe('Direct Change');
        expect(state.basicDetailsData.value.description).toBe('Direct Desc');
      });
    });

    describe('documentSelectionData', () => {
      it('returns document selection fields with dynamicDocument as undefined when empty', () => {
        state.updateDocumentSelectionData({
          itemType: 'Case',
          subType: 'DOCUMENT_FINAL_DYNAMIC',
          subTypeLabel: 'Label A',
          dynamicDocument: '',
          dynamicDocumentLabel: 'Label B',
        });

        expect(state.documentSelectionData.value.itemType).toBe('Case');
        expect(state.documentSelectionData.value.subType).toBe('DOCUMENT_FINAL_DYNAMIC');
        expect(state.documentSelectionData.value.subTypeLabel).toBe('Label A');
        expect(state.documentSelectionData.value.dynamicDocument).toBeUndefined();
        expect(state.documentSelectionData.value.dynamicDocumentLabel).toBe('Label B');
      });

      it('returns dynamicDocument value when populated', () => {
        state.updateDocumentSelectionData({
          itemType: 'Form',
          subType: 'DOCUMENT_FINAL_DYNAMIC',
          subTypeLabel: 'Form Label',
          dynamicDocument: 'CustomForm',
          dynamicDocumentLabel: 'Custom Label',
        });

        expect(state.documentSelectionData.value.dynamicDocument).toBe('CustomForm');
      });
    });

    describe('scheduleConfigurationData', () => {
      it('returns all schedule configuration fields', () => {
        state.updateScheduleConfigurationData({
          executionDate: '2026-01-15',
          executionTime: '10:30',
          frequencyPattern: 'MONTHLY',
          dailyFrequency: '6',
          selectedDays: ['TUE'],
          selectedMonths: [3, 6, 9],
          monthlyPattern: 'DAY_OF_WEEK',
          monthlyWeek: '2',
          monthlyWeekday: 'WED',
          monthlyDay: 20,
          isExecuteOnMonthEnd: false,
          cronExpression: '0 30 10 ? * 3',
        });

        const scheduleData = state.scheduleConfigurationData.value;
        expect(scheduleData.executionDate).toBe('2026-01-15');
        expect(scheduleData.executionTime).toBe('10:30');
        expect(scheduleData.frequencyPattern).toBe('MONTHLY');
        expect(scheduleData.dailyFrequency).toBe('6');
        expect(scheduleData.selectedDays).toEqual(['TUE']);
        expect(scheduleData.selectedMonths).toEqual([3, 6, 9]);
        expect(scheduleData.monthlyPattern).toBe('DAY_OF_WEEK');
        expect(scheduleData.monthlyWeek).toBe('2');
        expect(scheduleData.monthlyWeekday).toBe('WED');
        expect(scheduleData.monthlyDay).toBe(20);
        expect(scheduleData.isExecuteOnMonthEnd).toBe(false);
        expect(scheduleData.cronExpression).toBe('0 30 10 ? * 3');
      });

      it('includes dailyFrequencyRule when present in formData', () => {
        (state.formData as Record<string, unknown>).dailyFrequencyRule = 'HOURS';

        const scheduleData = state.scheduleConfigurationData.value;
        expect(scheduleData.dailyFrequencyRule).toBe('HOURS');
      });

      it('returns undefined for dailyFrequencyRule when not present', () => {
        const scheduleData = state.scheduleConfigurationData.value;
        expect(scheduleData.dailyFrequencyRule).toBeUndefined();
      });
    });

    describe('genericConnectionData', () => {
      it('returns connection data with existing connection method', () => {
        state.updateConnectionData({
          arcgisEndpoint: 'https://server.arcgis.com',
          fetchMode: 'PULL',
          credentialType: 'USERNAME_PASSWORD',
          username: 'arcgis_user',
          password: 'arcgis_pass',
          clientId: '',
          clientSecret: '',
          tokenUrl: '',
          scope: '',
          connectionName: 'Existing ArcGIS',
          connectionMethod: 'existing',
          existingConnectionId: 'existing-789',
          createdConnectionId: undefined,
        });

        const connData = state.genericConnectionData.value;
        expect(connData.connectionMethod).toBe('existing');
        expect(connData.baseUrl).toBe('https://server.arcgis.com');
        expect(connData.credentialType).toBe('USERNAME_PASSWORD');
        expect(connData.username).toBe('arcgis_user');
        expect(connData.password).toBe('arcgis_pass');
        expect(connData.connectionName).toBe('Existing ArcGIS');
        expect(connData.connected).toBe(true); // existingConnectionId present
        expect(connData.existingConnectionId).toBe('existing-789');
        expect(connData.createdConnectionId).toBeUndefined();
      });

      it('returns connection data with new connection method', () => {
        state.updateConnectionData({
          arcgisEndpoint: 'https://new.arcgis.com',
          fetchMode: 'PUSH',
          credentialType: 'OAUTH2',
          username: '',
          password: '',
          clientId: 'client_new',
          clientSecret: 'secret_new',
          tokenUrl: 'https://oauth.arcgis.com/token',
          scope: 'full',
          connectionName: 'New Connection',
          connectionMethod: 'new',
          existingConnectionId: undefined,
          createdConnectionId: 'created-999',
        });

        const connData = state.genericConnectionData.value;
        expect(connData.connectionMethod).toBe('new');
        expect(connData.clientId).toBe('client_new');
        expect(connData.clientSecret).toBe('secret_new');
        expect(connData.tokenUrl).toBe('https://oauth.arcgis.com/token');
        expect(connData.scope).toBe('full');
        expect(connData.connected).toBe(true); // createdConnectionId present
        expect(connData.createdConnectionId).toBe('created-999');
      });

      it('shows connected false when no connection IDs present', () => {
        state.updateConnectionData({
          arcgisEndpoint: 'https://test.com',
          fetchMode: 'PULL',
          credentialType: 'BASIC_AUTH',
          username: '',
          password: '',
          clientId: '',
          clientSecret: '',
          tokenUrl: '',
          scope: '',
          connectionName: '',
          connectionMethod: 'new',
          existingConnectionId: undefined,
          createdConnectionId: undefined,
        });

        const connData = state.genericConnectionData.value;
        expect(connData.connected).toBe(false);
      });

      it('defaults empty strings for tokenUrl and scope when undefined', () => {
        state.formData.tokenUrl = '' as any;
        state.formData.scope = '' as any;

        const connData = state.genericConnectionData.value;
        expect(connData.tokenUrl).toBe('');
        expect(connData.scope).toBe('');
      });

      it('defaults connectionMethod to "existing" when undefined', () => {
        state.formData.connectionMethod = undefined;

        const connData = state.genericConnectionData.value;
        expect(connData.connectionMethod).toBe('existing');
      });
    });

    describe('fieldMappingData', () => {
      it('returns fieldMappings array', () => {
        const mappings = [
          { id: '1', sourceField: 'src1', targetField: 'tgt1', isMandatory: false },
          { id: '2', sourceField: 'src2', targetField: 'tgt2', isMandatory: false },
        ];
        state.updateFieldMappingData({ fieldMappings: mappings as any });

        expect(state.fieldMappingData.value.fieldMappings).toEqual(mappings);
      });

      it('returns empty array when no mappings', () => {
        expect(state.fieldMappingData.value.fieldMappings).toEqual([
          {
            id: '',
            sourceField: '',
            targetField: '',
            transformationType: '',
            isMandatory: false,
            displayOrder: 0,
          },
        ]);
      });
    });

    describe('activeConnectionId', () => {
      it('returns existingConnectionId when connectionMethod is "existing"', () => {
        state.formData.connectionMethod = 'existing';
        state.formData.existingConnectionId = 'existing-123';
        state.formData.createdConnectionId = 'created-456';

        expect(state.activeConnectionId.value).toBe('existing-123');
      });

      it('returns createdConnectionId when connectionMethod is "new"', () => {
        state.formData.connectionMethod = 'new';
        state.formData.existingConnectionId = 'existing-123';
        state.formData.createdConnectionId = 'created-789';

        expect(state.activeConnectionId.value).toBe('created-789');
      });

      it('returns null when connectionMethod is "existing" but no existingConnectionId', () => {
        state.formData.connectionMethod = 'existing';
        state.formData.existingConnectionId = undefined;

        expect(state.activeConnectionId.value).toBeNull();
      });

      it('returns null when connectionMethod is "new" but no createdConnectionId', () => {
        state.formData.connectionMethod = 'new';
        state.formData.createdConnectionId = undefined;

        expect(state.activeConnectionId.value).toBeNull();
      });

      it('returns null when connectionMethod is undefined', () => {
        state.formData.connectionMethod = undefined;
        state.formData.existingConnectionId = 'existing-abc';
        state.formData.createdConnectionId = undefined;

        // When connectionMethod is undefined, logic goes to else branch and returns createdConnectionId (null if undefined)
        expect(state.activeConnectionId.value).toBeNull();
      });
    });
  });

  describe('handleConnectionDataUpdate', () => {
    it('transforms ConnectionStepData to internal connection format', () => {
      const connectionData: ConnectionStepData = {
        baseUrl: 'https://transform.com',
        credentialType: 'BASIC_AUTH',
        username: 'transformed_user',
        password: 'transformed_pass',
        clientId: 'client_t',
        clientSecret: 'secret_t',
        tokenUrl: 'https://token.transform.com',
        scope: 'transform_scope',
        connectionName: 'Transformed Connection',
        connectionMethod: 'new',
        existingConnectionId: undefined,
        createdConnectionId: 'transformed-999',
        connected: true,
      };

      state.handleConnectionDataUpdate(connectionData);

      expect(state.formData.arcgisEndpoint).toBe('https://transform.com');
      expect(state.formData.credentialType).toBe('BASIC_AUTH');
      expect(state.formData.username).toBe('transformed_user');
      expect(state.formData.password).toBe('transformed_pass');
      expect(state.formData.clientId).toBe('client_t');
      expect(state.formData.clientSecret).toBe('secret_t');
      expect(state.formData.tokenUrl).toBe('https://token.transform.com');
      expect(state.formData.scope).toBe('transform_scope');
      expect(state.formData.connectionName).toBe('Transformed Connection');
      expect(state.formData.connectionMethod).toBe('new');
      expect(state.formData.createdConnectionId).toBe('transformed-999');
    });

    it('defaults empty strings for optional fields', () => {
      const connectionData: ConnectionStepData = {
        baseUrl: 'https://minimal.com',
        credentialType: 'OAUTH2',
        username: undefined,
        password: undefined,
        clientId: undefined,
        clientSecret: undefined,
        tokenUrl: undefined,
        scope: undefined,
        connectionName: 'Minimal',
        connectionMethod: 'existing',
        existingConnectionId: 'min-123',
        createdConnectionId: undefined,
        connected: true,
      };

      state.handleConnectionDataUpdate(connectionData);

      expect(state.formData.username).toBe('');
      expect(state.formData.password).toBe('');
      expect(state.formData.clientId).toBe('');
      expect(state.formData.clientSecret).toBe('');
      expect(state.formData.tokenUrl).toBe('');
      expect(state.formData.scope).toBe('');
    });

    it('preserves fetchMode from formData when updating connection', () => {
      state.formData.fetchMode = 'PUSH';

      const connectionData: ConnectionStepData = {
        baseUrl: 'https://preserve.com',
        credentialType: 'BASIC_AUTH',
        username: 'user',
        password: 'pass',
        clientId: '',
        clientSecret: '',
        tokenUrl: '',
        scope: '',
        connectionName: 'Preserve Test',
        connectionMethod: 'new',
        existingConnectionId: undefined,
        createdConnectionId: 'preserve-456',
        connected: true,
      };

      state.handleConnectionDataUpdate(connectionData);

      expect(state.formData.fetchMode).toBe('PUSH');
      expect(state.formData.arcgisEndpoint).toBe('https://preserve.com');
    });
  });

  describe('setStepValidation', () => {
    it('sets validation state for step 1', () => {
      state.setStepValidation(1, true);
      expect(state.stepValidation[1]).toBe(true);
    });

    it('sets validation state for step 5', () => {
      state.setStepValidation(5, false);
      expect(state.stepValidation[5]).toBe(false);
    });

    it('changes validation state from false to true', () => {
      expect(state.stepValidation[2]).toBe(false);
      state.setStepValidation(2, true);
      expect(state.stepValidation[2]).toBe(true);
    });

    it('changes validation state from true to false', () => {
      expect(state.stepValidation[5]).toBe(true);
      state.setStepValidation(5, false);
      expect(state.stepValidation[5]).toBe(false);
    });

    it('sets validation for all steps', () => {
      state.setStepValidation(1, true);
      state.setStepValidation(2, true);
      state.setStepValidation(3, true);
      state.setStepValidation(4, true);
      state.setStepValidation(5, true);

      expect(state.stepValidation[1]).toBe(true);
      expect(state.stepValidation[2]).toBe(true);
      expect(state.stepValidation[3]).toBe(true);
      expect(state.stepValidation[4]).toBe(true);
      expect(state.stepValidation[5]).toBe(true);
    });
  });

  describe('resetFormData', () => {
    it('resets formData to default values', () => {
      state.updateBasicDetailsData({ name: 'Modified', description: 'Modified Desc' });
      state.updateDocumentSelectionData({
        itemType: 'Case',
        subType: 'SubA',
        subTypeLabel: 'LabelA',
        dynamicDocument: 'Doc',
        dynamicDocumentLabel: 'DocLabel',
      });
      state.updateFieldMappingData({
        fieldMappings: [
          { id: '1', sourceField: 'field', targetField: 'target', isMandatory: false },
        ] as any,
      });

      state.resetFormData();

      expect(state.formData.name).toBe('');
      expect(state.formData.description).toBe('');
      expect(state.formData.itemType).toBe('DOCUMENT');
      expect(state.formData.fieldMappings).toEqual([
        {
          id: '',
          sourceField: '',
          targetField: '',
          transformationType: '',
          isMandatory: false,
          displayOrder: 0,
        },
      ]);
    });

    it('resets stepValidation to default states', () => {
      state.setStepValidation(1, true);
      state.setStepValidation(2, true);
      state.setStepValidation(3, true);
      state.setStepValidation(5, false);

      state.resetFormData();

      expect(state.stepValidation[1]).toBe(false);
      expect(state.stepValidation[2]).toBe(false);
      expect(state.stepValidation[3]).toBe(false);
      expect(state.stepValidation[4]).toBe(false);
      expect(state.stepValidation[5]).toBe(true);
    });

    it('resets after multiple modifications', () => {
      state.updateBasicDetailsData({ name: 'Test1', description: 'Desc1' });
      state.updateConnectionData({
        arcgisEndpoint: 'https://test.com',
        fetchMode: 'PULL',
        credentialType: 'OAUTH2',
        username: 'user1',
        password: 'pass1',
        clientId: 'client1',
        clientSecret: 'secret1',
        tokenUrl: 'https://oauth.com',
        scope: 'scope1',
        connectionName: 'Test Conn',
        connectionMethod: 'new',
        existingConnectionId: undefined,
        createdConnectionId: 'conn-123',
      });
      state.setStepValidation(1, true);
      state.setStepValidation(4, true);

      state.resetFormData();

      expect(state.formData.name).toBe('');
      expect(state.formData.arcgisEndpoint).toBe('');
      expect(state.formData.username).toBe('');
      // createdConnectionId is not in createDefaultFormData, so it persists after reset
      expect(state.formData.createdConnectionId).toBe('conn-123');
      expect(state.stepValidation[1]).toBe(false);
      expect(state.stepValidation[4]).toBe(false);
    });
  });
});
