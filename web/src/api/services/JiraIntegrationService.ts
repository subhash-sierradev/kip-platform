import { ServiceType } from '@/api/models/enums';
import { withLoadingMessage } from '@/utils/setupGlobalLoader';

import type { IntegrationConnectionRequest, IntegrationConnectionResponse } from '../models';
import { api } from '../OpenAPI';

// Minimal Jira-specific interfaces that don't duplicate shared models
export interface JiraConnectionTestResult {
  success: boolean;
  message?: string;
}
export interface JiraProjectResponse {
  key: string;
  name: string;
}
export interface JiraIssueTypeResponse {
  id: string;
  name: string;
}
export interface JiraUserResponse {
  accountId: string;
  displayName: string;
}
export interface JiraFieldResponse {
  id: string;
  name: string;
  custom?: boolean;
}
export interface JiraParentIssueResponse {
  key: string;
  summary?: string;
  issueType?: string;
}
export interface JiraTeamResponse {
  id: string;
  name: string;
}
export interface JiraSprintResponse {
  id: number;
  name: string;
  state: 'active' | 'future' | 'closed';
  startDate?: string;
  endDate?: string;
  originBoardId?: number;
  archived?: boolean;
}

export class JiraIntegrationService {
  static async testAndCreateConnection(
    requestBody: IntegrationConnectionRequest
  ): Promise<IntegrationConnectionResponse> {
    // Will automatically show "Testing connection..." due to URL pattern matching
    const resp = await api.post('/integrations/connections/test-connection', requestBody);
    return resp.data as IntegrationConnectionResponse;
  }

  static async listConnections(): Promise<IntegrationConnectionResponse[]> {
    // Will automatically show "Loading connections..." (intelligent naming)
    const resp = await api.get('/integrations/connections', {
      params: {
        serviceType: ServiceType.JIRA,
      },
    });
    return resp.data as IntegrationConnectionResponse[];
  }

  static async testExistingConnection(connectionId: string): Promise<JiraConnectionTestResult> {
    // Example of custom loading message for better UX
    const resp = await api.post(
      `/integrations/connections/${connectionId}/test`,
      {},
      withLoadingMessage({}, 'Verifying Jira connection...')
    );
    return resp.data as JiraConnectionTestResult;
  }

  static async getProjectsByConnectionId(connectionId: string): Promise<JiraProjectResponse[]> {
    // Example of custom loading message for specific operation
    const resp = await api.post(
      `/integrations/jira/connections/${connectionId}/projects`,
      {},
      withLoadingMessage({}, 'Fetching Jira projects...')
    );
    return resp.data as JiraProjectResponse[];
  }

  static async getProjectIssueTypesByConnectionId(
    connectionId: string,
    projectKey: string
  ): Promise<JiraIssueTypeResponse[]> {
    // Will automatically show "Loading issue types..." (intelligent URL parsing)
    const resp = await api.post(
      `/integrations/jira/connections/${connectionId}/projects/${projectKey}/issue-types`
    );
    return resp.data as JiraIssueTypeResponse[];
  }

  static async getProjectUsersByConnectionId(
    connectionId: string,
    projectKey: string
  ): Promise<JiraUserResponse[]> {
    const resp = await api.post(
      `/integrations/jira/connections/${connectionId}/projects/${projectKey}/users`
    );
    return resp.data as JiraUserResponse[];
  }

  static async getProjectParentIssuesByConnectionId(
    connectionId: string,
    projectKey: string,
    opts?: { startAt?: number; maxResults?: number; query?: string }
  ): Promise<JiraParentIssueResponse[]> {
    const resp = await api.post(
      `/integrations/jira/connections/${connectionId}/projects/${projectKey}/parent-issues`,
      null,
      { params: opts ?? {} }
    );
    return resp.data as JiraParentIssueResponse[];
  }

  static async getFieldsByConnectionId(connectionId: string): Promise<JiraFieldResponse[]> {
    const resp = await api.post(`/integrations/jira/connections/${connectionId}/fields`);
    return resp.data as JiraFieldResponse[];
  }

  static async getProjectMetaFieldsByConnectionId(
    connectionId: string,
    projectKey: string,
    issueTypeId?: string
  ): Promise<JiraFieldResponse[]> {
    const resp = await api.post(
      `/integrations/jira/connections/${connectionId}/projects/${projectKey}/meta-fields`,
      null,
      { params: { issueTypeId } }
    );
    return resp.data as JiraFieldResponse[];
  }

  // Teams lookup (Jira atlassian-team custom field)
  static async getTeamsByConnectionId(
    connectionId: string,
    opts?: { query?: string; startAt?: number; maxResults?: number }
  ): Promise<JiraTeamResponse[]> {
    const resp = await api.post(`/integrations/jira/connections/${connectionId}/teams`, null, {
      params: opts ?? {},
    });
    return resp.data as JiraTeamResponse[];
  }

  // Sprints lookup (Jira Agile)
  static async getSprintsByConnectionId(
    connectionId: string,
    opts?: {
      boardId?: number;
      state?: string;
      query?: string;
      startAt?: number;
      maxResults?: number;
      projectKey?: string;
    }
  ): Promise<JiraSprintResponse[]> {
    const resp = await api.post(`/integrations/jira/connections/${connectionId}/sprints`, null, {
      params: opts ?? {},
    });
    return resp.data as JiraSprintResponse[];
  }
}
