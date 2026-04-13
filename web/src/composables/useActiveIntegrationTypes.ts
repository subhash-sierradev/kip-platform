import { computed } from 'vue';

import { NotificationEntityType } from '@/api/models/NotificationEntityType';
import { useAuthStore } from '@/store/auth';

/**
 * Derives the set of NotificationEntityTypes the current user is allowed to see,
 * based on their Keycloak feature roles. Mirrors the backend ServiceTypeAuthorizationHelper logic:
 *   feature_jira_webhook          → JIRA_WEBHOOK
 *   feature_arcgis_integration    → ARCGIS_INTEGRATION
 *   feature_confluence_integration → CONFLUENCE_INTEGRATION
 *   any of the above              → INTEGRATION_CONNECTION (connections are shared across types)
 *   always                        → SITE_CONFIG
 */
export function useActiveIntegrationTypes() {
  const authStore = useAuthStore();

  const activeEntityTypes = computed((): Set<NotificationEntityType> => {
    const roles = authStore.userRoles;
    const hasJira = roles.includes('feature_jira_webhook');
    const hasArcGIS = roles.includes('feature_arcgis_integration');
    const hasConfluence = roles.includes('feature_confluence_integration');

    const types = new Set<NotificationEntityType>([NotificationEntityType.SITE_CONFIG]);
    if (hasJira) {
      types.add(NotificationEntityType.JIRA_WEBHOOK);
    }
    if (hasArcGIS) {
      types.add(NotificationEntityType.ARCGIS_INTEGRATION);
    }
    if (hasConfluence) {
      types.add(NotificationEntityType.CONFLUENCE_INTEGRATION);
    }
    if (hasJira || hasArcGIS || hasConfluence) {
      types.add(NotificationEntityType.INTEGRATION_CONNECTION);
    }
    return types;
  });

  return { activeEntityTypes };
}
