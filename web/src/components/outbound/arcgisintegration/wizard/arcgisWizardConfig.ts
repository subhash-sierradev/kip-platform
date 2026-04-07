import type { ArcGISFormData } from '../../../../types/ArcGISFormData';

const baseSteps = [
  { key: 'combined', title: 'Integration Details' },
  { key: 'schedule', title: 'Schedule Configuration' },
  { key: 'connection', title: 'ArcGIS Connection' },
  { key: 'mapping', title: 'Field Mapping' },
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

export const createDefaultFormData = (): ArcGISFormData => ({
  name: '',
  description: '',
  itemType: 'DOCUMENT',
  subType: '',
  dynamicDocument: '',
  selectedDocumentId: '',
  executionDate: '',
  executionTime: '',
  frequencyPattern: '',
  dailyFrequency: '',
  selectedDays: [],
  selectedMonths: [],
  monthlyPattern: '',
  monthlyWeek: '',
  monthlyWeekday: '',
  monthlyDay: null,
  isExecuteOnMonthEnd: false,
  businessTimeZone: 'UTC',
  timeCalculationMode: 'FLEXIBLE_INTERVAL',
  arcgisEndpoint: '',
  fetchMode: 'GET',
  credentialType: 'OAUTH2', // ArcGIS integrations use OAuth2 authentication by default
  username: '',
  password: '',
  clientId: '',
  clientSecret: '',
  tokenUrl: 'https://www.arcgis.com/sharing/rest/oauth2/token',
  scope: '',
  fieldMappings: [
    {
      id: '',
      sourceField: '',
      targetField: '',
      transformationType: '',
      isMandatory: false,
      displayOrder: 0,
    },
  ],
});
