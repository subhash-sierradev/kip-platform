import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, ref } from 'vue';

import type { JiraIssueType } from '@/api/models/jirawebhook/JiraIssueType';
import type { JiraProject } from '@/api/models/jirawebhook/JiraProject';
import type { JiraUser } from '@/api/models/jirawebhook/JiraUser';
import type { MappingData } from '@/api/models/jirawebhook/MappingData';
import { useJiraProjectMetadata } from '@/components/outbound/jirawebhooks/utils/useJiraProjectMetadata';

const serviceHoisted = vi.hoisted(() => ({
  getProjectsByConnectionId: vi.fn(),
  getProjectIssueTypesByConnectionId: vi.fn(),
  getProjectUsersByConnectionId: vi.fn(),
}));

vi.mock('@/api/services/JiraIntegrationService', () => ({
  JiraIntegrationService: {
    getProjectsByConnectionId: serviceHoisted.getProjectsByConnectionId,
    getProjectIssueTypesByConnectionId: serviceHoisted.getProjectIssueTypesByConnectionId,
    getProjectUsersByConnectionId: serviceHoisted.getProjectUsersByConnectionId,
  },
}));

describe('useJiraProjectMetadata', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('fetches projects on connection change', async () => {
    serviceHoisted.getProjectsByConnectionId.mockResolvedValueOnce([
      { key: 'P1', name: 'Project 1' },
    ]);

    const emitProjectsChange = vi.fn();
    const emitIssueTypesChange = vi.fn();
    const emitUsersChange = vi.fn();

    const TestComponent = defineComponent({
      setup() {
        const mappingData = ref<MappingData>({
          selectedProject: '',
          selectedIssueType: '',
          selectedAssignee: '',
          summary: '',
          descriptionFieldMapping: '',
        });
        const projects = ref<JiraProject[] | undefined>(undefined);
        const issueTypes = ref<JiraIssueType[] | undefined>(undefined);
        const users = ref<JiraUser[] | undefined>(undefined);
        const connectionId = ref<string | undefined>('conn-1');
        const jiraConnectionRequest = ref(undefined);

        const { projectsLocal } = useJiraProjectMetadata({
          mappingData,
          projects,
          issueTypes,
          users,
          connectionId,
          jiraConnectionRequest,
          emitProjectsChange,
          emitIssueTypesChange,
          emitUsersChange,
        });

        return { projectsLocal };
      },
      template: '<div />',
    });

    const wrapper = mount(TestComponent);
    await flushPromises();

    expect(serviceHoisted.getProjectsByConnectionId).toHaveBeenCalledWith('conn-1');
    expect(emitProjectsChange).toHaveBeenCalledWith([{ key: 'P1', name: 'Project 1' }]);
    expect(wrapper.vm.projectsLocal).toEqual([{ key: 'P1', name: 'Project 1' }]);
  });

  it('fetches issue types and users on project change', async () => {
    serviceHoisted.getProjectsByConnectionId.mockResolvedValueOnce([]);
    serviceHoisted.getProjectIssueTypesByConnectionId.mockResolvedValueOnce([
      { id: 'I1', name: 'Bug', subtask: false },
    ]);
    serviceHoisted.getProjectUsersByConnectionId.mockResolvedValueOnce([
      { accountId: 'U1', displayName: 'User 1' },
    ]);

    const emitProjectsChange = vi.fn();
    const emitIssueTypesChange = vi.fn();
    const emitUsersChange = vi.fn();

    const TestComponent = defineComponent({
      setup() {
        const mappingData = ref<MappingData>({
          selectedProject: '',
          selectedIssueType: '',
          selectedAssignee: '',
          summary: '',
          descriptionFieldMapping: '',
        });
        const projects = ref<JiraProject[] | undefined>(undefined);
        const issueTypes = ref<JiraIssueType[] | undefined>(undefined);
        const users = ref<JiraUser[] | undefined>(undefined);
        const connectionId = ref<string | undefined>('conn-1');
        const jiraConnectionRequest = ref(undefined);

        const { issueTypesLocal, usersLocal } = useJiraProjectMetadata({
          mappingData,
          projects,
          issueTypes,
          users,
          connectionId,
          jiraConnectionRequest,
          emitProjectsChange,
          emitIssueTypesChange,
          emitUsersChange,
        });

        return { mappingData, issueTypesLocal, usersLocal };
      },
      template: '<div />',
    });

    const wrapper = mount(TestComponent);
    wrapper.vm.mappingData.selectedProject = 'P1';
    await flushPromises();

    expect(serviceHoisted.getProjectIssueTypesByConnectionId).toHaveBeenCalledWith('conn-1', 'P1');
    expect(serviceHoisted.getProjectUsersByConnectionId).toHaveBeenCalledWith('conn-1', 'P1');
    expect(emitIssueTypesChange).toHaveBeenCalledWith([{ id: 'I1', name: 'Bug', subtask: false }]);
    expect(emitUsersChange).toHaveBeenCalledWith([{ accountId: 'U1', displayName: 'User 1' }]);
    expect(wrapper.vm.issueTypesLocal).toEqual([{ id: 'I1', name: 'Bug', subtask: false }]);
    expect(wrapper.vm.usersLocal).toEqual([{ accountId: 'U1', displayName: 'User 1' }]);
  });
});
