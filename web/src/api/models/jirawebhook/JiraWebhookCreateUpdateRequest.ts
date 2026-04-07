import type { JiraFieldMapping as ServiceJiraFieldMapping } from '@/api/services/JiraWebhookService';

export interface JiraWebhookCreateUpdateRequest {
  name: string;
  connectionId: string;
  description?: string;
  fieldsMapping: ServiceJiraFieldMapping[];
  samplePayload: string;
}
