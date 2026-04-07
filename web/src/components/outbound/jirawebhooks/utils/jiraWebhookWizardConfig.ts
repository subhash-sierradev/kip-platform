import type { MappingData } from '@/api/models/jirawebhook/MappingData';
import type { ConnectionStepData } from '@/types/ConnectionStepData';

export type JiraWebhookWizardMode = 'create' | 'edit' | 'clone';

export const getJiraWebhookSteps = (mode: JiraWebhookWizardMode): string[] => {
  if (mode === 'clone') {
    return ['Basic Details', 'Sample Payload', 'Connect Jira', 'Field Mapping', 'Review & Clone'];
  }
  if (mode === 'edit') {
    return ['Basic Details', 'Sample Payload', 'Connect Jira', 'Field Mapping', 'Review & Update'];
  }
  return ['Basic Details', 'Sample Payload', 'Connect Jira', 'Field Mapping', 'Review & Create'];
};

export const createDefaultConnectionData = (): ConnectionStepData => ({
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

export const createDefaultMappingData = (): MappingData => ({
  selectedProject: '',
  selectedIssueType: '',
  selectedAssignee: '',
  summary: '',
  descriptionFieldMapping: '',
});
