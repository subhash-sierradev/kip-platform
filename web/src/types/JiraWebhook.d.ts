/**
 * Represents a mapping between a Jira field and a target field.
 */
export interface JiraFieldMapping {
  sourceField: string;
  targetField: string;
  dataType?: string;
  required?: boolean;
  jiraFieldId?: string;
  displayLabel?: string;
  jiraFieldName?: string;
  [key: string]: unknown; // Allow for additional properties if needed
}

/**
 * Represents the history of the last event triggered by the webhook.
 */
export interface JiraWebhookEventHistory {
  eventId: string;
  eventType: string;
  timestamp: string;
  status: string;
  message?: string;
  triggeredAt?: string;
  [key: string]: unknown; // Allow for additional properties if needed
}

export interface JiraWebhook {
  id: string;
  name: string;
  webhookUrl: string;
  jiraFieldMappings: JiraFieldMapping[];
  isEnabled: boolean;
  isDeleted: boolean;
  createdBy: string;
  createdDate: string;
  lastEventHistory: JiraWebhookEventHistory;
  updatedDate?: string;
  triggerCount?: number;
  description?: string;
  samplePayload?: string;
  connectionId?: string;
  fieldsMapping?: JiraFieldMapping[];
}
