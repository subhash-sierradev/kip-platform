import type { BasicAuthCredential } from '@/api/models/AuthCredential';
import type { ConfluenceIntegrationCreateUpdateRequest } from '@/api/models/ConfluenceIntegrationCreateUpdateRequest';
import { ServiceType } from '@/api/models/enums';
import type { IntegrationConnectionRequest } from '@/api/models/IntegrationConnectionRequest';
import type { ArcGISFormData } from '@/types/ArcGISFormData';
import type { ConfluenceFormData } from '@/types/ConfluenceFormData';

import { buildScheduleRequest } from './arcgisIntegrationMapping';

export function buildConfluenceConnectionRequest(
  form: ConfluenceFormData,
  baseUrl: string
): IntegrationConnectionRequest {
  const credentials: BasicAuthCredential = {
    authType: 'BASIC_AUTH',
    username: form.username || '',
    password: form.password || '',
  };

  return {
    name: form.connectionName?.trim() || 'Untitled Confluence Connection',
    serviceType: ServiceType.CONFLUENCE,
    integrationSecret: {
      baseUrl: baseUrl || '',
      authType: 'BASIC_AUTH',
      credentials,
    },
  };
}

export function buildConfluenceIntegrationRequest(
  form: ConfluenceFormData,
  connectionId: string
): ConfluenceIntegrationCreateUpdateRequest {
  const schedule = buildScheduleRequest(form as unknown as ArcGISFormData);

  return {
    name: form.name,
    description: form.description || undefined,
    itemType: form.itemType || 'DOCUMENT',
    itemSubtype: form.subType,
    dynamicDocumentType: form.dynamicDocument || undefined,
    dynamicDocumentTypeLabel: form.dynamicDocumentLabel || undefined,
    languageCodes: form.languageCodes,
    reportNameTemplate: form.reportNameTemplate,
    confluenceSpaceKey: form.confluenceSpaceKey,
    confluenceSpaceKeyFolderKey: form.confluenceSpaceKeyFolderKey || 'ROOT',
    includeTableOfContents: form.includeTableOfContents,
    connectionId,
    schedule,
  };
}
