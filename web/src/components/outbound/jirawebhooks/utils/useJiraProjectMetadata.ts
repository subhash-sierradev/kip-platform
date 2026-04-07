import { onMounted, type Ref, ref, watch } from 'vue';

import { JiraConnectionRequest } from '@/api/models/jirawebhook/JiraConnectionRequest';
import { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import { MappingData } from '@/api/models/jirawebhook/MappingData';
import { JiraIntegrationService } from '@/api/services/JiraIntegrationService';

interface UseJiraProjectMetadataOptions {
  mappingData: Ref<MappingData>;
  projects: Ref<JiraProject[] | undefined>;
  issueTypes: Ref<JiraIssueType[] | undefined>;
  users: Ref<JiraUser[] | undefined>;
  connectionId: Ref<string | undefined>;
  jiraConnectionRequest: Ref<JiraConnectionRequest | undefined>;
  emitProjectsChange: (list: JiraProject[]) => void;
  emitIssueTypesChange: (list: JiraIssueType[]) => void;
  emitUsersChange: (list: JiraUser[]) => void;
}

interface JiraIssueTypeResponse {
  id?: unknown;
  name?: unknown;
  subtask?: unknown;
}
interface JiraUserResponse {
  accountId?: unknown;
  displayName?: unknown;
}
interface JiraProjectResponse {
  key?: unknown;
  name?: unknown;
}

const asString = (value: unknown): string => (typeof value === 'string' ? value : '');

const mapIssueTypes = (items: JiraIssueTypeResponse[]): JiraIssueType[] =>
  items.map(it => ({
    id: asString(it.id),
    name: asString(it.name),
    ...(typeof it.subtask === 'boolean' ? { subtask: it.subtask } : {}),
  }));

const mapUsers = (items: JiraUserResponse[]): JiraUser[] =>
  items.map(u => ({ accountId: asString(u.accountId), displayName: asString(u.displayName) }));

const mapProjects = (items: JiraProjectResponse[]): JiraProject[] =>
  items.map(p => ({ key: asString(p.key), name: asString(p.name) }));

const syncLocalList = <T>(source: T[] | undefined, target: Ref<T[]>) => {
  if (source) target.value = [...source];
};

async function fetchIssueTypesAndUsers(
  options: UseJiraProjectMetadataOptions,
  projectKey: string,
  issueTypesLocal: Ref<JiraIssueType[]>,
  usersLocal: Ref<JiraUser[]>
) {
  try {
    if (options.connectionId.value) {
      const issueTypesResponse = await JiraIntegrationService.getProjectIssueTypesByConnectionId(
        options.connectionId.value,
        projectKey
      );
      const usersResponse = await JiraIntegrationService.getProjectUsersByConnectionId(
        options.connectionId.value,
        projectKey
      );
      issueTypesLocal.value = mapIssueTypes(issueTypesResponse as JiraIssueTypeResponse[]);
      usersLocal.value = mapUsers(usersResponse as JiraUserResponse[]);
    }
    options.emitIssueTypesChange(issueTypesLocal.value);
    options.emitUsersChange(usersLocal.value);
  } catch (err) {
    console.error('Failed to fetch Jira issue types/users', err);
    options.emitIssueTypesChange(issueTypesLocal.value);
    options.emitUsersChange(usersLocal.value);
  }
}

async function fetchProjects(
  options: UseJiraProjectMetadataOptions,
  projectsLocal: Ref<JiraProject[]>
) {
  try {
    if (options.connectionId.value) {
      const projectsResponse = await JiraIntegrationService.getProjectsByConnectionId(
        options.connectionId.value
      );
      const mapped = mapProjects(projectsResponse as JiraProjectResponse[]);
      projectsLocal.value = mapped;
      options.emitProjectsChange(mapped);
    } else if (options.jiraConnectionRequest.value) {
      options.emitProjectsChange(projectsLocal.value);
    }
  } catch (err) {
    console.error('Failed to fetch Jira projects', err);
    options.emitProjectsChange(projectsLocal.value);
  }
}

export function useJiraProjectMetadata(options: UseJiraProjectMetadataOptions) {
  const projectsLocal = ref<JiraProject[]>(options.projects.value || []);
  const issueTypesLocal = ref<JiraIssueType[]>(options.issueTypes.value || []);
  const usersLocal = ref<JiraUser[]>(options.users.value || []);

  watch(options.projects, val => syncLocalList(val, projectsLocal), { deep: true });
  watch(options.issueTypes, val => syncLocalList(val, issueTypesLocal), { deep: true });
  watch(options.users, val => syncLocalList(val, usersLocal), { deep: true });

  watch(
    () => options.mappingData.value.selectedProject,
    async projectKey => {
      if (!projectKey) return;
      await fetchIssueTypesAndUsers(options, projectKey, issueTypesLocal, usersLocal);
    }
  );

  watch(
    () => [options.connectionId.value, options.jiraConnectionRequest.value],
    () => {
      void fetchProjects(options, projectsLocal);
    },
    { deep: true, immediate: true }
  );

  onMounted(async () => {
    const projectKey = options.mappingData.value.selectedProject;
    if (projectKey && options.connectionId.value) {
      await fetchIssueTypesAndUsers(options, projectKey, issueTypesLocal, usersLocal);
    }
  });

  return {
    projectsLocal,
    issueTypesLocal,
    usersLocal,
  };
}
