import { describe, expect, it } from 'vitest';

import {
  createFieldMappings,
  type MappingData,
  type SimpleIssueType,
  type SimpleProject,
  type SimpleUser,
} from '@/utils/jiraFieldMappingUtils';

describe('jiraFieldMappingUtils.createFieldMappings', () => {
  const projects: SimpleProject[] = [{ key: 'P1', name: 'Project One' }];
  const issueTypes: SimpleIssueType[] = [{ id: 'IT1', name: 'Task' }];
  const users: SimpleUser[] = [{ accountId: 'U1', displayName: 'Alice' }];

  it('builds standard fields with optional branches and template labels', () => {
    const base: MappingData = {
      selectedProject: 'P1',
      selectedIssueType: 'IT1',
      selectedAssignee: '',
      summary: '{{name}}', // template => displayLabel null
      descriptionFieldMapping: '',
      customFields: [],
    };

    const out1 = createFieldMappings(base, projects, issueTypes, users);
    // project, issuetype, summary only (assignee/description omitted)
    expect(out1.map(f => f.jiraFieldId)).toEqual(['project', 'issuetype', 'summary']);
    expect(out1.find(f => f.jiraFieldId === 'project')?.displayLabel).toBe('Project One');
    expect(out1.find(f => f.jiraFieldId === 'issuetype')?.displayLabel).toBe('Task');
    // Optional fields should have displayLabel set to null when templates exist (like {{name}})
    expect(out1.find(f => f.jiraFieldId === 'summary')?.displayLabel).toBe(null);

    const withOptional: MappingData = {
      ...base,
      selectedAssignee: 'U1',
      descriptionFieldMapping: 'plain text desc', // not template => has label
      summary: 'plain summary',
    };
    const out2 = createFieldMappings(withOptional, projects, issueTypes, users);
    expect(out2.map(f => f.jiraFieldId)).toEqual([
      'project',
      'issuetype',
      'summary',
      'assignee',
      'description',
    ]);
    expect(out2.find(f => f.jiraFieldId === 'assignee')?.displayLabel).toBe('Alice');
    expect(out2.find(f => f.jiraFieldId === 'description')?.displayLabel).toBe('Description');
  });

  it('builds custom field mappings with type normalization, display label rules, and filtering', () => {
    const data: MappingData = {
      selectedProject: 'P1',
      selectedIssueType: 'IT1',
      selectedAssignee: '',
      summary: 'x',
      descriptionFieldMapping: '',
      customFields: [
        {
          jiraFieldKey: 'cf1',
          jiraFieldLabel: 'CF1',
          value: '123',
          type: 'number',
          required: true,
        },
        { jiraFieldKey: 'cf2', jiraFieldLabel: 'CF2', value: '{{slug}}', type: 'string' }, // template => displayLabel null
        { jiraFieldKey: 'cf3', value: 'abc', type: 'BOOLEAN', valueSource: 'json' }, // valueSource json => displayLabel null, label fallback to key
        { jiraFieldKey: 'cf4', jiraFieldLabel: 'CF4', value: 'y', type: 'user' },
        { jiraFieldKey: 'cf5', jiraFieldLabel: 'CF5', value: 'y', type: 'multiuser' },
        { jiraFieldKey: 'cf6', jiraFieldLabel: 'CF6', value: 'y', type: 'datetime' },
        { jiraFieldKey: 'cf7', jiraFieldLabel: 'CF7', value: 'y', type: 'url' },
        { jiraFieldKey: 'cf8', jiraFieldLabel: 'CF8', value: 'y', type: 'array' },
        { jiraFieldKey: 'cf10', jiraFieldLabel: 'Sprint', value: '123', type: 'sprint' },
        { jiraFieldKey: 'cf9', jiraFieldLabel: 'CF9', value: 'y', type: 'weird' }, // unknown => STRING
        // filtered out
        { jiraFieldKey: '   ', jiraFieldLabel: 'IGNORED', value: 'z' },
      ] as any,
    };

    const out = createFieldMappings(data, projects, issueTypes, users);
    const customs = out.filter(
      f => !['project', 'issuetype', 'summary', 'assignee', 'description'].includes(f.jiraFieldId)
    );

    // One filtered out => 10 custom fields remain
    expect(customs).toHaveLength(10);

    const byId = (id: string) => customs.find(f => f.jiraFieldId === id)!;

    expect(byId('cf1').dataType).toBe('NUMBER');
    expect(byId('cf1').required).toBe(true);

    expect(byId('cf2').dataType).toBe('STRING');
    expect(byId('cf2').displayLabel).toBeNull();

    expect(byId('cf3').dataType).toBe('BOOLEAN');
    expect(byId('cf3').jiraFieldName).toBe('cf3'); // label fallback to key
    expect(byId('cf3').displayLabel).toBeNull();

    expect(byId('cf4').dataType).toBe('USER');
    expect(byId('cf5').dataType).toBe('MULTIUSER');
    expect(byId('cf6').dataType).toBe('DATE');
    expect(byId('cf7').dataType).toBe('URL');
    expect(byId('cf8').dataType).toBe('ARRAY');
    expect(byId('cf9').dataType).toBe('STRING');
    expect(byId('cf10').dataType).toBe('NUMBER');
    expect(byId('cf10').metadata).toEqual({ fieldType: 'sprint' });
  });
});

