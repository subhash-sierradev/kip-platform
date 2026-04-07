import { MappingData } from '@/api/models/jirawebhook/MappingData';
import type {
  CustomFieldMapping,
  JiraFieldMapping as ServiceJiraFieldMapping,
} from '@/api/services/JiraWebhookService';

export type { MappingData };

export interface SimpleProject {
  key: string;
  name: string;
}
export interface SimpleIssueType {
  id: string;
  name: string;
  subtask?: boolean;
}
export interface SimpleUser {
  accountId: string;
  displayName: string;
}
export type JiraFieldMapping = ServiceJiraFieldMapping;

// ---- Helpers & constants ----
const TYPE_MAP: Record<string, JiraFieldMapping['dataType']> = {
  string: 'STRING',
  number: 'NUMBER',
  sprint: 'NUMBER',
  boolean: 'BOOLEAN',
  option: 'STRING',
  array: 'ARRAY',
  datetime: 'DATE',
  date: 'DATE',
  user: 'USER',
  multiuser: 'MULTIUSER',
  url: 'URL',
  object: 'OBJECT',
};

function isJsonTemplate(s: string | undefined): boolean {
  if (!s) return false;
  return /{{\s*[^}]+\s*}}/.test(s);
}

function computeLabel<T>(
  value: string,
  list: T[],
  getKey: (item: T) => string | undefined,
  getName: (item: T) => string | undefined
): string {
  if (!value) return '';
  const found = list.find(x => getKey(x) === value);
  return found ? (getName(found) ?? value) : value;
}

function buildField(
  id: string,
  name: string,
  displayLabel: string | null,
  dataType: JiraFieldMapping['dataType'],
  template: string,
  required: boolean,
  metadata?: Record<string, unknown>
): JiraFieldMapping {
  return {
    jiraFieldId: id,
    jiraFieldName: name,
    displayLabel,
    dataType,
    template: template || '',
    required,
    defaultValue: null,
    metadata,
  };
}

function buildStandardMappings(
  mappingData: MappingData,
  projectLabel: string,
  issueType: SimpleIssueType | undefined,
  issueTypeLabel: string,
  assigneeLabel: string
): JiraFieldMapping[] {
  const f: JiraFieldMapping[] = [
    buildField('project', 'Project', projectLabel, 'OBJECT', mappingData.selectedProject, true),
    buildField(
      'issuetype',
      'Issue Type',
      issueTypeLabel,
      'OBJECT',
      mappingData.selectedIssueType,
      true,
      issueType ? { subtask: issueType.subtask === true } : undefined
    ),
    buildField(
      'summary',
      'Summary',
      isJsonTemplate(mappingData.summary) ? null : 'Summary',
      'STRING',
      mappingData.summary,
      true
    ),
  ];

  if (mappingData.selectedAssignee?.trim()) {
    f.push(
      buildField(
        'assignee',
        'Assignee',
        assigneeLabel,
        'OBJECT',
        mappingData.selectedAssignee,
        false
      )
    );
  }
  if (mappingData.descriptionFieldMapping?.trim()) {
    f.push(
      buildField(
        'description',
        'Description',
        isJsonTemplate(mappingData.descriptionFieldMapping) ? null : 'Description',
        'STRING',
        mappingData.descriptionFieldMapping,
        false
      )
    );
  }
  return f;
}

function buildCustomMappings(customFields: CustomFieldMapping[] | undefined): JiraFieldMapping[] {
  const out: JiraFieldMapping[] = [];
  const list = (customFields ?? []).filter(Boolean);
  for (const cf of list) {
    const key = String(cf.jiraFieldKey || '').trim();
    if (!key) continue;
    out.push(buildCustomFieldMapping(cf, key));
  }
  return out;
}

function buildCustomFieldMapping(cf: CustomFieldMapping, key: string): JiraFieldMapping {
  const label = (cf.jiraFieldLabel || key) as string;
  const template = String(cf.value ?? '');
  const normalizedType = (cf.type as string | undefined)?.toLowerCase();
  const dataType = TYPE_MAP[normalizedType || 'string'] || 'STRING';

  return {
    jiraFieldId: key,
    jiraFieldName: label,
    displayLabel: cf.valueSource === 'json' || isJsonTemplate(template) ? null : label,
    dataType,
    template,
    required: !!cf.required,
    defaultValue: null,
    metadata: normalizedType ? { fieldType: normalizedType } : undefined,
  };
}

export function createFieldMappings(
  mappingData: MappingData,
  projects: SimpleProject[] = [],
  issueTypes: SimpleIssueType[] = [],
  users: SimpleUser[] = []
): JiraFieldMapping[] {
  // Standard field labels
  const projectLabel = computeLabel(
    mappingData.selectedProject,
    projects,
    p => p.key,
    p => p.name
  );
  const selectedIssueType = issueTypes.find(i => i.id === mappingData.selectedIssueType);
  const issueTypeLabel = computeLabel(
    mappingData.selectedIssueType,
    issueTypes,
    i => i.id,
    i => i.name
  );
  const assigneeLabel = computeLabel(
    mappingData.selectedAssignee,
    users,
    u => u.accountId,
    u => u.displayName
  );

  const standard = buildStandardMappings(
    mappingData,
    projectLabel,
    selectedIssueType,
    issueTypeLabel,
    assigneeLabel
  );
  const custom = buildCustomMappings(mappingData.customFields);
  return [...standard, ...custom];
}
