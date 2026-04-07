import { describe, expect, it } from 'vitest';

import {
  createDefaultConnectionData,
  createDefaultMappingData,
  getJiraWebhookSteps,
} from '@/components/outbound/jirawebhooks/utils/jiraWebhookWizardConfig';

describe('jiraWebhookWizardConfig', () => {
  it('returns steps for create mode', () => {
    expect(getJiraWebhookSteps('create')).toEqual([
      'Basic Details',
      'Sample Payload',
      'Connect Jira',
      'Field Mapping',
      'Review & Create',
    ]);
  });

  it('returns steps for edit mode', () => {
    expect(getJiraWebhookSteps('edit')).toEqual([
      'Basic Details',
      'Sample Payload',
      'Connect Jira',
      'Field Mapping',
      'Review & Update',
    ]);
  });

  it('returns steps for clone mode', () => {
    expect(getJiraWebhookSteps('clone')).toEqual([
      'Basic Details',
      'Sample Payload',
      'Connect Jira',
      'Field Mapping',
      'Review & Clone',
    ]);
  });

  it('creates default connection data', () => {
    expect(createDefaultConnectionData()).toEqual({
      connectionMethod: 'existing',
      baseUrl: '',
      credentialType: 'BASIC_AUTH',
      username: '',
      password: '',
      connectionName: '',
      connected: false,
      existingConnectionId: undefined,
      createdConnectionId: undefined,
    });
  });

  it('creates default mapping data', () => {
    expect(createDefaultMappingData()).toEqual({
      selectedProject: '',
      selectedIssueType: '',
      selectedAssignee: '',
      summary: '',
      descriptionFieldMapping: '',
    });
  });
});
