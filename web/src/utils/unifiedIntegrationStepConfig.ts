import type { UnifiedIntegrationStepConfig } from '@/types/UnifiedIntegrationStep';

export const ARCGIS_UNIFIED_STEP_CONFIG: UnifiedIntegrationStepConfig = {
  integrationTitle: 'ArcGIS Integration Details',
  integrationNameLabel: 'ArcGIS Integration Name',
  integrationNamePlaceholder: 'Enter ArcGIS integration name',
  duplicateNameMessage: 'ArcGIS Integration name already exists',
  documentTitle: 'Document Selection',
  itemTypeLabel: 'Item Type',
  itemSubtypeLabel: 'Item Subtype',
  dynamicDocumentLabel: 'Dynamic Document',
  descriptionPlaceholder:
    "Provide a brief description of this integration's purpose and functionality...",
};

export const CONFLUENCE_UNIFIED_STEP_CONFIG: UnifiedIntegrationStepConfig = {
  integrationTitle: 'Confluence Integration Details',
  integrationNameLabel: 'Confluence Integration Name',
  integrationNamePlaceholder: 'Enter Confluence integration name',
  duplicateNameMessage: 'Confluence Integration name already exists',
  documentTitle: 'Document Configuration',
  itemTypeLabel: 'Item Type',
  itemSubtypeLabel: 'Item Subtype',
  dynamicDocumentLabel: 'Dynamic Document',
  descriptionPlaceholder: "Provide a brief description of this integration's purpose...",
};
