export interface JiraConnectionRequest {
  jiraBaseUrl: string;
  integrationSecret: {
    baseUrl: string;
    authType: string;
    credentials: { apiKey: string } | { username: string; password: string; authType: string };
  };
  organizationKey?: string;
}
