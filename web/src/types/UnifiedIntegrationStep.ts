export interface UnifiedIntegrationStepData {
  name: string;
  description: string;
  itemType: string;
  subType: string;
  subTypeLabel?: string;
  dynamicDocument?: string;
  dynamicDocumentLabel?: string;
}

export interface UnifiedIntegrationStepConfig {
  integrationTitle: string;
  integrationNameLabel: string;
  integrationNamePlaceholder: string;
  duplicateNameMessage: string;
  documentTitle: string;
  itemTypeLabel: string;
  itemSubtypeLabel: string;
  dynamicDocumentLabel: string;
  descriptionPlaceholder: string;
  dynamicSubtypeCodes?: string[];
}
