import { ref } from 'vue';

import {
  JiraIntegrationService,
  type JiraParentIssueResponse,
} from '@/api/services/JiraIntegrationService';

const MIN_PARENT_REMOTE_SEARCH_LENGTH = 5;

interface LookupState {
  parentIssues: { value: JiraParentIssueResponse[] };
  defaultParentIssues: { value: JiraParentIssueResponse[] };
  lastParentIssuesFetchKey: { value: string };
  inFlightParentIssuesFetchKey: { value: string };
  lastParentIssuesSearchFetchKey: { value: string };
  inFlightParentIssuesSearchFetchKey: { value: string };
}

function resetState(state: LookupState) {
  state.parentIssues.value = [];
  state.defaultParentIssues.value = [];
  state.lastParentIssuesFetchKey.value = '';
  state.inFlightParentIssuesFetchKey.value = '';
  state.lastParentIssuesSearchFetchKey.value = '';
  state.inFlightParentIssuesSearchFetchKey.value = '';
}

function getInitialFetchKey(connectionId: string, projectKey: string): string {
  return `${connectionId}|${projectKey}|subtask`;
}

function getSearchFetchKey(
  connectionId: string,
  projectKey: string,
  normalizedQuery: string
): string {
  return `${connectionId}|${projectKey}|subtask|${normalizedQuery.toLowerCase()}`;
}

function shouldSkipInitialFetch(state: LookupState, fetchKey: string): boolean {
  return (
    fetchKey === state.lastParentIssuesFetchKey.value ||
    fetchKey === state.inFlightParentIssuesFetchKey.value
  );
}

function shouldSkipSearchFetch(state: LookupState, searchFetchKey: string): boolean {
  return (
    searchFetchKey === state.lastParentIssuesSearchFetchKey.value ||
    searchFetchKey === state.inFlightParentIssuesSearchFetchKey.value
  );
}

function applyInitialFetchSuccess(
  state: LookupState,
  fetchKey: string,
  response: JiraParentIssueResponse[]
) {
  const fetchedParentIssues = Array.isArray(response) ? response : [];
  state.defaultParentIssues.value = fetchedParentIssues;
  state.parentIssues.value = fetchedParentIssues;
  state.lastParentIssuesFetchKey.value = fetchKey;
  state.lastParentIssuesSearchFetchKey.value = '';
}

function applySearchSuccess(
  state: LookupState,
  searchFetchKey: string,
  response: JiraParentIssueResponse[]
) {
  state.parentIssues.value = Array.isArray(response) ? response : [];
  state.lastParentIssuesSearchFetchKey.value = searchFetchKey;
}

function clearSearchState(state: LookupState) {
  state.lastParentIssuesSearchFetchKey.value = '';
  state.inFlightParentIssuesSearchFetchKey.value = '';
}

async function fetchParentIssuesForProjectInternal(
  state: LookupState,
  connectionId: string | undefined,
  projectKey: string,
  isSubtask: boolean
) {
  if (!connectionId || !projectKey || !isSubtask) {
    resetState(state);
    return;
  }

  const fetchKey = getInitialFetchKey(connectionId, projectKey);
  if (shouldSkipInitialFetch(state, fetchKey)) {
    return;
  }

  state.inFlightParentIssuesFetchKey.value = fetchKey;

  try {
    const response = await JiraIntegrationService.getProjectParentIssuesByConnectionId(
      connectionId,
      projectKey,
      { maxResults: 100 }
    );

    if (state.inFlightParentIssuesFetchKey.value !== fetchKey) {
      return;
    }
    applyInitialFetchSuccess(state, fetchKey, response);
  } catch (error) {
    console.error('Failed to fetch Jira parent issues', error);
    resetState(state);
  } finally {
    if (state.inFlightParentIssuesFetchKey.value === fetchKey) {
      state.inFlightParentIssuesFetchKey.value = '';
    }
  }
}

async function searchParentIssuesByQueryInternal(
  state: LookupState,
  query: string,
  connectionId: string | undefined,
  projectKey: string,
  isSubtask: boolean
) {
  if (!connectionId || !projectKey || !isSubtask) {
    return;
  }

  const normalizedQuery = (query || '').trim();
  if (normalizedQuery.length < MIN_PARENT_REMOTE_SEARCH_LENGTH) {
    state.parentIssues.value = state.defaultParentIssues.value;
    clearSearchState(state);
    return;
  }

  const searchFetchKey = getSearchFetchKey(connectionId, projectKey, normalizedQuery);
  if (shouldSkipSearchFetch(state, searchFetchKey)) {
    return;
  }

  state.inFlightParentIssuesSearchFetchKey.value = searchFetchKey;

  try {
    const response = await JiraIntegrationService.getProjectParentIssuesByConnectionId(
      connectionId,
      projectKey,
      {
        maxResults: 100,
        query: normalizedQuery,
      }
    );

    if (state.inFlightParentIssuesSearchFetchKey.value !== searchFetchKey) {
      return;
    }
    applySearchSuccess(state, searchFetchKey, response);
  } catch (error) {
    console.error('Failed to search Jira parent issues', error);
    state.parentIssues.value = [];
    state.lastParentIssuesSearchFetchKey.value = '';
  } finally {
    if (state.inFlightParentIssuesSearchFetchKey.value === searchFetchKey) {
      state.inFlightParentIssuesSearchFetchKey.value = '';
    }
  }
}

export function useParentIssueLookup() {
  const parentIssues = ref<JiraParentIssueResponse[]>([]);
  const defaultParentIssues = ref<JiraParentIssueResponse[]>([]);
  const lastParentIssuesFetchKey = ref('');
  const inFlightParentIssuesFetchKey = ref('');
  const lastParentIssuesSearchFetchKey = ref('');
  const inFlightParentIssuesSearchFetchKey = ref('');
  const state: LookupState = {
    parentIssues,
    defaultParentIssues,
    lastParentIssuesFetchKey,
    inFlightParentIssuesFetchKey,
    lastParentIssuesSearchFetchKey,
    inFlightParentIssuesSearchFetchKey,
  };
  const resetParentIssues = () => resetState(state);
  const fetchParentIssuesForProject = (
    connectionId: string | undefined,
    projectKey: string,
    isSubtask: boolean
  ) => fetchParentIssuesForProjectInternal(state, connectionId, projectKey, isSubtask);
  const searchParentIssuesByQuery = (
    query: string,
    connectionId: string | undefined,
    projectKey: string,
    isSubtask: boolean
  ) => searchParentIssuesByQueryInternal(state, query, connectionId, projectKey, isSubtask);

  return {
    parentIssues,
    fetchParentIssuesForProject,
    searchParentIssuesByQuery,
    resetParentIssues,
  };
}