describe('jiraFieldMappingUtils', () => {
  const mockProjects: SimpleProject[] = [
    { key: 'TEST', name: 'Test Project' },
    { key: 'DEMO', name: 'Demo Project' },
  ];

  const mockIssueTypes: SimpleIssueType[] = [
    { id: '1', name: 'Bug', subtask: false },
    { id: '2', name: 'Task', subtask: false },
    { id: '3', name: 'Story', subtask: false },
  ];

  const mockUsers: SimpleUser[] = [
    { accountId: 'user1', displayName: 'John Doe' },
    { accountId: 'user2', displayName: 'Jane Smith' },
  ];

  const validMappingData: MappingData = {
    selectedProject: 'TEST',
    selectedIssueType: '1',
    selectedAssignee: 'user1',
    summary: 'Test Summary Template',
    descriptionFieldMapping: 'Test Description Template',
  };

  describe('createFieldMappings', () => {
    it('creates field mappings with all data', () => {
      const result = createFieldMappings(validMappingData, mockProjects, mockIssueTypes, mockUsers);

      expect(result).toBeInstanceOf(Array);
      expect(result.length).toBeGreaterThan(0);

      // Check project field
      const projectField = result.find(field => field.jiraFieldId === 'project');
      expect(projectField).toBeDefined();
      expect(projectField?.template).toBe('TEST');
      expect(projectField?.displayLabel).toBe('Test Project');
      expect(projectField?.required).toBe(true);

      // Check issue type field
      const issueTypeField = result.find(field => field.jiraFieldId === 'issuetype');
      expect(issueTypeField).toBeDefined();
      expect(issueTypeField?.template).toBe('1');
      expect(issueTypeField?.displayLabel).toBe('Bug');

      // Check assignee field
      const assigneeField = result.find(field => field.jiraFieldId === 'assignee');
      expect(assigneeField).toBeDefined();
      expect(assigneeField?.template).toBe('user1');
      expect(assigneeField?.displayLabel).toBe('John Doe');

      // Check summary field
      const summaryField = result.find(field => field.jiraFieldId === 'summary');
      expect(summaryField).toBeDefined();
      expect(summaryField?.template).toBe('Test Summary Template');
      expect(summaryField?.required).toBe(true);

      // Check description field
      const descriptionField = result.find(field => field.jiraFieldId === 'description');
      expect(descriptionField).toBeDefined();
      expect(descriptionField?.template).toBe('Test Description Template');
    });

    it('creates field mappings with empty arrays', () => {
      const result = createFieldMappings(validMappingData, [], [], []);

      expect(result).toBeInstanceOf(Array);
      expect(result.length).toBeGreaterThan(0);

      // Should still create fields but with original values as labels
      const projectField = result.find(field => field.jiraFieldId === 'project');
      expect(projectField?.displayLabel).toBe('TEST'); // Original value used as label
    });

    it('handles missing selected values', () => {
      const incompleteMappingData: MappingData = {
        selectedProject: '',
        selectedIssueType: '',
        selectedAssignee: '',
        summary: '',
        descriptionFieldMapping: '',
      };

      const result = createFieldMappings(
        incompleteMappingData,
        mockProjects,
        mockIssueTypes,
        mockUsers
      );

      expect(result).toBeInstanceOf(Array);

      // Check that empty values result in empty templates
      const projectField = result.find(field => field.jiraFieldId === 'project');
      expect(projectField?.template).toBe('');
      expect(projectField?.displayLabel).toBe('');
    });

    it('handles non-existent selected values', () => {
      const mappingDataWithInvalidValues: MappingData = {
        selectedProject: 'NONEXISTENT',
        selectedIssueType: '999',
        selectedAssignee: 'invaliduser',
        summary: 'Test Summary',
        descriptionFieldMapping: 'Test Description',
      };

      const result = createFieldMappings(
        mappingDataWithInvalidValues,
        mockProjects,
        mockIssueTypes,
        mockUsers
      );

      expect(result).toBeInstanceOf(Array);

      // Should use the original value as label when not found
      const projectField = result.find(field => field.jiraFieldId === 'project');
      expect(projectField?.template).toBe('NONEXISTENT');
      expect(projectField?.displayLabel).toBe('NONEXISTENT');
    });

    it('creates fields with correct data types', () => {
      const result = createFieldMappings(validMappingData, mockProjects, mockIssueTypes, mockUsers);

      const projectField = result.find(field => field.jiraFieldId === 'project');
      expect(projectField?.dataType).toBe('OBJECT');

      const summaryField = result.find(field => field.jiraFieldId === 'summary');
      expect(summaryField?.dataType).toBe('STRING');

      const assigneeField = result.find(field => field.jiraFieldId === 'assignee');
      expect(assigneeField?.dataType).toBe('OBJECT');
    });
  });

  describe('type definitions', () => {
    it('should have correct SimpleProject structure', () => {
      const project: SimpleProject = {
        key: 'TEST',
        name: 'Test Project',
      };

      expect(project.key).toBe('TEST');
      expect(project.name).toBe('Test Project');
    });

    it('should have correct SimpleIssueType structure', () => {
      const issueType: SimpleIssueType = {
        id: '1',
        name: 'Bug',
      };

      expect(issueType.id).toBe('1');
      expect(issueType.name).toBe('Bug');
    });

    it('should have correct SimpleUser structure', () => {
      const user: SimpleUser = {
        accountId: 'user1',
        displayName: 'John Doe',
      };

      expect(user.accountId).toBe('user1');
      expect(user.displayName).toBe('John Doe');
    });

    it('should have correct MappingData structure', () => {
      const mappingData: MappingData = {
        selectedProject: 'TEST',
        selectedIssueType: '1',
        selectedAssignee: 'user1',
        summary: 'Test Summary',
        descriptionFieldMapping: 'Test Description',
      };

      expect(mappingData.selectedProject).toBe('TEST');
      expect(mappingData.selectedIssueType).toBe('1');
      expect(mappingData.selectedAssignee).toBe('user1');
      expect(mappingData.summary).toBe('Test Summary');
      expect(mappingData.descriptionFieldMapping).toBe('Test Description');
    });
  });

  describe('edge cases and advanced scenarios', () => {
    it('handles whitespace in assignee field correctly', () => {
      const dataWithWhitespace: MappingData = {
        selectedProject: 'TEST',
        selectedIssueType: '1',
        selectedAssignee: '   ',
        summary: 'Test',
        descriptionFieldMapping: '',
      };

      const result = createFieldMappings(
        dataWithWhitespace,
        mockProjects,
        mockIssueTypes,
        mockUsers
      );

      // Assignee with only whitespace should be trimmed and not included
      const assigneeField = result.find(field => field.jiraFieldId === 'assignee');
      expect(assigneeField).toBeUndefined();
    });

    it('handles whitespace in description field correctly', () => {
      const dataWithWhitespace: MappingData = {
        selectedProject: 'TEST',
        selectedIssueType: '1',
        selectedAssignee: '',
        summary: 'Test',
        descriptionFieldMapping: '   ',
      };

      const result = createFieldMappings(
        dataWithWhitespace,
        mockProjects,
        mockIssueTypes,
        mockUsers
      );

      // Description with only whitespace should be trimmed and not included
      const descriptionField = result.find(field => field.jiraFieldId === 'description');
      expect(descriptionField).toBeUndefined();
    });

    it('handles custom fields with undefined values', () => {
      const dataWithUndefinedCustomField: MappingData = {
        selectedProject: 'TEST',
        selectedIssueType: '1',
        selectedAssignee: '',
        summary: 'Test',
        descriptionFieldMapping: '',
        customFields: [
          {
            _id: '1',
            jiraFieldKey: 'cf1',
            jiraFieldLabel: 'CF1',
            value: undefined as any,
            type: 'string',
            valueSource: 'STATIC',
          } as any,
        ],
      };

      const result = createFieldMappings(
        dataWithUndefinedCustomField,
        mockProjects,
        mockIssueTypes,
        mockUsers
      );
      const customField = result.find(field => field.jiraFieldId === 'cf1');

      expect(customField).toBeDefined();
      expect(customField?.template).toBe(''); // undefined coerced to empty string
    });

    it('handles custom fields with null jiraFieldKey', () => {
      const dataWithNullKey: MappingData = {
        selectedProject: 'TEST',
        selectedIssueType: '1',
        selectedAssignee: '',
        summary: 'Test',
        descriptionFieldMapping: '',
        customFields: [
          {
            _id: '1',
            jiraFieldKey: null as any,
            jiraFieldLabel: 'CF1',
            value: 'test',
            type: 'string',
            valueSource: 'STATIC',
          } as any,
        ],
      };

      const result = createFieldMappings(dataWithNullKey, mockProjects, mockIssueTypes, mockUsers);
      const customFields = result.filter(
        field =>
          !['project', 'issuetype', 'summary', 'assignee', 'description'].includes(
            field.jiraFieldId
          )
      );

      // Should be filtered out
      expect(customFields).toHaveLength(0);
    });

    it('handles custom fields array as undefined', () => {
      const dataWithNoCustomFields: MappingData = {
        selectedProject: 'TEST',
        selectedIssueType: '1',
        selectedAssignee: '',
        summary: 'Test',
        descriptionFieldMapping: '',
        customFields: undefined,
      };

      const result = createFieldMappings(
        dataWithNoCustomFields,
        mockProjects,
        mockIssueTypes,
        mockUsers
      );

      // Should only contain standard fields
      expect(result.length).toBe(3); // project, issuetype, summary
    });

    it('handles date type variations', () => {
      const dataWithDateTypes: MappingData = {
        selectedProject: 'TEST',
        selectedIssueType: '1',
        selectedAssignee: '',
        summary: 'Test',
        descriptionFieldMapping: '',
        customFields: [
          { jiraFieldKey: 'cf1', jiraFieldLabel: 'Date1', value: 'test', type: 'date' },
          { jiraFieldKey: 'cf2', jiraFieldLabel: 'Date2', value: 'test', type: 'datetime' },
          { jiraFieldKey: 'cf3', jiraFieldLabel: 'Date3', value: 'test', type: 'DATE' },
        ] as any,
      };

      const result = createFieldMappings(
        dataWithDateTypes,
        mockProjects,
        mockIssueTypes,
        mockUsers
      );
      const cf1 = result.find(field => field.jiraFieldId === 'cf1');
      const cf2 = result.find(field => field.jiraFieldId === 'cf2');
      const cf3 = result.find(field => field.jiraFieldId === 'cf3');

      expect(cf1?.dataType).toBe('DATE');
      expect(cf2?.dataType).toBe('DATE');
      expect(cf3?.dataType).toBe('DATE');
    });

    it('sets defaultValue and metadata to expected values', () => {
      const result = createFieldMappings(validMappingData, mockProjects, mockIssueTypes, mockUsers);

      result.forEach(field => {
        expect(field.defaultValue).toBeNull();
        // Issue type field should have subtask metadata, other fields should be undefined
        if (field.jiraFieldId === 'issuetype') {
          expect(field.metadata).toEqual({ subtask: false });
        } else {
          expect(field.metadata).toBeUndefined();
        }
      });
    });

    it('handles custom fields with falsy but valid values', () => {
      const dataWithFalsyValues: MappingData = {
        selectedProject: 'TEST',
        selectedIssueType: '1',
        selectedAssignee: '',
        summary: 'Test',
        descriptionFieldMapping: '',
        customFields: [
          {
            _id: '1',
            jiraFieldKey: 'cf1',
            jiraFieldLabel: 'CF1',
            value: 0 as any,
            type: 'number',
            valueSource: 'STATIC',
          } as any,
          {
            _id: '2',
            jiraFieldKey: 'cf2',
            jiraFieldLabel: 'CF2',
            value: false as any,
            type: 'boolean',
            valueSource: 'STATIC',
          } as any,
        ],
      };

      const result = createFieldMappings(
        dataWithFalsyValues,
        mockProjects,
        mockIssueTypes,
        mockUsers
      );
      const cf1 = result.find(field => field.jiraFieldId === 'cf1');
      const cf2 = result.find(field => field.jiraFieldId === 'cf2');

      expect(cf1?.template).toBe('0');
      expect(cf2?.template).toBe('false');
    });

    it('handles option type mapping to STRING', () => {
      const dataWithOptionType: MappingData = {
        selectedProject: 'TEST',
        selectedIssueType: '1',
        selectedAssignee: '',
        summary: 'Test',
        descriptionFieldMapping: '',
        customFields: [
          { jiraFieldKey: 'cf1', jiraFieldLabel: 'CF1', value: 'test', type: 'option' },
        ] as any,
      };

      const result = createFieldMappings(
        dataWithOptionType,
        mockProjects,
        mockIssueTypes,
        mockUsers
      );
      const cf1 = result.find(field => field.jiraFieldId === 'cf1');

      expect(cf1?.dataType).toBe('STRING');
    });
  });
});
