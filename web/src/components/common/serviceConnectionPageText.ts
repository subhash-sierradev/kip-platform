import { ServiceType, ServiceTypeMetadata } from '@/api/models/enums';

export function getConfirmDialogDescription(
  action: 'test' | 'delete' | null,
  serviceType: ServiceType
): string {
  if (action === 'delete') {
    return 'Are you sure you want to delete this connection? This action is permanent and cannot be undone.';
  }
  if (action === 'test') {
    const serviceLabel = ServiceTypeMetadata[serviceType]?.displayName ?? serviceType;
    return `This will test the ${serviceLabel} connection using the selected credentials to verify connectivity. Do you want to continue?`;
  }
  return '';
}

const DEPENDENT_NAME_CAPTIONS: Partial<Record<ServiceType, string>> = {
  [ServiceType.ARCGIS]: 'ArcGIS Integration Name',
  [ServiceType.CONFLUENCE]: 'Confluence Integration Name',
  [ServiceType.JIRA]: 'Jira Webhook Name',
};

export function getDependentsNameCaption(serviceType?: ServiceType): string {
  return serviceType
    ? DEPENDENT_NAME_CAPTIONS[serviceType] || 'Integration Name'
    : 'Integration Name';
}
