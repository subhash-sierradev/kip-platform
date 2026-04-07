import type { ConfluenceFormData } from '@/types/ConfluenceFormData';

const baseSteps = [
  { key: 'combined', title: 'Integration Details' },
  { key: 'schedule', title: 'Schedule Configuration' },
  { key: 'connection', title: 'Confluence Connection' },
  { key: 'pageConfig', title: 'Confluence Configuration' },
];

const reviewTitleByMode: Record<'create' | 'edit' | 'clone', string> = {
  create: 'Review & Create',
  edit: 'Review & Update',
  clone: 'Review & Clone',
};

export const createSteps = (mode: 'create' | 'edit' | 'clone' = 'create') => [
  ...baseSteps,
  { key: 'review', title: reviewTitleByMode[mode] },
];

export const createDefaultFormData = (): ConfluenceFormData => ({
  name: '',
  description: '',
  itemType: 'DOCUMENT',
  subType: '',
  subTypeLabel: '',
  dynamicDocument: '',
  dynamicDocumentLabel: '',
  languageCodes: ['en'],
  reportNameTemplate: 'Aggregated Daily Report - {date}',
  includeTableOfContents: true,
  executionDate: null,
  executionTime: '02:00',
  frequencyPattern: 'DAILY',
  dailyFrequency: '24',
  selectedDays: [],
  selectedMonths: [],
  isExecuteOnMonthEnd: false,
  cronExpression: undefined,
  businessTimeZone: 'UTC',
  timeCalculationMode: 'FIXED_DAY_BOUNDARY',
  confluenceSpaceKey: '',
  confluenceSpaceLabel: '',
  confluenceSpaceKeyFolderKey: '',
  confluenceSpaceFolderLabel: '',
  connectionMethod: 'existing',
  existingConnectionId: '',
  createdConnectionId: '',
  connectionName: '',
  username: '',
  password: '',
});
