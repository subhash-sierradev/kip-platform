import { describe, expect, it } from 'vitest';

import { useJiraWebhookWizardState } from '@/composables/useJiraWebhookWizardState';

describe('useJiraWebhookWizardState', () => {
  it('initializes mappingModeEdit from editMode', () => {
    const state = useJiraWebhookWizardState({ editMode: true });
    expect(state.mappingModeEdit.value).toBe(true);
  });

  it('validates each step requirements', () => {
    const state = useJiraWebhookWizardState({});

    expect(state.isStepValid(0)).toBe(false);
    state.setIntegrationName('Webhook');
    state.setBasicDetailsValid(true);
    expect(state.isStepValid(0)).toBe(true);

    expect(state.isStepValid(1)).toBe(false);
    state.setJsonSample('{"ok":true}');
    state.setJsonValid(true);
    expect(state.isStepValid(1)).toBe(true);

    expect(state.isStepValid(2)).toBe(false);
    state.jiraConnected.value = true;
    state.connectionId.value = 'conn-1';
    expect(state.isStepValid(2)).toBe(true);

    expect(state.isStepValid(3)).toBe(false);
    state.setMappingValid(true);
    expect(state.isStepValid(3)).toBe(true);

    expect(state.isStepValid(4)).toBe(false);
    state.updateMappingData({
      selectedProject: 'PROJ',
      selectedIssueType: 'ISSUE',
      selectedAssignee: '',
      summary: 'Summary',
      descriptionFieldMapping: '',
    });
    expect(state.isStepValid(4)).toBe(true);
  });

  it('resetAllFields restores defaults', () => {
    const state = useJiraWebhookWizardState({ editMode: true });
    state.activeStep.value = 3;
    state.setIntegrationName('Name');
    state.setBasicDetailsValid(true);
    state.setJsonSample('{"ok":true}');
    state.setJsonValid(true);
    state.connectionId.value = 'conn-1';
    state.jiraConnected.value = true;
    state.updateMappingData({
      selectedProject: 'PROJ',
      selectedIssueType: 'ISSUE',
      selectedAssignee: 'user',
      summary: 'Summary',
      descriptionFieldMapping: 'Desc',
    });

    state.resetAllFields(true);

    expect(state.activeStep.value).toBe(0);
    expect(state.integrationName.value).toBe('');
    expect(state.jsonSample.value).toBe('');
    expect(state.connectionId.value).toBe('');
    expect(state.mappingData.value.selectedProject).toBe('');
    expect(state.mappingModeEdit.value).toBe(true);
  });

  it('updates all setter-backed state fields and resets mapping metadata', () => {
    const state = useJiraWebhookWizardState({});

    state.setWebhookDescription('Description');
    state.setVerifyHmac(true);
    state.setHmacKey('secret');
    state.setSampleCaptured(true);
    state.setShowJsonInput(true);
    state.setProjects([{ key: 'PROJ', name: 'Project' }] as any);
    state.setIssueTypes([{ id: '100', name: 'Bug', subtask: false }] as any);
    state.setUsers([{ accountId: 'u1', displayName: 'User' }] as any);
    state.connectionId.value = 'conn-42';
    state.updateMappingData({
      selectedProject: 'PROJ',
      selectedIssueType: '100',
      selectedAssignee: 'u1',
      summary: 'Summary',
      descriptionFieldMapping: 'desc',
      customFields: [
        {
          fieldPath: 'a',
          jiraFieldId: 'b',
          jiraFieldLabel: 'B',
          jiraFieldType: 'text',
          templatePath: 'x',
        },
      ],
    } as any);

    expect(state.webhookDescription.value).toBe('Description');
    expect(state.verifyHmac.value).toBe(true);
    expect(state.hmacKey.value).toBe('secret');
    expect(state.sampleCaptured.value).toBe(true);
    expect(state.showJsonInput.value).toBe(true);
    expect(state.projects.value).toHaveLength(1);
    expect(state.issueTypes.value).toHaveLength(1);
    expect(state.users.value).toHaveLength(1);

    state.resetMappingData();

    expect(state.mappingData.value.selectedProject).toBe('');
    expect(state.mappingData.value.customFields).toBeUndefined();
    expect(state.projects.value).toEqual([]);
    expect(state.issueTypes.value).toEqual([]);
    expect(state.users.value).toEqual([]);
    expect(state.lastResetConnectionId.value).toBe('conn-42');
    expect(state.isMappingValid.value).toBe(false);
  });
});
