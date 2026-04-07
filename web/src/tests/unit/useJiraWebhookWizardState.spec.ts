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
});
