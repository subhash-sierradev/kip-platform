import { MappingData } from '@/api/models/jirawebhook/MappingData';
import type {
  CustomFieldMapping,
  JiraFieldMapping as ServiceJiraFieldMapping,
} from '@/api/services/JiraWebhookService';
import { extractParentKey, parentLabelMatchesKey } from '@/utils/parentFieldUtils';

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

function toUserNameMap(users: SimpleUser[]): Map<string, string> {
  const map = new Map<string, string>();
  (users || []).forEach(user => {
    map.set(user.accountId, user.displayName);
  });
  return map;
}

function formatMultiUserDisplayValue(template: string, userMap: Map<string, string>): string {
  const raw = String(template || '').trim();
  if (!raw) {
    return '';
  }

  const values = raw
    .split(',')
    .map(value => value.trim())
    .filter(Boolean);
  if (values.length === 0) {
    return raw;
  }

  return values.map(value => userMap.get(value) ?? value).join(', ');
}

function resolveParentDisplayLabel(template: string, selectedParentLabel: string): string {
  const parentKey = extractParentKey(template);
  const parentLabel = String(selectedParentLabel || '').trim();
  return parentLabel && parentLabelMatchesKey(parentLabel, parentKey) ? parentLabel : parentKey;
}

function resolveUserDisplayLabel(
  normalizedType: string | undefined,
  template: string,
  userMap: Map<string, string>
): string {
  if (normalizedType === 'user') {
    const userId = template.trim();
    return userId ? (userMap.get(userId) ?? userId) : '';
  }

  if (normalizedType === 'multiuser') {
    return formatMultiUserDisplayValue(template, userMap);
  }

  return '';
}

function resolveCustomDisplayLabel(
  cf: CustomFieldMapping,
  keyLower: string,
  normalizedType: string | undefined,
  template: string,
  label: string,
  userMap: Map<string, string>,
  selectedParentLabel: string
): string | null {
  if (cf.valueSource === 'json' || isJsonTemplate(template)) {
    return null;
  }

  if (keyLower === 'parent') {
    return resolveParentDisplayLabel(template, selectedParentLabel);
  }

  const userDisplayLabel = resolveUserDisplayLabel(normalizedType, template, userMap);
  if (normalizedType === 'user' || normalizedType === 'multiuser') {
    return userDisplayLabel;
  }

  return label;
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

function buildCustomMappings(
  customFields: CustomFieldMapping[] | undefined,
  users: SimpleUser[],
  selectedParentLabel: string
): JiraFieldMapping[] {
  const out: JiraFieldMapping[] = [];
  const userMap = toUserNameMap(users || []);
  const list = (customFields ?? []).filter(Boolean);
  for (const cf of list) {
    const key = String(cf.jiraFieldKey || '').trim();
    if (!key) continue;
    out.push(buildCustomFieldMapping(cf, key, userMap, selectedParentLabel));
  }
  return out;
}

function buildCustomFieldMapping(
  cf: CustomFieldMapping,
  key: string,
  userMap: Map<string, string>,
  selectedParentLabel: string
): JiraFieldMapping {
  const label = (cf.jiraFieldLabel || key) as string;
  const template = String(cf.value ?? '');
  const normalizedType = (cf.type as string | undefined)?.toLowerCase();
  const dataType = TYPE_MAP[normalizedType || 'string'] || 'STRING';
  const keyLower = key.toLowerCase();
  const displayLabel = resolveCustomDisplayLabel(
    cf,
    keyLower,
    normalizedType,
    template,
    label,
    userMap,
    selectedParentLabel
  );

  return {
    jiraFieldId: key,
    jiraFieldName: label,
    displayLabel,
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
  users: SimpleUser[] = [],
  selectedParentLabel = ''
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
  const custom = buildCustomMappings(mappingData.customFields, users, selectedParentLabel);
  return [...standard, ...custom];
}
