import type { CustomFieldMapping } from '@/api/services/JiraWebhookService';

export interface MappingData {
  selectedProject: string;
  selectedIssueType: string;
  selectedAssignee: string;
  summary: string;
  descriptionFieldMapping: string;
  customFields?: CustomFieldMapping[];
}
