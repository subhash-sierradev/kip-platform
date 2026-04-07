export interface IntegrationConnection {
  id: string;
  name?: string;
  serviceType?: string;
  baseUrl?: string;
  jiraBaseUrl?: string; // For backward compatibility with Jira
  lastConnectionStatus?: string;
  authType?: string;
  createdDate?: string;
  lastConnectionTest?: string;
}
