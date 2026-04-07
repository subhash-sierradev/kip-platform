import { describe, expect, it, vi } from 'vitest';

import { ServiceType } from '@/api/models/enums';
import {
  buildConfluenceConnectionRequest,
  buildConfluenceIntegrationRequest,
} from '@/utils/confluenceIntegrationMapping';

const scheduleMock = { cronExpression: '0 0 2 * * *' };

vi.mock('@/utils/arcgisIntegrationMapping', () => ({
  buildScheduleRequest: vi.fn(() => scheduleMock),
}));

describe('confluenceIntegrationMapping', () => {
  it('buildConfluenceConnectionRequest returns expected payload with defaults', () => {
    const request = buildConfluenceConnectionRequest(
      {
        connectionName: '   ',
        username: '',
        password: '',
        name: '',
        description: '',
        itemType: 'DOCUMENT',
        subType: '',
        languageCodes: ['en'],
        reportNameTemplate: 'Template',
        includeTableOfContents: true,
        executionDate: null,
        executionTime: '02:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '24',
        selectedDays: [],
        selectedMonths: [],
        isExecuteOnMonthEnd: false,
        confluenceSpaceKey: '',
      },
      ''
    );

    expect(request).toEqual({
      name: 'Untitled Confluence Connection',
      serviceType: ServiceType.CONFLUENCE,
      integrationSecret: {
        baseUrl: '',
        authType: 'BASIC_AUTH',
        credentials: {
          authType: 'BASIC_AUTH',
          username: '',
          password: '',
        },
      },
    });
  });

  it('buildConfluenceConnectionRequest preserves provided values', () => {
    const request = buildConfluenceConnectionRequest(
      {
        connectionName: 'Confluence Primary',
        username: 'owner@acme.com',
        password: 'api-token',
        name: 'Integration Name',
        description: '',
        itemType: 'DOCUMENT',
        subType: '',
        languageCodes: ['en'],
        reportNameTemplate: 'Template',
        includeTableOfContents: true,
        executionDate: null,
        executionTime: '02:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '24',
        selectedDays: [],
        selectedMonths: [],
        isExecuteOnMonthEnd: false,
        confluenceSpaceKey: '',
      },
      'https://acme.atlassian.net/wiki'
    );

    expect(request.name).toBe('Confluence Primary');
    expect(request.integrationSecret?.baseUrl).toBe('https://acme.atlassian.net/wiki');
    expect(request.integrationSecret?.credentials).toMatchObject({
      username: 'owner@acme.com',
      password: 'api-token',
    });
  });

  it('buildConfluenceIntegrationRequest maps fields and optional values', () => {
    const request = buildConfluenceIntegrationRequest(
      {
        name: 'Confluence Integration',
        description: 'Sync docs',
        itemType: 'DOCUMENT',
        subType: 'DOCUMENT_FINAL_DYNAMIC',
        dynamicDocument: 'Invoice',
        dynamicDocumentLabel: 'Invoice Label',
        languageCodes: ['en', 'fr'],
        reportNameTemplate: 'Report {date}',
        includeTableOfContents: true,
        confluenceSpaceKey: 'ABC',
        confluenceSpaceKeyFolderKey: 'SPACE/FOLDER',
        connectionName: 'unused',
        username: 'user',
        password: 'pass',
        executionDate: null,
        executionTime: '02:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '24',
        selectedDays: [],
        selectedMonths: [],
        isExecuteOnMonthEnd: false,
      },
      'connection-123'
    );

    expect(request).toEqual({
      name: 'Confluence Integration',
      description: 'Sync docs',
      itemType: 'DOCUMENT',
      itemSubtype: 'DOCUMENT_FINAL_DYNAMIC',
      dynamicDocumentType: 'Invoice',
      dynamicDocumentTypeLabel: 'Invoice Label',
      languageCodes: ['en', 'fr'],
      reportNameTemplate: 'Report {date}',
      confluenceSpaceKey: 'ABC',
      confluenceSpaceKeyFolderKey: 'SPACE/FOLDER',
      includeTableOfContents: true,
      connectionId: 'connection-123',
      schedule: scheduleMock,
    });
  });

  it('buildConfluenceIntegrationRequest omits optional empty values', () => {
    const request = buildConfluenceIntegrationRequest(
      {
        name: 'Confluence Integration',
        description: '',
        itemType: 'DOCUMENT',
        subType: '',
        dynamicDocument: '',
        dynamicDocumentLabel: '',
        languageCodes: ['en'],
        reportNameTemplate: 'Report {date}',
        includeTableOfContents: false,
        confluenceSpaceKey: 'ABC',
        confluenceSpaceKeyFolderKey: '',
        connectionName: 'unused',
        username: '',
        password: '',
        executionDate: null,
        executionTime: '02:00',
        frequencyPattern: 'DAILY',
        dailyFrequency: '24',
        selectedDays: [],
        selectedMonths: [],
        isExecuteOnMonthEnd: false,
      },
      'connection-123'
    );

    expect(request.description).toBeUndefined();
    expect(request.itemSubtype).toBe('');
    expect(request.dynamicDocumentType).toBeUndefined();
    expect(request.dynamicDocumentTypeLabel).toBeUndefined();
    expect(request.confluenceSpaceKeyFolderKey).toBeUndefined();
  });
});
